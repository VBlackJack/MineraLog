package com.argumentor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.argumentor.data.local.entity.EvidenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvidenceDao {
    @Query(
        """
        SELECT e.* FROM evidences e
        INNER JOIN claims c ON c.id = e.claimId
        WHERE c.topicId = :topicId
        ORDER BY e.id DESC
        """
    )
    fun observeEvidencesByTopic(topicId: Long): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidences WHERE claimId = :claimId ORDER BY id DESC")
    fun observeEvidencesByClaim(claimId: Long): Flow<List<EvidenceEntity>>

    @Query("SELECT * FROM evidences")
    suspend fun getAll(): List<EvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(evidence: EvidenceEntity): Long

    @Update
    suspend fun update(evidence: EvidenceEntity)

    @Delete
    suspend fun delete(evidence: EvidenceEntity)

    @Query("DELETE FROM evidences WHERE id = :evidenceId")
    suspend fun deleteById(evidenceId: Long)
}
