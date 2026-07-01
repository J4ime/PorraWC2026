package com.porrawc2026.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LiveHelp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porrawc2026.app.R
import com.porrawc2026.app.ui.screens.goalscorers.GoalscorersScreen
import com.porrawc2026.app.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.porrawc2026.app.ui.screens.groups.MatchesScreen
import com.porrawc2026.app.ui.screens.home.AjustesScreen
import com.porrawc2026.app.ui.screens.home.HomeScreen
import com.porrawc2026.app.ui.screens.home.HomeViewModel
import com.porrawc2026.app.ui.screens.knockout.KnockoutScreen
import com.porrawc2026.app.ui.screens.players.PlayersScreen
import com.porrawc2026.app.ui.screens.questions.QuestionsScreen
import com.porrawc2026.app.ui.screens.results.ResultsScreen
import kotlinx.coroutines.launch

data class NavItem(val label: String, val icon: ImageVector)

sealed class DetailScreen {
    data object Results : DetailScreen()
    data object Knockout : DetailScreen()
    data object Players : DetailScreen()
}

@Composable
fun PorraNavGraph() {
    val homeVM: HomeViewModel = hiltViewModel()
    val totalPoints by homeVM.totalPoints.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { 5 })
    val scope = rememberCoroutineScope()

    var detailScreen by remember { mutableStateOf<DetailScreen?>(null) }
    var homeRefreshTrigger by remember { mutableStateOf(0) }
    var partidosScrollTrigger by remember { mutableStateOf(0) }

    val navItems = listOf(
        NavItem("Goleadores", Icons.Filled.SportsSoccer),
        NavItem("Partidos", Icons.Filled.Scoreboard),
        NavItem("Inicio", Icons.Filled.Home),
        NavItem("Preguntas", Icons.AutoMirrored.Filled.LiveHelp),
        NavItem("Ajustes", Icons.Filled.Settings)
    )

    Column(modifier = Modifier.fillMaxSize().background(SurfaceDark)) {
        Box(modifier = Modifier.fillMaxWidth().background(WCDarkBlue).statusBarsPadding().height(56.dp).padding(horizontal = 12.dp)) {
            val headerTitle = when {
                detailScreen != null -> when (detailScreen) {
                    is DetailScreen.Results -> "RESULTADOS"
                    is DetailScreen.Knockout -> "ELIMINATORIAS"
                    is DetailScreen.Players -> "JUGADORES"
                    null -> "PORRA WC 2026"
                }
                else -> when (pagerState.currentPage) {
                    0 -> "GOLEADORES"
                    1 -> "PARTIDOS"
                    2 -> "PORRA WC 2026"
                    3 -> "PREGUNTAS"
                    4 -> "AJUSTES"
                    else -> "PORRA WC 2026"
                }
            }
            Image(painter = painterResource(R.drawable.logo_porra), contentDescription = "WC2026", modifier = Modifier.size(36.dp).align(Alignment.CenterStart), contentScale = ContentScale.Fit)
            Text(headerTitle, Modifier.align(Alignment.Center).padding(horizontal = 48.dp), style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(SurfaceLight).align(Alignment.CenterEnd), contentAlignment = Alignment.Center) {
                Text("$totalPoints", style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (detailScreen != null) {
                BackHandler { detailScreen = null }
                when (val screen = detailScreen) {
                    is DetailScreen.Results -> ResultsScreen(onBackClick = { detailScreen = null })
                    is DetailScreen.Knockout -> KnockoutScreen(onBackClick = { detailScreen = null })
                    is DetailScreen.Players -> PlayersScreen(onBackClick = { detailScreen = null })
                    null -> {}
                }
            } else {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (page) {
                        0 -> GoalscorersScreen()
                        1 -> MatchesScreen(scrollTrigger = partidosScrollTrigger)
                        2 -> HomeScreen(refreshTrigger = homeRefreshTrigger)
                        3 -> QuestionsScreen()
                        4 -> AjustesScreen()
                    }
                }
            }
        }

        NavigationBar(
            containerColor = WCDarkBlue,
            contentColor = TextMuted
        ) {
            navItems.forEachIndexed { index, item ->
                val selected = pagerState.currentPage == index && detailScreen == null
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        detailScreen = null
                        if (index == 1) partidosScrollTrigger++
                        if (index == 2) homeRefreshTrigger++
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (selected) AccentOrange else TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
