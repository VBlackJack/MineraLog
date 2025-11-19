package com.argumentor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import com.argumentor.data.local.entity.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT t.id, t.title, t.summary, t.stance, t.color, t.createdAt,
               COUNT(c.id) AS claimCount,
               SUM(CASE WHEN c.position = 'SUPPORT' THEN 1 ELSE 0 END) AS supportCount,
               SUM(CASE WHEN c.position = 'CHALLENGE' THEN 1 ELSE 0 END) AS challengeCount
        FROM topics t
        LEFT JOIN claims c ON c.topicId = t.id
        GROUP BY t.id
        ORDER BY t.createdAt DESC
        """
    )
    fun observeTopicOverview(): Flow<List<TopicOverviewProjection>>

    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT t.id, t.title, t.summary, t.stance, t.color, t.createdAt,
               COUNT(c.id) AS claimCount,
               SUM(CASE WHEN c.position = 'SUPPORT' THEN 1 ELSE 0 END) AS supportCount,
               SUM(CASE WHEN c.position = 'CHALLENGE' THEN 1 ELSE 0 END) AS challengeCount
        FROM topics t
        JOIN topics_fts fts ON fts.rowid = t.id
        LEFT JOIN claims c ON c.topicId = t.id
        WHERE topics_fts MATCH :query
        GROUP BY t.id
        ORDER BY t.createdAt DESC
        """
    )
    fun searchTopicOverview(query: String): Flow<List<TopicOverviewProjection>>

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    fun observeTopic(topicId: Long): Flow<TopicEntity?>

    @Query("SELECT * FROM topics")
    suspend fun getAll(): List<TopicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(topic: TopicEntity): Long

    @Update
    suspend fun update(topic: TopicEntity)

    @Delete
    suspend fun delete(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :topicId")
    suspend fun deleteById(topicId: Long)
}

data class TopicOverviewProjection(
    val id: Long,
    val title: String,
    val summary: String,
    val stance: String,
    val color: Long,
    val createdAt: Long,
    val claimCount: Int,
    val supportCount: Int?,
    val challengeCount: Int?
)
