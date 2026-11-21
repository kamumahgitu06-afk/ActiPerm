package com.example.util

import android.content.Context
import android.net.Uri
import com.example.model.InstalledAppInfo
import com.example.ui.ActiPermUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private val indonesianLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()

    fun defaultFullReportFileName(nowMillis: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        return "Laporan_ActiPerm_${fmt.format(Date(nowMillis))}.txt"
    }

    fun defaultSingleAppReportFileName(
        app: InstalledAppInfo,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val safeName = app.appName
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .trim('_')
            .take(28)
            .ifBlank { "Aplikasi" }
        val fmt = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        return "Laporan_ActiPerm_${safeName}_${fmt.format(Date(nowMillis))}.txt"
    }

    fun buildFullReport(
        uiState: ActiPermUiState,
        generatedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", indonesianLocale)
        val sb = StringBuilder(4096)

        sb.appendLine("================================================================")
        sb.appendLine("LAPORAN LENGKAP AUDIT IZIN & KEAMANAN APLIKASI - ACTIPERM")
        sb.appendLine("Dibuat oleh Yaudah Iya")
        sb.appendLine("================================================================")
        sb.appendLine("Waktu Ekspor       : ${dateFmt.format(Date(generatedAtMillis))}")
        sb.appendLine("Perangkat          : ${uiState.detectedDeviceBrandName.ifBlank { "Android" }} • ${uiState.androidVersionLabel.ifBlank { "-" }}")
        sb.appendLine("Total Aplikasi     : ${uiState.totalAppsCount} (Unduhan: ${uiState.downloadedAppsCount} | Bawaan: ${uiState.systemAppsCount})")
        sb.appendLine("Ringkasan Risiko   : Berbahaya: ${uiState.dangerousAppsCount} | Mencurigakan: ${uiState.suspiciousAppsCount} | Aman: ${uiState.safeAppsCount}")
        sb.appendLine("Total Izin Tak Wajar: ${uiState.totalDangerousPermissionsCount}")
        sb.appendLine("Akses Penggunaan   : ${if (uiState.hasUsageAccess) "Aktif" else "Belum Diaktifkan"}")
        sb.appendLine("================================================================")
        sb.appendLine()

        val downloaded = uiState.allApps.filter { !it.isSystemApp }
        val system = uiState.allApps.filter { it.isSystemApp }

        sb.appendLine("--- BAGIAN 1: APLIKASI UNDUHAN (${downloaded.size}) ---")
        sb.appendLine()
        if (downloaded.isEmpty()) {
            sb.appendLine("(Tidak ada aplikasi unduhan terdeteksi)")
            sb.appendLine()
        } else {
            downloaded.forEachIndexed { index, app ->
                appendAppSection(sb, index + 1, app, generatedAtMillis)
            }
        }

        sb.appendLine("--- BAGIAN 2: APLIKASI BAWAAN (${system.size}) ---")
        sb.appendLine()
        if (system.isEmpty()) {
            sb.appendLine("(Tidak ada aplikasi bawaan terdeteksi)")
            sb.appendLine()
        } else {
            system.forEachIndexed { index, app ->
                appendAppSection(sb, index + 1, app, generatedAtMillis)
            }
        }

        sb.appendLine("================================================================")
        sb.appendLine("Akhir Laporan - ActiPerm")
        sb.appendLine("================================================================")
        return sb.toString()
    }

    fun buildSingleAppReport(
        app: InstalledAppInfo,
        generatedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val dateFmt = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", indonesianLocale)
        val sb = StringBuilder(1024)
        sb.appendLine("================================================================")
        sb.appendLine("LAPORAN DETAIL IZIN APLIKASI - ACTIPERM")
        sb.appendLine("Dibuat oleh Yaudah Iya")
        sb.appendLine("================================================================")
        sb.appendLine("Waktu Ekspor       : ${dateFmt.format(Date(generatedAtMillis))}")
        sb.appendLine("================================================================")
        sb.appendLine()
        appendAppSection(sb, 1, app, generatedAtMillis)
        sb.appendLine("================================================================")
        return sb.toString()
    }

    private fun appendAppSection(
        sb: StringBuilder,
        number: Int,
        app: InstalledAppInfo,
        nowMillis: Long
    ) {
        sb.appendLine("[$number] ${app.appName} (${app.packageName})")
        sb.appendLine("  Status Aplikasi  : ${app.installStatus.label} (${if (app.isEnabled) "Aktif" else "Nonaktif"})")
        sb.appendLine("  Merk / Android   : ${app.detectedDeviceBrandName.ifBlank { "-" }} • ${app.androidVersionLabel.ifBlank { "-" }}")
        sb.appendLine("  Kategori Fungsi  : ${app.functionalCategory.label}")
        sb.appendLine("  Versi & Ukuran   : v${app.versionName} | ${FormattingUtils.formatSize(app.sizeBytes)}")
        sb.appendLine("  Terakhir Dipakai : ${FormattingUtils.formatLastUsed(app.lastUsedTimeMillis, nowMillis)}")
        sb.appendLine("  Skor Bahaya      : ${app.riskLevel.label} (${app.riskScore}/100)")
        sb.appendLine("  Ringkasan Izin   : Total Diminta=${app.permissions.size}, Sensitif Diminta=${app.dangerousPermissionCount}, Sensitif Diizinkan=${app.grantedDangerousCount}, Tak Wajar=${app.unexpectedDangerousCount}")
        sb.appendLine("  Analisis Keamanan:")
        app.riskReasons.forEach { reason ->
            sb.appendLine("    - $reason")
        }
        if (app.permissions.isEmpty()) {
            sb.appendLine("  Daftar Izin      : Tidak meminta izin khusus.")
        } else {
            sb.appendLine("  Daftar Izin Diminta (${app.permissions.size}):")
            app.permissions.forEach { perm ->
                val grantTag = when {
                    perm.isAutoRevoked -> "AUTO-REVOKE"
                    perm.isGranted -> "DIIZINKAN"
                    else -> "BELUM DIIZINKAN - BELUM BERISIKO"
                }
                val riskTag = when {
                    perm.isSystemInternal -> "SIGNATURE/INTERNAL"
                    perm.isDangerous && perm.isExpectedForApp -> "SESUAI FUNGSI"
                    perm.isDangerous && !perm.isExpectedForApp -> "DI LUAR FUNGSI"
                    else -> "NORMAL"
                }
                sb.appendLine("    * [$grantTag] [$riskTag] ${perm.friendlyName} (${perm.rawName})")
            }
        }
        sb.appendLine()
    }

    suspend fun writeTextToUri(
        context: Context,
        uri: Uri,
        content: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Tidak dapat membuka file tujuan")
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(content)
            }
        }
    }
}
