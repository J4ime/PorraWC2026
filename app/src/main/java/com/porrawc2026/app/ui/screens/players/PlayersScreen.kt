package com.porrawc2026.app.ui.screens.players

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    onBackClick: () -> Unit,
    viewModel: PlayersViewModel = hiltViewModel()
) {
    val predictions by viewModel.predictions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        TopAppBar(
            title = {
                Text(
                    "GOLEADORES",
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Elige tus 3 jugadores favoritos.\nPuntuación por gol (sin penaltis):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            items(predictions.sortedBy { it.rank }, key = { prediction -> prediction.rank }) { prediction ->
                PlayerCard(prediction = prediction)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = AccentBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "1er: 50 pts · 2do: 30 pts · 3er: 10 pts\nPuntos por cada gol marcado",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PlayerCard(prediction: PlayerPredictionEntity) {
    val colors = listOf(WCGold, AccentBlue, AccentOrange)
    val medals = listOf("🥇", "🥈", "🥉")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    medals[prediction.rank - 1],
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "${prediction.rank}er Goleador",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors[prediction.rank - 1],
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${prediction.pointsPerGoal} pts por gol",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InputBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.SportsSoccer, contentDescription = null, tint = colors[prediction.rank - 1])
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = prediction.predictedName ?: "-",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (prediction.predictedName != null) TextPrimary else TextMuted,
                    fontWeight = if (prediction.predictedName != null) FontWeight.Medium else FontWeight.Normal
                )
            }

            if (prediction.goalsScored > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Goles: ${prediction.goalsScored} → +${prediction.pointsEarned} pts",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
