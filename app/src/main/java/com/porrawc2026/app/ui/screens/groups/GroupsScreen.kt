package com.porrawc2026.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    onBackClick: () -> Unit,
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val allMatches by viewModel.allMatches.collectAsState()
    val sortedMatches = allMatches.sortedBy { it.dateTime }

    Column(Modifier.fillMaxSize().background(SurfaceDark)) {
        TopAppBar(
            title = { Text("PREDICCIONES", style = MaterialTheme.typography.titleLarge, color = WCGold, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WCDarkBlue)
        )

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).background(GroupHeaderBg, RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Hora", Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Partido", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Pred.", Modifier.width(46.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                    Text("Pts", Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                }
            }

            var currentDate = ""
            for (match in sortedMatches) {
                val dateLabel = match.dateTime.take(10)
                if (currentDate != dateLabel) {
                    currentDate = dateLabel
                    val displayDate = formatDisplayDate(match.dateTime)
                    item { Text(displayDate, style = MaterialTheme.typography.labelMedium, color = WCGold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) }
                }
                item { MatchRow(match) }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun formatDisplayDate(dateTime: String): String {
    if (dateTime.isBlank()) return ""
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val fmt = java.text.SimpleDateFormat("EEE d MMM", java.util.Locale("es", "ES"))
        val d = sdf.parse(dateTime.take(10))
        if (d != null) fmt.format(d).replace(".", "").uppercase() else dateTime.take(10)
    } catch (e: Exception) { dateTime.take(10) }
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

    Row(Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val time = if (match.dateTime.length >= 16) match.dateTime.substring(11, 16) else match.dateTime
        Text(time, Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, color = WCGold, fontWeight = FontWeight.Bold)

        val label = if (match.isKnockout) match.knockoutRound ?: match.groupName else match.groupName
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
