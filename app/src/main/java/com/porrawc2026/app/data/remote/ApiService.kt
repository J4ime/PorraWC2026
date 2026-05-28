package com.porrawc2026.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Header

data class FootballMatch(
    val id: Int,
    val utcDate: String,
    val status: String?,
    val matchday: Int?,
    val stage: String?,
    val group: String?,
    val homeTeam: FootballTeam?,
    val awayTeam: FootballTeam?,
    val score: FootballScore?
)

data class FootballTeam(
    val id: Int?,
    val name: String?,
    val shortName: String?,
    val tla: String?
)

data class FootballScore(
    val winner: String?,
    val duration: String?,
    val fullTime: FootballTimeScore?,
    val halfTime: FootballTimeScore?,
    val extraTime: FootballTimeScore?,
    val penalties: FootballTimeScore?
)

data class FootballTimeScore(
    val home: Int?,
    val away: Int?
)

data class MatchesResponse(
    val count: Int,
    val filters: Map<String, Any>?,
    val matches: List<FootballMatch>
)

data class StandingsResponse(
    val standings: List<StandingGroup>
)

data class StandingGroup(
    val stage: String?,
    val type: String?,
    val group: String?,
    val table: List<StandingTeam>
)

data class StandingTeam(
    val position: Int?,
    val team: FootballTeam?,
    val playedGames: Int?,
    val won: Int?,
    val draw: Int?,
    val lost: Int?,
    val goalsFor: Int?,
    val goalsAgainst: Int?,
    val goalDifference: Int?,
    val points: Int?
)

data class TeamsResponse(
    val count: Int,
    val teams: List<FootballTeamDetail>
)

data class FootballTeamDetail(
    val id: Int,
    val name: String?,
    val shortName: String?,
    val tla: String?,
    val crest: String?
)

interface ApiService {

    @GET("competitions/WC/matches")
    suspend fun getWorldCupMatches(
        @Header("X-Auth-Token") apiKey: String = "2a91da71f2384b659a3bf57e444eacd8",
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("stage") stage: String? = null,
        @Query("status") status: String? = null
    ): MatchesResponse

    @GET("competitions/WC/standings")
    suspend fun getWorldCupStandings(
        @Header("X-Auth-Token") apiKey: String = "YOUR_API_KEY_HERE"
    ): StandingsResponse

    @GET("competitions/WC/teams")
    suspend fun getWorldCupTeams(
        @Header("X-Auth-Token") apiKey: String = "YOUR_API_KEY_HERE"
    ): TeamsResponse

    @GET("matches/{id}")
    suspend fun getMatchDetail(
        @Header("X-Auth-Token") apiKey: String = "2a91da71f2384b659a3bf57e444eacd8",
        @Path("id") matchId: Int
    ): FootballMatch
}

object ApiConfig {
    const val BASE_URL = "https://api.football-data.org/v4/"
    const val COMPETITION_ID = "WC"
}
