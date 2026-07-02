package com.porrawc2026.app.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.KnockoutTeamProgressEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.local.entity.QuestionEntity
import com.porrawc2026.app.data.remote.ApiService
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.domain.model.KnockoutCalculator
import com.porrawc2026.app.domain.model.PointsCalculator
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import com.porrawc2026.app.util.DateTimeUtil
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

data class KnockoutResultDisplay(
    val matchNumber: Int,
    val round: String,
    val homeTeam: String,
    val awayTeam: String,
    val predictedWinnerTeam: String?,
    val actualWinnerTeam: String?,
    val pointsEarned: Int,
    val isCorrect: Boolean
)

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

    val allTeamProgress: StateFlow<List<KnockoutTeamProgressEntity>> = repository.getKnockoutTeamProgress()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val groupPoints: StateFlow<Int> = allMatches
        .map { matches -> matches.filter { !it.isKnockout }.sumOf { it.pointsEarned } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val qPoints: StateFlow<Int> = questions
        .map { questions -> questions.sumOf { it.pointsEarned } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val pPoints: StateFlow<Int> = playerPredictions
        .map { players -> players.sumOf { it.pointsEarned } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _knockoutResults = MutableStateFlow<List<KnockoutResultDisplay>>(emptyList())
    val knockoutResults: StateFlow<List<KnockoutResultDisplay>> = _knockoutResults.asStateFlow()

    val kokoPoints: StateFlow<Int> = _knockoutResults
        .map { results -> results.sumOf { it.pointsEarned } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val totalPoints: StateFlow<Int> = combine(groupPoints, kokoPoints, qPoints, pPoints) { g, k, q, p ->
        g + k + q + p
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            combine(allTeamProgress, knockoutPredictions) { progress, predictions ->
                computeKnockoutResults(progress, predictions)
            }.collect { results ->
                _knockoutResults.value = results
            }
        }
    }

private fun computeKnockoutResults(
        progress: List<KnockoutTeamProgressEntity>,
        predictions: List<KnockoutPredictionEntity>
    ): List<KnockoutResultDisplay> {
        val advancement = KnockoutCalculator.advancementMapFromEntities(progress)
        val resolvedHome = predictions.associate {
            it.matchNumber to PointsCalculator.resolvePredictionTeamName(it.homeTeamRef, predictions)
        }
        val resolvedAway = predictions.associate {
            it.matchNumber to PointsCalculator.resolvePredictionTeamName(it.awayTeamRef, predictions)
        }

        return predictions.map { prediction ->
            val homeTeam = resolvedHome[prediction.matchNumber] ?: prediction.homeTeamRef
            val awayTeam = resolvedAway[prediction.matchNumber] ?: prediction.awayTeamRef
            
            val unresolvedTeams = homeTeam.startsWith("W") || homeTeam.startsWith("L") || 
                awayTeam.startsWith("W") || awayTeam.startsWith("L")
            
            val predictionRoundLevel = KnockoutCalculator.roundLevel(prediction.round)
            var pointsEarned = 0
            
            if (!unresolvedTeams && predictionRoundLevel > 0) {
                val homeReachedRound = advancement.entries.firstOrNull { (team, _) ->
                    TeamNameNormalizer.matches(team, homeTeam)
                }?.value
                if (homeReachedRound != null) {
                    pointsEarned += PointsCalculator.getKnockoutPoints(homeReachedRound)
                }
                
                val awayReachedRound = advancement.entries.firstOrNull { (team, _) ->
                    TeamNameNormalizer.matches(team, awayTeam)
                }?.value
                if (awayReachedRound != null) {
                    pointsEarned += PointsCalculator.getKnockoutPoints(awayReachedRound)
                }
            }

            KnockoutResultDisplay(
                matchNumber = prediction.matchNumber,
                round = prediction.round,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                predictedWinnerTeam = null,
                actualWinnerTeam = null,
                pointsEarned = pointsEarned,
                isCorrect = pointsEarned > 0
            )
        }
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
            }.onFailure { e ->
                _errorMessage.emit("Error conectando al servidor: ${e.message}")
            }
            _isRefreshing.value = false
        }
    }

    private fun parseInstant(dateTime: String): Instant? =
        DateTimeUtil.parseMadridInstant(dateTime, "ResultsVM")
}
