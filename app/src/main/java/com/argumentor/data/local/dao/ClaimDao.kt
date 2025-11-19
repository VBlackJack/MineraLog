package com.argumentor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.argumentor.data.local.entity.ClaimEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClaimDao {
    @Query("SELECT * FROM claims WHERE topicId = :topicId ORDER BY id DESC")
    fun observeClaimsByTopic(topicId: Long): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims")
    suspend fun getAll(): List<ClaimEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(claim: ClaimEntity): Long

    @Update
    suspend fun update(claim: ClaimEntity)

    @Delete
    suspend fun delete(claim: ClaimEntity)

    @Query("DELETE FROM claims WHERE id = :claimId")
    suspend fun deleteById(claimId: Long)
}
