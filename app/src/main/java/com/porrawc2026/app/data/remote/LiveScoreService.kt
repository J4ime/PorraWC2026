package com.porrawc2026.app.data.remote

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.domain.model.TeamNameNormalizer
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
    private val wc26: WorldCup26Service,
    private val espnService: EspnService
) {

    suspend fun fetchScoreUpdates(matches: List<MatchEntity>): List<LiveScoreUpdate> {
        val scoreUpdates = mutableListOf<LiveScoreUpdate>()

        runCatching {
            val resp = wc26.getGames()
            resp.games.forEach { g ->
                if (g.time_elapsed == "notstarted") return@forEach
                val hScore = g.home_score?.toIntOrNull() ?: return@forEach
                val aScore = g.away_score?.toIntOrNull() ?: return@forEach
                val entity = findMatchingMatch(matches, g.home_team_name_en ?: "", g.away_team_name_en ?: "")
                    ?: return@forEach
                val homeScorers = parseScorers(g.home_scorers)
                val awayScorers = parseScorers(g.away_scorers)
                val isFinished = g.finished == "TRUE"
                val minute = when (g.time_elapsed) {
                    "finished" -> "FINAL"
                    "live" -> "LIVE"
                    else -> null
                }
                scoreUpdates.add(
                    LiveScoreUpdate(
                        matchId = entity.id,
                        homeGoals = hScore,
                        awayGoals = aScore,
                        homeScorers = homeScorers,
                        awayScorers = awayScorers,
                        isFinished = isFinished,
                        liveMinute = minute
                    )
                )
            }
        }

        return scoreUpdates
    }

    suspend fun fetchEspnLiveMinutes(matches: List<MatchEntity>): List<LiveScoreUpdate> {
        val updates = mutableListOf<LiveScoreUpdate>()
        runCatching {
            val scoreboard = espnService.getScoreboard()
            val events = scoreboard.events ?: return@runCatching

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
                competition.details?.forEach { detail ->
                    val detailText = detail.type?.text ?: ""
                    if (detailText == "Goal" || detailText == "Own Goal") {
                        val playerName = detail.athletesInvolved?.firstOrNull()?.displayName ?: return@forEach
                        val minuteStr = detail.clock?.displayValue ?: return@forEach
                        val goalMinute = minuteStr.replace("'", "").replace("+", "").toIntOrNull() ?: 0
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
        }
        return updates
    }

    suspend fun fetchTopScorers(matches: List<MatchEntity>): List<TopScorerData> {
        val allScorers = mutableMapOf<String, TopScorerData>()
        
        runCatching {
            val resp = wc26.getGames()
            resp.games.forEach { g ->
                if (g.time_elapsed == "notstarted") return@forEach
                val homeTeam = g.home_team_name_en ?: return@forEach
                val awayTeam = g.away_team_name_en ?: return@forEach
                
                val homeScorers = parseScorers(g.home_scorers)
                val awayScorers = parseScorers(g.away_scorers)
                
                homeScorers.forEach { scorer ->
                    val key = scorer.playerName.lowercase()
                    val existing = allScorers[key]
                    if (existing != null) {
                        allScorers[key] = existing.copy(goals = existing.goals + 1)
                    } else {
                        allScorers[key] = TopScorerData(scorer.playerName, homeTeam, 1)
                    }
                }
                
                awayScorers.forEach { scorer ->
                    val key = scorer.playerName.lowercase()
                    val existing = allScorers[key]
                    if (existing != null) {
                        allScorers[key] = existing.copy(goals = existing.goals + 1)
                    } else {
                        allScorers[key] = TopScorerData(scorer.playerName, awayTeam, 1)
                    }
                }
            }
        }
        
        return allScorers.values.sortedByDescending { it.goals }.take(20)
    }

    private fun findMatchingMatch(matches: List<MatchEntity>, homeName: String, awayName: String): MatchEntity? {
        val candidates = matches.filter {
            TeamNameNormalizer.matches(it.homeTeam, homeName) &&
            TeamNameNormalizer.matches(it.awayTeam, awayName)
        }
        return if (candidates.size == 1) candidates.first() else null
    }

    private fun parseScorers(scorers: Any?): List<LiveScorer> {
        if (scorers == null) return emptyList()
        return when (scorers) {
            is List<*> -> scorers.mapNotNull { entry ->
                when (entry) {
                    is Map<*, *> -> parseScorerMap(entry)
                    is String -> parseSingleScorer(entry.trim())
                    else -> parseSingleScorer(entry.toString().trim())
                }
            }
            is Map<*, *> -> listOfNotNull(parseScorerMap(scorers))
            is String -> parseScorerString(scorers)
            else -> {
                val s = scorers.toString().trim()
                if (s.isBlank() || s == "null" || s == "[]" || s == "{}") emptyList()
                else parseScorerString(s)
            }
        }
    }

    private fun parseScorerMap(map: Map<*, *>): LiveScorer? {
        val name = (map["name"] as? String)?.trim()?.ifBlank { null }
            ?: (map["player"] as? String)?.trim()?.ifBlank { null }
            ?: (map["scorer"] as? String)?.trim()?.ifBlank { null }
            ?: return null
        val minute = when (val m = map["minute"]) {
            is Number -> m.toInt()
            is String -> m.replace("'", "").trim().toIntOrNull()
            else -> null
        } ?: return null
        return LiveScorer(name, minute)
    }

    private fun parseScorerString(s: String): List<LiveScorer> {
        val clean = s.trim().removeSurrounding("\"").removePrefix("{").removeSuffix("}")
            .replace("\"", "").trim()
        if (clean.isBlank() || clean == "null" || clean == "[]") return emptyList()
        return clean.split(",").mapNotNull { parseSingleScorer(it.trim()) }
    }

    private fun parseSingleScorer(entry: String): LiveScorer? {
        val clean = entry.trim()
            .removeSurrounding("\"").removeSurrounding("'")
            .replace("\"", "").trim()
        if (clean.isBlank() || clean == "null" || clean == "{}") return null

        val regex = Regex("^(.+?)\\s+(\\d+)'")
        val match = regex.find(clean) ?: return null

        val name = match.groupValues[1].trim()
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (name.isBlank()) return null

        return LiveScorer(name, minute)
    }
}
