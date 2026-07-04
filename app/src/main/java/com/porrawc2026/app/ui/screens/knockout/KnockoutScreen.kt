package com.porrawc2026.app.ui.screens.knockout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.remote.MatchScheduleProvider
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.ui.components.TournamentBracket
import com.porrawc2026.app.ui.theme.*
import com.porrawc2026.app.util.ExcelParser
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
            1 -> KnockoutSelectedTeamsList(
                predictions = predictions,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun KnockoutSelectedTeamsList(
    predictions: List<KnockoutPredictionEntity>,
    modifier: Modifier = Modifier
) {
    val rounds = listOf(
        "Dieciseisavos" to 20,
        "Octavos" to 40,
        "Cuartos" to 80,
        "Semifinales" to 160,
        "3er puesto" to 200,
        "Final" to 500
    )

    val currentRound = remember {
        getCurrentKnockoutRound()
    }

    val teamsByRound = remember(predictions) {
        predictions
            .filter { it.winner != null }
            .groupBy { it.round }
            .mapValues { (_, preds) ->
                preds.mapNotNull { p ->
                    val ref = if (p.winner == 1) p.homeTeamRef else p.awayTeamRef
                    val resolved = PointsCalculator.resolvePredictionTeamName(ref, predictions)
                    val isRef = (resolved.startsWith("W") && resolved.drop(1).toIntOrNull() != null) ||
                            (resolved.startsWith("L") && resolved.drop(1).toIntOrNull() != null)
                    if (isRef) null else resolved
                }.sorted()
            }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(predictions) {
        if (predictions.isNotEmpty()) {
            var targetIdx = 0
            for ((round, _) in rounds) {
                if (round == currentRound) break
                val teams = teamsByRound[round].orEmpty()
                if (teams.isNotEmpty()) targetIdx += 1 + teams.size
            }
            if (targetIdx > 0) {
                listState.animateScrollToItem(targetIdx)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rounds.forEach { (round, pts) ->
            val teams = teamsByRound[round].orEmpty()
            if (teams.isNotEmpty()) {
                item(key = "header_$round") {
                    Text(
                        text = "${round.uppercase()} — $pts pts",
                        style = MaterialTheme.typography.titleMedium,
                        color = WCGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(teams, key = { "team_${round}_$it" }) { team ->
                    TeamRow(team = team, points = pts)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun TeamRow(team: String, points: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MatchBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ExcelParser.getFlagEmoji(team),
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = team,
            fontSize = 14.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$points",
            fontSize = 14.sp,
            color = WCGold,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getCurrentKnockoutRound(): String {
    val schedule = MatchScheduleProvider.getHardcodedSchedule()
    val now = Instant.now()
    val madridZone = ZoneId.of("Europe/Madrid")
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    fun parseInstant(dateStr: String?): Instant? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(dateStr, fmt).atZone(madridZone).toInstant()
        } catch (_: Exception) { null }
    }

    val semiStart = parseInstant(schedule[101]?.date)
    val cuartosStart = parseInstant(schedule[97]?.date)
    val octavosStart = parseInstant(schedule[89]?.date)
    val dieciseisavosStart = parseInstant(schedule[73]?.date)

    return when {
        semiStart != null && now >= semiStart -> "Semifinales"
        cuartosStart != null && now >= cuartosStart -> "Cuartos"
        octavosStart != null && now >= octavosStart -> "Octavos"
        dieciseisavosStart != null && now >= dieciseisavosStart -> "Dieciseisavos"
        else -> "Dieciseisavos"
    }
}
