package com.porrawc2026.app.data.local.dao

import androidx.room.*
import com.porrawc2026.app.data.local.entity.KnockoutTeamProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnockoutTeamProgressDao {
    @Query("SELECT * FROM knockout_team_progress ORDER BY roundLevel, teamName")
    fun getAll(): Flow<List<KnockoutTeamProgressEntity>>

    @Query("SELECT teamName FROM knockout_team_progress WHERE roundLevel = :roundLevel")
    suspend fun getTeamsByRoundLevel(roundLevel: Int): List<String>

    @Transaction
    suspend fun replaceAll(entries: List<KnockoutTeamProgressEntity>) {
        deleteAll()
        insertAll(entries)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<KnockoutTeamProgressEntity>)

    @Query("DELETE FROM knockout_team_progress")
    suspend fun deleteAll()
}
