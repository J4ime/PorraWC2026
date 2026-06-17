package com.porrawc2026.app.data.local.dao

import androidx.room.*
import com.porrawc2026.app.data.local.entity.MatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches WHERE isKnockout = 0 ORDER BY id")
    fun getAllGroupMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE groupName = :group AND isKnockout = 0 ORDER BY id")
    fun getGroupMatches(group: String): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE isKnockout = 1 ORDER BY matchNumber")
    fun getKnockoutMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches ORDER BY id")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(matches: List<MatchEntity>)

    @Query("DELETE FROM matches")
    suspend fun deleteAll()

    @Query("SELECT SUM(pointsEarned) FROM matches")
    suspend fun getTotalMatchPoints(): Int

    @Query("UPDATE matches SET homeGoals = :homeGoals, awayGoals = :awayGoals WHERE id = :matchId")
    suspend fun updateMatchResult(matchId: Int, homeGoals: Int, awayGoals: Int)

    @Query("UPDATE matches SET homeRedCards = :homeReds, awayRedCards = :awayReds, homeYellowCards = :homeYellows, awayYellowCards = :awayYellows WHERE id = :matchId")
    suspend fun updateMatchCards(matchId: Int, homeReds: Int, awayReds: Int, homeYellows: Int, awayYellows: Int)

    @Query("UPDATE matches SET homeScorers = :homeScorers, awayScorers = :awayScorers WHERE id = :matchId")
    suspend fun updateMatchScorers(matchId: Int, homeScorers: String?, awayScorers: String?)

    @Query("UPDATE matches SET homeScorers = NULL, awayScorers = NULL, homeGoals = NULL, awayGoals = NULL")
    suspend fun clearAllMatchScores()
}
