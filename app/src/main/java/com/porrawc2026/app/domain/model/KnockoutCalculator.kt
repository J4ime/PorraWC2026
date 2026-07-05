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

    fun computeNextRoundMatchPoints(
        predictions: List<KnockoutPredictionEntity>,
        matches: List<MatchEntity>
    ): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()

        for (match in matches.filter { it.isKnockout && it.id > 88 }) {
            var totalPts = 0

            val refMatches = matches.filter { m ->
                m.isKnockout && (
                    extractRefMatchId(m.homeTeam) == match.id ||
                    extractRefMatchId(m.awayTeam) == match.id
                )
            }

            for (refMatch in refMatches) {
                val nextRound = refMatch.knockoutRound ?: continue
                val ptsPerTeam = PointsCalculator.getKnockoutPoints(nextRound)

                val nextPred = predictions.firstOrNull { it.matchNumber == refMatch.id } ?: continue
                val predHome = PointsCalculator.resolvePredictionTeamName(nextPred.homeTeamRef, predictions)
                val predAway = PointsCalculator.resolvePredictionTeamName(nextPred.awayTeamRef, predictions)

                val homeInPred = TeamNameNormalizer.matches(predHome, match.homeTeam) || TeamNameNormalizer.matches(predAway, match.homeTeam)
                val awayInPred = TeamNameNormalizer.matches(predHome, match.awayTeam) || TeamNameNormalizer.matches(predAway, match.awayTeam)

                val count = (if (homeInPred) 1 else 0) + (if (awayInPred) 1 else 0)

                val pts = when (count) {
                    2 -> ptsPerTeam
                    1 -> {
                        val predictedTeam = if (homeInPred) match.homeTeam else match.awayTeam
                        checkTeamMatchResult(match, predictedTeam, ptsPerTeam)
                    }
                    else -> 0
                }

                totalPts += pts
            }

            if (totalPts > 0) {
                result[match.id] = totalPts
            }
        }

        return result
    }

    private fun checkTeamMatchResult(match: MatchEntity, predictedTeam: String, ptsPerTeam: Int): Int {
        return when {
            match.winnerTeam != null && TeamNameNormalizer.matches(match.winnerTeam!!, predictedTeam) -> ptsPerTeam
            match.winnerTeam != null -> 0
            match.homeGoals != null && match.awayGoals != null -> {
                val homeWinning = match.homeGoals!! > match.awayGoals!!
                val awayWinning = match.awayGoals!! > match.homeGoals!!
                if ((TeamNameNormalizer.matches(predictedTeam, match.homeTeam) && homeWinning) ||
                    (TeamNameNormalizer.matches(predictedTeam, match.awayTeam) && awayWinning)) ptsPerTeam
                else 0
            }
            else -> 0
        }
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
            return prevMatch?.winnerTeam
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
                if (m.homeGoals == null || m.awayGoals == null) continue
                Log.d("KO_DEBUG", "  match ${m.id}: homeTeam='${m.homeTeam}' isRef=${isRef(m.homeTeam)} awayTeam='${m.awayTeam}' isRef=${isRef(m.awayTeam)}")
                val home = if (isRef(m.homeTeam)) null else resolveKnockoutTeam(m.homeTeam, matches)
                val away = if (isRef(m.awayTeam)) null else resolveKnockoutTeam(m.awayTeam, matches)
                Log.d("KO_DEBUG", "  resolve: home=$home away=$away")
                if (home != null && result[round].orEmpty().none { TeamNameNormalizer.matches(it, home) }) {
                    result[round]?.add(home)
                    Log.d("KO_DEBUG", "  added home=$home to $round, now size=${result[round]?.size}")
                }
                if (away != null && result[round].orEmpty().none { TeamNameNormalizer.matches(it, away) }) {
                    result[round]?.add(away)
                    Log.d("KO_DEBUG", "  added away=$away to $round, now size=${result[round]?.size}")
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
                if (match == null || match.homeGoals == null || match.awayGoals == null) continue
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
}
