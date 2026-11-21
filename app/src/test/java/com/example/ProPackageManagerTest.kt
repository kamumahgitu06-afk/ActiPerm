package com.example

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppScannerDataSource
import com.example.util.SystemIntents
import com.example.data.PermissionHistoryDataSource
import com.example.data.ScanSnapshot
import com.example.domain.PermissionAnalyzer
import com.example.domain.PermissionManagerBridge
import com.example.model.AppInstallStatus
import com.example.model.AppTab
import com.example.model.BatchRevokeType
import com.example.model.InstalledAppInfo
import com.example.model.MainNavTab
import com.example.model.PermissionCategory
import com.example.model.PermissionChangeLogEntry
import com.example.model.PermissionControlMode
import com.example.model.PermissionExecutionMode
import com.example.model.RiskLevel
import com.example.model.ShizukuConnectionState
import com.example.model.ShizukuStatusSnapshot
import com.example.ui.ActiPermEvent
import com.example.ui.ActiPermViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProPackageManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `permission manager resolves execution modes and adb commands properly`() {
        val shizukuGranted = ShizukuStatusSnapshot(
            connectionState = ShizukuConnectionState.ACTIVE_GRANTED,
            isInstalled = true,
            isBinderAlive = true,
            hasPermission = true
        )
        assertEquals(
            PermissionExecutionMode.SHIZUKU,
            PermissionManagerBridge.resolveExecutionMode(shizukuGranted, isRootAvailable = false)
        )

        val stopped = ShizukuStatusSnapshot(
            connectionState = ShizukuConnectionState.INSTALLED_STOPPED,
            isInstalled = true
        )
        assertEquals(
            PermissionExecutionMode.MANUAL_ADB,
            PermissionManagerBridge.resolveExecutionMode(stopped, isRootAvailable = false)
        )
        assertEquals(
            PermissionExecutionMode.ROOT,
            PermissionManagerBridge.resolveExecutionMode(stopped, isRootAvailable = true)
        )

        assertEquals(
            "adb shell pm revoke com.example.spy android.permission.CAMERA",
            PermissionManagerBridge.buildAdbCommand("com.example.spy", "android.permission.CAMERA", grant = false)
        )
        assertEquals(
            "adb shell pm grant com.example.spy android.permission.CAMERA",
            PermissionManagerBridge.buildAdbCommand("com.example.spy", "android.permission.CAMERA", grant = true)
        )

        assertTrue(PermissionManagerBridge.isProtectedCriticalSystemApp("com.android.systemui", isSystemApp = true))
        assertTrue(PermissionManagerBridge.isProtectedCriticalSystemApp("android", isSystemApp = true))
        assertTrue(PermissionManagerBridge.isProtectedCriticalSystemApp("com.android.settings", isSystemApp = true))
        assertFalse(PermissionManagerBridge.isProtectedCriticalSystemApp("com.spy.cam", isSystemApp = false))
    }

    @Test
    fun `critical system apps cannot be revoked and Mode C falls back to system settings cleanly`() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val history = InMemoryHistoryStore()
        val fakeRepo = ProFakeScannerDataSource(
            shizuku = ShizukuStatusSnapshot(connectionState = ShizukuConnectionState.NOT_INSTALLED)
        )

        var executorCalled = false
        val vm = ActiPermViewModel(
            application = app,
            repository = fakeRepo,
            historyRepository = history,
            permissionExecutor = { _, _, _, _ ->
                executorCalled = true
                Result.success("ok")
            }
        )
        advanceUntilIdle()

        // 1. Critical system app revocation is blocked
        val sysUiApp = vm.uiState.value.allApps.first { it.packageName == "com.android.systemui" }
        val sysCamera = sysUiApp.permissions.first { it.rawName == "android.permission.CAMERA" }
        vm.executePermissionToggle(sysUiApp, sysCamera, grant = false)
        assertFalse(executorCalled)
        assertTrue(vm.uiState.value.permissionActionMessage?.contains("Dilindungi") == true)
        assertEquals(0, vm.uiState.value.permissionHistory.size)

        // 2. Mode C fallback on non-critical app emits OpenSystemAppDetails without error and logs
        val emittedEvents = mutableListOf<ActiPermEvent>()
        val job = launch { vm.events.collect { emittedEvents.add(it) } }

        val spyApp = vm.uiState.value.allApps.first { it.packageName == "com.spy.location" }
        val locPerm = spyApp.permissions.first { it.rawName == "android.permission.ACCESS_FINE_LOCATION" }
        vm.executePermissionToggle(spyApp, locPerm, grant = false)
        advanceUntilIdle()

        assertFalse(executorCalled)
        assertEquals(1, emittedEvents.size)
        val openEvent = emittedEvents.first() as ActiPermEvent.OpenSystemAppDetails
        assertEquals("com.spy.location", openEvent.packageName)
        assertEquals(1, vm.uiState.value.permissionHistory.size)
        assertEquals("Pengaturan Manual", vm.uiState.value.permissionHistory.first().executionModeLabel)

        job.cancel()
    }

    @Test
    fun `mode A shizuku executes single and batch revokes skipping protected apps and logs history`() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val history = InMemoryHistoryStore()
        val fakeRepo = ProFakeScannerDataSource(
            shizuku = ShizukuStatusSnapshot(
                connectionState = ShizukuConnectionState.ACTIVE_GRANTED,
                isInstalled = true,
                isBinderAlive = true,
                hasPermission = true
            )
        )
        val executedCmds = mutableListOf<Triple<PermissionExecutionMode, String, String>>()
        val vm = ActiPermViewModel(
            application = app,
            repository = fakeRepo,
            historyRepository = history,
            permissionExecutor = { mode, pkg, perm, _ ->
                executedCmds.add(Triple(mode, pkg, perm))
                Result.success("Izin dicabut via Shizuku")
            }
        )
        advanceUntilIdle()

        assertEquals(MainNavTab.DASHBOARD, vm.uiState.value.selectedMainTab)
        assertEquals(PermissionExecutionMode.SHIZUKU, vm.uiState.value.executionMode)

        // Batch 1: Cabut Semua Izin Lokasi Berbahaya (System UI must be skipped!)
        vm.openBatchRevokeProposal(BatchRevokeType.DANGEROUS_LOCATION)
        val proposal = vm.uiState.value.pendingBatchProposal
        assertNotNull(proposal)
        assertEquals(1, proposal!!.skippedProtectedCount)
        assertEquals(1, proposal.candidates.size)
        assertEquals("com.spy.location", proposal.candidates.first().app.packageName)

        vm.confirmBatchRevoke()
        advanceUntilIdle()

        assertEquals(1, executedCmds.size)
        assertEquals(PermissionExecutionMode.SHIZUKU, executedCmds.first().first)
        assertEquals("com.spy.location", executedCmds.first().second)
        assertEquals(1, vm.uiState.value.permissionHistory.size)
        assertEquals("Batch Shizuku", vm.uiState.value.permissionHistory.first().executionModeLabel)

        // Batch 2: Cabut Semua Izin Kamera Aplikasi Mencurigakan
        vm.openBatchRevokeProposal(BatchRevokeType.SUSPICIOUS_CAMERA)
        val camProposal = vm.uiState.value.pendingBatchProposal
        assertNotNull(camProposal)
        assertEquals(1, camProposal!!.candidates.size)
        assertEquals("com.suspicious.cam", camProposal.candidates.first().app.packageName)

        vm.confirmBatchRevoke()
        advanceUntilIdle()

        assertEquals(2, executedCmds.size)
        assertEquals(2, vm.uiState.value.permissionHistory.size)

        // Kelola Izin matrix filtering
        vm.selectMainTab(MainNavTab.ALL_APPS)
        vm.selectTab(AppTab.PERMISSION_MANAGER)
        vm.selectPermissionCategory(PermissionCategory.LOCATION)
        vm.selectPermissionGrantFilter(true)
        val locEntries = vm.uiState.value.visiblePermissionEntries
        assertEquals(1, locEntries.size)
        assertEquals(PermissionControlMode.RUNTIME_CONTROLLABLE, locEntries.first().controlMode)
    }

    @Test
    fun `saweria support button launches ACTION_VIEW external browser intent`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val launched = SystemIntents.openExternalUrl(app)
        assertTrue(launched)

        val startedIntent = Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals("https://saweria.co/Yaudahiya9", startedIntent.dataString)
        assertTrue(startedIntent.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    private class InMemoryHistoryStore : PermissionHistoryDataSource {
        private val entries = mutableListOf<PermissionChangeLogEntry>()

        override fun loadHistory(): List<PermissionChangeLogEntry> = entries.toList()

        override fun recordChange(entry: PermissionChangeLogEntry): List<PermissionChangeLogEntry> {
            entries.add(0, entry)
            return entries.toList()
        }

        override fun clearHistory(): List<PermissionChangeLogEntry> {
            entries.clear()
            return emptyList()
        }
    }

    private class ProFakeScannerDataSource(
        private val shizuku: ShizukuStatusSnapshot
    ) : AppScannerDataSource {
        override fun hasUsageStatsPermission(): Boolean = true

        override fun checkShizukuStatus(): ShizukuStatusSnapshot = shizuku

        override suspend fun scanInstalledApps(): ScanSnapshot {
            val apps = listOf(
                InstalledAppInfo(
                    packageName = "com.spy.location",
                    appName = "Pelacak Lokasi",
                    versionName = "1.0",
                    isSystemApp = false,
                    isEnabled = true,
                    sizeBytes = 12_000_000L,
                    lastUsedTimeMillis = 1_760_000_000_000L,
                    permissions = listOf(
                        PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true)
                    ),
                    dangerousPermissionCount = 1,
                    grantedDangerousCount = 1,
                    riskScore = 80,
                    riskLevel = RiskLevel.DANGEROUS,
                    riskReasons = listOf("Lokasi tak wajar"),
                    hasLocationPermission = true,
                    hasCameraPermission = false,
                    hasMicrophonePermission = false
                ),
                InstalledAppInfo(
                    packageName = "com.suspicious.cam",
                    appName = "Senter Kamera",
                    versionName = "1.5",
                    isSystemApp = false,
                    isEnabled = true,
                    sizeBytes = 5_000_000L,
                    lastUsedTimeMillis = 1_760_000_000_000L,
                    permissions = listOf(
                        PermissionAnalyzer.mapPermission("android.permission.CAMERA", isGranted = true)
                    ),
                    dangerousPermissionCount = 1,
                    grantedDangerousCount = 1,
                    riskScore = 45,
                    riskLevel = RiskLevel.SUSPICIOUS,
                    riskReasons = listOf("Kamera mencurigakan"),
                    hasLocationPermission = false,
                    hasCameraPermission = true,
                    hasMicrophonePermission = false
                ),
                InstalledAppInfo(
                    packageName = "com.android.systemui",
                    appName = "UI Sistem",
                    versionName = "14",
                    isSystemApp = true,
                    isEnabled = true,
                    sizeBytes = 80_000_000L,
                    lastUsedTimeMillis = 1_760_000_000_000L,
                    permissions = listOf(
                        PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true),
                        PermissionAnalyzer.mapPermission("android.permission.CAMERA", isGranted = true)
                    ),
                    dangerousPermissionCount = 2,
                    grantedDangerousCount = 2,
                    riskScore = 85,
                    riskLevel = RiskLevel.DANGEROUS,
                    riskReasons = listOf("Sistem kritis"),
                    hasLocationPermission = true,
                    hasCameraPermission = true,
                    hasMicrophonePermission = false,
                    installStatus = AppInstallStatus.SYSTEM,
                    isProtectedCriticalSystem = true
                )
            )
            return ScanSnapshot(
                apps = apps,
                hasUsageAccess = true,
                scannedAtMillis = 1_760_000_000_000L,
                shizukuStatus = shizuku
            )
        }
    }
}
