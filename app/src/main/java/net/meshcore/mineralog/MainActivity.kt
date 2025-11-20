package net.meshcore.mineralog

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.migration.AutoReferenceCreator
import net.meshcore.mineralog.ui.navigation.AppNavigationDrawer
import net.meshcore.mineralog.ui.dialogs.MigrationReportDialog
import net.meshcore.mineralog.ui.navigation.MineraLogNavHost
import net.meshcore.mineralog.ui.navigation.Screen
import net.meshcore.mineralog.ui.screens.main.MigrationViewModel
import net.meshcore.mineralog.ui.theme.MineraLogTheme
import net.meshcore.mineralog.util.AppLogger
import java.util.UUID

/**
 * Main Activity for MineraLog application.
 * Handles deep links (mineralapp://mineral/{uuid}) and sets up Compose UI.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Get deep link URI if present, validate scheme/host/UUID to prevent injection attacks
        val deepLinkMineralId = intent?.data?.let { uri ->
            // Validate scheme and host first
            if (uri.scheme != "mineralapp" || uri.host != "mineral") {
                AppLogger.w("MainActivity", "Invalid deep link scheme or host rejected: ${uri.scheme}://${uri.host}")
                return@let null
            }

            // Then validate UUID format
            uri.lastPathSegment?.let { id ->
                try {
                    // Validate that the ID is a valid UUID
                    UUID.fromString(id)
                    id // Return the valid ID
                } catch (e: IllegalArgumentException) {
                    // Log security event without exposing the invalid UUID
                    AppLogger.w("MainActivity", "Invalid deep link UUID rejected")
                    null
                }
            }
        }

        setContent {
            MineraLogTheme {
                MineraLogApp(
                    deepLinkMineralId = deepLinkMineralId
                )
            }
        }
    }

    // Note: attachBaseContext() removed - AppCompatDelegate.setApplicationLocales() handles locale management
}

@Composable
fun MineraLogApp(
    deepLinkMineralId: String? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as MineraLogApplication
    val navController = rememberNavController() // Hoist controller so global UI (dialogs) can drive navigation safely
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Get current route for drawer item highlighting
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    // Create migration ViewModel
    val migrationViewModel = remember {
        MigrationViewModel(
            autoReferenceCreator = AutoReferenceCreator(
                context = context,
                database = application.database
            )
        )
    }

    // Trigger migration check on first composition
    LaunchedEffect(Unit) {
        AppLogger.i("MineraLog", "=== Application started, checking reference minerals ===")
        migrationViewModel.checkAndRunMigration()

        // NOTE: Initial dataset loading is now handled by ReferenceMineralListScreen
        // to avoid double-loading and to show progress to the user
    }

    // Observe migration state
    val showMigrationDialog by migrationViewModel.showMigrationDialog.collectAsState()
    val migrationReport = migrationViewModel.getMigrationReport()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppNavigationDrawer(
                currentRoute = currentRoute,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLibrary = {
                    navController.navigate(Screen.ReferenceLibrary.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToIdentification = {
                    navController.navigate(Screen.Identification.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToStatistics = {
                    navController.navigate(Screen.Statistics.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCloseDrawer = {
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            MineraLogNavHost(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                navController = navController,
                deepLinkMineralId = deepLinkMineralId,
                onOpenDrawer = {
                    scope.launch {
                        drawerState.open()
                    }
                }
            )

            // Show migration report dialog if needed
            if (showMigrationDialog && migrationReport != null) {
                MigrationReportDialog(
                    report = migrationReport,
                    onDismiss = { migrationViewModel.dismissMigrationDialog() },
                    onViewLibrary = {
                        migrationViewModel.dismissMigrationDialog()
                        // Direct the user to the reference library once migration completes successfully
                        navController.navigate(Screen.ReferenceLibrary.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
