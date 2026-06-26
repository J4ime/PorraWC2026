package com.porrawc2026.app.domain.model

import com.porrawc2026.app.data.local.entity.MatchEntity

data class StandingEntry(
    val teamName: String,
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val goalDifference: Int = 0,
    val points: Int = 0
)

object StandingsCalculator {

    fun calculateGroupStandings(matches: List<MatchEntity>, groupTeams: List<String>): List<StandingEntry> {
        val teamMap = groupTeams.associateBy { TeamNameNormalizer.normalize(it) }
        val standings = groupTeams.associateWith { StandingEntry(it) }.toMutableMap()

        for (match in matches) {
            val homeGoals = match.homeGoals ?: continue
            val awayGoals = match.awayGoals ?: continue
            val home = teamMap[TeamNameNormalizer.normalize(match.homeTeam)] ?: continue
            val away = teamMap[TeamNameNormalizer.normalize(match.awayTeam)] ?: continue

            val homeEntry = standings[home] ?: continue
            val awayEntry = standings[away] ?: continue

            val homeWon = homeGoals > awayGoals
            val draw = homeGoals == awayGoals

            standings[home] = homeEntry.copy(
                played = homeEntry.played + 1,
                won = if (homeWon) homeEntry.won + 1 else homeEntry.won,
                drawn = if (draw) homeEntry.drawn + 1 else homeEntry.drawn,
                lost = if (!homeWon && !draw) homeEntry.lost + 1 else homeEntry.lost,
                goalsFor = homeEntry.goalsFor + homeGoals,
                goalsAgainst = homeEntry.goalsAgainst + awayGoals,
                goalDifference = homeEntry.goalDifference + (homeGoals - awayGoals),
                points = homeEntry.points + when {
                    homeWon -> 3; draw -> 1; else -> 0
                }
            )

            standings[away] = awayEntry.copy(
                played = awayEntry.played + 1,
                won = if (!homeWon && !draw) awayEntry.won + 1 else awayEntry.won,
                drawn = if (draw) awayEntry.drawn + 1 else awayEntry.drawn,
                lost = if (homeWon) awayEntry.lost + 1 else awayEntry.lost,
                goalsFor = awayEntry.goalsFor + awayGoals,
                goalsAgainst = awayEntry.goalsAgainst + homeGoals,
                goalDifference = awayEntry.goalDifference + (awayGoals - homeGoals),
                points = awayEntry.points + when {
                    !homeWon && !draw -> 3; draw -> 1; else -> 0
                }
            )
        }

        return standings.values.sortedWith(
            compareByDescending<StandingEntry> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
                .thenBy { it.teamName }
        )
    }

    data class PredictionStats(
        val totalMatches: Int,
        val matchesWithResults: Int,
        val exactScoreHits: Int,
        val resultHits: Int,
        val missedMatches: Int,
        val totalPoints: Int,
        val accuracyPercent: Float
    )

    fun calculatePredictionStats(matches: List<MatchEntity>): PredictionStats {
        val withResults = matches.filter { it.homeGoals != null && it.awayGoals != null }
        val withPredictions = withResults.filter {
            it.predictedHomeGoals != null && it.predictedAwayGoals != null
        }

        var exactScore = 0
        var resultOnly = 0

        for (match in withPredictions) {
            val predHome = match.predictedHomeGoals!!
            val predAway = match.predictedAwayGoals!!
            val realHome = match.homeGoals!!
            val realAway = match.awayGoals!!

            val exact = predHome == realHome && predAway == realAway
            val predResult = when { predHome > predAway -> "h"; predHome < predAway -> "a"; else -> "d" }
            val realResult = when { realHome > realAway -> "h"; realHome < realAway -> "a"; else -> "d" }

            if (exact) exactScore++ else if (predResult == realResult) resultOnly++
        }

        val totalPoints = withPredictions.sumOf {
            val pH = it.predictedHomeGoals!!; val pA = it.predictedAwayGoals!!
            val rH = it.homeGoals!!; val rA = it.awayGoals!!
            var pts = 0
            if (pH == rH) pts += 10
            if (pA == rA) pts += 10
            val pR = when { pH > pA -> "h"; pH < pA -> "a"; else -> "d" }
            val rR = when { rH > rA -> "h"; rH < rA -> "a"; else -> "d" }
            if (pR == rR) pts += 30
            pts
        }

        return PredictionStats(
            totalMatches = matches.size,
            matchesWithResults = withResults.size,
            exactScoreHits = exactScore,
            resultHits = exactScore + resultOnly,
            missedMatches = withResults.size - exactScore - resultOnly,
            totalPoints = totalPoints,
            accuracyPercent = if (withResults.isNotEmpty())
                (exactScore + resultOnly).toFloat() / withResults.size * 100f else 0f
        )
    }
}
