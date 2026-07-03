package com.porrawc2026.app.domain.model

import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity

object KnockoutCalculator {

    private val KO_ROUNDS_IN_ORDER = listOf("Dieciseisavos", "Octavos", "Cuartos", "Semifinales", "Final")

    fun roundLevel(round: String): Int = when (round) {
        "Dieciseisavos" -> 1; "Octavos" -> 2; "Cuartos" -> 3; "Semifinales" -> 4
        "3er puesto" -> 5; "Final" -> 6; "Campeón" -> 7; else -> 0
    }

    fun buildLiveRoundLists(matches: List<MatchEntity>): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        KO_ROUNDS_IN_ORDER.forEach { result[it] = mutableListOf() }

        val dieciseisavos = matches.filter { it.isKnockout && it.knockoutRound == "Dieciseisavos" && it.homeTeam.isNotBlank() }
        dieciseisavos.forEach {
            result["Dieciseisavos"]?.add(it.homeTeam)
            result["Dieciseisavos"]?.add(it.awayTeam)
        }

        for (round in KO_ROUNDS_IN_ORDER.drop(1)) {
            val prevRoundIdx = KO_ROUNDS_IN_ORDER.indexOf(round) - 1
            val prevRound = KO_ROUNDS_IN_ORDER[prevRoundIdx]
            val prevRoundMatches = matches.filter { it.isKnockout && it.knockoutRound == prevRound && it.homeTeam.isNotBlank() }

            for (prevMatch in prevRoundMatches) {
                val hasResult = prevMatch.homeGoals != null && prevMatch.awayGoals != null
                val isFinished = prevMatch.winnerTeam != null
                val isLiveTied = hasResult && !isFinished && prevMatch.homeGoals == prevMatch.awayGoals
                val isLiveLeading = hasResult && !isFinished && prevMatch.homeGoals != prevMatch.awayGoals

                if (isFinished) {
                    val winner = if (prevMatch.homeGoals!! > prevMatch.awayGoals!!) prevMatch.homeTeam else prevMatch.awayTeam
                    if (!result[round].orEmpty().any { TeamNameNormalizer.matches(it, winner) }) {
                        result[round]?.add(winner)
                    }
                } else if (isLiveLeading) {
                    val leadingTeam = if (prevMatch.homeGoals!! > prevMatch.awayGoals!!) prevMatch.homeTeam 
                                     else prevMatch.awayTeam
                    if (!result[round].orEmpty().any { TeamNameNormalizer.matches(it, leadingTeam) }) {
                        result[round]?.add(leadingTeam)
                    }
                } else if (isLiveTied) {
                    if (!result[round].orEmpty().any { TeamNameNormalizer.matches(it, prevMatch.homeTeam) }) {
                        result[round]?.add(prevMatch.homeTeam)
                    }
                    if (!result[round].orEmpty().any { TeamNameNormalizer.matches(it, prevMatch.awayTeam) }) {
                        result[round]?.add(prevMatch.awayTeam)
                    }
                }
            }
        }

        return result
    }

    fun computePointsFromLiveLists(
        predictions: List<KnockoutPredictionEntity>,
        liveRoundLists: Map<String, List<String>>,
        matches: List<MatchEntity>
    ): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        
        val roundToNextRound = mapOf(
            "Dieciseisavos" to "Octavos",
            "Octavos" to "Cuartos",
            "Cuartos" to "Semifinales",
            "Semifinales" to "Final"
        )
        
        val roundPoints = mapOf(
            "Dieciseisavos" to 20,
            "Octavos" to 40,
            "Cuartos" to 80,
            "Semifinales" to 160,
            "Final" to 500
        )
        
        val predictionsByRound = predictions.groupBy { it.round }
        
        val koMatches = matches.filter { it.isKnockout && it.knockoutRound != null }
        
        for (match in koMatches) {
            val hasResult = match.homeGoals != null && match.awayGoals != null
            if (!hasResult) continue
            
            val round = match.knockoutRound ?: continue
            
            if (round == "3er puesto") {
                val actualWinner = if (match.homeGoals!! > match.awayGoals!!) match.homeTeam else match.awayTeam
                val prediction = predictions.firstOrNull { it.matchNumber == match.id }
                if (prediction != null) {
                    val homeTeam = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                    val awayTeam = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                    val predictedWinner = if (prediction.winner == 1) homeTeam else if (prediction.winner == 2) awayTeam else null
                    if (predictedWinner != null && TeamNameNormalizer.matches(predictedWinner, actualWinner)) {
                        result[match.id] = 200
                    }
                }
                continue
            }
            
            val nextRound = roundToNextRound[round] ?: continue
            val pointsForNextRound = roundPoints[nextRound] ?: continue
            
            val nextRoundPredictions = predictionsByRound[nextRound].orEmpty()
            
            val isTied = match.homeGoals == match.awayGoals
            
            if (isTied) {
                val homeInNextRound = nextRoundPredictions.any { prediction ->
                    val homeTeam = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                    val awayTeam = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                    TeamNameNormalizer.matches(homeTeam, match.homeTeam) || TeamNameNormalizer.matches(awayTeam, match.homeTeam)
                }
                val awayInNextRound = nextRoundPredictions.any { prediction ->
                    val homeTeam = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                    val awayTeam = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                    TeamNameNormalizer.matches(homeTeam, match.awayTeam) || TeamNameNormalizer.matches(awayTeam, match.awayTeam)
                }
                
                if (homeInNextRound && awayInNextRound) {
                    result[match.id] = pointsForNextRound
                }
            } else {
                val leadingTeam = if (match.homeGoals!! > match.awayGoals!!) match.homeTeam 
                                 else match.awayTeam
                
                val userPredictedLeadingInNextRound = nextRoundPredictions.any { prediction ->
                    val homeTeam = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                    val awayTeam = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                    TeamNameNormalizer.matches(homeTeam, leadingTeam) || TeamNameNormalizer.matches(awayTeam, leadingTeam)
                }
                
                if (userPredictedLeadingInNextRound) {
                    result[match.id] = pointsForNextRound
                }
            }
        }
        
        return result
    }
}
