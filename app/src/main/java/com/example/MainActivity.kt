package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ActiPermEvent
import com.example.ui.ActiPermViewModel
import com.example.ui.screens.AppDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.ActiPermTheme
import com.example.util.SystemIntents
import rikka.shizuku.Shizuku

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_APP_DETAIL = "app_detail/{packageName}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            ActiPermTheme {
                ActiPermApp()
            }
        }
    }
}

@Composable
fun ActiPermApp(
    viewModel: ActiPermViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val uninstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshApps()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAppResumed()
    }

    DisposableEffect(viewModel) {
        val binderReceivedListener = Shizuku.OnBinderReceivedListener {
            viewModel.refreshShizukuStatus()
        }
        val binderDeadListener = Shizuku.OnBinderDeadListener {
            viewModel.refreshShizukuStatus()
        }
        val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
            viewModel.refreshShizukuStatus()
        }
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }
        onDispose {
            runCatching {
                Shizuku.removeBinderReceivedListener(binderReceivedListener)
                Shizuku.removeBinderDeadListener(binderDeadListener)
                Shizuku.removeRequestPermissionResultListener(permissionResultListener)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ActiPermEvent.OpenSystemAppDetails -> {
                    SystemIntents.openApplicationDetailsSettings(context, event.packageName)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_DASHBOARD
    ) {
        composable(route = ROUTE_DASHBOARD) {
            HomeScreen(
                uiState = uiState,
                uninstallLauncher = uninstallLauncher,
                onRescan = { viewModel.refreshApps() },
                onSelectTab = viewModel::selectTab,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onToggleRiskFilter = viewModel::toggleRiskFilter,
                onSelectDeviceFilter = viewModel::selectDeviceFilter,
                onToggleSensorTrio = viewModel::toggleSensorTrioFilter,
                onToggleSpecificSensor = viewModel::toggleSpecificSensor,
                onToggleOnlyUnusedSystem = viewModel::toggleOnlyUnusedSystemApps,
                onResetFilters = viewModel::resetFilters,
                onOpenAppDetail = { pkg ->
                    val encoded = Uri.encode(pkg)
                    navController.navigate("app_detail/$encoded")
                },
                onToggleSystemAppSelection = viewModel::toggleSystemAppSelection,
                onSelectAllVisibleSystem = viewModel::selectAllUnusedOrVisibleSystemApps,
                onClearSystemSelection = viewModel::clearSystemAppSelection,
                onStartBulkDisable = viewModel::startBulkDisableQueue,
                onBulkOpenNow = viewModel::openCurrentQueueItem,
                onBulkSkip = viewModel::skipCurrentQueueItem,
                onBulkCancel = viewModel::cancelBulkDisableQueue,
                onSelectMainTab = viewModel::selectMainTab,
                onCheckShizuku = viewModel::refreshShizukuStatus,
                onRequestShizukuPermission = viewModel::requestShizukuPermission,
                onTriggerBatchRevoke = viewModel::openBatchRevokeProposal,
                onConfirmBatchRevoke = viewModel::confirmBatchRevoke,
                onDismissBatchRevoke = viewModel::dismissBatchProposal,
                onClearHistory = viewModel::clearPermissionHistory,
                onSelectPermissionCategory = viewModel::selectPermissionCategory,
                onSelectPermissionGrantFilter = viewModel::selectPermissionGrantFilter,
                onToggleIncludeSystemInPermManager = viewModel::toggleIncludeSystemAppsInPermManager,
                onRootTogglePermission = viewModel::togglePermissionViaRoot,
                onClearPermissionActionMessage = viewModel::clearPermissionActionMessage
            )
        }

        composable(
            route = ROUTE_APP_DETAIL,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedPkg = backStackEntry.arguments?.getString("packageName").orEmpty()
            val packageName = Uri.decode(encodedPkg)
            val app = uiState.allApps.firstOrNull { it.packageName == packageName }
            AppDetailScreen(
                app = app,
                uninstallLauncher = uninstallLauncher,
                isRootAvailable = uiState.isRootAvailable,
                executionMode = uiState.executionMode,
                permissionActionMessage = uiState.permissionActionMessage,
                onExecutePermissionToggle = viewModel::executePermissionToggle,
                onRootTogglePermission = viewModel::togglePermissionViaRoot,
                onClearPermissionActionMessage = viewModel::clearPermissionActionMessage,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
