package com.porrawc2026.app.domain.model

import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity

object KnockoutCalculator {

    private val KO_ROUNDS_IN_ORDER = listOf("Dieciseisavos", "Octavos", "Cuartos", "Semifinales", "3er puesto", "Final")

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
            for (m in roundMatches) {
                val home = resolveKnockoutTeam(m.homeTeam, matches)
                val away = resolveKnockoutTeam(m.awayTeam, matches)
                if (home != null && result[round].orEmpty().none { TeamNameNormalizer.matches(it, home) }) {
                    result[round]?.add(home)
                }
                if (away != null && result[round].orEmpty().none { TeamNameNormalizer.matches(it, away) }) {
                    result[round]?.add(away)
                }
            }
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
            if (actualTeams.isEmpty()) continue

            for (prediction in predictions.filter { it.round == round }) {
                val home = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                val away = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                var pts = 0
                if (actualTeams.any { TeamNameNormalizer.matches(it, home) }) pts += ptsPerTeam
                if (actualTeams.any { TeamNameNormalizer.matches(it, away) }) pts += ptsPerTeam
                if (pts > 0) {
                    pointsByMatch[prediction.matchNumber] = (pointsByMatch[prediction.matchNumber] ?: 0) + pts
                    pointsByPrediction[prediction.matchNumber] = (pointsByPrediction[prediction.matchNumber] ?: 0) + pts
                }
            }
        }

        return Pair(pointsByMatch, pointsByPrediction)
    }
}
