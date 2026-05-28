package com.porrawc2026.app.data.local.dao

import androidx.room.*
import com.porrawc2026.app.data.local.entity.PlayerPredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerPredictionDao {
    @Query("SELECT * FROM player_predictions ORDER BY rank")
    fun getAll(): Flow<List<PlayerPredictionEntity>>

    @Update
    suspend fun update(prediction: PlayerPredictionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(predictions: List<PlayerPredictionEntity>)

    @Query("DELETE FROM player_predictions")
    suspend fun deleteAll()

    @Query("SELECT SUM(pointsEarned) FROM player_predictions")
    suspend fun getTotalPoints(): Int
}
