package com.porrawc2026.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Inicio")
    data object Groups : Screen("groups", "Grupos")
    data object GroupDetail : Screen("group_detail/{groupLetter}", "Grupo") {
        fun createRoute(groupLetter: String) = "group_detail/$groupLetter"
    }
    data object Knockout : Screen("knockout", "Eliminatorias")
    data object Questions : Screen("questions", "Preguntas")
    data object Players : Screen("players", "Jugadores")
    data object Results : Screen("results", "Resultados")
}
