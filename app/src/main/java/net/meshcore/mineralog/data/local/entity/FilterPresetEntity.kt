package net.meshcore.mineralog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Room entity for saved filter presets.
 * Allows users to save frequently used filter combinations for quick access.
 */
@Entity(
    tableName = "filter_presets",
    indices = [
        Index(value = ["name"]),
        Index(value = ["createdAt"])
    ]
)
data class FilterPresetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /** User-friendly name for the preset (e.g., "High-Value Quartz", "UV Fluorescent") */
    val name: String,

    /** Icon identifier (Material Icons name, e.g., "filter_list", "star", "bookmark") */
    val icon: String = "filter_list",

    /** JSON-serialized FilterCriteria */
    val criteriaJson: String,

    /** Creation timestamp */
    val createdAt: Instant = Instant.now(),

    /** Last updated timestamp */
    val updatedAt: Instant = Instant.now()
)
