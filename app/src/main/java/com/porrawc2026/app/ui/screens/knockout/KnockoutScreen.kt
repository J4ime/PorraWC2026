package com.porrawc2026.app.ui.screens.knockout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnockoutScreen(
    onBackClick: () -> Unit,
    viewModel: KnockoutViewModel = hiltViewModel()
) {
    val matches by viewModel.matches.collectAsState()
    val predictions by viewModel.predictions.collectAsState()

    val rounds = listOf(
        "Dieciseisavos" to "DIECISEISAVOS - 20 pts",
        "Octavos" to "OCTAVOS - 40 pts",
        "Cuartos" to "CUARTOS - 80 pts",
        "Semifinales" to "SEMIFINALES - 160 pts",
        "3er puesto" to "3er PUESTO - 250 pts",
        "Final" to "FINAL - 500 pts"
    )

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
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
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
            rounds.forEach { (round, title) ->
                val roundMatches = matches.filter { it.knockoutRound == round }
                val roundPredictions = predictions.filter { it.round == round }

                if (roundMatches.isNotEmpty() || roundPredictions.isNotEmpty()) {
                    item {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = WCGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }

                    items(roundPredictions) { prediction ->
                        KnockoutPredictionCard(
                            prediction = prediction,
                            roundPoints = when (round) {
                                "Dieciseisavos" -> 20
                                "Octavos" -> 40
                                "Cuartos" -> 80
                                "Semifinales" -> 160
                                "3er puesto" -> 250
                                "Final" -> 500
                                else -> 0
                            },
                            onPickWinner = { winner ->
                                viewModel.savePrediction(prediction.copy(winner = winner))
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "1 = Gana equipo izquierda · 2 = Gana equipo derecha",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun KnockoutPredictionCard(
    prediction: KnockoutPredictionEntity,
    roundPoints: Int,
    onPickWinner: (Int) -> Unit
) {
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
                Text(
                    "+$roundPoints pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Home team
                Card(
                    onClick = { onPickWinner(1) },
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelectedHome) AccentGreen.copy(alpha = 0.2f) else SurfaceMedium
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = if (isSelectedHome) CardDefaults.outlinedCardBorder() else null
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
                            prediction.homeTeamRef,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelectedHome) AccentGreen else TextPrimary,
                            fontWeight = if (isSelectedHome) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    "VS",
                    style = MaterialTheme.typography.titleMedium,
                    color = WCGold,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Away team
                Card(
                    onClick = { onPickWinner(2) },
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelectedAway) AccentGreen.copy(alpha = 0.2f) else SurfaceMedium
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = if (isSelectedAway) CardDefaults.outlinedCardBorder() else null
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
                            prediction.awayTeamRef,
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
