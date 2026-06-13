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

    fun clearMessage() { _evalMessage.value = null }

    fun evaluateQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isEvaluating.value = true
            try {
                // Reset all previously resolved answers
                val all = repository.getAllQuestions().first()
                all.forEach { q ->
                    if (q.correctAnswer != null || q.pointsEarned != 0) {
                        repository.updateQuestionPrediction(q.copy(correctAnswer = null, pointsEarned = 0))
                    }
                }

                val allMatches = repository.getAllMatches().first()
                val groupMatches = allMatches.filter { !it.isKnockout && it.id < 900 }
                val finished = allMatches.filter { it.homeGoals != null && it.awayGoals != null }
                val groupFinished = groupMatches.filter { it.homeGoals != null && it.awayGoals != null }
                val knockout = allMatches.filter { it.isKnockout }
                val standings = computeStandings(groupFinished)

                fun findMatch(home: String, away: String) = finished.firstOrNull {
                    matchName(it.homeTeam, home) && matchName(it.awayTeam, away)
                }
                fun teamPoints(team: String) = standings[team]?.points ?: 0
                fun teamPosition(team: String) = standings[team]?.position ?: 0
                fun qualifiedFromGroup(team: String) = teamPosition(team) in 1..3

                val totalGoals = finished.sumOf { (it.homeGoals ?: 0) + (it.awayGoals ?: 0) }
                val totalRedCards = finished.sumOf { (it.homeRedCards ?: 0) + (it.awayRedCards ?: 0) }

                var resolved = 0
                questions.value.filter { it.correctAnswer == null }.forEach { q ->
                    val result: Boolean? = evaluateById(q.id, finished, groupFinished, knockout,
                        standings, totalGoals, totalRedCards, ::findMatch, ::teamPoints,
                        ::teamPosition, ::qualifiedFromGroup, ::matchName)
                    if (result != null) {
                        val pts = if (q.predictedAnswer == result) 20 else 0
                        repository.updateQuestionPrediction(q.copy(correctAnswer = result, pointsEarned = pts))
                        resolved++
                    }
                }
                // Also recalculate total points in HomeViewModel via repository
                val pts = repository.calculateTotalPoints()
                _evalMessage.value = if (resolved > 0) "$resolved preguntas resueltas ($pts pts total)" else "Sin cambios"
            } catch (e: Exception) {
                _evalMessage.value = "Error: ${e.message}"
            } finally { _isEvaluating.value = false }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun evaluateById(id: Int, finished: List<MatchEntity>, groupFinished: List<MatchEntity>,
        knockout: List<MatchEntity>, standings: Map<String, TeamStanding>,
        totalGoals: Int, totalRedCards: Int,
        findMatch: (String, String) -> MatchEntity?, teamPoints: (String) -> Int,
        teamPosition: (String) -> Int, qualifiedFromGroup: (String) -> Boolean,
        matchName: (String, String) -> Boolean
    ): Boolean? {
        val allDone = finished.size >= 104
        val groupsDone = groupFinished.size >= 72

        return when (id) {
            // Q1-12: Partidos específicos → se cierran cuando el partido acaba
            1 -> findMatch("México", "República Checa")?.let { (it.homeGoals?:0) > 0 && (it.awayGoals?:0) > 0 }
            2 -> findMatch("Suiza", "Canadá")?.let { kotlin.math.abs((it.homeGoals?:0) - (it.awayGoals?:0)) == 1 }
            3 -> findMatch("Brasil", "Marruecos")?.let { (it.homeGoals?:0) + (it.awayGoals?:0) >= 3 }
            4 -> findMatch("Estados Unidos", "Turquía")?.let { (it.homeGoals?:0) > 0 && (it.awayGoals?:0) > 0 }
            5 -> null // necesita datos de goles por parte
            6 -> findMatch("Países Bajos", "Suecia")?.let { (it.homeGoals?:0) + (it.awayGoals?:0) >= 2 }
            7 -> findMatch("Bélgica", "Egipto")?.let {
                val h = it.homeGoals ?: 0; val a = it.awayGoals ?: 0
                if (matchName(it.homeTeam, "Bélgica")) h > a && a > 0 else a > h && h > 0
            }
            8 -> null // necesita timeline de goles
            9 -> findMatch("Francia", "Senegal")?.let { (it.homeGoals?:0) > 0 && (it.awayGoals?:0) > 0 }
            10 -> findMatch("Argentina", "Austria")?.let {
                if (matchName(it.homeTeam, "Argentina")) (it.homeGoals?:0) - (it.awayGoals?:0) >= 2
                else (it.awayGoals?:0) - (it.homeGoals?:0) >= 2
            }
            11 -> findMatch("Portugal", "Colombia")?.let { (it.homeGoals?:0) + (it.awayGoals?:0) <= 3 }
            12 -> findMatch("Inglaterra", "Croacia")?.let { kotlin.math.abs((it.homeGoals?:0)-(it.awayGoals?:0)) == 2 }

            // Q13-26: Clasificación de grupos → solo cuando todos los grupos terminan (72 partidos)
            13 -> if (groupsDone) !qualifiedFromGroup("Francia") else null
            14 -> if (groupsDone) teamPoints("Irak") > teamPoints("Haití") else null
            15 -> if (groupsDone) qualifiedFromGroup("Alemania") else null
            16 -> null
            17 -> if (groupsDone) qualifiedFromGroup("Curazao") else null
            18 -> if (groupsDone) teamPoints("Brasil") > teamPoints("Argentina") else null
            19 -> {
                val found = finished.any { (it.homeYellowCards?:0)+(it.awayYellowCards?:0)+(it.homeRedCards?:0)+(it.awayRedCards?:0) > 8 }
                if (found) true else if (allDone) false else null
            }
            20 -> if (groupsDone) standings.any { (_,s) -> s.points>0 && s.wins==0 && s.position in 1..3 } else null
            21 -> if (groupsDone) teamPosition("Noruega") == 2 else null
            22 -> if (allDone) false else null // own goals not tracked, can only answer FALSE at end
            23 -> {
                val ausWins = groupFinished.count {
                    (it.homeTeam=="Australia" && (it.homeGoals?:0) > (it.awayGoals?:0)) ||
                    (it.awayTeam=="Australia" && (it.awayGoals?:0) > (it.homeGoals?:0))
                }
                if (ausWins > 0) true else if (groupsDone) false else null
            }
            24 -> if (groupsDone) teamPoints("Colombia") > teamPoints("Marruecos") else null
            25 -> null
            26 -> if (groupsDone) qualifiedFromGroup("Panamá") else null

            // Q27-50: Torneo completo → TRUE si ya ocurrió, FALSE solo al final
            27 -> if (allDone) null else null // necesita datos de goleadores
            28 -> null; 29 -> null; 30 -> null
            31 -> {
                val found = finished.any {
                    val h=it.homeGoals?:0; val a=it.awayGoals?:0
                    (h>a && (it.homeRedCards?:0)>0) || (a>h && (it.awayRedCards?:0)>0)
                }
                if (found) true else if (allDone) false else null
            }
            32 -> {
                val hasSF = knockout.any { it.knockoutRound=="Semifinales" && it.homeGoals!=null }
                if (hasSF) knockout.any { it.knockoutRound=="Semifinales" && (it.homeTeam=="España"||it.awayTeam=="España") && it.homeGoals!=null } else null
            }
            33 -> null; 34 -> null; 35 -> null
            36 -> { if (totalRedCards>=4) true else if (allDone) false else null }
            37 -> null; 38 -> null; 39 -> null; 40 -> null; 41 -> null; 42 -> null
            43 -> null; 44 -> null; 45 -> null; 46 -> null; 47 -> null; 48 -> null
            49 -> null; 50 -> null
            else -> null
        }
    }

    data class TeamStanding(val points: Int, val position: Int, val wins: Int)

    private fun computeStandings(matches: List<MatchEntity>): Map<String, TeamStanding> {
        val teamData = mutableMapOf<String, Triple<Int, Int, Int>>()
        matches.forEach { m ->
            val h = m.homeGoals ?: return@forEach; val a = m.awayGoals ?: return@forEach
            val home = teamData.getOrPut(m.homeTeam) { Triple(0,0,0) }
            val away = teamData.getOrPut(m.awayTeam) { Triple(0,0,0) }
            when {
                h > a -> { teamData[m.homeTeam]=Triple(home.first+3, home.second+(h-a), home.third+1); teamData[m.awayTeam]=Triple(away.first, away.second+(a-h), away.third) }
                h < a -> { teamData[m.awayTeam]=Triple(away.first+3, away.second+(a-h), away.third+1); teamData[m.homeTeam]=Triple(home.first, home.second+(h-a), home.third) }
                else -> { teamData[m.homeTeam]=Triple(home.first+1, home.second, home.third); teamData[m.awayTeam]=Triple(away.first+1, away.second, away.third) }
            }
        }
        val groups = matches.groupBy { it.groupName }
        val result = mutableMapOf<String, TeamStanding>()
        groups.forEach { (_, gms) ->
            val teams = gms.flatMap { listOf(it.homeTeam, it.awayTeam) }.distinct()
            val sorted = teams.sortedByDescending { t -> val (p,g,_)=teamData[t]?:Triple(0,0,0); p*1000+g }
            sorted.forEachIndexed { idx, team -> val (p,_,w)=teamData[team]?:Triple(0,0,0); result[team]=TeamStanding(p,idx+1,w) }
        }
        return result
    }

    private fun normalize(name: String) = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").replace(" ", "").lowercase()
    private fun matchName(a: String, b: String) = normalize(a) == normalize(b)
}
