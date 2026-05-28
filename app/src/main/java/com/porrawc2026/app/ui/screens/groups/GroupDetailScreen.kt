package com.porrawc2026.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.TeamEntity
import com.porrawc2026.app.ui.theme.*

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

            items(teams) { team ->
                StandingRow(team = team, position = team.rank)
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
                MatchPredictionCard(
                    match = match,
                    onPredictionChange = { home, away ->
                        viewModel.savePrediction(
                            match.copy(
                                predictedHomeGoals = home,
                                predictedAwayGoals = away
                            )
                        )
                    }
                )
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
private fun StandingRow(team: TeamEntity, position: Int) {
    val bgColor = if (position <= 2) AccentGreen.copy(alpha = 0.1f) else SurfaceMedium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$position", Modifier.width(24.dp), color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(team.flagEmoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(team.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
        }
        Text("0", Modifier.width(28.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("0", Modifier.width(24.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("0", Modifier.width(24.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("0", Modifier.width(24.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("0", Modifier.width(28.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("0", Modifier.width(28.dp), color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text("0", Modifier.width(32.dp), color = WCGold, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MatchPredictionCard(
    match: MatchEntity,
    onPredictionChange: (Int?, Int?) -> Unit
) {
    var homeGoals by remember(match.id) { mutableStateOf(match.predictedHomeGoals?.toString() ?: "") }
    var awayGoals by remember(match.id) { mutableStateOf(match.predictedAwayGoals?.toString() ?: "") }

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
                Text(
                    match.matchday,
                    style = MaterialTheme.typography.labelSmall,
                    color = WCGold
                )
                if (match.pointsEarned > 0) {
                    Text(
                        "+${match.pointsEarned} pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    match.homeTeam,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = homeGoals,
                    onValueChange = { value ->
                        if (value.length <= 2 && (value.isEmpty() || value.all { it.isDigit() })) {
                            homeGoals = value
                            onPredictionChange(
                                value.toIntOrNull(),
                                awayGoals.toIntOrNull()
                            )
                        }
                    },
                    modifier = Modifier.width(50.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                        color = TextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputBg,
                        unfocusedContainerColor = InputBg,
                        focusedBorderColor = WCGold,
                        unfocusedBorderColor = InputBorder
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text(
                    " - ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
                OutlinedTextField(
                    value = awayGoals,
                    onValueChange = { value ->
                        if (value.length <= 2 && (value.isEmpty() || value.all { it.isDigit() })) {
                            awayGoals = value
                            onPredictionChange(
                                homeGoals.toIntOrNull(),
                                value.toIntOrNull()
                            )
                        }
                    },
                    modifier = Modifier.width(50.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                        color = TextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputBg,
                        unfocusedContainerColor = InputBg,
                        focusedBorderColor = WCGold,
                        unfocusedBorderColor = InputBorder
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    match.awayTeam,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
            }

            // Real score display
            if (match.homeGoals != null && match.awayGoals != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Resultado real: ${match.homeGoals} - ${match.awayGoals}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentOrange,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
