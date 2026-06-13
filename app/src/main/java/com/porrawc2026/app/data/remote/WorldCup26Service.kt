package com.porrawc2026.app.data.remote

import retrofit2.http.GET

data class WorldCup26GamesResponse(
    val games: List<WorldCup26Game>
)

data class WorldCup26Game(
    val id: String?,
    val home_team_name_en: String?,
    val away_team_name_en: String?,
    val home_score: String?,
    val away_score: String?,
    val home_scorers: Any?,  // String or JSON array
    val away_scorers: Any?,
    val finished: String?,
    val time_elapsed: String?,
    val group: String?,
    val type: String?,
    val local_date: String?
)

interface WorldCup26Service {
    @GET("get/games")
    suspend fun getGames(): WorldCup26GamesResponse
}

object WorldCup26Config {
    const val BASE_URL = "https://worldcup26.ir/"
}
