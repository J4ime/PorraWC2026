package com.porrawc2026.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: Int,
    val groupName: String,
    val matchday: String,
    val dateTime: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeGoals: Int? = null,
    val awayGoals: Int? = null,
    val predictedHomeGoals: Int? = null,
    val predictedAwayGoals: Int? = null,
    val isKnockout: Boolean = false,
    val knockoutRound: String? = null,
    val matchNumber: Int? = null,
    val pointsEarned: Int = 0,
    val tvChannel: String = ""
)
