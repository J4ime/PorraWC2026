package com.porrawc2026.app.data.local.dao

import androidx.room.*
import com.porrawc2026.app.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY id")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Update
    suspend fun updateQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions")
    suspend fun deleteAll()

    @Query("SELECT SUM(pointsEarned) FROM questions")
    suspend fun getTotalQuestionPoints(): Int
}
