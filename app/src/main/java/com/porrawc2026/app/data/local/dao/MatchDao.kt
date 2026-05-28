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
}
