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
        "3er puesto" to 200,
        "Final" to 500
    )

    val liveRoundLists = remember(allMatches) {
        KnockoutCalculator.buildLiveRoundLists(allMatches)
    }

    data class KOItem(val team: String, val points: Int, val userPredicted: Boolean, val matchPlayed: Boolean, val correct: Boolean)

    val roundItems = remember(liveRoundLists, koPredictions, allMatches, koPointsMap) {
        Log.d("KO_DEBUG", "=== Recomputing roundItems ===")
        for ((r, t) in liveRoundLists) {
            Log.d("KO_DEBUG", "liveRoundLists[$r] = $t (size=${t.size})")
        }
        val result = mutableMapOf<String, List<KOItem>>()
        for ((round, teams) in liveRoundLists) {
            result[round] = teams.map { team ->
                Log.d("KO_DEBUG", "Processing: round=$round team=$team")
                val match = allMatches.firstOrNull { m ->
                    m.isKnockout && m.knockoutRound == round &&
                    TeamNameNormalizer.matches(m.homeTeam, team)
                } ?: allMatches.firstOrNull { m ->
                    m.isKnockout && m.knockoutRound == round &&
                    TeamNameNormalizer.matches(m.awayTeam, team)
                }
                Log.d("KO_DEBUG", "  match found: ${match?.id} homeTeam=${match?.homeTeam} awayTeam=${match?.awayTeam}")
                val matchPlayed = match?.let { it.homeGoals != null && it.awayGoals != null } ?: false
                Log.d("KO_DEBUG", "  matchPlayed=$matchPlayed goals=${match?.homeGoals}-${match?.awayGoals}")
                val pred = if (match != null) koPredictions.firstOrNull { it.matchNumber == match.id } else null
                Log.d("KO_DEBUG", "  pred found: ${pred?.matchNumber} winner=${pred?.winner}")
                val userPredicted = if (pred != null) {
                    val home = PointsCalculator.resolvePredictionTeamName(pred.homeTeamRef, koPredictions)
                    val away = PointsCalculator.resolvePredictionTeamName(pred.awayTeamRef, koPredictions)
                    val up = TeamNameNormalizer.matches(home, team) || TeamNameNormalizer.matches(away, team)
                    Log.d("KO_DEBUG", "  userPredicted=$up home=$home away=$away")
                    up
                } else {
                    Log.d("KO_DEBUG", "  userPredicted=false (no pred)")
                    false
                }
                val correct = matchPlayed && userPredicted && pred != null && match != null && (
                    (pred.winner == 1 && TeamNameNormalizer.matches(team, match.homeTeam)) ||
                    (pred.winner == 2 && TeamNameNormalizer.matches(team, match.awayTeam))
                )
                Log.d("KO_DEBUG", "  correct=$correct (matchPlayed=$matchPlayed userPredicted=$userPredicted pred!=null=${pred != null} match!=null=${match != null})")
                val koPts = koPointsMap[match?.id]
                Log.d("KO_DEBUG", "  koPointsMap[${match?.id}] = $koPts")
                val points = if (matchPlayed && correct && match != null) (koPointsMap[match.id] ?: 0) else 0
                Log.d("KO_DEBUG", "  FINAL: points=$points team=$team")
                KOItem(team, points, userPredicted, matchPlayed, correct)
            }
        }
        Log.d("KO_DEBUG", "=== roundItems keys: ${result.keys} ===")
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
                    val bgColor = when {
                        item.matchPlayed && item.correct -> AccentGreen.copy(alpha = 0.1f)
                        item.matchPlayed -> AccentRed.copy(alpha = 0.1f)
                        else -> SurfaceMedium.copy(alpha = 0.3f)
                    }
                    val teamColor = when {
                        item.matchPlayed && item.correct -> AccentGreen
                        item.matchPlayed -> AccentRed
                        else -> TextPrimary
                    }
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
                            text = "0",
                            fontSize = 11.sp,
                            color = TextMuted,
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


