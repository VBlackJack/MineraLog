package com.argumentor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = ClaimEntity::class,
            parentColumns = ["id"],
            childColumns = ["claimId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("claimId")]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val claimId: Long,
    val prompt: String,
    val expectedAnswer: String
)
