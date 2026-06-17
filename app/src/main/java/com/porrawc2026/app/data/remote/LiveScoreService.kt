package com.porrawc2026.app.data.remote

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

data class LiveScoreUpdate(
    val matchId: Int,
    val homeGoals: Int,
    val awayGoals: Int,
    val homeScorers: List<LiveScorer> = emptyList(),
    val awayScorers: List<LiveScorer> = emptyList(),
    val isFinished: Boolean = false,
    val liveMinute: String? = null
)

data class LiveScorer(val playerName: String, val minute: Int)

data class TopScorerData(
    val playerName: String,
    val teamName: String,
    val goals: Int
)

@Singleton
class LiveScoreService @Inject constructor(
    private val espnService: EspnService
) {
    suspend fun fetchScoreUpdates(matches: List<MatchEntity>): List<LiveScoreUpdate> {
        val updates = mutableListOf<LiveScoreUpdate>()
        val dates = matches.mapNotNull { it.dateTime.take(10).replace("-", "").ifBlank { null } }.distinct()
        val scoreboard = espnService.getScoreboard(dates = dates.ifEmpty { null })
        val events = scoreboard.events ?: return updates

        events.forEach { event ->
            val competition = event.competitions?.firstOrNull() ?: return@forEach
            val status = competition.status ?: return@forEach
            val competitors = competition.competitors ?: return@forEach

            val homeTeam = competitors.firstOrNull { it.homeAway == "home" } ?: return@forEach
            val awayTeam = competitors.firstOrNull { it.homeAway == "away" } ?: return@forEach

            val homeName = homeTeam.team?.displayName ?: homeTeam.team?.name ?: return@forEach
            val awayName = awayTeam.team?.displayName ?: awayTeam.team?.name ?: return@forEach

            val entity = findMatchingMatch(matches, homeName, awayName) ?: return@forEach

            val hScore = homeTeam.score?.toIntOrNull() ?: 0
            val aScore = awayTeam.score?.toIntOrNull() ?: 0

            val statusType = status.type
            val state = statusType?.state ?: ""
            val displayClock = status.displayClock ?: ""
            val shortDetail = statusType?.shortDetail ?: ""

            val minute = when {
                statusType?.completed == true -> "FINAL"
                state == "in" && displayClock.isNotBlank() -> displayClock
                shortDetail == "Halftime" || shortDetail == "HT" -> "HT"
                else -> null
            }

            val homeScorers = mutableListOf<LiveScorer>()
            val awayScorers = mutableListOf<LiveScorer>()
            val minuteRegex = Regex("""(\d+)'(\+(\d+))?""")
            competition.details?.forEach { detail ->
                if (detail.scoringPlay == true) {
                    val playerName = detail.athletesInvolved?.firstOrNull()?.displayName ?: return@forEach
                    val minuteStr = detail.clock?.displayValue ?: return@forEach
                    val m = minuteRegex.find(minuteStr)
                    val goalMinute = if (m != null) {
                        (m.groupValues[1].toIntOrNull() ?: 0) + (m.groupValues[3].toIntOrNull() ?: 0)
                    } else 0
                    val isHome = detail.team?.id == homeTeam.id
                    val scorer = LiveScorer(playerName, goalMinute)
                    if (isHome) homeScorers.add(scorer) else awayScorers.add(scorer)
                }
            }

            updates.add(LiveScoreUpdate(
                matchId = entity.id,
                homeGoals = hScore,
                awayGoals = aScore,
                homeScorers = homeScorers,
                awayScorers = awayScorers,
                isFinished = statusType?.completed == true,
                liveMinute = minute
            ))
        }
        return updates
    }

    suspend fun fetchTopScorers(matches: List<MatchEntity>): List<TopScorerData> {
        val allScorers = mutableMapOf<String, MutableMap<String, Int>>()

        val gson = Gson()
        val type = object : TypeToken<List<LiveScorer>>() {}.type
        for (match in matches) {
            val homeRaw = match.homeScorers
            val awayRaw = match.awayScorers
            if (homeRaw != null) {
                try {
                    val scorers: List<LiveScorer> = gson.fromJson(homeRaw, type) ?: emptyList()
                    scorers.forEach { s ->
                        allScorers.getOrPut(match.homeTeam) { mutableMapOf() }
                            .merge(s.playerName, 1) { a, b -> a + b }
                    }
                } catch (_: Exception) { }
            }
            if (awayRaw != null) {
                try {
                    val scorers: List<LiveScorer> = gson.fromJson(awayRaw, type) ?: emptyList()
                    scorers.forEach { s ->
                        allScorers.getOrPut(match.awayTeam) { mutableMapOf() }
                            .merge(s.playerName, 1) { a, b -> a + b }
                    }
                } catch (_: Exception) { }
            }
        }

        try {
            val dates = matches.mapNotNull { it.dateTime.take(10).replace("-", "").ifBlank { null } }.distinct()
            val scoreboard = espnService.getScoreboard(dates = dates.ifEmpty { null })
            val minuteRegex = Regex("""(\d+)'(\+(\d+))?""")
            scoreboard.events?.forEach { event ->
                val competition = event.competitions?.firstOrNull() ?: return@forEach
                val competitors = competition.competitors ?: return@forEach
                val homeTeam = competitors.firstOrNull { it.homeAway == "home" } ?: return@forEach
                val awayTeam = competitors.firstOrNull { it.homeAway == "away" } ?: return@forEach
                val homeName = homeTeam.team?.displayName ?: homeTeam.team?.name ?: return@forEach
                val awayName = awayTeam.team?.displayName ?: awayTeam.team?.name ?: return@forEach
                competition.details?.forEach { detail ->
                    if (detail.scoringPlay == true) {
                        val playerName = detail.athletesInvolved?.firstOrNull()?.displayName ?: return@forEach
                        val isHome = detail.team?.id == homeTeam.id
                        val teamName = if (isHome) homeName else awayName
                        allScorers.getOrPut(teamName) { mutableMapOf() }
                            .merge(playerName, 1) { a, b -> a + b }
                    }
                }
            }
        } catch (_: Exception) { }

        return allScorers.flatMap { (team, players) ->
            players.map { (player, goals) ->
                TopScorerData(player, team, goals)
            }
        }.sortedByDescending { it.goals }
    }

    private fun findMatchingMatch(matches: List<MatchEntity>, homeName: String, awayName: String): MatchEntity? {
        val candidates = matches.filter {
            TeamNameNormalizer.matches(it.homeTeam, homeName) &&
            TeamNameNormalizer.matches(it.awayTeam, awayName)
        }
        return if (candidates.size == 1) candidates.first() else null
    }
}
