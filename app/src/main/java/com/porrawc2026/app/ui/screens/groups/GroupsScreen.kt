package com.porrawc2026.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.ui.theme.*

@Composable
fun MatchesScreen(viewModel: GroupsViewModel = hiltViewModel()) {
    val allMatches by viewModel.allMatches.collectAsState()
    val koPredictions by viewModel.allKnockoutPredictions.collectAsState()
    val sorted = remember(allMatches) { allMatches.sortedBy { it.dateTime } }

    LazyColumn(Modifier.fillMaxSize().background(SurfaceDark).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).background(GroupHeaderBg, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Fecha", Modifier.width(64.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("Partido", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                Text("Pts", Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
            }
        }

        var lastLabel = ""
        for (match in sorted) {
            if (match.isKnockout) {
                val label = match.knockoutRound ?: "Eliminatorias"
                if (label != lastLabel) {
                    lastLabel = label
                    item { Text(label.uppercase(), style = MaterialTheme.typography.titleSmall, color = WCGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
                }
                item { KnockoutMatchRow(match, koPredictions) }
            } else {
                item { GroupMatchRow(match) }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun fmtDate(match: MatchEntity): String {
    val dt = match.dateTime.ifBlank { return "" }
    return try {
        val date = dt.take(10).substringAfter("-2026-", "").replace("-", "-")
        val time = if (dt.length >= 16) dt.substring(11, 16) else ""
        if (date.isBlank()) time else "$date $time"
    } catch (_: Exception) { dt }
}

@Composable
private fun GroupMatchRow(match: MatchEntity) {
    val hasResult = match.homeGoals != null && match.awayGoals != null
    val hasPred = match.predictedHomeGoals != null && match.predictedAwayGoals != null
    val pts = if (hasResult) match.pointsEarned else 0

    val bgColor = when {
        hasResult && pts >= 30 -> AccentGreen.copy(alpha = 0.1f)
        hasResult && pts > 0 -> AccentBlue.copy(alpha = 0.1f)
        hasResult -> AccentRed.copy(alpha = 0.1f)
        else -> SurfaceMedium.copy(alpha = 0.3f)
    }

    Row(Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(fmtDate(match), Modifier.width(64.dp), style = MaterialTheme.typography.labelSmall, color = WCGold, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)

        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(match.homeTeam, fontSize = 11.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text(if (hasPred) " ${match.predictedHomeGoals}-${match.predictedAwayGoals} " else " - ", fontSize = 11.sp, color = if (hasPred) TextPrimary else TextMuted, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(match.awayTeam, fontSize = 11.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }

        Text(if (pts > 0) "$pts" else "", Modifier.width(24.dp), fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun KnockoutMatchRow(match: MatchEntity, koPredictions: List<KnockoutPredictionEntity>) {
    val hasResult = match.homeGoals != null && match.awayGoals != null
    val koPred = koPredictions.firstOrNull { it.homeTeamRef == match.homeTeam && it.awayTeamRef == match.awayTeam }
    val homeWins = koPred?.winner == 1
    val awayWins = koPred?.winner == 2
    val correct = hasResult && match.pointsEarned > 0

    val bgColor = when {
        correct -> AccentGreen.copy(alpha = 0.1f)
        hasResult -> AccentRed.copy(alpha = 0.1f)
        else -> SurfaceMedium.copy(alpha = 0.3f)
    }

    val hColor = when {
        correct && homeWins -> AccentGreen
        hasResult && homeWins -> AccentRed
        else -> TextPrimary
    }
    val aColor = when {
        correct && awayWins -> AccentGreen
        hasResult && awayWins -> AccentRed
        else -> TextPrimary
    }

    Row(Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(fmtDate(match), Modifier.width(64.dp), style = MaterialTheme.typography.labelSmall, color = WCGold, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)

        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(match.homeTeam, fontSize = 11.sp, color = hColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = if (homeWins) FontWeight.Bold else FontWeight.Normal)
            Text(" vs ", fontSize = 11.sp, color = TextMuted, maxLines = 1)
            Text(match.awayTeam, fontSize = 11.sp, color = aColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontWeight = if (awayWins) FontWeight.Bold else FontWeight.Normal)
        }

        Text(if (correct) "+${match.pointsEarned}" else "", Modifier.width(24.dp), fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
