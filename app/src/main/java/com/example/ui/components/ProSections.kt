package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.PermissionManagerBridge
import com.example.model.BatchRevokeProposal
import com.example.model.BatchRevokeType
import com.example.model.MainNavTab
import com.example.model.PermissionExecutionMode
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
import com.example.ui.theme.SaweriaAccent
import com.example.ui.theme.SaweriaContainer
import com.example.ui.theme.SaweriaOnAccent
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ActiPermBottomNavBar(
    selectedTab: MainNavTab,
    historyCount: Int,
    shizukuActive: Boolean,
    onSelectTab: (MainNavTab) -> Unit
) {
    NavigationBar(
        containerColor = SlateSurface,
        contentColor = TextPrimary,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == MainNavTab.DASHBOARD,
            onClick = { onSelectTab(MainNavTab.DASHBOARD) },
            modifier = Modifier.testTag("bottom_nav_dashboard"),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Dashboard,
                    contentDescription = MainNavTab.DASHBOARD.label
                )
            },
            label = {
                Text(
                    text = MainNavTab.DASHBOARD.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberCyan,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyanContainer,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = selectedTab == MainNavTab.ALL_APPS,
            onClick = { onSelectTab(MainNavTab.ALL_APPS) },
            modifier = Modifier.testTag("bottom_nav_all_apps"),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = MainNavTab.ALL_APPS.label
                )
            },
            label = {
                Text(
                    text = "Aplikasi",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberCyan,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyanContainer,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = selectedTab == MainNavTab.HISTORY,
            onClick = { onSelectTab(MainNavTab.HISTORY) },
            modifier = Modifier.testTag("bottom_nav_history"),
            icon = {
                BadgedBox(
                    badge = {
                        if (historyCount > 0) {
                            Badge(
                                containerColor = CyberCyan,
                                contentColor = ObsidianBg
                            ) {
                                Text(if (historyCount > 99) "99+" else historyCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = MainNavTab.HISTORY.label
                    )
                }
            },
            label = {
                Text(
                    text = MainNavTab.HISTORY.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberCyan,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyanContainer,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = selectedTab == MainNavTab.SHIZUKU,
            onClick = { onSelectTab(MainNavTab.SHIZUKU) },
            modifier = Modifier.testTag("bottom_nav_shizuku"),
            icon = {
                BadgedBox(
                    badge = {
                        if (shizukuActive) {
                            Badge(containerColor = EmeraldSafe)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = MainNavTab.SHIZUKU.label
                    )
                }
            },
            label = {
                Text(
                    text = MainNavTab.SHIZUKU.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberCyan,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyanContainer,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}

@Composable
fun SupportDeveloperCard(
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonTag: String = "saweria_support_btn"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("support_developer_card"),
        shape = RoundedCornerShape(18.dp),
        color = SaweriaContainer.copy(alpha = 0.72f),
        border = BorderStroke(1.2.dp, SaweriaAccent.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SaweriaAccent.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, SaweriaAccent.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = SaweriaAccent,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.support_developer_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SaweriaAccent
                    )
                    Text(
                        text = stringResource(R.string.support_developer_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }

            SaweriaSupportButton(
                onClick = onSupportClick,
                compact = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(buttonTag)
            )
        }
    }
}

@Composable
fun SaweriaSupportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (compact) 48.dp else 52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SaweriaAccent,
            contentColor = SaweriaOnAccent
        ),
        shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
        contentPadding = PaddingValues(
            horizontal = if (compact) 16.dp else 18.dp,
            vertical = if (compact) 10.dp else 12.dp
        )
    ) {
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = CrimsonDanger,
            modifier = Modifier.size(if (compact) 16.dp else 19.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.action_sawer_here),
            style = if (compact) {
                MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
            } else {
                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
            },
            color = SaweriaOnAccent
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = SaweriaOnAccent,
            modifier = Modifier.size(if (compact) 15.dp else 17.dp)
        )
    }
}

@Composable
fun BatchActionCard(
    executionMode: PermissionExecutionMode,
    onTriggerBatch: (BatchRevokeType) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_action_card"),
        shape = RoundedCornerShape(18.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aksi Massal Keamanan (Batch Action)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Mode aktif: ${executionMode.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Button(
                onClick = { onTriggerBatch(BatchRevokeType.DANGEROUS_LOCATION) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("batch_revoke_location_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonDangerContainer,
                    contentColor = CrimsonDanger
                ),
                border = BorderStroke(1.dp, CrimsonDanger.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = BatchRevokeType.DANGEROUS_LOCATION.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            OutlinedButton(
                onClick = { onTriggerBatch(BatchRevokeType.SUSPICIOUS_CAMERA) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("batch_revoke_camera_btn"),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = AmberSuspiciousContainer.copy(alpha = 0.45f),
                    contentColor = AmberSuspicious
                ),
                border = BorderStroke(1.dp, AmberSuspicious.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = BatchRevokeType.SUSPICIOUS_CAMERA.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun BatchConfirmationDialog(
    proposal: BatchRevokeProposal,
    executionMode: PermissionExecutionMode,
    isExecuting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCopyBatchAdb: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isExecuting) onDismiss() },
        containerColor = SlateSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Konfirmasi Aksi Massal",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = proposal.type.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberCyan
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = proposal.type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                if (proposal.skippedProtectedCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldSafeContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, EmeraldSafe.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = EmeraldSafe,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${proposal.skippedProtectedCount} Aplikasi Sistem - Dilindungi otomatis dilewati untuk mencegah bootloop.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldSafe
                            )
                        }
                    }
                }

                if (proposal.candidates.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateSurfaceHigh
                    ) {
                        Text(
                            text = "Bagus! Tidak ada aplikasi non-sistem dengan izin aktif sesuai kriteria tersebut.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSafe,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Daftar aplikasi & izin yang akan dicabut (${proposal.candidates.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = proposal.candidates,
                            key = { "${it.app.packageName}:${it.permission.rawName}" }
                        ) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = SlateSurfaceHigh,
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = item.app.appName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.permission.friendlyName} • ${item.app.packageName}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (executionMode == PermissionExecutionMode.MANUAL_ADB) {
                        OutlinedButton(
                            onClick = { onCopyBatchAdb(PermissionManagerBridge.buildBatchAdbScript(proposal)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Skrip ADB Massal", color = CyberCyan)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (proposal.candidates.isNotEmpty()) {
                Button(
                    onClick = onConfirm,
                    enabled = !isExecuting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonDanger,
                        contentColor = TextPrimary
                    )
                ) {
                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (executionMode == PermissionExecutionMode.MANUAL_ADB) {
                                "Buka Pengaturan Satu per Satu"
                            } else {
                                "Cabut Sekarang"
                            }
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExecuting
            ) {
                Text("Tutup", color = TextSecondary)
            }
        }
    )
}
