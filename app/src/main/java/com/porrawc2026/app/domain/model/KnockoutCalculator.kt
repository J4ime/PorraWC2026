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

                if (isFinished && hasResult) {
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
    ): Pair<Map<Int, Int>, Map<Int, Int>> {
        val pointsByMatch = mutableMapOf<Int, Int>()
        val pointsByPrediction = mutableMapOf<Int, Int>()
        
        val roundToNextRound = mapOf(
            "Dieciseisavos" to "Octavos",
            "Octavos" to "Cuartos",
            "Cuartos" to "Semifinales",
            "Semifinales" to "Final"
        )
        
        val roundPoints = mapOf(
            "Octavos" to 40,
            "Cuartos" to 80,
            "Semifinales" to 160,
            "Final" to 500
        )
        
        // Para cada partido de eliminatorias (excepto 3er puesto)
        val koMatches = matches.filter { it.isKnockout && it.knockoutRound != null && it.knockoutRound != "3er puesto" }
        
        for (match in koMatches) {
            val round = match.knockoutRound ?: continue
            val nextRound = roundToNextRound[round] ?: continue
            val pointsForNextRound = roundPoints[nextRound] ?: continue
            
            // Buscar predicciones de la siguiente ronda
            val nextRoundPredictions = predictions.filter { it.round == nextRound }
            if (nextRoundPredictions.isEmpty()) continue
            
            val hasResult = match.homeGoals != null && match.awayGoals != null
            val isFinished = match.winnerTeam != null
            
            // Caso 1: Partido finalizado
            if (isFinished) {
                val winner = match.winnerTeam ?: continue
                // Verificar si el usuario tiene al ganador en su predicción de la siguiente ronda
                val predictionsWithWinner = nextRoundPredictions.filter { prediction ->
                    val homeTeam = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                    val awayTeam = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                    TeamNameNormalizer.matches(homeTeam, winner) || TeamNameNormalizer.matches(awayTeam, winner)
                }
                if (predictionsWithWinner.isNotEmpty()) {
                    pointsByMatch[match.id] = pointsForNextRound
                    // Asignar puntos a las predicciones que tienen al ganador
                    predictionsWithWinner.forEach { prediction ->
                        pointsByPrediction[prediction.matchNumber] = pointsForNextRound
                    }
                }
            }
            // Caso 2: Partido en vivo
            else if (hasResult) {
                val homeGoals = match.homeGoals!!
                val awayGoals = match.awayGoals!!
                
                // Caso 2a: Hay un equipo ganando provisionalmente
                if (homeGoals != awayGoals) {
                    val leadingTeam = if (homeGoals > awayGoals) match.homeTeam else match.awayTeam
                    val predictionsWithLeader = nextRoundPredictions.filter { prediction ->
                        val homeTeam = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                        val awayTeam = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                        TeamNameNormalizer.matches(homeTeam, leadingTeam) || TeamNameNormalizer.matches(awayTeam, leadingTeam)
                    }
                    if (predictionsWithLeader.isNotEmpty()) {
                        pointsByMatch[match.id] = pointsForNextRound
                        predictionsWithLeader.forEach { prediction ->
                            pointsByPrediction[prediction.matchNumber] = pointsForNextRound
                        }
                    }
                }
                // Caso 2b: Partido empatado
                else {
                    val predictionsWithBoth = nextRoundPredictions.filter { prediction ->
                        val homeTeam = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                        val awayTeam = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                        (TeamNameNormalizer.matches(homeTeam, match.homeTeam) || TeamNameNormalizer.matches(awayTeam, match.homeTeam)) &&
                        (TeamNameNormalizer.matches(homeTeam, match.awayTeam) || TeamNameNormalizer.matches(awayTeam, match.awayTeam))
                    }
                    if (predictionsWithBoth.isNotEmpty()) {
                        pointsByMatch[match.id] = pointsForNextRound
                        predictionsWithBoth.forEach { prediction ->
                            pointsByPrediction[prediction.matchNumber] = pointsForNextRound
                        }
                    }
                }
            }
            // Caso 3: Partido no jugado aún
            else {
                val predictionsWithBoth = nextRoundPredictions.filter { prediction ->
                    val homeTeam = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                    val awayTeam = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                    (TeamNameNormalizer.matches(homeTeam, match.homeTeam) || TeamNameNormalizer.matches(awayTeam, match.homeTeam)) &&
                    (TeamNameNormalizer.matches(homeTeam, match.awayTeam) || TeamNameNormalizer.matches(awayTeam, match.awayTeam))
                }
                if (predictionsWithBoth.isNotEmpty()) {
                    pointsByMatch[match.id] = pointsForNextRound
                    predictionsWithBoth.forEach { prediction ->
                        pointsByPrediction[prediction.matchNumber] = pointsForNextRound
                    }
                }
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
                    pointsByMatch[thirdPlaceMatch.id] = 200
                    pointsByPrediction[thirdPlacePrediction.matchNumber] = 200
                }
            }
        }
        
        return Pair(pointsByMatch, pointsByPrediction)
    }
}
