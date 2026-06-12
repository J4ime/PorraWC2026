package com.porrawc2026.app.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.QuestionEntity
import com.porrawc2026.app.data.remote.ApiService
import com.porrawc2026.app.data.repository.PorraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionsViewModel @Inject constructor(
    private val repository: PorraRepository,
    private val apiService: ApiService
) : ViewModel() {

    val questions: StateFlow<List<QuestionEntity>> = repository.getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isEvaluating = MutableStateFlow(false)
    val isEvaluating: StateFlow<Boolean> = _isEvaluating.asStateFlow()

    private val _evalMessage = MutableStateFlow<String?>(null)
    val evalMessage: StateFlow<String?> = _evalMessage.asStateFlow()

    fun evaluateQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isEvaluating.value = true
            try {
                val allMatches = repository.getAllMatches().first()
                val finishedMatches = allMatches.filter {
                    !it.isKnockout && it.id < 900 && it.homeGoals != null && it.awayGoals != null
                }
                val totalGoals = finishedMatches.sumOf { (it.homeGoals ?: 0) + (it.awayGoals ?: 0) }
                val redCards = finishedMatches.size // Placeholder
                var resolved = 0

                questions.value.filter { it.correctAnswer == null }.forEach { q ->
                    val text = q.text.lowercase()
                    val result: Boolean? = when {
                        text.contains("goles") && text.contains("total") && text.contains("más de") -> {
                            val nums = text.split(" ").mapNotNull { it.replace(".", "").toIntOrNull() }
                            if (nums.isNotEmpty()) totalGoals > nums.last() else null
                        }
                        text.contains("goles") && text.contains("total") && (text.contains("menos") || text.contains("menor")) -> {
                            val nums = text.split(" ").mapNotNull { it.replace(".", "").toIntOrNull() }
                            if (nums.isNotEmpty()) totalGoals < nums.last() else null
                        }
                        text.contains("goles") && text.contains("total") -> {
                            val nums = text.split(" ").mapNotNull { it.replace(".", "").toIntOrNull() }
                            if (nums.isNotEmpty()) totalGoals >= nums.last() else null
                        }
                        text.contains("tarjeta") && text.contains("roja") -> null // Can't determine yet
                        text.contains("campeon") || text.contains("ganar") && text.contains("mundial") -> null // End of tournament
                        else -> null
                    }
                    if (result != null) {
                        val pts = if (q.predictedAnswer == result) 20 else 0
                        repository.updateQuestionPrediction(q.copy(correctAnswer = result, pointsEarned = pts))
                        resolved++
                    }
                }
                _evalMessage.value = if (resolved > 0) "$resolved preguntas resueltas" else "Sin cambios"
            } catch (e: Exception) {
                _evalMessage.value = "Error: ${e.message}"
            } finally {
                _isEvaluating.value = false
            }
        }
    }
}
