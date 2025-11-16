package net.meshcore.mineralog.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model for physical and chemical properties of a simple (non-aggregate) mineral.
 *
 * This model contains all the mineralogical properties that define a simple mineral specimen.
 * Used only for minerals of type SIMPLE. For AGGREGATE minerals, properties are stored
 * per component in the MineralComponent model.
 */
@Serializable
data class SimpleProperties(
    // v3.0: Reference mineral link
    // When set, this specimen's properties inherit from a reference mineral template
    val referenceMineralId: String? = null,

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
    val fluorescence: String? = null,

    // v3.0: Specimen-specific fields (override reference properties)
    val colorVariety: String? = null,        // Variété de couleur du spécimen
    val actualDiaphaneity: String? = null,  // Diaphanéité réelle du spécimen (peut différer de la référence)
    val qualityNotes: String? = null         // Notes de qualité du spécimen
) {
    /**
     * Get hardness range as a string.
     * Example: "7" or "6-6.5"
     */
    val hardnessRange: String?
        get() = when {
            mohsMin == null && mohsMax == null -> null
            mohsMin == mohsMax -> mohsMin?.toString()
            mohsMin == null -> mohsMax?.toString()
            mohsMax == null -> mohsMin.toString()
            else -> "$mohsMin-$mohsMax"
        }

    /**
     * Check if all required basic properties are filled.
     */
    val hasBasicProperties: Boolean
        get() = group != null || formula != null || mohsMin != null

    /**
     * Calculate completeness percentage (0-100).
     * v3.0: Updated to include new specimen-specific fields
     */
    val completenessPercentage: Int
        get() {
            val totalFields = 16 // 13 original + 3 specimen-specific
            val filledFields = listOfNotNull(
                group, mohsMin, mohsMax, density, formula, crystalSystem,
                luster, diaphaneity, cleavage, fracture, habit, streak, fluorescence,
                colorVariety, actualDiaphaneity, qualityNotes
            ).size
            return (filledFields * 100) / totalFields
        }
}
