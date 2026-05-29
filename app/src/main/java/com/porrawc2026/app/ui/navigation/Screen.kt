package com.porrawc2026.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Inicio")
    data object Matches : Screen("matches", "Partidos")
    data object Questions : Screen("questions", "Preguntas")
}
