package net.meshcore.mineralog.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Domain model for a complete mineral specimen with all related data.
 * Used for business logic and UI display.
 *
 * Performance: Marked @Immutable for Compose optimization - prevents unnecessary recompositions.
 */
@Immutable
@Serializable
data class Mineral(
    val id: String,
    val name: String,
    // v2.0: Mineral type (SIMPLE or AGGREGATE)
    val mineralType: MineralType = MineralType.SIMPLE,
    val group: String? = null,
    val formula: String? = null,
    val crystalSystem: String? = null,
    val mohsMin: Float? = null,
    val mohsMax: Float? = null,
    val cleavage: String? = null,
    val fracture: String? = null,
    val luster: String? = null,
    val streak: String? = null,
    val diaphaneity: String? = null,
    val habit: String? = null,
    val specificGravity: Float? = null,
    val fluorescence: String? = null,
    val magnetic: Boolean = false,
    val radioactive: Boolean = false,
    val dimensionsMm: String? = null,
    val weightGr: Float? = null,
    // v3.1: Aggregate-specific fields (for type=AGGREGATE)
    val rockType: String? = null,
    val texture: String? = null,
    val dominantMinerals: String? = null,
    val interestingFeatures: String? = null,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    // v1.0 status (backward compatibility)
    val status: String = "incomplete",
    // v1.1 status & lifecycle fields
    val statusType: String = "in_collection",
    val statusDetails: String? = null,
    val qualityRating: Int? = null,
    val completeness: Int = 0,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now(),
    val provenance: Provenance? = null,
    val storage: Storage? = null,
    val photos: List<Photo> = emptyList(),
    // v2.0: Aggregate components (only for AGGREGATE type minerals)
    val components: List<MineralComponent> = emptyList()
)

@Immutable
@Serializable
data class Provenance(
    val id: String,
    val mineralId: String,
    val site: String? = null,
    val locality: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Serializable(with = InstantSerializer::class)
    val acquiredAt: Instant? = null,
    val source: String? = null,
    val price: Float? = null,
    val estimatedValue: Float? = null,
    val currency: String? = "USD",
    // v3.1: Collector-focused fields
    val mineName: String? = null,
    val collectorName: String? = null,
    val dealer: String? = null,
    val catalogNumber: String? = null,
    val acquisitionNotes: String? = null
)

@Immutable
@Serializable
data class Storage(
    val id: String,
    val mineralId: String,
    val place: String? = null,
    val container: String? = null,
    val box: String? = null,
    val slot: String? = null,
    val nfcTagId: String? = null,
    val qrContent: String? = null
)

@Immutable
@Serializable
data class Photo(
    val id: String,
    val mineralId: String,
    val type: String = "NORMAL",
    val caption: String? = null,
    @Serializable(with = InstantSerializer::class)
    val takenAt: Instant = Instant.now(),
    val fileName: String
)

/**
 * Extension functions for synthesizing aggregate properties from components.
 * v3.1: Aggregate optimization
 */

/**
 * Calculate the overall hardness range from all components.
 * Returns "min-max" format, e.g., "2-7" for an aggregate with components ranging from Mohs 2 to 7.
 */
fun Mineral.getSynthesizedHardnessRange(): String? {
    if (components.isEmpty()) return null
    
    val allMohsMin = components.mapNotNull { it.mohsMin }
    val allMohsMax = components.mapNotNull { it.mohsMax }
    
    if (allMohsMin.isEmpty() && allMohsMax.isEmpty()) return null
    
    val overallMin = allMohsMin.minOrNull() ?: allMohsMax.minOrNull() ?: return null
    val overallMax = allMohsMax.maxOrNull() ?: allMohsMin.maxOrNull() ?: return null
    
    return if (overallMin == overallMax) {
        String.format("%.1f", overallMin)
    } else {
        String.format("%.1f - %.1f", overallMin, overallMax)
    }
}

/**
 * Get all unique formulas from components.
 */
fun Mineral.getSynthesizedFormulas(): List<Pair<String, String?>> {
    return components
        .filter { !it.formula.isNullOrBlank() }
        .map { it.mineralName to it.formula }
        .distinctBy { it.first }
}

/**
 * Get all unique crystal systems from components.
 */
fun Mineral.getSynthesizedCrystalSystems(): List<Pair<String, String?>> {
    return components
        .filter { !it.crystalSystem.isNullOrBlank() }
        .map { it.mineralName to it.crystalSystem }
        .distinctBy { it.first }
}
