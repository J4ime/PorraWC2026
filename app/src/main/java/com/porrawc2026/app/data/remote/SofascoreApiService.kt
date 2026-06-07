package com.porrawc2026.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

data class SofascoreTopPlayersResponse(
    val topPlayers: SofascoreTopPlayers?
)

data class SofascoreTopPlayers(
    val goals: List<SofascoreTopPlayer>?
)

data class SofascoreTopPlayer(
    val player: SofascorePlayer?,
    val statistics: SofascoreStats?,
    val playedMatches: Int?,
    val position: Int?
)

data class SofascorePlayer(
    val id: Int?,
    val name: String?,
    val slug: String?,
    val shortName: String?,
    val country: SofascoreCountry?
)

data class SofascoreCountry(
    val name: String?,
    val alpha2: String?
)

data class SofascoreStats(
    val goals: Int?,
    val assists: Int?,
    val totalShots: Int?,
    val shotsOnTarget: Int?,
    val minutesPlayed: Int?
)

interface SofascoreApiService {

    @GET("unique-tournament/{tournamentId}/season/{seasonId}/top-players/overall")
    suspend fun getTopPlayers(
        @Path("tournamentId") tournamentId: Int = 16,
        @Path("seasonId") seasonId: Int = 56966
    ): SofascoreTopPlayersResponse
}

object SofascoreConfig {
    const val BASE_URL = "https://api.sofascore.com/api/v1/"
    const val WC_UNIQUE_TOURNAMENT_ID = 16
    const val WC_2026_SEASON_ID = 56966
}
