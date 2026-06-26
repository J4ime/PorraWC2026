package com.porrawc2026.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.ui.theme.*

@Composable
fun MatchesScreen(scrollTrigger: Int = 0, viewModel: GroupsViewModel = hiltViewModel()) {
    val allMatches by viewModel.allMatches.collectAsState()
    val allTeams by viewModel.allTeams.collectAsState()
    val koPredictions by viewModel.allKnockoutPredictions.collectAsState()
    val sorted = remember(allMatches) { allMatches.sortedBy { it.dateTime } }
    val listState = rememberLazyListState()

    val actualAdvancing = remember(allMatches, allTeams) {
        if (allMatches.isEmpty() || allTeams.isEmpty()) PointsCalculator.AdvancingTeams()
        else PointsCalculator.computeActualAdvancingTeams(allMatches, allTeams)
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

    LazyColumn(Modifier.fillMaxSize().background(SurfaceDark).padding(horizontal = 12.dp), state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
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

        var lastRound = ""
        for (match in sorted) {
            if (match.isKnockout) {
                val round = match.knockoutRound ?: "Eliminatorias"
                if (round != lastRound) {
                    lastRound = round
                    item { Text(round.uppercase(), style = MaterialTheme.typography.titleSmall, color = WCGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
                }
                item { KnockoutMatchRow(match, koPredictions, allMatches, actualAdvancing) }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
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

        Text(if (pts > 0) "$pts" else "", Modifier.width(24.dp), fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun KnockoutMatchRow(match: MatchEntity, koPredictions: List<KnockoutPredictionEntity>, allMatches: List<MatchEntity>, actualAdvancing: PointsCalculator.AdvancingTeams) {
    val koPred = koPredictions.firstOrNull { it.matchNumber == match.id }
    val homeWins = koPred?.winner == 1
    val awayWins = koPred?.winner == 2
    val hasResult = match.homeGoals != null && match.awayGoals != null
    val actualWinner = if (hasResult && match.winnerTeam != null) {
        when {
            TeamNameNormalizer.matches(match.homeTeam, match.winnerTeam) -> 1
            TeamNameNormalizer.matches(match.awayTeam, match.winnerTeam) -> 2
            else -> null
        }
    } else null
    val correct = actualWinner != null && ((actualWinner == 1 && homeWins) || (actualWinner == 2 && awayWins))

    val bgColor = when {
        correct -> AccentGreen.copy(alpha = 0.1f)
        actualWinner != null -> AccentRed.copy(alpha = 0.1f)
        else -> SurfaceMedium.copy(alpha = 0.3f)
    }

    val hColor = when {
        correct && homeWins -> AccentGreen
        actualWinner != null && homeWins -> AccentRed
        else -> TextPrimary
    }
    val aColor = when {
        correct && awayWins -> AccentGreen
        actualWinner != null && awayWins -> AccentRed
        else -> TextPrimary
    }

    val advPts = remember(match, allMatches, actualAdvancing) {
        PointsCalculator.computeKnockoutMatchAdvancementPoints(match, allMatches, actualAdvancing)
    }

    Row(Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(fmtDate(match), Modifier.width(72.dp), fontSize = 9.sp, color = WCGold, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)

        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            val hText = if (koPred != null) koPred.homeTeamRef else match.homeTeam
            val aText = if (koPred != null) koPred.awayTeamRef else match.awayTeam
            Text(hText, fontSize = 11.sp, color = hColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = if (homeWins) FontWeight.Bold else FontWeight.Normal)
            Text(" vs ", fontSize = 11.sp, color = TextMuted, maxLines = 1)
            Text(aText, fontSize = 11.sp, color = aColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontWeight = if (awayWins) FontWeight.Bold else FontWeight.Normal)
        }

        Text(if (advPts > 0) "+$advPts" else "", Modifier.width(24.dp), fontSize = 11.sp, color = if (advPts > 0) AccentGreen else TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}


