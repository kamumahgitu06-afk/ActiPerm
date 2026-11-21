package com.example

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppScannerRepository
import com.example.data.PermissionHistoryRepository
import com.example.domain.PermissionAnalyzer
import com.example.model.PermissionChangeLogEntry
import com.example.model.RiskLevel
import com.example.ui.ActiPermViewModel
import com.example.util.SystemIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@Config(sdk = [24])
class Android7CompatibilityTest {

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
    fun `android 7 nougat api 24 and 25 format and evaluate risks properly`() {
        assertEquals(Build.VERSION_CODES.N, Build.VERSION.SDK_INT)
        assertEquals("Android 7.0 (API 24)", PermissionAnalyzer.formatAndroidVersionLabel(24, "7.0"))
        assertEquals("Android 7.1.1 (API 25)", PermissionAnalyzer.formatAndroidVersionLabel(25, "7.1.1"))
        assertEquals("Android 7.1 (API 25)", PermissionAnalyzer.formatAndroidVersionLabel(25, ""))

        val ungrantedUtilityEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.CAMERA", isGranted = false),
                PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = false)
            ),
            isSystemApp = false,
            packageName = "com.flashlight.app",
            appName = "Senter",
            androidSdkInt = 24
        )
        assertEquals(RiskLevel.SAFE, ungrantedUtilityEval.level)

        val grantedUtilityEval = PermissionAnalyzer.evaluateRisk(
            permissions = listOf(
                PermissionAnalyzer.mapPermission("android.permission.READ_CONTACTS", isGranted = true),
                PermissionAnalyzer.mapPermission("android.permission.ACCESS_FINE_LOCATION", isGranted = true)
            ),
            isSystemApp = false,
            packageName = "com.flashlight.app",
            appName = "Senter",
            androidSdkInt = 24
        )
        assertEquals(RiskLevel.DANGEROUS, grantedUtilityEval.level)
    }

    @Test
    fun `appScannerRepository and viewModel scan cleanly on Android 7 api 24`() = runTest(testDispatcher) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = AppScannerRepository(app, ioDispatcher = testDispatcher)
        val snapshot = repo.scanInstalledApps()
        assertNotNull(snapshot)
        assertTrue(snapshot.apps.isNotEmpty())
        assertTrue(snapshot.androidVersionLabel.contains("7.0"))

        val vm = ActiPermViewModel(app, repo)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.allApps.isNotEmpty())
    }

    @Test
    fun `system intents and persistent history repository work cleanly on Android 7 api 24`() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        // No ActivityNotFoundException / SecurityException / ClassCastException
        SystemIntents.openSpecificPermissionSettings(
            app,
            "com.example.target",
            "android.permission.REQUEST_INSTALL_PACKAGES"
        )
        SystemIntents.openUsageAccessSettings(app)

        val historyRepo = PermissionHistoryRepository(app)
        historyRepo.clearHistory()
        val saved = historyRepo.recordChange(
            PermissionChangeLogEntry(
                id = 1L,
                timestampMillis = 1_760_000_000_000L,
                appName = "Uji Android 7",
                packageName = "com.example.nougat",
                permissionRaw = "android.permission.CAMERA",
                permissionFriendly = "Akses Kamera",
                granted = false,
                executionModeLabel = "Pengaturan Manual"
            )
        )
        assertEquals(1, saved.size)
        assertEquals("Uji Android 7", historyRepo.loadHistory().first().appName)
    }
}
