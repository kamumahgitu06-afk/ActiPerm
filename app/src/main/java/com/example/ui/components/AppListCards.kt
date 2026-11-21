package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.InstalledAppInfo
import com.example.ui.theme.AmberSuspicious
import com.example.ui.theme.AmberSuspiciousContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonDangerContainer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormattingUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadedAppCard(
    app: InstalledAppInfo,
    isSelfApp: Boolean,
    onCardClick: () -> Unit,
    onUninstallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("downloaded_app_card_${app.packageName}"),
        shape = RoundedCornerShape(18.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppIconImage(
                    packageName = app.packageName,
                    contentDescription = "Ikon ${app.appName}",
                    modifier = Modifier.size(48.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "v${app.versionName} • ${FormattingUtils.formatSize(app.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                RiskBadge(riskLevel = app.riskLevel, riskScore = app.riskScore)
            }

            // Sensor badges + ringkasan izin berbahaya
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (app.hasLocationPermission) {
                    SensorMiniTag(icon = Icons.Rounded.LocationOn, text = "Lokasi", tint = CyberCyan)
                }
                if (app.hasCameraPermission) {
                    SensorMiniTag(icon = Icons.Rounded.CameraAlt, text = "Kamera", tint = CyberCyan)
                }
                if (app.hasMicrophonePermission) {
                    SensorMiniTag(icon = Icons.Rounded.Mic, text = "Mic", tint = CyberCyan)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateSurfaceHigh
                ) {
                    val permText = if (app.unexpectedDangerousCount == 0 && app.hasExpectedPermissionsForFunction) {
                        "Izin wajar (${app.functionalCategory.label})"
                    } else if (app.unexpectedDangerousCount > 0) {
                        "${app.unexpectedDangerousCount} izin tak wajar / ${app.permissions.size} total"
                    } else {
                        "${app.dangerousPermissionCount} izin sensitif / ${app.permissions.size} total"
                    }
                    Text(
                        text = permText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (app.unexpectedDangerousCount == 0 && app.hasExpectedPermissionsForFunction) {
                            com.example.ui.theme.EmeraldSafe
                        } else {
                            TextSecondary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onCardClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("detail_btn_${app.packageName}"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = CyberCyanContainer,
                        contentColor = CyberCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Periksa Izin",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (!isSelfApp) {
                    OutlinedButton(
                        onClick = onUninstallClick,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("uninstall_btn_${app.packageName}"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CrimsonDangerContainer.copy(alpha = 0.55f),
                            contentColor = CrimsonDanger
                        ),
                        border = BorderStroke(1.dp, CrimsonDanger.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_uninstall),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateSurfaceHigh
                    ) {
                        Text(
                            text = "Aplikasi Ini",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemAppCard(
    app: InstalledAppInfo,
    isChecked: Boolean,
    onToggleCheck: () -> Unit,
    onCardClick: () -> Unit,
    onDisableClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastUsedFormatted = FormattingUtils.formatLastUsed(app.lastUsedTimeMillis)
    val isUnused = app.isUnused

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("system_app_card_${app.packageName}"),
        shape = RoundedCornerShape(18.dp),
        color = SlateSurface,
        border = BorderStroke(
            width = if (isChecked) 1.5.dp else 1.dp,
            color = if (isChecked) CyberCyan else SlateBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Checkbox untuk antrean penonaktifan massal (48dp touch target)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = app.isEnabled, onClick = onToggleCheck),
                    contentAlignment = Alignment.Center
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = if (app.isEnabled) { _ -> onToggleCheck() } else null,
                        enabled = app.isEnabled,
                        modifier = Modifier.testTag("checkbox_${app.packageName}"),
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyberCyan,
                            uncheckedColor = TextSecondary,
                            checkmarkColor = Color(0xFF070B12),
                            disabledCheckedColor = TextMuted,
                            disabledUncheckedColor = TextMuted.copy(alpha = 0.4f)
                        )
                    )
                }

                AppIconImage(
                    packageName = app.packageName,
                    contentDescription = "Ikon ${app.appName}",
                    dimmed = !app.isEnabled,
                    modifier = Modifier.size(46.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (app.isEnabled) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!app.isEnabled) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SlateSurfaceHigh
                            ) {
                                Text(
                                    text = "Nonaktif",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                RiskBadge(riskLevel = app.riskLevel, riskScore = app.riskScore)
            }

            // Info wajib aplikasi bawaan: Ukuran aplikasi & Terakhir dipakai kapan
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateSurfaceHigh)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Column {
                        Text(
                            text = "Ukuran aplikasi",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = FormattingUtils.formatSize(app.sizeBytes),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily),
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1.3f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = if (isUnused) AmberSuspicious else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Column {
                        Text(
                            text = "Terakhir dipakai",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = lastUsedFormatted,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isUnused) AmberSuspicious else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Sensor mini tags & kategori sistem wajar
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (app.hasLocationPermission) {
                    SensorMiniTag(icon = Icons.Rounded.LocationOn, text = "Lokasi", tint = CyberCyan)
                }
                if (app.hasCameraPermission) {
                    SensorMiniTag(icon = Icons.Rounded.CameraAlt, text = "Kamera", tint = CyberCyan)
                }
                if (app.hasMicrophonePermission) {
                    SensorMiniTag(icon = Icons.Rounded.Mic, text = "Mic", tint = CyberCyan)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateSurfaceHigh
                ) {
                    val permText = if (app.unexpectedDangerousCount == 0 && app.hasExpectedPermissionsForFunction) {
                        "${app.functionalCategory.label} • ${app.permissions.size} izin"
                    } else if (app.unexpectedDangerousCount > 0) {
                        "${app.unexpectedDangerousCount} izin tak wajar / ${app.permissions.size} total"
                    } else {
                        "${app.permissions.size} izin terdeklarasi"
                    }
                    Text(
                        text = permText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (app.unexpectedDangerousCount == 0 && app.hasExpectedPermissionsForFunction) {
                            com.example.ui.theme.EmeraldSafe
                        } else {
                            TextSecondary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Tulisan wajib: "Aplikasi ini tidak bisa dihapus, hanya bisa dinonaktifkan"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AmberSuspiciousContainer.copy(alpha = 0.45f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = AmberSuspicious,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = stringResource(R.string.system_app_cannot_uninstall),
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberSuspicious
                )
            }

            // Tombol Periksa Izin & Nonaktifkan (membuka ACTION_APPLICATION_DETAILS_SETTINGS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onCardClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("system_detail_btn_${app.packageName}"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SlateSurfaceHigh,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Detail Izin",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(
                    onClick = onDisableClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("disable_btn_${app.packageName}"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (app.isEnabled) AmberSuspiciousContainer else SlateSurfaceHigh,
                        contentColor = if (app.isEnabled) AmberSuspicious else TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (app.isEnabled) {
                            stringResource(R.string.action_disable)
                        } else {
                            "Pengaturan"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorMiniTag(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = CyberCyanContainer.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
        }
    }
}
