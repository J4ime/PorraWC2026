package com.porrawc2026.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
    var selectedDay by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            viewModel.forceCheckUpdate()
            viewModel.refreshLiveScores()
            selectedDay = null
            pullRefreshState.endRefresh()
        }
    }
    val dayNavState = rememberLazyListState()

    val todayDayKey = if (allMatches.isNotEmpty()) allMatches.firstOrNull { it.dateLabel == "HOY" }?.dayKey else null
    val navigableDays = remember(dayKeys, todayDayKey) {
        val filtered = dayKeys.filter { it != todayDayKey }
        listOf("HOY") + filtered
    }

    val visibleMatches = if (selectedDay == null) {
        upcomingMatches
    } else {
        allMatches.filter { it.dayKey == selectedDay }
    }

    LaunchedEffect(selectedDay, navigableDays) {
        val idx = if (selectedDay == null) 0 else navigableDays.indexOf(selectedDay)
        if (idx >= 0) {
            dayNavState.animateScrollToItem(idx)
        }
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
            if (navigableDays.isNotEmpty()) {
                LazyRow(
                    state = dayNavState,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF141414)).padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    itemsIndexed(navigableDays) { _, day ->
                        val isSelected = if (day == "HOY") selectedDay == null else selectedDay == day
                        DayChip(day, isSelected) {
                            selectedDay = if (day == "HOY") null else day
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).nestedScroll(pullRefreshState.nestedScrollConnection).pointerInput(navigableDays, selectedDay) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val isTopHalf = down.position.y < size.height * 0.55f
                    if (!isTopHalf) return@awaitEachGesture
                    var totalDrag = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        totalDrag += change.position.x - change.previousPosition.x
                        if (Math.abs(totalDrag) > 10f) change.consume()
                    }
                    val threshold = 200f
                    if (totalDrag > threshold) {
                        when {
                            selectedDay == null && navigableDays.size > 1 -> selectedDay = navigableDays.last()
                            selectedDay != null -> {
                                val idx = navigableDays.indexOf(selectedDay) - 1
                                selectedDay = if (idx >= 0) navigableDays[idx] else null
                            }
                        }
                    } else if (totalDrag < -threshold) {
                        when {
                            selectedDay == null && navigableDays.size > 1 -> selectedDay = navigableDays[1]
                            selectedDay != null -> {
                                val idx = navigableDays.indexOf(selectedDay) + 1
                                selectedDay = if (idx < navigableDays.size) navigableDays[idx] else null
                            }
                        }
                    }
                }
            }, contentPadding = PaddingValues(bottom = 8.dp)) {
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
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                            visibleMatches.take(12).forEach { match ->
                                MatchRow(match)
                                if (match != visibleMatches.take(12).last()) Spacer(Modifier.height(6.dp))
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
            .background(if (selected) Color(0xFFE65100) else Color(0xFF333333), RoundedCornerShape(8.dp))
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
    val isFinished = hasResult && !isLive
    val hasLiveMinute = match.liveMinute != null

    val timeColor = when {
        isLive -> Color(0xFF4CAF50)
        isFinished -> Color(0xFF888888)
        else -> Color.White
    }

    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp)).padding(vertical = 3.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        // Time column - 20%
        Column(
            modifier = Modifier.weight(0.20f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val liveText = hasLiveMinute?.let { match.liveMinute }
            val statusText = when {
                isFinished -> "FINAL"
                liveText != null -> liveText
                else -> match.time.ifBlank { "?" }
            }
            Text(statusText, fontSize = 14.sp, color = timeColor, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }

        // Teams + Score column - 50%
        Row(modifier = Modifier.weight(0.50f), verticalAlignment = Alignment.CenterVertically) {
            // Score column
            Column(
                modifier = Modifier.width(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val homeGoals = if (hasResult || isLive) match.homeGoals?.toString() ?: "0" else "-"
                val awayGoals = if (hasResult || isLive) match.awayGoals?.toString() ?: "0" else "-"
                val scoreColor = when {
                    isLive -> Color(0xFF4CAF50)
                    hasResult -> Color.White
                    else -> Color(0xFF777777)
                }
                Text(homeGoals, fontSize = 15.sp, color = scoreColor, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(awayGoals, fontSize = 15.sp, color = scoreColor, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.width(6.dp))

            // Teams
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(match.homeFlag, fontSize = 13.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(match.homeTeam, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (isLive) {
                        Spacer(Modifier.width(4.dp))
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF4CAF50)))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(match.awayFlag, fontSize = 13.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(match.awayTeam, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                if (hasPred && (isLive || hasResult)) {
                    Spacer(Modifier.height(2.dp))
                    val pts = if (match.pointsEarned > 0) "+${match.pointsEarned}" else "0"
                    val ptsColor = when {
                        isLive -> Color(0xFF4CAF50)
                        match.pointsEarned > 0 -> Color(0xFF4CAF50)
                        else -> Color(0xFF666666)
                    }
                    Text(pts, fontSize = 10.sp, color = ptsColor, fontWeight = FontWeight.Bold)
                }
            }
        }

        // TV column - 30%
        Column(
            modifier = Modifier.weight(0.30f).padding(start = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isLive && !hasResult) {
                val channels = match.tvChannel.split(",").filter { it.isNotBlank() }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    channels.forEach { ch ->
                        val bg = if (ch.contains("RTVE", ignoreCase = true)) Color(0xFF0037A1) else Color(0xFF333333)
                        Text(ch.trim().take(4), fontSize = 8.sp, color = Color.White, modifier = Modifier.background(bg, RoundedCornerShape(3.dp)).padding(horizontal = 2.dp, vertical = 1.dp), maxLines = 1)
                    }
                }
            }
        }
    }
}
