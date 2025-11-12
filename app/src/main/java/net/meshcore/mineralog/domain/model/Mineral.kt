package net.meshcore.mineralog.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Domain model for a complete mineral specimen with all related data.
 * Used for business logic and UI display.
 */
@Serializable
data class Mineral(
    val id: String,
    val name: String,
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
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val status: String = "incomplete",
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now(),
    val provenance: Provenance? = null,
    val storage: Storage? = null,
    val photos: List<Photo> = emptyList()
)

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
    val estimatedValue: Float? = null
)

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
