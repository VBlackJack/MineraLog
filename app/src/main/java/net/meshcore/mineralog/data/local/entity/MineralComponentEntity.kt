package net.meshcore.mineralog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Room entity representing a mineral component within an aggregate.
 *
 * This entity stores information about individual minerals that make up an aggregate
 * (e.g., Quartz in Granite). Each component includes its own mineralogical properties,
 * percentage composition, and role in the aggregate.
 *
 * The entity has a many-to-one relationship with MineralEntity via the aggregateId foreign key.
 * When an aggregate is deleted, all its components are automatically deleted via CASCADE.
 *
 * @property id Unique identifier for this component entry.
 * @property aggregateId Foreign key to the parent aggregate MineralEntity.
 * @property displayOrder Order in which this component should be displayed (0-based index).
 * @property mineralName Name of the mineral component (e.g., "Quartz", "Feldspath").
 * @property mineralGroup Mineral group/class (e.g., "Silicates", "Sulfides").
 * @property percentage Volumetric or mass percentage of this component in the aggregate (0-100).
 * @property role Role/importance of this component (PRINCIPAL, ACCESSORY, or TRACE).
 * @property mohsMin Minimum hardness of this component on Mohs scale (1.0 - 10.0).
 * @property mohsMax Maximum hardness of this component on Mohs scale (1.0 - 10.0).
 * @property density Specific gravity/density of this component in g/cm³.
 * @property formula Chemical formula of this component (e.g., "SiO₂", "KAlSi₃O₈").
 * @property crystalSystem Crystal system of this component.
 * @property luster Surface luster of this component.
 * @property diaphaneity Transparency of this component.
 * @property cleavage Cleavage description of this component.
 * @property fracture Fracture type of this component.
 * @property habit Crystal habit/form of this component.
 * @property streak Color of powdered component.
 * @property fluorescence Fluorescence behavior of this component.
 * @property notes Additional notes specific to this component.
 * @property createdAt Timestamp when this component was created.
 * @property updatedAt Timestamp when this component was last updated.
 */
@Entity(
    tableName = "mineral_components",
    foreignKeys = [
        ForeignKey(
            entity = MineralEntity::class,
            parentColumns = ["id"],
            childColumns = ["aggregateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["aggregateId"]),
        Index(value = ["role"]),
        Index(value = ["mineralName"]),
        Index(value = ["displayOrder"])
    ]
)
data class MineralComponentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Relationship
    val aggregateId: String,
    val displayOrder: Int,

    // Component identification
    val mineralName: String,
    val mineralGroup: String? = null,

    // Composition
    val percentage: Float? = null,
    val role: String, // ComponentRole enum value: "PRINCIPAL", "ACCESSORY", "TRACE"

    // Physical properties
    val mohsMin: Float? = null,
    val mohsMax: Float? = null,
    val density: Float? = null,

    // Chemical properties
    val formula: String? = null,

    // Crystallographic properties
    val crystalSystem: String? = null,

    // Optical and physical characteristics
    val luster: String? = null,
    val diaphaneity: String? = null,
    val cleavage: String? = null,
    val fracture: String? = null,
    val habit: String? = null,
    val streak: String? = null,
    val fluorescence: String? = null,

    // Additional notes
    val notes: String? = null,

    // Metadata
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
