package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormattingUtils {

    private val indonesianLocale = Locale.Builder().setLanguage("id").setRegion("ID").build()

    fun formatSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        val df = DecimalFormat("#,##0.#")
        return when {
            gb >= 1.0 -> "${DecimalFormat("#,##0.##").format(gb)} GB"
            mb >= 1.0 -> "${df.format(mb)} MB"
            else -> "${df.format(kb.coerceAtLeast(1.0))} KB"
        }
    }

    fun formatLastUsed(lastUsedMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
        if (lastUsedMillis == null) return "Perlu izin Akses Penggunaan"
        // Timestamp di bawah tahun 2000 (946684800000L) dianggap belum pernah dipakai
        if (lastUsedMillis < 946_684_800_000L) return "Belum pernah dipakai"

        val diff = (nowMillis - lastUsedMillis).coerceAtLeast(0L)
        val minutes = diff / 60_000L
        val hours = diff / 3_600_000L

        if (minutes < 2L) return "Baru saja"
        if (minutes < 60L) return "$minutes menit lalu"

        val calNow = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val calUsed = Calendar.getInstance().apply { timeInMillis = lastUsedMillis }
        val timeFmt = SimpleDateFormat("HH:mm", indonesianLocale)

        val sameDay = calNow.get(Calendar.YEAR) == calUsed.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calUsed.get(Calendar.DAY_OF_YEAR)
        if (sameDay) {
            return "Hari ini, ${timeFmt.format(Date(lastUsedMillis))}"
        }

        val calYesterday = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val wasYesterday = calYesterday.get(Calendar.YEAR) == calUsed.get(Calendar.YEAR) &&
            calYesterday.get(Calendar.DAY_OF_YEAR) == calUsed.get(Calendar.DAY_OF_YEAR)
        if (wasYesterday) {
            return "Kemarin, ${timeFmt.format(Date(lastUsedMillis))}"
        }

        val days = (hours / 24L).coerceAtLeast(1L)
        if (days < 7L) return "$days hari lalu"
        if (days < 30L) return "${days / 7L} minggu lalu"

        val dateFmt = SimpleDateFormat("d MMM yyyy", indonesianLocale)
        return dateFmt.format(Date(lastUsedMillis))
    }
}
