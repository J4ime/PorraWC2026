package com.porrawc2026.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knockout_team_progress")
data class KnockoutTeamProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val roundLevel: Int,
    val roundName: String,
    val teamName: String
)
