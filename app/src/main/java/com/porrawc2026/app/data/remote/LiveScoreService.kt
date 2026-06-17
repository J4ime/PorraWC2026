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
            val minute = when (val te = g.time_elapsed) {
                "finished" -> "FINAL"
                "live" -> null
                else -> te?.toIntOrNull()?.let { "${it}'" }
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

        return scoreUpdates
    }

    suspend fun fetchEspnLiveMinutes(matches: List<MatchEntity>): List<LiveScoreUpdate> {
        val updates = mutableListOf<LiveScoreUpdate>()
        val scoreboard = espnService.getScoreboard()
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
        return updates
    }

    suspend fun fetchTopScorers(matches: List<MatchEntity>): List<TopScorerData> {
        val allScorers = mutableMapOf<String, TopScorerData>()
        
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
        
        return allScorers.values.sortedByDescending { it.goals }.take(50)
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
        val clean = s.trim().removeSurrounding("\"").removeSurrounding("'")
            .replace("\u201C", "").replace("\u201D", "").trim()
        if (clean.isBlank() || clean == "null" || clean == "[]" || clean == "{}") return emptyList()

        if (clean.startsWith("[") && clean.endsWith("]")) {
            return parseJsonArrayScorers(clean)
        }
        if (clean.startsWith("{") && clean.endsWith("}")) {
            return listOfNotNull(parseJsonObjectScorer(clean))
        }

        val cleaned = clean.replace("\"", "").trim()
        return cleaned.split(",").mapNotNull { parseSingleScorer(it.trim()) }
    }

    private fun parseJsonArrayScorers(json: String): List<LiveScorer> {
        val result = mutableListOf<LiveScorer>()
        val inner = json.substring(1, json.length - 1).trim()
        if (inner.isBlank()) return result

        var i = 0
        while (i < inner.length) {
            val objStart = inner.indexOf('{', i)
            if (objStart < 0) break
            var depth = 0
            var objEnd = objStart
            while (objEnd < inner.length) {
                when (inner[objEnd]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) break
                    }
                }
                objEnd++
            }
            if (depth != 0) break
            val objStr = inner.substring(objStart, objEnd + 1)
            val scorer = parseJsonObjectScorer(objStr)
            if (scorer != null) result.add(scorer)
            i = objEnd + 1
        }
        return result
    }

    private fun parseJsonObjectScorer(obj: String): LiveScorer? {
        val namePattern = Regex(""""name"\s*:\s*"([^"]*)"""")
        val playerPattern = Regex(""""player"\s*:\s*"([^"]*)"""")
        val scorerPattern = Regex(""""scorer"\s*:\s*"([^"]*)"""")
        val minutePattern = Regex(""""minute"\s*:\s*"?(\d+)"?""")

        val name = namePattern.find(obj)?.groupValues?.get(1)
            ?: playerPattern.find(obj)?.groupValues?.get(1)
            ?: scorerPattern.find(obj)?.groupValues?.get(1)
            ?: return null

        val minute = minutePattern.find(obj)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (name.isBlank()) return null
        return LiveScorer(name.trim(), minute)
    }

    private fun parseSingleScorer(entry: String): LiveScorer? {
        val clean = entry.trim()
            .removeSurrounding("\"").removeSurrounding("'")
            .replace("\"", "").replace("\u201C", "").replace("\u201D", "").trim()
        if (clean.isBlank() || clean == "null" || clean == "{}") return null

        val regex = Regex("^(.+?)\\s+(\\d+)'")
        val match = regex.find(clean) ?: return null

        val name = match.groupValues[1].trim()
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (name.isBlank()) return null

        return LiveScorer(name, minute)
    }
}
