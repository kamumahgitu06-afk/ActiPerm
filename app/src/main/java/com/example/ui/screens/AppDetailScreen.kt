package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.PermissionAnalyzer
import com.example.domain.PermissionManagerBridge
import com.example.model.AppInstallStatus
import com.example.model.AppPermissionDetail
import com.example.model.InstalledAppInfo
import com.example.model.PermissionControlMode
import com.example.model.PermissionExecutionMode
import com.example.ui.components.AppIconImage
import com.example.ui.components.PermissionControlDialog
import com.example.ui.components.RiskBadge
import com.example.ui.components.RiskGaugeCanvas
import com.example.ui.components.riskColor
import com.example.ui.theme.AmberSuspicious
import com.example.ui.theme.AmberSuspiciousContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonDangerContainer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanContainer
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.EmeraldSafeContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormattingUtils
import com.example.util.ReportExporter
import com.example.util.SystemIntents
import kotlinx.coroutines.launch

private enum class DetailPermFilter(val label: String) {
    ALL("Semua"),
    DANGEROUS("Berbahaya"),
    GRANTED("Diizinkan")
}

@Composable
fun AppDetailScreen(
    app: InstalledAppInfo?,
    uninstallLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    isRootAvailable: Boolean = false,
    executionMode: PermissionExecutionMode = if (isRootAvailable) {
        PermissionExecutionMode.ROOT
    } else {
        PermissionExecutionMode.MANUAL_ADB
    },
    permissionActionMessage: String? = null,
    onExecutePermissionToggle: ((InstalledAppInfo, AppPermissionDetail, Boolean) -> Unit)? = null,
    onRootTogglePermission: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onClearPermissionActionMessage: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val successMsg = stringResource(R.string.export_success_msg)
    val errorMsg = stringResource(R.string.export_error_msg)
    val reportCopiedMsg = stringResource(R.string.report_copied_msg)
    val adbCopiedMsg = stringResource(R.string.adb_copied_msg)
    var permFilter by remember { mutableStateOf(DetailPermFilter.ALL) }
    var showReportDialog by remember { mutableStateOf(false) }
    var controlledPermission by remember { mutableStateOf<AppPermissionDetail?>(null) }

    LaunchedEffect(permissionActionMessage) {
        if (permissionActionMessage != null) {
            snackbarHostState.showSnackbar(permissionActionMessage)
            onClearPermissionActionMessage()
        }
    }

    val exportSingleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && app != null) {
            coroutineScope.launch {
                val reportText = ReportExporter.buildSingleAppReport(app)
                val result = ReportExporter.writeTextToUri(context, uri, reportText)
                snackbarHostState.showSnackbar(
                    if (result.isSuccess) successMsg else errorMsg
                )
            }
        }
    }

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
            Surface(
                color = ObsidianBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back),
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = app?.appName ?: "Detail Aplikasi",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (app != null) {
                        Surface(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    exportSingleLauncher.launch(
                                        ReportExporter.defaultSingleAppReportFileName(app)
                                    )
                                }
                                .testTag("detail_export_txt_btn"),
                            shape = RoundedCornerShape(10.dp),
                            color = SlateSurfaceHigh,
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = stringResource(R.string.action_export_txt),
                                    tint = CyberCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = stringResource(R.string.action_export_txt),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (app == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Aplikasi tidak lagi terinstal di perangkat.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = ObsidianBg
                        )
                    ) {
                        Text("Kembali ke Daftar")
                    }
                }
            }
            return@Scaffold
        }

        val displayedPermissions = remember(app.permissions, permFilter) {
            when (permFilter) {
                DetailPermFilter.ALL -> app.permissions
                DetailPermFilter.DANGEROUS -> app.permissions.filter { it.isDangerous }
                DetailPermFilter.GRANTED -> app.permissions.filter { it.isGranted }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .testTag("detail_permissions_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header info aplikasi
                item(key = "hero") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = SlateSurface,
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                AppIconImage(
                                    packageName = app.packageName,
                                    contentDescription = "Ikon ${app.appName}",
                                    dimmed = !app.isEnabled,
                                    modifier = Modifier.size(62.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFamily
                                        ),
                                        color = TextMuted
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val statusColor = when (app.installStatus) {
                                            AppInstallStatus.SYSTEM -> AmberSuspicious
                                            AppInstallStatus.BUILT_IN -> EmeraldSafe
                                            AppInstallStatus.DOWNLOADED -> CyberCyan
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SlateSurfaceHigh
                                        ) {
                                            Text(
                                                text = "Status: ${app.installStatus.label}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = statusColor,
                                                modifier = Modifier
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    .testTag("detail_install_status")
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SlateSurfaceHigh
                                        ) {
                                            Text(
                                                text = app.functionalCategory.label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        if (!app.isEnabled) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = SlateSurfaceHigh
                                            ) {
                                                Text(
                                                    text = "Nonaktif",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = TextMuted,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SlateSurfaceHigh)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetaColumn("Versi", "v${app.versionName}", Modifier.weight(1f))
                                MetaColumn("Ukuran", FormattingUtils.formatSize(app.sizeBytes), Modifier.weight(1f))
                                MetaColumn(
                                    "Terakhir Dipakai",
                                    FormattingUtils.formatLastUsed(app.lastUsedTimeMillis),
                                    Modifier.weight(1.3f)
                                )
                            }

                            val deviceBrandText = app.detectedDeviceBrandName.ifBlank {
                                PermissionAnalyzer.detectDeviceBrandDisplayName()
                            }
                            val androidVerText = app.androidVersionLabel.ifBlank {
                                PermissionAnalyzer.formatAndroidVersionLabel()
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SlateSurfaceHigh)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetaColumn(
                                    label = stringResource(R.string.detail_detected_device),
                                    value = deviceBrandText,
                                    modifier = Modifier.weight(1f)
                                )
                                MetaColumn(
                                    label = stringResource(R.string.detail_android_version),
                                    value = androidVerText,
                                    modifier = Modifier.weight(1f)
                                )
                                MetaColumn(
                                    label = stringResource(R.string.detail_install_status),
                                    value = app.installStatus.label,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SlateSurfaceHigh)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetaColumn(
                                    label = stringResource(R.string.detail_target_sdk),
                                    value = "API ${app.targetSdkVersion}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetaColumn(
                                    label = stringResource(R.string.detail_installer),
                                    value = app.installerLabel.ifBlank { "Sistem / Lokal" },
                                    modifier = Modifier.weight(1.2f)
                                )
                                MetaColumn(
                                    label = "Dukungan OS",
                                    value = "Android 7.0+",
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                        }
                    }
                }

                // Skor Bahaya + Alasan
                item(key = "risk_card") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_risk_card"),
                        shape = RoundedCornerShape(20.dp),
                        color = SlateSurface,
                        border = BorderStroke(1.dp, riskColor(app.riskLevel).copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RiskGaugeCanvas(
                                    score = app.riskScore,
                                    riskLevel = app.riskLevel
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.detail_risk_score_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                    RiskBadge(riskLevel = app.riskLevel, riskScore = app.riskScore)
                                    if (app.hasExpectedPermissionsForFunction) {
                                        Surface(
                                            modifier = Modifier.testTag("expected_for_function_badge"),
                                            shape = RoundedCornerShape(999.dp),
                                            color = EmeraldSafeContainer,
                                            border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = null,
                                                    tint = EmeraldSafe,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.label_expected_for_function),
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = EmeraldSafe
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (app.unexpectedDangerousCount == 0 && app.hasExpectedPermissionsForFunction) {
                                            "Diminta: ${app.dangerousPermissionCount} sensitif • Diizinkan: ${app.grantedDangerousCount} (sesuai fungsi)"
                                        } else {
                                            "Tak wajar: ${app.unexpectedDangerousCount} • Diminta: ${app.dangerousPermissionCount} • Diizinkan: ${app.grantedDangerousCount}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.detail_risk_reasons_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                app.riskReasons.forEach { reason ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 6.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(riskColor(app.riskLevel))
                                        )
                                        Text(
                                            text = reason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tombol Aksi Sistem Asli: "Buka Pengaturan Izin" + "Buka Aplikasi" + "Hapus" / "Nonaktifkan"
                item(key = "actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isProtectedCritical = app.isProtectedCriticalSystem ||
                            PermissionManagerBridge.isProtectedCriticalSystemApp(
                                app.packageName,
                                app.isSystemApp,
                                app.installStatus
                            )

                        if (isProtectedCritical) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("protected_system_banner"),
                                shape = RoundedCornerShape(14.dp),
                                color = EmeraldSafeContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = EmeraldSafe,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Aplikasi Sistem - Dilindungi",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldSafe
                                        )
                                        Text(
                                            text = "Aplikasi sistem kritis ini dilindungi. Pencabutan izin otomatis dinonaktifkan untuk mencegah HP bootloop.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = SlateSurface,
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Mode Kontrol Izin:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = executionMode.label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (executionMode == PermissionExecutionMode.MANUAL_ADB) AmberSuspicious else EmeraldSafe
                                )
                            }
                        }

                        Button(
                            onClick = {
                                SystemIntents.openApplicationDetailsSettings(context, app.packageName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("open_permission_settings_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_open_permission_settings),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        val canLaunchApp = remember(app.packageName) {
                            app.packageName != context.packageName &&
                                app.isEnabled &&
                                context.packageManager.getLaunchIntentForPackage(app.packageName) != null
                        }
                        if (canLaunchApp) {
                            OutlinedButton(
                                onClick = {
                                    SystemIntents.launchInstalledApp(context, app.packageName)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_launch_app_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SlateSurface,
                                    contentColor = EmeraldSafe
                                ),
                                border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_launch_app),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        if (app.isSystemApp) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AmberSuspiciousContainer.copy(alpha = 0.45f))
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = AmberSuspicious,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.system_app_cannot_uninstall),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberSuspicious
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    SystemIntents.openApplicationDetailsSettings(context, app.packageName)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_disable_system_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AmberSuspiciousContainer.copy(alpha = 0.4f),
                                    contentColor = AmberSuspicious
                                ),
                                border = BorderStroke(1.dp, AmberSuspicious.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${stringResource(R.string.action_disable)} di Pengaturan Sistem",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        } else if (app.packageName != context.packageName) {
                            OutlinedButton(
                                onClick = {
                                    uninstallLauncher.launch(
                                        SystemIntents.buildUninstallIntent(app.packageName)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_uninstall_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = CrimsonDangerContainer.copy(alpha = 0.45f),
                                    contentColor = CrimsonDanger
                                ),
                                border = BorderStroke(1.dp, CrimsonDanger.copy(alpha = 0.55f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${stringResource(R.string.action_uninstall)} Aplikasi Ini",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // Tombol Laporkan Salah Deteksi (selalu tersedia di semua aplikasi)
                        OutlinedButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("report_false_detection_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SlateSurface,
                                contentColor = CyberCyan
                            ),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_report_false_detection),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                // Header daftar izin + filter mini
                item(key = "perm_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${stringResource(R.string.detail_requested_permissions)} (${app.permissions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DetailPermFilter.entries.forEach { filter ->
                                val selected = permFilter == filter
                                val count = when (filter) {
                                    DetailPermFilter.ALL -> app.permissions.size
                                    DetailPermFilter.DANGEROUS -> app.dangerousPermissionCount
                                    DetailPermFilter.GRANTED -> app.permissions.count { it.isGranted }
                                }
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { permFilter = filter }
                                        .testTag("detail_perm_filter_${filter.name.lowercase()}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) CyberCyanContainer else SlateSurface,
                                    border = BorderStroke(1.dp, if (selected) CyberCyan else SlateBorder)
                                ) {
                                    Text(
                                        text = "${filter.label} ($count)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) CyberCyan else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (displayedPermissions.isEmpty()) {
                    item(key = "no_perms") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = SlateSurface,
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = EmeraldSafe
                                )
                                Text(
                                    text = stringResource(R.string.detail_no_permissions),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = displayedPermissions,
                        key = { it.rawName }
                    ) { perm ->
                        val controlMode = PermissionManagerBridge.classifyControlMode(perm)
                        val isControllable = controlMode != PermissionControlMode.INSTALL_TIME_INFO_ONLY
                        val isProtectedCritical = app.isProtectedCriticalSystem ||
                            PermissionManagerBridge.isProtectedCriticalSystemApp(
                                app.packageName,
                                app.isSystemApp,
                                app.installStatus
                            )
                        PermissionItemCard(
                            perm = perm,
                            isControllable = isControllable,
                            executionMode = executionMode,
                            isProtectedCriticalSystem = isProtectedCritical,
                            onDirectToggleClick = if (isControllable && !isProtectedCritical) {
                                {
                                    if (controlMode == PermissionControlMode.SPECIAL_SETTINGS_CONTROLLABLE) {
                                        SystemIntents.openSpecificPermissionSettings(
                                            context = context,
                                            packageName = app.packageName,
                                            rawPermissionName = perm.rawName
                                        )
                                    } else if (executionMode == PermissionExecutionMode.MANUAL_ADB) {
                                        SystemIntents.openApplicationDetailsSettings(context, app.packageName)
                                    } else if (onExecutePermissionToggle != null) {
                                        onExecutePermissionToggle(app, perm, !perm.isGranted)
                                    } else {
                                        onRootTogglePermission(app.packageName, perm.rawName, !perm.isGranted)
                                    }
                                }
                            } else {
                                null
                            },
                            onManageClick = if (isControllable) {
                                { controlledPermission = perm }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }

        controlledPermission?.let { targetPerm ->
            val isProtectedCritical = app.isProtectedCriticalSystem ||
                PermissionManagerBridge.isProtectedCriticalSystemApp(
                    app.packageName,
                    app.isSystemApp,
                    app.installStatus
                )
            PermissionControlDialog(
                app = app,
                permission = targetPerm,
                isRootAvailable = isRootAvailable || executionMode == PermissionExecutionMode.SHIZUKU,
                isProtectedCriticalSystem = isProtectedCritical,
                executionMode = executionMode,
                onDismiss = { controlledPermission = null },
                onOpenSystemSettings = {
                    controlledPermission = null
                    SystemIntents.openApplicationDetailsSettings(context, app.packageName)
                },
                onRootGrantRevoke = { grant ->
                    controlledPermission = null
                    if (onExecutePermissionToggle != null) {
                        onExecutePermissionToggle(app, targetPerm, grant)
                    } else {
                        onRootTogglePermission(app.packageName, targetPerm.rawName, grant)
                    }
                },
                onCopyAdbCommand = { cmd ->
                    controlledPermission = null
                    clipboardManager.setText(AnnotatedString(cmd))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(adbCopiedMsg)
                    }
                }
            )
        }

        if (showReportDialog) {
            FalseDetectionReportDialog(
                app = app,
                onDismiss = { showReportDialog = false },
                onSend = { reportBody ->
                    showReportDialog = false
                    val sent = SystemIntents.shareFalseDetectionReport(
                        context = context,
                        subject = "[ActiPerm] Laporan Salah Deteksi: ${app.appName}",
                        body = reportBody
                    )
                    if (!sent) {
                        clipboardManager.setText(AnnotatedString(reportBody))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(reportCopiedMsg)
                        }
                    }
                },
                onCopy = { reportBody ->
                    showReportDialog = false
                    clipboardManager.setText(AnnotatedString(reportBody))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(reportCopiedMsg)
                    }
                }
            )
        }
    }
}

@Composable
private fun MetaColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PermissionItemCard(
    perm: AppPermissionDetail,
    isControllable: Boolean = false,
    executionMode: PermissionExecutionMode = PermissionExecutionMode.MANUAL_ADB,
    isProtectedCriticalSystem: Boolean = false,
    onDirectToggleClick: (() -> Unit)? = null,
    onManageClick: (() -> Unit)? = null
) {
    val isAbnormal = perm.isDangerous && !perm.isExpectedForApp
    val isExpectedSensitive = perm.isDangerous && perm.isExpectedForApp
    val accent = when {
        isAbnormal && perm.isGranted -> CrimsonDanger
        isAbnormal -> AmberSuspicious
        isExpectedSensitive -> EmeraldSafe
        else -> EmeraldSafe
    }
    val canDirectExecute = executionMode == PermissionExecutionMode.SHIZUKU ||
        executionMode == PermissionExecutionMode.ROOT

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onManageClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onManageClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isAbnormal && perm.isGranted -> CrimsonDanger.copy(alpha = 0.45f)
                isAbnormal -> AmberSuspicious.copy(alpha = 0.35f)
                else -> SlateBorder
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isAbnormal) Icons.Rounded.WarningAmber else Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = perm.friendlyName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (perm.isGranted) EmeraldSafeContainer else SlateSurfaceHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (perm.isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = if (perm.isGranted) EmeraldSafe else TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        val grantStatusLabel = when {
                            perm.isAutoRevoked -> stringResource(R.string.perm_status_auto_revoked)
                            perm.isGranted -> stringResource(R.string.perm_status_granted)
                            else -> stringResource(R.string.perm_status_not_granted_low_risk)
                        }
                        Text(
                            text = grantStatusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (perm.isGranted) EmeraldSafe else TextMuted
                        )
                    }
                }
            }

            Text(
                text = perm.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = perm.rawName,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isExpectedSensitive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldSafeContainer
                        ) {
                            Text(
                                text = "Sesuai Fungsi",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSafe,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isAbnormal) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CrimsonDangerContainer
                        ) {
                            Text(
                                text = "Di Luar Fungsi",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonDanger,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SlateSurfaceHigh
                    ) {
                        Text(
                            text = perm.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAbnormal) accent else TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Badge status eksekusi & tombol [Cabut] / [Berikan] untuk izin berbahaya
            if (isControllable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeText = when {
                        isProtectedCriticalSystem -> "Aplikasi Sistem - Dilindungi"
                        canDirectExecute -> "Bisa Diatur Langsung"
                        else -> "Butuh Pengaturan Manual"
                    }
                    val badgeColor = when {
                        isProtectedCriticalSystem -> EmeraldSafe
                        canDirectExecute -> CyberCyan
                        else -> AmberSuspicious
                    }
                    val badgeContainer = when {
                        isProtectedCriticalSystem -> EmeraldSafeContainer.copy(alpha = 0.7f)
                        canDirectExecute -> CyberCyanContainer
                        else -> AmberSuspiciousContainer.copy(alpha = 0.6f)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeContainer
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (onManageClick != null) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onManageClick),
                                shape = RoundedCornerShape(8.dp),
                                color = SlateSurfaceHigh,
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = "Opsi ADB",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "ADB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        if (isProtectedCriticalSystem) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SlateSurfaceHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = EmeraldSafe,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Dilindungi",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = EmeraldSafe
                                    )
                                }
                            }
                        } else if (onDirectToggleClick != null) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onDirectToggleClick)
                                    .testTag("perm_toggle_${perm.rawName}"),
                                shape = RoundedCornerShape(8.dp),
                                color = if (perm.isGranted) CrimsonDangerContainer else EmeraldSafeContainer,
                                border = BorderStroke(
                                    1.dp,
                                    if (perm.isGranted) CrimsonDanger.copy(alpha = 0.55f) else EmeraldSafe.copy(alpha = 0.55f)
                                )
                            ) {
                                Text(
                                    text = if (perm.isGranted) "Cabut" else "Berikan",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (perm.isGranted) CrimsonDanger else EmeraldSafe,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FalseDetectionReportDialog(
    app: InstalledAppInfo,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
    onCopy: (String) -> Unit
) {
    val options = listOf(
        "Aplikasi aman, tapi terdeteksi Mencurigakan/Berbahaya",
        "Aplikasi bawaan HP, tapi masuk ke Aplikasi Unduhan",
        "Aplikasi pinjol/berisiko, seharusnya ditandai Berbahaya",
        "Kategori fungsi aplikasi kurang tepat"
    )
    var selectedReason by remember { mutableStateOf(options.first()) }
    var extraNote by remember { mutableStateOf("") }

    val deviceText = app.detectedDeviceBrandName.ifBlank {
        PermissionAnalyzer.detectDeviceBrandDisplayName()
    }
    val androidText = app.androidVersionLabel.ifBlank {
        PermissionAnalyzer.formatAndroidVersionLabel()
    }

    fun buildReportBody(): String = buildString {
        appendLine("=== LAPORAN SALAH DETEKSI ACTIPERM ===")
        appendLine("Nama Aplikasi : ${app.appName}")
        appendLine("Package Name  : ${app.packageName}")
        appendLine("Merk HP       : $deviceText")
        appendLine("Versi Android : $androidText")
        appendLine("Status Saat Ini: ${app.installStatus.label}")
        appendLine("Kategori      : ${app.functionalCategory.label}")
        appendLine("Skor Saat Ini : ${app.riskLevel.label} (${app.riskScore}/100)")
        appendLine("Keluhan       : $selectedReason")
        if (extraNote.isNotBlank()) {
            appendLine("Catatan       : ${extraNote.trim()}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = stringResource(R.string.report_dialog_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${app.appName} (${app.packageName})",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = CyberCyan
                )
                options.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedReason = opt }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == opt,
                            onClick = { selectedReason = opt },
                            colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                        )
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
                OutlinedTextField(
                    value = extraNote,
                    onValueChange = { extraNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan tambahan (opsional)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(buildReportBody()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = ObsidianBg
                )
            ) {
                Text(stringResource(R.string.report_dialog_send))
            }
        },
        dismissButton = {
            TextButton(onClick = { onCopy(buildReportBody()) }) {
                Text(
                    text = stringResource(R.string.report_dialog_copy),
                    color = CyberCyan
                )
            }
        }
    )
}
