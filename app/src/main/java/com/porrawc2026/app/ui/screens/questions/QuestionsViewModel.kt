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
                val allMatches = repository.getAllMatches().first()
                val groupMatches = allMatches.filter { !it.isKnockout && it.id < 900 }
                val finished = allMatches.filter { it.homeGoals != null && it.awayGoals != null }
                val groupFinished = groupMatches.filter { it.homeGoals != null && it.awayGoals != null }
                val knockout = allMatches.filter { it.isKnockout }
                val standings = computeStandings(groupFinished)

                val totalRedCards = finished.sumOf { (it.homeRedCards?:0) + (it.awayRedCards?:0) }
                val allDone = finished.size >= 104
                val groupsDone = groupFinished.size >= 72

                var resolved = 0
                val allQuestions = repository.getAllQuestions().first()

                allQuestions.forEach { q ->
                    val result: Boolean? = when (q.id) {
                        1 -> findMatch(finished, "México", "República Checa")?.let { (it.homeGoals?:0) > 0 && (it.awayGoals?:0) > 0 }
                        2 -> findMatch(finished, "Suiza", "Canadá")?.let { kotlin.math.abs((it.homeGoals?:0)-(it.awayGoals?:0)) == 1 }
                        3 -> findMatch(finished, "Brasil", "Marruecos")?.let { (it.homeGoals?:0)+(it.awayGoals?:0) >= 3 }
                        4 -> findMatch(finished, "Estados Unidos", "Turquía")?.let { (it.homeGoals?:0) > 0 && (it.awayGoals?:0) > 0 }
                        5 -> null
                        6 -> findMatch(finished, "Países Bajos", "Suecia")?.let { (it.homeGoals?:0)+(it.awayGoals?:0) >= 2 }
                        7 -> findMatch(finished, "Bélgica", "Egipto")?.let {
                            val h=it.homeGoals?:0; val a=it.awayGoals?:0
                            if (matchName(it.homeTeam,"Bélgica")) h>a && a>0 else a>h && h>0
                        }
                        8 -> null; 9 -> findMatch(finished, "Francia", "Senegal")?.let { (it.homeGoals?:0)>0 && (it.awayGoals?:0)>0 }
                        10 -> findMatch(finished, "Argentina", "Austria")?.let {
                            if (matchName(it.homeTeam,"Argentina")) (it.homeGoals?:0)-(it.awayGoals?:0)>=2 else (it.awayGoals?:0)-(it.homeGoals?:0)>=2
                        }
                        11 -> findMatch(finished, "Portugal", "Colombia")?.let { (it.homeGoals?:0)+(it.awayGoals?:0) <= 3 }
                        12 -> findMatch(finished, "Inglaterra", "Croacia")?.let { kotlin.math.abs((it.homeGoals?:0)-(it.awayGoals?:0)) == 2 }
                        13 -> if(groupsDone) !qualified(standings,"Francia") else null
                        14 -> if(groupsDone) pts(standings,"Irak") > pts(standings,"Haití") else null
                        15 -> if(groupsDone) qualified(standings,"Alemania") else null
                        16 -> null
                        17 -> if(groupsDone) qualified(standings,"Curazao") else null
                        18 -> if(groupsDone) pts(standings,"Brasil") > pts(standings,"Argentina") else null
                        19 -> { val f = finished.any { (it.homeYellowCards?:0)+(it.awayYellowCards?:0)+(it.homeRedCards?:0)+(it.awayRedCards?:0) > 8 }; if(f) true else if(allDone) false else null }
                        20 -> if(groupsDone) standings.any { (_,s) -> s.points>0 && s.wins==0 && s.position in 1..3 } else null
                        21 -> if(groupsDone) pos(standings,"Noruega") == 2 else null
                        22 -> if(allDone) false else null
                        23 -> {
                            val w = groupFinished.count { (it.homeTeam=="Australia" && (it.homeGoals?:0)>(it.awayGoals?:0)) || (it.awayTeam=="Australia" && (it.awayGoals?:0)>(it.homeGoals?:0)) }
                            if(w>0) true else if(groupsDone) false else null
                        }
                        24 -> if(groupsDone) pts(standings,"Colombia") > pts(standings,"Marruecos") else null
                        25 -> null
                        26 -> if(groupsDone) qualified(standings,"Panamá") else null
                        27 -> null; 28 -> null; 29 -> null; 30 -> null
                        31 -> { val f = finished.any { val h=it.homeGoals?:0; val a=it.awayGoals?:0; (h>a && (it.homeRedCards?:0)>0) || (a>h && (it.awayRedCards?:0)>0) }; if(f) true else if(allDone) false else null }
                        32 -> null; 33 -> false // 4 corners opening match
                        34 -> null; 35 -> null
                        36 -> { if(totalRedCards>=4) true else if(allDone) false else null }
                        37 -> null; 38 -> null; 39 -> null; 40 -> null; 41 -> null; 42 -> null; 43 -> null; 44 -> null
                        45 -> null; 46 -> null; 47 -> null; 48 -> null; 49 -> null; 50 -> null
                        else -> null
                    }
                    if (result != null && q.correctAnswer != result) {
                        repository.updateQuestionPrediction(q.copy(correctAnswer = result, pointsEarned = if(q.predictedAnswer == result) 20 else 0))
                        resolved++
                    }
                }
                val pts = repository.calculateTotalPoints()
                _evalMessage.value = if (resolved > 0) "$resolved preguntas resueltas ($pts pts)" else "Sin cambios"
            } catch (e: Exception) {
                _evalMessage.value = "Error: ${e.message}"
            } finally { _isEvaluating.value = false }
        }
    }

    private fun findMatch(finished: List<MatchEntity>, home: String, away: String) = finished.firstOrNull { matchName(it.homeTeam, home) && matchName(it.awayTeam, away) }
    private fun pts(s: Map<String, TeamStanding>, t: String) = s[t]?.points ?: 0
    private fun pos(s: Map<String, TeamStanding>, t: String) = s[t]?.position ?: 0
    private fun qualified(s: Map<String, TeamStanding>, t: String) = pos(s, t) in 1..3

    data class TeamStanding(val points: Int, val position: Int, val wins: Int)

    private fun computeStandings(matches: List<MatchEntity>): Map<String, TeamStanding> {
        val td = mutableMapOf<String, Triple<Int,Int,Int>>()
        matches.forEach { m ->
            val h = m.homeGoals ?: return@forEach; val a = m.awayGoals ?: return@forEach
            val ho = td.getOrPut(m.homeTeam) { Triple(0,0,0) }
            val aw = td.getOrPut(m.awayTeam) { Triple(0,0,0) }
            if (h > a) {
                td[m.homeTeam] = Triple(ho.first+3, ho.second+(h-a), ho.third+1)
                td[m.awayTeam] = Triple(aw.first, aw.second+(a-h), aw.third)
            } else if (h < a) {
                td[m.awayTeam] = Triple(aw.first+3, aw.second+(a-h), aw.third+1)
                td[m.homeTeam] = Triple(ho.first, ho.second+(h-a), ho.third)
            } else {
                td[m.homeTeam] = Triple(ho.first+1, ho.second, ho.third)
                td[m.awayTeam] = Triple(aw.first+1, aw.second, aw.third)
            }
        }
        val r = mutableMapOf<String, TeamStanding>()
        matches.groupBy { it.groupName }.forEach { (_, g) ->
            val teams = g.flatMap { listOf(it.homeTeam, it.awayTeam) }.distinct()
            val sorted = teams.sortedByDescending { t ->
                val (p, gd, _) = td[t] ?: Triple(0,0,0); p * 1000 + gd
            }
            sorted.forEachIndexed { i, t ->
                val (p, _, w) = td[t] ?: Triple(0,0,0); r[t] = TeamStanding(p, i+1, w)
            }
        }
        return r
    }

    private fun normalize(name: String) = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"),"").replace(" ","").lowercase()
    private fun matchName(a: String, b: String) = normalize(a) == normalize(b)
}
