package com.argumentor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.argumentor.data.local.entity.RebuttalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RebuttalDao {
    @Query(
        """
        SELECT r.* FROM rebuttals r
        INNER JOIN claims c ON c.id = r.claimId
        WHERE c.topicId = :topicId
        ORDER BY r.id DESC
        """
    )
    fun observeRebuttalsByTopic(topicId: Long): Flow<List<RebuttalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rebuttal: RebuttalEntity): Long

    @Update
    suspend fun update(rebuttal: RebuttalEntity)

    @Delete
    suspend fun delete(rebuttal: RebuttalEntity)

    @Query("DELETE FROM rebuttals WHERE id = :rebuttalId")
    suspend fun deleteById(rebuttalId: Long)

    @Query("SELECT * FROM rebuttals")
    suspend fun getAll(): List<RebuttalEntity>
}
