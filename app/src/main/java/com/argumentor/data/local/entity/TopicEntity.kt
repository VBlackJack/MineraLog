package com.argumentor.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey
import com.argumentor.domain.model.TopicStance

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val summary: String,
    val stance: TopicStance,
    val color: Long,
    val createdAt: Long
)

@Fts4(contentEntity = TopicEntity::class)
@Entity(tableName = "topics_fts")
data class TopicFtsEntity(
    val title: String,
    val summary: String
)
