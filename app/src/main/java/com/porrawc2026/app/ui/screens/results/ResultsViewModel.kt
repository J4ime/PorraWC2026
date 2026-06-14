package com.porrawc2026.app.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.QuestionEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.remote.ApiService
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                val response = apiService.getWorldCupMatches(status = "FINISHED")
                val currentMatches = allMatches.value
                response.matches.forEach { liveMatch ->
                    val homeGoals = liveMatch.score?.fullTime?.home
                    val awayGoals = liveMatch.score?.fullTime?.away
                    if (homeGoals == null || awayGoals == null) return@forEach
                    val localMatch = currentMatches.firstOrNull { m ->
                        TeamNameNormalizer.matches(m.homeTeam, liveMatch.homeTeam?.name ?: "") &&
                        TeamNameNormalizer.matches(m.awayTeam, liveMatch.awayTeam?.name ?: "")
                    } ?: return@forEach
                    // No actualizar partidos futuros
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("Europe/Madrid")
                    val matchDate = try { sdf.parse(localMatch.dateTime) } catch (_: Exception) { null }
                    if (matchDate != null && matchDate.after(Date())) return@forEach
                    if (localMatch.homeGoals != homeGoals || localMatch.awayGoals != awayGoals) {
                        repository.updateMatchResults(localMatch.id, homeGoals, awayGoals)
                        val pts = PointsCalculator.calculateMatchPoints(
                            localMatch.predictedHomeGoals, localMatch.predictedAwayGoals,
                            homeGoals, awayGoals
                        )
                        repository.updateMatchPrediction(localMatch.copy(homeGoals = homeGoals, awayGoals = awayGoals, pointsEarned = pts))
                    }
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
