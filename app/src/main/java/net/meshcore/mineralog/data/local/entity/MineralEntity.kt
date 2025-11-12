package net.meshcore.mineralog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Room entity representing a mineral specimen.
 * Stores all mineralogical properties and metadata.
 */
@Entity(
    tableName = "minerals",
    indices = [
        Index(value = ["name"]),
        Index(value = ["group"]),
        Index(value = ["crystalSystem"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class MineralEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Basic info
    val name: String,
    val group: String? = null,
    val formula: String? = null,
    val crystalSystem: String? = null, // Triclinic, Monoclinic, Orthorhombic, Tetragonal, Trigonal, Hexagonal, Cubic

    // Physical properties
    val mohsMin: Float? = null, // 1.0 - 10.0
    val mohsMax: Float? = null, // 1.0 - 10.0
    val cleavage: String? = null,
    val fracture: String? = null,
    val luster: String? = null, // Metallic, Vitreous, Pearly, Resinous, Silky, Greasy, Dull
    val streak: String? = null,
    val diaphaneity: String? = null, // Transparent, Translucent, Opaque
    val habit: String? = null,
    val specificGravity: Float? = null,

    // Special properties
    val fluorescence: String? = null, // Format: "LW:blue,SW:green" or "none"
    val magnetic: Boolean = false,
    val radioactive: Boolean = false,

    // Physical measurements
    val dimensionsMm: String? = null, // Format: "length x width x height" or free text
    val weightGr: Float? = null,

    // User notes and categorization
    val notes: String? = null,
    val tags: String? = null, // Comma-separated tags

    // Status
    val status: String = "incomplete", // complete, incomplete

    // Metadata
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
