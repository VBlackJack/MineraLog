package com.argumentor.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.argumentor.domain.model.RebuttalStyle

@Entity(
    tableName = "rebuttals",
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
data class RebuttalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val claimId: Long,
    val text: String,
    val style: RebuttalStyle
)
