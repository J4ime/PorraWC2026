package com.porrawc2026.app.ui.screens.questions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        "50 PREGUNTAS",
                        style = MaterialTheme.typography.titleLarge,
                        color = WCGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "20 puntos por acierto",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = WCDarkBlue)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val answered = questions.count { it.predictedAnswer != null }
                val progress = if (questions.isNotEmpty()) answered.toFloat() / questions.size else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = WCGold,
                    trackColor = SurfaceMedium,
                )
                Text(
                    text = "$answered / ${questions.size} contestadas",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            itemsIndexed(questions) { index, question ->
                QuestionCard(
                    question = question,
                    onAnswer = { answer ->
                        viewModel.saveAnswer(
                            question.copy(
                                predictedAnswer = answer,
                                pointsEarned = if (question.correctAnswer != null && answer == question.correctAnswer) 20 else 0
                            )
                        )
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun QuestionCard(
    question: QuestionEntity,
    onAnswer: (Boolean) -> Unit
) {
    val isSelectedTrue = question.predictedAnswer == true
    val isSelectedFalse = question.predictedAnswer == false

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(WCGold.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${question.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = WCGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    question.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onAnswer(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelectedTrue) AccentGreen else SurfaceMedium,
                        contentColor = if (isSelectedTrue) TextPrimary else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSelectedTrue) Icon(Icons.Filled.CheckCircle, contentDescription = null, Modifier.size(16.dp))
                    Text("VERDADERO", modifier = Modifier.padding(start = 4.dp))
                }
                Button(
                    onClick = { onAnswer(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelectedFalse) AccentRed else SurfaceMedium,
                        contentColor = if (isSelectedFalse) TextPrimary else TextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSelectedFalse) Icon(Icons.Filled.CheckCircle, contentDescription = null, Modifier.size(16.dp))
                    Text("FALSO", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
