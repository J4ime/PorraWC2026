package com.porrawc2026.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knockout_predictions")
data class KnockoutPredictionEntity(
    @PrimaryKey val matchNumber: Int,
    val round: String,
    val homeTeamRef: String,
    val awayTeamRef: String,
    val winner: Int? = null,
    val pointsEarned: Int = 0
)
