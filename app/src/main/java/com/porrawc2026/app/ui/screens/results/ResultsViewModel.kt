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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repository: PorraRepository,
    private val apiService: ApiService
) : ViewModel() {
    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

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
        viewModelScope.launch(ioDispatcher) {
            _isRefreshing.value = true
            runCatching {
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
                    val matchInstant = parseInstant(localMatch.dateTime)
                    if (matchInstant != null && matchInstant.isAfter(Instant.now())) return@forEach
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
            }.onFailure { e ->
                _errorMessage.emit("Error conectando al servidor: ${e.message}")
            }
            _isRefreshing.value = false
        }
    }

    private fun refreshPoints() {
        viewModelScope.launch {
            _totalPoints.value = repository.calculateTotalPoints()
        }
    }

    private fun parseInstant(dateTime: String): Instant? {
        if (dateTime.isBlank()) return null
        return try {
            if (dateTime.endsWith("Z")) {
                Instant.parse(dateTime)
            } else {
                val local = LocalDateTime.parse(dateTime, dateTimeFormatter)
                local.atZone(madridZone).toInstant()
            }
        } catch (_: Exception) { null }
    }

    companion object {
        private val madridZone = ZoneId.of("Europe/Madrid")
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    }
}
