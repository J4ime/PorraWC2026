package com.porrawc2026.app.ui.screens.home

import com.porrawc2026.app.domain.model.TeamNameNormalizer
import com.porrawc2026.app.util.ExcelParser
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    homeScrollTrigger: Int = 0,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val totalPoints by viewModel.totalPoints.collectAsStateWithLifecycle()
    val hasData by viewModel.hasData.collectAsStateWithLifecycle()
    val upcomingMatches by viewModel.upcomingMatches.collectAsStateWithLifecycle()
    val allMatches by viewModel.allMatches.collectAsStateWithLifecycle()
    val dayKeys by viewModel.dayKeys.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

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

    LaunchedEffect(homeScrollTrigger) {
        selectedDay = null
    }

    LaunchedEffect(selectedDay, todayIdx) {
        val rawIdx = if (selectedDay == null) todayIdx
        else allDaysSorted.indexOf(selectedDay).let { if (it < 0) todayIdx else it }
        if (rawIdx < 0) return@LaunchedEffect
        val lazyIdx = rawIdx + 1
        delay(100)
        listState.animateScrollToItem(lazyIdx)
        delay(100)
        val layoutInfo = listState.layoutInfo
        val viewportW = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == lazyIdx } ?: return@LaunchedEffect
        val itemWidth = itemInfo.size
        val centerOffset = -(viewportW - itemWidth) / 2
        listState.animateScrollToItem(lazyIdx, centerOffset)
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
                items(allDaysSorted, key = { it }) { day ->
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
                    val displayMatches = visibleMatches.take(40)
                    val lastMatch = displayMatches.last()
                    item { Spacer(Modifier.height(1.dp)) }
                    items(displayMatches, key = { it.id }) { match ->
                        MatchRow(match)
                        if (match !== lastMatch) Spacer(Modifier.height(6.dp))
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

    val hasWinner = match.isKnockout && match.winnerTeam != null
    val homeIsLoser = hasWinner && !TeamNameNormalizer.matches(match.homeTeam, match.winnerTeam!!)
    val awayIsLoser = hasWinner && !TeamNameNormalizer.matches(match.awayTeam, match.winnerTeam!!)
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
            val hasShootout = isFinished && (match.homeShootoutScore > 0 || match.awayShootoutScore > 0)
            Column(modifier = Modifier.width(if (hasShootout) 42.dp else 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val h = if (showScore || isLive) match.homeGoals?.toString() ?: "0" else "-"
                val a = if (showScore || isLive) match.awayGoals?.toString() ?: "0" else "-"
                val sc = when { isLive -> Color(0xFF4CAF50); showScore -> Color.White; else -> Color(0xFF777777) }
                val hDisplay = if (hasShootout) "$h (${match.homeShootoutScore})" else h
                val aDisplay = if (hasShootout) "$a (${match.awayShootoutScore})" else a
                Text(hDisplay, fontSize = 15.sp, color = sc, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(aDisplay, fontSize = 15.sp, color = sc, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
            }

            Spacer(Modifier.width(8.dp))

            // Teams column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (match.homePossibleTeams.isNotBlank()) {
                        Text("Ganador ", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                        val teams = match.homePossibleTeams.split("-")
                        if (teams.size == 1 && teams[0].startsWith("P")) {
                            Text("partido ${teams[0].removePrefix("P")}", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                        } else {
                            teams.forEachIndexed { index, team ->
                                if (index > 0) Text(" vs ", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                Text("${ExcelParser.getFlagEmoji(team)} $team", fontSize = 11.sp, color = Color.White, maxLines = 1)
                            }
                        }
                    } else {
                        Text(match.homeFlag, fontSize = 13.sp); Spacer(Modifier.width(4.dp))
                        Text(match.homeTeam, fontSize = 12.sp, color = if (homeIsLoser) Color(0xFF666666) else Color.White, textDecoration = if (homeIsLoser) TextDecoration.LineThrough else null, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (match.awayPossibleTeams.isNotBlank()) {
                        Text("Ganador ", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                        val teams = match.awayPossibleTeams.split("-")
                        if (teams.size == 1 && teams[0].startsWith("P")) {
                            Text("partido ${teams[0].removePrefix("P")}", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                        } else {
                            teams.forEachIndexed { index, team ->
                                if (index > 0) Text(" vs ", fontSize = 10.sp, color = Color(0xFFAAAAAA))
                                Text("${ExcelParser.getFlagEmoji(team)} $team", fontSize = 11.sp, color = Color.White, maxLines = 1)
                            }
                        }
                    } else {
                        Text(match.awayFlag, fontSize = 13.sp); Spacer(Modifier.width(4.dp))
                        Text(match.awayTeam, fontSize = 12.sp, color = if (awayIsLoser) Color(0xFF666666) else Color.White, textDecoration = if (awayIsLoser) TextDecoration.LineThrough else null, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }

            // Points + TV + round column
            Column(modifier = Modifier.width(38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val showKoPoints = match.isKnockout && match.hasKnockoutPred
                val showGroupPoints = !match.isKnockout && hasPred && (isLive || showScore || match.pointsEarned > 0)
            if (showGroupPoints || showKoPoints) {
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
                    val isMissedPen = goal.playerName.endsWith("(pen miss)")
                    if (!isMissedPen) {
                        if (isHome) runningH++ else runningA++
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 50.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        val minuteText = goal.minuteLabel ?: "${goal.minute}"
                        if (isMissedPen) {
                            Text("${minuteText}' [--]", fontSize = 9.sp, color = Color(0xFF888888))
                            Spacer(Modifier.width(6.dp))
                            Text("\u274C", fontSize = 10.sp); Spacer(Modifier.width(4.dp))
                        } else {
                            Text("${minuteText}' [$runningH-$runningA]", fontSize = 9.sp, color = Color(0xFF888888))
                            Spacer(Modifier.width(6.dp))
                            Text("\u26BD", fontSize = 10.sp); Spacer(Modifier.width(4.dp))
                        }
                        val displayName = when {
                            goal.playerName.endsWith("(OG)") || goal.playerName.endsWith("(pen)") || goal.playerName.endsWith("(pen miss)") -> goal.playerName
                            else -> goal.playerName.split(" ").last()
                        }
                        Text(displayName, fontSize = 10.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
            // Penalty shootout attempts
            if (match.shootoutAttempts.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().padding(start = 50.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("TANDA DE PENALTIS", fontSize = 8.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("${match.homeShootoutScore}-${match.awayShootoutScore}", fontSize = 9.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth().padding(start = 50.dp)) {
                    // Home team column
                    Column(Modifier.weight(1f)) {
                        match.shootoutAttempts.filter { it.isHome }.forEach { attempt ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = if (attempt.isScored) "\u26BD" else "\u274C"
                                Text(icon, fontSize = 10.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(attempt.playerName.split(" ").last(), fontSize = 10.sp, color = if (attempt.isScored) Color(0xFF4CAF50) else Color(0xFFFF5252), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                    // Away team column
                    Column(Modifier.weight(1f)) {
                        match.shootoutAttempts.filter { !it.isHome }.forEach { attempt ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = if (attempt.isScored) "\u26BD" else "\u274C"
                                Text(icon, fontSize = 10.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(attempt.playerName.split(" ").last(), fontSize = 10.sp, color = if (attempt.isScored) Color(0xFF4CAF50) else Color(0xFFFF5252), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}
