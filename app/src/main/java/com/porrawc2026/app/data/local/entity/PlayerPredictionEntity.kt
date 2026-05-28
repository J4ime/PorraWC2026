package com.porrawc2026.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_predictions")
data class PlayerPredictionEntity(
    @PrimaryKey val rank: Int,
    val playerName: String,
    val predictedName: String? = null,
    val goalsScored: Int = 0,
    val pointsPerGoal: Int = when (rank) {
        1 -> 50
        2 -> 30
        3 -> 10
        else -> 0
    },
    val pointsEarned: Int = 0
)
