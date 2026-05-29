package com.porrawc2026.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.porrawc2026.app.ui.screens.home.HomeScreen
import com.porrawc2026.app.ui.screens.groups.MatchesScreen
import com.porrawc2026.app.ui.screens.questions.QuestionsScreen

@Composable
fun PorraNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToMatches = { navController.navigate(Screen.Matches.route) },
                onNavigateToQuestions = { navController.navigate(Screen.Questions.route) }
            )
        }

        composable(Screen.Matches.route) {
            MatchesScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Questions.route) {
            QuestionsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
