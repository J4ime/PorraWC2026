package com.porrawc2026.app.domain.model

import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity

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
        "3er puesto" -> 250
        "Final" -> 500
        else -> 0
    }

    fun getRoundAdvancementPoints(round: String): Int = when (round) {
        "Dieciseisavos" -> 20
        "Octavos" -> 40
        "Cuartos" -> 80
        "Semifinales" -> 160
        "Final" -> 250
        "Campeon" -> 500
        "3er puesto" -> 200
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

    data class AdvancingTeams(
        val dieciseisavos: Set<String> = emptySet(),
        val octavos: Set<String> = emptySet(),
        val cuartos: Set<String> = emptySet(),
        val semifinales: Set<String> = emptySet(),
        val final: Set<String> = emptySet(),
        val campeon: Set<String> = emptySet(),
        val tercero: Set<String> = emptySet()
    )

    fun computeActualAdvancingTeams(
        allMatches: List<MatchEntity>,
        allTeams: List<com.porrawc2026.app.data.local.entity.TeamEntity>
    ): AdvancingTeams {
        val standings = computeGroupStandings(allMatches, allTeams, usePredicted = false)
        val dieciseisavos = getTeamsAdvancingFromGroups(standings, allMatches, allTeams)

        val winnerByMatch = allMatches
            .filter { it.isKnockout && it.winnerTeam != null }
            .associate { it.id to it.winnerTeam!! }

        return AdvancingTeams(
            dieciseisavos = dieciseisavos,
            octavos = resolveAdvancing(73..88, winnerByMatch),
            cuartos = resolveAdvancing(89..96, winnerByMatch),
            semifinales = resolveAdvancing(97..100, winnerByMatch),
            final = resolveAdvancing(101..102, winnerByMatch),
            campeon = resolveAdvancing(104..104, winnerByMatch),
            tercero = resolveAdvancing(103..103, winnerByMatch)
        )
    }

    fun computePredictedAdvancingTeams(
        allMatches: List<MatchEntity>,
        allTeams: List<com.porrawc2026.app.data.local.entity.TeamEntity>,
        knockoutPredictions: List<KnockoutPredictionEntity>
    ): AdvancingTeams {
        val standings = computeGroupStandings(allMatches, allTeams, usePredicted = true)
        val dieciseisavos = getTeamsAdvancingFromGroups(standings, allMatches, allTeams)

        val predictedWinners = resolvePredictedKnockoutWinners(knockoutPredictions, allMatches)
        val flat = predictedWinners.mapValues { (_, v) -> v.teamName }

        return AdvancingTeams(
            dieciseisavos = dieciseisavos,
            octavos = resolveAdvancing(73..88, flat),
            cuartos = resolveAdvancing(89..96, flat),
            semifinales = resolveAdvancing(97..100, flat),
            final = resolveAdvancing(101..102, flat),
            campeon = resolveAdvancing(104..104, flat),
            tercero = resolveAdvancing(103..103, flat)
        )
    }

    fun calculateTotalAdvancementPoints(predicted: AdvancingTeams, actual: AdvancingTeams): Int {
        var total = 0
        total += predicted.dieciseisavos.intersect(actual.dieciseisavos).size * 20
        total += predicted.octavos.intersect(actual.octavos).size * 40
        total += predicted.cuartos.intersect(actual.cuartos).size * 80
        total += predicted.semifinales.intersect(actual.semifinales).size * 160
        total += predicted.final.intersect(actual.final).size * 250
        total += predicted.campeon.intersect(actual.campeon).size * 500
        total += predicted.tercero.intersect(actual.tercero).size * 200
        return total
    }

    private fun matchResult(home: Int, away: Int): String = when {
        home > away -> "h"
        home < away -> "a"
        else -> "d"
    }

    private fun resolveAdvancing(matchIds: IntRange, winnerByMatch: Map<Int, String>): Set<String> {
        val result = mutableSetOf<String>()
        for (id in matchIds) {
            val winner = winnerByMatch[id] ?: continue
            if (winner.isNotBlank()) result.add(winner)
        }
        return result
    }

    private fun resolvePredictedKnockoutWinners(
        knockoutPredictions: List<KnockoutPredictionEntity>,
        allMatches: List<MatchEntity>
    ): Map<Int, PredictedWinner> {
        val predByMatch = knockoutPredictions.associateBy { it.matchNumber }
        val result = mutableMapOf<Int, PredictedWinner>()
        val allMatchIds = allMatches.map { it.id }.toSet()

        val processedRounds = listOf(
            "Dieciseisavos" to (73..88),
            "Octavos" to (89..96),
            "Cuartos" to (97..100),
            "Semifinales" to (101..102),
            "3er puesto" to (103..103),
            "Final" to (104..104)
        )

        for ((round, matchIds) in processedRounds) {
            for (id in matchIds) {
                val pred = predByMatch[id] ?: continue
                val winner = pred.winner ?: continue
                val teamName = if (winner == 1) {
                    resolveTeamRef(pred.homeTeamRef, result, allMatchIds)
                } else {
                    resolveTeamRef(pred.awayTeamRef, result, allMatchIds)
                }
                if (teamName != null) {
                    val koMatch = allMatches.firstOrNull { it.id == id }
                    result[id] = PredictedWinner(teamName, round, koMatch)
                }
            }
        }
        return result
    }

    private data class PredictedWinner(
        val teamName: String,
        val round: String,
        val match: MatchEntity?
    )

    private fun resolveTeamRef(
        ref: String,
        resolved: Map<Int, PredictedWinner>,
        allMatchIds: Set<Int>
    ): String? {
        if (ref.startsWith("W")) {
            val refId = ref.substring(1).toIntOrNull() ?: return null
            return resolved[refId]?.teamName
        }
        if (ref.startsWith("L")) {
            val refId = ref.substring(1).toIntOrNull() ?: return null
            val predicted = resolved[refId]
            if (predicted != null) {
                val match = predicted.match ?: return null
                val opp = if (match.homeTeam == predicted.teamName) match.awayTeam else match.homeTeam
                return opp
            }
            return null
        }
        return ref
    }

    private fun computeGroupStandings(
        allMatches: List<MatchEntity>,
        allTeams: List<com.porrawc2026.app.data.local.entity.TeamEntity>,
        usePredicted: Boolean
    ): Map<String, List<GroupStanding>> {
        val groupsMap = mutableMapOf<String, MutableMap<String, GroupStanding>>()

        for (team in allTeams) {
            val g = team.groupLetter.uppercase()
            val standings = groupsMap.getOrPut(g) { mutableMapOf() }
            standings[team.name] = GroupStanding(
                teamName = team.name, points = 0, goalDifference = 0, goalsFor = 0, position = 0
            )
        }

        val groupMatches = allMatches.filter { !it.isKnockout }
        for (match in groupMatches) {
            val homeGoals = if (usePredicted) match.predictedHomeGoals else match.homeGoals
            val awayGoals = if (usePredicted) match.predictedAwayGoals else match.awayGoals
            if (homeGoals == null || awayGoals == null) continue
            val group = match.groupName.removePrefix("Grupo ").trim().uppercase()
            val standings = groupsMap[group] ?: continue
            val home = standings[match.homeTeam] ?: continue
            val away = standings[match.awayTeam] ?: continue

            standings[match.homeTeam] = home.copy(
                points = home.points + when {
                    homeGoals > awayGoals -> 3
                    homeGoals == awayGoals -> 1
                    else -> 0
                },
                goalDifference = home.goalDifference + (homeGoals - awayGoals),
                goalsFor = home.goalsFor + homeGoals
            )
            standings[match.awayTeam] = away.copy(
                points = away.points + when {
                    awayGoals > homeGoals -> 3
                    awayGoals == homeGoals -> 1
                    else -> 0
                },
                goalDifference = away.goalDifference + (awayGoals - homeGoals),
                goalsFor = away.goalsFor + awayGoals
            )
        }

        return groupsMap.mapValues { (_, teamMap) ->
            teamMap.values.sortedWith(
                compareByDescending<GroupStanding> { it.points }
                    .thenByDescending { it.goalDifference }
                    .thenByDescending { it.goalsFor }
            ).mapIndexed { idx, entry -> entry.copy(position = idx + 1) }
        }
    }

    private fun getTeamsAdvancingFromGroups(
        standings: Map<String, List<GroupStanding>>,
        allMatches: List<MatchEntity>,
        allTeams: List<com.porrawc2026.app.data.local.entity.TeamEntity>
    ): Set<String> {
        val topTwo = mutableSetOf<String>()
        val thirdPlaced = mutableListOf<ThirdPlaceTeam>()

        for ((group, entries) in standings) {
            for ((i, entry) in entries.withIndex()) {
                val pos = i + 1
                if (pos <= 2) {
                    topTwo.add(entry.teamName)
                } else if (pos == 3) {
                    thirdPlaced.add(
                        ThirdPlaceTeam(
                            teamName = entry.teamName,
                            group = group,
                            points = entry.points,
                            goalDifference = entry.goalDifference,
                            goalsFor = entry.goalsFor,
                            rank = 0
                        )
                    )
                }
            }
        }

        if (thirdPlaced.size < 8) return topTwo

        val ranked = thirdPlaced.sortedWith(
            compareByDescending<ThirdPlaceTeam> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
        )

        val qualified = ranked.take(8)
        topTwo.addAll(qualified.map { it.teamName })
        return topTwo
    }
}
