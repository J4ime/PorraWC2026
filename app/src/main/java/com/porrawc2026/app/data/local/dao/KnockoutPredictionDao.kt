package com.porrawc2026.app.data.local.dao

import androidx.room.*
import com.porrawc2026.app.data.local.entity.KnockoutPredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KnockoutPredictionDao {
    @Query("SELECT * FROM knockout_predictions ORDER BY matchNumber")
    fun getAll(): Flow<List<KnockoutPredictionEntity>>

    @Query("SELECT * FROM knockout_predictions WHERE round = :round ORDER BY matchNumber")
    fun getByRound(round: String): Flow<List<KnockoutPredictionEntity>>

    @Update
    suspend fun update(prediction: KnockoutPredictionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(predictions: List<KnockoutPredictionEntity>)

    @Query("DELETE FROM knockout_predictions")
    suspend fun deleteAll()

    @Query("SELECT SUM(pointsEarned) FROM knockout_predictions")
    suspend fun getTotalPoints(): Int

    @Query("UPDATE knockout_predictions SET pointsEarned = :points WHERE matchNumber = :matchNumber")
    suspend fun updatePoints(matchNumber: Int, points: Int)
}
