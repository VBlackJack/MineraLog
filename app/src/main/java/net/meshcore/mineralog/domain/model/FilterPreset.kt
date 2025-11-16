package net.meshcore.mineralog.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import net.meshcore.mineralog.data.model.FilterCriteria
import java.time.Instant
import java.util.UUID

/**
 * Domain model for a saved filter preset.
 * Allows users to save and reuse frequently-used filter combinations.
 *
 * Performance: Marked @Immutable for Compose optimization.
 */
@Immutable
@Serializable
data class FilterPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "filter_list",
    val criteria: FilterCriteria,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now()
)
