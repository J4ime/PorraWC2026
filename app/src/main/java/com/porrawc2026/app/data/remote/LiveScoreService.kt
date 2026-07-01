package com.porrawc2026.app.data.remote

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import com.porrawc2026.app.util.LogManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.coroutineScope
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
    val awayRedCards: Int = 0,
    val homeMissedPenalties: Int = 0,
    val awayMissedPenalties: Int = 0,
    val winnerTeam: String? = null,
    val homeHeadedGoals: Int = 0,
    val awayHeadedGoals: Int = 0,
    val hasSubGoal: Boolean = false,
    val homeShootoutScore: Int = 0,
    val awayShootoutScore: Int = 0,
    val apiHomeTeam: String? = null,
    val apiAwayTeam: String? = null
)

data class LiveScorer(val playerName: String, val minute: Int, val minuteLabel: String? = null)

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
        val allUpdates = mutableListOf<LiveScoreUpdate>()
        val dateGroups = groupMatchesByDate(matches)
        val dateFmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
        for ((dateStr, dayMatches) in dateGroups) {
            val dateObj = try { java.time.LocalDate.parse(dateStr, dateFmt) } catch (e: Exception) { LogManager.log("LiveScoreService", "Failed to parse date group $dateStr", e); continue }
            val rangeStart = dateObj.minusDays(1).format(dateFmt)
            val rangeEnd = dateObj.plusDays(1).format(dateFmt)
            val scoreboard = espnService.getScoreboard(dates = "$rangeStart-$rangeEnd")
            val events = scoreboard.events ?: continue
            // Use the FULL matches list (not just dayMatches) for lookup, so timezone-shifted events match
            val parsed = coroutineScope { events.mapNotNull { parseEvent(it, matches) } }
            allUpdates.addAll(parsed)
        }
        return allUpdates
    }

    private fun groupMatchesByDate(matches: List<MatchEntity>): Map<String, List<MatchEntity>> {
        return matches.groupBy { m ->
            try { m.dateTime.take(10).replace("-", "") } catch (e: Exception) { LogManager.log("LiveScoreService", "Failed to parse dateTime for match ${m.id}", e); "" }
        }.filterKeys { it.isNotBlank() }.mapValues { (_, list) -> list.sortedBy { it.id } }
    }

    private fun buildDateRange(matches: List<MatchEntity>): String? {
        val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
        val minDate = matches.minOfOrNull { it.dateTime.take(10).replace("-", "") }
            ?.ifBlank { null } ?: return null
        val maxDate = matches.maxOfOrNull { it.dateTime.take(10).replace("-", "") }
            ?.ifBlank { null } ?: return null
        val minLocal = try { java.time.LocalDate.parse(minDate, fmt).minusDays(1) } catch (e: Exception) { LogManager.log("LiveScoreService", "buildDateRange: failed to parse minDate=$minDate", e); return null }
        val maxLocal = try { java.time.LocalDate.parse(maxDate, fmt).plusDays(1) } catch (e: Exception) { LogManager.log("LiveScoreService", "buildDateRange: failed to parse maxDate=$maxDate", e); return null }
        return "${minLocal.format(fmt)}-${maxLocal.format(fmt)}"
    }

    private suspend fun parseEvent(event: EspnEvent, matches: List<MatchEntity>): LiveScoreUpdate? {
        val competition = event.competitions?.firstOrNull() ?: return null
        val status = competition.status ?: return null
        val competitors = competition.competitors ?: return null

        val homeTeam = competitors.firstOrNull { it.homeAway == "home" } ?: return null
        val awayTeam = competitors.firstOrNull { it.homeAway == "away" } ?: return null

        val homeName = homeTeam.team?.displayName ?: homeTeam.team?.name ?: return null
        val awayName = awayTeam.team?.displayName ?: awayTeam.team?.name ?: return null

        var entity = findMatchingMatch(matches, homeName, awayName)
        if (entity == null) {
            entity = findMatchByDate(competition.date ?: event.date, event.name, matches)
            if (entity == null) {
                LogManager.log("LiveScoreService", "No match found for $homeName vs $awayName (event=${event.name})")
                return null
            }
        }

        val hScore = homeTeam.score?.toIntOrNull() ?: 0
        val aScore = awayTeam.score?.toIntOrNull() ?: 0
        val minute = computeMinute(status)
        val (homeScorers, awayScorers, hYellows, aYellows, hReds, aReds, hMissedPens, aMissedPens, hHeaded, aHeaded) = parseDetails(competition.details, homeTeam.id)

        val isFinished = status.type?.completed == true
        val winnerName = if (isFinished) {
            competitors.firstOrNull { it.winner == true }?.team?.displayName
        } else null

        val hasSubGoal = if (isFinished && entity.knockoutRound == "Semifinales") {
            checkSubstituteGoals(event, competition)
        } else false

        val (homeShootout, awayShootout) = parseShootout(competition.shootout, homeTeam.id, awayTeam.id)

        return LiveScoreUpdate(
            matchId = entity.id, homeGoals = hScore, awayGoals = aScore,
            homeScorers = homeScorers, awayScorers = awayScorers,
            isFinished = isFinished, liveMinute = minute,
            homeYellowCards = hYellows, awayYellowCards = aYellows,
            homeRedCards = hReds, awayRedCards = aReds,
            homeMissedPenalties = hMissedPens, awayMissedPenalties = aMissedPens,
            winnerTeam = winnerName,
            homeHeadedGoals = hHeaded, awayHeadedGoals = aHeaded,
            hasSubGoal = hasSubGoal,
            homeShootoutScore = homeShootout,
            awayShootoutScore = awayShootout,
            apiHomeTeam = homeName,
            apiAwayTeam = awayName
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
        var hMissedPens = 0; var aMissedPens = 0
        var hHeaded = 0; var aHeaded = 0
        details?.forEach { detail ->
            if (detail.scoringPlay == true) {
                // Skip penalty shootout goals: no match clock (displayValue "0'") + penaltyKick
                if (detail.penaltyKick == true && detail.clock?.displayValue == "0'") return@forEach
                val playerNameBase = detail.athletesInvolved?.firstOrNull()?.displayName ?: return@forEach
                val playerName = if (detail.ownGoal == true) "$playerNameBase (OG)" else playerNameBase
                val minuteStr = detail.clock?.displayValue ?: return@forEach
                val m = minuteRegex.find(minuteStr)
                val goalMinute = if (m != null) {
                    (m.groupValues[1].toIntOrNull() ?: 0) + (m.groupValues[3].toIntOrNull() ?: 0)
                } else 0
                val minuteLabel = m?.let {
                    val base = it.groupValues[1]
                    val extra = it.groupValues[3]
                    if (extra.isNotEmpty()) "${base}+${extra}" else base
                } ?: minuteStr
                val isHome = detail.team?.id == homeTeamId
                if (isHome) homeScorers.add(LiveScorer(playerName, goalMinute, minuteLabel))
                else awayScorers.add(LiveScorer(playerName, goalMinute, minuteLabel))
                if (detail.type?.id == "137") {
                    if (isHome) hHeaded++ else aHeaded++
                }
            }
            if (detail.yellowCard == true) { if (detail.team?.id == homeTeamId) hYellows++ else aYellows++ }
            if (detail.redCard == true) { if (detail.team?.id == homeTeamId) hReds++ else aReds++ }
            if (detail.penaltyKick == true && detail.scoringPlay == false) {
                if (detail.team?.id == homeTeamId) hMissedPens++ else aMissedPens++
            }
        }
        return ParsedDetails(homeScorers, awayScorers, hYellows, aYellows, hReds, aReds, hMissedPens, aMissedPens, hHeaded, aHeaded)
    }

    private data class ParsedDetails(
        val homeScorers: List<LiveScorer>, val awayScorers: List<LiveScorer>,
        val homeYellowCards: Int, val awayYellowCards: Int,
        val homeRedCards: Int, val awayRedCards: Int,
        val homeMissedPenalties: Int, val awayMissedPenalties: Int,
        val homeHeadedGoals: Int, val awayHeadedGoals: Int
    )

    private suspend fun checkSubstituteGoals(event: EspnEvent, competition: EspnCompetition): Boolean {
        val eventId = event.id ?: return false
        val compId = competition.id ?: return false
        return try {
            val plays = espnService.getPlays(eventId, compId)
            val items = plays.items ?: return false

            val subbedInIds = mutableSetOf<String>()
            val scorerIds = mutableSetOf<String>()

            for (play in items) {
                val pid = play.type?.id ?: continue
                if (pid == "76") {
                    play.participants?.forEach { p ->
                        if (p.type == "subbed-in") p.athlete?.id?.let { subbedInIds.add(it) }
                    }
                }
                play.participants?.forEach { p ->
                    if (p.type == "scorer") p.athlete?.id?.let { scorerIds.add(it) }
                }
            }

            scorerIds.any { it in subbedInIds }
        } catch (e: Exception) {
            LogManager.log("LiveScoreService", "Error checking substitute goals for event $eventId", e)
            false
        }
    }

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

    private fun findMatchByDate(eventDate: String?, eventName: String?, matches: List<MatchEntity>): MatchEntity? {
        if (eventDate == null) return null
        val eventInstant = parseEspnDate(eventDate) ?: return null
        val eventGroup = eventName?.let { extractGroup(it) }

        for (match in matches) {
            val matchInstant = parseMadridDate(match.dateTime) ?: continue
            val diff = Math.abs(matchInstant.toEpochMilli() - eventInstant.toEpochMilli())
            if (diff >= 45 * 60 * 1000L) continue
            if (eventGroup != null && !match.groupName.contains(eventGroup, ignoreCase = true)) continue
            return match
        }
        return null
    }

    private fun extractGroup(eventName: String): String? {
        val g = Regex("""[Gg]roup\s+([A-Z])""").find(eventName)?.groupValues?.getOrNull(1) ?: return null
        return g
    }

    private fun parseEspnDate(date: String): java.time.Instant? {
        return try {
            java.time.Instant.parse(date)
        } catch (e: java.time.format.DateTimeParseException) {
            try {
                java.time.OffsetDateTime.parse(date).toInstant()
            } catch (e2: java.time.format.DateTimeParseException) {
                null
            }
        }
    }

    private fun parseMadridDate(date: String): java.time.Instant? {
        return try {
            java.time.LocalDateTime.parse(date, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(java.time.ZoneId.of("Europe/Madrid")).toInstant()
        } catch (e: java.time.format.DateTimeParseException) {
            null
        }
    }

    private fun findMatchingMatch(matches: List<MatchEntity>, homeName: String, awayName: String): MatchEntity? {
        val candidates = matches.filter {
            TeamNameNormalizer.matches(it.homeTeam, homeName) &&
            TeamNameNormalizer.matches(it.awayTeam, awayName)
        }
        if (candidates.size == 1) return candidates.first()
        val swapped = matches.filter {
            TeamNameNormalizer.matches(it.homeTeam, awayName) &&
            TeamNameNormalizer.matches(it.awayTeam, homeName)
        }
        if (swapped.size == 1) return swapped.first()
        return null
    }

    private fun parseShootout(shootout: List<EspnShootout>?, homeId: String?, awayId: String?): Pair<Int, Int> {
        if (shootout == null) return 0 to 0
        val homeMade = shootout.firstOrNull { it.team?.id == homeId }?.made ?: 0
        val awayMade = shootout.firstOrNull { it.team?.id == awayId }?.made ?: 0
        return homeMade to awayMade
    }

    companion object {
        private val minuteRegex = Regex("""(\d+)'(\+(\d+))?""")
        private val gson = Gson()
        private val scorerListType = object : TypeToken<List<LiveScorer>>() {}.type
    }
}
