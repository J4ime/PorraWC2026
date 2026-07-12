package com.porrawc2026.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.util.Log
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.domain.model.KnockoutCalculator
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import com.porrawc2026.app.ui.theme.*
import com.porrawc2026.app.util.ExcelParser

@Composable
fun MatchesScreen(scrollTrigger: Int = 0, onRefreshRequest: () -> Unit = {}, viewModel: GroupsViewModel = hiltViewModel()) {
    val allMatches by viewModel.allMatches.collectAsStateWithLifecycle()
    val allTeams by viewModel.allTeams.collectAsStateWithLifecycle()
    val koPredictions by viewModel.allKnockoutPredictions.collectAsStateWithLifecycle()
    val koPointsMap by viewModel.knockoutPointsMap.collectAsStateWithLifecycle()
    val sorted = remember(allMatches) { allMatches.sortedBy { it.dateTime } }
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            onRefreshRequest()
            pullRefreshState.endRefresh()
        }
    }

    val rounds = listOf(
        "Dieciseisavos" to 20,
        "Octavos" to 40,
        "Cuartos" to 80,
        "Semifinales" to 160,
        "Final" to 500,
        "3er puesto" to 200,
        "Campeón" to 500
    )

    data class KOItem(val team: String, val points: Int, val correct: Boolean)

    val roundItems = remember(koPredictions, allMatches, koPointsMap) {
        val result = mutableMapOf<String, List<KOItem>>()
        val roundPts = mapOf("Dieciseisavos" to 20, "Octavos" to 40, "Cuartos" to 80,
            "Semifinales" to 160, "Final" to 500, "3er puesto" to 200)

        for ((round, ptsPerTeam) in roundPts) {
            val preds = koPredictions.filter { it.round == round }.sortedBy { it.matchNumber }
            if (preds.isEmpty()) continue
            val actualTeams = KnockoutCalculator.buildLiveRoundLists(allMatches)[round].orEmpty()
            val items = mutableListOf<KOItem>()
            val seenTeams = mutableListOf<String>()

            for (pred in preds) {
                val match = allMatches.firstOrNull { it.id == pred.matchNumber }
                val home = PointsCalculator.resolvePredictionTeamName(pred.homeTeamRef, koPredictions)
                val away = PointsCalculator.resolvePredictionTeamName(pred.awayTeamRef, koPredictions)

                if (round == "3er puesto") {
                    val winnerTeam = if (pred.winner == 1) home else if (pred.winner == 2) away else null
                    if (winnerTeam != null && winnerTeam.isNotBlank() && seenTeams.none { TeamNameNormalizer.matches(it, winnerTeam) }) {
                        seenTeams.add(winnerTeam)
                        val winnerEs = match?.winnerTeam?.let { TeamNameNormalizer.enToEs(it) }
                        val correct = winnerEs != null && TeamNameNormalizer.matches(winnerEs, winnerTeam)
                        items.add(KOItem(winnerTeam, if (correct) ptsPerTeam else 0, correct))
                    }
                } else {
                    for (team in listOf(home, away)) {
                        if (team.isBlank() || seenTeams.any { TeamNameNormalizer.matches(it, team) }) continue
                        seenTeams.add(team)
                        val correct = actualTeams.any { TeamNameNormalizer.matches(it, team) }
                        val pts = if (round == "Final") 250 else ptsPerTeam
                        items.add(KOItem(team, if (correct) pts else 0, correct))
                    }
                }
            }
            if (items.isNotEmpty()) result[round] = items
        }

        val finalPred = koPredictions.firstOrNull { it.round == "Final" }
        if (finalPred != null && "Campeón" !in result) {
            val match = allMatches.firstOrNull { it.id == finalPred.matchNumber }
            val home = PointsCalculator.resolvePredictionTeamName(finalPred.homeTeamRef, koPredictions)
            val away = PointsCalculator.resolvePredictionTeamName(finalPred.awayTeamRef, koPredictions)
            val predictedChampion = when (finalPred.winner) {
                1 -> home; 2 -> away; else -> null
            }
            if (predictedChampion != null && predictedChampion.isNotBlank()) {
                val actualWinner = match?.winnerTeam?.let { TeamNameNormalizer.enToEs(it) }
                    ?: if (match != null && match.homeGoals != null && match.awayGoals != null)
                        KnockoutCalculator.getWinnerSimple(match) else null
                val correct = actualWinner != null && TeamNameNormalizer.matches(actualWinner, predictedChampion)
                result["Campeón"] = listOf(KOItem(predictedChampion, if (correct) 500 else 0, correct))
            }
        }

        result
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().background(SurfaceDark).padding(horizontal = 12.dp).nestedScroll(pullRefreshState.nestedScrollConnection), state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(items.size, key = { "ko_team_${round}_${items[it].team}" }) { idx ->
                    val item = items[idx]
                    val bgColor = if (item.correct) AccentGreen.copy(alpha = 0.1f) else SurfaceMedium.copy(alpha = 0.3f)
                    val teamColor = if (item.correct) AccentGreen else TextPrimary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ExcelParser.getFlagEmoji(item.team),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.team,
                            fontSize = 11.sp,
                            color = teamColor,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${item.points}",
                            fontSize = 11.sp,
                            color = if (item.correct) AccentGreen else TextMuted,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
    PullToRefreshContainer(state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter).zIndex(1f), containerColor = Color.Transparent, contentColor = TextPrimary)
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


