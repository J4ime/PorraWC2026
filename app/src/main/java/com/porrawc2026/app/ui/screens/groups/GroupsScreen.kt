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
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.ui.theme.*

@Composable
fun MatchesScreen(viewModel: GroupsViewModel = hiltViewModel()) {
    val allMatches by viewModel.allMatches.collectAsState()
    val sorted = remember(allMatches) { allMatches.sortedBy { it.dateTime } }

    LazyColumn(Modifier.fillMaxSize().background(SurfaceDark).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).background(GroupHeaderBg, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Fecha", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("Partido", Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                Text("Pts", Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
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
                item { KnockoutMatchRow(match) }
            } else {
                item { GroupMatchRow(match) }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun GroupMatchRow(match: MatchEntity) {
    val hasResult = match.homeGoals != null && match.awayGoals != null
    val hasPred = match.predictedHomeGoals != null && match.predictedAwayGoals != null

    val bgColor = when {
        hasResult && match.pointsEarned >= 30 -> AccentGreen.copy(alpha = 0.1f)
        hasResult && match.pointsEarned > 0 -> AccentBlue.copy(alpha = 0.1f)
        hasResult -> AccentRed.copy(alpha = 0.1f)
        else -> SurfaceMedium.copy(alpha = 0.3f)
    }

    Row(Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            val time = if (match.dateTime.length >= 16) match.dateTime.substring(11, 16) else ""
            Text(time, style = MaterialTheme.typography.labelSmall, color = WCGold, fontWeight = FontWeight.Bold)
            if (match.dateTime.length >= 10) {
                Text(match.dateTime.substring(5, 10), fontSize = 9.sp, color = TextMuted)
            }
        }

        Column(Modifier.weight(2f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(match.homeTeam, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text(if (hasPred) " ${match.predictedHomeGoals}-${match.predictedAwayGoals} " else " - ", style = MaterialTheme.typography.bodySmall, color = if (hasPred) TextPrimary else TextMuted, fontWeight = FontWeight.Bold)
                Text(match.awayTeam, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }

        Text(
            if (hasResult && match.pointsEarned > 0) "${match.pointsEarned}" else "",
            Modifier.width(36.dp), style = MaterialTheme.typography.bodySmall, color = AccentGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun KnockoutMatchRow(match: MatchEntity) {
    val hasPred = match.predictedHomeGoals != null && match.predictedAwayGoals != null
    val hasResult = match.homeGoals != null && match.awayGoals != null
    val predHome = match.predictedHomeGoals ?: 1
    val predAway = match.predictedAwayGoals ?: 0
    val homeWins = hasPred && predHome > predAway
    val awayWins = hasPred && predAway > predHome
    val correct = hasResult && match.pointsEarned > 0

    val bgColor = when {
        correct -> AccentGreen.copy(alpha = 0.1f)
        hasResult -> AccentRed.copy(alpha = 0.1f)
        else -> SurfaceMedium.copy(alpha = 0.3f)
    }

    Row(Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            val time = if (match.dateTime.length >= 16) match.dateTime.substring(11, 16) else ""
            Text(time, style = MaterialTheme.typography.labelSmall, color = WCGold, fontWeight = FontWeight.Bold)
            if (match.dateTime.length >= 10) {
                Text(match.dateTime.substring(5, 10), fontSize = 9.sp, color = TextMuted)
            }
        }

        Column(Modifier.weight(2f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Text(match.homeTeam, style = MaterialTheme.typography.bodySmall, color = hColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = if (homeWins) FontWeight.Bold else FontWeight.Normal)
                Text(" vs ", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text(match.awayTeam, style = MaterialTheme.typography.bodySmall, color = aColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), fontWeight = if (awayWins) FontWeight.Bold else FontWeight.Normal)
            }
        }

        Text(
            if (hasResult && match.pointsEarned > 0) "+${match.pointsEarned}" else "",
            Modifier.width(36.dp), style = MaterialTheme.typography.bodySmall, color = AccentGreen, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
    }
}
