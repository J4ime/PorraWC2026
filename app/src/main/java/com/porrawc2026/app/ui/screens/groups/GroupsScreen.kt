package com.porrawc2026.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
fun GroupsScreen(
    onGroupClick: (String) -> Unit,
    onBackClick: () -> Unit,
    viewModel: GroupsViewModel = hiltViewModel()
) {
    val allMatches by viewModel.allMatches.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val groupMatches = allMatches.filter { !it.isKnockout }
        .sortedBy { it.dateTime }

    val groups = allMatches.filter { !it.isKnockout }
        .groupBy { it.groupName }.keys.sorted()

    Column(
        modifier = Modifier.fillMaxSize().background(SurfaceDark)
    ) {
        TopAppBar(
            title = { Text("FASE DE GRUPOS", style = MaterialTheme.typography.titleLarge, color = WCGold, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, null, tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WCDarkBlue)
        )

        TabRow(selectedTabIndex = selectedTab, containerColor = WCDarkBlue, contentColor = WCGold) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("PARTIDOS", style = MaterialTheme.typography.labelMedium) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("GRUPOS", style = MaterialTheme.typography.labelMedium) })
        }

        when (selectedTab) {
            0 -> ChronologicalMatchList(groupMatches)
            1 -> GroupGrid(groups, viewModel, onGroupClick)
        }
    }
}

@Composable
private fun ChronologicalMatchList(matches: List<MatchEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp).background(GroupHeaderBg, RoundedCornerShape(8.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hora", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("Partido · Predicción", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("TV", Modifier.width(44.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                Text("Pts", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
            }
        }

        var currentDate = ""
        for (match in matches) {
            val dateLabel = match.dateTime.take(10)
            if (currentDate != dateLabel) {
                currentDate = dateLabel
                val displayDate = formatDisplayDate(match.dateTime)
                item {
                    Text(displayDate, style = MaterialTheme.typography.labelMedium, color = WCGold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
                }
            }
            item { ChronologicalMatchRow(match) }
        }
        item { Spacer(Modifier.height(16.dp)) }
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
private fun ChronologicalMatchRow(match: MatchEntity) {
    val hasResult = match.homeGoals != null && match.awayGoals != null
    val hasPred = match.predictedHomeGoals != null && match.predictedAwayGoals != null
    val bgColor = when {
        hasResult && match.pointsEarned >= 30 -> AccentGreen.copy(alpha = 0.1f)
        hasResult && match.pointsEarned > 0 -> AccentBlue.copy(alpha = 0.1f)
        hasResult -> AccentRed.copy(alpha = 0.1f)
        else -> SurfaceMedium.copy(alpha = 0.3f)
    }

    Row(
        Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val time = if (match.dateTime.length >= 16) match.dateTime.substring(11, 16) else match.dateTime
        Text(time, Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = WCGold, fontWeight = FontWeight.Bold)

        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(match.homeTeam, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 1, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text(" ${if (hasPred) match.predictedHomeGoals?.toString() ?: "-" else "-"} - ${if (hasPred) match.predictedAwayGoals?.toString() ?: "-" else "-"} ", style = MaterialTheme.typography.bodySmall, color = if (hasPred) TextPrimary else TextMuted, maxLines = 1)
            Text(match.awayTeam, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 1, modifier = Modifier.weight(1f))
        }

        val channels = match.tvChannel.split(",").filter { it.isNotBlank() }
        Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            channels.forEach { ch ->
                val bg = if (ch.contains("RTVE", ignoreCase = true)) AccentBlue else Color(0xFF0A0A0A)
                Text(ch.trim().take(4), fontSize = 8.sp, color = TextPrimary,
                    modifier = Modifier.background(bg, RoundedCornerShape(3.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
            }
        }

        Text(
            if (match.pointsEarned > 0 || hasResult) "+${match.pointsEarned}" else "",
            Modifier.width(40.dp),
            style = MaterialTheme.typography.bodySmall, color = if (match.pointsEarned > 0) AccentGreen else TextMuted,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GroupGrid(groups: List<String>, viewModel: GroupsViewModel, onGroupClick: (String) -> Unit) {
    val teams by viewModel.allTeams.collectAsState()
    val groupTeams = teams.groupBy { it.groupLetter }
    LazyColumn(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(groups) { group ->
            GroupCard(group, groupTeams[group] ?: emptyList()) { onGroupClick(group) }
        }
    }
}

@Composable
private fun GroupCard(groupLetter: String, teams: List<com.porrawc2026.app.data.local.entity.TeamEntity>, onClick: () -> Unit) {
    Card(onClick = onClick, Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("GRUPO $groupLetter", style = MaterialTheme.typography.titleMedium, color = WCGold, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            teams.forEach { team ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(team.flagEmoji, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(team.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
