package com.porrawc2026.app.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.TeamEntity
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.domain.model.PointsCalculator
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

    val allMatches: StateFlow<List<MatchEntity>> = repository.getAllMatches()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allKnockoutPredictions: StateFlow<List<KnockoutPredictionEntity>> = repository.getKnockoutPredictions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getGroupTeams(group: String): Flow<List<TeamEntity>> = repository.getTeamsByGroup(group)

    fun getGroupMatches(group: String): Flow<List<MatchEntity>> = repository.getGroupMatches(group)

    fun savePrediction(match: MatchEntity) {
        viewModelScope.launch {
            val updated = match.copy(
                pointsEarned = PointsCalculator.calculateMatchPoints(match)
            )
            repository.updateMatchPrediction(updated)
        }
    }
}
