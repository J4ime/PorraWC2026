package com.porrawc2026.app.ui.screens.groups

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.TeamEntity
import com.porrawc2026.app.data.repository.PorraRepository
import com.porrawc2026.app.domain.model.KnockoutCalculator
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

    private val _knockoutPointsMap = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val knockoutPointsMap: StateFlow<Map<Int, Int>> = _knockoutPointsMap.asStateFlow()

    init {
        viewModelScope.launch {
            combine(allMatches, allKnockoutPredictions) { matches, predictions ->
                val liveRoundLists = KnockoutCalculator.buildLiveRoundLists(matches)
                // DEBUG: dump Octavos match data from DB
                val octavosMatches = matches.filter { it.id in 89..96 }
                for (m in octavosMatches) {
                    Log.d("KO_DEBUG", "GroupsVM DB id=${m.id} koRound=${m.knockoutRound} isKo=${m.isKnockout} homeTeam='${m.homeTeam}' awayTeam='${m.awayTeam}' homeGoals=${m.homeGoals} awayGoals=${m.awayGoals}")
                }
                Log.d("KO_DEBUG", "GroupsVM DB liveRoundLists[Octavos]=${liveRoundLists["Octavos"]}")
                Log.d("KO_DEBUG", "GroupsVM DB octavos predictions: ${predictions.filter { it.round == "Octavos" }.map { "${it.matchNumber}: refs ${it.homeTeamRef}/${it.awayTeamRef}" }}")
                val (matchPts, predPts) = KnockoutCalculator.computePointsFromLiveLists(predictions, liveRoundLists, matches)
                val combined = matchPts + predPts
                Log.d("KO_DEBUG", "GroupsViewModel knockoutPointsMap=$combined matchPts=$matchPts predPts=$predPts")
                combined
            }.collect { _knockoutPointsMap.value = it }
        }
    }

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
