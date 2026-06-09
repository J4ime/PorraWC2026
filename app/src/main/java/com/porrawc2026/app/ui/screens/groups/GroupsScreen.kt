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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.ui.theme.*

@Composable
fun MatchesScreen(
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val allMatches by viewModel.allMatches.collectAsState()
    val sorted = remember(allMatches) { allMatches.sortedBy { it.dateTime } }

    LazyColumn(Modifier.fillMaxSize().background(SurfaceDark).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).background(GroupHeaderBg, RoundedCornerShape(8.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Hora", Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("Partido", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("Pred.", Modifier.width(46.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                Text("Pts", Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
            }
        }

        var lastDate = ""
        for (match in sorted) {
            val dl = showDateLabel(match)
            if (dl != lastDate) {
                lastDate = dl
                item { Text(dl, style = MaterialTheme.typography.labelMedium, color = WCGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) }
            }
            item { MatchRow(match) }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun showDateLabel(m: MatchEntity): String {
    if (m.isKnockout) return m.knockoutRound ?: "Eliminatorias"
    if (m.dateTime.isBlank() || m.dateTime.length < 10) return ""
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val fmt = java.text.SimpleDateFormat("EEE d MMM", java.util.Locale("es", "ES"))
    return try {
        val d = sdf.parse(m.dateTime.take(10))
        if (d != null) fmt.format(d).replace(".", "").uppercase() else m.dateTime.take(10)
    } catch (_: Exception) { m.dateTime.take(10) }
}

@Composable
private fun MatchRow(match: MatchEntity) {
    val hasResult = match.homeGoals != null && match.awayGoals != null
    val hasPred = match.predictedHomeGoals != null && match.predictedAwayGoals != null
    val isLive = !hasResult && hasPred

    val bgColor = when {
        hasResult && match.pointsEarned >= 30 -> AccentGreen.copy(alpha = 0.1f)
        hasResult && match.pointsEarned > 0 -> AccentBlue.copy(alpha = 0.1f)
        hasResult -> AccentRed.copy(alpha = 0.1f)
        isLive -> AccentOrange.copy(alpha = 0.1f)
        else -> SurfaceMedium.copy(alpha = 0.3f)
    }

    Row(Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val time = if (match.dateTime.length >= 16) match.dateTime.substring(11, 16) else match.dateTime
        Text(time, Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, color = WCGold, fontWeight = FontWeight.Bold)

        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(match.homeTeam, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 1, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text(" ${if (hasPred) "${match.predictedHomeGoals}-${match.predictedAwayGoals}" else "-"} ", style = MaterialTheme.typography.bodySmall, color = if (hasPred) TextPrimary else TextMuted, maxLines = 1)
            Text(match.awayTeam, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
        }

        Text(
            if (hasResult) "${match.homeGoals}-${match.awayGoals}" else if (hasPred) "${match.predictedHomeGoals}-${match.predictedAwayGoals}" else "-",
            Modifier.width(46.dp),
            style = MaterialTheme.typography.bodySmall, color = if (hasResult) AccentOrange else if (hasPred) TextPrimary else TextMuted, textAlign = TextAlign.Center, fontWeight = if (hasResult) FontWeight.Bold else FontWeight.Normal
        )

        Text(
            if (hasResult) "${match.pointsEarned}" else "",
            Modifier.width(42.dp),
            style = MaterialTheme.typography.bodySmall, color = if (hasResult && match.pointsEarned > 0) AccentGreen else TextMuted,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
    }
}
