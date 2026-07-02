package com.porrawc2026.app.domain.model

import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity

object KnockoutCalculator {

    fun roundLevel(round: String): Int = when (round) {
        "Dieciseisavos" -> 1; "Octavos" -> 2; "Cuartos" -> 3; "Semifinales" -> 4
        "3er puesto" -> 5; "Final" -> 6; "Campeón" -> 7; else -> 0
    }

    fun buildAdvancement(matches: List<MatchEntity>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val koRounds = listOf("Dieciseisavos", "Octavos", "Cuartos", "Semifinales", "Final")
        for (match in matches.filter { it.homeTeam.isNotBlank() }) {
            if (!match.isKnockout || match.knockoutRound == null) continue
            val round = match.knockoutRound
            if (round !in koRounds && round != "3er puesto") continue
            for (team in listOf(match.homeTeam, match.awayTeam)) {
                val prev = result[team]
                if (prev == null || roundLevel(round) > roundLevel(prev)) result[team] = round
            }
            if (round == "3er puesto") continue
            val winner = match.winnerTeam?.let { w ->
                val es = TeamNameNormalizer.enToEs(w)
                if (TeamNameNormalizer.matches(es, match.homeTeam) || TeamNameNormalizer.matches(es, match.awayTeam)) es else null
            } ?: if (match.homeGoals != null && match.awayGoals != null && match.homeGoals != match.awayGoals) {
                if (match.homeGoals!! > match.awayGoals!!) match.homeTeam else match.awayTeam
            } else null
            if (winner != null) {
                val nextIdx = koRounds.indexOf(round) + 1
                if (nextIdx < koRounds.size) {
                    val nextRound = koRounds[nextIdx]
                    val prev = result[winner]
                    if (prev == null || roundLevel(nextRound) > roundLevel(prev)) result[winner] = nextRound
                }
            }
        }
        return result
    }

    fun computePoints(
        matches: List<MatchEntity>,
        predictions: List<KnockoutPredictionEntity>
    ): Map<Int, Int> {
        val advancement = buildAdvancement(matches)
        val resolvedHome = predictions.associate { it.matchNumber to PointsCalculator.resolvePredictionTeamName(it.homeTeamRef, predictions) }
        val resolvedAway = predictions.associate { it.matchNumber to PointsCalculator.resolvePredictionTeamName(it.awayTeamRef, predictions) }
        return predictions.mapNotNull { prediction ->
            val homeTeam = resolvedHome[prediction.matchNumber] ?: prediction.homeTeamRef
            val awayTeam = resolvedAway[prediction.matchNumber] ?: prediction.awayTeamRef
            val predictedWinner = when (prediction.winner) { 1 -> homeTeam; 2 -> awayTeam; else -> null } ?: return@mapNotNull null
            val actualReachedRound = advancement.entries.firstOrNull { (team, _) -> TeamNameNormalizer.matches(team, predictedWinner) }?.value
            val isCorrect = if (prediction.round == "3er puesto") {
                actualReachedRound != null && roundLevel(actualReachedRound) == roundLevel(prediction.round)
            } else if (prediction.round == "Dieciseisavos") {
                actualReachedRound != null && roundLevel(actualReachedRound) > roundLevel(prediction.round)
            } else {
                actualReachedRound != null && roundLevel(actualReachedRound) >= roundLevel(prediction.round)
            }
            if (isCorrect) prediction.matchNumber to PointsCalculator.getKnockoutPoints(prediction.round) else null
        }.toMap()
    }
}
