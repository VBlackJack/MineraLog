package net.meshcore.mineralog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing storage location for a mineral specimen.
 * Supports hierarchical location: Place → Container → Box → Slot
 */
@Entity(
    tableName = "storage",
    foreignKeys = [
        ForeignKey(
            entity = MineralEntity::class,
            parentColumns = ["id"],
            childColumns = ["mineralId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mineralId"], unique = true),
        Index(value = ["place"]),
        Index(value = ["container"]),
        Index(value = ["box"]),
        Index(value = ["nfcTagId"])
    ]
)
data class StorageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val mineralId: String,

    // Hierarchical location
    val place: String? = null,      // e.g., "Living room", "Office", "Basement"
    val container: String? = null,  // e.g., "Cabinet A", "Display case 1"
    val box: String? = null,        // e.g., "Box 1", "Drawer 3"
    val slot: String? = null,       // e.g., "A1", "Slot 12"

    // Optional identifiers
    val nfcTagId: String? = null,
    val qrContent: String? = null   // QR code content for physical label
)
