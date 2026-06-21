package com.porrawc2026.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class FootballMatch(
    val id: Int,
    val utcDate: String,
    val homeTeam: FootballTeam?,
    val awayTeam: FootballTeam?,
    val score: FootballScore?
)

data class FootballTeam(
    val id: Int?,
    val name: String?
)

data class FootballScore(
    val fullTime: FootballTimeScore?
)

data class FootballTimeScore(
    val home: Int?,
    val away: Int?
)

data class MatchesResponse(
    val matches: List<FootballMatch>
)

interface ApiService {
    @GET("competitions/WC/matches")
    suspend fun getWorldCupMatches(
        @Query("status") status: String? = null
    ): MatchesResponse
}

object ApiConfig {
    const val BASE_URL = "https://api.football-data.org/v4/"
}
