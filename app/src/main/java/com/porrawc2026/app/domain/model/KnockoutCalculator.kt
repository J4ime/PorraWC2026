package com.porrawc2026.app.domain.model

import android.util.Log
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity

object KnockoutCalculator {

    private val KO_ROUNDS_IN_ORDER = listOf("Dieciseisavos", "Octavos", "Cuartos", "Semifinales", "3er puesto", "Final")

    fun extractRefMatchId(teamName: String): Int? {
        val ganador = when {
            teamName.startsWith("Ganador ") -> teamName.removePrefix("Ganador ").trim().toIntOrNull()
            teamName.startsWith("W") -> teamName.removePrefix("W").trim().toIntOrNull()
            else -> null
        }
        if (ganador != null) return ganador
        return when {
            teamName.startsWith("Perdedor ") -> teamName.removePrefix("Perdedor ").trim().toIntOrNull()
            teamName.startsWith("L") -> teamName.removePrefix("L").trim().toIntOrNull()
            else -> null
        }
    }

    fun getWinnerSimple(match: MatchEntity): String? {
        match.winnerTeam?.let { return TeamNameNormalizer.enToEs(it) }
        val hg = match.homeGoals ?: return null
        val ag = match.awayGoals ?: return null
        if (hg > ag) return match.homeTeam.takeIf { it.isNotBlank() }
        if (ag > hg) return match.awayTeam.takeIf { it.isNotBlank() }
        if (match.homeShootoutScore > match.awayShootoutScore) return match.homeTeam.takeIf { it.isNotBlank() }
        if (match.awayShootoutScore > match.homeShootoutScore) return match.awayTeam.takeIf { it.isNotBlank() }
        return null
    }

    private fun resolvePredictionTeams(
        ref: String, matches: List<MatchEntity>,
        visited: MutableSet<Int> = mutableSetOf()
    ): Set<String> {
        if (ref.isBlank()) return emptySet()
        if (ref.startsWith("Perdedor ") || ref.startsWith("L ")) return setOf(ref)
        val matchId = extractRefMatchId(ref) ?: return setOf(ref)
        if (matchId in visited) return setOf(ref)
        visited.add(matchId)
        val sourceMatch = matches.firstOrNull { it.id == matchId } ?: return setOf(ref)
        val homeTeams = resolvePredictionTeams(sourceMatch.homeTeam, matches, visited)
        val awayTeams = resolvePredictionTeams(sourceMatch.awayTeam, matches, visited)
        val all = homeTeams + awayTeams
        if (all.isEmpty()) return setOf(ref)
        return all
    }

    fun computeCrossRoundPoints(
        predictions: List<KnockoutPredictionEntity>,
        matches: List<MatchEntity>
    ): Pair<Map<Int, Int>, Map<Int, Int>> {
        val displayMap = mutableMapOf<Int, Int>()
        val dbMap = mutableMapOf<Int, Int>()
        val ptsPerRound = mapOf(
            "Dieciseisavos" to 20, "Octavos" to 40, "Cuartos" to 80,
            "Semifinales" to 160, "3er puesto" to 200, "Final" to 500
        )

        for (match in matches.filter { it.isKnockout && it.id in 73..104 }) {
            val home = match.homeTeam.takeIf { it.isNotBlank() } ?: continue
            val away = match.awayTeam.takeIf { it.isNotBlank() } ?: continue

            val nextRound = when (match.knockoutRound) {
                "Dieciseisavos" -> "Octavos"
                "Octavos" -> "Cuartos"
                "Cuartos" -> "Semifinales"
                "Semifinales" -> "3er puesto"
                else -> null
            }

            val nextMatches = matches.filter { m ->
                m.isKnockout && (
                    extractRefMatchId(m.homeTeam) == match.id ||
                    extractRefMatchId(m.awayTeam) == match.id ||
                    (m.knockoutRound != null && (m.knockoutRound == nextRound ||
                     (match.knockoutRound == "Semifinales" && m.knockoutRound == "Final")) &&
                     (TeamNameNormalizer.matches(m.homeTeam, home) || TeamNameNormalizer.matches(m.awayTeam, home) ||
                      TeamNameNormalizer.matches(m.homeTeam, away) || TeamNameNormalizer.matches(m.awayTeam, away)))
                )
            }
            if (nextMatches.isEmpty()) continue

            for (nextMatch in nextMatches) {
                val round = nextMatch.knockoutRound ?: continue
                val pts = ptsPerRound[round] ?: continue

                val pred = predictions.firstOrNull { it.matchNumber == nextMatch.id } ?: continue
                val predHomeTeams = resolvePredictionTeams(pred.homeTeamRef, matches)
                val predAwayTeams = resolvePredictionTeams(pred.awayTeamRef, matches)
                val allPredTeams = predHomeTeams + predAwayTeams
                if (allPredTeams.isEmpty() || allPredTeams.all { it.isBlank() || it.startsWith("W") || it.startsWith("L") }) continue

                val homeInPred = allPredTeams.any { TeamNameNormalizer.matches(it, home) }
                val awayInPred = allPredTeams.any { TeamNameNormalizer.matches(it, away) }

                val count = (if (homeInPred) 1 else 0) + (if (awayInPred) 1 else 0)
                when (count) {
                    2 -> {
                        displayMap[match.id] = (displayMap[match.id] ?: 0) + pts
                        dbMap[nextMatch.id] = (dbMap[nextMatch.id] ?: 0) + pts
                    }
                    1 -> {
                        val winner = getWinnerSimple(match)
                        if (winner != null) {
                            if (allPredTeams.any { TeamNameNormalizer.matches(winner, it) }) {
                                displayMap[match.id] = (displayMap[match.id] ?: 0) + pts
                                dbMap[nextMatch.id] = (dbMap[nextMatch.id] ?: 0) + pts
                            } else {
                                displayMap.putIfAbsent(match.id, 0)
                            }
                        }
                    }
                    0 -> {
                        displayMap.putIfAbsent(match.id, 0)
                    }
                }
            }
        }

        return Pair(displayMap, dbMap)
    }

    fun isRef(teamName: String): Boolean =
        (teamName.startsWith("Ganador ") && teamName.removePrefix("Ganador ").toIntOrNull() != null) ||
        (teamName.startsWith("Perdedor ") && teamName.removePrefix("Perdedor ").toIntOrNull() != null) ||
        (teamName.startsWith("W") && teamName.drop(1).toIntOrNull() != null) ||
        (teamName.startsWith("L") && teamName.drop(1).toIntOrNull() != null)

    fun roundLevel(round: String): Int = when (round) {
        "Dieciseisavos" -> 1; "Octavos" -> 2; "Cuartos" -> 3; "Semifinales" -> 4
        "3er puesto" -> 5; "Final" -> 6; "Campeón" -> 7; else -> 0
    }

    private fun resolveKnockoutTeam(ref: String, matches: List<MatchEntity>): String? {
        val ganadorId = when {
            ref.startsWith("Ganador ") -> ref.removePrefix("Ganador ").trim().toIntOrNull()
            ref.startsWith("W") -> ref.removePrefix("W").trim().toIntOrNull()
            else -> null
        }
        if (ganadorId != null) {
            val prevMatch = matches.firstOrNull { it.id == ganadorId }
            // Fallback: if winnerTeam is null but scores exist, infer winner from goals/shootout
            return prevMatch?.winnerTeam ?: inferWinnerFromScores(prevMatch)
        }

        val perdedorId = when {
            ref.startsWith("Perdedor ") -> ref.removePrefix("Perdedor ").trim().toIntOrNull()
            ref.startsWith("L") -> ref.removePrefix("L").trim().toIntOrNull()
            else -> null
        }
        if (perdedorId != null) {
            val prevMatch = matches.firstOrNull { it.id == perdedorId } ?: return null
            val winner = prevMatch.winnerTeam ?: return null
            return if (TeamNameNormalizer.matches(prevMatch.homeTeam, winner)) prevMatch.awayTeam else prevMatch.homeTeam
        }

        return if (ref.isNotBlank()) ref else null
    }

    fun buildLiveRoundLists(matches: List<MatchEntity>): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        KO_ROUNDS_IN_ORDER.forEach { result[it] = mutableListOf() }

        for (round in KO_ROUNDS_IN_ORDER) {
            val roundMatches = matches.filter { it.isKnockout && it.knockoutRound == round }
            Log.d("KO_DEBUG", "buildLiveRoundLists round=$round matches=${roundMatches.map { it.id }}")
            for (m in roundMatches) {
                resolveKnockoutTeam(m.homeTeam, matches)?.let { team ->
                    if (result[round].orEmpty().none { TeamNameNormalizer.matches(it, team) })
                        result[round]?.add(team)
                }
                resolveKnockoutTeam(m.awayTeam, matches)?.let { team ->
                    if (result[round].orEmpty().none { TeamNameNormalizer.matches(it, team) })
                        result[round]?.add(team)
                }
            }
            Log.d("KO_DEBUG", "final $round teams: ${result[round]}")
        }

        return result
    }

    fun computePointsFromLiveLists(
        predictions: List<KnockoutPredictionEntity>,
        liveRoundLists: Map<String, List<String>>,
        matches: List<MatchEntity>
    ): Pair<Map<Int, Int>, Map<Int, Int>> {
        val pointsByMatch = mutableMapOf<Int, Int>()
        val pointsByPrediction = mutableMapOf<Int, Int>()

        val roundPoints = mapOf(
            "Dieciseisavos" to 20,
            "Octavos" to 40,
            "Cuartos" to 80,
            "Semifinales" to 160,
            "3er puesto" to 200,
            "Final" to 500
        )

        for ((round, ptsPerTeam) in roundPoints) {
            val actualTeams = liveRoundLists[round].orEmpty()
            Log.d("KO_DEBUG", "computePointsFromLiveLists round=$round actualTeams=$actualTeams (empty=${actualTeams.isEmpty()})")
            if (actualTeams.isEmpty()) continue

            for (prediction in predictions.filter { it.round == round }) {
                val match = matches.firstOrNull { it.id == prediction.matchNumber }
                if (match == null) continue
                val home = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                val away = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                var pts = 0
                if (actualTeams.any { TeamNameNormalizer.matches(it, home) }) pts += ptsPerTeam
                if (actualTeams.any { TeamNameNormalizer.matches(it, away) }) pts += ptsPerTeam
                Log.d("KO_DEBUG", "  pred match=${prediction.matchNumber} home=$home away=$away pts=$pts")
                if (pts > 0) {
                    pointsByMatch[prediction.matchNumber] = (pointsByMatch[prediction.matchNumber] ?: 0) + pts
                    pointsByPrediction[prediction.matchNumber] = (pointsByPrediction[prediction.matchNumber] ?: 0) + pts
                }
            }
        }

        Log.d("KO_DEBUG", "computePointsFromLiveLists result matchPts=$pointsByMatch predPts=$pointsByPrediction")
        return Pair(pointsByMatch, pointsByPrediction)
    }

    private fun inferWinnerFromScores(match: MatchEntity?): String? {
        if (match == null) return null
        val hg = match.homeGoals ?: return null
        val ag = match.awayGoals ?: return null
        if (hg > ag) return match.homeTeam.takeIf { it.isNotBlank() }
        if (ag > hg) return match.awayTeam.takeIf { it.isNotBlank() }
        // Draw → shootout
        if (match.homeShootoutScore > match.awayShootoutScore) return match.homeTeam.takeIf { it.isNotBlank() }
        if (match.awayShootoutScore > match.homeShootoutScore) return match.awayTeam.takeIf { it.isNotBlank() }
        return null
    }

    // ── Home screen display (cross-round + "certain only" for pending matches) ──

    private fun expandPredictionTeams(
        ref: String,
        predictions: List<KnockoutPredictionEntity>,
        visited: MutableSet<Int> = mutableSetOf()
    ): Set<String> {
        if (ref.isBlank()) return emptySet()
        val matchId = extractRefMatchId(ref) ?: return setOf(ref)
        if (matchId in visited) return setOf(ref)
        visited.add(matchId)
        val pred = predictions.firstOrNull { it.matchNumber == matchId } ?: return setOf(ref)
        val teams = mutableSetOf<String>()
        if (pred.homeTeamRef.isNotBlank()) teams += expandPredictionTeams(pred.homeTeamRef, predictions, visited)
        if (pred.awayTeamRef.isNotBlank()) teams += expandPredictionTeams(pred.awayTeamRef, predictions, visited)
        return if (teams.isEmpty()) setOf(ref) else teams
    }

    private fun getLoserSimple(match: MatchEntity): String? {
        val winner = getWinnerSimple(match) ?: return null
        return if (TeamNameNormalizer.matches(match.homeTeam, winner)) match.awayTeam
        else if (TeamNameNormalizer.matches(match.awayTeam, winner)) match.homeTeam
        else null
    }

    fun computeHomeScreenDisplayPoints(
        predictions: List<KnockoutPredictionEntity>,
        matches: List<MatchEntity>
    ): Map<Int, Int> {
        val displayMap = mutableMapOf<Int, Int>()

        // Static bracket: which source match feeds into which next match
        val bracketNext = mapOf(
            73 to 89, 74 to 89, 75 to 90, 76 to 90,
            77 to 91, 78 to 91, 79 to 92, 80 to 92,
            81 to 93, 82 to 93, 83 to 94, 84 to 94,
            85 to 95, 86 to 95, 87 to 96, 88 to 96,
            89 to 97, 90 to 97, 91 to 99, 92 to 99,
            93 to 98, 94 to 98, 95 to 100, 96 to 100,
            97 to 101, 98 to 101, 99 to 102, 100 to 102,
            101 to 104, 102 to 104
        )

        val nextPts = mapOf("Octavos" to 40, "Cuartos" to 80, "Semifinales" to 160, "Final" to 500, "3er puesto" to 200)

        for (match in matches.filter { it.isKnockout && it.id in 73..104 }) {
            val home = match.homeTeam.takeIf { it.isNotBlank() } ?: continue
            val away = match.awayTeam.takeIf { it.isNotBlank() } ?: continue
            val id = match.id
            val isPlayed = match.homeGoals != null && match.awayGoals != null
            val round = match.knockoutRound ?: continue

            when (round) {
                "Dieciseisavos", "Octavos", "Cuartos" -> {
                    val nextId = bracketNext[id] ?: continue
                    val nextMatch = matches.firstOrNull { it.id == nextId } ?: continue
                    val pts = nextPts[nextMatch.knockoutRound] ?: continue
                    val pred = predictions.firstOrNull { it.matchNumber == nextId } ?: continue

                    val predTeams = expandPredictionTeams(pred.homeTeamRef, predictions) +
                            expandPredictionTeams(pred.awayTeamRef, predictions)
                    val resolvedTeams = predTeams.filterNot { isRef(it) }.toSet()
                    if (resolvedTeams.isEmpty()) continue

                    val homeInPred = resolvedTeams.any { TeamNameNormalizer.matches(it, home) }
                    val awayInPred = resolvedTeams.any { TeamNameNormalizer.matches(it, away) }
                    val count = (if (homeInPred) 1 else 0) + (if (awayInPred) 1 else 0)

                    if (isPlayed) {
                        when (count) {
                            2 -> displayMap[id] = pts
                            1 -> {
                                val winner = getWinnerSimple(match)
                                if (winner != null && resolvedTeams.any { TeamNameNormalizer.matches(it, winner) })
                                    displayMap[id] = pts
                                else
                                    displayMap.putIfAbsent(id, 0)
                            }
                            0 -> displayMap.putIfAbsent(id, 0)
                        }
                    } else {
                        when (count) {
                            2 -> displayMap[id] = pts
                            0 -> displayMap[id] = 0
                        }
                    }
                }
                "Semifinales" -> {
                    for ((nextRound, pts) in listOf("Final" to 500, "3er puesto" to 200)) {
                        val nextPreds = predictions.filter { it.round == nextRound }
                        if (nextPreds.isEmpty()) continue

                        val resolvedPreds = nextPreds.filter { pred ->
                            val t = expandPredictionTeams(pred.homeTeamRef, predictions) +
                                    expandPredictionTeams(pred.awayTeamRef, predictions)
                            t.any { !isRef(it) }
                        }
                        if (resolvedPreds.isEmpty()) continue

                        val homeInNext = resolvedPreds.any { pred ->
                            val t = expandPredictionTeams(pred.homeTeamRef, predictions) +
                                    expandPredictionTeams(pred.awayTeamRef, predictions)
                            t.filterNot { isRef(it) }.any { TeamNameNormalizer.matches(it, home) }
                        }
                        val awayInNext = resolvedPreds.any { pred ->
                            val t = expandPredictionTeams(pred.homeTeamRef, predictions) +
                                    expandPredictionTeams(pred.awayTeamRef, predictions)
                            t.filterNot { isRef(it) }.any { TeamNameNormalizer.matches(it, away) }
                        }
                        val count = (if (homeInNext) 1 else 0) + (if (awayInNext) 1 else 0)

                        if (isPlayed) {
                            val relevantTeam = if (nextRound == "Final") getWinnerSimple(match)
                            else getLoserSimple(match)
                            when {
                                count == 2 -> displayMap[id] = (displayMap[id] ?: 0) + pts
                                count == 1 && relevantTeam != null -> {
                                    val hit = resolvedPreds.any { pred ->
                                        val t = expandPredictionTeams(pred.homeTeamRef, predictions) +
                                                expandPredictionTeams(pred.awayTeamRef, predictions)
                                        t.filterNot { isRef(it) }.any { TeamNameNormalizer.matches(it, relevantTeam) }
                                    }
                                    if (hit) displayMap[id] = (displayMap[id] ?: 0) + pts
                                    else displayMap.putIfAbsent(id, 0)
                                }
                                count == 0 -> displayMap.putIfAbsent(id, 0)
                            }
                        } else {
                            when (count) {
                                2 -> displayMap[id] = (displayMap[id] ?: 0) + pts
                                0 -> displayMap[id] = (displayMap[id] ?: 0)
                            }
                        }
                    }
                }
            }
        }
        return displayMap
    }
}
