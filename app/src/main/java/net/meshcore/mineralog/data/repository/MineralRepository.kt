package net.meshcore.mineralog.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.dao.MineralDao
import net.meshcore.mineralog.data.local.dao.PhotoDao
import net.meshcore.mineralog.data.local.dao.ProvenanceDao
import net.meshcore.mineralog.data.local.dao.StorageDao
import net.meshcore.mineralog.data.local.paging.MineralPagingSource
import net.meshcore.mineralog.data.mapper.*
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.ui.screens.home.SortOption
import net.meshcore.mineralog.domain.model.Photo
import net.meshcore.mineralog.domain.model.Provenance
import net.meshcore.mineralog.domain.model.Storage

interface MineralRepository {
    suspend fun insert(mineral: Mineral): String
    suspend fun update(mineral: Mineral)
    suspend fun delete(id: String)
    suspend fun deleteByIds(ids: List<String>)
    suspend fun deleteAll()
    suspend fun getById(id: String): Mineral?
    suspend fun getByIds(ids: List<String>): List<Mineral>
    fun getByIdFlow(id: String): Flow<Mineral?>
    fun getAllFlow(sortOption: SortOption = SortOption.DATE_NEWEST): Flow<List<Mineral>>
    suspend fun getAll(): List<Mineral>
    fun searchFlow(query: String, sortOption: SortOption = SortOption.DATE_NEWEST): Flow<List<Mineral>>
    fun filterAdvancedFlow(criteria: FilterCriteria, sortOption: SortOption = SortOption.DATE_NEWEST): Flow<List<Mineral>>
    suspend fun getCount(): Int
    fun getCountFlow(): Flow<Int>

    // Paging 3 support (v1.5.0)
    fun getAllPaged(sortOption: SortOption = SortOption.DATE_NEWEST): Flow<PagingData<Mineral>>
    fun searchPaged(query: String, sortOption: SortOption = SortOption.DATE_NEWEST): Flow<PagingData<Mineral>>
    fun filterAdvancedPaged(criteria: FilterCriteria, sortOption: SortOption = SortOption.DATE_NEWEST): Flow<PagingData<Mineral>>

    // Quick Win #8: Tag autocomplete support (v1.7.0)
    suspend fun getAllUniqueTags(): List<String>

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
    private val database: MineraLogDatabase,
    private val mineralDao: MineralDao,
    private val provenanceDao: ProvenanceDao,
    private val storageDao: StorageDao,
    private val photoDao: PhotoDao
) : MineralRepository {

    override suspend fun insert(mineral: Mineral): String = database.withTransaction {
        mineralDao.insert(mineral.toEntity())
        mineral.provenance?.let { provenanceDao.insert(it.toEntity()) }
        mineral.storage?.let { storageDao.insert(it.toEntity()) }
        mineral.photos.forEach { photoDao.insert(it.toEntity()) }
        mineral.id
    }

    override suspend fun update(mineral: Mineral): Unit = database.withTransaction {
        mineralDao.update(mineral.toEntity())
        mineral.provenance?.let { provenanceDao.insert(it.toEntity()) }
        mineral.storage?.let { storageDao.insert(it.toEntity()) }
    }

    override suspend fun delete(id: String) = database.withTransaction {
        // Delete related entities first to maintain referential integrity
        provenanceDao.deleteByMineralId(id)
        storageDao.deleteByMineralId(id)
        photoDao.deleteByMineralId(id)
        mineralDao.deleteById(id)
    }

    override suspend fun deleteByIds(ids: List<String>) {
        if (ids.isEmpty()) return

        database.withTransaction {
            // Batch delete related entities first to maintain referential integrity
            provenanceDao.deleteByMineralIds(ids)
            storageDao.deleteByMineralIds(ids)
            photoDao.deleteByMineralIds(ids)
            mineralDao.deleteByIds(ids)
        }
    }

    override suspend fun deleteAll() = database.withTransaction {
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

    override suspend fun getByIds(ids: List<String>): List<Mineral> {
        if (ids.isEmpty()) return emptyList()

        val entities = mineralDao.getByIds(ids)
        if (entities.isEmpty()) return emptyList()

        // Batch load all related entities to avoid N+1 problem
        val mineralIds = entities.map { it.id }
        val provenances = provenanceDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
        val storages = storageDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
        val photos = photoDao.getByMineralIds(mineralIds).groupBy { it.mineralId }

        return entities.map { entity ->
            entity.toDomain(
                provenances[entity.id],
                storages[entity.id],
                photos[entity.id] ?: emptyList()
            )
        }
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

    override fun getAllFlow(sortOption: SortOption): Flow<List<Mineral>> {
        return mineralDao.getAllFlow().map { entities ->
            if (entities.isEmpty()) return@map emptyList()

            // Batch load all related entities to avoid N+1 problem
            val mineralIds = entities.map { it.id }
            val provenances = provenanceDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
            val storages = storageDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
            val photos = photoDao.getByMineralIds(mineralIds).groupBy { it.mineralId }

            val minerals = entities.map { entity ->
                entity.toDomain(
                    provenances[entity.id],
                    storages[entity.id],
                    photos[entity.id] ?: emptyList()
                )
            }

            // Apply in-memory sorting (legacy flow for bulk operations)
            when (sortOption) {
                SortOption.NAME_ASC -> minerals.sortedBy { it.name.lowercase() }
                SortOption.NAME_DESC -> minerals.sortedByDescending { it.name.lowercase() }
                SortOption.DATE_NEWEST -> minerals.sortedByDescending { it.updatedAt }
                SortOption.DATE_OLDEST -> minerals.sortedBy { it.updatedAt }
                SortOption.GROUP -> minerals.sortedWith(compareBy({ it.group }, { it.name.lowercase() }))
                SortOption.HARDNESS_LOW -> minerals.sortedWith(compareBy({ it.mohsMin }, { it.name.lowercase() }))
                SortOption.HARDNESS_HIGH -> minerals.sortedWith(compareByDescending<Mineral> { it.mohsMax }.thenBy { it.name.lowercase() })
            }
        }
    }

    override suspend fun getAll(): List<Mineral> {
        val entities = mineralDao.getAll()
        if (entities.isEmpty()) return emptyList()

        // Batch load all related entities to avoid N+1 problem
        val mineralIds = entities.map { it.id }
        val provenances = provenanceDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
        val storages = storageDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
        val photos = photoDao.getByMineralIds(mineralIds).groupBy { it.mineralId }

        return entities.map { entity ->
            entity.toDomain(
                provenances[entity.id],
                storages[entity.id],
                photos[entity.id] ?: emptyList()
            )
        }
    }

    override fun searchFlow(query: String, sortOption: SortOption): Flow<List<Mineral>> {
        // Pre-format query with wildcards to prevent SQL injection
        val formattedQuery = "%$query%"
        return mineralDao.searchFlow(formattedQuery).map { entities ->
            if (entities.isEmpty()) return@map emptyList()

            // Batch load all related entities to avoid N+1 problem
            val mineralIds = entities.map { it.id }
            val provenances = provenanceDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
            val storages = storageDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
            val photos = photoDao.getByMineralIds(mineralIds).groupBy { it.mineralId }

            val minerals = entities.map { entity ->
                entity.toDomain(
                    provenances[entity.id],
                    storages[entity.id],
                    photos[entity.id] ?: emptyList()
                )
            }

            // Apply in-memory sorting (legacy flow for bulk operations)
            when (sortOption) {
                SortOption.NAME_ASC -> minerals.sortedBy { it.name.lowercase() }
                SortOption.NAME_DESC -> minerals.sortedByDescending { it.name.lowercase() }
                SortOption.DATE_NEWEST -> minerals.sortedByDescending { it.updatedAt }
                SortOption.DATE_OLDEST -> minerals.sortedBy { it.updatedAt }
                SortOption.GROUP -> minerals.sortedWith(compareBy({ it.group }, { it.name.lowercase() }))
                SortOption.HARDNESS_LOW -> minerals.sortedWith(compareBy({ it.mohsMin }, { it.name.lowercase() }))
                SortOption.HARDNESS_HIGH -> minerals.sortedWith(compareByDescending<Mineral> { it.mohsMax }.thenBy { it.name.lowercase() })
            }
        }
    }

    override fun filterAdvancedFlow(criteria: FilterCriteria, sortOption: SortOption): Flow<List<Mineral>> {
        // If criteria is empty, return all minerals
        if (criteria.isEmpty()) {
            return getAllFlow(sortOption)
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
            fluorescent = criteria.fluorescent,
            mineralTypes = criteria.mineralTypes.takeIf { it.isNotEmpty() }
        ).map { entities ->
            if (entities.isEmpty()) return@map emptyList()

            // Batch load all related entities to avoid N+1 problem
            val mineralIds = entities.map { it.id }
            val provenances = provenanceDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
            val storages = storageDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
            val photos = photoDao.getByMineralIds(mineralIds).groupBy { it.mineralId }

            val minerals = entities.map { entity ->
                entity.toDomain(
                    provenances[entity.id],
                    storages[entity.id],
                    photos[entity.id] ?: emptyList()
                )
            }

            // Apply in-memory sorting (legacy flow for bulk operations)
            when (sortOption) {
                SortOption.NAME_ASC -> minerals.sortedBy { it.name.lowercase() }
                SortOption.NAME_DESC -> minerals.sortedByDescending { it.name.lowercase() }
                SortOption.DATE_NEWEST -> minerals.sortedByDescending { it.updatedAt }
                SortOption.DATE_OLDEST -> minerals.sortedBy { it.updatedAt }
                SortOption.GROUP -> minerals.sortedWith(compareBy({ it.group }, { it.name.lowercase() }))
                SortOption.HARDNESS_LOW -> minerals.sortedWith(compareBy({ it.mohsMin }, { it.name.lowercase() }))
                SortOption.HARDNESS_HIGH -> minerals.sortedWith(compareByDescending<Mineral> { it.mohsMax }.thenBy { it.name.lowercase() })
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

    // ========== Paging 3 Support (v1.5.0) ==========
    // Optimized with batch loading to eliminate N+1 query problem

    override fun getAllPaged(sortOption: SortOption): Flow<PagingData<Mineral>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                prefetchDistance = 5
            ),
            pagingSourceFactory = {
                MineralPagingSource(
                    mineralDao = mineralDao,
                    provenanceDao = provenanceDao,
                    storageDao = storageDao,
                    photoDao = photoDao,
                    basePagingSource = when (sortOption) {
                        SortOption.NAME_ASC -> mineralDao.getAllPagedSortedByNameAsc()
                        SortOption.NAME_DESC -> mineralDao.getAllPagedSortedByNameDesc()
                        SortOption.DATE_NEWEST -> mineralDao.getAllPagedSortedByDateDesc()
                        SortOption.DATE_OLDEST -> mineralDao.getAllPagedSortedByDateAsc()
                        SortOption.GROUP -> mineralDao.getAllPagedSortedByGroup()
                        SortOption.HARDNESS_LOW -> mineralDao.getAllPagedSortedByHardnessAsc()
                        SortOption.HARDNESS_HIGH -> mineralDao.getAllPagedSortedByHardnessDesc()
                    }
                )
            }
        ).flow
    }

    override fun searchPaged(query: String, sortOption: SortOption): Flow<PagingData<Mineral>> {
        // Pre-format query with wildcards to prevent SQL injection
        val formattedQuery = "%$query%"
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                prefetchDistance = 5
            ),
            pagingSourceFactory = {
                MineralPagingSource(
                    mineralDao = mineralDao,
                    provenanceDao = provenanceDao,
                    storageDao = storageDao,
                    photoDao = photoDao,
                    basePagingSource = when (sortOption) {
                        SortOption.NAME_ASC -> mineralDao.searchPagedSortedByNameAsc(formattedQuery)
                        SortOption.NAME_DESC -> mineralDao.searchPagedSortedByNameDesc(formattedQuery)
                        SortOption.DATE_NEWEST -> mineralDao.searchPagedSortedByDateDesc(formattedQuery)
                        SortOption.DATE_OLDEST -> mineralDao.searchPagedSortedByDateAsc(formattedQuery)
                        SortOption.GROUP -> mineralDao.searchPagedSortedByGroup(formattedQuery)
                        SortOption.HARDNESS_LOW -> mineralDao.searchPagedSortedByHardnessAsc(formattedQuery)
                        SortOption.HARDNESS_HIGH -> mineralDao.searchPagedSortedByHardnessDesc(formattedQuery)
                    }
                )
            }
        ).flow
    }

    override fun filterAdvancedPaged(criteria: FilterCriteria, sortOption: SortOption): Flow<PagingData<Mineral>> {
        // If criteria is empty, return all minerals
        if (criteria.isEmpty()) {
            return getAllPaged(sortOption)
        }

        val groups = criteria.groups.takeIf { it.isNotEmpty() }
        val countries = criteria.countries.takeIf { it.isNotEmpty() }
        val crystalSystems = criteria.crystalSystems.takeIf { it.isNotEmpty() }
        val statusTypes = criteria.statusTypes.takeIf { it.isNotEmpty() }
        val mineralTypes = criteria.mineralTypes.takeIf { it.isNotEmpty() }

        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = true,
                prefetchDistance = 5
            ),
            pagingSourceFactory = {
                MineralPagingSource(
                    mineralDao = mineralDao,
                    provenanceDao = provenanceDao,
                    storageDao = storageDao,
                    photoDao = photoDao,
                    basePagingSource = when (sortOption) {
                        SortOption.NAME_ASC -> mineralDao.filterAdvancedPagedSortedByNameAsc(
                            groups, countries, crystalSystems, criteria.mohsMin, criteria.mohsMax,
                            statusTypes, criteria.qualityMin, criteria.qualityMax, criteria.hasPhotos, criteria.fluorescent, mineralTypes
                        )
                        SortOption.NAME_DESC -> mineralDao.filterAdvancedPagedSortedByNameDesc(
                            groups, countries, crystalSystems, criteria.mohsMin, criteria.mohsMax,
                            statusTypes, criteria.qualityMin, criteria.qualityMax, criteria.hasPhotos, criteria.fluorescent, mineralTypes
                        )
                        SortOption.DATE_NEWEST -> mineralDao.filterAdvancedPagedSortedByDateDesc(
                            groups, countries, crystalSystems, criteria.mohsMin, criteria.mohsMax,
                            statusTypes, criteria.qualityMin, criteria.qualityMax, criteria.hasPhotos, criteria.fluorescent, mineralTypes
                        )
                        SortOption.DATE_OLDEST -> mineralDao.filterAdvancedPagedSortedByDateAsc(
                            groups, countries, crystalSystems, criteria.mohsMin, criteria.mohsMax,
                            statusTypes, criteria.qualityMin, criteria.qualityMax, criteria.hasPhotos, criteria.fluorescent, mineralTypes
                        )
                        SortOption.GROUP -> mineralDao.filterAdvancedPagedSortedByGroup(
                            groups, countries, crystalSystems, criteria.mohsMin, criteria.mohsMax,
                            statusTypes, criteria.qualityMin, criteria.qualityMax, criteria.hasPhotos, criteria.fluorescent, mineralTypes
                        )
                        SortOption.HARDNESS_LOW -> mineralDao.filterAdvancedPagedSortedByHardnessAsc(
                            groups, countries, crystalSystems, criteria.mohsMin, criteria.mohsMax,
                            statusTypes, criteria.qualityMin, criteria.qualityMax, criteria.hasPhotos, criteria.fluorescent, mineralTypes
                        )
                        SortOption.HARDNESS_HIGH -> mineralDao.filterAdvancedPagedSortedByHardnessDesc(
                            groups, countries, crystalSystems, criteria.mohsMin, criteria.mohsMax,
                            statusTypes, criteria.qualityMin, criteria.qualityMax, criteria.hasPhotos, criteria.fluorescent, mineralTypes
                        )
                    }
                )
            }
        ).flow
    }

    // Quick Win #8: Get all unique tags for autocomplete (v1.7.0)
    override suspend fun getAllUniqueTags(): List<String> {
        val allTagStrings = mineralDao.getAllTags()
        // Tags are stored as comma-separated strings in the DB
        // Parse them and return unique sorted list
        return allTagStrings
            .flatMap { tagString ->
                tagString.split(",").map { it.trim() }
            }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
}
