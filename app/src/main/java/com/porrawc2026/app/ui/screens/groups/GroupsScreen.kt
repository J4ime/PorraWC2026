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

private val koOrder = mapOf(
    "Dieciseisavos" to 1, "Octavos" to 2, "Cuartos" to 3,
    "Semifinales" to 4, "3er puesto" to 5, "Final" to 6
)

@Composable
fun MatchesScreen(
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val allMatches by viewModel.allMatches.collectAsState()

    val groupedMatches = remember(allMatches) {
        val sorted = allMatches.sortedBy { it.dateTime }
        val groups = linkedMapOf<String, MutableList<MatchEntity>>()
        for (m in sorted) {
            val key = if (m.isKnockout) m.knockoutRound ?: "Eliminatorias" else m.groupName
            groups.getOrPut(key) { mutableListOf() }.add(m)
        }
        groups.toList().sortedBy { (key, _) ->
            if (key.startsWith("Grupo")) {
                key.substringAfter("Grupo ").trim().padStart(2, '0')
            } else {
                (koOrder[key] ?: 99).toString().padStart(2, '0')
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize().background(SurfaceDark).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for ((groupName, matches) in groupedMatches) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(groupName, style = MaterialTheme.typography.titleSmall, color = WCGold, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
            }

            if (groupName == groupedMatches.first().first) {
                item {
                    Row(Modifier.fillMaxWidth().background(GroupHeaderBg, RoundedCornerShape(8.dp)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Hora", Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("Partido", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("Pred.", Modifier.width(46.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                        Text("Pts", Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                    }
                }
            }

            for (match in matches) {
                item { MatchRow(match) }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
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
