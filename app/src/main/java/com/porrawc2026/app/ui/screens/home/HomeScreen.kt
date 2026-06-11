package com.porrawc2026.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val totalPoints by viewModel.totalPoints.collectAsState()
    val hasData by viewModel.hasData.collectAsState()
    val upcomingMatches by viewModel.upcomingMatches.collectAsState()
    val yesterdayMatches by viewModel.yesterdayMatches.collectAsState()
    val allMatches by viewModel.allMatches.collectAsState()
    val dayKeys by viewModel.dayKeys.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()
    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            viewModel.forceCheckUpdate()
            pullRefreshState.endRefresh()
        }
    }

    var selectedDay by remember { mutableStateOf<String?>(null) }

    val visibleMatches = if (selectedDay == null) {
        upcomingMatches
    } else {
        allMatches.filter { it.dayKey == selectedDay }
    }

    var showYesterday by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E0E))) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (dayKeys.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF141414)).padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    item {
                        val isToday = selectedDay == null
                        DayChip("HOY", isToday) { selectedDay = null }
                    }
                    itemsIndexed(dayKeys) { _, day ->
                        val sel = selectedDay == day
                        DayChip(day, sel) { selectedDay = day }
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).nestedScroll(pullRefreshState.nestedScrollConnection), contentPadding = PaddingValues(bottom = 8.dp)) {
                if (selectedDay == null && yesterdayMatches.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Row(
                                Modifier.fillMaxWidth().clickable { showYesterday = !showYesterday }.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AYER", style = MaterialTheme.typography.titleSmall, color = Color(0xFF777777), fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Icon(if (showYesterday) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Color(0xFF777777), modifier = Modifier.size(18.dp))
                            }
                            AnimatedVisibility(visible = showYesterday) {
                                Column {
                                    Spacer(Modifier.height(4.dp))
                                    yesterdayMatches.forEach { match ->
                                        MatchRow(match)
                                        if (match != yesterdayMatches.last()) Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                if (visibleMatches.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            visibleMatches.take(12).forEach { match ->
                                MatchRow(match)
                                if (match != visibleMatches.take(12).last()) Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                } else if (hasData) {
                    item { Text("Sin partidos", Modifier.fillMaxWidth().padding(24.dp), color = Color(0xFF777777), textAlign = TextAlign.Center) }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp))

        if (pullRefreshState.isRefreshing) {
            Box(modifier = Modifier.align(Alignment.TopCenter).zIndex(1f).padding(top = 12.dp), contentAlignment = Alignment.Center) {
                val inf = rememberInfiniteTransition("ptr")
                val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)))
                val scale by inf.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(800, easing = LinearEasing)))
                Text("\u26BD", fontSize = 36.sp, color = Color.White, modifier = Modifier.graphicsLayer { rotationZ = rot; scaleX = scale; scaleY = scale })
            }
        }

        if (isBusy) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
                val inf = rememberInfiniteTransition("busy")
                val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)))
                val scale by inf.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(800, easing = LinearEasing)))
                Text("\u26BD", fontSize = 64.sp, modifier = Modifier.graphicsLayer { rotationZ = rot; scaleX = scale; scaleY = scale })
            }
        }
    }
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .clickable { onClick() }
            .background(
                if (selected) Color(0xFFE65100) else Color(0xFF333333),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        fontSize = 11.sp,
        color = if (selected) Color.White else Color(0xFFAAAAAA),
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1
    )
}

@Composable
private fun MatchRow(match: MatchDisplay) {
    val hasPred = match.predictedHomeGoals != null && match.predictedAwayGoals != null
    val hasResult = match.homeGoals != null && match.awayGoals != null
    val isLive = match.status == MatchStatus.LIVE
    val hasLiveMinute = match.liveMinute != null
    val scoreBg = if (isLive) Color(0xFF2E7D32) else Color.Transparent

    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        val timeText = if (hasLiveMinute) match.liveMinute ?: "?" else match.time.ifBlank { "?" }
        val timeColor = if (isLive) Color(0xFF4CAF50) else Color.White
        Text(timeText, Modifier.width(42.dp).padding(top = 2.dp), style = MaterialTheme.typography.labelMedium, color = timeColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)

        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(match.homeTeam, style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (match.homeScorers.isNotEmpty()) {
                match.homeScorers.forEachIndexed { idx, scorer ->
                    Text("\u26BD ${scorer.playerName.split(" ").last()} ${scorer.minute}'", style = MaterialTheme.typography.labelSmall, color = if (isLive) Color(0xFF4CAF50) else Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (idx < match.homeScorers.lastIndex) Spacer(Modifier.height(2.dp))
                }
            }
        }

        Box(modifier = Modifier.padding(horizontal = 4.dp).background(scoreBg, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
            if (hasResult || isLive) {
                val h = if (hasResult) "${match.homeGoals}" else (match.homeGoals?.toString() ?: "0")
                val a = if (hasResult) "${match.awayGoals}" else (match.awayGoals?.toString() ?: "0")
                Text("$h - $a", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            } else {
                Text("-", style = MaterialTheme.typography.bodySmall, color = Color(0xFF777777), maxLines = 1)
            }
        }

        Column(Modifier.weight(1f)) {
            Text(match.awayTeam, style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (match.awayScorers.isNotEmpty()) {
                match.awayScorers.forEachIndexed { idx, scorer ->
                    Text("\u26BD ${scorer.playerName.split(" ").last()} ${scorer.minute}'", style = MaterialTheme.typography.labelSmall, color = if (isLive) Color(0xFF4CAF50) else Color(0xFF888888), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (idx < match.awayScorers.lastIndex) Spacer(Modifier.height(2.dp))
                }
            }
        }

        Spacer(Modifier.width(4.dp))
        if (!isLive && !hasResult) {
            val channels = match.tvChannel.split(",").filter { it.isNotBlank() }
            channels.forEach { ch ->
                val bg = if (ch.contains("RTVE", ignoreCase = true)) Color(0xFF0037A1) else Color(0xFF333333)
                Text(ch.trim().take(4), fontSize = 8.sp, color = Color.White, modifier = Modifier.background(bg, RoundedCornerShape(3.dp)).padding(horizontal = 3.dp, vertical = 1.dp))
            }
        }

        if (hasPred && (isLive || hasResult)) {
            val ptsValue = if (match.pointsEarned > 0) "${match.pointsEarned}" else "0"
            val ptsBg = if (isLive) Color(0xFF2E7D32) else Color.Transparent
            val ptsColor = when {
                isLive -> Color.White
                hasResult && match.pointsEarned > 0 -> Color(0xFF4CAF50)
                else -> Color(0xFF666666)
            }
            Text(ptsValue, modifier = Modifier.width(24.dp).background(ptsBg, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = ptsColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
