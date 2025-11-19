package com.argumentor.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.argumentor.domain.model.ArgumentStrength
import com.argumentor.domain.model.ClaimPosition

@Entity(
    tableName = "claims",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("topicId")]
)
data class ClaimEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val text: String,
    val position: ClaimPosition,
    val strength: ArgumentStrength
)

@Fts4(contentEntity = ClaimEntity::class)
@Entity(tableName = "claims_fts")
data class ClaimFtsEntity(
    val text: String
)
