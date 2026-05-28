package com.porrawc2026.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: Int,
    val text: String,
    val predictedAnswer: Boolean? = null,
    val correctAnswer: Boolean? = null,
    val pointsEarned: Int = 0
)
