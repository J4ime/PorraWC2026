package com.porrawc2026.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val totalPoints by viewModel.totalPoints.collectAsState()
    val hasData by viewModel.hasData.collectAsState()
    val upcomingMatches by viewModel.upcomingMatches.collectAsState()
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

    val todayDayKey = allMatches.firstOrNull { it.dateLabel == "HOY" }?.dayKey ?: ""
    val yesterdayDayKey = allMatches.firstOrNull { it.dateLabel == "AYER" }?.dayKey ?: ""

    fun sortNum(dayKey: String): Int = allMatches.firstOrNull { it.dayKey == dayKey }?.sortKey?.toIntOrNull() ?: 0

    val allDaysSorted = remember(dayKeys) { dayKeys.sortedBy { sortNum(it) } }
    val todayIdx = allDaysSorted.indexOf(todayDayKey)
    val listState = rememberLazyListState()

    LaunchedEffect(selectedDay) {
        val rawIdx = if (selectedDay == null) todayIdx
        else allDaysSorted.indexOf(selectedDay).let { if (it < 0) todayIdx else it }
        if (rawIdx < 0) return@LaunchedEffect
        val lazyIdx = rawIdx + 1
        listState.animateScrollToItem(lazyIdx)
        delay(350)
        val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == lazyIdx } ?: return@LaunchedEffect
        val viewportW = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
        val centeringOffset = (viewportW / 2 - itemInfo.size / 2).coerceAtLeast(0)
        val currentOffset = itemInfo.offset
        val diff = centeringOffset - currentOffset
        if (diff > 2 || diff < -2) {
            listState.animateScrollToItem(lazyIdx, scrollOffset = centeringOffset)
        }
    }

    val dayLabel = { day: String ->
        val s = sortNum(day); val t = sortNum(todayDayKey)
        when { s == t - 1 -> "AYER"; s == t -> "HOY"; s == t + 1 -> "MAÑANA"; else -> day }
    }

    val visibleMatches = remember(upcomingMatches, allMatches, selectedDay) {
        val base = if (selectedDay == null) upcomingMatches
        else allMatches.filter { it.dayKey == selectedDay }
        base.sortedBy { if (it.status == MatchStatus.LIVE) 0 else 1 }
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
            // Day navigation bar
            LazyRow(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF141414)).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                state = listState
            ) {
                item { Spacer(Modifier.width(8.dp)) }
                items(allDaysSorted) { day ->
                    val isSelected = if (day == todayDayKey) selectedDay == null else selectedDay == day
                    DayChip(dayLabel(day), isSelected) {
                        selectedDay = if (day == todayDayKey) null else day
                    }
                    Spacer(Modifier.width(4.dp))
                }
                item { Spacer(Modifier.width(8.dp)) }
            }

            LazyColumn(modifier = Modifier.weight(1f).nestedScroll(pullRefreshState.nestedScrollConnection).pointerInput(allDaysSorted, selectedDay) {
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
                        val selIdx = if (selectedDay == null) allDaysSorted.indexOf(todayDayKey) else allDaysSorted.indexOf(selectedDay)
                        if (selIdx > 0) {
                            val prev = allDaysSorted[selIdx - 1]
                            selectedDay = if (prev == todayDayKey) null else prev
                        }
                    } else if (totalDrag < -threshold) {
                        val selIdx = if (selectedDay == null) allDaysSorted.indexOf(todayDayKey) else allDaysSorted.indexOf(selectedDay)
                        if (selIdx < allDaysSorted.size - 1) {
                            val next = allDaysSorted[selIdx + 1]
                            selectedDay = if (next == todayDayKey) null else next
                        }
                    }
                }
            }, contentPadding = PaddingValues(bottom = 8.dp)) {
                if (visibleMatches.isNotEmpty()) {
                    items(visibleMatches.take(12), key = { it.id }) { match ->
                        MatchRow(match)
                        if (match != visibleMatches.take(12).last()) Spacer(Modifier.height(6.dp))
                    }
                } else if (hasData && !isBusy) {
                    item { Text("Sin partidos", Modifier.fillMaxWidth().padding(24.dp), color = Color(0xFF777777), textAlign = TextAlign.Center) }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp))

        if (pullRefreshState.isRefreshing) {
            Box(modifier = Modifier.align(Alignment.TopCenter).zIndex(1f).padding(top = 12.dp), contentAlignment = Alignment.Center) {
                SpinningBall(36.sp)
            }
        }

        if (isBusy) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
                SpinningBall(64.sp)
            }
        }
    }
}

@Composable
private fun SpinningBall(fontSize: androidx.compose.ui.unit.TextUnit) {
    val inf = rememberInfiniteTransition("spin")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)))
    val scale by inf.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(800, easing = LinearEasing)))
    Text("\u26BD", fontSize = fontSize, color = Color.White, modifier = Modifier.graphicsLayer { rotationZ = rot; scaleX = scale; scaleY = scale })
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.clickable { onClick() }.background(if (selected) Color(0xFFE65100) else Color(0xFF333333), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        fontSize = 11.sp, color = if (selected) Color.White else Color(0xFFAAAAAA),
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1
    )
}

@Composable
private fun MatchRow(match: MatchDisplay) {
    val hasPred = match.predictedHomeGoals != null && match.predictedAwayGoals != null
    val hasResult = match.homeGoals != null && match.awayGoals != null
    val isLive = match.status == MatchStatus.LIVE
    val isFinished = hasResult && match.status == MatchStatus.FINISHED
    val hasLiveMinute = match.liveMinute != null
    val showScore = hasResult && match.status != MatchStatus.UPCOMING

    val borderColor = if (isLive) Color(0xFF4CAF50) else Color.Transparent
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(10.dp)).then(if (isLive) Modifier.border(2.dp, borderColor, RoundedCornerShape(10.dp)) else Modifier).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Minute + Score column
            Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val timeText = when {
                    isFinished -> "FINAL"
                    hasLiveMinute -> match.liveMinute ?: "?"
                    else -> match.time.ifBlank { "?" }
                }
                Text(timeText, fontSize = 12.sp, color = if (isLive) Color(0xFF4CAF50) else Color(0xFF888888), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            }

            // Goals column  
            Column(modifier = Modifier.width(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val h = if (showScore || isLive) match.homeGoals?.toString() ?: "0" else "-"
                val a = if (showScore || isLive) match.awayGoals?.toString() ?: "0" else "-"
                val sc = when { isLive -> Color(0xFF4CAF50); showScore -> Color.White; else -> Color(0xFF777777) }
                Text(h, fontSize = 15.sp, color = sc, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(a, fontSize = 15.sp, color = sc, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.width(8.dp))

            // Teams column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(match.homeFlag, fontSize = 13.sp); Spacer(Modifier.width(4.dp))
                    Text(match.homeTeam, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(match.awayFlag, fontSize = 13.sp); Spacer(Modifier.width(4.dp))
                    Text(match.awayTeam, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }

            // Points + TV column
            Column(modifier = Modifier.width(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasPred && (isLive || showScore)) {
                    val pts = if (match.pointsEarned > 0) "+${match.pointsEarned}" else "0"
                    val pc = when { match.pointsEarned > 0 -> Color(0xFF4CAF50); else -> Color(0xFF666666) }
                    Text(pts, fontSize = 12.sp, color = pc, fontWeight = FontWeight.Bold)
                }
                if (!isLive && !showScore) {
                    val channels = match.tvChannel.split(",").filter { it.isNotBlank() }
                    channels.forEach { ch ->
                        val bg = if (ch.contains("RTVE", true)) Color(0xFF0037A1) else Color(0xFF333333)
                        Text(ch.trim().take(4), fontSize = 7.sp, color = Color.White, modifier = Modifier.background(bg, RoundedCornerShape(3.dp)).padding(horizontal = 2.dp, vertical = 1.dp))
                    }
                }
            }
        }

        // Goal events during live/finished matches
        if (isLive || isFinished) {
            val allScorers = match.homeScorers.map { Triple(it, true, match.homeTeam) } + match.awayScorers.map { Triple(it, false, match.awayTeam) }
            val sortedScorers = allScorers.sortedBy { it.first.minute }
            if (sortedScorers.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                val currentH = match.homeGoals ?: 0
                val currentA = match.awayGoals ?: 0
                var runningH = 0
                var runningA = 0
                sortedScorers.forEach { (goal, isHome, team) ->
                    if (isHome) runningH++ else runningA++
                    Row(Modifier.fillMaxWidth().padding(start = 50.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("${goal.minute}' [$runningH-$runningA]", fontSize = 9.sp, color = Color(0xFF888888))
                        Spacer(Modifier.width(6.dp))
                        Text("\u26BD", fontSize = 10.sp); Spacer(Modifier.width(4.dp))
                        Text(goal.playerName.split(" ").last(), fontSize = 10.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}
