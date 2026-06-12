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

    private fun enToEsTeamName(name: String): String {
        val map = mapOf(
            "Mexico" to "México", "South Africa" to "Sudáfrica", "South Korea" to "Corea del Sur",
            "Korea Republic" to "Corea del Sur", "Czechia" to "República Checa",
            "Canada" to "Canadá", "Bosnia-Herzegovina" to "Bosnia y Herzegovina",
            "United States" to "Estados Unidos", "Paraguay" to "Paraguay",
            "Australia" to "Australia", "Turkey" to "Turquía", "Germany" to "Alemania",
            "Curaçao" to "Curazao", "Ivory Coast" to "Costa de Marfil",
            "Ecuador" to "Ecuador", "Netherlands" to "Países Bajos", "Japan" to "Japón",
            "Sweden" to "Suecia", "Tunisia" to "Túnez", "Belgium" to "Bélgica",
            "Egypt" to "Egipto", "Iran" to "Irán", "New Zealand" to "Nueva Zelanda",
            "Spain" to "España", "Cape Verde" to "Cabo Verde",
            "Saudi Arabia" to "Arabia Saudita", "Uruguay" to "Uruguay",
            "France" to "Francia", "Senegal" to "Senegal", "Iraq" to "Irak",
            "Norway" to "Noruega", "Argentina" to "Argentina", "Algeria" to "Argelia",
            "Austria" to "Austria", "Jordan" to "Jordania", "Portugal" to "Portugal",
            "Congo DR" to "RD Congo", "Uzbekistan" to "Uzbekistán",
            "Colombia" to "Colombia", "England" to "Inglaterra", "Croatia" to "Croacia",
            "Panama" to "Panamá", "Ghana" to "Ghana", "Brazil" to "Brasil",
            "Morocco" to "Marruecos", "Scotland" to "Escocia", "Haiti" to "Haití",
            "Switzerland" to "Suiza", "Qatar" to "Catar"
        )
        return map[name] ?: name
    }

    fun loadTopScorers() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val response = apiService.getWorldCupScorers()
                val scorers = response.scorers.take(10).mapIndexed { idx, s ->
                    val teamName = enToEsTeamName(s.team.name ?: "")
                    TopScorerDisplay(
                        rank = idx + 1,
                        name = s.player.name ?: "?",
                        team = teamName,
                        goals = s.goals ?: 0,
                        assists = s.assists,
                        matches = s.playedMatches,
                        minutesPlayed = null,
                        flagEmoji = com.porrawc2026.app.util.ExcelParser.getFlagEmoji(teamName)
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
