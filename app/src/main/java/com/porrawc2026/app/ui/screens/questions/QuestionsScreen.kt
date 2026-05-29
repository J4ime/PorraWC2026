package com.porrawc2026.app.ui.screens.questions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.porrawc2026.app.data.local.entity.QuestionEntity
import com.porrawc2026.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    onBackClick: () -> Unit,
    viewModel: QuestionsViewModel = hiltViewModel()
) {
    val questions by viewModel.questions.collectAsState()

    Column(Modifier.fillMaxSize().background(SurfaceDark)) {
        TopAppBar(
            title = { Text("50 PREGUNTAS", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, null, tint = TextPrimary) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WCDarkBlue)
        )

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
    }
}

@Composable
private fun QuestionRow(q: QuestionEntity) {
    val bg = if (q.id % 2 == 0) MatchBg else MatchBgAlternate
    Row(Modifier.fillMaxWidth().background(bg, RoundedCornerShape(6.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text("${q.id}", Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall, color = TextMuted, fontWeight = FontWeight.Bold)
        Text(q.text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        val answer = when (q.predictedAnswer) { true -> "V" ; false -> "F" ; else -> "-" }
        val ansColor = when (q.predictedAnswer) { true -> AccentGreen ; false -> AccentRed ; else -> TextMuted }
        Text(answer, Modifier.width(50.dp), style = MaterialTheme.typography.bodySmall, color = ansColor, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text(if (q.pointsEarned > 0) "+${q.pointsEarned}" else "", Modifier.width(40.dp),
            style = MaterialTheme.typography.bodySmall, color = if (q.pointsEarned > 0) AccentGreen else TextMuted,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
