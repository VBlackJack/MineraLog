package com.argumentor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.argumentor.data.local.entity.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query(
        """
        SELECT q.* FROM questions q
        INNER JOIN claims c ON c.id = q.claimId
        WHERE c.topicId = :topicId
        ORDER BY q.id DESC
        """
    )
    fun observeQuestionsByTopic(topicId: Long): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(question: QuestionEntity): Long

    @Update
    suspend fun update(question: QuestionEntity)

    @Delete
    suspend fun delete(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :questionId")
    suspend fun deleteById(questionId: Long)

    @Query("SELECT * FROM questions")
    suspend fun getAll(): List<QuestionEntity>
}
