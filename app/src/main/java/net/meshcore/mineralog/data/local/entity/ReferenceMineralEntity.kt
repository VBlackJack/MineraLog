package net.meshcore.mineralog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Room entity representing a reference mineral in the library.
 *
 * This entity serves as a template library for mineral properties. Users can select
 * a reference mineral when creating a specimen, which auto-fills technical properties
 * while allowing specimen-specific customizations (color variety, actual transparency, etc.).
 *
 * The library contains both pre-populated minerals (isUserDefined = false) and
 * user-created custom minerals (isUserDefined = true).
 *
 * Reference minerals can be linked to:
 * - SimplePropertiesEntity (for simple mineral specimens)
 * - MineralComponentEntity (for aggregate components)
 *
 * @property id Unique identifier for the reference mineral.
 * @property nameFr French name of the mineral (e.g., "Quartz", "Calcite").
 * @property nameEn English name of the mineral (e.g., "Quartz", "Calcite").
 * @property synonyms Alternative names or synonyms, comma-separated (e.g., "Rose quartz,Pink quartz").
 * @property mineralGroup Mineral group/class (e.g., "Silicates", "Carbonates", "Sulfides").
 * @property formula Chemical formula (e.g., "SiO₂", "CaCO₃", "FeS₂").
 * @property mohsMin Minimum hardness on Mohs scale (1.0 - 10.0).
 * @property mohsMax Maximum hardness on Mohs scale (1.0 - 10.0).
 * @property density Typical specific gravity/density in g/cm³.
 * @property crystalSystem Crystal system (e.g., "Hexagonal", "Trigonal", "Cubic", "Monoclinic", "Triclinic", "Orthorhombic", "Tetragonal").
 * @property cleavage Cleavage description (e.g., "Perfect {10-11}", "Poor", "None").
 * @property fracture Fracture type (e.g., "Conchoidal", "Uneven", "Splintery", "Hackly").
 * @property habit Typical crystal habit/form (e.g., "Prismatic", "Massive", "Fibrous", "Granular").
 * @property luster Surface luster (e.g., "Vitreous", "Metallic", "Pearly", "Adamantine", "Resinous").
 * @property streak Color of powdered mineral (e.g., "White", "Black", "Red-brown").
 * @property diaphaneity Typical transparency (e.g., "Transparent", "Translucent", "Opaque").
 * @property fluorescence Fluorescence behavior under UV light (e.g., "LW:blue,SW:green", "none", "Variable").
 * @property magnetism Magnetic properties (e.g., "Non-magnetic", "Weakly magnetic", "Strongly magnetic").
 * @property radioactivity Radioactivity level (e.g., "None", "Weak", "Moderate", "Strong").
 * @property careInstructions Care and maintenance instructions for collectors.
 * @property sensitivity Sensitivity to environmental factors (e.g., "Soluble in water", "Degrades in acids", "Fades in UV light").
 * @property hazards Safety hazards and toxicity information (e.g., "Toxic - contains arsenic", "Radioactive", "Sharp edges").
 * @property storageRecommendations Storage recommendations (e.g., "Keep dry", "Avoid direct sunlight", "Store separately").
 * @property identificationTips Practical tips for identifying this mineral in the field or collection.
 * @property diagnosticProperties Main diagnostic properties that distinguish this mineral.
 * @property colors Possible colors, comma-separated (e.g., "Colorless,White,Purple,Yellow").
 * @property varieties Notable varieties or color variants (e.g., "Amethyst (purple), Citrine (yellow), Rose quartz (pink)").
 * @property confusionWith Minerals that might be confused with this one (e.g., "Can be confused with calcite or dolomite").
 * @property geologicalEnvironment Geological formation environment (e.g., "Hydrothermal veins", "Pegmatites", "Sedimentary").
 * @property typicalLocations Famous or typical localities (e.g., "Brazil, Madagascar, USA (Arkansas)").
 * @property associatedMinerals Minerals frequently found together (e.g., "Feldspar, Mica, Tourmaline").
 * @property uses Industrial, ornamental, or other uses (e.g., "Gemstone, Glass manufacturing, Electronics").
 * @property rarity Rarity level (e.g., "Common", "Uncommon", "Rare", "Very rare", "Extremely rare").
 * @property collectingDifficulty Difficulty of obtaining for collectors (e.g., "Easy to find", "Requires dealer", "Museum-grade only").
 * @property historicalInfo Historical context, discovery, or cultural significance.
 * @property etymology Origin and meaning of the mineral's name.
 * @property notes Additional reference notes, mineralogical context, or important observations.
 * @property isUserDefined True if this mineral was created by the user, false if from the standard library.
 * @property source Data source or reference (e.g., "mindat.org", "webmineral.com", "User-defined").
 * @property createdAt Timestamp when this reference mineral was created.
 * @property updatedAt Timestamp when this reference mineral was last updated.
 */
@Entity(
    tableName = "reference_minerals",
    indices = [
        Index(value = ["nameFr"]),
        Index(value = ["nameEn"]),
        Index(value = ["mineralGroup"]),
        Index(value = ["crystalSystem"]),
        Index(value = ["isUserDefined"])
    ]
)
data class ReferenceMineralEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Identification
    val nameFr: String,
    val nameEn: String,
    val synonyms: String? = null,
    val mineralGroup: String? = null,

    // Chemical properties
    val formula: String? = null,

    // Physical properties
    val mohsMin: Float? = null,
    val mohsMax: Float? = null,
    val density: Float? = null,

    // Crystallographic properties
    val crystalSystem: String? = null,
    val cleavage: String? = null,
    val fracture: String? = null,
    val habit: String? = null,

    // Optical and physical characteristics
    val luster: String? = null,
    val streak: String? = null,
    val diaphaneity: String? = null,

    // Special properties
    val fluorescence: String? = null,
    val magnetism: String? = null,
    val radioactivity: String? = null,

    // Practical information & Safety
    val careInstructions: String? = null,         // Care and maintenance instructions
    val sensitivity: String? = null,              // Sensitivity (water, acids, UV light, heat, air)
    val hazards: String? = null,                  // Hazards/toxicity with risk level
    val storageRecommendations: String? = null,   // Storage recommendations

    // Identification & Recognition
    val identificationTips: String? = null,       // How to identify the mineral
    val diagnosticProperties: String? = null,     // Main diagnostic properties
    val colors: String? = null,                   // Possible colors (comma-separated)
    val varieties: String? = null,                // Notable varieties
    val confusionWith: String? = null,            // Risk of confusion with other minerals

    // Geological context
    val geologicalEnvironment: String? = null,    // Geological formation environment
    val typicalLocations: String? = null,         // Typical/famous localities
    val associatedMinerals: String? = null,       // Frequently associated minerals

    // Additional information
    val uses: String? = null,                     // Uses (industrial, jewelry, collection)
    val rarity: String? = null,                   // Rarity (Common, Uncommon, Rare, Very rare)
    val collectingDifficulty: String? = null,     // Difficulty of obtaining
    val historicalInfo: String? = null,           // Historical information
    val etymology: String? = null,                // Etymology of the name

    // Metadata
    val notes: String? = null,
    val isUserDefined: Boolean = false,
    val source: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
