package com.porrawc2026.app.ui.screens.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.QuestionEntity
import com.porrawc2026.app.data.remote.LiveScorer
import com.porrawc2026.app.data.repository.PorraRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

    private val _pendingManualAnswer = MutableStateFlow<Pair<QuestionEntity, Boolean>?>(null)
    val pendingManualAnswer: StateFlow<Pair<QuestionEntity, Boolean>?> = _pendingManualAnswer.asStateFlow()

    private val gson = Gson()
    private val scorerListType = object : TypeToken<List<LiveScorer>>() {}.type

    fun clearMessage() { _evalMessage.value = null }

    fun showManualDialog(question: QuestionEntity, answer: Boolean) {
        _pendingManualAnswer.value = question to answer
    }

    fun confirmManualAnswer() {
        val (q, answer) = _pendingManualAnswer.value ?: return
        _pendingManualAnswer.value = null
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateQuestionPrediction(q.copy(correctAnswer = answer, pointsEarned = if(q.predictedAnswer == answer) 20 else 0))
            val pts = repository.calculateTotalPoints()
            _evalMessage.value = "Pregunta ${q.id} resuelta ($pts pts)"
        }
    }

    fun cancelManualAnswer() {
        _pendingManualAnswer.value = null
    }

    private fun parseScorers(json: String?): List<LiveScorer> {
        if (json == null) return emptyList()
        return try { gson.fromJson(json, scorerListType) ?: emptyList() } catch (_: Exception) { emptyList() }
    }

    fun evaluateQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            _isEvaluating.value = true
            runCatching {
                val allMatches = repository.getAllMatches().first()
                val groupMatches = allMatches.filter { !it.isKnockout && it.id < 900 }
                val finished = allMatches.filter { it.homeGoals != null && it.awayGoals != null }
                val groupFinished = groupMatches.filter { it.homeGoals != null && it.awayGoals != null }
                val knockout = allMatches.filter { it.isKnockout }
                val standings = computeStandings(groupFinished)

                val totalRedCards = finished.sumOf { (it.homeRedCards?:0) + (it.awayRedCards?:0) }
                val totalOwnGoals = finished.sumOf { match ->
                    val all = parseScorers(match.homeScorers) + parseScorers(match.awayScorers)
                    all.count { s ->
                        val n = s.playerName.uppercase()
                        "(OG)" in n || "(O.G.)" in n || "(O.G)" in n
                    }
                }
                val allDone = finished.size >= 104
                val groupsDone = groupFinished.size >= 72

                var resolved = 0
                val allQuestions = repository.getAllQuestions().first()

                allQuestions.forEach { q ->
                    val advancement = computeAdvancement(finished, standings)
                    val result: Boolean? = when (q.id) {
                        1 -> findMatch(finished, "México", "República Checa")?.let { (it.homeGoals?:0) > 0 && (it.awayGoals?:0) > 0 }
                        2 -> findMatch(finished, "Suiza", "Canadá")?.let { kotlin.math.abs((it.homeGoals?:0)-(it.awayGoals?:0)) == 1 }
                        3 -> findMatch(finished, "Brasil", "Marruecos")?.let { (it.homeGoals?:0)+(it.awayGoals?:0) >= 3 }
                        4 -> findMatch(finished, "Estados Unidos", "Turquía")?.let { (it.homeGoals?:0) > 0 && (it.awayGoals?:0) > 0 }
                        5 -> findMatch(finished, "Alemania", "Ecuador")?.let { match ->
                            val scorers = if (matchName(match.homeTeam, "Alemania")) parseScorers(match.homeScorers) else parseScorers(match.awayScorers)
                            if (scorers.isEmpty()) return@let null
                            val minutes = scorers.map { it.minute }
                            minutes.any { it <= 45 } && minutes.any { it > 45 }
                        }
                        6 -> findMatch(finished, "Países Bajos", "Suecia")?.let { (it.homeGoals?:0)+(it.awayGoals?:0) >= 2 }
                        7 -> findMatch(finished, "Bélgica", "Egipto")?.let {
                            val h=it.homeGoals?:0; val a=it.awayGoals?:0
                            if (matchName(it.homeTeam,"Bélgica")) h>a && a>0 else a>h && h>0
                        }
                        8 -> findMatch(finished, "España", "Uruguay")?.let { match ->
                            val homeScorers = parseScorers(match.homeScorers)
                            val awayScorers = parseScorers(match.awayScorers)
                            val spainIsHome = matchName(match.homeTeam, "España")
                            val allGoals = mutableListOf<Pair<Int, Boolean>>()
                            (if (spainIsHome) homeScorers else awayScorers).forEach { allGoals.add(it.minute to true) }
                            (if (spainIsHome) awayScorers else homeScorers).forEach { allGoals.add(it.minute to false) }
                            if (allGoals.isEmpty()) false else allGoals.minBy { it.first }.second
                        }
                            9 -> findMatch(finished, "Francia", "Senegal")?.let { (it.homeGoals?:0)>0 && (it.awayGoals?:0)>0 }
                        10 -> findMatch(finished, "Argentina", "Austria")?.let {
                            if (matchName(it.homeTeam,"Argentina")) (it.homeGoals?:0)-(it.awayGoals?:0)>=2 else (it.awayGoals?:0)-(it.homeGoals?:0)>=2
                        }
                        11 -> findMatch(finished, "Portugal", "Colombia")?.let { (it.homeGoals?:0)+(it.awayGoals?:0) <= 3 }
                        12 -> findMatch(finished, "Inglaterra", "Croacia")?.let { kotlin.math.abs((it.homeGoals?:0)-(it.awayGoals?:0)) == 2 }
                        13 -> if(groupsDone) !qualified(standings,"Francia") else null
                        14 -> if(groupsDone) pts(standings,"Irak") > pts(standings,"Haití") else null
                        15 -> if(groupsDone) qualified(standings,"Alemania") else null
                        16 -> {
                            val f = finished.firstOrNull { hasComebackWin(it) }
                            if (f != null) true else if (allDone) false else null
                        }
                        17 -> if(groupsDone) qualified(standings,"Curazao") else null
                        18 -> if(groupsDone) pts(standings,"Brasil") > pts(standings,"Argentina") else null
                        19 -> { val f = finished.any { (it.homeYellowCards?:0)+(it.awayYellowCards?:0)+(it.homeRedCards?:0)+(it.awayRedCards?:0) > 8 }; if(f) true else if(allDone) false else null }
                        20 -> if(groupsDone) standings.any { (_,s) -> s.points>0 && s.wins==0 && s.position in 1..3 } else null
                        21 -> if(groupsDone) pos(standings,"Noruega") == 2 else null
                        22 -> if (totalOwnGoals >= 3) true else if (allDone) false else null
                        23 -> {
                            val w = groupFinished.count { (it.homeTeam=="Australia" && (it.homeGoals?:0)>(it.awayGoals?:0)) || (it.awayTeam=="Australia" && (it.awayGoals?:0)>(it.homeGoals?:0)) }
                            if(w>0) true else if(groupsDone) false else null
                        }
                        24 -> if(groupsDone) pts(standings,"Colombia") > pts(standings,"Marruecos") else null
                        25 -> {
                            val f = finished.any { (it.homeMissedPenalties + it.awayMissedPenalties) >= 2 }
                            if (f) true else if (allDone) false else null
                        }
                        26 -> if(groupsDone) qualified(standings,"Panamá") else null
                        27 -> {
                            val goalCounts = mutableMapOf<String, Int>()
                            finished.forEach { match ->
                                val all = parseScorers(match.homeScorers) + parseScorers(match.awayScorers)
                                all.forEach { s -> goalCounts.merge(s.playerName, 1) { a, b -> a + b } }
                            }
                            val maxGoals = goalCounts.maxOfOrNull { it.value } ?: 0
                            if (maxGoals >= 8) true else if (allDone) false else null
                        }
                        28 -> {
                            val koET = finished.count { m ->
                                m.isKnockout && (parseScorers(m.homeScorers) + parseScorers(m.awayScorers)).any { it.minute > 100 }
                            }
                            if (koET >= 4) true else if (allDone) false else null
                        }
                        29 -> {
                            val koPens = finished.count { m ->
                                m.isKnockout && m.homeGoals != null && m.awayGoals != null && m.homeGoals == m.awayGoals
                            }
                            if (koPens >= 3) true else if (allDone) false else null
                        }
                        30 -> null
                        31 -> { val f = finished.any { val h=it.homeGoals?:0; val a=it.awayGoals?:0; (h>a && (it.homeRedCards?:0)>0) || (a>h && (it.awayRedCards?:0)>0) }; if(f) true else if(allDone) false else null }
                        32 -> {
                            val spainRound = advancement.entries.firstOrNull { matchName(it.key, "España") }?.value
                            if (spainRound == "Semifinales" || spainRound == "Final" || spainRound == "Campeón") true
                            else if (allDone) false else if (spainRound != null) false else null
                        }
                        33 -> false
                        34 -> {
                            val african = listOf("Marruecos", "Senegal", "Túnez", "Argelia", "Egipto", "Nigeria", "Camerún", "Ghana", "Costa de Marfil", "Mali", "Burkina Faso", "Sudáfrica", "RD Congo")
                            val inR16 = advancement.count { (team, round) ->
                                round == "Dieciseisavos" && african.any { matchName(team, it) }
                            }
                            if (inR16 >= 3) true else if (allDone) false else if (groupsDone) inR16 >= 3 else null
                        }
                        35 -> {
                            val champion = advancement.entries.firstOrNull { it.value == "Campeón" }?.key
                            if (champion != null) {
                                val goalCounts = mutableMapOf<String, Int>()
                                finished.forEach { match ->
                                    val all = parseScorers(match.homeScorers) + parseScorers(match.awayScorers)
                                    all.forEach { s -> goalCounts.merge(s.playerName, 1) { a, b -> a + b } }
                                }
                                val championGoals = goalCounts.entries.firstOrNull { matchName(it.key, champion) }?.value ?: 0
                                val maxGoals = goalCounts.maxOfOrNull { it.value } ?: 0
                                championGoals >= maxGoals
                            } else if (allDone) false else null
                        }
                        37 -> {
                            val totalHeaded = finished.sumOf { (it.homeHeadedGoals + it.awayHeadedGoals) }
                            if (totalHeaded >= 10) true else if (allDone) false else null
                        }
                        38 -> {
                            val european = listOf("Alemania", "Francia", "Inglaterra", "España", "Italia", "Países Bajos", "Portugal", "Bélgica", "Croacia", "Suiza", "Suecia", "Dinamarca", "Noruega", "Turquía", "Polonia", "Ucrania", "Austria", "República Checa", "Serbia", "Escocia", "Gales", "Irlanda", "Hungría", "Rumanía", "Eslovaquia", "Eslovenia", "Grecia", "Rusia")
                            val inQF = advancement.count { (team, round) ->
                                (round == "Cuartos" || round == "Semifinales" || round == "Final" || round == "Campeón") && european.any { matchName(team, it) }
                            }
                            if (inQF >= 4) true else if (allDone) false else null
                        }
                        39 -> {
                            val f = finished.firstOrNull { it.knockoutRound == "Dieciseisavos" && (it.homeGoals ?: 0) + (it.awayGoals ?: 0) >= 7 }
                            if (f != null) true else if (allDone) false else null
                        }
                        40 -> {
                            val tp = finished.firstOrNull { it.knockoutRound == "3er puesto" }
                            if (tp != null) (tp.homeGoals ?: 0) + (tp.awayGoals ?: 0) >= 4 else if (allDone) false else null
                        }
                        41 -> {
                            val champion = advancement.entries.firstOrNull { it.value == "Campeón" }?.key
                            if (champion != null) {
                                val champMatches = finished.filter { matchName(it.homeTeam, champion) || matchName(it.awayTeam, champion) }
                                champMatches.none { m ->
                                    if (matchName(m.homeTeam, champion)) (m.awayGoals ?: 0) > (m.homeGoals ?: 0)
                                    else (m.homeGoals ?: 0) > (m.awayGoals ?: 0)
                                }
                            } else if (allDone) false else null
                        }
                        42 -> {
                            val f = finished.firstOrNull { m ->
                                if (m.knockoutRound != "Octavos") return@firstOrNull false
                                val scorers = parseScorers(m.homeScorers) + parseScorers(m.awayScorers)
                                scorers.groupBy { it.playerName }.any { it.value.size >= 2 }
                            }
                            if (f != null) true else if (allDone) false else null
                        }
                        43 -> {
                            val european = listOf("Alemania", "Francia", "Inglaterra", "España", "Italia", "Países Bajos", "Portugal", "Bélgica", "Croacia", "Suiza", "Suecia", "Dinamarca", "Noruega", "Turquía", "Polonia", "Ucrania", "Austria", "República Checa", "Serbia", "Escocia", "Gales", "Irlanda", "Hungría", "Rumanía", "Eslovaquia", "Eslovenia", "Grecia", "Rusia")
                            val inSemis = advancement.count { (team, round) ->
                                (round == "Semifinales" || round == "Final" || round == "Campeón") && european.any { matchName(team, it) }
                            }
                            if (inSemis >= 1) true else if (allDone) false else null
                        }
                        44 -> {
                            val usaRound = advancement.entries.firstOrNull { matchName(it.key, "Estados Unidos") }?.value
                            if (usaRound == "Dieciseisavos" || usaRound == "Octavos" || usaRound == "Cuartos" || usaRound == "Semifinales" || usaRound == "Final" || usaRound == "Campeón") true
                            else if (allDone) false else if (groupsDone && usaRound == null) false else null
                        }
                        45 -> {
                            val f = finished.any { m ->
                                m.knockoutRound == "Cuartos" && ((m.homeYellowCards ?: 0) + (m.awayYellowCards ?: 0) + (m.homeRedCards ?: 0) + (m.awayRedCards ?: 0)) >= 6
                            }
                            if (f) true else if (allDone) false else null
                        }
                        46 -> {
                            val final = finished.firstOrNull { it.knockoutRound == "Final" }
                            if (final != null) (final.homeRedCards ?: 0) + (final.awayRedCards ?: 0) >= 1 else if (allDone) false else null
                        }
                        47 -> {
                            val f = finished.firstOrNull { it.isKnockout && it.knockoutRound == "Semifinales" && it.hasSubGoal }
                            if (f != null) true else if (allDone) false else null
                        }
                        48 -> {
                            val canRoundNum = advancement.entries.firstOrNull { matchName(it.key, "Canadá") }?.let { roundNum(it.value) }
                            val mexRoundNum = advancement.entries.firstOrNull { matchName(it.key, "México") }?.let { roundNum(it.value) }
                            if (canRoundNum != null && mexRoundNum != null) canRoundNum > mexRoundNum
                            else if (allDone) false else null
                        }
                        49 -> {
                            val final = finished.firstOrNull { it.knockoutRound == "Final" }
                            if (final != null) (final.homeMissedPenalties + final.awayMissedPenalties) > 0 else if (allDone) false else null
                        }
                        else -> null
                    }
                    if (result != null && q.correctAnswer != result) {
                        repository.updateQuestionPrediction(q.copy(correctAnswer = result, pointsEarned = if(q.predictedAnswer == result) 20 else 0))
                        resolved++
                    }
                }
                val pts = repository.calculateTotalPoints()
                _evalMessage.value = if (resolved > 0) "$resolved preguntas resueltas ($pts pts)" else "Sin cambios"
            }.onFailure { e ->
                _evalMessage.value = "Error: ${e.message}"
            }
            _isEvaluating.value = false
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

    private fun hasComebackWin(match: MatchEntity): Boolean {
        val homeGoals = parseScorers(match.homeScorers)
        val awayGoals = parseScorers(match.awayScorers)
        if (homeGoals.isEmpty() && awayGoals.isEmpty()) return false

        val h = match.homeGoals ?: return false
        val a = match.awayGoals ?: return false
        if (h == a) return false

        val winnerIsHome = h > a
        val winnerGoals = if (winnerIsHome) h else a
        val loserGoals = if (winnerIsHome) a else h
        if (winnerGoals < 3 || loserGoals < 2) return false

        val events = mutableListOf<Pair<Int, Boolean>>()
        if (winnerIsHome) {
            homeGoals.forEach { events.add(it.minute to true) }
            awayGoals.forEach { events.add(it.minute to false) }
        } else {
            awayGoals.forEach { events.add(it.minute to true) }
            homeGoals.forEach { events.add(it.minute to false) }
        }
        events.sortBy { it.first }

        var wScore = 0
        var lScore = 0
        var maxLoserLeadAfter45 = 0

        for ((minute, isWinner) in events) {
            if (isWinner) wScore++ else lScore++
            if (minute > 45) {
                maxLoserLeadAfter45 = maxOf(maxLoserLeadAfter45, lScore - wScore)
            }
        }
        return maxLoserLeadAfter45 >= 2
    }

    private fun computeAdvancement(finished: List<MatchEntity>, standings: Map<String, TeamStanding>): Map<String, String> {
        val result = mutableMapOf<String, String>()

        // Groups: top 3 per group reach Dieciseisavos
        for ((team, st) in standings) {
            if (st.position in 1..3) result[team] = "Dieciseisavos"
        }

        // Process KO rounds in order
        val koRounds = listOf("Dieciseisavos", "Octavos", "Cuartos", "Semifinales", "Final")
        val nextRound = mapOf("Dieciseisavos" to "Octavos", "Octavos" to "Cuartos", "Cuartos" to "Semifinales", "Semifinales" to "Final", "Final" to "Campeón")

        for (round in koRounds) {
            for (match in finished.filter { it.knockoutRound == round }) {
                val nr = nextRound[round] ?: continue
                val winner = match.winnerTeam
                if (winner != null) {
                    result[winner] = nr
                } else if (match.homeGoals != null && match.awayGoals != null && match.homeGoals != match.awayGoals) {
                    val w = if (match.homeGoals!! > match.awayGoals!!) match.homeTeam else match.awayTeam
                    result[w] = nr
                }
            }
        }
        return result
    }

    private fun roundNum(round: String): Int = when (round) {
        "Dieciseisavos" -> 1; "Octavos" -> 2; "Cuartos" -> 3
        "Semifinales" -> 4; "Final" -> 5; "Campeón" -> 6
        else -> 0
    }
}
