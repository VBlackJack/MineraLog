package net.meshcore.mineralog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity representing the physical and chemical properties of a simple mineral.
 *
 * This entity is used only for minerals of type SIMPLE. For AGGREGATE type minerals,
 * properties are stored in the MineralComponentEntity for each constituent mineral.
 *
 * The entity has a one-to-one relationship with MineralEntity via the mineralId foreign key.
 * When a mineral is deleted, its properties are automatically deleted via CASCADE.
 *
 * @property id Unique identifier for the properties entry.
 * @property mineralId Foreign key to the MineralEntity this belongs to.
 * @property group Mineral group/class (e.g., "Silicates", "Sulfides", "Oxides").
 * @property mohsMin Minimum hardness on Mohs scale (1.0 - 10.0).
 * @property mohsMax Maximum hardness on Mohs scale (1.0 - 10.0).
 * @property density Specific gravity/density in g/cm³.
 * @property formula Chemical formula (e.g., "SiO₂", "FeS₂").
 * @property crystalSystem Crystal system (e.g., "Hexagonal", "Cubic", "Monoclinic").
 * @property luster Surface luster (e.g., "Vitreous", "Metallic", "Pearly").
 * @property diaphaneity Transparency (e.g., "Transparent", "Translucent", "Opaque").
 * @property cleavage Cleavage description (e.g., "Perfect", "Poor", "None").
 * @property fracture Fracture type (e.g., "Conchoidal", "Uneven", "Splintery").
 * @property habit Crystal habit/form (e.g., "Prismatic", "Massive", "Fibrous").
 * @property streak Color of powdered mineral.
 * @property fluorescence Fluorescence behavior under UV light (e.g., "LW:blue,SW:green", "none").
 */
@Entity(
    tableName = "simple_properties",
    foreignKeys = [
        ForeignKey(
            entity = MineralEntity::class,
            parentColumns = ["id"],
            childColumns = ["mineralId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mineralId"], unique = true)
    ]
)
data class SimplePropertiesEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val mineralId: String,

    // Mineralogical classification
    val group: String? = null,

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
    val fluorescence: String? = null
)
