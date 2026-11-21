package com.example

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppScannerDataSource
import com.example.data.ScanSnapshot
import com.example.domain.PermissionAnalyzer
import com.example.model.AppTab
import com.example.model.BulkItemStatus
import com.example.model.InstalledAppInfo
import com.example.model.RiskLevel
import com.example.ui.ActiPermEvent
import com.example.ui.ActiPermViewModel
import com.example.util.FormattingUtils
import com.example.util.ReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ActiPermLogicTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `permission analyzer assigns safe suspicious and dangerous levels accurately`() {
        val safePerms = listOf(
            PermissionAnalyzer.mapPermission("android.permission.INTERNET", true),
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_NETWORK_STATE", true)
        )
        val safeEval = PermissionAnalyzer.evaluateRisk(safePerms, isSystemApp = false)
        assertEquals(RiskLevel.SAFE, safeEval.level)

        val suspiciousPerms = listOf(
            PermissionAnalyzer.mapPermission("android.permission.CAMERA", true),
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", true),
            PermissionAnalyzer.mapPermission("android.permission.READ_MEDIA_IMAGES", true)
        )
        val suspEval = PermissionAnalyzer.evaluateRisk(suspiciousPerms, isSystemApp = false)
        assertEquals(RiskLevel.SUSPICIOUS, suspEval.level)

        val dangerousPerms = listOf(
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", true),
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_BACKGROUND_LOCATION", true),
            PermissionAnalyzer.mapPermission("android.permission.CAMERA", true),
            PermissionAnalyzer.mapPermission("android.permission.RECORD_AUDIO", true),
            PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", true),
            PermissionAnalyzer.mapPermission("android.permission.READ_SMS", true),
            PermissionAnalyzer.mapPermission("android.permission.SYSTEM_ALERT_WINDOW", true)
        )
        val dangerEval = PermissionAnalyzer.evaluateRisk(dangerousPerms, isSystemApp = false)
        assertEquals(RiskLevel.DANGEROUS, dangerEval.level)
        assertTrue(dangerEval.score >= 60)
    }

    @Test
    fun `whatsapp and chat social apps are scored Green Safe with chat function message`() {
        val whatsappPerms = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.INTERNET"
        ).map { PermissionAnalyzer.mapPermission(it, isGranted = true) }

        val waEval = PermissionAnalyzer.evaluateRisk(
            permissions = whatsappPerms,
            isSystemApp = false,
            packageName = "com.whatsapp",
            appName = "WhatsApp"
        )
        assertEquals(RiskLevel.SAFE, waEval.level)
        assertEquals(0, waEval.unexpectedDangerousCount)
        assertTrue(waEval.hasExpectedPermissionsForFunction)
        assertTrue(waEval.reasons.any { it.contains("Izin sesuai fungsi chatting") })

        val igPerms = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.INTERNET"
        ).map { PermissionAnalyzer.mapPermission(it, isGranted = true) }

        val igEval = PermissionAnalyzer.evaluateRisk(
            permissions = igPerms,
            isSystemApp = false,
            packageName = "com.instagram.android",
            appName = "Instagram"
        )
        assertEquals(RiskLevel.SAFE, igEval.level)
        assertTrue(igEval.hasExpectedPermissionsForFunction)
    }

    @Test
    fun `utility apps requesting mismatched permissions are flagged Dangerous`() {
        val calcPerms = listOf(
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", true),
            PermissionAnalyzer.mapPermission("android.permission.RECORD_AUDIO", true)
        )
        val calcEval = PermissionAnalyzer.evaluateRisk(
            permissions = calcPerms,
            isSystemApp = false,
            packageName = "com.shady.calculator",
            appName = "Kalkulator Pro"
        )
        assertEquals(RiskLevel.DANGEROUS, calcEval.level)
        assertEquals(2, calcEval.unexpectedDangerousCount)

        val flashlightPerms = listOf(
            PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", true),
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", true)
        )
        val flashlightEval = PermissionAnalyzer.evaluateRisk(
            permissions = flashlightPerms,
            isSystemApp = false,
            packageName = "com.super.flashlight",
            appName = "Senter Super"
        )
        assertEquals(RiskLevel.DANGEROUS, flashlightEval.level)

        val wallpaperPerms = listOf(
            PermissionAnalyzer.mapPermission("android.permission.READ_SMS", true),
            PermissionAnalyzer.mapPermission("android.permission.RECEIVE_SMS", true)
        )
        val wallpaperEval = PermissionAnalyzer.evaluateRisk(
            permissions = wallpaperPerms,
            isSystemApp = false,
            packageName = "com.hd.wallpaper",
            appName = "Wallpaper Keren"
        )
        assertEquals(RiskLevel.DANGEROUS, wallpaperEval.level)
    }

    @Test
    fun `shopee dana and samsung calendar are Green Safe while getcontact remains Red Dangerous`() {
        // 1. Shopee (E-Commerce)
        val shopeePerms = listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.INTERNET"
        ).map { PermissionAnalyzer.mapPermission(it, isGranted = true) }

        val shopeeEval = PermissionAnalyzer.evaluateRisk(
            permissions = shopeePerms,
            isSystemApp = false,
            packageName = "com.shopee.id",
            appName = "Shopee"
        )
        assertEquals(RiskLevel.SAFE, shopeeEval.level)
        assertEquals(0, shopeeEval.unexpectedDangerousCount)
        assertTrue(shopeeEval.hasExpectedPermissionsForFunction)

        // 2. DANA (Keuangan / Dompet Digital)
        val danaPerms = listOf(
            "android.permission.CAMERA",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_PHONE_STATE",
            "android.permission.RECEIVE_SMS",
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.INTERNET"
        ).map { PermissionAnalyzer.mapPermission(it, isGranted = true) }

        val danaEval = PermissionAnalyzer.evaluateRisk(
            permissions = danaPerms,
            isSystemApp = false,
            packageName = "id.dana",
            appName = "DANA"
        )
        assertEquals(RiskLevel.SAFE, danaEval.level)
        assertEquals(0, danaEval.unexpectedDangerousCount)
        assertTrue(danaEval.hasExpectedPermissionsForFunction)

        // 3. Kalender Samsung dengan 184 izin internal sistem (tidak disetujui / internal OEM)
        val internalSamsungPerms = (1..180).map { idx ->
            PermissionAnalyzer.mapPermission(
                rawName = "com.samsung.android.calendar.permission.INTERNAL_CALENDAR_LOCATION_STORAGE_$idx",
                isGranted = true,
                isRuntimeDangerousFromPm = false
            )
        } + listOf(
            PermissionAnalyzer.mapPermission("android.permission.READ_CALENDAR", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.WRITE_CALENDAR", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = false),
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = false)
        )

        val samsungCalEval = PermissionAnalyzer.evaluateRisk(
            permissions = internalSamsungPerms,
            isSystemApp = true,
            packageName = "com.samsung.android.calendar",
            appName = "Kalender"
        )
        assertEquals(RiskLevel.SAFE, samsungCalEval.level)
        assertEquals(0, samsungCalEval.unexpectedDangerousCount)

        // 4. Getcontact TETAP Merah / Berbahaya
        val getcontactPerms = listOf(
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_CALL_LOG",
            "android.permission.READ_PHONE_STATE",
            "android.permission.INTERNET"
        ).map { PermissionAnalyzer.mapPermission(it, isGranted = true) }

        val getcontactEval = PermissionAnalyzer.evaluateRisk(
            permissions = getcontactPerms,
            isSystemApp = false,
            packageName = "app.source.getcontact",
            appName = "Getcontact"
        )
        assertEquals(RiskLevel.DANGEROUS, getcontactEval.level)
        assertTrue(getcontactEval.score >= 60)
        assertTrue(getcontactEval.reasons.any { it.contains("Getcontact") })
    }

    @Test
    fun `formatting utils formats size and last used timestamps in Indonesian`() {
        assertEquals("0 KB", FormattingUtils.formatSize(0L))
        assertEquals("500 KB", FormattingUtils.formatSize(500 * 1024L))
        assertEquals("12 MB", FormattingUtils.formatSize(12L * 1024 * 1024))
        assertEquals("Perlu izin Akses Penggunaan", FormattingUtils.formatLastUsed(null))
        assertEquals("Belum pernah dipakai", FormattingUtils.formatLastUsed(0L))

        val now = 1_760_000_000_000L
        assertEquals("Baru saja", FormattingUtils.formatLastUsed(now - 30_000L, now))
        assertEquals("3 hari lalu", FormattingUtils.formatLastUsed(now - 3L * 24 * 3600 * 1000, now))
    }

    @Test
    fun `viewModel filters tabs sensors unused system apps and advances bulk queue`() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepo = FakeScannerDataSource()
        val vm = ActiPermViewModel(app, fakeRepo)
        advanceUntilIdle()

        val initialState = vm.uiState.value
        assertEquals(4, initialState.totalAppsCount)
        assertEquals(2, initialState.downloadedAppsCount)
        assertEquals(2, initialState.systemAppsCount)
        assertEquals(8, initialState.totalDangerousPermissionsCount)

        // Default tab DOWNLOADED
        assertEquals(AppTab.DOWNLOADED, initialState.selectedTab)
        assertEquals(2, initialState.visibleApps.size)

        // Filter Lokasi, Kamera & Mic
        vm.toggleSensorTrioFilter()
        assertEquals(1, vm.uiState.value.visibleApps.size)
        assertEquals("com.spy.cam", vm.uiState.value.visibleApps.first().packageName)
        vm.toggleSensorTrioFilter()

        // Switch to SYSTEM tab & filter unused system apps
        vm.selectTab(AppTab.SYSTEM)
        assertEquals(2, vm.uiState.value.visibleApps.size)

        vm.toggleOnlyUnusedSystemApps()
        assertEquals(1, vm.uiState.value.visibleApps.size)
        assertEquals("com.vendor.bloat", vm.uiState.value.visibleApps.first().packageName)

        // Select system apps and run bulk disable queue
        vm.toggleOnlyUnusedSystemApps()
        vm.toggleSystemAppSelection("com.vendor.bloat")
        vm.toggleSystemAppSelection("com.android.browser")

        val emittedEvents = mutableListOf<ActiPermEvent>()
        val collectJob = launch {
            vm.events.collect { emittedEvents.add(it) }
        }

        vm.startBulkDisableQueue()
        advanceUntilIdle()

        assertTrue(emittedEvents.isNotEmpty())
        val firstEvent = emittedEvents.first() as ActiPermEvent.OpenSystemAppDetails
        val firstPkg = firstEvent.packageName

        // Simulate user disabling firstPkg in Android System Settings and pressing Back
        fakeRepo.disabledPackages.add(firstPkg)
        vm.onAppResumed()
        advanceUntilIdle()

        val queue = vm.uiState.value.bulkDisableQueue
        assertNotNull(queue)
        assertEquals(BulkItemStatus.DISABLED, queue!!.entries[0].status)
        assertEquals(2, emittedEvents.size)
        assertFalse(queue.isFinished)

        collectJob.cancel()
    }

    @Test
    fun `androidViewModelFactory instantiates ActiPermViewModel with Application only`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val vm = factory.create(ActiPermViewModel::class.java)
        assertNotNull(vm)
    }

    @Test
    fun `reportExporter generates complete Indonesian txt report for all apps`() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val fakeRepo = FakeScannerDataSource()
        val vm = ActiPermViewModel(app, fakeRepo)
        advanceUntilIdle()

        val report = ReportExporter.buildFullReport(vm.uiState.value, generatedAtMillis = 1_760_000_000_000L)
        assertTrue(report.contains("LAPORAN LENGKAP AUDIT IZIN & KEAMANAN APLIKASI - ACTIPERM"))
        assertTrue(report.contains("Dibuat oleh Yaudah Iya"))
        assertTrue(report.contains("Total Aplikasi     : 4"))
        assertTrue(report.contains("Kamera Rahasia (com.spy.cam)"))
        assertTrue(report.contains("Bloatware Bawaan (com.vendor.bloat)"))
        assertTrue(ReportExporter.defaultFullReportFileName(1_760_000_000_000L).endsWith(".txt"))
    }

    @Test
    fun `system apps with 300-580 permissions are Sistem - Wajar and excluded from dangerous counts`() = runTest(testDispatcher) {
        fun buildSystemPerms(count: Int) = (1..count).map { idx ->
            PermissionAnalyzer.mapPermission(
                rawName = "com.android.internal.permission.SYS_PERM_$idx",
                isGranted = true,
                isRuntimeDangerousFromPm = false
            )
        } + listOf(
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.CAMERA", isGranted = true)
        )

        val systemCases = listOf(
            "com.android.shell" to ("Shell" to 380),
            "com.google.android.gms" to ("Layanan Google Play" to 580),
            "com.android.settings" to ("Pengaturan" to 420),
            "com.android.systemui" to ("UI Sistem" to 350),
            "com.samsung.android.scs" to ("Samsung Core Services" to 310)
        )

        systemCases.forEach { (pkg, pair) ->
            val (name, count) = pair
            val eval = PermissionAnalyzer.evaluateRisk(
                permissions = buildSystemPerms(count),
                isSystemApp = true,
                packageName = pkg,
                appName = name
            )
            assertEquals("Harus aman untuk $name", RiskLevel.SAFE, eval.level)
            assertEquals("Harus kategori Sistem - Wajar untuk $name", com.example.model.AppFunctionalCategory.SYSTEM_REASONABLE, eval.functionalCategory)
            assertEquals("Tidak boleh menyumbang izin tak wajar untuk $name", 0, eval.unexpectedDangerousCount)
            assertTrue(eval.reasons.any { it.contains("Sistem - Wajar") })
        }
    }

    @Test
    fun `perekam suara samsung notes and jam samsung classify as built-in Aplikasi Bawaan`() {
        assertTrue(
            com.example.data.AppScannerRepository.isBuiltInSystemApp(
                packageName = "com.sec.android.app.voicenote",
                appName = "Perekam Suara",
                flags = 0,
                sourceDir = "/data/app/voicenote/base.apk"
            )
        )
        assertTrue(
            com.example.data.AppScannerRepository.isBuiltInSystemApp(
                packageName = "com.samsung.android.app.notes",
                appName = "Samsung Notes",
                flags = 0,
                sourceDir = "/data/app/notes/base.apk"
            )
        )
        assertTrue(
            com.example.data.AppScannerRepository.isBuiltInSystemApp(
                packageName = "com.sec.android.app.clockpackage",
                appName = "Jam",
                flags = 0,
                sourceDir = "/data/app/clock/base.apk"
            )
        )
        assertFalse(
            com.example.data.AppScannerRepository.isBuiltInSystemApp(
                packageName = "com.finaccel.android",
                appName = "Kredivo",
                flags = 0,
                sourceDir = "/data/app/kredivo/base.apk"
            )
        )
    }

    @Test
    fun `telecom operators reject location and contacts while pinjol with contacts sms location is Red Dangerous`() {
        // 1. Operator Seluler (myIM3) hanya Nomor Telepon -> Aman
        val operatorPhoneOnly = listOf(
            PermissionAnalyzer.mapPermission("android.permission.READ_PHONE_NUMBERS", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.READ_PHONE_STATE", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.INTERNET", isGranted = true)
        )
        val myIm3SafeEval = PermissionAnalyzer.evaluateRisk(
            permissions = operatorPhoneOnly,
            isSystemApp = false,
            packageName = "com.pure.indosat.care",
            appName = "myIM3"
        )
        assertEquals(com.example.model.AppFunctionalCategory.TELECOM_OPERATOR, myIm3SafeEval.functionalCategory)
        assertEquals(RiskLevel.SAFE, myIm3SafeEval.level)
        assertEquals(0, myIm3SafeEval.unexpectedDangerousCount)

        // 2. Operator Seluler (AXISNET) minta Lokasi & Kontak -> Di Luar Fungsi (tidak wajar)
        val operatorTooNosey = listOf(
            PermissionAnalyzer.mapPermission("android.permission.READ_PHONE_NUMBERS", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.INTERNET", isGranted = true)
        )
        val axisNoseyEval = PermissionAnalyzer.evaluateRisk(
            permissions = operatorTooNosey,
            isSystemApp = false,
            packageName = "com.axis.net",
            appName = "AXISNET"
        )
        assertEquals(com.example.model.AppFunctionalCategory.TELECOM_OPERATOR, axisNoseyEval.functionalCategory)
        assertEquals(2, axisNoseyEval.unexpectedDangerousCount)
        assertTrue(axisNoseyEval.level != RiskLevel.SAFE)
        assertTrue(axisNoseyEval.reasons.any { it.contains("Nomor Telepon") })

        // 3. Pinjol (Kredivo) minta Kontak, SMS, Lokasi -> MERAH / BERBAHAYA
        val kredivoPerms = listOf(
            PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.READ_SMS", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.CAMERA", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.INTERNET", isGranted = true)
        )
        val kredivoEval = PermissionAnalyzer.evaluateRisk(
            permissions = kredivoPerms,
            isSystemApp = false,
            packageName = "com.finaccel.android",
            appName = "Kredivo"
        )
        assertEquals(com.example.model.AppFunctionalCategory.LOAN_CREDIT, kredivoEval.functionalCategory)
        assertEquals(RiskLevel.DANGEROUS, kredivoEval.level)
        assertTrue(kredivoEval.score >= 60)
        assertTrue(kredivoEval.reasons.any { it.contains("Pinjol") })
    }

    @Test
    fun `xiaomi miui oppo coloros vivo and pixel system apps are Sistem - Wajar with score 0`() {
        assertEquals(com.example.model.DeviceBrand.XIAOMI, PermissionAnalyzer.detectDeviceBrand("Xiaomi", "Redmi"))
        assertEquals(com.example.model.DeviceBrand.OPPO, PermissionAnalyzer.detectDeviceBrand("OPPO", "OPPO"))
        assertEquals(com.example.model.DeviceBrand.VIVO, PermissionAnalyzer.detectDeviceBrand("vivo", "iQOO"))
        assertEquals(com.example.model.DeviceBrand.PIXEL, PermissionAnalyzer.detectDeviceBrand("Google", "Pixel"))
        assertEquals(com.example.model.DeviceBrand.REALME, PermissionAnalyzer.detectDeviceBrand("realme", "realme"))
        assertEquals(com.example.model.DeviceBrand.TRANSSION, PermissionAnalyzer.detectDeviceBrand("INFINIX", "Infinix"))

        val oemCases = listOf(
            Triple("com.miui.securitycenter", "Keamanan MIUI", com.example.model.DeviceBrand.XIAOMI),
            Triple("com.coloros.safecenter", "Manajer Telepon ColorOS", com.example.model.DeviceBrand.OPPO),
            Triple("com.vivo.notes", "Catatan Vivo", com.example.model.DeviceBrand.VIVO),
            Triple("com.iqoo.secure", "iQOO iManager", com.example.model.DeviceBrand.VIVO),
            Triple("com.google.android.apps.recorder", "Perekam Pixel", com.example.model.DeviceBrand.PIXEL),
            Triple("com.transsion.calculator", "Kalkulator Infinix", com.example.model.DeviceBrand.TRANSSION),
            Triple("com.huawei.notepad", "Huawei Notepad", com.example.model.DeviceBrand.HUAWEI)
        )

        val heavySystemPerms = listOf(
            PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.CAMERA", isGranted = true),
            PermissionAnalyzer.mapPermission("android.permission.QUERY_ALL_PACKAGES", isGranted = true)
        )

        oemCases.forEach { (pkg, name, expectedBrand) ->
            assertEquals(expectedBrand, PermissionAnalyzer.detectPackageVendorBrand(pkg))
            assertTrue(com.example.data.AppScannerRepository.isBuiltInSystemApp(pkg, name, flags = 0, sourceDir = "/system/priv-app/$pkg/base.apk"))
            val eval = PermissionAnalyzer.evaluateRisk(
                permissions = heavySystemPerms,
                isSystemApp = true,
                packageName = pkg,
                appName = name
            )
            assertEquals("Skor sistem $name harus 0", 0, eval.score)
            assertEquals("Level sistem $name harus Hijau", RiskLevel.SAFE, eval.level)
            assertEquals(com.example.model.AppFunctionalCategory.SYSTEM_REASONABLE, eval.functionalCategory)
            assertEquals(0, eval.unexpectedDangerousCount)
        }

        // Cek status Sistem vs Bawaan vs Unduhan berdasarkan partisi & FLAG_UPDATED_SYSTEM_APP
        assertEquals(
            com.example.model.AppInstallStatus.SYSTEM,
            com.example.data.AppScannerRepository.classifyInstallStatus(
                packageName = "com.miui.daemon",
                appName = "MIUI Daemon",
                flags = 0,
                sourceDir = "/system_ext/priv-app/MiuiDaemon/MiuiDaemon.apk"
            )
        )
        assertEquals(
            com.example.model.AppInstallStatus.BUILT_IN,
            com.example.data.AppScannerRepository.classifyInstallStatus(
                packageName = "com.coloros.soundrecorder",
                appName = "Perekam Suara",
                flags = android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP,
                sourceDir = "/data/app/recorder/base.apk"
            )
        )
        assertEquals(
            com.example.model.AppInstallStatus.BUILT_IN,
            com.example.data.AppScannerRepository.classifyInstallStatus(
                packageName = "com.vendor.preloadedapp",
                appName = "Vendor App",
                flags = 0,
                sourceDir = "/data/app/vendor/base.apk",
                preInstalledByInstaller = true
            )
        )
    }

    @Test
    fun `android 8 to 15 rules handle scoped storage iot bluetooth notifications exact alarm and auto revoke`() {
        // Android 8-10: izin belum diizinkan runtime tidak dihitung bahaya
        val android9Eval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.CAMERA", isGranted = false),
                PermissionAnalyzer.mapPermission("android.permission.RECORD_AUDIO", isGranted = false)
            ),
            isSystemApp = false,
            packageName = "com.random.utilityapp",
            appName = "Random App",
            androidSdkInt = 28
        )
        assertEquals(RiskLevel.SAFE, android9Eval.level)
        assertEquals(0, android9Eval.dangerousCount)

        // Android 11+: Scoped Storage - READ_EXTERNAL_STORAGE aman untuk File Manager / Galeri
        val fileManagerEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.READ_EXTERNAL_STORAGE", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.WRITE_EXTERNAL_STORAGE", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "com.custom.filemanager",
            appName = "Pengelola File Saya",
            androidSdkInt = 30
        )
        assertEquals(com.example.model.AppFunctionalCategory.FILE_MANAGER, fileManagerEval.functionalCategory)
        assertEquals(RiskLevel.SAFE, fileManagerEval.level)
        assertEquals(0, fileManagerEval.unexpectedDangerousCount)

        // Android 12+: BLUETOOTH_SCAN & NEARBY_WIFI_DEVICES aman untuk IoT / TWS
        val twsEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.BLUETOOTH_SCAN", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.BLUETOOTH_CONNECT", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.NEARBY_WIFI_DEVICES", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "com.brand.tws.companion",
            appName = "TWS Earbuds Control",
            androidSdkInt = 31
        )
        assertEquals(com.example.model.AppFunctionalCategory.IOT_WEARABLE, twsEval.functionalCategory)
        assertEquals(RiskLevel.SAFE, twsEval.level)
        assertEquals(0, twsEval.unexpectedDangerousCount)

        // Android 13+: POST_NOTIFICATIONS & READ_MEDIA_IMAGES/VIDEO/AUDIO wajar
        val android13Eval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.POST_NOTIFICATIONS", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.READ_MEDIA_IMAGES", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.READ_MEDIA_VIDEO", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.READ_MEDIA_AUDIO", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "com.daily.journal",
            appName = "Buku Harian",
            androidSdkInt = 33
        )
        assertEquals(RiskLevel.SAFE, android13Eval.level)
        assertEquals(0, android13Eval.score)

        // Android 14+: SCHEDULE_EXACT_ALARM & USE_FULL_SCREEN_INTENT wajar untuk Alarm / Jam
        val android14AlarmEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.SCHEDULE_EXACT_ALARM", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.USE_FULL_SCREEN_INTENT", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "com.cool.alarmclock",
            appName = "Alarm Pagi",
            androidSdkInt = 34
        )
        assertEquals(RiskLevel.SAFE, android14AlarmEval.level)
        assertEquals(0, android14AlarmEval.score)

        // Android 15+: Izin yang auto-revoke jangan dihitung
        val android15AutoRevokedEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = false, isAutoRevoked = true),
                PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = false)
            ),
            isSystemApp = false,
            packageName = "com.dormant.tracker",
            appName = "Dormant App",
            androidSdkInt = 35,
            isUnusedApp = true
        )
        assertEquals(0, android15AutoRevokedEval.dangerousCount)
        assertEquals(0, android15AutoRevokedEval.score)
        assertEquals(RiskLevel.SAFE, android15AutoRevokedEval.level)
    }

    @Test
    fun `pinjol vs official bank with signature check health transport game and getcontact behave accurately`() {
        // 1. Bank Resmi (BRImo / BCA) dengan release signature valid -> Aman Hijau
        val bankEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.CAMERA", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "id.co.bri.brimo",
            appName = "BRImo BRI",
            hasValidReleaseSignature = true
        )
        assertEquals(com.example.model.AppFunctionalCategory.FINANCE_WALLET, bankEval.functionalCategory)
        assertEquals(RiskLevel.SAFE, bankEval.level)

        // 2. Pinjol / Finance palsu atau dengan kombinasi Kontak + SMS + Lokasi + Call Log + Accessibility -> MERAH
        val sketchyPinjolEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.READ_SMS", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.READ_CALL_LOG", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.BIND_ACCESSIBILITY_SERVICE", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "com.fast.wallet.cashnow",
            appName = "Dompet Kilat Finance",
            hasValidReleaseSignature = false
        )
        assertEquals(com.example.model.AppFunctionalCategory.LOAN_CREDIT, sketchyPinjolEval.functionalCategory)
        assertEquals(RiskLevel.DANGEROUS, sketchyPinjolEval.level)

        // 3. Getcontact / Truecaller -> TETAP MERAH
        val truecallerEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.READ_CALL_LOG", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "com.truecaller",
            appName = "Truecaller"
        )
        assertEquals(RiskLevel.DANGEROUS, truecallerEval.level)

        // 4. Kesehatan (SATUSEHAT / Halodoc): Lokasi wajar, Kontak tidak wajar
        val satusehatSafe = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.CAMERA", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "id.go.kemkes.satusehat",
            appName = "SATUSEHAT Mobile"
        )
        assertEquals(com.example.model.AppFunctionalCategory.HEALTH_MEDICAL, satusehatSafe.functionalCategory)
        assertEquals(RiskLevel.SAFE, satusehatSafe.level)

        val halodocWithContacts = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "com.linkdokter.halodoc.android",
            appName = "Halodoc"
        )
        assertEquals(1, halodocWithContacts.unexpectedDangerousCount)

        // 5. Game: READ_PHONE_STATE untuk iklan tidak dianggap bahaya
        val gameEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.READ_PHONE_STATE", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.INTERNET", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "com.awesome.racinggame",
            appName = "Racing Game",
            androidCategory = android.content.pm.ApplicationInfo.CATEGORY_GAME
        )
        assertEquals(com.example.model.AppFunctionalCategory.GAME, gameEval.functionalCategory)
        assertEquals(RiskLevel.SAFE, gameEval.level)
        assertEquals(0, gameEval.unexpectedDangerousCount)

        // 6. Normal & Signature permissions berbobot 0, manifest belum diizinkan berbobot 0.2x
        val normalAndSignatureEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission(
                    "android.permission.INTERNET",
                    isGranted = true,
                    protectionTypeFromPm = com.example.model.PermissionProtectionType.NORMAL
                ),
                PermissionAnalyzer.mapPermission(
                    "com.oem.INTERNAL_SYNC",
                    isGranted = true,
                    protectionTypeFromPm = com.example.model.PermissionProtectionType.SIGNATURE
                )
            ),
            isSystemApp = false,
            packageName = "com.clean.notes",
            appName = "Catatan Clean"
        )
        assertEquals(0, normalAndSignatureEval.score)
        assertEquals(0, normalAndSignatureEval.dangerousCount)
    }

    @Test
    fun `viewModel filters by device brand Samsung Xiaomi Oppo Vivo Pixel`() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = ActiPermViewModel(app, FakeScannerDataSource())
        advanceUntilIdle()

        vm.selectTab(AppTab.SYSTEM)
        assertEquals(2, vm.uiState.value.visibleApps.size)

        vm.selectDeviceFilter(com.example.model.DeviceFilterOption.PIXEL)
        assertEquals(1, vm.uiState.value.visibleApps.size)
        assertEquals("com.android.browser", vm.uiState.value.visibleApps.first().packageName)

        vm.selectDeviceFilter(com.example.model.DeviceFilterOption.ALL)
        assertEquals(2, vm.uiState.value.visibleApps.size)
    }

    private class FakeScannerDataSource : AppScannerDataSource {
        val disabledPackages = mutableSetOf<String>()

        override fun hasUsageStatsPermission(): Boolean = true

        override suspend fun scanInstalledApps(): ScanSnapshot {
            val apps = listOf(
                InstalledAppInfo(
                    packageName = "com.spy.cam",
                    appName = "Kamera Rahasia",
                    versionName = "1.0",
                    isSystemApp = false,
                    isEnabled = true,
                    sizeBytes = 25_000_000L,
                    lastUsedTimeMillis = 1_760_000_000_000L,
                    permissions = emptyList(),
                    dangerousPermissionCount = 5,
                    grantedDangerousCount = 4,
                    riskScore = 78,
                    riskLevel = RiskLevel.DANGEROUS,
                    riskReasons = listOf("Berbahaya"),
                    hasLocationPermission = true,
                    hasCameraPermission = true,
                    hasMicrophonePermission = true
                ),
                InstalledAppInfo(
                    packageName = "com.notes.safe",
                    appName = "Catatan Simpel",
                    versionName = "2.1",
                    isSystemApp = false,
                    isEnabled = true,
                    sizeBytes = 4_000_000L,
                    lastUsedTimeMillis = 1_760_000_000_000L,
                    permissions = emptyList(),
                    dangerousPermissionCount = 0,
                    grantedDangerousCount = 0,
                    riskScore = 2,
                    riskLevel = RiskLevel.SAFE,
                    riskReasons = listOf("Aman"),
                    hasLocationPermission = false,
                    hasCameraPermission = false,
                    hasMicrophonePermission = false
                ),
                InstalledAppInfo(
                    packageName = "com.vendor.bloat",
                    appName = "Bloatware Bawaan",
                    versionName = "1.2",
                    isSystemApp = true,
                    isEnabled = "com.vendor.bloat" !in disabledPackages,
                    sizeBytes = 90_000_000L,
                    lastUsedTimeMillis = 0L,
                    permissions = emptyList(),
                    dangerousPermissionCount = 2,
                    grantedDangerousCount = 1,
                    riskScore = 32,
                    riskLevel = RiskLevel.SUSPICIOUS,
                    riskReasons = listOf("Mencurigakan"),
                    hasLocationPermission = true,
                    hasCameraPermission = false,
                    hasMicrophonePermission = false
                ),
                InstalledAppInfo(
                    packageName = "com.android.browser",
                    appName = "Browser Bawaan",
                    versionName = "9.0",
                    isSystemApp = true,
                    isEnabled = "com.android.browser" !in disabledPackages,
                    sizeBytes = 120_000_000L,
                    lastUsedTimeMillis = 1_760_000_000_000L,
                    permissions = emptyList(),
                    dangerousPermissionCount = 1,
                    grantedDangerousCount = 1,
                    riskScore = 15,
                    riskLevel = RiskLevel.SAFE,
                    riskReasons = listOf("Aman"),
                    hasLocationPermission = false,
                    hasCameraPermission = false,
                    hasMicrophonePermission = false,
                    packageBrand = PermissionAnalyzer.detectPackageVendorBrand("com.android.browser")
                )
            )
            return ScanSnapshot(apps = apps, hasUsageAccess = true, scannedAtMillis = 1_760_000_000_000L)
        }
    }
}
