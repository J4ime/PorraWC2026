package com.porrawc2026.app.ui.navigation

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.R
import com.porrawc2026.app.ui.screens.goalscorers.GoalscorersScreen
import com.porrawc2026.app.ui.screens.groups.MatchesScreen
import com.porrawc2026.app.ui.screens.home.AjustesScreen
import com.porrawc2026.app.ui.screens.home.HomeScreen
import com.porrawc2026.app.ui.screens.home.HomeViewModel
import com.porrawc2026.app.ui.screens.questions.QuestionsScreen
import kotlinx.coroutines.launch

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun PorraNavGraph() {
    val homeVM: HomeViewModel = hiltViewModel()
    val totalPoints by homeVM.totalPoints.collectAsState()
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 5 })
    val scope = rememberCoroutineScope()
    val pageTitles = listOf("GOLEADORES", "INICIO", "PARTIDOS", "PREGUNTAS", "AJUSTES")

    val navItems = listOf(
        NavItem("Goleadores", Icons.Filled.EmojiEvents),
        NavItem("Inicio", Icons.Filled.Home),
        NavItem("Partidos", Icons.Filled.SportsSoccer),
        NavItem("Preguntas", Icons.AutoMirrored.Filled.LiveHelp),
        NavItem("Ajustes", Icons.Filled.Settings)
    )

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E))) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).statusBarsPadding().height(56.dp).padding(horizontal = 12.dp)) {
            val headerTitle = when (pagerState.currentPage) {
                0 -> "GOLEADORES"
                 1 -> "PORRA WC 2026"
                2 -> "PARTIDOS"
                3 -> "PREGUNTAS"
                4 -> "AJUSTES"
                else -> "PORRA WC 2026"
            }
            Image(painter = painterResource(R.drawable.logo_porra), contentDescription = "WC2026", modifier = Modifier.size(36.dp).align(Alignment.CenterStart), contentScale = ContentScale.Fit)
            Text(headerTitle, Modifier.align(Alignment.Center).padding(horizontal = 48.dp), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF333333)).align(Alignment.CenterEnd), contentAlignment = Alignment.Center) {
                Text("$totalPoints", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize(), userScrollEnabled = pagerState.currentPage != 1) { page ->
                when (page) {
                    0 -> GoalscorersScreen()
                    1 -> HomeScreen()
                    2 -> MatchesScreen()
                    3 -> QuestionsScreen()
                    4 -> AjustesScreen()
                }
            }
        }

        NavigationBar(
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color(0xFF555555)
        ) {
            navItems.forEachIndexed { index, item ->
                val selected = pagerState.currentPage == index
                NavigationBarItem(
                    selected = selected,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (selected) Color(0xFFE65100) else Color(0xFF555555),
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
