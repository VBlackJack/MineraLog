package net.meshcore.mineralog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.meshcore.mineralog.ui.screens.home.HomeScreen
import net.meshcore.mineralog.ui.screens.detail.MineralDetailScreen
import net.meshcore.mineralog.ui.screens.add.AddMineralScreen
import net.meshcore.mineralog.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Add : Screen("add")
    data object Detail : Screen("detail/{mineralId}") {
        fun createRoute(mineralId: String) = "detail/$mineralId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun MineraLogNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
    deepLinkMineralId: String? = null
) {
    // Handle deep link
    LaunchedEffect(deepLinkMineralId) {
        deepLinkMineralId?.let { id ->
            navController.navigate(Screen.Detail.createRoute(id))
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
                onNavigateBack = { navController.popBackStack() }
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
    }
}
