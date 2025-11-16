package net.meshcore.mineralog.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Domain model for a mineral component within an aggregate.
 *
 * Represents an individual mineral that makes up part of an aggregate/rock specimen.
 * For example, in Granite: Quartz (35%), Feldspath (40%), Mica (20%), Biotite (5%).
 */
@Serializable
data class MineralComponent(
    val id: String,

    // v3.0: Reference mineral link
    // When set, this component's properties inherit from a reference mineral template
    val referenceMineralId: String? = null,

    // Component identification
    val mineralName: String,
    val mineralGroup: String? = null,

    // Composition
    val percentage: Float? = null,
    val role: ComponentRole,

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
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now()
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
     * Get percentage as formatted string.
     * Example: "35%" or "~20%" (for estimated values)
     */
    val percentageFormatted: String?
        get() = percentage?.let { "${it.toInt()}%" }

    /**
     * Get role display name (localized in UI layer).
     */
    val roleDisplayName: String
        get() = when (role) {
            ComponentRole.PRINCIPAL -> "Principal"
            ComponentRole.ACCESSORY -> "Accessory"
            ComponentRole.TRACE -> "Trace"
        }

    /**
     * Check if this component has sufficient data for display.
     */
    val isValid: Boolean
        get() = mineralName.isNotBlank()

    /**
     * Calculate completeness percentage for this component (0-100).
     */
    val completenessPercentage: Int
        get() {
            val totalFields = 15 // Total property fields
            val filledFields = listOfNotNull(
                mineralName,
                mineralGroup,
                percentage,
                mohsMin,
                mohsMax,
                density,
                formula,
                crystalSystem,
                luster,
                diaphaneity,
                cleavage,
                fracture,
                habit,
                streak,
                fluorescence
            ).size
            return (filledFields * 100) / totalFields
        }
}
