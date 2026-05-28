package com.porrawc2026.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.porrawc2026.app.data.local.dao.*
import com.porrawc2026.app.data.local.entity.*

@Database(
    entities = [
        TeamEntity::class,
        MatchEntity::class,
        QuestionEntity::class,
        PlayerPredictionEntity::class,
        KnockoutPredictionEntity::class,
        GroupStandingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun matchDao(): MatchDao
    abstract fun questionDao(): QuestionDao
    abstract fun playerPredictionDao(): PlayerPredictionDao
    abstract fun knockoutPredictionDao(): KnockoutPredictionDao
    abstract fun groupStandingDao(): GroupStandingDao
}
