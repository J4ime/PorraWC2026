package com.porrawc2026.app.data.remote

import retrofit2.http.GET

data class EspnScoreboardResponse(
    val events: List<EspnEvent>?
)

data class EspnEvent(
    val id: String?,
    val date: String?,
    val name: String?,
    val shortName: String?,
    val competitions: List<EspnCompetition>?
)

data class EspnCompetition(
    val id: String?,
    val date: String?,
    val status: EspnStatus?,
    val competitors: List<EspnCompetitor>?,
    val details: List<EspnDetail>?
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
    val scoringPlay: Boolean?
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

interface EspnService {
    @GET("site/v2/sports/soccer/fifa.world/scoreboard")
    suspend fun getScoreboard(): EspnScoreboardResponse
}

object EspnConfig {
    const val BASE_URL = "https://site.api.espn.com/apis/"
}
