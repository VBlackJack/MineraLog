package net.meshcore.mineralog.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.ui.screens.home.HomeScreen
import net.meshcore.mineralog.ui.screens.detail.MineralDetailScreen
import net.meshcore.mineralog.ui.screens.add.AddMineralScreen
import net.meshcore.mineralog.ui.screens.edit.EditMineralScreen
import net.meshcore.mineralog.ui.screens.settings.SettingsScreen
import net.meshcore.mineralog.ui.screens.statistics.StatisticsScreen
import net.meshcore.mineralog.ui.screens.statistics.StatisticsViewModel
import net.meshcore.mineralog.ui.screens.comparator.ComparatorScreen
import net.meshcore.mineralog.ui.screens.comparator.ComparatorViewModel
import net.meshcore.mineralog.ui.screens.qr.QrScannerScreen
import net.meshcore.mineralog.ui.screens.camera.CameraCaptureScreen
import net.meshcore.mineralog.ui.screens.gallery.PhotoGalleryScreen
import net.meshcore.mineralog.ui.screens.gallery.FullscreenPhotoViewerScreen
import java.util.UUID

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Add : Screen("add")
    data object Detail : Screen("detail/{mineralId}") {
        fun createRoute(mineralId: String) = "detail/$mineralId"
    }
    data object Edit : Screen("edit/{mineralId}") {
        fun createRoute(mineralId: String) = "edit/$mineralId"
    }
    data object Camera : Screen("camera/{mineralId}") {
        fun createRoute(mineralId: String) = "camera/$mineralId"
    }
    data object PhotoGallery : Screen("gallery/{mineralId}") {
        fun createRoute(mineralId: String) = "gallery/$mineralId"
    }
    data object PhotoFullscreen : Screen("photo/{mineralId}/{photoId}") {
        fun createRoute(mineralId: String, photoId: String) = "photo/$mineralId/$photoId"
    }
    data object Settings : Screen("settings")
    data object Statistics : Screen("statistics")
    data object Compare : Screen("compare/{mineralIds}") {
        fun createRoute(mineralIds: List<String>) = "compare/${mineralIds.joinToString(",")}"
    }
    data object QrScanner : Screen("qr_scanner")
}

@Composable
fun MineraLogNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
    deepLinkMineralId: String? = null
) {
    // Handle deep link with UUID validation (defense-in-depth)
    LaunchedEffect(deepLinkMineralId) {
        deepLinkMineralId?.let { id ->
            try {
                // Double-check UUID validity before navigation
                UUID.fromString(id)
                navController.navigate(Screen.Detail.createRoute(id))
            } catch (e: IllegalArgumentException) {
                // Log security event and ignore invalid navigation
                Log.w("NavHost", "Invalid deep link navigation rejected: $id", e)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onMineralClick = { mineralId ->
                    navController.navigate(Screen.Detail.createRoute(mineralId))
                },
                onAddClick = {
                    navController.navigate(Screen.Add.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onStatisticsClick = {
                    navController.navigate(Screen.Statistics.route)
                },
                onCompareClick = { mineralIds ->
                    navController.navigate(Screen.Compare.createRoute(mineralIds))
                },
                onQrScanClick = {
                    navController.navigate(Screen.QrScanner.route)
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("mineralId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mineralId = backStackEntry.arguments?.getString("mineralId") ?: return@composable
            MineralDetailScreen(
                mineralId = mineralId,
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Screen.Edit.createRoute(id))
                },
                onCameraClick = { id ->
                    navController.navigate(Screen.Camera.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.Edit.route,
            arguments = listOf(
                navArgument("mineralId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mineralId = backStackEntry.arguments?.getString("mineralId") ?: return@composable
            EditMineralScreen(
                mineralId = mineralId,
                onNavigateBack = { navController.popBackStack() },
                onMineralUpdated = { id ->
                    navController.popBackStack()
                    navController.navigate(Screen.Detail.createRoute(id))
                }
            )
        }

        composable(Screen.Add.route) {
            AddMineralScreen(
                onNavigateBack = { navController.popBackStack() },
                onMineralAdded = { mineralId ->
                    navController.popBackStack()
                    navController.navigate(Screen.Detail.createRoute(mineralId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Statistics.route) {
            val application = LocalContext.current.applicationContext as MineraLogApplication
            val viewModel = StatisticsViewModel(
                statisticsRepository = application.statisticsRepository
            )
            StatisticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Compare.route,
            arguments = listOf(
                navArgument("mineralIds") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mineralIdsString = backStackEntry.arguments?.getString("mineralIds") ?: return@composable
            val mineralIds = mineralIdsString.split(",")

            val application = LocalContext.current.applicationContext as MineraLogApplication
            val viewModel = ComparatorViewModel(
                mineralIds = mineralIds,
                mineralRepository = application.mineralRepository
            )
            ComparatorScreen(
                mineralIds = mineralIds,
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel
            )
        }

        composable(Screen.QrScanner.route) {
            QrScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onQrCodeScanned = { mineralId ->
                    navController.popBackStack()
                    navController.navigate(Screen.Detail.createRoute(mineralId))
                }
            )
        }

        composable(
            route = Screen.Camera.route,
            arguments = listOf(
                navArgument("mineralId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mineralId = backStackEntry.arguments?.getString("mineralId") ?: return@composable
            val application = LocalContext.current.applicationContext as MineraLogApplication

            val viewModelScope = rememberCoroutineScope()

            CameraCaptureScreen(
                mineralId = mineralId,
                onPhotoCaptured = { uri, photoType ->
                    // Save photo to repository
                    viewModelScope.launch {
                        try {
                            val fileName = uri.lastPathSegment ?: "photo_${System.currentTimeMillis()}.jpg"
                            val photo = net.meshcore.mineralog.domain.model.Photo(
                                id = java.util.UUID.randomUUID().toString(),
                                mineralId = mineralId,
                                type = photoType.name,
                                caption = null,
                                takenAt = java.time.Instant.now(),
                                fileName = fileName
                            )
                            application.mineralRepository.insertPhoto(photo)
                        } catch (e: Exception) {
                            android.util.Log.e("Navigation", "Failed to save photo", e)
                        } finally {
                            navController.popBackStack()
                        }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PhotoGallery.route,
            arguments = listOf(
                navArgument("mineralId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mineralId = backStackEntry.arguments?.getString("mineralId") ?: return@composable

            PhotoGalleryScreen(
                mineralId = mineralId,
                onNavigateBack = { navController.popBackStack() },
                onPhotoClick = { photoId ->
                    navController.navigate(Screen.PhotoFullscreen.createRoute(mineralId, photoId))
                },
                onCameraClick = { id ->
                    navController.navigate(Screen.Camera.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.PhotoFullscreen.route,
            arguments = listOf(
                navArgument("mineralId") { type = NavType.StringType },
                navArgument("photoId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val mineralId = backStackEntry.arguments?.getString("mineralId") ?: return@composable
            val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable

            FullscreenPhotoViewerScreen(
                mineralId = mineralId,
                initialPhotoId = photoId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
