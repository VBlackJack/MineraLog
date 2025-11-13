package net.meshcore.mineralog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Room entity representing provenance information for a mineral specimen.
 * Tracks acquisition details and geographic origin.
 */
@Entity(
    tableName = "provenances",
    foreignKeys = [
        ForeignKey(
            entity = MineralEntity::class,
            parentColumns = ["id"],
            childColumns = ["mineralId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mineralId"], unique = true),
        Index(value = ["country"]),
        Index(value = ["acquiredAt"]),
        Index(value = ["source"])
    ]
)
data class ProvenanceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val mineralId: String,

    // Geographic origin
    val site: String? = null,
    val locality: String? = null,
    val country: String? = null,
    val latitude: Double? = null,  // -90.0 to 90.0
    val longitude: Double? = null, // -180.0 to 180.0

    // Acquisition details
    val acquiredAt: Instant? = null,
    val source: String? = null, // purchase, exchange, collected, gift, inheritance
    val price: Float? = null, // In local currency
    val estimatedValue: Float? = null,
    val currency: String? = "USD" // ISO 4217 currency code
)
