package com.porrawc2026.app.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.QuestionEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.remote.ApiService
import com.porrawc2026.app.data.repository.PorraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repository: PorraRepository,
    private val apiService: ApiService
) : ViewModel() {

    val allMatches: StateFlow<List<MatchEntity>> = repository.getAllMatches()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val questions: StateFlow<List<QuestionEntity>> = repository.getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val playerPredictions: StateFlow<List<PlayerPredictionEntity>> = repository.getPlayerPredictions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val knockoutPredictions: StateFlow<List<KnockoutPredictionEntity>> = repository.getKnockoutPredictions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    init {
        refreshPoints()
    }

    fun refreshLiveScores() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val response = apiService.getWorldCupMatches()
                response.matches.forEach { liveMatch: com.porrawc2026.app.data.remote.FootballMatch ->
                    // Compare and calculate points when live
                }
                refreshPoints()
            } catch (e: Exception) {
                _errorMessage.emit("Error conectando al servidor: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun refreshPoints() {
        viewModelScope.launch {
            _totalPoints.value = repository.calculateTotalPoints()
        }
    }

    fun getGroupPoints(): Int {
        return allMatches.value.sumOf { it.pointsEarned }
    }

    fun getKnockoutPoints(): Int {
        return knockoutPredictions.value.sumOf { it.pointsEarned }
    }

    fun getQuestionPoints(): Int {
        return questions.value.sumOf { it.pointsEarned }
    }

    fun getPlayerPoints(): Int {
        return playerPredictions.value.sumOf { it.pointsEarned }
    }
}
