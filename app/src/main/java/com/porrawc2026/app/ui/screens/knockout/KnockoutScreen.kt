package com.porrawc2026.app.ui.screens.knockout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.ui.components.TournamentBracket
import com.porrawc2026.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnockoutScreen(
    onBackClick: () -> Unit,
    viewModel: KnockoutViewModel = hiltViewModel()
) {
    val predictions by viewModel.predictions.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        TopAppBar(
            title = {
                Text(
                    "ELIMINATORIAS",
                    style = MaterialTheme.typography.titleLarge,
                    color = WCGold,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WCDarkBlue)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = WCDarkBlue,
            contentColor = WCGold
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("BRACKET", style = MaterialTheme.typography.labelMedium) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("PREDECIR", style = MaterialTheme.typography.labelMedium) }
            )
        }

        when (selectedTab) {
            0 -> TournamentBracket(
                predictions = predictions,
                modifier = Modifier.fillMaxSize()
            )
            1 -> KnockoutPredictionList(predictions = predictions)
        }
    }
}

@Composable
private fun KnockoutPredictionList(predictions: List<KnockoutPredictionEntity>) {
    val resolvedHome = remember(predictions) { predictions.associate { it.matchNumber to PointsCalculator.resolvePredictionTeamName(it.homeTeamRef, predictions) } }
    val resolvedAway = remember(predictions) { predictions.associate { it.matchNumber to PointsCalculator.resolvePredictionTeamName(it.awayTeamRef, predictions) } }
    val rounds = listOf(
        "Dieciseisavos" to 20,
        "Octavos" to 40,
        "Cuartos" to 80,
        "Semifinales" to 160,
        "3er puesto" to 250,
        "Final" to 500
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rounds.forEach { (round, pts) ->
            val roundPredictions = predictions.filter { it.round == round }
            if (roundPredictions.isNotEmpty()) {
                item {
                    Text(
                        text = "${round.uppercase()} — $pts pts",
                        style = MaterialTheme.typography.titleMedium,
                        color = WCGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                items(roundPredictions, key = { prediction -> prediction.matchNumber }) { prediction ->
                    KnockoutPredictionCard(
                        prediction = prediction,
                        homeTeam = resolvedHome[prediction.matchNumber] ?: prediction.homeTeamRef,
                        awayTeam = resolvedAway[prediction.matchNumber] ?: prediction.awayTeamRef
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun KnockoutPredictionCard(prediction: KnockoutPredictionEntity, homeTeam: String = prediction.homeTeamRef, awayTeam: String = prediction.awayTeamRef) {
    val isSelectedHome = prediction.winner == 1
    val isSelectedAway = prediction.winner == 2

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Partido ${prediction.matchNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                if (prediction.pointsEarned > 0) {
                    Text(
                        "+${prediction.pointsEarned} pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelectedHome) AccentGreen.copy(alpha = 0.2f) else SurfaceMedium
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isSelectedHome) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            homeTeam,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelectedHome) AccentGreen else TextPrimary,
                            fontWeight = if (isSelectedHome) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Text(
                    "VS",
                    style = MaterialTheme.typography.titleMedium,
                    color = WCGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelectedAway) AccentGreen.copy(alpha = 0.2f) else SurfaceMedium
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isSelectedAway) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            awayTeam,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelectedAway) AccentGreen else TextPrimary,
                            fontWeight = if (isSelectedAway) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
