package com.porrawc2026.app.ui.screens.goalscorers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.data.remote.ApiService
import com.porrawc2026.app.data.repository.PorraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val apiService: ApiService
) : ViewModel() {

    private val _players = MutableStateFlow<List<PlayerPredictionEntity>>(emptyList())
    val players: StateFlow<List<PlayerPredictionEntity>> = _players.asStateFlow()

    private val _topScorers = MutableStateFlow<List<TopScorerDisplay>>(emptyList())
    val topScorers: StateFlow<List<TopScorerDisplay>> = _topScorers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPlayers()
        loadTopScorers()
    }

    private fun loadPlayers() {
        viewModelScope.launch {
            repository.getPlayerPredictions().collect { dbPlayers ->
                _players.value = dbPlayers.sortedBy { it.rank }
            }
        }
    }

    fun loadTopScorers() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val response = apiService.getWorldCupScorers()
                val scorers = response.scorers.take(10).mapIndexed { idx, s ->
                    TopScorerDisplay(
                        rank = idx + 1,
                        name = s.player.name ?: "?",
                        team = s.team.name ?: "",
                        goals = s.goals ?: 0,
                        assists = s.assists,
                        matches = s.playedMatches,
                        minutesPlayed = null,
                        flagEmoji = com.porrawc2026.app.util.ExcelParser.getFlagEmoji(s.team.name ?: "")
                    )
                }
                _topScorers.value = scorers
            } catch (e: Exception) {
                _topScorers.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
