package com.porrawc2026.app.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.QuestionEntity
import com.porrawc2026.app.data.repository.PorraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionsViewModel @Inject constructor(
    private val repository: PorraRepository
) : ViewModel() {

    val questions: StateFlow<List<QuestionEntity>> = repository.getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isEvaluating = MutableStateFlow(false)
    val isEvaluating: StateFlow<Boolean> = _isEvaluating.asStateFlow()

    private val _evalMessage = MutableStateFlow<String?>(null)
    val evalMessage: StateFlow<String?> = _evalMessage.asStateFlow()

    fun evaluateQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isEvaluating.value = true
            try {
                val allMatches = repository.getAllMatches().first()
                val groupMatches = allMatches.filter { !it.isKnockout && it.id < 900 }
                val finished = allMatches.filter { it.homeGoals != null && it.awayGoals != null }
                val groupFinished = groupMatches.filter { it.homeGoals != null && it.awayGoals != null }
                val knockout = allMatches.filter { it.isKnockout }

                // Compute standings per group
                val standings = computeStandings(groupFinished)

                // Helper: find match by teams
                fun findMatch(home: String, away: String) = finished.firstOrNull {
                    matchName(it.homeTeam, home) && matchName(it.awayTeam, away)
                }

                // Helper: match names with normalization
                fun matchName(a: String, b: String) = normalize(a) == normalize(b)

                // Helper: team points in group
                fun teamPoints(team: String) = standings[team]?.points ?: 0

                // Helper: team position in group
                fun teamPosition(team: String) = standings[team]?.position ?: 0

                // Helper: team qualified from group (top 3)
                fun qualifiedFromGroup(team: String) = teamPosition(team) in 1..3

                // Tournament totals
                val totalGoals = finished.sumOf { (it.homeGoals ?: 0) + (it.awayGoals ?: 0) }
                val ownGoals = 0 // Not tracked currently

                var resolved = 0
                questions.value.filter { it.correctAnswer == null }.forEach { q ->
                    val result: Boolean? = when (q.id) {
                        1 -> findMatch("México", "República Checa")?.let {
                            (it.homeGoals ?: 0) > 0 && (it.awayGoals ?: 0) > 0
                        }
                        2 -> findMatch("Suiza", "Canadá")?.let {
                            kotlin.math.abs((it.homeGoals ?: 0) - (it.awayGoals ?: 0)) == 1
                        }
                        3 -> findMatch("Brasil", "Marruecos")?.let {
                            (it.homeGoals ?: 0) + (it.awayGoals ?: 0) >= 3
                        }
                        4 -> findMatch("Estados Unidos", "Turquía")?.let {
                            (it.homeGoals ?: 0) > 0 && (it.awayGoals ?: 0) > 0
                        }
                        5 -> findMatch("Alemania", "Ecuador")?.let {
                            // Germany scoring in both halves - can't determine from final score alone
                            null
                        }
                        6 -> findMatch("Países Bajos", "Suecia")?.let {
                            (it.homeGoals ?: 0) + (it.awayGoals ?: 0) >= 2
                        }
                        7 -> findMatch("Bélgica", "Egipto")?.let {
                            val h = it.homeGoals ?: 0; val a = it.awayGoals ?: 0
                            if (matchName(it.homeTeam, "Bélgica")) h > a && a > 0
                            else a > h && h > 0
                        }
                        8 -> findMatch("España", "Uruguay")?.let {
                            // Spain scores first - need goal timeline, can't determine
                            null
                        }
                        9 -> findMatch("Francia", "Senegal")?.let {
                            (it.homeGoals ?: 0) > 0 && (it.awayGoals ?: 0) > 0
                        }
                        10 -> findMatch("Argentina", "Austria")?.let {
                            if (matchName(it.homeTeam, "Argentina"))
                                (it.homeGoals ?: 0) - (it.awayGoals ?: 0) >= 2
                            else (it.awayGoals ?: 0) - (it.homeGoals ?: 0) >= 2
                        }
                        11 -> findMatch("Portugal", "Colombia")?.let {
                            (it.homeGoals ?: 0) + (it.awayGoals ?: 0) <= 3
                        }
                        12 -> findMatch("Inglaterra", "Croacia")?.let {
                            kotlin.math.abs((it.homeGoals ?: 0) - (it.awayGoals ?: 0)) == 2
                        }
                        // Group standings questions - only evaluate when group is done
                        13 -> {
                            val g = groupFinished.filter { it.groupName.contains("I") || it.groupName.contains("Grupo I") }
                            if (g.size >= 12) !qualifiedFromGroup("Francia") else null
                        }
                        14 -> {
                            val ptsIraq = teamPoints("Irak"); val ptsHaiti = teamPoints("Haití")
                            if (ptsIraq > 0 || ptsHaiti > 0) ptsIraq > ptsHaiti else null
                        }
                        15 -> {
                            val g = groupFinished.filter { it.groupName.contains("E") || it.groupName.contains("Grupo E") }
                            if (g.size >= 12) qualifiedFromGroup("Alemania") else null
                        }
                        16 -> {
                            // Team came back from 2 goals down in 2nd half - need timeline
                            null
                        }
                        17 -> qualifiedFromGroup("Curazao") // Group E
                        18 -> teamPoints("Brasil") > teamPoints("Argentina") && groupFinished.size >= 72
                        19 -> {
                            // Match with >8 cards - need card data from Zafronix
                            null
                        }
                        20 -> {
                            // Team advances without winning - need all groups done
                            if (groupFinished.size >= 72) {
                                standings.any { (_, s) -> s.points > 0 && s.wins == 0 && s.position in 1..3 }
                            } else null
                        }
                        21 -> {
                            val g = groupFinished.filter { it.groupName.contains("I") || it.groupName.contains("Grupo I") }
                            if (g.size >= 12) teamPosition("Noruega") == 2 else null
                        }
                        22 -> ownGoals >= 3 && groupFinished.size >= 72
                        23 -> {
                            // Australia wins at least 1 group match
                            val ausWins = groupFinished.count {
                                it.homeTeam == "Australia" && (it.homeGoals ?: 0) > (it.awayGoals ?: 0) ||
                                it.awayTeam == "Australia" && (it.awayGoals ?: 0) > (it.homeGoals ?: 0)
                            }
                            if (ausWins > 0) true
                            else if (groupFinished.size >= 72) false
                            else null
                        }
                        24 -> teamPoints("Colombia") > teamPoints("Marruecos") && groupFinished.size >= 72
                        25 -> {
                            // 2+ penalties missed in a match - need penalty data
                            null
                        }
                        26 -> qualifiedFromGroup("Panamá")
                        27 -> {
                            // Top scorer 8+ goals - check at end
                            if (finished.size >= totalMatches) totalGoals >= 8 else null
                        }
                        28 -> {
                            // 4+ matches decided in extra time - knockout stage
                            null
                        }
                        29 -> {
                            // 3+ matches decided on penalties
                            null
                        }
                        30 -> {
                            // Goalkeeper saves 2 penalties in shootout
                            null
                        }
                        31 -> {
                            // Team wins with a player sent off - need card data
                            null
                        }
                        32 -> {
                            // Spain reaches semifinals
                            if (knockout.any { it.knockoutRound == "Semifinales" && it.homeGoals != null }) {
                                knockout.any {
                                    it.knockoutRound == "Semifinales" &&
                                    (it.homeTeam == "España" || it.awayTeam == "España") &&
                                    it.homeGoals != null
                                }
                            } else null
                        }
                        33 -> {
                            // 7+ corners in opening match
                            findMatch("México", "Sudáfrica")?.let { null } // Need corner data
                        }
                        34 -> {
                            // 3+ African teams in round of 32 - need knockout data
                            null
                        }
                        35 -> {
                            // Champion is top scorer - end of tournament
                            null
                        }
                        36 -> {
                            // 4+ direct red cards during tournament - need card data
                            null
                        }
                        37 -> {
                            // 10+ headed goals - need goal type data
                            null
                        }
                        38 -> {
                            // 4+ European teams in round of 16
                            null
                        }
                        39 -> {
                            // Round of 32 match with 7 goals
                            null
                        }
                        40 -> {
                            // 3rd place match: 4+ goals
                            null
                        }
                        41 -> {
                            // Champion didn't lose any match
                            null
                        }
                        42 -> {
                            // Player scores brace in round of 16
                            null
                        }
                        43 -> {
                            // European team in semifinals - end of tournament
                            null
                        }
                        44 -> {
                            // USA reaches round of 16
                            null
                        }
                        45 -> {
                            // Quarterfinal match with 6+ cards
                            null
                        }
                        46 -> {
                            // Final has at least 1 sending off
                            null
                        }
                        47 -> {
                            // Semifinal: sub scores from bench
                            null
                        }
                        48 -> {
                            // Canada advances further than Mexico
                            null
                        }
                        49 -> {
                            // Final has a penalty
                            null
                        }
                        50 -> {
                            // Champion wins final on penalties
                            null
                        }
                        else -> null
                    }
                    if (result != null) {
                        val pts = if (q.predictedAnswer == result) 20 else 0
                        repository.updateQuestionPrediction(q.copy(correctAnswer = result, pointsEarned = pts))
                        resolved++
                    }
                }
                _evalMessage.value = if (resolved > 0) "$resolved preguntas resueltas" else "Sin cambios"
            } catch (e: Exception) {
                _evalMessage.value = "Error: ${e.message}"
            } finally {
                _isEvaluating.value = false
            }
        }
    }

    data class TeamStanding(val points: Int, val position: Int, val wins: Int)

    private fun computeStandings(matches: List<MatchEntity>): Map<String, TeamStanding> {
        val teamData = mutableMapOf<String, Triple<Int, Int, Int>>() // points, GD, wins
        matches.forEach { m ->
            val h = m.homeGoals ?: return@forEach; val a = m.awayGoals ?: return@forEach
            val home = teamData.getOrPut(m.homeTeam) { Triple(0, 0, 0) }
            val away = teamData.getOrPut(m.awayTeam) { Triple(0, 0, 0) }
            when {
                h > a -> {
                    teamData[m.homeTeam] = Triple(home.first + 3, home.second + (h - a), home.third + 1)
                    teamData[m.awayTeam] = Triple(away.first, away.second + (a - h), away.third)
                }
                h < a -> {
                    teamData[m.homeTeam] = Triple(home.first, home.second + (h - a), home.third)
                    teamData[m.awayTeam] = Triple(away.first + 3, away.second + (a - h), away.third + 1)
                }
                else -> {
                    teamData[m.homeTeam] = Triple(home.first + 1, home.second, home.third)
                    teamData[m.awayTeam] = Triple(away.first + 1, away.second, away.third)
                }
            }
        }
        // Sort by group (using groupName) and rank
        val groups = matches.groupBy { it.groupName }
        val result = mutableMapOf<String, TeamStanding>()
        groups.forEach { (_, groupMatches) ->
            val teams = groupMatches.flatMap { listOf(it.homeTeam, it.awayTeam) }.distinct()
            val sorted = teams.sortedByDescending { t ->
                val (pts, gd, _) = teamData[t] ?: Triple(0, 0, 0)
                pts * 1000 + gd
            }
            sorted.forEachIndexed { idx, team ->
                val (pts, _, wins) = teamData[team] ?: Triple(0, 0, 0)
                result[team] = TeamStanding(pts, idx + 1, wins)
            }
        }
        return result
    }

    private fun normalize(name: String): String {
        return java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "").replace(" ", "").lowercase()
    }

    private fun matchName(a: String, b: String) = normalize(a) == normalize(b)

    companion object {
        const val totalMatches = 104
    }
}
