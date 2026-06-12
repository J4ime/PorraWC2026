package com.porrawc2026.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class ZafronixMatchesResponse(
    val year: Int?,
    val count: Int,
    val data: List<ZafronixMatch>
)

data class ZafronixMatch(
    val id: String?,
    val date: String?,
    val kickoff: String?,
    val stage: String?,
    val homeTeam: String?,
    val awayTeam: String?,
    val homeScore: Int?,
    val awayScore: Int?,
    val result: String?,
    val extraTime: Boolean? = false
)

interface ZafronixService {
    @GET("matches")
    suspend fun getMatches(
        @Header("X-API-Key") apiKey: String = "zwc_free_90b800ad63b19b2d4c60c23d",
        @Query("year") year: Int = 2026
    ): ZafronixMatchesResponse
}

object ZafronixConfig {
    const val BASE_URL = "https://api.zafronix.com/fifa/worldcup/v1/"
}
