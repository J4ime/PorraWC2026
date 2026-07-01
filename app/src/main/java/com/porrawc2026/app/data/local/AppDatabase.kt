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
    version = 11,
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
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN homeMissedPenalties INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE matches ADD COLUMN awayMissedPenalties INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN winnerTeam TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN homeHeadedGoals INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE matches ADD COLUMN awayHeadedGoals INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN hasSubGoal INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN homeShootoutScore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE matches ADD COLUMN awayShootoutScore INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN espnId TEXT DEFAULT NULL")
            }
        }
    }
}
