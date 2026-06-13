package com.porrawc2026.app.data.remote

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.domain.model.PointsCalculator
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
    private val apiService: ApiService
) {

    suspend fun fetchScoreUpdates(matches: List<MatchEntity>): Pair<List<LiveScoreUpdate>, List<CardUpdate>> {
        val scoreUpdates = mutableListOf<LiveScoreUpdate>()
        val cardUpdates = mutableListOf<CardUpdate>()

        try {
            val resp = wc26.getGames()
            resp.games.forEach { g ->
                if (g.finished != "TRUE") return@forEach
                val hScore = g.home_score?.toIntOrNull() ?: return@forEach
                val aScore = g.away_score?.toIntOrNull() ?: return@forEach
                val entity = findMatchingMatch(matches, g.home_team_name_en ?: "", g.away_team_name_en ?: "")
                    ?: return@forEach
                val homeScorers = parseScorers(g.home_scorers)
                val awayScorers = parseScorers(g.away_scorers)
                scoreUpdates.add(
                    LiveScoreUpdate(
                        matchId = entity.id,
                        homeGoals = hScore,
                        awayGoals = aScore,
                        homeScorers = homeScorers,
                        awayScorers = awayScorers,
                        isFinished = true,
                        liveMinute = "FINAL"
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
            }
        } catch (_: Exception) { }

        return Pair(scoreUpdates, cardUpdates)
    }

    suspend fun fetchLiveMatchDetails(matches: List<MatchEntity>): List<LiveScoreUpdate> {
        val updates = mutableListOf<LiveScoreUpdate>()
        
        matches.filter { match ->
            match.homeGoals == null || match.awayGoals == null
        }.forEach { match ->
            try {
                val detail = apiService.getMatchDetail(match.id)
                val status = detail.status ?: return@forEach
                
                if (status !in listOf("IN_PLAY", "PAUSED", "SCHEDULED")) return@forEach
                
                val homeGoals = detail.score?.fullTime?.home 
                    ?: detail.score?.halfTime?.home 
                    ?: 0
                val awayGoals = detail.score?.fullTime?.away 
                    ?: detail.score?.halfTime?.away 
                    ?: 0
                
                val minute = detail.minute ?: when(status) {
                    "PAUSED" -> "HT"
                    else -> null
                }
                
                val homeScorers = detail.goals?.filter { 
                    it.team?.name?.let { teamName -> 
                        TeamNameNormalizer.matches(match.homeTeam, teamName) 
                    } ?: false 
                }?.mapNotNull { goal ->
                    val scorerName = goal.scorer?.name ?: return@mapNotNull null
                    val goalMinute = goal.minute ?: return@mapNotNull null
                    LiveScorer(scorerName, goalMinute)
                } ?: emptyList()
                
                val awayScorers = detail.goals?.filter { 
                    it.team?.name?.let { teamName -> 
                        TeamNameNormalizer.matches(match.awayTeam, teamName) 
                    } ?: false 
                }?.mapNotNull { goal ->
                    val scorerName = goal.scorer?.name ?: return@mapNotNull null
                    val goalMinute = goal.minute ?: return@mapNotNull null
                    LiveScorer(scorerName, goalMinute)
                } ?: emptyList()
                
                updates.add(
                    LiveScoreUpdate(
                        matchId = match.id,
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
        if (scorers == null || scorers.toString() == "null") return emptyList()
        val s = scorers.toString().trim().replace("{", "").replace("}", "").replace("\"", "")
        if (s.isEmpty() || s == "[]") return emptyList()
        return s.split(",").mapNotNull { entry ->
            val clean = entry.trim()
            val minuteStr = clean.substringAfterLast(" ").replace("'", "")
            val minute = minuteStr.toIntOrNull() ?: return@mapNotNull null
            val name = clean.substringBeforeLast(" ").trim()
            if (name.isBlank() || name == "null") null else LiveScorer(name, minute)
        }
    }
}
