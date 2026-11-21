package com.example.domain

import android.content.pm.ApplicationInfo
import android.os.Build
import com.example.model.AppFunctionalCategory
import com.example.model.AppPermissionDetail
import com.example.model.DeviceBrand
import com.example.model.PermissionCategory
import com.example.model.PermissionProtectionType
import com.example.model.RiskLevel
import kotlin.math.min
import kotlin.math.roundToInt

data class RiskEvaluation(
    val score: Int,
    val level: RiskLevel,
    val reasons: List<String>,
    val dangerousCount: Int,
    val grantedDangerousCount: Int,
    val unexpectedDangerousCount: Int,
    val functionalCategory: AppFunctionalCategory,
    val hasExpectedPermissionsForFunction: Boolean,
    val annotatedPermissions: List<AppPermissionDetail>
)

object PermissionAnalyzer {

    private data class PermissionMetadata(
        val friendlyName: String,
        val description: String,
        val category: PermissionCategory,
        val protectionType: PermissionProtectionType,
        val weight: Int
    ) {
        val isDangerous: Boolean
            get() = protectionType == PermissionProtectionType.DANGEROUS ||
                protectionType == PermissionProtectionType.SPECIAL
    }

    private val samsungPrefixes = listOf("com.samsung.", "com.sec.", "com.samsung.android.")
    private val xiaomiPrefixes = listOf("com.miui.", "com.xiaomi.", "com.mi.")
    private val oppoPrefixes = listOf("com.coloros.", "com.oppo.", "com.heytap.")
    private val vivoPrefixes = listOf("com.vivo.", "com.bbk.", "com.iqoo.")
    private val realmePrefixes = listOf("com.realme.", "com.rust.")
    private val transsionPrefixes = listOf("com.transsion.", "com.infinix.", "com.tecno.", "com.itel.")
    private val asusPrefixes = listOf("com.asus.")
    private val pixelAospPrefixes = listOf("com.google.android.", "com.android.")
    private val huaweiHonorPrefixes = listOf("com.huawei.", "com.honor.")
    private val hardwareVendorPrefixes = listOf("com.qualcomm.", "com.qti.", "com.mediatek.")

    private val chatPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.telegram.plus",
        "com.facebook.orca",
        "com.facebook.mlite",
        "jp.naver.line.android",
        "com.discord",
        "com.viber.voip",
        "org.thoughtcrime.securesms",
        "com.tencent.mm",
        "com.zing.zalo",
        "com.google.android.apps.dynamite",
        "com.google.android.apps.tachyon",
        "us.zoom.videomeetings",
        "com.microsoft.teams",
        "com.skype.raider",
        "com.kakao.talk",
        "kik.android"
    )

    private val socialPackages = setOf(
        "com.facebook.katana",
        "com.facebook.lite",
        "com.instagram.android",
        "com.instagram.lite",
        "com.instagram.barcelona",
        "com.twitter.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.snapchat.android",
        "com.linkedin.android",
        "com.pinterest",
        "com.reddit.frontpage"
    )

    private val ecommercePackages = setOf(
        "com.shopee.id",
        "com.shopee.my",
        "com.shopee.sg",
        "com.tokopedia.tkpd",
        "com.lazada.android",
        "blibli.mobile.commerce",
        "com.bukalapak.android",
        "com.jd.id",
        "com.zalora.android",
        "com.traveloka.android",
        "com.tiket.gits",
        "com.sociolla.app",
        "com.alfamart.alfagift",
        "com.indomaret.klikindomaret",
        "com.tiktok.shop",
        "com.bytedance.tiktok.seller"
    )

    private val officialFinancePackages = setOf(
        "id.dana",
        "ovo.id",
        "com.gojek.gopay",
        "com.linkaja.android",
        "com.bca",
        "src.com.bca",
        "com.bca.mybca.omni.android",
        "id.co.bri.brimo",
        "com.bri.brimo",
        "com.bankmandiri.mandirionline",
        "id.bmri.livin",
        "src.com.bni",
        "id.co.bni.wondr",
        "com.jago.digitalBanking",
        "id.co.bankraya.raya",
        "com.seabank.id",
        "com.cimbniaga.octomobile",
        "id.co.bankmega.msmile",
        "com.bsi.mobile",
        "id.co.btn.mobilebanking",
        "com.flip.id",
        "id.flip",
        "com.bibit.bibitid",
        "com.ajaib.android",
        "com.paypal.android.p2pmobile"
    )

    private val loanPackages = setOf(
        "com.finaccel.android",
        "com.akulaku.indonesia",
        "io.silvrr.installment",
        "com.kreditpintar",
        "com.adakami.android",
        "com.rupiahcepat.cash",
        "com.indodana.app",
        "com.easycash",
        "com.julo.android",
        "com.homecredit.id",
        "com.uangme",
        "com.tunaiku",
        "com.ada.pundi",
        "com.cairin",
        "com.pinjamduit"
    )

    private val telecomOperatorPackages = setOf(
        "com.pure.indosat.care",
        "com.axis.net",
        "com.telkomsel.telkomselcm",
        "com.xl.myxl",
        "com.tri.bimaplus",
        "com.smartfren",
        "com.byu.id"
    )

    private val healthPackages = setOf(
        "com.telkom.tracencare",
        "id.go.kemkes.satusehat",
        "com.linkdokter.halodoc.android",
        "com.alodokter.android",
        "com.klikdokter",
        "com.gooddoctor.android",
        "app.bpjs.mobile"
    )

    private val transportPackages = setOf(
        "com.gojek.app",
        "com.grabtaxi.passenger",
        "com.taxsee.taxsee",
        "sinet.startup.inDriver",
        "com.bluebird.mobile",
        "com.waze",
        "com.google.android.apps.maps",
        "com.ubercab"
    )

    private val callerIdHarvesterPackages = setOf(
        "app.source.getcontact",
        "com.truecaller",
        "com.syncme.syncmeapp"
    )

    private val knownPermissions: Map<String, PermissionMetadata> = mapOf(
        // Lokasi
        "android.permission.ACCESS_FINE_LOCATION" to PermissionMetadata(
            "Lokasi Akurat (GPS)",
            "Melacak posisi GPS perangkat secara presisi.",
            PermissionCategory.LOCATION,
            PermissionProtectionType.DANGEROUS,
            weight = 14
        ),
        "android.permission.ACCESS_COARSE_LOCATION" to PermissionMetadata(
            "Lokasi Perkiraan",
            "Mengetahui perkiraan lokasi dari jaringan seluler/Wi-Fi.",
            PermissionCategory.LOCATION,
            PermissionProtectionType.DANGEROUS,
            weight = 8
        ),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to PermissionMetadata(
            "Lokasi di Latar Belakang",
            "Mengakses lokasi secara terus-menerus meski aplikasi ditutup.",
            PermissionCategory.LOCATION,
            PermissionProtectionType.DANGEROUS,
            weight = 22
        ),
        // Kamera
        "android.permission.CAMERA" to PermissionMetadata(
            "Akses Kamera",
            "Mengambil foto dan merekam video melalui kamera.",
            PermissionCategory.CAMERA,
            PermissionProtectionType.DANGEROUS,
            weight = 15
        ),
        // Mikrofon
        "android.permission.RECORD_AUDIO" to PermissionMetadata(
            "Rekam Audio / Mikrofon",
            "Merekam suara melalui mikrofon perangkat.",
            PermissionCategory.MICROPHONE,
            PermissionProtectionType.DANGEROUS,
            weight = 16
        ),
        // Kontak
        "android.permission.READ_CONTACTS" to PermissionMetadata(
            "Baca Daftar Kontak",
            "Melihat nama, nomor telepon, dan email di buku kontak.",
            PermissionCategory.CONTACTS,
            PermissionProtectionType.DANGEROUS,
            weight = 14
        ),
        "android.permission.WRITE_CONTACTS" to PermissionMetadata(
            "Ubah Daftar Kontak",
            "Menambah, mengubah, atau menghapus data kontak.",
            PermissionCategory.CONTACTS,
            PermissionProtectionType.DANGEROUS,
            weight = 14
        ),
        "android.permission.GET_ACCOUNTS" to PermissionMetadata(
            "Lihat Akun Perangkat",
            "Membaca daftar akun Google atau layanan lain di HP.",
            PermissionCategory.CONTACTS,
            PermissionProtectionType.DANGEROUS,
            weight = 6
        ),
        // Telepon & SMS
        "android.permission.READ_SMS" to PermissionMetadata(
            "Baca Pesan SMS",
            "Membaca isi SMS masuk termasuk kode OTP.",
            PermissionCategory.PHONE_SMS,
            PermissionProtectionType.DANGEROUS,
            weight = 18
        ),
        "android.permission.RECEIVE_SMS" to PermissionMetadata(
            "Terima & Pantau SMS",
            "Memantau pesan SMS baru yang masuk secara langsung.",
            PermissionCategory.PHONE_SMS,
            PermissionProtectionType.DANGEROUS,
            weight = 18
        ),
        "android.permission.SEND_SMS" to PermissionMetadata(
            "Kirim Pesan SMS",
            "Mengirim SMS yang berpotensi memotong pulsa.",
            PermissionCategory.PHONE_SMS,
            PermissionProtectionType.DANGEROUS,
            weight = 20
        ),
        "android.permission.READ_CALL_LOG" to PermissionMetadata(
            "Baca Riwayat Panggilan",
            "Melihat log panggilan masuk dan keluar.",
            PermissionCategory.PHONE_SMS,
            PermissionProtectionType.DANGEROUS,
            weight = 18
        ),
        "android.permission.WRITE_CALL_LOG" to PermissionMetadata(
            "Ubah Riwayat Panggilan",
            "Menghapus atau memodifikasi riwayat panggilan telepon.",
            PermissionCategory.PHONE_SMS,
            PermissionProtectionType.DANGEROUS,
            weight = 18
        ),
        "android.permission.CALL_PHONE" to PermissionMetadata(
            "Lakukan Panggilan Telepon",
            "Memulai panggilan telepon langsung tanpa dialer.",
            PermissionCategory.PHONE_SMS,
            PermissionProtectionType.DANGEROUS,
            weight = 12
        ),
        "android.permission.READ_PHONE_STATE" to PermissionMetadata(
            "Status & Identitas Telepon",
            "Membaca status jaringan, status panggilan aktif, dan identitas perangkat.",
            PermissionCategory.PHONE_SMS,
            PermissionProtectionType.DANGEROUS,
            weight = 8
        ),
        "android.permission.READ_PHONE_NUMBERS" to PermissionMetadata(
            "Baca Nomor Telepon",
            "Membaca nomor SIM yang terpasang di perangkat.",
            PermissionCategory.PHONE_SMS,
            PermissionProtectionType.DANGEROUS,
            weight = 9
        ),
        "android.permission.ANSWER_PHONE_CALLS" to PermissionMetadata(
            "Jawab Panggilan Otomatis",
            "Menerima panggilan telepon masuk secara terprogram.",
            PermissionCategory.PHONE_SMS,
            PermissionProtectionType.DANGEROUS,
            weight = 10
        ),
        // Penyimpanan & Media
        "android.permission.MANAGE_EXTERNAL_STORAGE" to PermissionMetadata(
            "Akses Penuh Semua File",
            "Membaca dan menulis seluruh file di penyimpanan internal.",
            PermissionCategory.STORAGE,
            PermissionProtectionType.SPECIAL,
            weight = 18
        ),
        "android.permission.READ_EXTERNAL_STORAGE" to PermissionMetadata(
            "Baca Penyimpanan",
            "Membaca foto, video, dan dokumen di memori perangkat.",
            PermissionCategory.STORAGE,
            PermissionProtectionType.DANGEROUS,
            weight = 10
        ),
        "android.permission.WRITE_EXTERNAL_STORAGE" to PermissionMetadata(
            "Tulis Penyimpanan",
            "Menyimpan atau mengubah file di memori perangkat.",
            PermissionCategory.STORAGE,
            PermissionProtectionType.DANGEROUS,
            weight = 10
        ),
        "android.permission.READ_MEDIA_IMAGES" to PermissionMetadata(
            "Baca Galeri Foto",
            "Mengakses koleksi foto di penyimpanan perangkat (Android 13+).",
            PermissionCategory.STORAGE,
            PermissionProtectionType.DANGEROUS,
            weight = 6
        ),
        "android.permission.READ_MEDIA_VIDEO" to PermissionMetadata(
            "Baca Galeri Video",
            "Mengakses koleksi video di penyimpanan perangkat (Android 13+).",
            PermissionCategory.STORAGE,
            PermissionProtectionType.DANGEROUS,
            weight = 6
        ),
        "android.permission.READ_MEDIA_AUDIO" to PermissionMetadata(
            "Baca File Audio",
            "Mengakses musik dan rekaman suara di penyimpanan (Android 13+).",
            PermissionCategory.STORAGE,
            PermissionProtectionType.DANGEROUS,
            weight = 5
        ),
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to PermissionMetadata(
            "Baca Media Terpilih",
            "Mengakses foto/video yang dipilih pengguna secara selektif (Android 14+).",
            PermissionCategory.STORAGE,
            PermissionProtectionType.DANGEROUS,
            weight = 3
        ),
        // Kalender
        "android.permission.READ_CALENDAR" to PermissionMetadata(
            "Baca Jadwal Kalender",
            "Melihat acara, pengingat, dan undangan kalender.",
            PermissionCategory.CALENDAR,
            PermissionProtectionType.DANGEROUS,
            weight = 9
        ),
        "android.permission.WRITE_CALENDAR" to PermissionMetadata(
            "Ubah Jadwal Kalender",
            "Menambah atau mengubah acara di kalender.",
            PermissionCategory.CALENDAR,
            PermissionProtectionType.DANGEROUS,
            weight = 9
        ),
        // Sensor & Perangkat Terdekat
        "android.permission.BODY_SENSORS" to PermissionMetadata(
            "Sensor Tubuh & Kesehatan",
            "Mengakses data detak jantung atau sensor biometrik.",
            PermissionCategory.SENSORS,
            PermissionProtectionType.DANGEROUS,
            weight = 11
        ),
        "android.permission.ACTIVITY_RECOGNITION" to PermissionMetadata(
            "Deteksi Aktivitas Fisik",
            "Mendeteksi saat Anda berjalan, berlari, atau berkendara.",
            PermissionCategory.SENSORS,
            PermissionProtectionType.DANGEROUS,
            weight = 8
        ),
        "android.permission.BLUETOOTH_SCAN" to PermissionMetadata(
            "Pindai Perangkat Bluetooth",
            "Mencari perangkat Bluetooth di sekitar Anda (Android 12+).",
            PermissionCategory.SENSORS,
            PermissionProtectionType.DANGEROUS,
            weight = 6
        ),
        "android.permission.BLUETOOTH_CONNECT" to PermissionMetadata(
            "Hubungkan Bluetooth",
            "Terhubung dengan perangkat Bluetooth / TWS (Android 12+).",
            PermissionCategory.SENSORS,
            PermissionProtectionType.DANGEROUS,
            weight = 4
        ),
        "android.permission.BLUETOOTH_ADVERTISE" to PermissionMetadata(
            "Siaran Bluetooth",
            "Mengumumkan perangkat ke perangkat Bluetooth di sekitar.",
            PermissionCategory.SENSORS,
            PermissionProtectionType.DANGEROUS,
            weight = 4
        ),
        "android.permission.NEARBY_WIFI_DEVICES" to PermissionMetadata(
            "Perangkat Wi-Fi Terdekat",
            "Menemukan dan terhubung ke perangkat Wi-Fi / IoT sekitar.",
            PermissionCategory.SENSORS,
            PermissionProtectionType.DANGEROUS,
            weight = 6
        ),
        // Izin Sistem Khusus
        "android.permission.SYSTEM_ALERT_WINDOW" to PermissionMetadata(
            "Tampil di Atas Aplikasi Lain",
            "Menampilkan jendela melayang (overlay) di atas layar aplikasi lain.",
            PermissionCategory.SYSTEM_SPECIAL,
            PermissionProtectionType.SPECIAL,
            weight = 16
        ),
        "android.permission.REQUEST_INSTALL_PACKAGES" to PermissionMetadata(
            "Pasang Aplikasi (APK)",
            "Meminta izin untuk menginstal paket aplikasi lain.",
            PermissionCategory.SYSTEM_SPECIAL,
            PermissionProtectionType.SPECIAL,
            weight = 18
        ),
        "android.permission.PACKAGE_USAGE_STATS" to PermissionMetadata(
            "Akses Statistik Penggunaan",
            "Memantau riwayat aplikasi yang dibuka dan durasinya.",
            PermissionCategory.SYSTEM_SPECIAL,
            PermissionProtectionType.SPECIAL,
            weight = 12
        ),
        "android.permission.WRITE_SETTINGS" to PermissionMetadata(
            "Ubah Pengaturan Sistem",
            "Memodifikasi konfigurasi sistem perangkat secara langsung.",
            PermissionCategory.SYSTEM_SPECIAL,
            PermissionProtectionType.SPECIAL,
            weight = 12
        ),
        "android.permission.QUERY_ALL_PACKAGES" to PermissionMetadata(
            "Lihat Semua Aplikasi Terinstal",
            "Membaca daftar seluruh aplikasi yang terpasang di perangkat.",
            PermissionCategory.SYSTEM_SPECIAL,
            PermissionProtectionType.SPECIAL,
            weight = 9
        ),
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to PermissionMetadata(
            "Layanan Aksesibilitas",
            "Dapat membaca isi layar dan melakukan interaksi otomatis.",
            PermissionCategory.SYSTEM_SPECIAL,
            PermissionProtectionType.SPECIAL,
            weight = 24
        ),
        // Android 13/14+ Notifikasi, Alarm & Full Screen Intent (Normal/Wajar = bobot 0)
        "android.permission.POST_NOTIFICATIONS" to PermissionMetadata(
            "Tampilkan Notifikasi",
            "Mengirim pemberitahuan di bilah status (wajar pada Android 13+).",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        "android.permission.SCHEDULE_EXACT_ALARM" to PermissionMetadata(
            "Jadwalkan Alarm Tepat Waktu",
            "Menjalankan alarm atau pengingat pada waktu yang tepat (Android 14+).",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        "android.permission.USE_EXACT_ALARM" to PermissionMetadata(
            "Gunakan Alarm Presisi",
            "Memicu alarm tepat waktu untuk aplikasi jam/kalender (Android 14+).",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        "android.permission.USE_FULL_SCREEN_INTENT" to PermissionMetadata(
            "Tampilan Layar Penuh (Alarm/Panggilan)",
            "Menampilkan alarm atau panggilan masuk saat layar terkunci (Android 14+).",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        // Izin Normal Umum (bobot 0)
        "android.permission.INTERNET" to PermissionMetadata(
            "Akses Internet",
            "Membuka koneksi jaringan dan mengirim/menerima data.",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        "android.permission.ACCESS_NETWORK_STATE" to PermissionMetadata(
            "Lihat Status Jaringan",
            "Memeriksa apakah perangkat tersambung ke Wi-Fi atau seluler.",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        "android.permission.ACCESS_WIFI_STATE" to PermissionMetadata(
            "Lihat Status Wi-Fi",
            "Membaca informasi koneksi Wi-Fi aktif.",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        "android.permission.VIBRATE" to PermissionMetadata(
            "Kontrol Getar",
            "Mengaktifkan motor getar perangkat.",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        "android.permission.WAKE_LOCK" to PermissionMetadata(
            "Cegah Layar Tidur",
            "Menjaga prosesor atau layar tetap menyala saat aktif.",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        "android.permission.RECEIVE_BOOT_COMPLETED" to PermissionMetadata(
            "Jalankan Saat HP Menyala",
            "Memulai layanan otomatis ketika perangkat dinyalakan.",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        "android.permission.FOREGROUND_SERVICE" to PermissionMetadata(
            "Layanan Latar Depan",
            "Menjalankan tugas berjalan terus dengan notifikasi aktif.",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        ),
        "android.permission.USE_BIOMETRIC" to PermissionMetadata(
            "Autentikasi Biometrik",
            "Menggunakan sidik jari atau pengenalan wajah untuk login.",
            PermissionCategory.OTHER,
            PermissionProtectionType.NORMAL,
            weight = 0
        )
    )

    private val systemSpecialPermissionSet = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.WRITE_SETTINGS",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.MANAGE_EXTERNAL_STORAGE"
    )

    fun detectDeviceBrand(
        manufacturer: String = Build.MANUFACTURER.orEmpty(),
        brand: String = Build.BRAND.orEmpty()
    ): DeviceBrand {
        val combined = "${manufacturer.lowercase()} ${brand.lowercase()}"
        return when {
            "samsung" in combined -> DeviceBrand.SAMSUNG
            "xiaomi" in combined || "redmi" in combined || "poco" in combined || "blackshark" in combined ->
                DeviceBrand.XIAOMI
            "oppo" in combined || "oneplus" in combined -> DeviceBrand.OPPO
            "vivo" in combined || "iqoo" in combined -> DeviceBrand.VIVO
            "realme" in combined -> DeviceBrand.REALME
            "infinix" in combined || "tecno" in combined || "itel" in combined || "transsion" in combined ->
                DeviceBrand.TRANSSION
            "asus" in combined -> DeviceBrand.ASUS
            "huawei" in combined || "honor" in combined -> DeviceBrand.HUAWEI
            "google" in combined || "pixel" in combined -> DeviceBrand.PIXEL
            else -> DeviceBrand.OTHER
        }
    }

    fun detectDeviceBrandDisplayName(
        manufacturer: String = Build.MANUFACTURER.orEmpty(),
        brand: String = Build.BRAND.orEmpty()
    ): String {
        val detected = detectDeviceBrand(manufacturer, brand)
        val rawMfg = manufacturer.trim()
        return if (detected != DeviceBrand.OTHER) {
            if (rawMfg.isNotBlank() && !detected.displayName.contains(rawMfg, ignoreCase = true)) {
                "${detected.displayName} ($rawMfg)"
            } else {
                detected.displayName
            }
        } else {
            rawMfg.ifBlank { brand.ifBlank { "Android" } }
                .replaceFirstChar { it.uppercaseChar() }
        }
    }

    fun formatAndroidVersionLabel(
        sdkInt: Int = Build.VERSION.SDK_INT,
        release: String = Build.VERSION.RELEASE.orEmpty()
    ): String {
        val rel = release.ifBlank {
            when (sdkInt) {
                24 -> "7.0"
                25 -> "7.1"
                26 -> "8.0"
                27 -> "8.1"
                28 -> "9"
                29 -> "10"
                30 -> "11"
                31 -> "12"
                32 -> "12L"
                33 -> "13"
                34 -> "14"
                35 -> "15"
                36 -> "16"
                else -> "SDK $sdkInt"
            }
        }
        return "Android $rel (API $sdkInt)"
    }

    fun detectPackageVendorBrand(packageName: String): DeviceBrand {
        val pkg = packageName.lowercase()
        return when {
            samsungPrefixes.any { pkg.startsWith(it) } -> DeviceBrand.SAMSUNG
            xiaomiPrefixes.any { pkg.startsWith(it) } -> DeviceBrand.XIAOMI
            oppoPrefixes.any { pkg.startsWith(it) } -> DeviceBrand.OPPO
            vivoPrefixes.any { pkg.startsWith(it) } -> DeviceBrand.VIVO
            realmePrefixes.any { pkg.startsWith(it) } -> DeviceBrand.REALME
            transsionPrefixes.any { pkg.startsWith(it) } -> DeviceBrand.TRANSSION
            asusPrefixes.any { pkg.startsWith(it) } -> DeviceBrand.ASUS
            huaweiHonorPrefixes.any { pkg.startsWith(it) } -> DeviceBrand.HUAWEI
            pkg == "android" || pixelAospPrefixes.any { pkg.startsWith(it) } -> DeviceBrand.PIXEL
            else -> DeviceBrand.OTHER
        }
    }

    fun isCoreSystemOrOemPackage(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return detectPackageVendorBrand(pkg) != DeviceBrand.OTHER ||
            hardwareVendorPrefixes.any { pkg.startsWith(it) }
    }

    fun mapPermission(
        rawName: String,
        isGranted: Boolean,
        isRuntimeDangerousFromPm: Boolean? = null,
        protectionTypeFromPm: PermissionProtectionType? = null,
        isAutoRevoked: Boolean = false
    ): AppPermissionDetail {
        val meta = knownPermissions[rawName]
        if (meta != null) {
            val effectiveProtection = when {
                meta.protectionType == PermissionProtectionType.SPECIAL -> PermissionProtectionType.SPECIAL
                protectionTypeFromPm == PermissionProtectionType.SIGNATURE -> PermissionProtectionType.SIGNATURE
                else -> meta.protectionType
            }
            val effectiveDangerous = if (effectiveProtection == PermissionProtectionType.DANGEROUS ||
                effectiveProtection == PermissionProtectionType.SPECIAL
            ) {
                !isAutoRevoked && meta.isDangerous
            } else {
                false
            }
            return AppPermissionDetail(
                rawName = rawName,
                friendlyName = meta.friendlyName,
                description = meta.description,
                category = meta.category,
                isGranted = isGranted,
                isDangerous = effectiveDangerous,
                weight = if (effectiveDangerous) meta.weight else 0,
                isSystemInternal = false,
                protectionType = effectiveProtection,
                isAutoRevoked = isAutoRevoked
            )
        }

        val isOfficialAndroidPerm = rawName.startsWith("android.permission.")
        val isSpecial = rawName in systemSpecialPermissionSet
        val isInternalVendorPerm = !isOfficialAndroidPerm
        val inferredCategory = if (isInternalVendorPerm) PermissionCategory.OTHER else inferCategory(rawName)

        val resolvedProtection = when {
            isSpecial -> PermissionProtectionType.SPECIAL
            isInternalVendorPerm -> PermissionProtectionType.SIGNATURE
            protectionTypeFromPm != null -> protectionTypeFromPm
            isRuntimeDangerousFromPm == true -> PermissionProtectionType.DANGEROUS
            isRuntimeDangerousFromPm == false -> PermissionProtectionType.NORMAL
            inferredCategory != PermissionCategory.OTHER && isOfficialRuntimeLike(rawName) ->
                PermissionProtectionType.DANGEROUS
            else -> PermissionProtectionType.NORMAL
        }

        val isDangerous = !isAutoRevoked && (
            resolvedProtection == PermissionProtectionType.DANGEROUS ||
                resolvedProtection == PermissionProtectionType.SPECIAL
            )

        val shortName = rawName.substringAfterLast('.')
        val readableName = shortName
            .lowercase()
            .split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch -> ch.uppercaseChar() }
            }
            .ifBlank { rawName }

        return AppPermissionDetail(
            rawName = rawName,
            friendlyName = readableName,
            description = when {
                isInternalVendorPerm || resolvedProtection == PermissionProtectionType.SIGNATURE ->
                    "Izin signature/internal sistem atau layanan komponen ($shortName)."
                isDangerous -> "Izin akses terkait ${inferredCategory.label.lowercase()} ($shortName)."
                else -> "Izin normal/standar sistem ($shortName)."
            },
            category = inferredCategory,
            isGranted = isGranted,
            isDangerous = isDangerous,
            weight = if (isDangerous) 6 else 0,
            isSystemInternal = isInternalVendorPerm || resolvedProtection == PermissionProtectionType.SIGNATURE,
            protectionType = resolvedProtection,
            isAutoRevoked = isAutoRevoked
        )
    }

    private fun isOfficialRuntimeLike(rawName: String): Boolean {
        val nonDangerousPrefixes = listOf(
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK",
            "android.permission.ACCESS_WIFI",
            "android.permission.CHANGE_WIFI",
            "android.permission.CHANGE_NETWORK",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_EXACT_ALARM",
            "android.permission.USE_FULL_SCREEN_INTENT"
        )
        if (rawName in nonDangerousPrefixes || nonDangerousPrefixes.any {
                rawName.startsWith(it + "_") &&
                    rawName != "android.permission.BLUETOOTH_SCAN" &&
                    rawName != "android.permission.BLUETOOTH_CONNECT" &&
                    rawName != "android.permission.BLUETOOTH_ADVERTISE"
            }
        ) {
            return false
        }
        return rawName.startsWith("android.permission.READ_") ||
            rawName.startsWith("android.permission.WRITE_") ||
            rawName.startsWith("android.permission.ACCESS_") ||
            rawName.startsWith("android.permission.RECEIVE_") ||
            rawName.startsWith("android.permission.SEND_") ||
            rawName == "android.permission.CAMERA" ||
            rawName == "android.permission.RECORD_AUDIO" ||
            rawName == "android.permission.CALL_PHONE" ||
            rawName == "android.permission.BODY_SENSORS" ||
            rawName == "android.permission.ACTIVITY_RECOGNITION"
    }

    private fun inferCategory(rawName: String): PermissionCategory {
        val upper = rawName.uppercase()
        return when {
            "LOCATION" in upper -> PermissionCategory.LOCATION
            "CAMERA" in upper -> PermissionCategory.CAMERA
            "RECORD_AUDIO" in upper || "MICROPHONE" in upper -> PermissionCategory.MICROPHONE
            "CONTACTS" in upper || "GET_ACCOUNTS" in upper -> PermissionCategory.CONTACTS
            "SMS" in upper || "CALL_LOG" in upper || "CALL_PHONE" in upper || "PHONE_STATE" in upper || "PHONE_NUMBERS" in upper ->
                PermissionCategory.PHONE_SMS
            "STORAGE" in upper || "READ_MEDIA" in upper -> PermissionCategory.STORAGE
            "CALENDAR" in upper -> PermissionCategory.CALENDAR
            "BODY_SENSORS" in upper || "BLUETOOTH_SCAN" in upper || "BLUETOOTH_CONNECT" in upper ||
                "BLUETOOTH_ADVERTISE" in upper || "ACTIVITY_RECOGNITION" in upper || "NEARBY_WIFI" in upper ->
                PermissionCategory.SENSORS
            else -> PermissionCategory.OTHER
        }
    }

    fun detectFunctionalCategory(
        packageName: String,
        appName: String,
        androidCategory: Int = -1,
        isSystemApp: Boolean = false,
        permissions: List<AppPermissionDetail> = emptyList(),
        hasValidReleaseSignature: Boolean = true
    ): AppFunctionalCategory {
        val pkg = packageName.lowercase()
        val name = appName.lowercase()

        // 1. Pengumpul Kontak / Caller ID (Getcontact, Truecaller, Sync.ME) -> TETAP MERAH bila ambil kontak
        if (pkg in callerIdHarvesterPackages ||
            pkg.contains("getcontact") ||
            pkg.contains("truecaller") ||
            pkg.contains("syncme") ||
            pkg.contains("sync.me") ||
            name.contains("getcontact") ||
            name.contains("truecaller") ||
            name.contains("sync.me")
        ) {
            return AppFunctionalCategory.CALLER_ID_HARVESTER
        }

        // 2. Pinjol & Kredit Digital (dibedakan dari Bank resmi via daftar + pola izin + signature)
        val hasLoanKeywords = pkg in loanPackages ||
            pkg.contains("kredivo") || pkg.contains("akulaku") || pkg.contains("kreditpintar") ||
            pkg.contains("adakami") || pkg.contains("rupiahcepat") || pkg.contains("indodana") ||
            pkg.contains("easycash") || pkg.contains("adapundi") || pkg.contains("tunaiku") ||
            pkg.contains("pinjam") || pkg.contains("kredit") ||
            name.contains("kredivo") || name.contains("akulaku") || name.contains("kredit pintar") ||
            name.contains("adakami") || name.contains("rupiah cepat") || name.contains("indodana") ||
            name.contains("easycash") || name.contains("adapundi") || name.contains("pinjol") ||
            name.contains("tunaiku") || name.contains("uangme") || name.contains("pinjam")

        val isOfficialBankOrWallet = (pkg in officialFinancePackages ||
            pkg.contains(".dana") || pkg.contains("ovo.id") || pkg.contains("gopay") ||
            pkg.contains("brimo") || pkg.contains("mybca") || pkg.contains("livin") ||
            pkg.contains("seabank") || name == "dana" || name.startsWith("dana ") ||
            name == "ovo" || name.startsWith("ovo ") || name.contains("gopay") ||
            name.contains("linkaja") || name.contains("brimo") || name.contains("mybca") ||
            name.contains("livin") || name.contains("m-banking") || name.contains("mbanking") ||
            name.contains("mobile banking") || name.contains("seabank") || name.contains("bank jago")) &&
            hasValidReleaseSignature

        val hasPinjolRiskyCombo = hasPinjolPermissionPattern(permissions)
        val hasFinanceTrait = isOfficialBankOrWallet || hasLoanKeywords ||
            name.contains("bank") || name.contains("wallet") || name.contains("dompet") ||
            name.contains("pay") || name.contains("keuangan") || name.contains("finance")

        if (hasLoanKeywords && !isOfficialBankOrWallet) {
            return AppFunctionalCategory.LOAN_CREDIT
        }
        if (hasFinanceTrait && !isOfficialBankOrWallet && hasPinjolRiskyCombo) {
            return AppFunctionalCategory.LOAN_CREDIT
        }

        // 3. Operator Seluler (myIM3, AXISNET, MyTelkomsel, myXL, bima+, MySmartfren, by.U)
        if (pkg in telecomOperatorPackages ||
            pkg.contains("indosat") || pkg.contains("axis.net") || pkg.contains("telkomsel") ||
            pkg.contains("myxl") || pkg.contains("bimaplus") || pkg.contains("smartfren") ||
            name.contains("myim3") || name.contains("axisnet") || name.contains("mytelkomsel") ||
            name.contains("myxl") || name.contains("bima+") || name.contains("mysmartfren")
        ) {
            return AppFunctionalCategory.TELECOM_OPERATOR
        }

        // 4. Aplikasi Sistem / OEM Bawaan (Samsung, Xiaomi, Oppo, Vivo, Realme, Infinix/Tecno, Asus, Pixel, Huawei)
        if (isSystemApp || isCoreSystemOrOemPackage(pkg)) {
            return AppFunctionalCategory.SYSTEM_REASONABLE
        }

        // 5. Chatting & Sosial Media (termasuk Play Store CATEGORY_SOCIAL + pola fungsi)
        if (pkg in chatPackages ||
            name.contains("whatsapp") || name.contains("telegram") || name.contains("messenger") ||
            name.contains("discord") || name.contains("wechat") || name.contains("signal") ||
            pkg.contains("whatsapp") || pkg.contains("telegram") || name.contains("chat")
        ) {
            return AppFunctionalCategory.CHAT_MESSAGING
        }

        if (pkg in socialPackages ||
            name.contains("facebook") || name.contains("instagram") || name.contains("tiktok") ||
            name.contains("snapchat") || name.contains("twitter") || name.contains("threads") ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && androidCategory == ApplicationInfo.CATEGORY_SOCIAL)
        ) {
            return AppFunctionalCategory.SOCIAL_MEDIA
        }

        // 6. E-Commerce & Belanja
        if (pkg in ecommercePackages ||
            pkg.contains("shopee") || pkg.contains("tokopedia") || pkg.contains("lazada") ||
            pkg.contains("blibli") || pkg.contains("bukalapak") || pkg.contains("tiktok.shop") ||
            name.contains("shopee") || name.contains("tokopedia") || name.contains("lazada") ||
            name.contains("blibli") || name.contains("bukalapak") || name.contains("traveloka") ||
            name.contains("tiktok shop") || name.contains("shop")
        ) {
            return AppFunctionalCategory.ECOMMERCE
        }

        // 7. Keuangan Aman & Bank Resmi (cek signature rilis)
        if (isOfficialBankOrWallet) {
            return AppFunctionalCategory.FINANCE_WALLET
        }

        // 8. Kesehatan & Medis (PeduliLindungi, SATUSEHAT, Halodoc, dsb.)
        if (pkg in healthPackages ||
            pkg.contains("satusehat") || pkg.contains("pedulilindungi") ||
            pkg.contains("halodoc") || pkg.contains("alodokter") ||
            name.contains("pedulilindungi") || name.contains("satusehat") ||
            name.contains("halodoc") || name.contains("alodokter") ||
            name.contains("kesehatan") || name.contains("klinik")
        ) {
            return AppFunctionalCategory.HEALTH_MEDICAL
        }

        // 9. Transportasi & Navigasi (Gojek, Grab, Maxim, Waze, Maps + CATEGORY_MAPS)
        if (pkg in transportPackages ||
            pkg.contains("gojek") || pkg.contains("grab") || pkg.contains("maxim") ||
            pkg.contains("indriver") || pkg.contains("maps") || pkg.contains("waze") ||
            name.contains("gojek") || name.contains("grab") || name.contains("maxim") ||
            name.contains("maps") || name.contains("peta") || name.contains("navigasi") ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && androidCategory == ApplicationInfo.CATEGORY_MAPS)
        ) {
            return AppFunctionalCategory.MAPS_NAVIGATION
        }

        // 10. Perangkat IoT, TWS & Wearable (Android 12+ Bluetooth/Nearby devices wajar)
        if (pkg.contains("tuya") || pkg.contains("smartlife") || pkg.contains("mihome") ||
            pkg.contains("soundcore") || pkg.contains("wearable") || pkg.contains("garmin") ||
            pkg.contains("bardi") || name.contains("tws") || name.contains("earbuds") ||
            name.contains("smartwatch") || name.contains("wearable") || name.contains("smart home") ||
            name.contains("mi home") || name.contains("buds") || name.contains("iot")
        ) {
            return AppFunctionalCategory.IOT_WEARABLE
        }

        // 11. Pengelola File & Berkas (Android 11+ Scoped Storage)
        if (pkg.contains("filemanager") || pkg.contains("myfiles") || pkg.contains("zarchiver") ||
            pkg.contains("solidexplorer") || pkg.contains("files") ||
            name.contains("file manager") || name.contains("pengelola file") ||
            name.contains("file saya") || name.contains("berkas") || name.contains("zarchiver")
        ) {
            return AppFunctionalCategory.FILE_MANAGER
        }

        // 12. Kalender & Alarm / Jam / Produktivitas
        if (pkg.contains("calendar") || name.contains("kalender") || name.contains("calendar") ||
            pkg.contains("alarm") || pkg.contains("deskclock") || name.contains("alarm")
        ) {
            return AppFunctionalCategory.CALENDAR_PRODUCTIVITY
        }

        // 13. Utilitas Sederhana (Kalkulator, Senter, Wallpaper)
        if (isSimpleUtility(pkg, name)) {
            return AppFunctionalCategory.SIMPLE_UTILITY
        }

        if (pkg.contains("dialer") || pkg.contains("contacts") || pkg.contains("messaging") ||
            pkg.contains("mms") || pkg.contains("telecom") ||
            name == "telepon" || name == "phone" || name == "pesan" || name == "messages"
        ) {
            return AppFunctionalCategory.PHONE_SMS_DIALER
        }

        if (pkg.contains("camera") || pkg.contains("gallery") || pkg.contains("photos") ||
            pkg.contains("youtube") || pkg.contains("spotify") ||
            name.contains("kamera") || name.contains("camera") ||
            name.contains("galeri") || name.contains("foto")
        ) {
            return AppFunctionalCategory.CAMERA_MEDIA
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (androidCategory) {
                ApplicationInfo.CATEGORY_IMAGE,
                ApplicationInfo.CATEGORY_VIDEO,
                ApplicationInfo.CATEGORY_AUDIO -> return AppFunctionalCategory.CAMERA_MEDIA
                ApplicationInfo.CATEGORY_GAME -> return AppFunctionalCategory.GAME
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> {
                    if (hasFinanceTrait) return AppFunctionalCategory.FINANCE_WALLET
                    return AppFunctionalCategory.CALENDAR_PRODUCTIVITY
                }
            }
        }

        // Deteksi game dari pola izin atau nama jika Play Store category tidak terisi
        if (pkg.contains("game") || name.contains("game") || pkg.startsWith("com.supercell") ||
            pkg.startsWith("com.mobile.legends") || pkg.startsWith("com.tencent.ig") ||
            pkg.startsWith("com.dts.freefire")
        ) {
            return AppFunctionalCategory.GAME
        }

        return AppFunctionalCategory.GENERAL
    }

    private fun hasPinjolPermissionPattern(permissions: List<AppPermissionDetail>): Boolean {
        val rawSet = permissions.mapTo(HashSet()) { it.rawName }
        val hasContacts = "android.permission.READ_CONTACTS" in rawSet || "android.permission.WRITE_CONTACTS" in rawSet
        val hasSms = "android.permission.READ_SMS" in rawSet || "android.permission.RECEIVE_SMS" in rawSet
        val hasLocation = "android.permission.ACCESS_FINE_LOCATION" in rawSet ||
            "android.permission.ACCESS_COARSE_LOCATION" in rawSet ||
            "android.permission.ACCESS_BACKGROUND_LOCATION" in rawSet
        val hasCallLog = "android.permission.READ_CALL_LOG" in rawSet || "android.permission.WRITE_CALL_LOG" in rawSet
        val hasAccessibility = "android.permission.BIND_ACCESSIBILITY_SERVICE" in rawSet
        return (hasContacts && hasSms) ||
            (hasContacts && hasLocation && hasSms) ||
            hasCallLog ||
            hasAccessibility
    }

    private fun isSimpleUtility(pkg: String, name: String): Boolean {
        val keywords = listOf(
            "calculator", "kalkulator", "calc2",
            "flashlight", "senter", "torch",
            "wallpaper", "wallpapers", "livewallpaper",
            "clock", "stopwatch", "timer", "jam",
            "compass", "kompas", "leveler", "waterpass",
            "notepad", "notes", "catatan", "sticky",
            "converter", "konverter"
        )
        return keywords.any { kw -> pkg.contains(kw) || name.contains(kw) }
    }

    private fun isEffectivelyNonDangerousByOsVersion(
        perm: AppPermissionDetail,
        category: AppFunctionalCategory,
        pkgLower: String,
        nameLower: String,
        androidSdkInt: Int,
        isUnusedApp: Boolean
    ): Boolean {
        // 1. Izin Normal / Signature / Internal tidak pernah dihitung bahaya
        if (perm.protectionType == PermissionProtectionType.NORMAL ||
            perm.protectionType == PermissionProtectionType.SIGNATURE ||
            perm.isSystemInternal ||
            !perm.isDangerous
        ) {
            return true
        }

        val raw = perm.rawName

        // 2. Android 15+: Izin yang terkena Auto-Revoke jangan dihitung
        if (perm.isAutoRevoked || (androidSdkInt >= 35 && isUnusedApp && !perm.isGranted && perm.protectionType == PermissionProtectionType.DANGEROUS)) {
            return true
        }

        // 3. Android 7-10 (API 24-29): izin belum diizinkan runtime tidak dihitung sebagai bahaya aktif
        if (androidSdkInt in 24..29 && !perm.isGranted && perm.protectionType == PermissionProtectionType.DANGEROUS) {
            return true
        }

        // 4. Android 11+ (API 30+): Scoped Storage -> READ_EXTERNAL_STORAGE tidak dianggap bahaya untuk File Manager / Galeri
        if (androidSdkInt >= 30 &&
            (raw == "android.permission.READ_EXTERNAL_STORAGE" || raw == "android.permission.WRITE_EXTERNAL_STORAGE") &&
            (category == AppFunctionalCategory.FILE_MANAGER || category == AppFunctionalCategory.CAMERA_MEDIA)
        ) {
            return true
        }

        // 5. Android 12+ (API 31+): BLUETOOTH_SCAN / CONNECT / NEARBY_WIFI_DEVICES bukan bahaya untuk aplikasi IoT / TWS
        if (androidSdkInt >= 31 &&
            category == AppFunctionalCategory.IOT_WEARABLE &&
            (raw == "android.permission.BLUETOOTH_SCAN" ||
                raw == "android.permission.BLUETOOTH_CONNECT" ||
                raw == "android.permission.BLUETOOTH_ADVERTISE" ||
                raw == "android.permission.NEARBY_WIFI_DEVICES")
        ) {
            return true
        }

        // 6. Android 13+ (API 33+): POST_NOTIFICATIONS & READ_MEDIA_IMAGES/VIDEO/AUDIO itu wajar
        if (androidSdkInt >= 33 && raw == "android.permission.POST_NOTIFICATIONS") {
            return true
        }
        if (androidSdkInt >= 33 &&
            (raw == "android.permission.READ_MEDIA_IMAGES" ||
                raw == "android.permission.READ_MEDIA_VIDEO" ||
                raw == "android.permission.READ_MEDIA_AUDIO" ||
                raw == "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") &&
            category != AppFunctionalCategory.SIMPLE_UTILITY &&
            category != AppFunctionalCategory.CALLER_ID_HARVESTER
        ) {
            return true
        }

        // 7. Android 14+ (API 34+): SCHEDULE_EXACT_ALARM & USE_FULL_SCREEN_INTENT wajar untuk Alarm / Jam / Kalender
        val isClockOrAlarm = pkgLower.contains("clock") || pkgLower.contains("alarm") ||
            nameLower.contains("jam") || nameLower.contains("alarm") ||
            category == AppFunctionalCategory.CALENDAR_PRODUCTIVITY ||
            category == AppFunctionalCategory.CHAT_MESSAGING ||
            category == AppFunctionalCategory.PHONE_SMS_DIALER
        if (androidSdkInt >= 34 &&
            isClockOrAlarm &&
            (raw == "android.permission.SCHEDULE_EXACT_ALARM" ||
                raw == "android.permission.USE_EXACT_ALARM" ||
                raw == "android.permission.USE_FULL_SCREEN_INTENT")
        ) {
            return true
        }

        // 8. Game: Jangan hitung READ_PHONE_STATE sebagai bahaya jika hanya dipakai SDK iklan
        if (category == AppFunctionalCategory.GAME && raw == "android.permission.READ_PHONE_STATE") {
            return true
        }

        return false
    }

    private fun isPermissionExpectedForCategory(
        perm: AppPermissionDetail,
        category: AppFunctionalCategory,
        pkgLower: String,
        nameLower: String,
        androidSdkInt: Int
    ): Boolean {
        if (!perm.isDangerous) return true
        val raw = perm.rawName

        return when (category) {
            AppFunctionalCategory.CALLER_ID_HARVESTER -> false

            AppFunctionalCategory.LOAN_CREDIT -> {
                raw == "android.permission.READ_PHONE_STATE"
            }

            AppFunctionalCategory.TELECOM_OPERATOR -> {
                // Operator (myIM3, AXISNET, MyTelkomsel, bima+): Hanya Nomor / Status Telepon yang wajar; Lokasi & Kontak TIDAK WAJAR
                raw == "android.permission.READ_PHONE_STATE" ||
                    raw == "android.permission.READ_PHONE_NUMBERS"
            }

            AppFunctionalCategory.SYSTEM_REASONABLE -> true

            AppFunctionalCategory.HEALTH_MEDICAL -> {
                // PeduliLindungi, SATUSEHAT, Halodoc: Lokasi (dan Kamera/Mikrofon/Penyimpanan untuk konsultasi) wajar; KONTAK TIDAK WAJAR!
                when (perm.category) {
                    PermissionCategory.LOCATION -> raw != "android.permission.ACCESS_BACKGROUND_LOCATION"
                    PermissionCategory.CAMERA,
                    PermissionCategory.MICROPHONE,
                    PermissionCategory.STORAGE -> true
                    PermissionCategory.SENSORS -> {
                        raw == "android.permission.BLUETOOTH_SCAN" ||
                            raw == "android.permission.BLUETOOTH_CONNECT" ||
                            raw == "android.permission.ACTIVITY_RECOGNITION"
                    }
                    PermissionCategory.PHONE_SMS -> raw == "android.permission.READ_PHONE_STATE"
                    PermissionCategory.CONTACTS -> false
                    else -> false
                }
            }

            AppFunctionalCategory.IOT_WEARABLE -> {
                when (perm.category) {
                    PermissionCategory.SENSORS -> true
                    PermissionCategory.LOCATION -> androidSdkInt < 31 || raw != "android.permission.ACCESS_BACKGROUND_LOCATION"
                    PermissionCategory.CAMERA,
                    PermissionCategory.STORAGE -> true
                    else -> false
                }
            }

            AppFunctionalCategory.FILE_MANAGER -> {
                perm.category == PermissionCategory.STORAGE ||
                    raw == "android.permission.MANAGE_EXTERNAL_STORAGE" ||
                    raw == "android.permission.REQUEST_INSTALL_PACKAGES"
            }

            AppFunctionalCategory.CHAT_MESSAGING,
            AppFunctionalCategory.SOCIAL_MEDIA -> {
                when (perm.category) {
                    PermissionCategory.CAMERA,
                    PermissionCategory.MICROPHONE,
                    PermissionCategory.CONTACTS,
                    PermissionCategory.STORAGE -> true
                    PermissionCategory.LOCATION -> {
                        category == AppFunctionalCategory.CHAT_MESSAGING ||
                            raw != "android.permission.ACCESS_BACKGROUND_LOCATION"
                    }
                    PermissionCategory.SENSORS -> {
                        raw == "android.permission.BLUETOOTH_CONNECT" ||
                            raw == "android.permission.BLUETOOTH_SCAN" ||
                            raw == "android.permission.NEARBY_WIFI_DEVICES" ||
                            raw == "android.permission.ACTIVITY_RECOGNITION"
                    }
                    PermissionCategory.PHONE_SMS -> {
                        raw == "android.permission.READ_PHONE_STATE" ||
                            raw == "android.permission.READ_PHONE_NUMBERS" ||
                            raw == "android.permission.CALL_PHONE" ||
                            raw == "android.permission.ANSWER_PHONE_CALLS" ||
                            raw == "android.permission.MANAGE_OWN_CALLS" ||
                            (category == AppFunctionalCategory.CHAT_MESSAGING &&
                                (raw == "android.permission.RECEIVE_SMS" ||
                                    raw == "android.permission.READ_SMS" ||
                                    raw == "android.permission.READ_CALL_LOG"))
                    }
                    PermissionCategory.SYSTEM_SPECIAL -> {
                        raw == "android.permission.SYSTEM_ALERT_WINDOW" ||
                            (raw == "android.permission.REQUEST_INSTALL_PACKAGES" && pkgLower.startsWith("com.whatsapp"))
                    }
                    else -> false
                }
            }

            AppFunctionalCategory.ECOMMERCE -> {
                when (perm.category) {
                    PermissionCategory.LOCATION -> raw != "android.permission.ACCESS_BACKGROUND_LOCATION"
                    PermissionCategory.CAMERA,
                    PermissionCategory.MICROPHONE,
                    PermissionCategory.STORAGE -> true
                    PermissionCategory.CONTACTS -> raw == "android.permission.READ_CONTACTS" || raw == "android.permission.GET_ACCOUNTS"
                    PermissionCategory.SENSORS -> {
                        raw == "android.permission.BLUETOOTH_CONNECT" ||
                            raw == "android.permission.BLUETOOTH_SCAN" ||
                            raw == "android.permission.NEARBY_WIFI_DEVICES"
                    }
                    PermissionCategory.PHONE_SMS -> {
                        raw == "android.permission.READ_PHONE_STATE" ||
                            raw == "android.permission.READ_PHONE_NUMBERS" ||
                            raw == "android.permission.RECEIVE_SMS"
                    }
                    PermissionCategory.SYSTEM_SPECIAL -> {
                        raw == "android.permission.SYSTEM_ALERT_WINDOW" ||
                            raw == "android.permission.QUERY_ALL_PACKAGES"
                    }
                    else -> false
                }
            }

            AppFunctionalCategory.FINANCE_WALLET -> {
                when (perm.category) {
                    PermissionCategory.CAMERA,
                    PermissionCategory.LOCATION,
                    PermissionCategory.CONTACTS,
                    PermissionCategory.STORAGE -> raw != "android.permission.ACCESS_BACKGROUND_LOCATION"
                    PermissionCategory.MICROPHONE -> true
                    PermissionCategory.PHONE_SMS -> {
                        raw == "android.permission.READ_PHONE_STATE" ||
                            raw == "android.permission.READ_PHONE_NUMBERS" ||
                            raw == "android.permission.RECEIVE_SMS" ||
                            raw == "android.permission.READ_SMS" ||
                            raw == "android.permission.CALL_PHONE"
                    }
                    PermissionCategory.SENSORS -> {
                        raw == "android.permission.BLUETOOTH_CONNECT" ||
                            raw == "android.permission.BLUETOOTH_SCAN" ||
                            raw == "android.permission.ACTIVITY_RECOGNITION"
                    }
                    PermissionCategory.SYSTEM_SPECIAL -> {
                        raw == "android.permission.QUERY_ALL_PACKAGES" ||
                            raw == "android.permission.SYSTEM_ALERT_WINDOW"
                    }
                    else -> false
                }
            }

            AppFunctionalCategory.CALENDAR_PRODUCTIVITY -> {
                perm.category == PermissionCategory.CALENDAR ||
                    perm.category == PermissionCategory.CONTACTS ||
                    perm.category == PermissionCategory.STORAGE ||
                    raw == "android.permission.ACCESS_COARSE_LOCATION" ||
                    raw == "android.permission.ACCESS_FINE_LOCATION" ||
                    raw == "android.permission.READ_PHONE_STATE"
            }

            AppFunctionalCategory.CAMERA_MEDIA -> {
                perm.category == PermissionCategory.CAMERA ||
                    perm.category == PermissionCategory.MICROPHONE ||
                    perm.category == PermissionCategory.STORAGE ||
                    raw == "android.permission.ACCESS_COARSE_LOCATION" ||
                    raw == "android.permission.ACCESS_FINE_LOCATION"
            }

            AppFunctionalCategory.MAPS_NAVIGATION -> {
                perm.category == PermissionCategory.LOCATION ||
                    perm.category == PermissionCategory.CAMERA ||
                    perm.category == PermissionCategory.MICROPHONE ||
                    perm.category == PermissionCategory.STORAGE ||
                    perm.category == PermissionCategory.CONTACTS ||
                    raw == "android.permission.CALL_PHONE" ||
                    raw == "android.permission.READ_PHONE_STATE" ||
                    raw == "android.permission.READ_PHONE_NUMBERS"
            }

            AppFunctionalCategory.PHONE_SMS_DIALER -> {
                perm.category == PermissionCategory.PHONE_SMS ||
                    perm.category == PermissionCategory.CONTACTS ||
                    perm.category == PermissionCategory.MICROPHONE ||
                    perm.category == PermissionCategory.STORAGE
            }

            AppFunctionalCategory.SIMPLE_UTILITY -> {
                val isCompass = pkgLower.contains("compass") || nameLower.contains("kompas")
                val isNotes = pkgLower.contains("note") || nameLower.contains("catatan")
                when {
                    isCompass && perm.category == PermissionCategory.LOCATION -> true
                    isNotes && perm.category == PermissionCategory.STORAGE -> true
                    else -> false
                }
            }

            AppFunctionalCategory.GAME -> {
                perm.category == PermissionCategory.STORAGE ||
                    perm.category == PermissionCategory.MICROPHONE ||
                    perm.category == PermissionCategory.SENSORS ||
                    raw == "android.permission.READ_PHONE_STATE"
            }

            AppFunctionalCategory.GENERAL -> false
        }
    }

    fun evaluateRisk(
        permissions: List<AppPermissionDetail>,
        isSystemApp: Boolean,
        packageName: String = "",
        appName: String = "",
        androidCategory: Int = -1,
        androidSdkInt: Int = Build.VERSION.SDK_INT,
        hasValidReleaseSignature: Boolean = true,
        isUnusedApp: Boolean = false
    ): RiskEvaluation {
        val functionalCategory = detectFunctionalCategory(
            packageName = packageName,
            appName = appName,
            androidCategory = androidCategory,
            isSystemApp = isSystemApp,
            permissions = permissions,
            hasValidReleaseSignature = hasValidReleaseSignature
        )
        val pkgLower = packageName.lowercase()
        val nameLower = appName.lowercase()
        val deviceBrand = detectPackageVendorBrand(pkgLower)

        val annotated = permissions.map { perm ->
            val neutralizedByOs = isEffectivelyNonDangerousByOsVersion(
                perm = perm,
                category = functionalCategory,
                pkgLower = pkgLower,
                nameLower = nameLower,
                androidSdkInt = androidSdkInt,
                isUnusedApp = isUnusedApp
            )
            val isAutoRevokedFinal = perm.isAutoRevoked ||
                (androidSdkInt >= 35 && isUnusedApp && !perm.isGranted && perm.protectionType == PermissionProtectionType.DANGEROUS)
            val effectiveDangerous = if (neutralizedByOs) false else perm.isDangerous
            val effectiveWeight = if (effectiveDangerous) perm.weight else 0
            val updatedPerm = perm.copy(
                isDangerous = effectiveDangerous,
                weight = effectiveWeight,
                isAutoRevoked = isAutoRevokedFinal
            )
            val expected = isPermissionExpectedForCategory(
                perm = updatedPerm,
                category = functionalCategory,
                pkgLower = pkgLower,
                nameLower = nameLower,
                androidSdkInt = androidSdkInt
            )
            updatedPerm.copy(isExpectedForApp = expected)
        }

        val allDangerous = annotated.filter { it.isDangerous && !it.isSystemInternal }
        val grantedDangerous = allDangerous.count { it.isGranted }
        val unexpectedDangerous = allDangerous.filter { !it.isExpectedForApp }
        val expectedDangerous = allDangerous.filter { it.isExpectedForApp }

        // 1. Kategori Khusus: "Sistem - Wajar" -> Skor 0, Hijau, dikeluarkan dari hitungan Berbahaya
        if (functionalCategory == AppFunctionalCategory.SYSTEM_REASONABLE) {
            val vendorNote = if (deviceBrand != DeviceBrand.OTHER) " (${deviceBrand.displayName})" else ""
            return RiskEvaluation(
                score = 0,
                level = RiskLevel.SAFE,
                reasons = listOf(
                    "Sistem - Wajar$vendorNote: Aplikasi bawaan/sistem resmi dikecualikan dari hitungan bahaya (${permissions.size} izin terdeklarasi, $grantedDangerous izin runtime aktif)."
                ),
                dangerousCount = 0,
                grantedDangerousCount = grantedDangerous,
                unexpectedDangerousCount = 0,
                functionalCategory = AppFunctionalCategory.SYSTEM_REASONABLE,
                hasExpectedPermissionsForFunction = true,
                annotatedPermissions = annotated.map { it.copy(isExpectedForApp = true) }
            )
        }

        // 2. Getcontact, Truecaller, Sync.ME TETAP Merah bila mengakses Kontak / Upload Kontak
        if (functionalCategory == AppFunctionalCategory.CALLER_ID_HARVESTER) {
            val contactOrCallPerms = allDangerous.filter {
                it.category == PermissionCategory.CONTACTS || it.category == PermissionCategory.PHONE_SMS
            }
            if (contactOrCallPerms.isNotEmpty()) {
                val score = min(100, 72 + contactOrCallPerms.size * 5)
                val labelName = appName.ifBlank { "Aplikasi Identifikasi Nomor" }
                return RiskEvaluation(
                    score = score,
                    level = RiskLevel.DANGEROUS,
                    reasons = listOf(
                        "Peringatan Privasi ($labelName): Aplikasi identifikasi nomor meminta akses Kontak & panggilan untuk disinkronkan/diunggah, berisiko tinggi terhadap privasi buku kontak Anda.",
                        "${allDangerous.size} izin sensitif terdeteksi ($grantedDangerous sedang diizinkan)."
                    ),
                    dangerousCount = allDangerous.size,
                    grantedDangerousCount = grantedDangerous,
                    unexpectedDangerousCount = allDangerous.size,
                    functionalCategory = functionalCategory,
                    hasExpectedPermissionsForFunction = false,
                    annotatedPermissions = annotated.map { it.copy(isExpectedForApp = !it.isDangerous) }
                )
            }
        }

        // 3. Pinjol / Keuangan Tidak Resmi: Kontak + SMS + Lokasi + Call Log + Accessibility -> LANGSUNG MERAH
        if (functionalCategory == AppFunctionalCategory.LOAN_CREDIT) {
            val hasContacts = allDangerous.any { it.category == PermissionCategory.CONTACTS }
            val hasSms = allDangerous.any { it.rawName.contains("SMS") }
            val hasLocation = allDangerous.any { it.category == PermissionCategory.LOCATION }
            val hasCallLog = allDangerous.any { it.rawName.contains("CALL_LOG") }
            val hasAccessibility = allDangerous.any { it.rawName == "android.permission.BIND_ACCESSIBILITY_SERVICE" }
            if (hasContacts || hasSms || hasLocation || hasCallLog || hasAccessibility || !hasValidReleaseSignature) {
                val riskyParts = buildList {
                    if (hasContacts) add("Kontak")
                    if (hasSms) add("SMS")
                    if (hasLocation) add("Lokasi")
                    if (hasCallLog) add("Riwayat Panggilan")
                    if (hasAccessibility) add("Aksesibilitas")
                }.joinToString(", ").ifBlank { "Izin Finansial Sensitif" }
                val score = min(100, 72 + unexpectedDangerous.size * 5)
                return RiskEvaluation(
                    score = score,
                    level = RiskLevel.DANGEROUS,
                    reasons = listOf(
                        "Peringatan Privasi Pinjol: Aplikasi pinjaman/keuangan non-bank meminta akses sensitif ($riskyParts) yang sangat berisiko terhadap penyalahgunaan data pribadi.",
                        "${unexpectedDangerous.size} izin di luar batas aman terdeteksi ($grantedDangerous aktif diizinkan)."
                    ),
                    dangerousCount = allDangerous.size,
                    grantedDangerousCount = grantedDangerous,
                    unexpectedDangerousCount = unexpectedDangerous.size,
                    functionalCategory = functionalCategory,
                    hasExpectedPermissionsForFunction = false,
                    annotatedPermissions = annotated
                )
            }
        }

        // 4. Operator Seluler (myIM3, AXISNET, MyTelkomsel, bima+): Hanya READ_PHONE_STATE/NUMBERS wajar; Lokasi & Kontak TIDAK WAJAR
        if (functionalCategory == AppFunctionalCategory.TELECOM_OPERATOR) {
            val badLocationOrContacts = unexpectedDangerous.filter {
                it.category == PermissionCategory.LOCATION || it.category == PermissionCategory.CONTACTS
            }
            if (badLocationOrContacts.isEmpty() && unexpectedDangerous.isEmpty()) {
                return RiskEvaluation(
                    score = 5,
                    level = RiskLevel.SAFE,
                    reasons = listOf(
                        "Izin sesuai fungsi operator seluler (hanya status/nomor telepon untuk cek pulsa & kuota)."
                    ),
                    dangerousCount = allDangerous.size,
                    grantedDangerousCount = grantedDangerous,
                    unexpectedDangerousCount = 0,
                    functionalCategory = functionalCategory,
                    hasExpectedPermissionsForFunction = true,
                    annotatedPermissions = annotated
                )
            }
        }

        // 5. Whitelist Domain Aman Berdasarkan Fungsi + Kategori Play Store
        val isWhitelistedDomain = functionalCategory in setOf(
            AppFunctionalCategory.CHAT_MESSAGING,
            AppFunctionalCategory.SOCIAL_MEDIA,
            AppFunctionalCategory.ECOMMERCE,
            AppFunctionalCategory.FINANCE_WALLET,
            AppFunctionalCategory.HEALTH_MEDICAL,
            AppFunctionalCategory.MAPS_NAVIGATION,
            AppFunctionalCategory.IOT_WEARABLE,
            AppFunctionalCategory.FILE_MANAGER,
            AppFunctionalCategory.CALENDAR_PRODUCTIVITY,
            AppFunctionalCategory.GAME
        )

        if (isWhitelistedDomain && unexpectedDangerous.isEmpty()) {
            val msg = when (functionalCategory) {
                AppFunctionalCategory.CHAT_MESSAGING ->
                    "Izin sesuai fungsi chatting (Kamera, Mic, Lokasi, Kontak & Penyimpanan wajar untuk pesan dan panggilan)."
                AppFunctionalCategory.SOCIAL_MEDIA ->
                    "Izin sesuai fungsi media sosial (Kamera, Mic, Lokasi, Kontak & Penyimpanan wajar untuk unggah konten)."
                AppFunctionalCategory.ECOMMERCE ->
                    "Izin sesuai fungsi e-commerce (Lokasi untuk alamat, Kamera & Penyimpanan untuk foto produk, Mic untuk live)."
                AppFunctionalCategory.FINANCE_WALLET ->
                    "Keuangan Aman & Terverifikasi Signature: Kamera untuk scan QRIS, Lokasi & Kontak wajar untuk keamanan transaksi dan transfer."
                AppFunctionalCategory.HEALTH_MEDICAL ->
                    "Izin sesuai fungsi kesehatan/medis (Lokasi untuk fasilitas kesehatan/pelacakan, kamera & media untuk konsultasi)."
                AppFunctionalCategory.MAPS_NAVIGATION ->
                    "Izin sesuai fungsi transportasi & navigasi (Lokasi dan telepon wajar untuk penjemputan dan rute)."
                AppFunctionalCategory.IOT_WEARABLE ->
                    "Izin sesuai fungsi perangkat IoT/TWS (Bluetooth Scan & Nearby Devices wajar untuk koneksi perangkat)."
                AppFunctionalCategory.FILE_MANAGER ->
                    "Izin sesuai fungsi pengelola berkas (akses penyimpanan wajar sesuai kebijakan Scoped Storage Android)."
                AppFunctionalCategory.CALENDAR_PRODUCTIVITY ->
                    "Izin sesuai fungsi kalender & produktivitas (Jadwal, kontak undangan, & alarm pengingat)."
                AppFunctionalCategory.GAME ->
                    "Izin sesuai fungsi permainan (penyimpanan, audio, serta status telepon untuk SDK iklan)."
                else -> "Izin sesuai dengan fungsi aplikasi."
            }
            val safeScore = if (allDangerous.isEmpty()) 0 else min(16, 4 + (expectedDangerous.count { it.isGranted } / 2))
            return RiskEvaluation(
                score = safeScore,
                level = RiskLevel.SAFE,
                reasons = listOf(msg),
                dangerousCount = allDangerous.size,
                grantedDangerousCount = grantedDangerous,
                unexpectedDangerousCount = 0,
                functionalCategory = functionalCategory,
                hasExpectedPermissionsForFunction = true,
                annotatedPermissions = annotated
            )
        }

        // 6. Penilaian izin tak wajar: Diizinkan = 1.0x vs Hanya Diminta di Manifest = 0.2x (Belum Diizinkan - Belum Berisiko)
        var scoreFloat = 0f
        for (perm in unexpectedDangerous) {
            val multiplier = if (perm.isGranted) 1.0f else 0.2f
            scoreFloat += perm.weight * multiplier
        }

        val reasons = mutableListOf<String>()

        if (functionalCategory == AppFunctionalCategory.TELECOM_OPERATOR && unexpectedDangerous.isNotEmpty()) {
            val abnormalLabels = unexpectedDangerous.map { it.category.label }.distinct().joinToString(", ")
            val hasLocAndContacts = unexpectedDangerous.any { it.category == PermissionCategory.LOCATION } &&
                unexpectedDangerous.any { it.category == PermissionCategory.CONTACTS }
            val anyGranted = unexpectedDangerous.any { it.isGranted }
            scoreFloat += when {
                hasLocAndContacts && anyGranted -> 36f
                hasLocAndContacts -> 24f
                else -> 18f
            }
            reasons += "Operator seluler hanya membutuhkan Status/Nomor Telepon. Permintaan akses $abnormalLabels dinilai TIDAK WAJAR."
        }

        if (functionalCategory == AppFunctionalCategory.HEALTH_MEDICAL &&
            unexpectedDangerous.any { it.category == PermissionCategory.CONTACTS }
        ) {
            scoreFloat += 16f
            reasons += "Aplikasi kesehatan meminta akses Kontak yang tidak sesuai dengan fungsi layanan medis."
        }

        if (functionalCategory == AppFunctionalCategory.SIMPLE_UTILITY && unexpectedDangerous.isNotEmpty()) {
            val abnormalLabels = unexpectedDangerous.map { it.category.label }.distinct().joinToString(", ")
            val grantedAbnormal = unexpectedDangerous.count { it.isGranted }
            scoreFloat += if (grantedAbnormal > 0) 34f + (unexpectedDangerous.size * 10f) else 22f + (unexpectedDangerous.size * 6f)
            reasons += "Anomali fungsi! Aplikasi utilitas sederhana meminta izin tidak wajar ($abnormalLabels)."
        }

        val unexpectedHasLocation = unexpectedDangerous.any { it.category == PermissionCategory.LOCATION }
        val unexpectedHasCamera = unexpectedDangerous.any { it.category == PermissionCategory.CAMERA }
        val unexpectedHasMic = unexpectedDangerous.any { it.category == PermissionCategory.MICROPHONE }
        val unexpectedHasBgLocation = unexpectedDangerous.any {
            it.rawName == "android.permission.ACCESS_BACKGROUND_LOCATION"
        }
        val unexpectedHasContacts = unexpectedDangerous.any { it.category == PermissionCategory.CONTACTS }
        val unexpectedHasSmsOrCall = unexpectedDangerous.any {
            it.rawName.contains("SMS") || it.rawName.contains("CALL_LOG")
        }
        val hasInternet = annotated.any { it.rawName == "android.permission.INTERNET" }
        val unexpectedHasSpecialRisk = unexpectedDangerous.any {
            it.rawName == "android.permission.SYSTEM_ALERT_WINDOW" ||
                it.rawName == "android.permission.REQUEST_INSTALL_PACKAGES" ||
                it.rawName == "android.permission.BIND_ACCESSIBILITY_SERVICE" ||
                it.rawName == "android.permission.MANAGE_EXTERNAL_STORAGE"
        }

        if (unexpectedHasLocation && unexpectedHasCamera && unexpectedHasMic) {
            scoreFloat += 14f
            reasons += "Meminta 3 sensor privasi sekaligus (Lokasi, Kamera, Mikrofon) tanpa kesesuaian fungsi yang jelas."
        }
        if (unexpectedHasBgLocation) {
            scoreFloat += 12f
            reasons += "Meminta Lokasi Latar Belakang yang tidak sesuai dengan fungsi utama aplikasi."
        }
        if (unexpectedHasSmsOrCall && unexpectedHasContacts && hasInternet) {
            scoreFloat += 16f
            reasons += "Kombinasi izin sensitif tidak wajar: SMS/Panggilan + Kontak + Internet."
        } else if (unexpectedHasSmsOrCall) {
            scoreFloat += 10f
            reasons += "Meminta akses membaca pesan SMS atau riwayat panggilan di luar fungsi utamanya."
        }
        if (unexpectedHasSpecialRisk) {
            scoreFloat += 14f
            reasons += "Memiliki izin khusus berisiko tinggi (overlay layar, pasang APK, aksesibilitas, atau akses penuh semua file)."
        }

        val grantedUnexpectedCount = unexpectedDangerous.count { it.isGranted }
        val requestedOnlyCount = unexpectedDangerous.size - grantedUnexpectedCount
        if (unexpectedDangerous.size >= 5) {
            scoreFloat += 12f
            reasons += "Terlalu banyak izin tidak wajar (${unexpectedDangerous.size} izin di luar fungsi)."
        }
        if (grantedUnexpectedCount > 0) {
            reasons += "$grantedUnexpectedCount izin tak wajar sudah Diizinkan pengguna (bobot 1.0x)."
        }
        if (requestedOnlyCount > 0) {
            reasons += "$requestedOnlyCount izin hanya diminta di manifes & Belum Diizinkan - Belum Berisiko (bobot 0.2x)."
        }
        if (expectedDangerous.isNotEmpty()) {
            reasons += "${expectedDangerous.size} izin lainnya dinilai wajar untuk fungsi aplikasi ini."
        }

        val finalScore = min(100, scoreFloat.roundToInt().coerceAtLeast(0))
        val level = when {
            finalScore >= 60 -> RiskLevel.DANGEROUS
            finalScore >= 25 -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.SAFE
        }

        if (reasons.isEmpty()) {
            reasons += if (allDangerous.isEmpty()) {
                "Tidak ada izin privasi berbahaya aktif yang dipakai aplikasi ini."
            } else {
                "Permintaan izin masih dalam batas wajar ($grantedDangerous aktif dari ${allDangerous.size} diminta)."
            }
        }

        return RiskEvaluation(
            score = finalScore,
            level = level,
            reasons = reasons,
            dangerousCount = allDangerous.size,
            grantedDangerousCount = grantedDangerous,
            unexpectedDangerousCount = unexpectedDangerous.size,
            functionalCategory = functionalCategory,
            hasExpectedPermissionsForFunction = expectedDangerous.isNotEmpty() && unexpectedDangerous.isEmpty(),
            annotatedPermissions = annotated
        )
    }
}
