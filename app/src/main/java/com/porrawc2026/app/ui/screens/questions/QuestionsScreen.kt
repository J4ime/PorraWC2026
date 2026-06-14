package com.porrawc2026.app.ui.screens.questions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.QuestionEntity
import com.porrawc2026.app.ui.theme.*

@Composable
fun QuestionsScreen(
    viewModel: QuestionsViewModel = hiltViewModel()
) {
    val questions by viewModel.questions.collectAsState()
    val isEvaluating by viewModel.isEvaluating.collectAsState()
    val evalMessage by viewModel.evalMessage.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()
    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            viewModel.evaluateQuestions()
            pullRefreshState.endRefresh()
        }
    }

    Box(Modifier.fillMaxSize().background(SurfaceDark)) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp).nestedScroll(pullRefreshState.nestedScrollConnection), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            item {
                val answered = questions.count { it.predictedAnswer != null }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).background(GroupHeaderBg, RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("#", Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Pregunta", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Resp.", Modifier.width(50.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                    Text("Pts", Modifier.width(40.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, textAlign = TextAlign.Center)
                }
                LinearProgressIndicator(
                    progress = { if (questions.isNotEmpty()) answered.toFloat() / questions.size else 0f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = TextSecondary, trackColor = SurfaceMedium
                )
            }

            itemsIndexed(questions) { _, q ->
                QuestionRow(q)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        PullToRefreshContainer(state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter), containerColor = Color.Transparent, contentColor = TextPrimary)

        // Show message only when there are new resolved questions
        LaunchedEffect(evalMessage) {
            delay(3000); viewModel.clearMessage()
        }
        evalMessage?.let { msg ->
            if (msg != "Sin cambios") {
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = { viewModel.clearMessage() }) { Text("OK") } }) {
                    Text(msg)
                }
            }
        }
    }
}

@Composable
private fun QuestionRow(q: QuestionEntity) {
    val closed = q.correctAnswer != null
    val bg = when {
        closed && q.pointsEarned > 0 -> CorrectBg
        closed -> IncorrectBg
        q.id % 2 == 0 -> MatchBg
        else -> MatchBgAlternate
    }
    Column(Modifier.fillMaxWidth().background(bg, RoundedCornerShape(6.dp)).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${q.id}", Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
            Text(q.text, Modifier.weight(1f).padding(vertical = 2.dp), style = MaterialTheme.typography.bodySmall, color = TextPrimary, softWrap = true)
            val answer = when { q.correctAnswer == true -> "V"; q.correctAnswer == false -> "F"; else -> "-" }
            val ansColor = when { q.correctAnswer == true -> AccentGreen; q.correctAnswer == false -> AccentRed; else -> TextMuted }
            Text(answer, Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall, color = ansColor, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            if (closed) {
                Text("+${q.pointsEarned}", Modifier.width(40.dp),
                    style = MaterialTheme.typography.bodySmall, color = if (q.pointsEarned > 0) AccentGreen else TextMuted,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            } else {
                Text("", Modifier.width(40.dp))
            }
        }
        if (closed) {
            val predicted = if (q.predictedAnswer == true) "V" else if (q.predictedAnswer == false) "F" else "-"
            val actual = if (q.correctAnswer == true) "V" else "F"
            Text(
                "Tu respuesta: $predicted  →  Real: $actual",
                fontSize = 12.sp, color = if (q.pointsEarned > 0) AccentGreen else AccentRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, top = 6.dp)
            )
        }
    }
}
