package com.porrawc2026.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun matchDao(): MatchDao
    abstract fun questionDao(): QuestionDao
    abstract fun playerPredictionDao(): PlayerPredictionDao
    abstract fun knockoutPredictionDao(): KnockoutPredictionDao
    abstract fun groupStandingDao(): GroupStandingDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE player_predictions ADD COLUMN photoPath TEXT")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN homeRedCards INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE matches ADD COLUMN awayRedCards INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE matches ADD COLUMN homeYellowCards INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE matches ADD COLUMN awayYellowCards INTEGER DEFAULT NULL")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN homeScorers TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE matches ADD COLUMN awayScorers TEXT DEFAULT NULL")
            }
        }
    }
}
