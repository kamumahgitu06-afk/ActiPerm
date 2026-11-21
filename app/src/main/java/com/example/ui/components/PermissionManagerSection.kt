package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.PermissionManagerBridge
import com.example.model.AppPermissionDetail
import com.example.model.InstalledAppInfo
import com.example.model.PermissionCategory
import com.example.model.PermissionControlMode
import com.example.model.PermissionExecutionMode
import com.example.ui.ActiPermUiState
import com.example.ui.PermissionMatrixEntry
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

@Composable
fun PermissionManagerHeaderPanel(
    uiState: ActiPermUiState,
    onSelectCategory: (PermissionCategory?) -> Unit,
    onSelectGrantFilter: (Boolean?) -> Unit,
    onToggleIncludeSystem: () -> Unit
) {
    val entries = uiState.visiblePermissionEntries
    val grantedCount = entries.count { it.permission.isGranted }
    val ungrantedCount = entries.size - grantedCount

    val controllableCategories = listOf(
        PermissionCategory.LOCATION,
        PermissionCategory.CAMERA,
        PermissionCategory.MICROPHONE,
        PermissionCategory.CONTACTS,
        PermissionCategory.PHONE_SMS,
        PermissionCategory.STORAGE,
        PermissionCategory.CALENDAR,
        PermissionCategory.SENSORS,
        PermissionCategory.SYSTEM_SPECIAL
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("permission_manager_header"),
            shape = RoundedCornerShape(18.dp),
            color = SlateSurface,
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
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
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manajer Izin & Package Manager Pro",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = when (uiState.executionMode) {
                                PermissionExecutionMode.SHIZUKU ->
                                    "Mode A (Shizuku Aktif): Cabut/berikan izin langsung tanpa root"
                                PermissionExecutionMode.ROOT ->
                                    "Mode Root Aktif: Cabut/berikan izin langsung via su"
                                PermissionExecutionMode.MANUAL_ADB ->
                                    "Mode B/C (ADB & Pengaturan Manual): Kelola semua izin tanpa root"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (uiState.executionMode != PermissionExecutionMode.MANUAL_ADB) {
                            EmeraldSafeContainer
                        } else {
                            SlateSurfaceHigh
                        }
                    ) {
                        Text(
                            text = when (uiState.executionMode) {
                                PermissionExecutionMode.SHIZUKU -> "SHIZUKU AKTIF"
                                PermissionExecutionMode.ROOT -> "ROOT AKTIF"
                                PermissionExecutionMode.MANUAL_ADB -> "MANUAL / ADB"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (uiState.executionMode != PermissionExecutionMode.MANUAL_ADB) {
                                EmeraldSafe
                            } else {
                                TextSecondary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldSafeContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "$grantedCount Diizinkan",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldSafe,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSurfaceHigh
                    ) {
                        Text(
                            text = "$ungrantedCount Belum Diizinkan",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleIncludeSystem)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sertakan Aplikasi Bawaan / Sistem",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Switch(
                        checked = uiState.includeSystemAppsInPermManager,
                        onCheckedChange = { onToggleIncludeSystem() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = CyberCyan
                        )
                    )
                }
            }
        }

        // Filter Kategori Izin
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedPermissionCategory == null,
                onClick = { onSelectCategory(null) },
                modifier = Modifier.testTag("pm_cat_all"),
                label = { Text("Semua Kategori") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SlateSurface,
                    labelColor = TextSecondary,
                    selectedContainerColor = CyberCyanContainer,
                    selectedLabelColor = CyberCyan
                )
            )
            controllableCategories.forEach { cat ->
                val selected = uiState.selectedPermissionCategory == cat
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(cat) },
                    modifier = Modifier.testTag("pm_cat_${cat.name.lowercase()}"),
                    label = { Text(cat.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SlateSurface,
                        labelColor = TextSecondary,
                        selectedContainerColor = CyberCyanContainer,
                        selectedLabelColor = CyberCyan
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

        // Filter Status Diizinkan vs Belum Diizinkan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statusOptions = listOf(
                null to "Semua Status",
                true to "Sudah Diizinkan",
                false to "Belum Diizinkan"
            )
            statusOptions.forEach { (value, label) ->
                val selected = uiState.permissionGrantFilter == value
                FilterChip(
                    selected = selected,
                    onClick = { onSelectGrantFilter(value) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SlateSurface,
                        labelColor = TextSecondary,
                        selectedContainerColor = CyberCyanContainer,
                        selectedLabelColor = CyberCyan
                    )
                )
            }
        }
    }
}

@Composable
fun PermissionMatrixRowCard(
    entry: PermissionMatrixEntry,
    isRootAvailable: Boolean,
    onOpenSettingsForPermission: () -> Unit,
    onOpenControlSheet: () -> Unit,
    onOpenAppDetail: () -> Unit,
    onRootToggle: (Boolean) -> Unit
) {
    val app = entry.app
    val perm = entry.permission
    val isSpecial = entry.controlMode == PermissionControlMode.SPECIAL_SETTINGS_CONTROLLABLE

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenControlSheet)
            .testTag("pm_row_${app.packageName}_${perm.rawName}"),
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(
            1.dp,
            if (perm.isGranted && perm.isDangerous && !perm.isExpectedForApp) {
                CrimsonDanger.copy(alpha = 0.45f)
            } else {
                SlateBorder
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppIconImage(
                    packageName = app.packageName,
                    contentDescription = "Ikon ${app.appName}",
                    modifier = Modifier.size(42.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = perm.friendlyName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = CyberCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = perm.rawName,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                        Text(
                            text = if (perm.isGranted) "Diizinkan" else "Belum",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (perm.isGranted) EmeraldSafe else TextMuted
                        )
                    }
                }
            }

            val isProtected = app.isProtectedCriticalSystem ||
                PermissionManagerBridge.isProtectedCriticalSystemApp(
                    app.packageName,
                    app.isSystemApp,
                    app.installStatus
                )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeText = when {
                    isProtected -> "Aplikasi Sistem - Dilindungi"
                    isRootAvailable -> "Bisa Diatur Langsung"
                    else -> "Butuh Pengaturan Manual"
                }
                val badgeColor = when {
                    isProtected -> EmeraldSafe
                    isRootAvailable -> CyberCyan
                    else -> AmberSuspicious
                }
                val badgeBg = when {
                    isProtected -> EmeraldSafeContainer.copy(alpha = 0.7f)
                    isRootAvailable -> CyberCyanContainer
                    else -> AmberSuspiciousContainer.copy(alpha = 0.6f)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isProtected) {
                    Button(
                        onClick = onOpenAppDetail,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlateSurfaceHigh,
                            contentColor = EmeraldSafe
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Dilindungi (Sistem Kritis)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else if (isRootAvailable && !isSpecial) {
                    Button(
                        onClick = { onRootToggle(!perm.isGranted) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (perm.isGranted) CrimsonDangerContainer else EmeraldSafeContainer,
                            contentColor = if (perm.isGranted) CrimsonDanger else EmeraldSafe
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (perm.isGranted) "Cabut Izin" else "Berikan Izin",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Button(
                        onClick = onOpenSettingsForPermission,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyanContainer,
                            contentColor = CyberCyan
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSpecial) {
                                "Atur Izin Khusus"
                            } else if (perm.isGranted) {
                                "Cabut (Pengaturan)"
                            } else {
                                "Berikan (Pengaturan)"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenControlSheet,
                    modifier = Modifier.heightIn(min = 44.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Opsi Package Manager",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                OutlinedButton(
                    onClick = onOpenAppDetail,
                    modifier = Modifier.heightIn(min = 44.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Detail",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionControlDialog(
    app: InstalledAppInfo,
    permission: AppPermissionDetail,
    isRootAvailable: Boolean,
    isProtectedCriticalSystem: Boolean = false,
    executionMode: PermissionExecutionMode = if (isRootAvailable) {
        PermissionExecutionMode.ROOT
    } else {
        PermissionExecutionMode.MANUAL_ADB
    },
    onDismiss: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onRootGrantRevoke: (Boolean) -> Unit,
    onCopyAdbCommand: (String) -> Unit
) {
    val mode = PermissionManagerBridge.classifyControlMode(permission)
    val grantAdbCmd = PermissionManagerBridge.buildAdbCommand(app.packageName, permission.rawName, grant = true)
    val revokeAdbCmd = PermissionManagerBridge.buildAdbCommand(app.packageName, permission.rawName, grant = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Kelola Izin Aplikasi",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${app.appName} • ${permission.friendlyName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = permission.rawName,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = TextMuted
                )
                Text(
                    text = "Status saat ini: ${if (permission.isGranted) "Diizinkan Pengguna" else "Belum Diizinkan"} (${mode.label}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
                Text(
                    text = "Mode Eksekusi: ${executionMode.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan
                )

                if (isProtectedCriticalSystem) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldSafeContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, EmeraldSafe)
                    ) {
                        Text(
                            text = "Aplikasi Sistem - Dilindungi: Pencabutan izin dibatasi demi mencegah bootloop pada perangkat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSafe,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = onOpenSystemSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Pengaturan Izin Android")
                }

                if (mode == PermissionControlMode.RUNTIME_CONTROLLABLE && !isProtectedCriticalSystem) {
                    if (isRootAvailable || executionMode != PermissionExecutionMode.MANUAL_ADB) {
                        OutlinedButton(
                            onClick = { onRootGrantRevoke(!permission.isGranted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (permission.isGranted) CrimsonDangerContainer.copy(alpha = 0.5f) else EmeraldSafeContainer.copy(alpha = 0.5f),
                                contentColor = if (permission.isGranted) CrimsonDanger else EmeraldSafe
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (permission.isGranted) CrimsonDanger else EmeraldSafe
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            val modeTag = if (executionMode == PermissionExecutionMode.SHIZUKU) "Shizuku" else "Root"
                            Text(
                                text = if (permission.isGranted) {
                                    "Cabut Langsung via $modeTag"
                                } else {
                                    "Berikan Langsung via $modeTag"
                                }
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SlateSurfaceHigh,
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = null,
                                    tint = AmberSuspicious,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Perintah ADB Package Manager",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AmberSuspicious
                                )
                            }
                            val targetCmd = if (permission.isGranted) revokeAdbCmd else grantAdbCmd
                            Text(
                                text = targetCmd,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = TextPrimary
                            )
                            TextButton(
                                onClick = { onCopyAdbCommand(targetCmd) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (permission.isGranted) {
                                        "Salin Perintah Cabut ADB"
                                    } else {
                                        "Salin Perintah Beri Izin ADB"
                                    },
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = TextSecondary)
            }
        }
    )
}
