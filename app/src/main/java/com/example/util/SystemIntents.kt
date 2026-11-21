package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object SystemIntents {

    const val SAWERIA_SUPPORT_URL = "https://saweria.co/Yaudahiya9"

    fun openApplicationDetailsSettings(context: Context, packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        return startSafely(context, intent)
    }

    fun openAppPermissionsDirectly(context: Context, packageName: String): Boolean {
        val managePermsIntent = Intent("android.intent.action.MANAGE_APP_PERMISSIONS").apply {
            putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        }
        if (startSafely(context, managePermsIntent)) {
            return true
        }
        return openApplicationDetailsSettings(context, packageName)
    }

    fun openSpecificPermissionSettings(
        context: Context,
        packageName: String,
        rawPermissionName: String
    ): Boolean {
        val pkgUri = Uri.fromParts("package", packageName, null)
        when (rawPermissionName) {
            "android.permission.SYSTEM_ALERT_WINDOW" -> {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri)
                if (startSafely(context, intent)) return true
            }
            "android.permission.WRITE_SETTINGS" -> {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkgUri)
                if (startSafely(context, intent)) return true
            }
            "android.permission.REQUEST_INSTALL_PACKAGES" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, pkgUri)
                    if (startSafely(context, intent)) return true
                } else {
                    val secIntent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                    if (startSafely(context, secIntent)) return true
                }
            }
            "android.permission.MANAGE_EXTERNAL_STORAGE" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkgUri)
                    if (startSafely(context, intent)) return true
                }
            }
            "android.permission.PACKAGE_USAGE_STATS" -> {
                if (openUsageAccessSettings(context)) return true
            }
            "android.permission.BIND_ACCESSIBILITY_SERVICE" -> {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                if (startSafely(context, intent)) return true
            }
        }
        return openAppPermissionsDirectly(context, packageName)
    }

    fun launchInstalledApp(context: Context, packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(context, launchIntent)
    }

    fun buildUninstallIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null)).apply {
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
    }

    fun openUsageAccessSettings(context: Context): Boolean {
        val pkgSpecific = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        if (startSafely(context, pkgSpecific)) return true

        val fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        return startSafely(context, fallback)
    }

    fun shareFalseDetectionReport(context: Context, subject: String, body: String): Boolean {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val chooser = Intent.createChooser(sendIntent, "Kirim Laporan Salah Deteksi")
        return startSafely(context, chooser)
    }

    fun openExternalUrl(context: Context, url: String = SAWERIA_SUPPORT_URL): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return startSafely(context, intent)
    }

    private fun startSafely(context: Context, intent: Intent): Boolean {
        return try {
            if (context !is android.app.Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
