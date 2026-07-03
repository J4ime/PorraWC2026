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
        
        val roundPoints = mapOf(
            "Dieciseisavos" to 20,
            "Octavos" to 40,
            "Cuartos" to 80,
            "Semifinales" to 160,
            "Final" to 500
        )
        
        // Para cada predicción, verificar si los equipos predichos están en la lista real de esa ronda
        for (prediction in predictions) {
            if (prediction.round == "3er puesto") continue
            
            val homeTeam = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
            val awayTeam = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
            
            if (homeTeam.startsWith("W") || homeTeam.startsWith("L") || 
                awayTeam.startsWith("W") || awayTeam.startsWith("L")) continue
            
            val roundList = liveRoundLists[prediction.round].orEmpty()
            var points = 0
            
            // Si el equipo local está en la lista real de esta ronda, sumar puntos
            if (roundList.any { TeamNameNormalizer.matches(it, homeTeam) }) {
                points += roundPoints[prediction.round] ?: 0
            }
            
            // Si el equipo visitante está en la lista real de esta ronda, sumar puntos
            if (roundList.any { TeamNameNormalizer.matches(it, awayTeam) }) {
                points += roundPoints[prediction.round] ?: 0
            }
            
            if (points > 0) {
                result[prediction.matchNumber] = points
            }
        }
        
        // Manejar el partido de 3er puesto (200 pts si se acierta el ganador)
        val thirdPlaceMatch = matches.firstOrNull { it.isKnockout && it.knockoutRound == "3er puesto" }
        if (thirdPlaceMatch != null && thirdPlaceMatch.homeGoals != null && thirdPlaceMatch.awayGoals != null) {
            val actualWinner = if (thirdPlaceMatch.homeGoals!! > thirdPlaceMatch.awayGoals!!) thirdPlaceMatch.homeTeam else thirdPlaceMatch.awayTeam
            val thirdPlacePrediction = predictions.firstOrNull { it.round == "3er puesto" }
            if (thirdPlacePrediction != null) {
                val homeTeam = PointsCalculator.resolvePredictionTeamName(thirdPlacePrediction.homeTeamRef, predictions)
                val awayTeam = PointsCalculator.resolvePredictionTeamName(thirdPlacePrediction.awayTeamRef, predictions)
                val predictedWinner = if (thirdPlacePrediction.winner == 1) homeTeam else if (thirdPlacePrediction.winner == 2) awayTeam else null
                if (predictedWinner != null && TeamNameNormalizer.matches(predictedWinner, actualWinner)) {
                    result[thirdPlacePrediction.matchNumber] = 200
                }
            }
        }
        
        return result
    }
}
