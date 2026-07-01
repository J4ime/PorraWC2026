package com.porrawc2026.app.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.local.entity.QuestionEntity
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
            combine(allMatches, knockoutPredictions) { matches, predictions ->
                computeKnockoutResults(matches, predictions)
            }.collect { results ->
                _knockoutResults.value = results
            }
        }
    }

    // Map each team to the furthest round they actually reached
    private fun buildAdvancement(matches: List<MatchEntity>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val koRounds = listOf("Dieciseisavos", "Octavos", "Cuartos", "Semifinales", "Final")

        for (match in matches.filter { it.homeTeam.isNotBlank() }) {
            if (!match.isKnockout || match.knockoutRound == null) continue
            val round = match.knockoutRound
            if (round !in koRounds) continue

            for (team in listOf(match.homeTeam, match.awayTeam)) {
                val prev = result[team]
                if (prev == null || roundLevel(round) > roundLevel(prev)) {
                    result[team] = round
                }
            }
            // Winner advances to the next round
            val winner = match.winnerTeam
            if (!winner.isNullOrBlank()) {
                val nextIdx = koRounds.indexOf(round) + 1
                if (nextIdx < koRounds.size) {
                    val nextRound = koRounds[nextIdx]
                    val prev = result[winner]
                    if (prev == null || roundLevel(nextRound) > roundLevel(prev)) {
                        result[winner] = nextRound
                    }
                }
            }
        }
        return result
    }

    private fun roundLevel(round: String): Int = when (round) {
        "Dieciseisavos" -> 1
        "Octavos" -> 2
        "Cuartos" -> 3
        "Semifinales" -> 4
        "3er puesto" -> 5
        "Final" -> 6
        "Campeón" -> 7
        else -> 0
    }

    private fun computeKnockoutResults(
        matches: List<MatchEntity>,
        predictions: List<KnockoutPredictionEntity>
    ): List<KnockoutResultDisplay> {
        val resolvedHome = predictions.associate {
            it.matchNumber to PointsCalculator.resolvePredictionTeamName(it.homeTeamRef, predictions)
        }
        val resolvedAway = predictions.associate {
            it.matchNumber to PointsCalculator.resolvePredictionTeamName(it.awayTeamRef, predictions)
        }
        val advancement = buildAdvancement(matches)

        // Per-match actual winner for strikethrough display (the team that won the match)
        val matchByNumber = matches.filter { it.matchNumber != null }.associateBy { it.matchNumber!! }
        val actualWinnerByMatch: Map<Int, String?> = matchByNumber.mapValues { (_, m) ->
            val mtch = m
            val wTeam = mtch.winnerTeam
            if (!wTeam.isNullOrBlank()) {
                // Convert English winner name to Spanish for display consistency
                TeamNameNormalizer.enToEs(wTeam)
            } else if (mtch.homeGoals != null && mtch.awayGoals != null && mtch.homeGoals != mtch.awayGoals) {
                if (mtch.homeGoals!! > mtch.awayGoals!!) mtch.homeTeam else mtch.awayTeam
            } else null
        }

        return predictions.map { prediction ->
            val homeTeam = resolvedHome[prediction.matchNumber] ?: prediction.homeTeamRef
            val awayTeam = resolvedAway[prediction.matchNumber] ?: prediction.awayTeamRef
            val predictedWinner = when (prediction.winner) {
                1 -> homeTeam
                2 -> awayTeam
                else -> null
            }
            // Check if the predicted winner advanced past this round (advancement-based)
            val actualReachedRound = predictedWinner?.let { winner ->
                advancement.entries.firstOrNull { (team, _) ->
                    TeamNameNormalizer.matches(team, winner)
                }?.value
            }
            val isCorrect = if (prediction.round == "3er puesto") {
                actualReachedRound != null && roundLevel(actualReachedRound) == roundLevel(prediction.round)
            } else {
                actualReachedRound != null && roundLevel(actualReachedRound) > roundLevel(prediction.round)
            }
            val roundPoints = PointsCalculator.getKnockoutPoints(prediction.round)
            val pointsEarned = if (isCorrect) roundPoints else 0
            val actualWinner = actualWinnerByMatch[prediction.matchNumber]

            KnockoutResultDisplay(
                matchNumber = prediction.matchNumber,
                round = prediction.round,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                predictedWinnerTeam = predictedWinner,
                actualWinnerTeam = actualWinner,
                pointsEarned = pointsEarned,
                isCorrect = isCorrect
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

    private fun parseInstant(dateTime: String): Instant? {
        if (dateTime.isBlank()) return null
        return try {
            if (dateTime.endsWith("Z")) {
                Instant.parse(dateTime)
            } else {
                val local = LocalDateTime.parse(dateTime, dateTimeFormatter)
                local.atZone(madridZone).toInstant()
            }
        } catch (e: Exception) { android.util.Log.e("ResultsVM", "parseMadridInstant failed for dateTime=$dateTime", e); null }
    }

    companion object {
        private val madridZone = ZoneId.of("Europe/Madrid")
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    }
}
