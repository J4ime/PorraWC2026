package com.porrawc2026.app.domain.model

import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.KnockoutTeamProgressEntity
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

    fun buildAdvancementEntries(matches: List<MatchEntity>): List<KnockoutTeamProgressEntity> =
        buildAdvancementEntries(buildAdvancement(matches))

    fun buildAdvancementEntries(advancement: Map<String, String>): List<KnockoutTeamProgressEntity> {
        val roundLevelMap = mapOf(
            "Dieciseisavos" to 1, "Octavos" to 2, "Cuartos" to 3,
            "Semifinales" to 4, "3er puesto" to 5, "Final" to 6
        )
        return advancement.mapNotNull { (team, round) ->
            val level = roundLevelMap[round] ?: return@mapNotNull null
            KnockoutTeamProgressEntity(roundLevel = level, roundName = round, teamName = team)
        }
    }

    fun advancementMapFromEntities(entities: List<KnockoutTeamProgressEntity>): Map<String, String> {
        val roundLevelRevMap = mapOf(
            1 to "Dieciseisavos", 2 to "Octavos", 3 to "Cuartos",
            4 to "Semifinales", 5 to "3er puesto", 6 to "Final"
        )
        return entities.mapNotNull { e ->
            val round = roundLevelRevMap[e.roundLevel] ?: return@mapNotNull null
            e.teamName to round
        }.toMap()
    }

    fun computePointsFromAdvancement(
        predictions: List<KnockoutPredictionEntity>,
        advancement: Map<String, String>
    ): Map<Int, Int> {
        val resolvedHome = predictions.associate { it.matchNumber to PointsCalculator.resolvePredictionTeamName(it.homeTeamRef, predictions) }
        val resolvedAway = predictions.associate { it.matchNumber to PointsCalculator.resolvePredictionTeamName(it.awayTeamRef, predictions) }
        return predictions.mapNotNull { prediction ->
            val homeTeam = resolvedHome[prediction.matchNumber] ?: prediction.homeTeamRef
            val awayTeam = resolvedAway[prediction.matchNumber] ?: prediction.awayTeamRef
            
            if (homeTeam.startsWith("W") || homeTeam.startsWith("L") || 
                awayTeam.startsWith("W") || awayTeam.startsWith("L")) {
                return@mapNotNull null
            }
            
            val predictedWinner = when (prediction.winner) { 1 -> homeTeam; 2 -> awayTeam; else -> null } ?: return@mapNotNull null
            val actualReachedRound = advancement.entries.firstOrNull { (team, _) -> TeamNameNormalizer.matches(team, predictedWinner) }?.value
            val isCorrect = if (prediction.round == "3er puesto") {
                actualReachedRound != null && roundLevel(actualReachedRound) == roundLevel(prediction.round)
            } else {
                actualReachedRound != null && roundLevel(actualReachedRound) >= roundLevel(prediction.round)
            }
            if (isCorrect) prediction.matchNumber to PointsCalculator.getKnockoutPoints(prediction.round) else null
        }.toMap()
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
            } else {
                actualReachedRound != null && roundLevel(actualReachedRound) >= roundLevel(prediction.round)
            }
            if (isCorrect) prediction.matchNumber to PointsCalculator.getKnockoutPoints(prediction.round) else null
        }.toMap()
    }
}
