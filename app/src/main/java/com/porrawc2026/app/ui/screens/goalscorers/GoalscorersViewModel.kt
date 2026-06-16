package com.porrawc2026.app.ui.screens.goalscorers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.remote.LiveScoreService
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import com.porrawc2026.app.util.GoalEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class TopScorerDisplay(
    val rank: Int,
    val name: String,
    val team: String,
    val goals: Int,
    val assists: Int? = null,
    val matches: Int? = null,
    val minutesPlayed: Int? = null,
    val flagEmoji: String = ""
)

@HiltViewModel
class GoalscorersViewModel @Inject constructor(
    private val repository: PorraRepository,
    private val liveScoreService: LiveScoreService,
    private val goalEventBus: GoalEventBus
) : ViewModel() {

    private val _players = MutableStateFlow<List<PlayerPredictionEntity>>(emptyList())
    val players: StateFlow<List<PlayerPredictionEntity>> = _players.asStateFlow()

    private val _topScorers = MutableStateFlow<List<TopScorerDisplay>>(emptyList())
    val topScorers: StateFlow<List<TopScorerDisplay>> = _topScorers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPlayers()
        viewModelScope.launch { fetchWithRetry() }
        viewModelScope.launch {
            goalEventBus.goalScored.collect {
                fetchTopScorers()
            }
        }
    }

    private suspend fun fetchWithRetry() {
        var attempt = 0
        while (attempt < 5) {
            fetchTopScorers()
            if (_topScorers.value.isNotEmpty()) break
            attempt++
            if (attempt < 5) delay(10_000L * attempt)
        }
    }

    fun refresh() {
        viewModelScope.launch { fetchTopScorers() }
    }

    private fun loadPlayers() {
        viewModelScope.launch {
            repository.getPlayerPredictions().collect { dbPlayers ->
                _players.value = dbPlayers.sortedBy { it.rank }
            }
        }
    }

    private suspend fun fetchTopScorers() {
        _isLoading.value = true
        runCatching {
            val allMatches = repository.getAllMatches().first()
            val scorers = liveScoreService.fetchTopScorers(allMatches)
            val tenthGoals = scorers.getOrNull(9)?.goals ?: Int.MAX_VALUE
            val displayScorers = scorers
                .take(10)
                .plus(scorers.drop(10).filter { it.goals >= tenthGoals })
                .mapIndexed { idx, s ->
                val teamName = TeamNameNormalizer.enToEs(s.teamName)
                TopScorerDisplay(
                    rank = idx + 1,
                    name = s.playerName,
                    team = teamName,
                    goals = s.goals,
                    assists = null,
                    matches = null,
                    minutesPlayed = null,
                    flagEmoji = com.porrawc2026.app.util.ExcelParser.getFlagEmoji(teamName)
                )
            }
            _topScorers.value = displayScorers
        }.onFailure {
            _topScorers.value = emptyList()
        }
        _isLoading.value = false
    }
}
