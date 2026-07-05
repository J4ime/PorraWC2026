package com.porrawc2026.app.domain.model

import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import com.porrawc2026.app.domain.model.TeamNameNormalizer

object PointsCalculator {

    fun calculateMatchPoints(match: MatchEntity): Int {
        return calculateMatchPoints(
            match.predictedHomeGoals, match.predictedAwayGoals,
            match.homeGoals, match.awayGoals
        )
    }

    fun calculateMatchPoints(
        predHome: Int?, predAway: Int?,
        realHome: Int?, realAway: Int?
    ): Int {
        if (predHome == null || predAway == null) return 0
        if (realHome == null || realAway == null) return 0
        var points = 0
        if (predHome == realHome) points += 10
        if (predAway == realAway) points += 10
        val predResult = matchResult(predHome, predAway)
        val realResult = matchResult(realHome, realAway)
        if (predResult == realResult) points += 30
        return points
    }

    fun getKnockoutPoints(round: String): Int = when (round) {
        "Dieciseisavos" -> 20
        "Octavos" -> 40
        "Cuartos" -> 80
        "Semifinales" -> 160
        "3er puesto" -> 200
        "Final" -> 500
        else -> 0
    }

    fun getQuestionPoints(predicted: Boolean?, correct: Boolean?): Int {
        if (predicted == null || correct == null) return 0
        return if (predicted == correct) 20 else 0
    }

    fun getPlayerPoints(rank: Int, goals: Int): Int {
        val ptsPerGoal = when (rank) {
            1 -> 50
            2 -> 30
            3 -> 10
            else -> 0
        }
        return ptsPerGoal * goals
    }

    fun getPlayerPointsForEntity(player: PlayerPredictionEntity): Int {
        return player.goalsScored * player.pointsPerGoal
    }

    private fun matchResult(home: Int, away: Int): String = when {
        home > away -> "h"
        home < away -> "a"
        else -> "d"
    }

    fun resolvePredictionTeamName(ref: String, predictions: List<KnockoutPredictionEntity>): String {
        val matchId = when {
            ref.startsWith("W") -> ref.substring(1).toIntOrNull()
            ref.startsWith("L") -> ref.substring(1).toIntOrNull()
            ref.startsWith("Ganador ") -> ref.removePrefix("Ganador ").trim().toIntOrNull()
            ref.startsWith("Perdedor ") -> ref.removePrefix("Perdedor ").trim().toIntOrNull()
            else -> return ref
        } ?: return ref

        val pred = predictions.firstOrNull { it.matchNumber == matchId } ?: return ref
        if (pred.winner == null) return ref

        val isWinner = ref.startsWith("W") || ref.startsWith("Ganador ")
        val teamRef = if (isWinner) {
            if (pred.winner == 1) pred.homeTeamRef else pred.awayTeamRef
        } else {
            if (pred.winner == 1) pred.awayTeamRef else pred.homeTeamRef
        }
        return resolvePredictionTeamName(teamRef, predictions)
    }
}
