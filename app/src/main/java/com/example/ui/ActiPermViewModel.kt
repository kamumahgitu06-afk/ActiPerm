package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppScannerDataSource
import com.example.data.AppScannerRepository
import com.example.data.PermissionHistoryDataSource
import com.example.data.PermissionHistoryRepository
import com.example.domain.PermissionAnalyzer
import com.example.domain.PermissionManagerBridge
import com.example.model.AppPermissionDetail
import com.example.model.AppTab
import com.example.model.BatchRevokeProposal
import com.example.model.BatchRevokeType
import com.example.model.BulkDisableQueueState
import com.example.model.BulkItemStatus
import com.example.model.BulkQueueEntry
import com.example.model.DeviceFilterOption
import com.example.model.InstalledAppInfo
import com.example.model.MainNavTab
import com.example.model.PermissionCategory
import com.example.model.PermissionChangeLogEntry
import com.example.model.PermissionControlMode
import com.example.model.PermissionExecutionMode
import com.example.model.RiskLevel
import com.example.model.SensorFilterType
import com.example.model.ShizukuStatusSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActiPermUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val selectedMainTab: MainNavTab = MainNavTab.DASHBOARD,
    val selectedTab: AppTab = AppTab.DOWNLOADED,
    val searchQuery: String = "",
    val riskFilter: RiskLevel? = null,
    val deviceFilter: DeviceFilterOption = DeviceFilterOption.ALL,
    val sensorTrioFilterActive: Boolean = false,
    val selectedSensors: Set<SensorFilterType> = emptySet(),
    val onlyUnusedSystemApps: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val lastScannedAtMillis: Long = 0L,
    val detectedDeviceBrandName: String = PermissionAnalyzer.detectDeviceBrandDisplayName(),
    val androidVersionLabel: String = PermissionAnalyzer.formatAndroidVersionLabel(),
    val allApps: List<InstalledAppInfo> = emptyList(),
    val selectedSystemPackages: Set<String> = emptySet(),
    val bulkDisableQueue: BulkDisableQueueState? = null,
    val autoAdvanceCountdownActive: Boolean = false,
    val selectedPermissionCategory: PermissionCategory? = PermissionCategory.LOCATION,
    val permissionGrantFilter: Boolean? = null,
    val includeSystemAppsInPermManager: Boolean = false,
    val isRootAvailable: Boolean = false,
    val permissionActionMessage: String? = null,
    val shizukuStatus: ShizukuStatusSnapshot = ShizukuStatusSnapshot(),
    val permissionHistory: List<PermissionChangeLogEntry> = emptyList(),
    val pendingBatchProposal: BatchRevokeProposal? = null,
    val isExecutingBatch: Boolean = false
) {
    val executionMode: PermissionExecutionMode
        get() = PermissionManagerBridge.resolveExecutionMode(shizukuStatus, isRootAvailable)
    val totalAppsCount: Int
        get() = allApps.size

    val downloadedAppsCount: Int
        get() = allApps.count { !it.isSystemApp }

    val systemAppsCount: Int
        get() = allApps.count { it.isSystemApp }

    val totalDangerousPermissionsCount: Int
        get() = allApps.sumOf { it.unexpectedDangerousCount }

    val dangerousAppsCount: Int
        get() = allApps.count { it.riskLevel == RiskLevel.DANGEROUS }

    val suspiciousAppsCount: Int
        get() = allApps.count { it.riskLevel == RiskLevel.SUSPICIOUS }

    val safeAppsCount: Int
        get() = allApps.count { it.riskLevel == RiskLevel.SAFE }

    val visibleApps: List<InstalledAppInfo>
        get() {
            val byTab = allApps.filter { app ->
                when (selectedTab) {
                    AppTab.DOWNLOADED -> !app.isSystemApp
                    AppTab.SYSTEM -> app.isSystemApp
                    AppTab.PERMISSION_MANAGER -> includeSystemAppsInPermManager || !app.isSystemApp
                }
            }
            return byTab.filter { app ->
                val matchesQuery = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

                val matchesRisk = riskFilter == null || app.riskLevel == riskFilter

                val targetBrand = deviceFilter.brand
                val matchesDeviceBrand = targetBrand == null || app.packageBrand == targetBrand

                val matchesTrio = !sensorTrioFilterActive || app.hasAnyCoreSensor

                val matchesSpecificSensors = selectedSensors.isEmpty() || selectedSensors.all { sensor ->
                    when (sensor) {
                        SensorFilterType.LOCATION -> app.hasLocationPermission
                        SensorFilterType.CAMERA -> app.hasCameraPermission
                        SensorFilterType.MICROPHONE -> app.hasMicrophonePermission
                    }
                }

                val matchesUnused = if (selectedTab == AppTab.SYSTEM && onlyUnusedSystemApps) {
                    app.isUnused
                } else {
                    true
                }

                matchesQuery && matchesRisk && matchesDeviceBrand && matchesTrio && matchesSpecificSensors && matchesUnused
            }
        }

    val visiblePermissionEntries: List<PermissionMatrixEntry>
        get() {
            val targetCategory = selectedPermissionCategory
            val list = ArrayList<PermissionMatrixEntry>()
            for (app in visibleApps) {
                for (perm in app.permissions) {
                    val mode = PermissionManagerBridge.classifyControlMode(perm)
                    if (mode == PermissionControlMode.INSTALL_TIME_INFO_ONLY) continue
                    if (targetCategory != null && perm.category != targetCategory) continue
                    if (permissionGrantFilter != null && perm.isGranted != permissionGrantFilter) continue
                    list.add(
                        PermissionMatrixEntry(
                            app = app,
                            permission = perm,
                            controlMode = mode
                        )
                    )
                }
            }
            return list
        }
}

data class PermissionMatrixEntry(
    val app: InstalledAppInfo,
    val permission: AppPermissionDetail,
    val controlMode: PermissionControlMode
)

sealed interface ActiPermEvent {
    data class OpenSystemAppDetails(val packageName: String) : ActiPermEvent
}

typealias PermissionCommandExecutor = suspend (
    mode: PermissionExecutionMode,
    packageName: String,
    permissionRaw: String,
    grant: Boolean
) -> Result<String>

class ActiPermViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AppScannerDataSource = AppScannerRepository(application),
    private val historyRepository: PermissionHistoryDataSource = PermissionHistoryRepository(application),
    private val permissionExecutor: PermissionCommandExecutor = { mode, pkg, perm, grant ->
        PermissionManagerBridge.executePermissionCommand(mode, pkg, perm, grant)
    }
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        ActiPermUiState(permissionHistory = historyRepository.loadHistory())
    )
    val uiState: StateFlow<ActiPermUiState> = _uiState.asStateFlow()

    private val _events = Channel<ActiPermEvent>(Channel.BUFFERED)
    val events: Flow<ActiPermEvent> = _events.receiveAsFlow()

    private var autoAdvanceJob: Job? = null

    init {
        refreshApps(initialLoad = true)
    }

    fun refreshApps(initialLoad: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = initialLoad && it.allApps.isEmpty(),
                    isScanning = true
                )
            }
            val snapshot = repository.scanInstalledApps()
            _uiState.update { current ->
                val installedSet = snapshot.apps.mapTo(HashSet()) { it.packageName }
                val prunedSelection = current.selectedSystemPackages.filterTo(HashSet()) { it in installedSet }
                current.copy(
                    isLoading = false,
                    isScanning = false,
                    hasUsageAccess = snapshot.hasUsageAccess,
                    lastScannedAtMillis = snapshot.scannedAtMillis,
                    detectedDeviceBrandName = snapshot.detectedDeviceBrandName,
                    androidVersionLabel = snapshot.androidVersionLabel,
                    isRootAvailable = snapshot.isRootAvailable,
                    shizukuStatus = snapshot.shizukuStatus,
                    allApps = snapshot.apps,
                    selectedSystemPackages = prunedSelection
                )
            }
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            val snapshot = repository.scanInstalledApps()
            var nextPackageToOpen: String? = null
            var shouldScheduleAutoAdvance = false

            _uiState.update { current ->
                val appMap = snapshot.apps.associateBy { it.packageName }
                val updatedQueue = current.bulkDisableQueue?.let { queue ->
                    if (!queue.awaitingReturnFromSettings || queue.isFinished) {
                        queue
                    } else {
                        val currIdx = queue.currentIndex
                        val currEntry = queue.entries.getOrNull(currIdx)
                        val refreshedEntries = queue.entries.mapIndexed { idx, entry ->
                            if (idx == currIdx && currEntry != null) {
                                val appAfter = appMap[entry.packageName]
                                val status = if (appAfter != null && !appAfter.isEnabled) {
                                    BulkItemStatus.DISABLED
                                } else {
                                    BulkItemStatus.STILL_ACTIVE
                                }
                                entry.copy(status = status)
                            } else {
                                entry
                            }
                        }

                        val nextIndex = currIdx + 1
                        if (nextIndex >= refreshedEntries.size) {
                            queue.copy(
                                entries = refreshedEntries,
                                awaitingReturnFromSettings = false,
                                isFinished = true
                            )
                        } else {
                            shouldScheduleAutoAdvance = true
                            nextPackageToOpen = refreshedEntries[nextIndex].packageName
                            queue.copy(
                                entries = refreshedEntries,
                                currentIndex = nextIndex,
                                awaitingReturnFromSettings = false
                            )
                        }
                    }
                }

                val installedSet = snapshot.apps.mapTo(HashSet()) { it.packageName }
                val prunedSelection = current.selectedSystemPackages.filterTo(HashSet()) { pkg ->
                    val app = appMap[pkg]
                    pkg in installedSet && (app?.isEnabled != false)
                }

                current.copy(
                    isLoading = false,
                    isScanning = false,
                    hasUsageAccess = snapshot.hasUsageAccess,
                    lastScannedAtMillis = snapshot.scannedAtMillis,
                    detectedDeviceBrandName = snapshot.detectedDeviceBrandName,
                    androidVersionLabel = snapshot.androidVersionLabel,
                    isRootAvailable = snapshot.isRootAvailable,
                    shizukuStatus = snapshot.shizukuStatus,
                    allApps = snapshot.apps,
                    selectedSystemPackages = prunedSelection,
                    bulkDisableQueue = updatedQueue,
                    autoAdvanceCountdownActive = shouldScheduleAutoAdvance
                )
            }

            if (shouldScheduleAutoAdvance && nextPackageToOpen != null) {
                scheduleAutoAdvance()
            }
        }
    }

    private fun scheduleAutoAdvance() {
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            delay(900L)
            openCurrentQueueItem()
        }
    }

    fun selectMainTab(tab: MainNavTab) {
        _uiState.update { it.copy(selectedMainTab = tab) }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun refreshShizukuStatus() {
        val status = repository.checkShizukuStatus()
        val msg = when (status.connectionState) {
            com.example.model.ShizukuConnectionState.ACTIVE_GRANTED ->
                "Shizuku aktif & izin sudah diberikan! Mode A siap digunakan."
            com.example.model.ShizukuConnectionState.ACTIVE_NEED_PERMISSION ->
                "Shizuku aktif, tetapi izin belum diberikan ke ActiPerm."
            com.example.model.ShizukuConnectionState.INSTALLED_STOPPED ->
                "Aplikasi Shizuku terinstal, tetapi layanan belum dijalankan."
            com.example.model.ShizukuConnectionState.NOT_INSTALLED ->
                "Shizuku belum terinstal. Gunakan Mode B/C (ADB atau Pengaturan Manual)."
        }
        _uiState.update {
            it.copy(
                shizukuStatus = status,
                permissionActionMessage = msg
            )
        }
    }

    fun requestShizukuPermission() {
        val triggered = PermissionManagerBridge.requestShizukuPermission()
        if (!triggered) {
            _uiState.update {
                it.copy(
                    permissionActionMessage = "Layanan Shizuku belum berjalan. Buka aplikasi Shizuku lalu jalankan terlebih dahulu."
                )
            }
        } else {
            refreshShizukuStatus()
        }
    }

    fun executePermissionToggle(
        app: InstalledAppInfo,
        permission: AppPermissionDetail,
        grant: Boolean
    ) {
        if (!grant && (app.isProtectedCriticalSystem || PermissionManagerBridge.isProtectedCriticalSystemApp(app.packageName, app.isSystemApp, app.installStatus))) {
            _uiState.update {
                it.copy(
                    permissionActionMessage = "Aplikasi Sistem - Dilindungi: izin ${app.appName} dikunci demi mencegah bootloop."
                )
            }
            return
        }

        val mode = _uiState.value.executionMode
        if (mode == PermissionExecutionMode.MANUAL_ADB) {
            // Mode C Fallback Manual: buka Settings > Izin Aplikasi secara aman tanpa error
            _events.trySend(ActiPermEvent.OpenSystemAppDetails(app.packageName))
            val updatedHistory = historyRepository.recordChange(
                PermissionChangeLogEntry(
                    id = System.nanoTime(),
                    timestampMillis = System.currentTimeMillis(),
                    appName = app.appName,
                    packageName = app.packageName,
                    permissionRaw = permission.rawName,
                    permissionFriendly = permission.friendlyName,
                    granted = grant,
                    executionModeLabel = "Pengaturan Manual",
                    success = true,
                    note = "Dialihkan ke Pengaturan Sistem Android"
                )
            )
            _uiState.update {
                it.copy(
                    permissionHistory = updatedHistory,
                    permissionActionMessage = "Membuka Pengaturan Aplikasi ${app.appName} untuk ubah izin manual."
                )
            }
            return
        }

        viewModelScope.launch {
            val result = permissionExecutor(mode, app.packageName, permission.rawName, grant)
            val msg = result.getOrElse { it.message ?: "Gagal mengubah izin." }
            val updatedHistory = if (result.isSuccess) {
                historyRepository.recordChange(
                    PermissionChangeLogEntry(
                        id = System.nanoTime(),
                        timestampMillis = System.currentTimeMillis(),
                        appName = app.appName,
                        packageName = app.packageName,
                        permissionRaw = permission.rawName,
                        permissionFriendly = permission.friendlyName,
                        granted = grant,
                        executionModeLabel = if (mode == PermissionExecutionMode.SHIZUKU) "Shizuku" else "Root",
                        success = true
                    )
                )
            } else {
                _uiState.value.permissionHistory
            }
            _uiState.update {
                it.copy(
                    permissionHistory = updatedHistory,
                    permissionActionMessage = msg
                )
            }
            if (result.isSuccess) {
                refreshApps()
            }
        }
    }

    fun openBatchRevokeProposal(type: BatchRevokeType) {
        val proposal = PermissionManagerBridge.collectBatchCandidates(
            apps = _uiState.value.allApps,
            type = type
        )
        _uiState.update { it.copy(pendingBatchProposal = proposal) }
    }

    fun dismissBatchProposal() {
        _uiState.update { it.copy(pendingBatchProposal = null) }
    }

    fun confirmBatchRevoke() {
        val proposal = _uiState.value.pendingBatchProposal ?: return
        if (proposal.candidates.isEmpty()) {
            _uiState.update {
                it.copy(
                    pendingBatchProposal = null,
                    permissionActionMessage = "Tidak ada izin aktif yang memenuhi kriteria untuk dicabut."
                )
            }
            return
        }

        val mode = _uiState.value.executionMode
        if (mode == PermissionExecutionMode.MANUAL_ADB) {
            val uniqueApps = proposal.candidates
                .distinctBy { it.app.packageName }
                .map { BulkQueueEntry(packageName = it.app.packageName, appName = it.app.appName) }
            _uiState.update {
                it.copy(
                    pendingBatchProposal = null,
                    bulkDisableQueue = BulkDisableQueueState(
                        entries = uniqueApps,
                        currentIndex = 0,
                        awaitingReturnFromSettings = false,
                        isFinished = false
                    ),
                    autoAdvanceCountdownActive = false,
                    permissionActionMessage = "Mode Manual aktif: buka Pengaturan tiap aplikasi secara berurutan."
                )
            }
            openCurrentQueueItem()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExecutingBatch = true) }
            var successCount = 0
            var failCount = 0
            var latestHistory = _uiState.value.permissionHistory

            for (candidate in proposal.candidates) {
                val res = permissionExecutor(
                    mode,
                    candidate.app.packageName,
                    candidate.permission.rawName,
                    false
                )
                if (res.isSuccess) {
                    successCount++
                    latestHistory = historyRepository.recordChange(
                        PermissionChangeLogEntry(
                            id = System.nanoTime(),
                            timestampMillis = System.currentTimeMillis(),
                            appName = candidate.app.appName,
                            packageName = candidate.app.packageName,
                            permissionRaw = candidate.permission.rawName,
                            permissionFriendly = candidate.permission.friendlyName,
                            granted = false,
                            executionModeLabel = if (mode == PermissionExecutionMode.SHIZUKU) {
                                "Batch Shizuku"
                            } else {
                                "Batch Root"
                            },
                            success = true
                        )
                    )
                } else {
                    failCount++
                }
            }

            val summary = buildString {
                append("Aksi massal selesai: $successCount izin dicabut")
                if (failCount > 0) append(", $failCount gagal")
                append(".")
            }
            _uiState.update {
                it.copy(
                    pendingBatchProposal = null,
                    isExecutingBatch = false,
                    permissionHistory = latestHistory,
                    permissionActionMessage = summary
                )
            }
            refreshApps()
        }
    }

    fun clearPermissionHistory() {
        val cleared = historyRepository.clearHistory()
        _uiState.update {
            it.copy(
                permissionHistory = cleared,
                permissionActionMessage = "Riwayat perubahan izin dibersihkan."
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleRiskFilter(level: RiskLevel?) {
        _uiState.update { current ->
            val next = if (current.riskFilter == level) null else level
            current.copy(riskFilter = next)
        }
    }

    fun selectDeviceFilter(option: DeviceFilterOption) {
        _uiState.update { current ->
            current.copy(deviceFilter = option)
        }
    }

    fun toggleSensorTrioFilter() {
        _uiState.update { current ->
            current.copy(sensorTrioFilterActive = !current.sensorTrioFilterActive)
        }
    }

    fun toggleSpecificSensor(sensor: SensorFilterType) {
        _uiState.update { current ->
            val nextSet = current.selectedSensors.toMutableSet()
            if (!nextSet.add(sensor)) {
                nextSet.remove(sensor)
            }
            current.copy(selectedSensors = nextSet)
        }
    }

    fun toggleOnlyUnusedSystemApps() {
        _uiState.update { current ->
            current.copy(onlyUnusedSystemApps = !current.onlyUnusedSystemApps)
        }
    }

    fun resetFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                riskFilter = null,
                deviceFilter = DeviceFilterOption.ALL,
                sensorTrioFilterActive = false,
                selectedSensors = emptySet(),
                onlyUnusedSystemApps = false,
                permissionGrantFilter = null
            )
        }
    }

    fun selectPermissionCategory(category: PermissionCategory?) {
        _uiState.update { it.copy(selectedPermissionCategory = category) }
    }

    fun selectPermissionGrantFilter(grantedOnly: Boolean?) {
        _uiState.update { it.copy(permissionGrantFilter = grantedOnly) }
    }

    fun toggleIncludeSystemAppsInPermManager() {
        _uiState.update { it.copy(includeSystemAppsInPermManager = !it.includeSystemAppsInPermManager) }
    }

    fun togglePermissionViaRoot(packageName: String, permissionRaw: String, grant: Boolean) {
        val targetApp = findAppByPackage(packageName)
        val targetPerm = targetApp?.permissions?.firstOrNull { it.rawName == permissionRaw }
        if (targetApp != null && targetPerm != null) {
            executePermissionToggle(targetApp, targetPerm, grant)
            return
        }
        viewModelScope.launch {
            val mode = _uiState.value.executionMode
            val result = permissionExecutor(
                if (mode == PermissionExecutionMode.MANUAL_ADB) PermissionExecutionMode.ROOT else mode,
                packageName,
                permissionRaw,
                grant
            )
            val msg = result.getOrElse { it.message ?: "Gagal mengubah izin." }
            _uiState.update { it.copy(permissionActionMessage = msg) }
            if (result.isSuccess) {
                refreshApps()
            }
        }
    }

    fun clearPermissionActionMessage() {
        _uiState.update { it.copy(permissionActionMessage = null) }
    }

    fun toggleSystemAppSelection(packageName: String) {
        _uiState.update { current ->
            val updated = current.selectedSystemPackages.toMutableSet()
            if (!updated.add(packageName)) {
                updated.remove(packageName)
            }
            current.copy(selectedSystemPackages = updated)
        }
    }

    fun selectAllUnusedOrVisibleSystemApps() {
        _uiState.update { current ->
            val visibleEnabledSystem = current.visibleApps
                .filter { it.isSystemApp && it.isEnabled }
                .map { it.packageName }
                .toSet()
            current.copy(selectedSystemPackages = current.selectedSystemPackages + visibleEnabledSystem)
        }
    }

    fun clearSystemAppSelection() {
        _uiState.update { it.copy(selectedSystemPackages = emptySet()) }
    }

    fun startBulkDisableQueue() {
        val selected = _uiState.value.selectedSystemPackages
        if (selected.isEmpty()) return

        val entries = _uiState.value.allApps
            .filter { it.isSystemApp && it.packageName in selected }
            .map { BulkQueueEntry(packageName = it.packageName, appName = it.appName) }

        if (entries.isEmpty()) return

        _uiState.update {
            it.copy(
                bulkDisableQueue = BulkDisableQueueState(
                    entries = entries,
                    currentIndex = 0,
                    awaitingReturnFromSettings = false,
                    isFinished = false
                ),
                autoAdvanceCountdownActive = false
            )
        }
        openCurrentQueueItem()
    }

    fun openCurrentQueueItem() {
        autoAdvanceJob?.cancel()
        val queue = _uiState.value.bulkDisableQueue ?: return
        val entry = queue.currentEntry ?: return

        val updatedEntries = queue.entries.mapIndexed { idx, item ->
            if (idx == queue.currentIndex) item.copy(status = BulkItemStatus.ACTIVE) else item
        }
        _uiState.update {
            it.copy(
                bulkDisableQueue = queue.copy(
                    entries = updatedEntries,
                    awaitingReturnFromSettings = true
                ),
                autoAdvanceCountdownActive = false
            )
        }
        _events.trySend(ActiPermEvent.OpenSystemAppDetails(entry.packageName))
    }

    fun skipCurrentQueueItem() {
        autoAdvanceJob?.cancel()
        val queue = _uiState.value.bulkDisableQueue ?: return
        val currIdx = queue.currentIndex
        val updatedEntries = queue.entries.mapIndexed { idx, item ->
            if (idx == currIdx) item.copy(status = BulkItemStatus.SKIPPED) else item
        }
        val nextIndex = currIdx + 1
        if (nextIndex >= updatedEntries.size) {
            _uiState.update {
                it.copy(
                    bulkDisableQueue = queue.copy(
                        entries = updatedEntries,
                        awaitingReturnFromSettings = false,
                        isFinished = true
                    ),
                    autoAdvanceCountdownActive = false
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    bulkDisableQueue = queue.copy(
                        entries = updatedEntries,
                        currentIndex = nextIndex,
                        awaitingReturnFromSettings = false
                    ),
                    autoAdvanceCountdownActive = false
                )
            }
            openCurrentQueueItem()
        }
    }

    fun cancelBulkDisableQueue() {
        autoAdvanceJob?.cancel()
        _uiState.update {
            it.copy(
                bulkDisableQueue = null,
                autoAdvanceCountdownActive = false
            )
        }
    }

    fun findAppByPackage(packageName: String): InstalledAppInfo? {
        return _uiState.value.allApps.firstOrNull { it.packageName == packageName }
    }
}
