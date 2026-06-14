package com.porrawc2026.app.data.remote

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
    private val apiService: ApiService
) {

    suspend fun fetchScoreUpdates(matches: List<MatchEntity>): Pair<List<LiveScoreUpdate>, List<CardUpdate>> {
        val scoreUpdates = mutableListOf<LiveScoreUpdate>()
        val cardUpdates = mutableListOf<CardUpdate>()

        try {
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
        } catch (_: Exception) { }

        try {
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

                // Add Zafronix goals as scorer source for matches not yet handled by worldcup26
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
        } catch (_: Exception) { }

        return Pair(scoreUpdates, cardUpdates)
    }

    suspend fun fetchLiveMatchDetails(matches: List<MatchEntity>): List<LiveScoreUpdate> {
        val updates = mutableListOf<LiveScoreUpdate>()

        try {
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val today = dateFmt.format(Date())
            val todayMatchesResponse = apiService.getMatches(dateFrom = today, dateTo = today)

            todayMatchesResponse.matches.forEach { fbMatch ->
                if (fbMatch.status !in listOf("IN_PLAY", "PAUSED", "FINISHED")) return@forEach

                val entity = findMatchingMatch(matches,
                    fbMatch.homeTeam?.name ?: fbMatch.homeTeam?.shortName ?: "",
                    fbMatch.awayTeam?.name ?: fbMatch.awayTeam?.shortName ?: ""
                ) ?: return@forEach

                try {
                    val detail = apiService.getMatchDetail(fbMatch.id)
                    val status = detail.status ?: return@forEach

                    if (status !in listOf("IN_PLAY", "PAUSED", "FINISHED")) return@forEach

                    val homeGoals = detail.score?.fullTime?.home
                        ?: detail.score?.halfTime?.home
                        ?: 0
                    val awayGoals = detail.score?.fullTime?.away
                        ?: detail.score?.halfTime?.away
                        ?: 0

                    val minute = detail.minute ?: when (status) {
                        "PAUSED" -> "HT"
                        "FINISHED" -> "FINAL"
                        else -> null
                    }

                    val homeScorers = detail.goals?.filter {
                        it.team?.name?.let { teamName ->
                            TeamNameNormalizer.matches(entity.homeTeam, teamName)
                        } ?: false
                    }?.mapNotNull { goal ->
                        val scorerName = goal.scorer?.name ?: return@mapNotNull null
                        val goalMinute = goal.minute ?: return@mapNotNull null
                        LiveScorer(scorerName, goalMinute)
                    } ?: emptyList()

                    val awayScorers = detail.goals?.filter {
                        it.team?.name?.let { teamName ->
                            TeamNameNormalizer.matches(entity.awayTeam, teamName)
                        } ?: false
                    }?.mapNotNull { goal ->
                        val scorerName = goal.scorer?.name ?: return@mapNotNull null
                        val goalMinute = goal.minute ?: return@mapNotNull null
                        LiveScorer(scorerName, goalMinute)
                    } ?: emptyList()

                    updates.add(
                        LiveScoreUpdate(
                            matchId = entity.id,
                            homeGoals = homeGoals,
                            awayGoals = awayGoals,
                            homeScorers = homeScorers,
                            awayScorers = awayScorers,
                            isFinished = status == "FINISHED",
                            liveMinute = minute
                        )
                    )
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }

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
        // The worldcup26 API returns strings like: {"Player 45'"} or {"Player1 45'","Player2 90'"}
        // where the outer braces and quotes are literal characters in the JSON string value
        val clean = s.trim().removeSurrounding("\"").removePrefix("{").removeSuffix("}")
            .replace("\"", "").trim()
        if (clean.isBlank() || clean == "null" || clean == "[]") return emptyList()
        return clean.split(",").mapNotNull { parseSingleScorer(it.trim()) }
    }

    private fun parseSingleScorer(entry: String): LiveScorer? {
        val clean = entry.trim().removeSurrounding("\"").removeSurrounding("'")
            .replace("\"", "").trim()
        if (clean.isBlank() || clean == "null") return null

        // The format is "Player Name 90'+5'" or "Player Name 17' (p)"
        // Find the first ' character, take everything before it,
        // the last word before ' is the minute number
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
