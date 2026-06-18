package com.porrawc2026.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.ui.theme.*

@Composable
fun TournamentBracket(
    predictions: List<KnockoutPredictionEntity>,
    modifier: Modifier = Modifier
) {
    val rounds = listOf(
        "Dieciseisavos" to 16,
        "Octavos" to 8,
        "Cuartos" to 4,
        "Semifinales" to 2,
        "Final" to 1
    )

    val roundColors = mapOf(
        "Dieciseisavos" to AccentBlue,
        "Octavos" to AccentGreen,
        "Cuartos" to AccentOrange,
        "Semifinales" to WCRed,
        "Final" to WCGold
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        val predictionsByRound = predictions.groupBy { it.round }

        rounds.forEach { (round, _) ->
            val roundPredictions = predictionsByRound[round].orEmpty()
            if (roundPredictions.isNotEmpty()) {
                val color = roundColors[round] ?: WCGold
                val pts = PointsCalculator.getKnockoutPoints(round)

                Text(
                    text = "${round.uppercase()} — $pts pts",
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    roundPredictions.forEach { prediction ->
                        BracketMatchRow(
                            prediction = prediction,
                            roundColor = color,
                            roundPoints = pts
                        )
                    }
                }
            }
        }

        // 3rd place
        val thirdPlace = predictionsByRound["3er puesto"]?.firstOrNull()
        if (thirdPlace != null) {
            val pts = PointsCalculator.getKnockoutPoints("3er puesto")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "3er PUESTO — $pts pts",
                style = MaterialTheme.typography.titleMedium,
                color = AccentOrange,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            BracketMatchRow(
                prediction = thirdPlace,
                roundColor = AccentOrange,
                roundPoints = pts
            )
        }
    }
}

@Composable
private fun BracketMatchRow(
    prediction: KnockoutPredictionEntity,
    roundColor: Color,
    roundPoints: Int
) {
    val isHomeWinner = prediction.winner == 1
    val isAwayWinner = prediction.winner == 2

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Match number
            Text(
                text = "${prediction.matchNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.width(24.dp)
            )

            // Home team
            Text(
                text = prediction.homeTeamRef,
                style = MaterialTheme.typography.bodySmall,
                color = if (isHomeWinner) AccentGreen else TextPrimary,
                fontWeight = if (isHomeWinner) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = " vs ",
                style = MaterialTheme.typography.labelSmall,
                color = roundColor,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Away team
            Text(
                text = prediction.awayTeamRef,
                style = MaterialTheme.typography.bodySmall,
                color = if (isAwayWinner) AccentGreen else TextPrimary,
                fontWeight = if (isAwayWinner) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Points
            if (prediction.pointsEarned > 0) {
                Text(
                    text = "+${prediction.pointsEarned}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
