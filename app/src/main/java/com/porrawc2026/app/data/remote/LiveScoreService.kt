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

data class CardUpdate(
    val matchId: Int,
    val homeReds: Int,
    val awayReds: Int,
    val homeYellows: Int,
    val awayYellows: Int
)

@Singleton
class LiveScoreService @Inject constructor(
    private val wc26: WorldCup26Service,
    private val zafronix: ZafronixService,
    private val espnService: EspnService
) {

    suspend fun fetchScoreUpdates(matches: List<MatchEntity>): Pair<List<LiveScoreUpdate>, List<CardUpdate>> {
        val scoreUpdates = mutableListOf<LiveScoreUpdate>()
        val cardUpdates = mutableListOf<CardUpdate>()

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

        runCatching {
            val zaf = zafronix.getMatches()
            zaf.data.forEach { m ->
                val entity = findMatchingMatch(matches, m.homeTeam ?: "", m.awayTeam ?: "")
                    ?: return@forEach
                val cards = m.cards ?: return@forEach
                val homeReds = cards.count { c -> c.team == "home" && c.color == "red" }
                val awayReds = cards.count { c -> c.team == "away" && c.color == "red" }
                val homeYellows = cards.count { c -> c.team == "home" && c.color == "yellow" }
                val awayYellows = cards.count { c -> c.team == "away" && c.color == "yellow" }
                cardUpdates.add(CardUpdate(entity.id, homeReds, awayReds, homeYellows, awayYellows))

                val existingEntry = scoreUpdates.indexOfFirst { it.matchId == entity.id }
                if (existingEntry < 0 || (scoreUpdates[existingEntry].homeScorers.isEmpty() && scoreUpdates[existingEntry].awayScorers.isEmpty())) {
                    val goals = m.goals ?: return@forEach
                    val homeG = goals.filter { it.team == "home" }.map { LiveScorer(it.scorer ?: "?", it.minute ?: 0) }
                    val awayG = goals.filter { it.team == "away" }.map { LiveScorer(it.scorer ?: "?", it.minute ?: 0) }
                    if (existingEntry >= 0) {
                        scoreUpdates[existingEntry] = scoreUpdates[existingEntry].copy(
                            homeScorers = homeG, awayScorers = awayG
                        )
                    } else if (homeG.isNotEmpty() || awayG.isNotEmpty()) {
                        val hScore = m.homeScore ?: 0
                        val aScore = m.awayScore ?: 0
                        scoreUpdates.add(
                            LiveScoreUpdate(
                                matchId = entity.id,
                                homeGoals = hScore,
                                awayGoals = aScore,
                                homeScorers = homeG,
                                awayScorers = awayG,
                                isFinished = m.status == "completed",
                                liveMinute = null
                            )
                        )
                    }
                }
            }
        }

        return Pair(scoreUpdates, cardUpdates)
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
        val clean = entry.trim().removeSurrounding("\"").removeSurrounding("'")
            .replace("\"", "").trim()
        if (clean.isBlank() || clean == "null") return null

        val quoteIndex = clean.indexOf("'")
        if (quoteIndex < 0) return null

        val beforeQuote = clean.substring(0, quoteIndex).trim()
        val parts = beforeQuote.split(" ")
        val minute = parts.lastOrNull()?.toIntOrNull() ?: return null
        val name = parts.dropLast(1).joinToString(" ").trim()
        if (name.isBlank()) return null

        return LiveScorer(name, minute)
    }
}
