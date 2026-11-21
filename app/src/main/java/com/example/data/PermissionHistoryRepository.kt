package com.example.data

import android.content.Context
import com.example.model.PermissionChangeLogEntry
import org.json.JSONArray
import org.json.JSONObject

interface PermissionHistoryDataSource {
    fun loadHistory(): List<PermissionChangeLogEntry>
    fun recordChange(entry: PermissionChangeLogEntry): List<PermissionChangeLogEntry>
    fun clearHistory(): List<PermissionChangeLogEntry>
}

class PermissionHistoryRepository(
    context: Context
) : PermissionHistoryDataSource {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadHistory(): List<PermissionChangeLogEntry> {
        val raw = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val result = ArrayList<PermissionChangeLogEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                result.add(
                    PermissionChangeLogEntry(
                        id = obj.optLong("id", 0L),
                        timestampMillis = obj.optLong("timestampMillis", 0L),
                        appName = obj.optString("appName", ""),
                        packageName = obj.optString("packageName", ""),
                        permissionRaw = obj.optString("permissionRaw", ""),
                        permissionFriendly = obj.optString("permissionFriendly", ""),
                        granted = obj.optBoolean("granted", false),
                        executionModeLabel = obj.optString("executionModeLabel", ""),
                        success = obj.optBoolean("success", true),
                        note = obj.optString("note", "")
                    )
                )
            }
            result.sortedByDescending { it.timestampMillis }
        }.getOrDefault(emptyList())
    }

    override fun recordChange(entry: PermissionChangeLogEntry): List<PermissionChangeLogEntry> {
        val updated = (listOf(entry) + loadHistory())
            .sortedByDescending { it.timestampMillis }
            .take(MAX_HISTORY_ENTRIES)
        saveHistory(updated)
        return updated
    }

    override fun clearHistory(): List<PermissionChangeLogEntry> {
        prefs.edit().remove(KEY_HISTORY_JSON).apply()
        return emptyList()
    }

    private fun saveHistory(entries: List<PermissionChangeLogEntry>) {
        val arr = JSONArray()
        entries.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("timestampMillis", item.timestampMillis)
                    put("appName", item.appName)
                    put("packageName", item.packageName)
                    put("permissionRaw", item.permissionRaw)
                    put("permissionFriendly", item.permissionFriendly)
                    put("granted", item.granted)
                    put("executionModeLabel", item.executionModeLabel)
                    put("success", item.success)
                    put("note", item.note)
                }
            )
        }
        prefs.edit().putString(KEY_HISTORY_JSON, arr.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "actiperm_permission_history"
        private const val KEY_HISTORY_JSON = "history_entries_v1"
        private const val MAX_HISTORY_ENTRIES = 200
    }
}
