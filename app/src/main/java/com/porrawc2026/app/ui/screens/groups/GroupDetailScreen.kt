package com.porrawc2026.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.ui.theme.*
import com.porrawc2026.app.domain.model.StandingsCalculator
import com.porrawc2026.app.domain.model.StandingEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupLetter: String,
    onBackClick: () -> Unit,
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val teams by viewModel.getGroupTeams(groupLetter)
        .collectAsState(initial = emptyList())
    val matches by viewModel.getGroupMatches(groupLetter)
        .collectAsState(initial = emptyList())

    val standings = remember(teams, matches) {
        StandingsCalculator.calculateGroupStandings(
            matches = matches,
            groupTeams = teams.map { it.name }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        TopAppBar(
            title = {
                Text(
                    "GRUPO $groupLetter",
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
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Standings header
            item {
                Text(
                    text = "CLASIFICACIÓN",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp),
                    letterSpacing = 2.sp
                )
                StandingHeader()
            }

            items(standings.size) { index ->
                StandingRow(entry = standings[index], position = index + 1)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PARTIDOS - 10pts goles · 30pts resultado",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(matches) { match ->
                MatchPredictionCard(match = match)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StandingHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GroupHeaderBg, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#", Modifier.width(24.dp), color = TextMuted, style = MaterialTheme.typography.labelSmall)
        Text("Equipo", Modifier.weight(1f), color = TextMuted, style = MaterialTheme.typography.labelSmall)
        Text("PJ", Modifier.width(28.dp), color = TextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Text("G", Modifier.width(24.dp), color = TextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Text("E", Modifier.width(24.dp), color = TextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Text("P", Modifier.width(24.dp), color = TextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Text("GF", Modifier.width(28.dp), color = TextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Text("GC", Modifier.width(28.dp), color = TextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Text("Pts", Modifier.width(32.dp), color = WCGold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StandingRow(entry: StandingEntry, position: Int) {
    val bgColor = if (position <= 2) AccentGreen.copy(alpha = 0.1f) else SurfaceMedium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$position", Modifier.width(24.dp), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Text(entry.teamName, Modifier.weight(1f), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Text("${entry.played}", Modifier.width(28.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${entry.won}", Modifier.width(24.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${entry.drawn}", Modifier.width(24.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${entry.lost}", Modifier.width(24.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${entry.goalsFor}", Modifier.width(28.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${entry.goalsAgainst}", Modifier.width(28.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("${entry.points}", Modifier.width(32.dp), color = WCGold, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MatchPredictionCard(match: MatchEntity) {
    val hasRealResult = match.homeGoals != null && match.awayGoals != null
    val hasPrediction = match.predictedHomeGoals != null && match.predictedAwayGoals != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (match.matchday == "J1") MatchBg else MatchBgAlternate),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(match.matchday, style = MaterialTheme.typography.labelSmall, color = WCGold)
                Row {
                    if (hasRealResult) {
                        Text(
                            "${match.homeGoals} - ${match.awayGoals}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (hasRealResult) "${match.pointsEarned} pts" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (match.pointsEarned > 0) AccentGreen else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(match.homeTeam, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Medium)
                Text(
                    text = match.predictedHomeGoals?.toString() ?: "-",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasPrediction) TextPrimary else TextMuted,
                    modifier = Modifier.width(50.dp), textAlign = TextAlign.Center
                )
                Text(" - ", style = MaterialTheme.typography.bodyLarge, color = TextMuted)
                Text(
                    text = match.predictedAwayGoals?.toString() ?: "-",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasPrediction) TextPrimary else TextMuted,
                    modifier = Modifier.width(50.dp), textAlign = TextAlign.Center
                )
                Text(match.awayTeam, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            }
        }
    }
}
