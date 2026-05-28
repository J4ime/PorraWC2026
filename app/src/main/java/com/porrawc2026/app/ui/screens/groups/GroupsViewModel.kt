package com.porrawc2026.app.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.TeamEntity
import com.porrawc2026.app.data.repository.PorraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val repository: PorraRepository
) : ViewModel() {

    val allTeams: StateFlow<List<TeamEntity>> = repository.getAllTeams()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allMatches: StateFlow<List<MatchEntity>> = repository.getAllGroupMatches()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getGroupTeams(group: String): Flow<List<TeamEntity>> = repository.getTeamsByGroup(group)

    fun getGroupMatches(group: String): Flow<List<MatchEntity>> = repository.getGroupMatches(group)

    fun savePrediction(match: MatchEntity) {
        viewModelScope.launch {
            val existing = allMatches.value.find { it.id == match.id }
            val updated = match.copy(
                pointsEarned = calculateGroupPoints(match)
            )
            repository.updateMatchPrediction(updated)
        }
    }

    private fun calculateGroupPoints(match: MatchEntity): Int {
        val predHome = match.predictedHomeGoals ?: return 0
        val predAway = match.predictedAwayGoals ?: return 0
        val realHome = match.homeGoals ?: return 0
        val realAway = match.awayGoals ?: return 0

        var points = 0
        if (predHome == realHome) points += 10
        if (predAway == realAway) points += 10

        val predResult = when {
            predHome > predAway -> "home"
            predHome < predAway -> "away"
            else -> "draw"
        }
        val realResult = when {
            realHome > realAway -> "home"
            realHome < realAway -> "away"
            else -> "draw"
        }

        if (predResult == realResult) points += 30

        return points
    }
}
