package com.porrawc2026.app.data.remote

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import com.porrawc2026.app.util.LogManager
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
    val liveMinute: String? = null,
    val homeYellowCards: Int = 0,
    val awayYellowCards: Int = 0,
    val homeRedCards: Int = 0,
    val awayRedCards: Int = 0
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
        val dateRange = buildDateRange(matches)
        val scoreboard = espnService.getScoreboard(dates = dateRange)
        val events = scoreboard.events ?: return emptyList()
        return events.mapNotNull { event -> parseEvent(event, matches) }
    }

    private fun buildDateRange(matches: List<MatchEntity>): String? {
        val minDate = matches.minOfOrNull { it.dateTime.take(10).replace("-", "") }
            ?.ifBlank { null } ?: return null
        val maxDate = matches.maxOfOrNull { it.dateTime.take(10).replace("-", "") }
            ?.ifBlank { null } ?: return null
        return "$minDate-$maxDate"
    }

    private fun parseEvent(event: EspnEvent, matches: List<MatchEntity>): LiveScoreUpdate? {
        val competition = event.competitions?.firstOrNull() ?: return null
        val status = competition.status ?: return null
        val competitors = competition.competitors ?: return null

        val homeTeam = competitors.firstOrNull { it.homeAway == "home" } ?: return null
        val awayTeam = competitors.firstOrNull { it.homeAway == "away" } ?: return null

        val homeName = homeTeam.team?.displayName ?: homeTeam.team?.name ?: return null
        val awayName = awayTeam.team?.displayName ?: awayTeam.team?.name ?: return null

        val entity = findMatchingMatch(matches, homeName, awayName)
        if (entity == null) {
            LogManager.log("LiveScoreService", "No match found for $homeName vs $awayName")
            return null
        }

        val hScore = homeTeam.score?.toIntOrNull() ?: 0
        val aScore = awayTeam.score?.toIntOrNull() ?: 0
        val minute = computeMinute(status)
        val (homeScorers, awayScorers, hYellows, aYellows, hReds, aReds) = parseDetails(competition.details, homeTeam.id)

        return LiveScoreUpdate(
            matchId = entity.id, homeGoals = hScore, awayGoals = aScore,
            homeScorers = homeScorers, awayScorers = awayScorers,
            isFinished = status.type?.completed == true, liveMinute = minute,
            homeYellowCards = hYellows, awayYellowCards = aYellows,
            homeRedCards = hReds, awayRedCards = aReds
        )
    }

    private fun computeMinute(status: EspnStatus): String? {
        val statusType = status.type ?: return null
        return when {
            statusType.completed == true -> "FINAL"
            statusType.state == "in" && !status.displayClock.isNullOrBlank() -> status.displayClock
            statusType.shortDetail == "Halftime" || statusType.shortDetail == "HT" -> "HT"
            else -> null
        }
    }

    private fun parseDetails(details: List<EspnDetail>?, homeTeamId: String?): ParsedDetails {
        val homeScorers = mutableListOf<LiveScorer>()
        val awayScorers = mutableListOf<LiveScorer>()
        var hYellows = 0; var aYellows = 0; var hReds = 0; var aReds = 0
        details?.forEach { detail ->
            if (detail.scoringPlay == true) {
                val playerName = detail.athletesInvolved?.firstOrNull()?.displayName ?: return@forEach
                val minuteStr = detail.clock?.displayValue ?: return@forEach
                val m = minuteRegex.find(minuteStr)
                val goalMinute = if (m != null) {
                    (m.groupValues[1].toIntOrNull() ?: 0) + (m.groupValues[3].toIntOrNull() ?: 0)
                } else 0
                val isHome = detail.team?.id == homeTeamId
                if (isHome) homeScorers.add(LiveScorer(playerName, goalMinute))
                else awayScorers.add(LiveScorer(playerName, goalMinute))
            }
            if (detail.yellowCard == true) { if (detail.team?.id == homeTeamId) hYellows++ else aYellows++ }
            if (detail.redCard == true) { if (detail.team?.id == homeTeamId) hReds++ else aReds++ }
        }
        return ParsedDetails(homeScorers, awayScorers, hYellows, aYellows, hReds, aReds)
    }

    private data class ParsedDetails(
        val homeScorers: List<LiveScorer>, val awayScorers: List<LiveScorer>,
        val homeYellowCards: Int, val awayYellowCards: Int,
        val homeRedCards: Int, val awayRedCards: Int
    )

    suspend fun fetchTopScorers(matches: List<MatchEntity>): List<TopScorerData> {
        val allScorers = mutableMapOf<String, MutableMap<String, Int>>()

        for (match in matches) {
            val homeRaw = match.homeScorers
            val awayRaw = match.awayScorers
            if (homeRaw != null) {
                try {
                    val scorers: List<LiveScorer> = gson.fromJson(homeRaw, scorerListType) ?: emptyList()
                    scorers.forEach { s ->
                        allScorers.getOrPut(match.homeTeam) { mutableMapOf() }
                            .merge(s.playerName, 1) { a, b -> a + b }
                    }
                } catch (e: Exception) {
                    LogManager.log("LiveScoreService", "Error parsing home scorers for match ${match.id}", e)
                }
            }
            if (awayRaw != null) {
                try {
                    val scorers: List<LiveScorer> = gson.fromJson(awayRaw, scorerListType) ?: emptyList()
                    scorers.forEach { s ->
                        allScorers.getOrPut(match.awayTeam) { mutableMapOf() }
                            .merge(s.playerName, 1) { a, b -> a + b }
                    }
                } catch (e: Exception) {
                    LogManager.log("LiveScoreService", "Error parsing away scorers for match ${match.id}", e)
                }
            }
        }

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

    companion object {
        private val minuteRegex = Regex("""(\d+)'(\+(\d+))?""")
        private val gson = Gson()
        private val scorerListType = object : TypeToken<List<LiveScorer>>() {}.type
    }
}
