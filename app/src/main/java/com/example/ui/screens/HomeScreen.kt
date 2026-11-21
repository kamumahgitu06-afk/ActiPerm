package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.HistoryToggleOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.PermissionManagerBridge
import com.example.model.AppTab
import com.example.model.BatchRevokeType
import com.example.model.DeviceFilterOption
import com.example.model.MainNavTab
import com.example.model.PermissionCategory
import com.example.model.PermissionExecutionMode
import com.example.model.RiskLevel
import com.example.model.SensorFilterType
import com.example.model.ShizukuConnectionState
import com.example.ui.ActiPermUiState
import com.example.ui.PermissionMatrixEntry
import com.example.ui.components.ActiPermBottomNavBar
import com.example.ui.components.BatchActionCard
import com.example.ui.components.BatchConfirmationDialog
import com.example.ui.components.BulkDisableQueueDialog
import com.example.ui.components.BulkSelectionBottomBar
import com.example.ui.components.DownloadedAppCard
import com.example.ui.components.HistoryEmptyState
import com.example.ui.components.HistoryHeaderCard
import com.example.ui.components.HistoryLogCard
import com.example.ui.components.PermissionControlDialog
import com.example.ui.components.PermissionManagerHeaderPanel
import com.example.ui.components.PermissionMatrixRowCard
import com.example.ui.components.SaweriaSupportButton
import com.example.ui.components.SecurityDashboardCard
import com.example.ui.components.ShizukuStatusSection
import com.example.ui.components.SupportDeveloperCard
import com.example.ui.components.SystemAppCard
import com.example.ui.components.riskColor
import com.example.ui.components.riskContainerColor
import com.example.ui.theme.AmberSuspicious
import com.example.ui.theme.AmberSuspiciousContainer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ReportExporter
import com.example.util.SystemIntents
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    uiState: ActiPermUiState,
    uninstallLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    onRescan: () -> Unit,
    onSelectTab: (AppTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleRiskFilter: (RiskLevel?) -> Unit,
    onSelectDeviceFilter: (DeviceFilterOption) -> Unit,
    onToggleSensorTrio: () -> Unit,
    onToggleSpecificSensor: (SensorFilterType) -> Unit,
    onToggleOnlyUnusedSystem: () -> Unit,
    onResetFilters: () -> Unit,
    onOpenAppDetail: (String) -> Unit,
    onToggleSystemAppSelection: (String) -> Unit,
    onSelectAllVisibleSystem: () -> Unit,
    onClearSystemSelection: () -> Unit,
    onStartBulkDisable: () -> Unit,
    onBulkOpenNow: () -> Unit,
    onBulkSkip: () -> Unit,
    onBulkCancel: () -> Unit,
    onSelectMainTab: (MainNavTab) -> Unit = {},
    onCheckShizuku: () -> Unit = {},
    onRequestShizukuPermission: () -> Unit = {},
    onTriggerBatchRevoke: (BatchRevokeType) -> Unit = {},
    onConfirmBatchRevoke: () -> Unit = {},
    onDismissBatchRevoke: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onSelectPermissionCategory: (PermissionCategory?) -> Unit = {},
    onSelectPermissionGrantFilter: (Boolean?) -> Unit = {},
    onToggleIncludeSystemInPermManager: () -> Unit = {},
    onRootTogglePermission: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onClearPermissionActionMessage: () -> Unit = {}
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val successMsg = stringResource(R.string.export_success_msg)
    val errorMsg = stringResource(R.string.export_error_msg)
    val adbCopiedMsg = stringResource(R.string.adb_copied_msg)
    val browserErrorMsg = stringResource(R.string.browser_open_error)
    var selectedMatrixEntry by remember { mutableStateOf<PermissionMatrixEntry?>(null) }

    val openSaweriaExternal: () -> Unit = {
        val opened = SystemIntents.openExternalUrl(context, SystemIntents.SAWERIA_SUPPORT_URL)
        if (!opened) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(browserErrorMsg)
            }
        }
    }

    LaunchedEffect(uiState.permissionActionMessage) {
        val msg = uiState.permissionActionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onClearPermissionActionMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val reportText = ReportExporter.buildFullReport(uiState)
                val result = ReportExporter.writeTextToUri(context, uri, reportText)
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) successMsg else errorMsg
                )
            }
        }
    }

    val visibleApps = uiState.visibleApps
    val visiblePermissionEntries = uiState.visiblePermissionEntries
    val showBulkBar = uiState.selectedMainTab == MainNavTab.ALL_APPS &&
        uiState.selectedTab == AppTab.SYSTEM &&
        uiState.selectedSystemPackages.isNotEmpty()
    val canDirectControl = uiState.executionMode != PermissionExecutionMode.MANUAL_ADB

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SlateSurfaceHigh,
                    contentColor = TextPrimary
                )
            }
        },
        topBar = {
            ActiPermTopBar(
                isScanning = uiState.isScanning,
                onRescan = onRescan
            )
        },
        bottomBar = {
            Column {
                AnimatedVisibility(visible = showBulkBar) {
                    BulkSelectionBottomBar(
                        selectedCount = uiState.selectedSystemPackages.size,
                        onClearSelection = onClearSystemSelection,
                        onStartBulkDisable = onStartBulkDisable
                    )
                }
                ActiPermBottomNavBar(
                    selectedTab = uiState.selectedMainTab,
                    historyCount = uiState.permissionHistory.size,
                    shizukuActive = uiState.shizukuStatus.connectionState == ShizukuConnectionState.ACTIVE_GRANTED,
                    onSelectTab = onSelectMainTab
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = CyberCyan)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.scanning_apps),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .testTag("apps_lazy_column"),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = if (showBulkBar) 96.dp else 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (uiState.selectedMainTab) {
                        MainNavTab.DASHBOARD -> {
                            item(key = "dashboard") {
                                SecurityDashboardCard(
                                    uiState = uiState,
                                    onRiskPillClick = { level ->
                                        onToggleRiskFilter(level)
                                        onSelectMainTab(MainNavTab.ALL_APPS)
                                    },
                                    onExportClick = {
                                        exportLauncher.launch(ReportExporter.defaultFullReportFileName())
                                    }
                                )
                            }

                            item(key = "support_developer_dashboard") {
                                SupportDeveloperCard(
                                    onSupportClick = openSaweriaExternal,
                                    buttonTag = "saweria_dashboard_btn"
                                )
                            }

                            item(key = "batch_actions") {
                                BatchActionCard(
                                    executionMode = uiState.executionMode,
                                    onTriggerBatch = onTriggerBatchRevoke
                                )
                            }

                            item(key = "dashboard_quick_actions") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { onSelectMainTab(MainNavTab.ALL_APPS) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("goto_all_apps_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyberCyan,
                                            contentColor = ObsidianBg
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(
                                            text = "Semua Aplikasi (${uiState.totalAppsCount})",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    Button(
                                        onClick = { onSelectMainTab(MainNavTab.SHIZUKU) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("goto_shizuku_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SlateSurface,
                                            contentColor = CyberCyan
                                        ),
                                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(
                                            text = "Status Shizuku",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        MainNavTab.ALL_APPS -> {
                            // 1. Ringkasan Dasbor
                            item(key = "dashboard") {
                                SecurityDashboardCard(
                                    uiState = uiState,
                                    onRiskPillClick = { level -> onToggleRiskFilter(level) },
                                    onExportClick = {
                                        exportLauncher.launch(ReportExporter.defaultFullReportFileName())
                                    }
                                )
                            }

                            // 2. Kolom Pencarian
                            item(key = "search_bar") {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_input"),
                                    placeholder = {
                                        Text(
                                            text = stringResource(R.string.search_hint),
                                            color = TextMuted
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = CyberCyan
                                        )
                                    },
                                    trailingIcon = {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { onSearchQueryChange("") }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Hapus pencarian",
                                                    tint = TextSecondary
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SlateSurface,
                                        unfocusedContainerColor = SlateSurface,
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = SlateBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = CyberCyan
                                    )
                                )
                            }

                            // 3. Sub-Tab: Aplikasi Unduhan, Aplikasi Bawaan & Kelola Izin
                            item(key = "tabs") {
                                AppCategoryTabs(
                                    selectedTab = uiState.selectedTab,
                                    downloadedCount = uiState.downloadedAppsCount,
                                    systemCount = uiState.systemAppsCount,
                                    onSelectTab = onSelectTab
                                )
                            }

                            // 4. Filter & Daftar sesuai sub-tab aktif
                            if (uiState.selectedTab != AppTab.PERMISSION_MANAGER) {
                                item(key = "filter_chips") {
                                    FilterChipsBar(
                                        uiState = uiState,
                                        onToggleSensorTrio = onToggleSensorTrio,
                                        onToggleSpecificSensor = onToggleSpecificSensor,
                                        onToggleRiskFilter = onToggleRiskFilter
                                    )
                                }

                                item(key = "device_filter_chips") {
                                    DeviceFilterChipsBar(
                                        selectedOption = uiState.deviceFilter,
                                        onSelectDeviceFilter = onSelectDeviceFilter
                                    )
                                }
                            }

                            // 5. Kontrol khusus Tab Aplikasi Bawaan atau Kelola Izin
                            if (uiState.selectedTab == AppTab.SYSTEM) {
                                if (!uiState.hasUsageAccess) {
                                    item(key = "usage_access_banner") {
                                        UsageAccessBannerCard(
                                            onGrantClick = {
                                                SystemIntents.openUsageAccessSettings(context)
                                            }
                                        )
                                    }
                                }

                                item(key = "unused_system_filter_and_bulk") {
                                    SystemToolsPanel(
                                        onlyUnused = uiState.onlyUnusedSystemApps,
                                        hasUsageAccess = uiState.hasUsageAccess,
                                        visibleEnabledCount = visibleApps.count { it.isEnabled },
                                        selectedCount = uiState.selectedSystemPackages.size,
                                        onToggleOnlyUnused = onToggleOnlyUnusedSystem,
                                        onSelectAllVisible = onSelectAllVisibleSystem,
                                        onClearSelection = onClearSystemSelection
                                    )
                                }
                            } else if (uiState.selectedTab == AppTab.PERMISSION_MANAGER) {
                                item(key = "permission_manager_header") {
                                    PermissionManagerHeaderPanel(
                                        uiState = uiState,
                                        onSelectCategory = onSelectPermissionCategory,
                                        onSelectGrantFilter = onSelectPermissionGrantFilter,
                                        onToggleIncludeSystem = onToggleIncludeSystemInPermManager
                                    )
                                }
                            }

                            // Jumlah hasil terfilter
                            item(key = "results_count") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val resultCountLabel = if (uiState.selectedTab == AppTab.PERMISSION_MANAGER) {
                                        "Menampilkan ${visiblePermissionEntries.size} izin aplikasi"
                                    } else {
                                        "Menampilkan ${visibleApps.size} aplikasi"
                                    }
                                    Text(
                                        text = resultCountLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextSecondary
                                    )
                                    val hasActiveFilters = uiState.searchQuery.isNotBlank() ||
                                        uiState.riskFilter != null ||
                                        uiState.deviceFilter != DeviceFilterOption.ALL ||
                                        uiState.sensorTrioFilterActive ||
                                        uiState.selectedSensors.isNotEmpty() ||
                                        uiState.permissionGrantFilter != null ||
                                        (uiState.selectedTab == AppTab.SYSTEM && uiState.onlyUnusedSystemApps)
                                    if (hasActiveFilters) {
                                        TextButton(
                                            onClick = onResetFilters,
                                            modifier = Modifier.testTag("reset_filters_inline_btn")
                                        ) {
                                            Text(
                                                text = stringResource(R.string.reset_filters),
                                                color = CyberCyan
                                            )
                                        }
                                    }
                                }
                            }

                            // 6. Daftar Aplikasi / Daftar Kelola Izin atau Empty State
                            if (uiState.selectedTab == AppTab.PERMISSION_MANAGER) {
                                if (visiblePermissionEntries.isEmpty()) {
                                    item(key = "empty_state_perm_manager") {
                                        EmptyAppsState(
                                            onResetFilters = onResetFilters
                                        )
                                    }
                                } else {
                                    items(
                                        items = visiblePermissionEntries,
                                        key = { "${it.app.packageName}:${it.permission.rawName}" }
                                    ) { entry ->
                                        PermissionMatrixRowCard(
                                            entry = entry,
                                            isRootAvailable = canDirectControl,
                                            onOpenSettingsForPermission = {
                                                SystemIntents.openApplicationDetailsSettings(
                                                    context = context,
                                                    packageName = entry.app.packageName
                                                )
                                            },
                                            onOpenControlSheet = { selectedMatrixEntry = entry },
                                            onOpenAppDetail = { onOpenAppDetail(entry.app.packageName) },
                                            onRootToggle = { grant ->
                                                onRootTogglePermission(
                                                    entry.app.packageName,
                                                    entry.permission.rawName,
                                                    grant
                                                )
                                            }
                                        )
                                    }
                                }
                            } else if (visibleApps.isEmpty()) {
                                item(key = "empty_state") {
                                    EmptyAppsState(
                                        onResetFilters = onResetFilters
                                    )
                                }
                            } else {
                                items(
                                    items = visibleApps,
                                    key = { it.packageName }
                                ) { app ->
                                    if (uiState.selectedTab == AppTab.DOWNLOADED) {
                                        DownloadedAppCard(
                                            app = app,
                                            isSelfApp = app.packageName == context.packageName,
                                            onCardClick = { onOpenAppDetail(app.packageName) },
                                            onUninstallClick = {
                                                uninstallLauncher.launch(
                                                    SystemIntents.buildUninstallIntent(app.packageName)
                                                )
                                            }
                                        )
                                    } else {
                                        SystemAppCard(
                                            app = app,
                                            isChecked = app.packageName in uiState.selectedSystemPackages,
                                            onToggleCheck = { onToggleSystemAppSelection(app.packageName) },
                                            onCardClick = { onOpenAppDetail(app.packageName) },
                                            onDisableClick = {
                                                SystemIntents.openApplicationDetailsSettings(
                                                    context = context,
                                                    packageName = app.packageName
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        MainNavTab.HISTORY -> {
                            item(key = "history_header") {
                                HistoryHeaderCard(
                                    historyCount = uiState.permissionHistory.size,
                                    onClearHistory = onClearHistory
                                )
                            }

                            if (uiState.permissionHistory.isEmpty()) {
                                item(key = "history_empty") {
                                    HistoryEmptyState()
                                }
                            } else {
                                items(
                                    items = uiState.permissionHistory,
                                    key = { it.id }
                                ) { logEntry ->
                                    HistoryLogCard(entry = logEntry)
                                }
                            }
                        }

                        MainNavTab.SHIZUKU -> {
                            item(key = "shizuku_section") {
                                ShizukuStatusSection(
                                    shizukuStatus = uiState.shizukuStatus,
                                    executionMode = uiState.executionMode,
                                    onCheckShizuku = onCheckShizuku,
                                    onRequestPermission = onRequestShizukuPermission,
                                    onOpenShizukuApp = {
                                        SystemIntents.launchInstalledApp(
                                            context = context,
                                            packageName = PermissionManagerBridge.SHIZUKU_PACKAGE_NAME
                                        )
                                    },
                                    onCopyCommand = { cmd ->
                                        clipboardManager.setText(AnnotatedString(cmd))
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(adbCopiedMsg)
                                        }
                                    },
                                    onSupportClick = openSaweriaExternal
                                )
                            }
                        }
                    }

                    // 7. Tentang / Footer
                    item(key = "about_footer") {
                        AboutFooterCard(onSupportClick = openSaweriaExternal)
                    }
                }
            }
        }

        selectedMatrixEntry?.let { entry ->
            val isProtected = entry.app.isProtectedCriticalSystem ||
                PermissionManagerBridge.isProtectedCriticalSystemApp(
                    entry.app.packageName,
                    entry.app.isSystemApp,
                    entry.app.installStatus
                )
            PermissionControlDialog(
                app = entry.app,
                permission = entry.permission,
                isRootAvailable = canDirectControl,
                isProtectedCriticalSystem = isProtected,
                executionMode = uiState.executionMode,
                onDismiss = { selectedMatrixEntry = null },
                onOpenSystemSettings = {
                    selectedMatrixEntry = null
                    SystemIntents.openApplicationDetailsSettings(
                        context = context,
                        packageName = entry.app.packageName
                    )
                },
                onRootGrantRevoke = { grant ->
                    selectedMatrixEntry = null
                    onRootTogglePermission(entry.app.packageName, entry.permission.rawName, grant)
                },
                onCopyAdbCommand = { cmd ->
                    selectedMatrixEntry = null
                    clipboardManager.setText(AnnotatedString(cmd))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(adbCopiedMsg)
                    }
                }
            )
        }

        uiState.pendingBatchProposal?.let { proposal ->
            BatchConfirmationDialog(
                proposal = proposal,
                executionMode = uiState.executionMode,
                isExecuting = uiState.isExecutingBatch,
                onDismiss = onDismissBatchRevoke,
                onConfirm = onConfirmBatchRevoke,
                onCopyBatchAdb = { script ->
                    onDismissBatchRevoke()
                    clipboardManager.setText(AnnotatedString(script))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(adbCopiedMsg)
                    }
                }
            )
        }

        val queue = uiState.bulkDisableQueue
        if (queue != null) {
            BulkDisableQueueDialog(
                queue = queue,
                autoAdvanceActive = uiState.autoAdvanceCountdownActive,
                onOpenNow = onBulkOpenNow,
                onSkip = onBulkSkip,
                onCancel = onBulkCancel
            )
        }
    }
}

@Composable
private fun ActiPermTopBar(
    isScanning: Boolean,
    onRescan: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanSpinAngle"
    )

    Surface(
        color = ObsidianBg,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_actiperm_logo),
                    contentDescription = "Logo ActiPerm",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isScanning, onClick = onRescan)
                    .testTag("rescan_button"),
                shape = RoundedCornerShape(12.dp),
                color = CyberCyanContainer,
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isScanning) rotation else 0f)
                    )
                    Text(
                        text = if (isScanning) "Memindai…" else stringResource(R.string.action_rescan),
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCategoryTabs(
    selectedTab: AppTab,
    downloadedCount: Int,
    systemCount: Int,
    onSelectTab: (AppTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabPillButton(
                title = "Unduhan ($downloadedCount)",
                selected = selectedTab == AppTab.DOWNLOADED,
                onClick = { onSelectTab(AppTab.DOWNLOADED) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_downloaded")
            )
            TabPillButton(
                title = "Bawaan ($systemCount)",
                selected = selectedTab == AppTab.SYSTEM,
                onClick = { onSelectTab(AppTab.SYSTEM) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_system")
            )
            TabPillButton(
                title = stringResource(R.string.tab_permission_manager),
                selected = selectedTab == AppTab.PERMISSION_MANAGER,
                onClick = { onSelectTab(AppTab.PERMISSION_MANAGER) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("tab_permission_manager")
            )
        }
    }
}

@Composable
private fun TabPillButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) CyberCyan else SlateSurface,
        border = if (selected) null else BorderStroke(0.dp, SlateSurface)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    lineHeight = 14.sp
                ),
                color = if (selected) ObsidianBg else TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterChipsBar(
    uiState: ActiPermUiState,
    onToggleSensorTrio: () -> Unit,
    onToggleSpecificSensor: (SensorFilterType) -> Unit,
    onToggleRiskFilter: (RiskLevel?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = uiState.sensorTrioFilterActive,
            onClick = onToggleSensorTrio,
            modifier = Modifier.testTag("filter_sensor_trio"),
            label = {
                Text(stringResource(R.string.filter_sensor_trio))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Sensors,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = SlateSurface,
                labelColor = TextPrimary,
                iconColor = CyberCyan,
                selectedContainerColor = CyberCyanContainer,
                selectedLabelColor = CyberCyan,
                selectedLeadingIconColor = CyberCyan
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = uiState.sensorTrioFilterActive,
                borderColor = SlateBorder,
                selectedBorderColor = CyberCyan
            )
        )

        SensorChip(
            label = stringResource(R.string.filter_location),
            icon = Icons.Rounded.LocationOn,
            selected = SensorFilterType.LOCATION in uiState.selectedSensors,
            onClick = { onToggleSpecificSensor(SensorFilterType.LOCATION) },
            testTag = "filter_location"
        )
        SensorChip(
            label = stringResource(R.string.filter_camera),
            icon = Icons.Rounded.CameraAlt,
            selected = SensorFilterType.CAMERA in uiState.selectedSensors,
            onClick = { onToggleSpecificSensor(SensorFilterType.CAMERA) },
            testTag = "filter_camera"
        )
        SensorChip(
            label = stringResource(R.string.filter_mic),
            icon = Icons.Rounded.Mic,
            selected = SensorFilterType.MICROPHONE in uiState.selectedSensors,
            onClick = { onToggleSpecificSensor(SensorFilterType.MICROPHONE) },
            testTag = "filter_mic"
        )

        for (level in RiskLevel.entries) {
            val selected = uiState.riskFilter == level
            val accent = riskColor(level)
            val bg = riskContainerColor(level)
            FilterChip(
                selected = selected,
                onClick = { onToggleRiskFilter(level) },
                modifier = Modifier.testTag("filter_risk_${level.name.lowercase()}"),
                label = { Text(level.label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SlateSurface,
                    labelColor = TextSecondary,
                    selectedContainerColor = bg,
                    selectedLabelColor = accent
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = SlateBorder,
                    selectedBorderColor = accent
                )
            )
        }
    }
}

@Composable
private fun DeviceFilterChipsBar(
    selectedOption: DeviceFilterOption,
    onSelectDeviceFilter: (DeviceFilterOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeviceFilterOption.entries.forEach { option ->
            val selected = selectedOption == option
            FilterChip(
                selected = selected,
                onClick = { onSelectDeviceFilter(option) },
                modifier = Modifier.testTag("filter_device_${option.name.lowercase()}"),
                label = { Text(option.label) },
                leadingIcon = if (option == DeviceFilterOption.ALL) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Smartphone,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SlateSurface,
                    labelColor = TextSecondary,
                    iconColor = CyberCyan,
                    selectedContainerColor = CyberCyanContainer,
                    selectedLabelColor = CyberCyan,
                    selectedLeadingIconColor = CyberCyan
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = SlateBorder,
                    selectedBorderColor = CyberCyan
                )
            )
        }
    }
}

@Composable
private fun SensorChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(testTag),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SlateSurface,
            labelColor = TextSecondary,
            iconColor = TextSecondary,
            selectedContainerColor = CyberCyanContainer,
            selectedLabelColor = CyberCyan,
            selectedLeadingIconColor = CyberCyan
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = SlateBorder,
            selectedBorderColor = CyberCyan
        )
    )
}

@Composable
private fun UsageAccessBannerCard(
    onGrantClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("usage_access_banner"),
        shape = RoundedCornerShape(16.dp),
        color = AmberSuspiciousContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, AmberSuspicious.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AdminPanelSettings,
                    contentDescription = null,
                    tint = AmberSuspicious,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.usage_access_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AmberSuspicious
                )
            }
            Text(
                text = stringResource(R.string.usage_access_banner_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
            Button(
                onClick = onGrantClick,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("grant_usage_access_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberSuspicious,
                    contentColor = ObsidianBg
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.usage_access_banner_btn),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun SystemToolsPanel(
    onlyUnused: Boolean,
    hasUsageAccess: Boolean,
    visibleEnabledCount: Int,
    selectedCount: Int,
    onToggleOnlyUnused: () -> Unit,
    onSelectAllVisible: () -> Unit,
    onClearSelection: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Filter: Hanya Tampilkan Aplikasi Bawaan Yang Tidak Pernah Dipakai
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleOnlyUnused)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .testTag("filter_unused_system_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (onlyUnused) AmberSuspiciousContainer else SlateSurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HistoryToggleOff,
                            contentDescription = null,
                            tint = if (onlyUnused) AmberSuspicious else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.filter_unused_system),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = if (hasUsageAccess) {
                                "Bersihkan aplikasi bawaan yang menganggur"
                            } else {
                                "Aktifkan Akses Penggunaan di atas agar akurat"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = onlyUnused,
                    onCheckedChange = { onToggleOnlyUnused() },
                    modifier = Modifier.testTag("filter_unused_system_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianBg,
                        checkedTrackColor = AmberSuspicious,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SlateSurfaceHigh
                    )
                )
            }

            // Checklist actions untuk bulk disable
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckBox,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Centang beberapa aplikasi untuk nonaktifkan cepat",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = visibleEnabledCount > 0, onClick = onSelectAllVisible)
                        .testTag("select_all_system_btn"),
                    shape = RoundedCornerShape(10.dp),
                    color = SlateSurfaceHigh,
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pilih Semua Aktif ($visibleEnabledCount)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (selectedCount > 0) {
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onClearSelection)
                            .testTag("clear_all_system_btn"),
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSurfaceHigh,
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Batal ($selectedCount)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAppsState(
    onResetFilters: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = stringResource(R.string.empty_filtered_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.empty_filtered_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onResetFilters,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("reset_filters_empty_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = ObsidianBg
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.reset_filters),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun AboutFooterCard(
    onSupportClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("about_footer_card"),
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.about_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = CyberCyan
            )
            Text(
                text = stringResource(R.string.about_footer_credit),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary
            )
            Text(
                text = "Aplikasi cek izin dan bersihkan HP",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            SaweriaSupportButton(
                onClick = onSupportClick,
                compact = true,
                modifier = Modifier.testTag("saweria_footer_btn")
            )
        }
    }
}
