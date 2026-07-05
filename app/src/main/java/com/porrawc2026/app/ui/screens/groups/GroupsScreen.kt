package com.porrawc2026.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.domain.model.KnockoutCalculator
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import com.porrawc2026.app.ui.theme.*
import com.porrawc2026.app.util.ExcelParser

@Composable
fun MatchesScreen(scrollTrigger: Int = 0, viewModel: GroupsViewModel = hiltViewModel()) {
    val allMatches by viewModel.allMatches.collectAsStateWithLifecycle()
    val allTeams by viewModel.allTeams.collectAsStateWithLifecycle()
    val koPredictions by viewModel.allKnockoutPredictions.collectAsStateWithLifecycle()
    val koPointsMap by viewModel.knockoutPointsMap.collectAsStateWithLifecycle()
    val sorted = remember(allMatches) { allMatches.sortedBy { it.dateTime } }
    val listState = rememberLazyListState()

    val rounds = listOf(
        "Dieciseisavos" to 20,
        "Octavos" to 40,
        "Cuartos" to 80,
        "Semifinales" to 160,
        "3er puesto" to 200,
        "Final" to 500
    )

    val liveRoundLists = remember(allMatches) {
        KnockoutCalculator.buildLiveRoundLists(allMatches)
    }

    data class KOItem(val team: String, val points: Int, val userPredicted: Boolean, val teamsKnown: Boolean, val correct: Boolean)

    val isRef: (String) -> Boolean = { resolved ->
        (resolved.startsWith("W") && resolved.drop(1).toIntOrNull() != null) ||
        (resolved.startsWith("L") && resolved.drop(1).toIntOrNull() != null) ||
        (resolved.startsWith("Ganador ") && resolved.removePrefix("Ganador ").toIntOrNull() != null) ||
        (resolved.startsWith("Perdedor ") && resolved.removePrefix("Perdedor ").toIntOrNull() != null)
    }

    val roundItems = remember(liveRoundLists, koPredictions, allMatches, koPointsMap) {
        val result = mutableMapOf<String, List<KOItem>>()
        val predsByRound = koPredictions.groupBy { it.round }
        for ((round, _) in rounds) {
            val actualTeams = liveRoundLists[round].orEmpty()
            val roundPreds = predsByRound[round].orEmpty()
            if (roundPreds.isEmpty()) continue
            result[round] = roundPreds.flatMap { pred ->
                val match = allMatches.firstOrNull { it.id == pred.matchNumber }
                val homeRaw = PointsCalculator.resolvePredictionTeamName(pred.homeTeamRef, koPredictions)
                val awayRaw = PointsCalculator.resolvePredictionTeamName(pred.awayTeamRef, koPredictions)
                val teams = listOfNotNull(
                    if (isRef(homeRaw)) null else homeRaw,
                    if (isRef(awayRaw)) null else awayRaw
                )
                teams.map { team ->
                    val correct = actualTeams.any { TeamNameNormalizer.matches(it, team) }
                    val points = if (correct) PointsCalculator.getKnockoutPoints(round) else 0
                    KOItem(team, points, userPredicted = true, teamsKnown = true, correct)
                }
            }
            if (result[round].orEmpty().isEmpty()) result.remove(round)
        }
        result
    }

    val loading = sorted.isEmpty() || (koPredictions.isNotEmpty() && koPointsMap.isEmpty())

    LaunchedEffect(scrollTrigger) {
        if (sorted.isEmpty()) return@LaunchedEffect
        var lazyIdx = 1
        var lastFinalIdx = -1
        for (match in sorted) {
            val status = fmtDate(match)
            if (status == "EN JUEGO") {
                listState.animateScrollToItem(lazyIdx)
                return@LaunchedEffect
            }
            if (status == "FINAL") lastFinalIdx = lazyIdx
            lazyIdx++
        }
        if (lastFinalIdx >= 0) {
            listState.animateScrollToItem(lastFinalIdx)
        }
    }

    Box(Modifier.fillMaxSize().background(SurfaceDark)) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = WCGold
            )
        }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).background(GroupHeaderBg, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Fecha", Modifier.width(72.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("Partido", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                Text("Pts", Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
            }
        }

        for (match in sorted) {
            if (!match.isKnockout) {
                item { GroupMatchRow(match) }
            }
        }

        rounds.forEach { (round, pts) ->
            val items = roundItems[round].orEmpty()
            if (items.isNotEmpty()) {
                item(key = "ko_header_$round") {
                    Text(
                        text = "${round.uppercase()} — $pts pts",
                        style = MaterialTheme.typography.titleSmall,
                        color = WCGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                items(items.size, key = { "ko_team_${round}_${items[it].team}" }) { idx ->
                    val item = items[idx]
                    val bgColor = when {
                        item.teamsKnown && item.correct -> AccentGreen.copy(alpha = 0.1f)
                        item.teamsKnown -> AccentRed.copy(alpha = 0.1f)
                        else -> SurfaceMedium.copy(alpha = 0.3f)
                    }
                    val teamColor = when {
                        item.teamsKnown && item.correct -> AccentGreen
                        item.teamsKnown -> AccentRed
                        else -> TextPrimary
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ExcelParser.getFlagEmoji(item.team),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.team,
                            fontSize = 11.sp,
                            color = teamColor,
                            modifier = Modifier.weight(1f)
                        )
                        val statusText = when {
                            item.teamsKnown && item.correct -> "✓"
                            item.teamsKnown -> "0"
                            else -> ""
                        }
                        if (statusText.isNotBlank()) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            color = teamColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(14.dp)
                        )
                        }
                        Text(
                            text = if (item.points > 0) "${item.points}" else "",
                            fontSize = 11.sp,
                            color = if (item.points > 0) AccentGreen else TextMuted,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
    }
}

private fun fmtDate(match: MatchEntity): String {
    val dt = match.dateTime.ifBlank { return "" }
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
    sdf.timeZone = java.util.TimeZone.getTimeZone("Europe/Madrid")
    val date = sdf.parse(dt) ?: return dt.take(10)
    if (match.homeGoals != null && match.awayGoals != null) {
        if (date.after(java.util.Date())) {
            val dayAbbr = java.text.SimpleDateFormat("EEE", java.util.Locale("es", "ES")).format(date).replace(".", "").uppercase()
            val dayNum = java.text.SimpleDateFormat("dd", java.util.Locale.US).format(date)
            val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(date)
            return "$dayAbbr$dayNum $time"
        }
        return "FINAL"
    }
    val now = java.util.Date()
    val end = java.util.Date(date.time + 150L * 60 * 1000)
    if (now.after(date) && now.before(end)) return "EN JUEGO"
    val dayAbbr = java.text.SimpleDateFormat("EEE", java.util.Locale("es", "ES")).format(date).replace(".", "").uppercase()
    val dayNum = java.text.SimpleDateFormat("dd", java.util.Locale.US).format(date)
    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(date)
    return "$dayAbbr$dayNum $time"
}

@Composable
private fun GroupMatchRow(match: MatchEntity) {
    val isFuture = kotlin.run {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("Europe/Madrid")
        sdf.parse(match.dateTime)?.after(java.util.Date()) ?: false
    }
    val hasResult = !isFuture && match.homeGoals != null && match.awayGoals != null
    val hasPred = match.predictedHomeGoals != null && match.predictedAwayGoals != null
    val pts = if (hasResult) match.pointsEarned else 0

    val bgColor = when {
        hasResult && pts >= 30 -> AccentGreen.copy(alpha = 0.1f)
        hasResult && pts > 0 -> AccentBlue.copy(alpha = 0.1f)
        hasResult -> AccentRed.copy(alpha = 0.1f)
        else -> SurfaceMedium.copy(alpha = 0.3f)
    }

    Row(Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(fmtDate(match), Modifier.width(72.dp), fontSize = 9.sp, color = WCGold, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)

        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(match.homeTeam, fontSize = 11.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text(if (hasPred) " ${match.predictedHomeGoals}-${match.predictedAwayGoals} " else " - ", fontSize = 11.sp, color = if (hasPred) TextPrimary else TextMuted, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(match.awayTeam, fontSize = 11.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }

        val ptsText = if (pts > 0) "$pts" else "0"
        val ptsColor = if (pts > 0) AccentGreen else TextMuted
        Text(ptsText, Modifier.width(24.dp), fontSize = 11.sp, color = ptsColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}


