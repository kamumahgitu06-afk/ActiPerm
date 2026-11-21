package com.example.domain

import android.content.Context
import android.content.pm.PackageManager
import com.example.model.AppInstallStatus
import com.example.model.AppPermissionDetail
import com.example.model.BatchRevokeCandidate
import com.example.model.BatchRevokeProposal
import com.example.model.BatchRevokeType
import com.example.model.InstalledAppInfo
import com.example.model.PermissionCategory
import com.example.model.PermissionControlMode
import com.example.model.PermissionExecutionMode
import com.example.model.PermissionProtectionType
import com.example.model.RiskLevel
import com.example.model.ShizukuConnectionState
import com.example.model.ShizukuStatusSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import java.util.concurrent.TimeUnit

object PermissionManagerBridge {

    const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    const val SHIZUKU_PERMISSION_REQUEST_CODE = 7101

    private val specialToggleablePermissions = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.WRITE_SETTINGS",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.BIND_ACCESSIBILITY_SERVICE"
    )

    private val protectedCriticalPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.shell",
        "com.android.keychain",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.externalstorage",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.android.inputdevices",
        "com.android.webview",
        "com.google.android.webview",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.ext.services",
        "com.samsung.android.scs",
        "com.samsung.android.lool",
        "com.miui.securitycenter",
        "com.coloros.safecenter",
        "com.iqoo.secure"
    )

    private val protectedCriticalPrefixes = listOf(
        "com.android.providers.",
        "com.android.server.",
        "com.android.internal."
    )

    fun isProtectedCriticalSystemApp(
        packageName: String,
        isSystemApp: Boolean = true,
        installStatus: AppInstallStatus? = null
    ): Boolean {
        val pkg = packageName.lowercase().trim()
        if (pkg in protectedCriticalPackages) return true
        if (protectedCriticalPrefixes.any { pkg.startsWith(it) }) return true
        if (isSystemApp && installStatus == AppInstallStatus.SYSTEM) {
            if (pkg.startsWith("com.android.telephony") ||
                pkg.startsWith("com.android.networkstack") ||
                pkg.startsWith("com.google.android.networkstack")
            ) {
                return true
            }
        }
        return false
    }

    fun classifyControlMode(perm: AppPermissionDetail): PermissionControlMode {
        return when {
            perm.rawName in specialToggleablePermissions ->
                PermissionControlMode.SPECIAL_SETTINGS_CONTROLLABLE
            perm.protectionType == PermissionProtectionType.DANGEROUS &&
                perm.rawName.startsWith("android.permission.") ->
                PermissionControlMode.RUNTIME_CONTROLLABLE
            else -> PermissionControlMode.INSTALL_TIME_INFO_ONLY
        }
    }

    fun buildAdbCommand(
        packageName: String,
        permissionRaw: String,
        grant: Boolean
    ): String {
        val verb = if (grant) "grant" else "revoke"
        return "adb shell pm $verb $packageName $permissionRaw"
    }

    fun buildShizukuStartAdbCommand(): String {
        return "adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh"
    }

    fun resolveExecutionMode(
        shizukuStatus: ShizukuStatusSnapshot,
        isRootAvailable: Boolean
    ): PermissionExecutionMode {
        return when {
            shizukuStatus.connectionState == ShizukuConnectionState.ACTIVE_GRANTED ->
                PermissionExecutionMode.SHIZUKU
            isRootAvailable ->
                PermissionExecutionMode.ROOT
            else ->
                PermissionExecutionMode.MANUAL_ADB
        }
    }

    fun detectShizukuStatus(context: Context): ShizukuStatusSnapshot {
        val now = System.currentTimeMillis()
        val isInstalled = runCatching {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
            true
        }.getOrDefault(false)

        val binderAlive = try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }

        if (!binderAlive) {
            return ShizukuStatusSnapshot(
                connectionState = if (isInstalled) {
                    ShizukuConnectionState.INSTALLED_STOPPED
                } else {
                    ShizukuConnectionState.NOT_INSTALLED
                },
                isInstalled = isInstalled,
                isBinderAlive = false,
                hasPermission = false,
                uid = -1,
                version = -1,
                checkedAtMillis = now
            )
        }

        val hasPermission = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
        val uid = try {
            Shizuku.getUid()
        } catch (_: Throwable) {
            -1
        }
        val version = try {
            Shizuku.getVersion()
        } catch (_: Throwable) {
            -1
        }

        val state = if (hasPermission) {
            ShizukuConnectionState.ACTIVE_GRANTED
        } else {
            ShizukuConnectionState.ACTIVE_NEED_PERMISSION
        }

        return ShizukuStatusSnapshot(
            connectionState = state,
            isInstalled = true,
            isBinderAlive = true,
            hasPermission = hasPermission,
            uid = uid,
            version = version,
            checkedAtMillis = now
        )
    }

    fun requestShizukuPermission(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) return false
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return true
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun isRootAvailable(ioDispatcher: CoroutineDispatcher = Dispatchers.IO): Boolean = withContext(ioDispatcher) {
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "/su/bin/su"
        )
        val hasBinary = suPaths.any { path ->
            runCatching { File(path).exists() }.getOrDefault(false)
        }
        if (!hasBinary) return@withContext false

        runCatching {
            val process = ProcessBuilder("su", "-c", "id")
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(2, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                false
            } else {
                val output = process.inputStream.bufferedReader().readText()
                process.exitValue() == 0 && output.contains("uid=0")
            }
        }.getOrDefault(false)
    }

    suspend fun executePermissionCommand(
        mode: PermissionExecutionMode,
        packageName: String,
        permissionRaw: String,
        grant: Boolean
    ): Result<String> {
        if (!grant && isProtectedCriticalSystemApp(packageName)) {
            return Result.failure(
                IllegalStateException(
                    "Aplikasi Sistem - Dilindungi: izin $packageName tidak boleh dicabut untuk mencegah bootloop."
                )
            )
        }
        return when (mode) {
            PermissionExecutionMode.SHIZUKU -> executeViaShizukuPm(packageName, permissionRaw, grant)
            PermissionExecutionMode.ROOT -> executePackageManagerPermission(packageName, permissionRaw, grant)
            PermissionExecutionMode.MANUAL_ADB -> Result.failure(
                IllegalStateException("Shizuku & Root belum aktif. Gunakan Pengaturan Manual atau ADB.")
            )
        }
    }

    suspend fun executeViaShizukuPm(
        packageName: String,
        permissionRaw: String,
        grant: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(isValidToken(packageName)) { "Nama paket tidak valid" }
            require(isValidToken(permissionRaw)) { "Nama izin tidak valid" }
            if (!Shizuku.pingBinder()) {
                throw IllegalStateException("Layanan Shizuku tidak berjalan.")
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                throw IllegalStateException("Izin akses Shizuku belum diberikan ke ActiPerm.")
            }

            val verb = if (grant) "grant" else "revoke"
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }

            val remoteProcess = newProcessMethod.invoke(
                null,
                arrayOf("pm", verb, packageName, permissionRaw),
                null,
                null
            ) as Process

            val finished = remoteProcess.waitFor(5, TimeUnit.SECONDS)
            if (!finished) {
                remoteProcess.destroyForcibly()
                throw IllegalStateException("Waktu eksekusi Shizuku habis.")
            }
            val stderr = runCatching { remoteProcess.errorStream.bufferedReader().readText().trim() }.getOrDefault("")
            val stdout = runCatching { remoteProcess.inputStream.bufferedReader().readText().trim() }.getOrDefault("")
            if (remoteProcess.exitValue() != 0) {
                val msg = listOf(stderr, stdout).firstOrNull { it.isNotBlank() }
                    ?: "Perintah Shizuku pm $verb gagal (kode ${remoteProcess.exitValue()})."
                throw IllegalStateException(msg)
            }
            if (grant) {
                "Izin berhasil diberikan ke $packageName via Shizuku"
            } else {
                "Izin berhasil dicabut dari $packageName via Shizuku"
            }
        }
    }

    suspend fun executePackageManagerPermission(
        packageName: String,
        permissionRaw: String,
        grant: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(isValidToken(packageName)) { "Nama paket tidak valid" }
            require(isValidToken(permissionRaw)) { "Nama izin tidak valid" }
            val verb = if (grant) "grant" else "revoke"
            val cmd = "pm $verb $packageName $permissionRaw"
            val process = ProcessBuilder("su", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(5, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                throw IllegalStateException("Waktu eksekusi root habis.")
            }
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.exitValue() != 0) {
                val errDetail = output.ifBlank { "Perintah ditolak oleh sistem atau perangkat belum di-root." }
                throw IllegalStateException(errDetail)
            }
            if (grant) {
                "Izin berhasil diberikan ke $packageName via Root"
            } else {
                "Izin berhasil dicabut dari $packageName via Root"
            }
        }
    }

    fun collectBatchCandidates(
        apps: List<InstalledAppInfo>,
        type: BatchRevokeType
    ): BatchRevokeProposal {
        val candidates = ArrayList<BatchRevokeCandidate>()
        var skippedProtected = 0

        for (app in apps) {
            val isProtected = app.isProtectedCriticalSystem ||
                isProtectedCriticalSystemApp(app.packageName, app.isSystemApp, app.installStatus)

            when (type) {
                BatchRevokeType.DANGEROUS_LOCATION -> {
                    if (app.riskLevel != RiskLevel.DANGEROUS) continue
                    val locationPerms = app.permissions.filter { perm ->
                        perm.isGranted &&
                            perm.category == PermissionCategory.LOCATION &&
                            classifyControlMode(perm) == PermissionControlMode.RUNTIME_CONTROLLABLE &&
                            (!perm.isExpectedForApp || perm.rawName == "android.permission.ACCESS_BACKGROUND_LOCATION" || !app.isSystemApp)
                    }
                    if (locationPerms.isNotEmpty()) {
                        if (isProtected) {
                            skippedProtected++
                        } else {
                            locationPerms.forEach { perm ->
                                candidates.add(BatchRevokeCandidate(app = app, permission = perm))
                            }
                        }
                    }
                }
                BatchRevokeType.SUSPICIOUS_CAMERA -> {
                    if (app.riskLevel != RiskLevel.SUSPICIOUS && app.riskLevel != RiskLevel.DANGEROUS) continue
                    val cameraPerms = app.permissions.filter { perm ->
                        perm.isGranted &&
                            perm.category == PermissionCategory.CAMERA &&
                            classifyControlMode(perm) == PermissionControlMode.RUNTIME_CONTROLLABLE
                    }
                    if (cameraPerms.isNotEmpty()) {
                        if (isProtected) {
                            skippedProtected++
                        } else {
                            cameraPerms.forEach { perm ->
                                candidates.add(BatchRevokeCandidate(app = app, permission = perm))
                            }
                        }
                    }
                }
            }
        }

        return BatchRevokeProposal(
            type = type,
            candidates = candidates,
            skippedProtectedCount = skippedProtected
        )
    }

    fun buildBatchAdbScript(proposal: BatchRevokeProposal): String {
        return proposal.candidates.joinToString(separator = "\n") { candidate ->
            buildAdbCommand(candidate.app.packageName, candidate.permission.rawName, grant = false)
        }
    }

    private fun isValidToken(value: String): Boolean {
        return value.isNotBlank() && value.all { ch ->
            ch.isLetterOrDigit() || ch == '.' || ch == '_' || ch == '-'
        }
    }
}
