package com.porrawc2026.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class EspnScoreboardResponse(
    val events: List<EspnEvent>?
)

data class EspnEventsResponse(
    val league: EspnLeagueInfo?,
    val events: List<EspnSimpleEvent>?
)

data class EspnLeagueInfo(
    val name: String?,
    val abbreviation: String?
)

data class EspnSimpleEvent(
    val id: String?,
    val date: String?,
    val name: String?,
    val shortName: String?,
    val fullStatus: EspnSimpleStatus?,
    val competitors: List<EspnSimpleCompetitor>?,
    val neutralSite: Boolean? = false
)

data class EspnSimpleStatus(
    val clock: Double?,
    val displayClock: String?,
    val period: Int?,
    val displayPeriod: String?,
    val type: EspnStatusType?
)

data class EspnSimpleCompetitor(
    val id: String?,
    val homeAway: String?,
    val winner: Boolean? = false,
    val displayName: String?,
    val abbreviation: String?,
    val score: String?
)

data class EspnEvent(
    val id: String?,
    val date: String?,
    val name: String?,
    val shortName: String?,
    val competitions: List<EspnCompetition>?
)

data class EspnShootout(
    val team: EspnTeam?,
    val made: Int?,
    val attempts: Int?
)

data class EspnCompetition(
    val id: String?,
    val date: String?,
    val status: EspnStatus?,
    val competitors: List<EspnCompetitor>?,
    val details: List<EspnDetail>?,
    val shootout: List<EspnShootout>?
)

data class EspnStatus(
    val clock: Double?,
    val displayClock: String?,
    val period: Int?,
    val type: EspnStatusType?
)

data class EspnStatusType(
    val id: String?,
    val name: String?,
    val state: String?,
    val completed: Boolean?,
    val description: String?,
    val detail: String?,
    val shortDetail: String?
)

data class EspnCompetitor(
    val id: String?,
    val homeAway: String?,
    val score: String?,
    val team: EspnTeam?,
    val winner: Boolean?
)

data class EspnTeam(
    val id: String?,
    val name: String?,
    val displayName: String?,
    val shortDisplayName: String?,
    val abbreviation: String?
)

data class EspnDetail(
    val type: EspnDetailType?,
    val clock: EspnClock?,
    val team: EspnTeam?,
    val athletesInvolved: List<EspnAthlete>?,
    val scoringPlay: Boolean?,
    val yellowCard: Boolean?,
    val redCard: Boolean?,
    val ownGoal: Boolean?,
    val penaltyKick: Boolean?
)

data class EspnDetailType(
    val id: String?,
    val text: String?
)

data class EspnClock(
    val value: Double?,
    val displayValue: String?
)

data class EspnAthlete(
    val id: String?,
    val displayName: String?,
    val shortName: String?
)

data class EspnPlaysResponse(
    val items: List<EspnPlayItem>?
)

data class EspnPlayItem(
    val type: EspnDetailType?,
    val participants: List<EspnPlayParticipant>?
)

data class EspnPlayParticipant(
    val type: String?,
    val athlete: EspnAthlete?
)

interface EspnService {
    @GET("site/v2/sports/soccer/fifa.world/scoreboard")
    suspend fun getScoreboard(
        @Query("dates") dates: String? = null
    ): EspnScoreboardResponse

    @GET("site/v2/sports/soccer/fifa.world/events/{eventId}")
    suspend fun getEvent(
        @Path("eventId") eventId: String
    ): EspnEvent

    @GET("site/v2/sports/soccer/fifa.world/events")
    suspend fun getEvents(
        @Query("event") eventId: String? = null
    ): EspnEventsResponse

    @GET("https://sports.core.api.espn.com/v2/sports/soccer/leagues/fifa.world/events/{eventId}/competitions/{competitionId}/plays")
    suspend fun getPlays(
        @Path("eventId") eventId: String,
        @Path("competitionId") competitionId: String,
        @Query("limit") limit: Int = 300
    ): EspnPlaysResponse
}

object EspnConfig {
    const val BASE_URL = "https://site.api.espn.com/apis/"
}
