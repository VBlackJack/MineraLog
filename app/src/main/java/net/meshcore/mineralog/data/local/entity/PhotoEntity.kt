package net.meshcore.mineralog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Room entity representing a photo of a mineral specimen.
 * Supports multiple photo types (normal, UV shortwave, UV longwave, macro).
 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = MineralEntity::class,
            parentColumns = ["id"],
            childColumns = ["mineralId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mineralId"]),
        Index(value = ["type"]),
        Index(value = ["takenAt"])
    ]
)
data class PhotoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val mineralId: String,

    val type: PhotoType = PhotoType.NORMAL,
    val caption: String? = null,
    val takenAt: Instant = Instant.now(),

    // File path relative to app's files directory: "media/{mineralId}/{filename}"
    val fileName: String
)

enum class PhotoType {
    NORMAL,
    UV_SW,      // UV Shortwave
    UV_LW,      // UV Longwave
    MACRO
}
