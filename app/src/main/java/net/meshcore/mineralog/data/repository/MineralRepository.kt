package net.meshcore.mineralog.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import net.meshcore.mineralog.data.local.dao.MineralDao
import net.meshcore.mineralog.data.local.dao.PhotoDao
import net.meshcore.mineralog.data.local.dao.ProvenanceDao
import net.meshcore.mineralog.data.local.dao.StorageDao
import net.meshcore.mineralog.data.mapper.*
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.Photo
import net.meshcore.mineralog.domain.model.Provenance
import net.meshcore.mineralog.domain.model.Storage

interface MineralRepository {
    suspend fun insert(mineral: Mineral): String
    suspend fun update(mineral: Mineral)
    suspend fun delete(id: String)
    suspend fun deleteAll()
    suspend fun getById(id: String): Mineral?
    fun getByIdFlow(id: String): Flow<Mineral?>
    fun getAllFlow(): Flow<List<Mineral>>
    suspend fun getAll(): List<Mineral>
    fun searchFlow(query: String): Flow<List<Mineral>>
    fun filterAdvancedFlow(criteria: FilterCriteria): Flow<List<Mineral>>
    suspend fun getCount(): Int
    fun getCountFlow(): Flow<Int>

    // Provenance
    suspend fun insertProvenance(provenance: Provenance)
    suspend fun updateProvenance(provenance: Provenance)

    // Storage
    suspend fun insertStorage(storage: Storage)
    suspend fun updateStorage(storage: Storage)

    // Photos
    suspend fun insertPhoto(photo: Photo)
    suspend fun deletePhoto(photoId: String)
    fun getPhotosFlow(mineralId: String): Flow<List<Photo>>
}

class MineralRepositoryImpl(
    private val mineralDao: MineralDao,
    private val provenanceDao: ProvenanceDao,
    private val storageDao: StorageDao,
    private val photoDao: PhotoDao
) : MineralRepository {

    override suspend fun insert(mineral: Mineral): String {
        mineralDao.insert(mineral.toEntity())
        mineral.provenance?.let { provenanceDao.insert(it.toEntity()) }
        mineral.storage?.let { storageDao.insert(it.toEntity()) }
        mineral.photos.forEach { photoDao.insert(it.toEntity()) }
        return mineral.id
    }

    override suspend fun update(mineral: Mineral) {
        mineralDao.update(mineral.toEntity())
        mineral.provenance?.let { provenanceDao.insert(it.toEntity()) }
        mineral.storage?.let { storageDao.insert(it.toEntity()) }
    }

    override suspend fun delete(id: String) {
        mineralDao.deleteById(id)
    }

    override suspend fun deleteAll() {
        mineralDao.deleteAll()
        provenanceDao.deleteAll()
        storageDao.deleteAll()
        photoDao.deleteAll()
    }

    override suspend fun getById(id: String): Mineral? {
        val entity = mineralDao.getById(id) ?: return null
        val provenance = provenanceDao.getByMineralId(id)
        val storage = storageDao.getByMineralId(id)
        val photos = photoDao.getByMineralId(id)
        return entity.toDomain(provenance, storage, photos)
    }

    override fun getByIdFlow(id: String): Flow<Mineral?> {
        return combine(
            mineralDao.getByIdFlow(id),
            provenanceDao.getByMineralIdFlow(id),
            storageDao.getByMineralIdFlow(id),
            photoDao.getByMineralIdFlow(id)
        ) { mineral, provenance, storage, photos ->
            mineral?.toDomain(provenance, storage, photos)
        }
    }

    override fun getAllFlow(): Flow<List<Mineral>> {
        return mineralDao.getAllFlow().map { entities ->
            entities.map { entity ->
                val provenance = provenanceDao.getByMineralId(entity.id)
                val storage = storageDao.getByMineralId(entity.id)
                val photos = photoDao.getByMineralId(entity.id)
                entity.toDomain(provenance, storage, photos)
            }
        }
    }

    override suspend fun getAll(): List<Mineral> {
        return mineralDao.getAll().map { entity ->
            val provenance = provenanceDao.getByMineralId(entity.id)
            val storage = storageDao.getByMineralId(entity.id)
            val photos = photoDao.getByMineralId(entity.id)
            entity.toDomain(provenance, storage, photos)
        }
    }

    override fun searchFlow(query: String): Flow<List<Mineral>> {
        return mineralDao.searchFlow(query).map { entities ->
            entities.map { entity ->
                val provenance = provenanceDao.getByMineralId(entity.id)
                val storage = storageDao.getByMineralId(entity.id)
                val photos = photoDao.getByMineralId(entity.id)
                entity.toDomain(provenance, storage, photos)
            }
        }
    }

    override fun filterAdvancedFlow(criteria: FilterCriteria): Flow<List<Mineral>> {
        // If criteria is empty, return all minerals
        if (criteria.isEmpty()) {
            return getAllFlow()
        }

        return mineralDao.filterAdvanced(
            groups = criteria.groups.takeIf { it.isNotEmpty() },
            countries = criteria.countries.takeIf { it.isNotEmpty() },
            mohsMin = criteria.mohsMin,
            mohsMax = criteria.mohsMax,
            statusTypes = criteria.statusTypes.takeIf { it.isNotEmpty() },
            qualityMin = criteria.qualityMin,
            qualityMax = criteria.qualityMax,
            hasPhotos = criteria.hasPhotos,
            fluorescent = criteria.fluorescent
        ).map { entities ->
            entities.map { entity ->
                val provenance = provenanceDao.getByMineralId(entity.id)
                val storage = storageDao.getByMineralId(entity.id)
                val photos = photoDao.getByMineralId(entity.id)
                entity.toDomain(provenance, storage, photos)
            }
        }
    }

    override suspend fun getCount(): Int = mineralDao.getCount()

    override fun getCountFlow(): Flow<Int> = mineralDao.getCountFlow()

    override suspend fun insertProvenance(provenance: Provenance) {
        provenanceDao.insert(provenance.toEntity())
    }

    override suspend fun updateProvenance(provenance: Provenance) {
        provenanceDao.update(provenance.toEntity())
    }

    override suspend fun insertStorage(storage: Storage) {
        storageDao.insert(storage.toEntity())
    }

    override suspend fun updateStorage(storage: Storage) {
        storageDao.update(storage.toEntity())
    }

    override suspend fun insertPhoto(photo: Photo) {
        photoDao.insert(photo.toEntity())
    }

    override suspend fun deletePhoto(photoId: String) {
        photoDao.deleteById(photoId)
    }

    override fun getPhotosFlow(mineralId: String): Flow<List<Photo>> {
        return photoDao.getByMineralIdFlow(mineralId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
