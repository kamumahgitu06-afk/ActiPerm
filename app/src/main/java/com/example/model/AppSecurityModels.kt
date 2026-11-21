package com.example.model

enum class RiskLevel(val label: String, val priority: Int) {
    DANGEROUS("Berbahaya", 3),
    SUSPICIOUS("Mencurigakan", 2),
    SAFE("Aman", 1)
}

enum class DeviceBrand(val displayName: String) {
    SAMSUNG("Samsung"),
    XIAOMI("Xiaomi / Redmi / POCO"),
    OPPO("Oppo"),
    VIVO("Vivo / iQOO"),
    PIXEL("Google Pixel / AOSP"),
    REALME("Realme"),
    TRANSSION("Infinix / Tecno / Itel"),
    ASUS("Asus"),
    HUAWEI("Huawei / Honor"),
    OTHER("Perangkat Lain")
}

enum class DeviceFilterOption(val label: String, val brand: DeviceBrand?) {
    ALL("Semua Perangkat", null),
    SAMSUNG("Samsung", DeviceBrand.SAMSUNG),
    XIAOMI("Xiaomi", DeviceBrand.XIAOMI),
    OPPO("Oppo", DeviceBrand.OPPO),
    VIVO("Vivo", DeviceBrand.VIVO),
    PIXEL("Pixel", DeviceBrand.PIXEL)
}

enum class AppInstallStatus(val label: String) {
    SYSTEM("Sistem"),
    BUILT_IN("Bawaan"),
    DOWNLOADED("Unduhan")
}

enum class PermissionProtectionType(val label: String) {
    DANGEROUS("Runtime Berbahaya"),
    SPECIAL("Izin Khusus"),
    NORMAL("Normal"),
    SIGNATURE("Signature / Internal")
}

enum class AppFunctionalCategory(val label: String) {
    CHAT_MESSAGING("Chatting & Komunikasi"),
    SOCIAL_MEDIA("Sosial Media"),
    ECOMMERCE("E-Commerce & Belanja"),
    FINANCE_WALLET("Keuangan Aman & Bank Resmi"),
    LOAN_CREDIT("Pinjol & Kredit Digital"),
    TELECOM_OPERATOR("Operator Seluler"),
    HEALTH_MEDICAL("Kesehatan & Medis"),
    IOT_WEARABLE("Perangkat IoT, TWS & Wearable"),
    FILE_MANAGER("Pengelola File & Penyimpanan"),
    CAMERA_MEDIA("Kamera, Foto & Media"),
    MAPS_NAVIGATION("Transportasi & Navigasi"),
    PHONE_SMS_DIALER("Telepon & SMS"),
    CALENDAR_PRODUCTIVITY("Kalender & Produktivitas"),
    SIMPLE_UTILITY("Utilitas Sederhana"),
    CALLER_ID_HARVESTER("Identifikasi Nomor / Pengumpul Kontak"),
    SYSTEM_REASONABLE("Sistem - Wajar"),
    GAME("Permainan"),
    GENERAL("Aplikasi Umum")
}

enum class PermissionCategory(val label: String) {
    LOCATION("Lokasi"),
    CAMERA("Kamera"),
    MICROPHONE("Mikrofon"),
    CONTACTS("Kontak"),
    PHONE_SMS("Telepon & SMS"),
    STORAGE("Penyimpanan & Media"),
    CALENDAR("Kalender"),
    SENSORS("Sensor & Perangkat"),
    SYSTEM_SPECIAL("Izin Khusus Sistem"),
    OTHER("Izin Standar / Internal")
}

enum class SensorFilterType(val label: String) {
    LOCATION("Lokasi"),
    CAMERA("Kamera"),
    MICROPHONE("Mic")
}

enum class AppTab(val label: String) {
    DOWNLOADED("Aplikasi Unduhan"),
    SYSTEM("Aplikasi Bawaan"),
    PERMISSION_MANAGER("Kelola Izin")
}

enum class MainNavTab(val label: String) {
    DASHBOARD("Dasbor"),
    ALL_APPS("Semua Aplikasi"),
    HISTORY("Riwayat"),
    SHIZUKU("Shizuku")
}

enum class PermissionExecutionMode(val label: String, val badgeLabel: String) {
    SHIZUKU("Mode A - Shizuku (Otomatis)", "Bisa Diatur Langsung"),
    ROOT("Mode Root (su pm grant/revoke)", "Bisa Diatur Langsung"),
    MANUAL_ADB("Mode B/C - ADB / Pengaturan Manual", "Butuh Pengaturan Manual")
}

enum class ShizukuConnectionState(val label: String) {
    ACTIVE_GRANTED("Aktif & Izin Diberikan"),
    ACTIVE_NEED_PERMISSION("Aktif, Belum Diizinkan"),
    INSTALLED_STOPPED("Terinstal, Layanan Belum Berjalan"),
    NOT_INSTALLED("Shizuku Belum Terinstal")
}

data class ShizukuStatusSnapshot(
    val connectionState: ShizukuConnectionState = ShizukuConnectionState.NOT_INSTALLED,
    val isInstalled: Boolean = false,
    val isBinderAlive: Boolean = false,
    val hasPermission: Boolean = false,
    val uid: Int = -1,
    val version: Int = -1,
    val checkedAtMillis: Long = 0L
)

data class PermissionChangeLogEntry(
    val id: Long,
    val timestampMillis: Long,
    val appName: String,
    val packageName: String,
    val permissionRaw: String,
    val permissionFriendly: String,
    val granted: Boolean,
    val executionModeLabel: String,
    val success: Boolean = true,
    val note: String = ""
)

enum class BatchRevokeType(val title: String, val description: String) {
    DANGEROUS_LOCATION(
        title = "Cabut Semua Izin Lokasi Berbahaya",
        description = "Mencabut izin akses lokasi aktif pada aplikasi berisiko Berbahaya (kecuali Aplikasi Sistem - Dilindungi)."
    ),
    SUSPICIOUS_CAMERA(
        title = "Cabut Semua Izin Kamera Aplikasi Mencurigakan",
        description = "Mencabut izin kamera aktif pada aplikasi Mencurigakan dan Berbahaya (kecuali Aplikasi Sistem - Dilindungi)."
    )
}

enum class PermissionControlMode(val label: String) {
    RUNTIME_CONTROLLABLE("Izin Runtime (Dapat Diatur)"),
    SPECIAL_SETTINGS_CONTROLLABLE("Izin Khusus Sistem (Dapat Diatur)"),
    INSTALL_TIME_INFO_ONLY("Izin Bawaan / Install-Time")
}

data class AppPermissionDetail(
    val rawName: String,
    val friendlyName: String,
    val description: String,
    val category: PermissionCategory,
    val isGranted: Boolean,
    val isDangerous: Boolean,
    val weight: Int,
    val isExpectedForApp: Boolean = false,
    val isSystemInternal: Boolean = false,
    val protectionType: PermissionProtectionType =
        if (isDangerous) PermissionProtectionType.DANGEROUS else PermissionProtectionType.NORMAL,
    val isAutoRevoked: Boolean = false
)

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val sizeBytes: Long,
    val lastUsedTimeMillis: Long?, // null = izin usage stats belum aktif; 0L = belum pernah dipakai
    val permissions: List<AppPermissionDetail>,
    val dangerousPermissionCount: Int,
    val grantedDangerousCount: Int,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val riskReasons: List<String>,
    val hasLocationPermission: Boolean,
    val hasCameraPermission: Boolean,
    val hasMicrophonePermission: Boolean,
    val functionalCategory: AppFunctionalCategory = AppFunctionalCategory.GENERAL,
    val hasExpectedPermissionsForFunction: Boolean = false,
    val unexpectedDangerousCount: Int = dangerousPermissionCount,
    val installStatus: AppInstallStatus =
        if (isSystemApp) AppInstallStatus.SYSTEM else AppInstallStatus.DOWNLOADED,
    val packageBrand: DeviceBrand = DeviceBrand.OTHER,
    val hasValidReleaseSignature: Boolean = true,
    val isPreInstalled: Boolean = isSystemApp,
    val detectedDeviceBrandName: String = "",
    val androidVersionLabel: String = "",
    val targetSdkVersion: Int = 24,
    val installerLabel: String = "",
    val isProtectedCriticalSystem: Boolean = false
) {
    val hasAnyCoreSensor: Boolean
        get() = hasLocationPermission || hasCameraPermission || hasMicrophonePermission

    val isUnused: Boolean
        get() = lastUsedTimeMillis != null && lastUsedTimeMillis <= 0L
}

data class BatchRevokeCandidate(
    val app: InstalledAppInfo,
    val permission: AppPermissionDetail
)

data class BatchRevokeProposal(
    val type: BatchRevokeType,
    val candidates: List<BatchRevokeCandidate>,
    val skippedProtectedCount: Int = 0
)

enum class BulkItemStatus(val label: String) {
    PENDING("Menunggu"),
    ACTIVE("Sedang Dibuka"),
    DISABLED("Sudah Dinonaktifkan"),
    STILL_ACTIVE("Masih Aktif"),
    SKIPPED("Dilewati")
}

data class BulkQueueEntry(
    val packageName: String,
    val appName: String,
    val status: BulkItemStatus = BulkItemStatus.PENDING
)

data class BulkDisableQueueState(
    val entries: List<BulkQueueEntry>,
    val currentIndex: Int,
    val awaitingReturnFromSettings: Boolean = false,
    val isFinished: Boolean = false
) {
    val currentEntry: BulkQueueEntry?
        get() = entries.getOrNull(currentIndex)

    val completedCount: Int
        get() = entries.count {
            it.status == BulkItemStatus.DISABLED ||
                it.status == BulkItemStatus.STILL_ACTIVE ||
                it.status == BulkItemStatus.SKIPPED
        }
}
