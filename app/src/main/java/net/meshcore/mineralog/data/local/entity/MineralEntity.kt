package net.meshcore.mineralog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * Room entity representing a mineral specimen.
 * Stores all mineralogical properties and metadata.
 */
@Entity(
    tableName = "minerals",
    indices = [
        Index(value = ["name"]),
        Index(value = ["group"]),
        Index(value = ["crystalSystem"]),
        Index(value = ["status"]), // v1.0 compat
        Index(value = ["statusType"]), // v1.1
        Index(value = ["completeness"]), // v1.1
        Index(value = ["qualityRating"]), // v1.1
        Index(value = ["provenanceId"]), // v1.1 FK
        Index(value = ["storageId"]), // v1.1 FK
        Index(value = ["createdAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class MineralEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Basic info
    val name: String,
    val group: String? = null,
    val formula: String? = null,
    val crystalSystem: String? = null, // Triclinic, Monoclinic, Orthorhombic, Tetragonal, Trigonal, Hexagonal, Cubic

    // Physical properties
    val mohsMin: Float? = null, // 1.0 - 10.0
    val mohsMax: Float? = null, // 1.0 - 10.0
    val cleavage: String? = null,
    val fracture: String? = null,
    val luster: String? = null, // Metallic, Vitreous, Pearly, Resinous, Silky, Greasy, Dull
    val streak: String? = null,
    val diaphaneity: String? = null, // Transparent, Translucent, Opaque
    val habit: String? = null,
    val specificGravity: Float? = null,

    // Special properties
    val fluorescence: String? = null, // Format: "LW:blue,SW:green" or "none"
    val magnetic: Boolean = false,
    val radioactive: Boolean = false,

    // Physical measurements
    val dimensionsMm: String? = null, // Format: "length x width x height" or free text
    val weightGr: Float? = null,

    // User notes and categorization
    val notes: String? = null,
    val tags: String? = null, // Comma-separated tags

    // Status (v1.0 - deprecated, kept for backward compatibility)
    val status: String = "incomplete", // complete, incomplete

    // v1.1 Status & Lifecycle Management
    val statusType: String = "in_collection", // MineralStatus enum value
    val statusDetails: String? = null, // JSON-serialized MineralStatusDetails
    val qualityRating: Int? = null, // 1-5, specimen quality assessment
    val completeness: Int = 0, // 0-100, calculated percentage of filled fields

    /**
     * Foreign key relationships - UNIDIRECTIONAL DESIGN
     *
     * These fields establish a one-to-one relationship with ProvenanceEntity and StorageEntity.
     * IMPORTANT: Room foreign keys are NOT used intentionally for the following reasons:
     *
     * 1. **Manual Cascade Control**: We handle cascades in the Repository layer for better control:
     *    - When a Mineral is deleted, we explicitly delete related Provenance/Storage
     *    - This allows logging, undo functionality, and custom cascade logic
     *    - See: MineralRepositoryImpl.delete() for cascade implementation
     *
     * 2. **Unidirectional Navigation**: Child entities (Provenance/Storage) have mineralId,
     *    but parent (Mineral) only stores IDs, not Room @Relation annotations:
     *    - Prevents circular dependencies
     *    - Simplifies serialization (JSON export/import)
     *    - Allows partial loading (load Mineral without related data)
     *    - Batch queries avoid N+1 problems (see BackupRepository.exportZip)
     *
     * 3. **Nullable by Design**: IDs can be null (Mineral can exist without provenance/storage):
     *    - Supports progressive data entry
     *    - Optional metadata collection
     *    - Orphan cleanup handled in Repository layer
     *
     * 4. **Lookup Pattern**: To get related entities:
     *    ```kotlin
     *    val provenance = provenanceDao.getByMineralId(mineral.id)
     *    val storage = storageDao.getByMineralId(mineral.id)
     *    ```
     *    Or use domain model mappers that handle the joins:
     *    ```kotlin
     *    mineralEntity.toDomain(provenance, storage, photos)
     *    ```
     *
     * WARNING: If lookup returns null unexpectedly, check:
     * - Cascade delete logic in MineralRepositoryImpl
     * - Orphan cleanup on import (BackupRepository.importZip)
     * - Transaction boundaries (all inserts/deletes must be in same transaction)
     *
     * @see ProvenanceEntity.mineralId for the bidirectional link
     * @see StorageEntity.mineralId for the bidirectional link
     * @see net.meshcore.mineralog.data.repository.MineralRepositoryImpl.delete cascade logic
     * @see net.meshcore.mineralog.data.mapper.EntityMappersKt.toDomain for relationship loading
     */
    val provenanceId: String? = null, // UUID reference to ProvenanceEntity
    val storageId: String? = null, // UUID reference to StorageEntity

    // Metadata
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
