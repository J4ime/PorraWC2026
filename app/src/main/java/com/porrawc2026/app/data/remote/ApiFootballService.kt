package com.porrawc2026.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class ApiFootballFixtureResponse(
    val results: Int,
    val response: List<ApiFootballFixture>
)

data class ApiFootballFixture(
    val fixture: ApiFootballFixtureInfo,
    val league: ApiFootballLeague,
    val teams: ApiFootballTeams,
    val goals: ApiFootballGoals,
    val score: ApiFootballScore,
    val events: List<ApiFootballEvent>? = null
)

data class ApiFootballFixtureInfo(
    val id: Long,
    val date: String?,
    val status: ApiFootballStatus?
)

data class ApiFootballStatus(
    val long: String?,
    val short: String?,
    val elapsed: Int?
)

data class ApiFootballLeague(
    val id: Int,
    val name: String?
)

data class ApiFootballTeams(
    val home: ApiFootballTeamInfo,
    val away: ApiFootballTeamInfo
)

data class ApiFootballTeamInfo(
    val id: Int?,
    val name: String?
)

data class ApiFootballGoals(
    val home: Int?,
    val away: Int?
)

data class ApiFootballScore(
    val halftime: ApiFootballScoreDetail?,
    val fulltime: ApiFootballScoreDetail?,
    val extratime: ApiFootballScoreDetail?,
    val penalty: ApiFootballScoreDetail?
)

data class ApiFootballScoreDetail(
    val home: Int?,
    val away: Int?
)

data class ApiFootballEvent(
    val time: ApiFootballEventTime?,
    val team: ApiFootballTeamInfo?,
    val player: ApiFootballPlayerInfo?,
    val assist: ApiFootballPlayerInfo?,
    val type: String?,
    val detail: String?
)

data class ApiFootballEventTime(
    val elapsed: Int?,
    val extra: Int?
)

data class ApiFootballPlayerInfo(
    val id: Int?,
    val name: String?
)

interface ApiFootballService {
    @GET("fixtures")
    suspend fun getFixtures(
        @Header("x-apisports-key") apiKey: String = "11ad7e440abb1ffc7f7d97ca5c3d23e3",
        @Query("date") date: String
    ): ApiFootballFixtureResponse

    @GET("fixtures/statistics")
    suspend fun getFixtureStatistics(
        @Header("x-apisports-key") apiKey: String = "11ad7e440abb1ffc7f7d97ca5c3d23e3",
        @Query("fixture") fixture: Long
    ): ApiFootballStatsResponse
}

data class ApiFootballStatsResponse(
    val results: Int,
    val response: List<ApiFootballTeamStats>
)

data class ApiFootballTeamStats(
    val team: ApiFootballTeamInfo?,
    val statistics: List<ApiFootballStatItem>
)

data class ApiFootballStatItem(
    val type: String?,
    val value: Any?
)

object ApiFootballConfig {
    const val BASE_URL = "https://v3.football.api-sports.io/"
}
