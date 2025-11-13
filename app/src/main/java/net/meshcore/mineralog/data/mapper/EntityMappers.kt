package net.meshcore.mineralog.data.mapper

import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.PhotoEntity
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity
import net.meshcore.mineralog.data.local.entity.StorageEntity
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.Photo
import net.meshcore.mineralog.domain.model.Provenance
import net.meshcore.mineralog.domain.model.Storage

/**
 * Mappers for converting between Room entities and domain models.
 */

fun MineralEntity.toDomain(
    provenance: ProvenanceEntity? = null,
    storage: StorageEntity? = null,
    photos: List<PhotoEntity> = emptyList()
): Mineral {
    return Mineral(
        id = id,
        name = name,
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
        photos = photos.map { it.toDomain() }
    )
}

fun Mineral.toEntity(): MineralEntity {
    return MineralEntity(
        id = id,
        name = name,
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
        currency = currency
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
        currency = currency
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
        type = net.meshcore.mineralog.data.local.entity.PhotoType.valueOf(type),
        caption = caption,
        takenAt = takenAt,
        fileName = fileName
    )
}
