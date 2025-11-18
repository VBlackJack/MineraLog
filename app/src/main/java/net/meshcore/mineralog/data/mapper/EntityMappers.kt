package net.meshcore.mineralog.data.mapper

import net.meshcore.mineralog.data.local.entity.MineralComponentEntity
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.PhotoEntity
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity
import net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity
import net.meshcore.mineralog.data.local.entity.StorageEntity
import net.meshcore.mineralog.domain.model.ComponentRole
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.MineralComponent
import net.meshcore.mineralog.domain.model.MineralType
import net.meshcore.mineralog.domain.model.Photo
import net.meshcore.mineralog.domain.model.Provenance
import net.meshcore.mineralog.domain.model.SimpleProperties
import net.meshcore.mineralog.domain.model.Storage
import java.util.UUID

/**
 * Mappers for converting between Room entities and domain models.
 */

fun MineralEntity.toDomain(
    provenance: ProvenanceEntity? = null,
    storage: StorageEntity? = null,
    photos: List<PhotoEntity> = emptyList(),
    components: List<MineralComponentEntity> = emptyList()
): Mineral {
    return Mineral(
        id = id,
        name = name,
        // v2.0: Map type from entity
        mineralType = when (type) {
            "AGGREGATE" -> MineralType.AGGREGATE
            else -> MineralType.SIMPLE
        },
        group = group,
        formula = formula,
        crystalSystem = crystalSystem,
        mohsMin = mohsMin,
        mohsMax = mohsMax,
        cleavage = cleavage,
        fracture = fracture,
        luster = luster,
        streak = streak,
        diaphaneity = diaphaneity,
        habit = habit,
        specificGravity = specificGravity,
        fluorescence = fluorescence,
        magnetic = magnetic,
        radioactive = radioactive,
        dimensionsMm = dimensionsMm,
        weightGr = weightGr,
        // v3.1: Aggregate-specific fields
        rockType = rockType,
        texture = texture,
        dominantMinerals = dominantMinerals,
        interestingFeatures = interestingFeatures,
        notes = notes,
        tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        status = status,
        statusType = statusType,
        statusDetails = statusDetails,
        qualityRating = qualityRating,
        completeness = completeness,
        createdAt = createdAt,
        updatedAt = updatedAt,
        provenance = provenance?.toDomain(),
        storage = storage?.toDomain(),
        photos = photos.map { it.toDomain() },
        components = components
            .sortedBy { it.displayOrder }
            .map { it.toDomain() }
    )
}

fun Mineral.toEntity(): MineralEntity {
    return MineralEntity(
        id = id,
        name = name,
        // v2.0: Map type to entity
        type = when (mineralType) {
            MineralType.AGGREGATE -> "AGGREGATE"
            MineralType.SIMPLE -> "SIMPLE"
            MineralType.ROCK -> "ROCK"
        },
        group = group,
        formula = formula,
        crystalSystem = crystalSystem,
        mohsMin = mohsMin,
        mohsMax = mohsMax,
        cleavage = cleavage,
        fracture = fracture,
        luster = luster,
        streak = streak,
        diaphaneity = diaphaneity,
        habit = habit,
        specificGravity = specificGravity,
        fluorescence = fluorescence,
        magnetic = magnetic,
        radioactive = radioactive,
        dimensionsMm = dimensionsMm,
        weightGr = weightGr,
        // v3.1: Aggregate-specific fields
        rockType = rockType,
        texture = texture,
        dominantMinerals = dominantMinerals,
        interestingFeatures = interestingFeatures,
        notes = notes,
        tags = tags.joinToString(","),
        status = status,
        statusType = statusType,
        statusDetails = statusDetails,
        qualityRating = qualityRating,
        completeness = completeness,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ProvenanceEntity.toDomain(): Provenance {
    return Provenance(
        id = id,
        mineralId = mineralId,
        site = site,
        locality = locality,
        country = country,
        latitude = latitude,
        longitude = longitude,
        acquiredAt = acquiredAt,
        source = source,
        price = price,
        estimatedValue = estimatedValue,
        currency = currency,
        // v3.1: Collector-focused fields
        mineName = mineName,
        collectorName = collectorName,
        dealer = dealer,
        catalogNumber = catalogNumber,
        acquisitionNotes = acquisitionNotes
    )
}

fun Provenance.toEntity(): ProvenanceEntity {
    return ProvenanceEntity(
        id = id,
        mineralId = mineralId,
        site = site,
        locality = locality,
        country = country,
        latitude = latitude,
        longitude = longitude,
        acquiredAt = acquiredAt,
        source = source,
        price = price,
        estimatedValue = estimatedValue,
        currency = currency,
        // v3.1: Collector-focused fields
        mineName = mineName,
        collectorName = collectorName,
        dealer = dealer,
        catalogNumber = catalogNumber,
        acquisitionNotes = acquisitionNotes
    )
}

fun StorageEntity.toDomain(): Storage {
    return Storage(
        id = id,
        mineralId = mineralId,
        place = place,
        container = container,
        box = box,
        slot = slot,
        nfcTagId = nfcTagId,
        qrContent = qrContent
    )
}

fun Storage.toEntity(): StorageEntity {
    return StorageEntity(
        id = id,
        mineralId = mineralId,
        place = place,
        container = container,
        box = box,
        slot = slot,
        nfcTagId = nfcTagId,
        qrContent = qrContent
    )
}

fun PhotoEntity.toDomain(): Photo {
    return Photo(
        id = id,
        mineralId = mineralId,
        type = type.name,
        caption = caption,
        takenAt = takenAt,
        fileName = fileName
    )
}

fun Photo.toEntity(): PhotoEntity {
    return PhotoEntity(
        id = id,
        mineralId = mineralId,
        type = runCatching {
            net.meshcore.mineralog.data.local.entity.PhotoType.valueOf(type)
        }.getOrDefault(net.meshcore.mineralog.data.local.entity.PhotoType.NORMAL),
        caption = caption,
        takenAt = takenAt,
        fileName = fileName
    )
}

// ========== v2.0 Mappers for Mineral Aggregates ==========

/**
 * Convert SimplePropertiesEntity to SimpleProperties domain model.
 */
fun SimplePropertiesEntity.toDomain(): SimpleProperties {
    return SimpleProperties(
        group = group,
        mohsMin = mohsMin,
        mohsMax = mohsMax,
        density = density,
        formula = formula,
        crystalSystem = crystalSystem,
        luster = luster,
        diaphaneity = diaphaneity,
        cleavage = cleavage,
        fracture = fracture,
        habit = habit,
        streak = streak,
        fluorescence = fluorescence
    )
}

/**
 * Convert SimpleProperties domain model to SimplePropertiesEntity.
 * @param mineralId The ID of the mineral this properties entry belongs to.
 */
fun SimpleProperties.toEntity(mineralId: String): SimplePropertiesEntity {
    return SimplePropertiesEntity(
        id = "${mineralId}_props",
        mineralId = mineralId,
        group = group,
        mohsMin = mohsMin,
        mohsMax = mohsMax,
        density = density,
        formula = formula,
        crystalSystem = crystalSystem,
        luster = luster,
        diaphaneity = diaphaneity,
        cleavage = cleavage,
        fracture = fracture,
        habit = habit,
        streak = streak,
        fluorescence = fluorescence
    )
}

/**
 * Convert MineralComponentEntity to MineralComponent domain model.
 */
fun MineralComponentEntity.toDomain(): MineralComponent {
    return MineralComponent(
        id = id,
        mineralName = mineralName,
        mineralGroup = mineralGroup,
        percentage = percentage,
        role = ComponentRole.fromString(role),
        mohsMin = mohsMin,
        mohsMax = mohsMax,
        density = density,
        formula = formula,
        crystalSystem = crystalSystem,
        luster = luster,
        diaphaneity = diaphaneity,
        cleavage = cleavage,
        fracture = fracture,
        habit = habit,
        streak = streak,
        fluorescence = fluorescence,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Convert MineralComponent domain model to MineralComponentEntity.
 * @param aggregateId The ID of the aggregate mineral this component belongs to.
 * @param displayOrder The display order of this component (0-based index).
 */
fun MineralComponent.toEntity(aggregateId: String, displayOrder: Int): MineralComponentEntity {
    return MineralComponentEntity(
        id = id.ifEmpty { UUID.randomUUID().toString() },
        aggregateId = aggregateId,
        displayOrder = displayOrder,
        mineralName = mineralName,
        mineralGroup = mineralGroup,
        percentage = percentage,
        role = role.name,
        mohsMin = mohsMin,
        mohsMax = mohsMax,
        density = density,
        formula = formula,
        crystalSystem = crystalSystem,
        luster = luster,
        diaphaneity = diaphaneity,
        cleavage = cleavage,
        fracture = fracture,
        habit = habit,
        streak = streak,
        fluorescence = fluorescence,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
