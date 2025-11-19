package com.argumentor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.argumentor.domain.model.EvidenceQuality
import com.argumentor.domain.model.EvidenceType

@Entity(
    tableName = "evidences",
    foreignKeys = [
        ForeignKey(
            entity = ClaimEntity::class,
            parentColumns = ["id"],
            childColumns = ["claimId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("claimId"), Index("sourceId")]
)
data class EvidenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val claimId: Long,
    val type: EvidenceType,
    val content: String,
    val sourceId: Long?,
    val quality: EvidenceQuality
)
