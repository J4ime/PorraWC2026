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
        
        // Puntos por acertar equipos en Dieciseisavos y Octavos (20 pts por equipo)
        for (round in listOf("Dieciseisavos", "Octavos", "Cuartos")) {
            val actualTeams = liveRoundLists[round].orEmpty()
            if (actualTeams.isEmpty()) continue
            for (prediction in predictions.filter { it.round == round }) {
                val home = PointsCalculator.resolvePredictionTeamName(prediction.homeTeamRef, predictions)
                val away = PointsCalculator.resolvePredictionTeamName(prediction.awayTeamRef, predictions)
                val ptsPerTeam = PointsCalculator.getKnockoutPoints(round)
                var pts = 0
                if (actualTeams.any { TeamNameNormalizer.matches(it, home) }) pts += ptsPerTeam
                if (actualTeams.any { TeamNameNormalizer.matches(it, away) }) pts += ptsPerTeam
                if (pts > 0) {
                    pointsByPrediction[prediction.matchNumber] = (pointsByPrediction[prediction.matchNumber] ?: 0) + pts
                }
            }
        }
        
        // Para cada partido de eliminatorias (excepto 3er puesto)
        val koMatches = matches.filter { it.isKnockout && it.knockoutRound != null && it.knockoutRound != "3er puesto" }
        
        for (match in koMatches) {
            val round = match.knockoutRound ?: continue
            val nextRound = roundToNextRound[round] ?: continue
            val pointsForNextRound = roundPoints[nextRound] ?: continue

            val nextRoundPredictions = predictions.filter { it.round == nextRound }
            if (nextRoundPredictions.isEmpty()) continue

            val teams = listOfNotNull(match.homeTeam, match.awayTeam).filter { it.isNotBlank() }
            if (teams.size < 2) continue

            val hasResult = match.homeGoals != null && match.awayGoals != null
            val isFinished = match.winnerTeam != null
            val hasLeader = hasResult && !isFinished && match.homeGoals != match.awayGoals

            val bothInPreds = teams.all { team ->
                nextRoundPredictions.any { pred ->
                    val h = PointsCalculator.resolvePredictionTeamName(pred.homeTeamRef, predictions)
                    val a = PointsCalculator.resolvePredictionTeamName(pred.awayTeamRef, predictions)
                    TeamNameNormalizer.matches(h, team) || TeamNameNormalizer.matches(a, team)
                }
            }
            val noneInPreds = teams.none { team ->
                nextRoundPredictions.any { pred ->
                    val h = PointsCalculator.resolvePredictionTeamName(pred.homeTeamRef, predictions)
                    val a = PointsCalculator.resolvePredictionTeamName(pred.awayTeamRef, predictions)
                    TeamNameNormalizer.matches(h, team) || TeamNameNormalizer.matches(a, team)
                }
            }

            if (noneInPreds) {
                pointsByMatch[match.id] = 0
                continue
            }

            val teamAwarded = when {
                isFinished -> match.winnerTeam
                hasLeader -> if (match.homeGoals!! > match.awayGoals!!) match.homeTeam else match.awayTeam
                bothInPreds -> match.homeTeam
                else -> null
            }

            val award = if (teamAwarded != null && (bothInPreds || nextRoundPredictions.any { pred ->
                val h = PointsCalculator.resolvePredictionTeamName(pred.homeTeamRef, predictions)
                val a = PointsCalculator.resolvePredictionTeamName(pred.awayTeamRef, predictions)
                TeamNameNormalizer.matches(h, teamAwarded) || TeamNameNormalizer.matches(a, teamAwarded)
            })) pointsForNextRound else 0

            if (award > 0) {
                pointsByMatch[match.id] = (pointsByMatch[match.id] ?: 0) + award
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
