package com.porrawc2026.app.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.R
import com.porrawc2026.app.ui.screens.goalscorers.GoalscorersScreen
import com.porrawc2026.app.ui.screens.groups.MatchesScreen
import com.porrawc2026.app.ui.screens.home.HomeScreen
import com.porrawc2026.app.ui.screens.home.HomeViewModel
import com.porrawc2026.app.ui.screens.questions.QuestionsScreen
import kotlinx.coroutines.launch

@Composable
fun PorraNavGraph() {
    val homeVM: HomeViewModel = hiltViewModel()
    val totalPoints by homeVM.totalPoints.collectAsState()
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val pageTitles = listOf("GOLEADORES", "INICIO", "PARTIDOS", "PREGUNTAS")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E))) {
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E)).statusBarsPadding().height(56.dp).padding(horizontal = 12.dp)) {
            val headerTitle = when (pagerState.currentPage) {
                0 -> "GOLEADORES"
                1 -> "PORRA MUNDIAL 26"
                2 -> "PARTIDOS"
                3 -> "PREGUNTAS"
                else -> "PORRA MUNDIAL 26"
            }
            Image(painter = painterResource(R.drawable.logo_porra), contentDescription = "WC2026", modifier = Modifier.size(36.dp).align(Alignment.CenterStart), contentScale = ContentScale.Fit)
            Text(headerTitle, Modifier.align(Alignment.Center).padding(horizontal = 48.dp), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF333333)).align(Alignment.CenterEnd), contentAlignment = Alignment.Center) {
                Text("$totalPoints", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> GoalscorersScreen()
                    1 -> HomeScreen()
                    2 -> MatchesScreen()
                    3 -> QuestionsScreen()
                }
            }

            val leftPages = (0 until pagerState.currentPage).reversed().toList()
            if (leftPages.isNotEmpty()) {
                Column(modifier = Modifier.align(Alignment.CenterStart).zIndex(10f), verticalArrangement = Arrangement.Center) {
                    leftPages.forEach { page ->
                        Box(modifier = Modifier.height(100.dp).clickable { scope.launch { pagerState.animateScrollToPage(page) } }, contentAlignment = Alignment.TopStart) {
                            Text(pageTitles[page], modifier = Modifier.graphicsLayer { rotationZ = -90f; transformOrigin = TransformOrigin(0f, 0f) }, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                        }
                    }
                }
            }

            val rightPages = ((pagerState.currentPage + 1) until 4).toList()
            if (rightPages.isNotEmpty()) {
                Column(modifier = Modifier.align(Alignment.CenterEnd).zIndex(10f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rightPages.forEach { page ->
                        Box(modifier = Modifier.width(12.dp).clickable { scope.launch { pagerState.animateScrollToPage(page) } }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                pageTitles[page].forEach { c ->
                                    Text("$c", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, lineHeight = 8.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).navigationBarsPadding(), horizontalArrangement = Arrangement.Center) {
            repeat(4) { index ->
                Box(modifier = Modifier.padding(horizontal = 4.dp).size(if (pagerState.currentPage == index) 5.dp else 3.dp).clip(CircleShape).background(if (pagerState.currentPage == index) Color(0xFFE65100) else Color(0xFF444444)))
            }
        }
    }
}
