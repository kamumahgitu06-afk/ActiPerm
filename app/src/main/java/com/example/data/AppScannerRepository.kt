package com.example.data

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import android.os.Process
import com.example.domain.PermissionAnalyzer
import com.example.domain.PermissionManagerBridge
import com.example.model.AppInstallStatus
import com.example.model.AppPermissionDetail
import com.example.model.DeviceBrand
import com.example.model.InstalledAppInfo
import com.example.model.PermissionCategory
import com.example.model.PermissionProtectionType
import com.example.model.ShizukuStatusSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

data class ScanSnapshot(
    val apps: List<InstalledAppInfo>,
    val hasUsageAccess: Boolean,
    val scannedAtMillis: Long,
    val detectedDeviceBrand: DeviceBrand = PermissionAnalyzer.detectDeviceBrand(),
    val detectedDeviceBrandName: String = PermissionAnalyzer.detectDeviceBrandDisplayName(),
    val androidVersionLabel: String = PermissionAnalyzer.formatAndroidVersionLabel(),
    val isRootAvailable: Boolean = false,
    val shizukuStatus: ShizukuStatusSnapshot = ShizukuStatusSnapshot()
)

interface AppScannerDataSource {
    suspend fun scanInstalledApps(): ScanSnapshot
    fun hasUsageStatsPermission(): Boolean
    fun checkShizukuStatus(): ShizukuStatusSnapshot = ShizukuStatusSnapshot()
}

class AppScannerRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AppScannerDataSource {

    override fun checkShizukuStatus(): ShizukuStatusSnapshot {
        return PermissionManagerBridge.detectShizukuStatus(context)
    }

    override fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val uid = Process.myUid()
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    override suspend fun scanInstalledApps(): ScanSnapshot = withContext(ioDispatcher) {
        val pm = context.packageManager
        val usageAccess = hasUsageStatsPermission()
        val now = System.currentTimeMillis()
        val lastUsedMap = if (usageAccess) queryLastUsedMap(now) else emptyMap()
        val storageStatsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && usageAccess) {
            context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        } else {
            null
        }

        val detectedBrand = PermissionAnalyzer.detectDeviceBrand()
        val detectedBrandName = PermissionAnalyzer.detectDeviceBrandDisplayName()
        val androidVersionLabel = PermissionAnalyzer.formatAndroidVersionLabel()
        val rootAvailable = PermissionManagerBridge.isRootAvailable(ioDispatcher)
        val shizukuStatus = PermissionManagerBridge.detectShizukuStatus(context)

        val packages = getInstalledPackagesCompat(pm)
        val result = ArrayList<InstalledAppInfo>(packages.size)
        val protectionCache = HashMap<String, PermissionProtectionType?>(128)

        for (pkgInfo in packages) {
            val appInfo = pkgInfo.applicationInfo ?: continue
            val packageName = pkgInfo.packageName ?: continue
            val appName = runCatching {
                pm.getApplicationLabel(appInfo).toString().trim()
            }.getOrDefault(packageName).ifBlank { packageName }

            val installerPkg = resolveInstallerPackage(pm, packageName)
            val preInstalledViaInstaller = isPreInstalledByInstaller(installerPkg)
            val installStatus = classifyInstallStatus(
                packageName = packageName,
                appName = appName,
                flags = appInfo.flags,
                sourceDir = appInfo.sourceDir ?: appInfo.publicSourceDir,
                preInstalledByInstaller = preInstalledViaInstaller
            )
            val isSystem = installStatus != AppInstallStatus.DOWNLOADED
            val hasValidReleaseSignature = verifyReleaseSignature(pkgInfo, appInfo)
            val lastUsed = if (usageAccess) (lastUsedMap[packageName] ?: 0L) else null
            val isUnusedApp = lastUsed != null && lastUsed <= 0L

            val requestedPerms = pkgInfo.requestedPermissions ?: emptyArray()
            val requestedFlags = pkgInfo.requestedPermissionsFlags ?: IntArray(0)

            val mappedPermissions = ArrayList<AppPermissionDetail>(requestedPerms.size)
            for (i in requestedPerms.indices) {
                val permName = requestedPerms[i] ?: continue
                val rawFlag = if (i < requestedFlags.size) requestedFlags[i] else 0
                val isGranted = (rawFlag and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                val protectionFromPm = protectionCache.getOrPut(permName) {
                    resolveProtectionType(pm, permName)
                }
                val isAutoRevoked = !isGranted &&
                    Build.VERSION.SDK_INT >= 35 &&
                    isUnusedApp &&
                    protectionFromPm == PermissionProtectionType.DANGEROUS

                mappedPermissions.add(
                    PermissionAnalyzer.mapPermission(
                        rawName = permName,
                        isGranted = isGranted,
                        protectionTypeFromPm = protectionFromPm,
                        isAutoRevoked = isAutoRevoked
                    )
                )
            }

            // Urutkan izin berbahaya di atas, lalu status diizinkan, lalu alfabetis
            val sortedPermissions = mappedPermissions.sortedWith(
                compareByDescending<AppPermissionDetail> { it.isDangerous }
                    .thenByDescending { it.weight }
                    .thenByDescending { it.isGranted }
                    .thenBy { it.friendlyName }
            )

            val androidCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appInfo.category
            } else {
                -1
            }

            val eval = PermissionAnalyzer.evaluateRisk(
                permissions = sortedPermissions,
                isSystemApp = isSystem,
                packageName = packageName,
                appName = appName,
                androidCategory = androidCategory,
                androidSdkInt = Build.VERSION.SDK_INT,
                hasValidReleaseSignature = hasValidReleaseSignature,
                isUnusedApp = isUnusedApp
            )
            val annotatedPermissions = eval.annotatedPermissions
            val sizeBytes = resolveAppSizeBytes(appInfo, packageName, storageStatsManager)
            val pkgBrand = PermissionAnalyzer.detectPackageVendorBrand(packageName)

            result.add(
                InstalledAppInfo(
                    packageName = packageName,
                    appName = appName,
                    versionName = pkgInfo.versionName ?: "-",
                    isSystemApp = isSystem,
                    isEnabled = appInfo.enabled,
                    sizeBytes = sizeBytes,
                    lastUsedTimeMillis = lastUsed,
                    permissions = annotatedPermissions,
                    dangerousPermissionCount = eval.dangerousCount,
                    grantedDangerousCount = eval.grantedDangerousCount,
                    riskScore = eval.score,
                    riskLevel = eval.level,
                    riskReasons = eval.reasons,
                    hasLocationPermission = annotatedPermissions.any { it.category == PermissionCategory.LOCATION },
                    hasCameraPermission = annotatedPermissions.any { it.category == PermissionCategory.CAMERA },
                    hasMicrophonePermission = annotatedPermissions.any { it.category == PermissionCategory.MICROPHONE },
                    functionalCategory = eval.functionalCategory,
                    hasExpectedPermissionsForFunction = eval.hasExpectedPermissionsForFunction,
                    unexpectedDangerousCount = eval.unexpectedDangerousCount,
                    installStatus = installStatus,
                    packageBrand = pkgBrand,
                    hasValidReleaseSignature = hasValidReleaseSignature,
                    isPreInstalled = isSystem || preInstalledViaInstaller,
                    detectedDeviceBrandName = detectedBrandName,
                    androidVersionLabel = androidVersionLabel,
                    targetSdkVersion = appInfo.targetSdkVersion,
                    installerLabel = installerPkg.orEmpty(),
                    isProtectedCriticalSystem = PermissionManagerBridge.isProtectedCriticalSystemApp(
                        packageName = packageName,
                        isSystemApp = isSystem,
                        installStatus = installStatus
                    )
                )
            )
        }

        val sortedApps = result.sortedWith(
            compareByDescending<InstalledAppInfo> { it.riskScore }
                .thenByDescending { it.dangerousPermissionCount }
                .thenBy { it.appName.lowercase() }
        )

        ScanSnapshot(
            apps = sortedApps,
            hasUsageAccess = usageAccess,
            scannedAtMillis = now,
            detectedDeviceBrand = detectedBrand,
            detectedDeviceBrandName = detectedBrandName,
            androidVersionLabel = androidVersionLabel,
            isRootAvailable = rootAvailable,
            shizukuStatus = shizukuStatus
        )
    }

    private fun getInstalledPackagesCompat(pm: PackageManager): List<PackageInfo> {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES
        }

        // Pada Android 7 (API 24-25), kueri getInstalledPackages dengan banyak flag sekaligus dapat
        // memicu TransactionTooLargeException jika jumlah app banyak; lakukan fallback per paket yang aman.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bulkResult = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledPackages(flags)
                }
            }.getOrNull()
            if (bulkResult != null) return bulkResult
        }

        val apps = runCatching {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }.getOrElse { emptyList() }

        val collected = ArrayList<PackageInfo>(apps.size)
        for (app in apps) {
            val pkgName = app.packageName ?: continue
            val pkgInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(flags.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkgName, flags)
                }
            }.getOrNull() ?: runCatching {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkgName, PackageManager.GET_PERMISSIONS)
            }.getOrNull()
            if (pkgInfo != null) {
                collected.add(pkgInfo)
            }
        }
        return collected
    }

    private fun resolveInstallerPackage(pm: PackageManager, packageName: String): String? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                info.initiatingPackageName ?: info.installingPackageName ?: info.originatingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        }.getOrNull()
    }

    private fun verifyReleaseSignature(pkgInfo: PackageInfo, appInfo: ApplicationInfo): Boolean {
        if ((appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return false
        }
        return runCatching {
            val sigBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = pkgInfo.signingInfo ?: return@runCatching true
                val signers = if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
                signers?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.signatures?.firstOrNull()?.toByteArray()
            }
            if (sigBytes == null || sigBytes.isEmpty()) return@runCatching false

            val certFactory = CertificateFactory.getInstance("X.509")
            val cert = certFactory.generateCertificate(ByteArrayInputStream(sigBytes)) as? X509Certificate
            val subject = cert?.subjectX500Principal?.name.orEmpty()
            !subject.contains("Android Debug", ignoreCase = true)
        }.getOrDefault(true)
    }

    private fun queryLastUsedMap(nowMillis: Long): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val oneYearAgo = nowMillis - 365L * 24L * 60L * 60L * 1000L
        val map = HashMap<String, Long>()

        runCatching {
            val yearlyStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, oneYearAgo, nowMillis)
            yearlyStats?.forEach { stat ->
                val prev = map[stat.packageName] ?: 0L
                if (stat.lastTimeUsed > prev) {
                    map[stat.packageName] = stat.lastTimeUsed
                }
            }
            val recentStats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                nowMillis - 7L * 24L * 60L * 60L * 1000L,
                nowMillis
            )
            recentStats?.forEach { stat ->
                val prev = map[stat.packageName] ?: 0L
                if (stat.lastTimeUsed > prev) {
                    map[stat.packageName] = stat.lastTimeUsed
                }
            }
        }
        return map
    }

    private fun resolveAppSizeBytes(
        appInfo: ApplicationInfo,
        packageName: String,
        storageStatsManager: StorageStatsManager?
    ): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && storageStatsManager != null) {
            val statsSize = runCatching {
                val stats = storageStatsManager.queryStatsForPackage(
                    appInfo.storageUuid,
                    packageName,
                    Process.myUserHandle()
                )
                stats.appBytes + stats.dataBytes + stats.cacheBytes
            }.getOrNull()
            if (statsSize != null && statsSize > 0L) {
                return statsSize
            }
        }

        return runCatching {
            var total = 0L
            appInfo.sourceDir?.let { path ->
                val file = File(path)
                if (file.exists()) total += file.length()
            }
            appInfo.splitSourceDirs?.forEach { splitPath ->
                val splitFile = File(splitPath)
                if (splitFile.exists()) total += splitFile.length()
            }
            total
        }.getOrDefault(0L)
    }

    private fun resolveProtectionType(pm: PackageManager, permName: String): PermissionProtectionType? {
        return runCatching {
            val info = pm.getPermissionInfo(permName, 0)
            val baseProtection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.protection
            } else {
                @Suppress("DEPRECATION")
                info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
            }
            when (baseProtection) {
                PermissionInfo.PROTECTION_DANGEROUS -> PermissionProtectionType.DANGEROUS
                PermissionInfo.PROTECTION_SIGNATURE,
                @Suppress("DEPRECATION")
                PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM -> PermissionProtectionType.SIGNATURE
                else -> PermissionProtectionType.NORMAL
            }
        }.getOrNull()
    }

    companion object {
        private val knownBuiltInOemPackages = setOf(
            // Samsung One UI
            "com.sec.android.app.voicenote",
            "com.samsung.android.app.voicenote",
            "com.samsung.android.app.notes",
            "com.samsung.android.notes",
            "com.sec.android.app.clockpackage",
            "com.samsung.android.app.clockpackage",
            "com.sec.android.app.popupcalculator",
            "com.samsung.android.calendar",
            "com.sec.android.gallery3d",
            "com.sec.android.app.myfiles",
            "com.samsung.android.messaging",
            "com.samsung.android.dialer",
            "com.samsung.android.contacts",
            "com.samsung.android.scs",
            "com.samsung.android.app.reminder",
            "com.samsung.android.app.sbrowser",
            "com.sec.android.app.sbrowser",
            // Xiaomi / MIUI / HyperOS
            "com.miui.notes",
            "com.android.soundrecorder",
            "com.miui.calculator",
            "com.miui.securitycenter",
            "com.miui.gallery",
            "com.miui.player",
            "com.miui.weather2",
            "com.mi.android.globalFileexplorer",
            "com.xiaomi.scanner",
            // Oppo / ColorOS & Realme
            "com.coloros.soundrecorder",
            "com.coloros.note",
            "com.coloros.calculator",
            "com.coloros.alarmclock",
            "com.coloros.filemanager",
            "com.coloros.gallery3d",
            "com.coloros.safecenter",
            "com.heytap.browser",
            "com.oppo.camera",
            // Vivo / iQOO
            "com.vivo.notes",
            "com.android.bbksoundrecorder",
            "com.vivo.calculator",
            "com.vivo.weather",
            "com.iqoo.secure",
            // Pixel / AOSP / Google
            "com.google.android.apps.recorder",
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.android.shell",
            "com.android.settings",
            "com.android.systemui",
            "com.google.android.gms",
            "com.google.android.gsf",
            // Asus / Huawei / Transsion
            "com.asus.soundrecorder",
            "com.asus.calculator",
            "com.asus.deskclock",
            "com.huawei.himovie",
            "com.huawei.notepad",
            "com.transsion.soundrecorder",
            "com.transsion.calculator"
        )

        private val alwaysBuiltInPrefixes = listOf(
            "com.android.",
            "com.sec.",
            "com.samsung.android.",
            "com.miui.",
            "com.coloros.",
            "com.heytap.",
            "com.bbk.",
            "com.iqoo.",
            "com.rust.",
            "com.transsion.",
            "com.qualcomm.",
            "com.qti.",
            "com.mediatek."
        )

        private val oemEcosystemPrefixes = listOf(
            "com.samsung.",
            "com.xiaomi.",
            "com.mi.",
            "com.oppo.",
            "com.vivo.",
            "com.realme.",
            "com.infinix.",
            "com.tecno.",
            "com.itel.",
            "com.asus.",
            "com.huawei.",
            "com.honor."
        )

        private val systemPartitionPrefixes = listOf(
            "/system/",
            "/system/priv-app/",
            "/system_ext/",
            "/product/",
            "/vendor/",
            "/odm/",
            "/apex/",
            "/oem/"
        )

        private val coreNonUtilityNames = listOf(
            "shell", "ui sistem", "system ui", "pengaturan", "settings",
            "layanan google play", "google play services", "core services",
            "security center", "keamanan sistem"
        )

        private val builtInUtilityNameKeywords = listOf(
            "perekam suara", "voice recorder", "sound recorder",
            "samsung notes", "catatan", "notes",
            "jam", "clock", "alarm",
            "kalkulator", "calculator",
            "kalender", "calendar",
            "file saya", "my files", "pengelola file"
        )

        fun isPreInstalledByInstaller(installerPackage: String?): Boolean {
            if (installerPackage.isNullOrBlank()) return false
            val lower = installerPackage.lowercase()
            return "preload" in lower || "factory" in lower || "ota" in lower || "system" in lower
        }

        fun classifyInstallStatus(
            packageName: String,
            appName: String,
            flags: Int,
            sourceDir: String? = null,
            preInstalledByInstaller: Boolean = false
        ): AppInstallStatus {
            val dirLower = sourceDir?.lowercase().orEmpty()
            val pkg = packageName.lowercase()
            val name = appName.lowercase()

            val hasSystemFlag = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val hasUpdatedSystemFlag = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val inPrivPartition = dirLower.startsWith("/system/priv-app/") ||
                dirLower.startsWith("/system_ext/") ||
                dirLower.startsWith("/vendor/") ||
                dirLower.startsWith("/odm/") ||
                dirLower.startsWith("/apex/") ||
                dirLower.startsWith("/oem/")
            val inAnySystemPartition = systemPartitionPrefixes.any { dirLower.startsWith(it) }

            val isBuiltInUtilityName = builtInUtilityNameKeywords.any { name.contains(it) }
            val isCoreBuiltIn = pkg == "android" ||
                pkg in knownBuiltInOemPackages ||
                alwaysBuiltInPrefixes.any { pkg.startsWith(it) } ||
                (oemEcosystemPrefixes.any { pkg.startsWith(it) } && isBuiltInUtilityName) ||
                (pkg.startsWith("com.google.android.") && (isBuiltInUtilityName || hasSystemFlag || hasUpdatedSystemFlag || inAnySystemPartition))

            if (inPrivPartition || coreNonUtilityNames.any { name == it } || pkg == "android" || pkg == "com.android.shell" || pkg == "com.android.systemui" || pkg == "com.android.settings" || pkg == "com.google.android.gms") {
                return AppInstallStatus.SYSTEM
            }

            if (hasUpdatedSystemFlag || preInstalledByInstaller) {
                return AppInstallStatus.BUILT_IN
            }

            if (inAnySystemPartition) {
                return if (isBuiltInUtilityName || dirLower.startsWith("/product/")) {
                    AppInstallStatus.BUILT_IN
                } else {
                    AppInstallStatus.SYSTEM
                }
            }

            if (hasSystemFlag) {
                return if (isBuiltInUtilityName) AppInstallStatus.BUILT_IN else AppInstallStatus.SYSTEM
            }

            if (isCoreBuiltIn) {
                return if (isBuiltInUtilityName || pkg in knownBuiltInOemPackages) {
                    AppInstallStatus.BUILT_IN
                } else {
                    AppInstallStatus.SYSTEM
                }
            }

            return AppInstallStatus.DOWNLOADED
        }

        fun isBuiltInSystemApp(
            packageName: String,
            appName: String,
            flags: Int,
            sourceDir: String? = null,
            preInstalledByInstaller: Boolean = false
        ): Boolean {
            return classifyInstallStatus(
                packageName = packageName,
                appName = appName,
                flags = flags,
                sourceDir = sourceDir,
                preInstalledByInstaller = preInstalledByInstaller
            ) != AppInstallStatus.DOWNLOADED
        }
    }
}
