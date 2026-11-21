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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.PermissionManagerBridge
import com.example.model.PermissionChangeLogEntry
import com.example.model.PermissionExecutionMode
import com.example.model.ShizukuConnectionState
import com.example.model.ShizukuStatusSnapshot
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryHeaderCard(
    historyCount: Int,
    onClearHistory: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_header_card"),
        shape = RoundedCornerShape(18.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "Riwayat Perubahan Izin",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "$historyCount catatan aksi dicatat",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (historyCount > 0) {
                TextButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = CrimsonDanger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bersihkan", color = CrimsonDanger)
                }
            }
        }
    }
}

@Composable
fun HistoryLogCard(entry: PermissionChangeLogEntry) {
    val formatter = remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale("id", "ID"))
    }
    val timeText = remember(entry.timestampMillis) {
        formatter.format(Date(entry.timestampMillis))
    }
    val actionColor = if (entry.granted) EmeraldSafe else CrimsonDanger
    val actionBg = if (entry.granted) EmeraldSafeContainer else CrimsonDangerContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.appName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = actionBg
                ) {
                    Text(
                        text = if (entry.granted) "Diberikan" else "Dicabut",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = actionColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = entry.permissionFriendly,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = CyberCyan
            )
            Text(
                text = entry.permissionRaw,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SlateSurfaceHigh
                ) {
                    Text(
                        text = entry.executionModeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (entry.note.isNotBlank()) {
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberSuspicious
                )
            }
        }
    }
}

@Composable
fun HistoryEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Belum Ada Riwayat Perubahan Izin",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "Setiap izin yang Anda cabut atau berikan lewat ActiPerm maupun aksi massal akan tercatat di sini beserta waktunya.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun ShizukuStatusSection(
    shizukuStatus: ShizukuStatusSnapshot,
    executionMode: PermissionExecutionMode,
    onCheckShizuku: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onCopyCommand: (String) -> Unit,
    onSupportClick: () -> Unit = {}
) {
    val isReady = shizukuStatus.connectionState == ShizukuConnectionState.ACTIVE_GRANTED
    val statusAccent = when (shizukuStatus.connectionState) {
        ShizukuConnectionState.ACTIVE_GRANTED -> EmeraldSafe
        ShizukuConnectionState.ACTIVE_NEED_PERMISSION -> AmberSuspicious
        else -> CrimsonDanger
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("shizuku_status_card"),
            shape = RoundedCornerShape(20.dp),
            color = SlateSurface,
            border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isReady) Icons.Rounded.CheckCircle else Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = statusAccent,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status Shizuku (Package Manager Pro)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = shizukuStatus.connectionState.label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = statusAccent
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SlateSurfaceHigh
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Mode Eksekusi Saat Ini: ${executionMode.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberCyan
                        )
                        Text(
                            text = "• Aplikasi Shizuku: ${if (shizukuStatus.isInstalled) "Terinstal" else "Belum Terinstal"}\n" +
                                "• Layanan Binder: ${if (shizukuStatus.isBinderAlive) "Berjalan (API v${shizukuStatus.version})" else "Mati"}\n" +
                                "• Izin ActiPerm: ${if (shizukuStatus.hasPermission) "Diizinkan" else "Belum Diizinkan"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = onCheckShizuku,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("check_shizuku_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cek Shizuku",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (shizukuStatus.connectionState == ShizukuConnectionState.ACTIVE_NEED_PERMISSION) {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("request_shizuku_perm_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberSuspicious,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Minta Izin Shizuku Sekarang",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                if (shizukuStatus.isInstalled) {
                    OutlinedButton(
                        onClick = onOpenShizukuApp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Aplikasi Shizuku", color = TextPrimary)
                    }
                }
            }
        }

        // Panduan 3 Mode Eksekusi & Aktivasi ADB (Android 7 - 15)
        Surface(
            modifier = Modifier.fillMaxWidth(),
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
                    Icon(Icons.Rounded.Terminal, contentDescription = null, tint = AmberSuspicious, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Panduan Aktivasi Shizuku & ADB (Tanpa Root)",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "1. Mode A (Shizuku - Otomatis):\n" +
                        "   • Android 11+: Instal Shizuku -> aktifkan Wireless Debugging di Opsi Pengembang -> Pasangkan & tekan Mulai.\n" +
                        "   • Android 7–10: Hubungkan HP ke PC (USB Debugging aktif) lalu jalankan 1x perintah berikut di terminal PC:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                val shizukuAdbCmd = PermissionManagerBridge.buildShizukuStartAdbCommand()
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SlateSurfaceHigh,
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = shizukuAdbCmd,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = CyberCyan
                        )
                        TextButton(
                            onClick = { onCopyCommand(shizukuAdbCmd) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Perintah Aktivasi Shizuku ADB", color = CyberCyan)
                        }
                    }
                }

                Text(
                    text = "2. Mode B (ADB Langsung Tanpa Shizuku):\n" +
                        "   Gunakan perintah ADB dari PC untuk cabut/beri izin paket apa pun:\n" +
                        "   adb shell pm revoke <nama_paket> <nama_izin>",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = EmeraldSafe, modifier = Modifier.size(16.dp))
                    Text(
                        text = "3. Mode C (Fallback Manual Wajib): Jika Shizuku belum aktif dan HP tidak di-root, tombol Cabut/Berikan otomatis membuka Pengaturan Izin Android tanpa error.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldSafe
                    )
                }

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AmberSuspicious, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Perlindungan Keamanan: Aplikasi sistem kritis (System UI, Android System, Pengaturan, dll) dilindungi otomatis untuk mencegah HP bootloop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberSuspicious
                    )
                }
            }
        }

        SupportDeveloperCard(
            onSupportClick = onSupportClick,
            buttonTag = "saweria_shizuku_btn"
        )
    }
}
