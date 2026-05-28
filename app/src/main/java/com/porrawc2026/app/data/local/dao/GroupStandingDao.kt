package com.porrawc2026.app.data.local.dao

import androidx.room.*
import com.porrawc2026.app.data.local.entity.GroupStandingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupStandingDao {
    @Query("SELECT * FROM group_standings ORDER BY groupLetter, position")
    fun getAll(): Flow<List<GroupStandingEntity>>

    @Query("SELECT * FROM group_standings WHERE groupLetter = :group ORDER BY position")
    fun getByGroup(group: String): Flow<List<GroupStandingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(standings: List<GroupStandingEntity>)

    @Query("DELETE FROM group_standings")
    suspend fun deleteAll()
}
