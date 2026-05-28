package com.porrawc2026.app.ui.screens.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onBackClick: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel()
) {
    val totalPoints by viewModel.totalPoints.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val matches by viewModel.allMatches.collectAsState()
    val questions by viewModel.questions.collectAsState()
    val playerPredictions by viewModel.playerPredictions.collectAsState()
    val knockoutPredictions by viewModel.knockoutPredictions.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { msg ->
            // Handle error
        }
    }

    val groupPoints = matches.filter { !it.isKnockout }.sumOf { it.pointsEarned }
    val kokoPoints = knockoutPredictions.sumOf { it.pointsEarned }
    val qPoints = questions.sumOf { it.pointsEarned }
    val pPoints = playerPredictions.sumOf { it.pointsEarned }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        TopAppBar(
            title = {
                Text(
                    "RESULTADOS",
                    style = MaterialTheme.typography.titleLarge,
                    color = WCGold,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                }
            },
            actions = {
                IconButton(
                    onClick = { viewModel.refreshLiveScores() },
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = WCGold,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Actualizar",
                            tint = WCGold
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WCDarkBlue)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Total points
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = WCGold.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "PUNTUACIÓN TOTAL",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "$totalPoints",
                            style = MaterialTheme.typography.displayLarge,
                            color = WCGold,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Points breakdown
            item {
                PointsBreakdownCard(
                    groupPoints = groupPoints,
                    knockoutPoints = kokoPoints,
                    questionPoints = qPoints,
                    playerPoints = pPoints
                )
            }

            // Group stage results
            item {
                SectionHeader("FASE DE GRUPOS", "+$groupPoints pts")
            }

            items(matches.filter { !it.isKnockout }.take(12)) { match ->
                ResultRow(match)
            }

            if (matches.count { !it.isKnockout } > 12) {
                item {
                    TextButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver todos los ${matches.count { !it.isKnockout }} partidos...", color = AccentBlue)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PointsBreakdownCard(
    groupPoints: Int,
    knockoutPoints: Int,
    questionPoints: Int,
    playerPoints: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "DESGLOSE DE PUNTOS",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            PointsRow("Fase de Grupos", groupPoints, WCGold)
            PointsRow("Eliminatorias", knockoutPoints, AccentGreen)
            PointsRow("50 Preguntas", questionPoints, AccentBlue)
            PointsRow("Goleadores", playerPoints, AccentOrange)
            HorizontalDivider(color = SurfaceMedium, modifier = Modifier.padding(vertical = 4.dp))
            PointsRow("TOTAL", groupPoints + knockoutPoints + questionPoints + playerPoints, WCGold, bold = true)
        }
    }
}

@Composable
private fun PointsRow(label: String, points: Int, color: androidx.compose.ui.graphics.Color, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            "+$points",
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SectionHeader(title: String, points: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = WCGold,
            fontWeight = FontWeight.Bold
        )
        Text(
            points,
            style = MaterialTheme.typography.labelMedium,
            color = AccentGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ResultRow(match: MatchEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceMedium, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(match.groupName, style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.width(60.dp))
        Text(match.homeTeam, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary, textAlign = TextAlign.End)
        Spacer(Modifier.width(8.dp))
        if (match.homeGoals != null && match.awayGoals != null) {
            Text(
                "${match.homeGoals} - ${match.awayGoals}",
                style = MaterialTheme.typography.bodySmall,
                color = WCGold,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text("vs", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Spacer(Modifier.width(8.dp))
        Text(match.awayTeam, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        if (match.pointsEarned > 0) {
            Text(" +${match.pointsEarned}", style = MaterialTheme.typography.labelSmall, color = AccentGreen, fontWeight = FontWeight.Bold)
        }
    }
}
