package com.argumentor.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.argumentor.ui.backup.BackupScreen
import com.argumentor.ui.dashboard.DashboardScreen
import com.argumentor.ui.debate.DebateScreen
import com.argumentor.ui.editor.TopicEditorScreen
import com.argumentor.ui.fallacies.FallacyScreen
import com.argumentor.ui.stats.StatsScreen

@Composable
fun ArguMentorApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = ArguMentorDestination.Dashboard.route
    ) {
        composable(ArguMentorDestination.Dashboard.route) {
            DashboardScreen(
                onTopic = { navController.navigate(ArguMentorDestination.TopicEditor.createRoute(it)) },
                onDebate = { navController.navigate(ArguMentorDestination.Debate.createRoute(it)) },
                onStats = { navController.navigate(ArguMentorDestination.Stats.createRoute(it)) },
                onBackup = { navController.navigate(ArguMentorDestination.Backup.route) },
                onFallacies = { navController.navigate(ArguMentorDestination.Fallacies.route) }
            )
        }
        composable(
            route = ArguMentorDestination.TopicEditor.route,
            arguments = listOf(navArgument(TOPIC_ID_ARG) { type = NavType.LongType })
        ) {
            TopicEditorScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDebate = { navController.navigate(ArguMentorDestination.Debate.createRoute(it)) },
                onOpenStats = { navController.navigate(ArguMentorDestination.Stats.createRoute(it)) }
            )
        }
        composable(
            route = ArguMentorDestination.Debate.route,
            arguments = listOf(navArgument(TOPIC_ID_ARG) { type = NavType.LongType })
        ) {
            DebateScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = ArguMentorDestination.Stats.route,
            arguments = listOf(navArgument(TOPIC_ID_ARG) { type = NavType.LongType })
        ) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(ArguMentorDestination.Backup.route) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(ArguMentorDestination.Fallacies.route) {
            FallacyScreen(onBack = { navController.popBackStack() })
        }
    }
}
