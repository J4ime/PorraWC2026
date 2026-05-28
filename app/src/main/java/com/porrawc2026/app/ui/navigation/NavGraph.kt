package com.porrawc2026.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.porrawc2026.app.ui.screens.home.HomeScreen
import com.porrawc2026.app.ui.screens.groups.GroupsScreen
import com.porrawc2026.app.ui.screens.groups.GroupDetailScreen
import com.porrawc2026.app.ui.screens.knockout.KnockoutScreen
import com.porrawc2026.app.ui.screens.questions.QuestionsScreen
import com.porrawc2026.app.ui.screens.players.PlayersScreen
import com.porrawc2026.app.ui.screens.results.ResultsScreen

@Composable
fun PorraNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGroups = { navController.navigate(Screen.Groups.route) },
                onNavigateToKnockout = { navController.navigate(Screen.Knockout.route) },
                onNavigateToQuestions = { navController.navigate(Screen.Questions.route) },
                onNavigateToPlayers = { navController.navigate(Screen.Players.route) },
                onNavigateToResults = { navController.navigate(Screen.Results.route) }
            )
        }

        composable(Screen.Groups.route) {
            GroupsScreen(
                onGroupClick = { group ->
                    navController.navigate(Screen.GroupDetail.createRoute(group))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("groupLetter") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupLetter = backStackEntry.arguments?.getString("groupLetter") ?: "A"
            GroupDetailScreen(
                groupLetter = groupLetter,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Knockout.route) {
            KnockoutScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Questions.route) {
            QuestionsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Players.route) {
            PlayersScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Results.route) {
            ResultsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
