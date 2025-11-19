package net.meshcore.mineralog.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.dao.MineralComponentDao
import net.meshcore.mineralog.data.local.dao.MineralDaoComposite
import net.meshcore.mineralog.data.local.dao.PhotoDao
import net.meshcore.mineralog.data.local.dao.ProvenanceDao
import net.meshcore.mineralog.data.local.dao.StorageDao
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.paging.MineralPagingSource
import net.meshcore.mineralog.data.mapper.*
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.Photo
import net.meshcore.mineralog.domain.model.Provenance
import net.meshcore.mineralog.domain.model.Storage
import net.meshcore.mineralog.domain.sorting.MineralSortStrategy
import net.meshcore.mineralog.ui.screens.home.SortOption

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

@Singleton
class MineralRepositoryImpl @Inject constructor(
    internal val database: MineraLogDatabase,
    private val mineralDao: MineralDaoComposite,
    private val provenanceDao: ProvenanceDao,
    private val storageDao: StorageDao,
    private val photoDao: PhotoDao,
    private val mineralComponentDao: MineralComponentDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MineralRepository {

    private suspend fun <T> ioCall(block: suspend () -> T): T = withContext(ioDispatcher) { block() }

    private suspend fun mapToDomain(entities: List<MineralEntity>): List<Mineral> {
        if (entities.isEmpty()) return emptyList()

        val mineralIds = entities.map { it.id }
        val provenances = provenanceDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
        val storages = storageDao.getByMineralIds(mineralIds).associateBy { it.mineralId }
        val photos = photoDao.getByMineralIds(mineralIds).groupBy { it.mineralId }
        val aggregateIds = entities.filter { it.type == "AGGREGATE" }.map { it.id }
        val components = if (aggregateIds.isNotEmpty()) {
            mineralComponentDao.getByAggregateIds(aggregateIds).groupBy { it.aggregateId }
        } else {
            emptyMap()
        }

        return entities.map { entity ->
            entity.toDomain(
                provenance = provenances[entity.id],
                storage = storages[entity.id],
                photos = photos[entity.id] ?: emptyList(),
                components = components[entity.id] ?: emptyList()
            )
        }
    }

    override suspend fun insert(mineral: Mineral): String = ioCall {
        database.withTransaction {
            mineralDao.insert(mineral.toEntity())
            mineral.provenance?.let { provenanceDao.insert(it.toEntity()) }
            mineral.storage?.let { storageDao.insert(it.toEntity()) }

            photoDao.deleteByMineralId(mineral.id)
            if (mineral.photos.isNotEmpty()) {
                photoDao.insertAll(mineral.photos.map { it.toEntity() })
            }

            mineralComponentDao.deleteByAggregateId(mineral.id)
            if (mineral.components.isNotEmpty()) {
                val componentEntities = mineral.components.mapIndexed { index, component ->
                    component.toEntity(mineral.id, index)
                }
                mineralComponentDao.insertAll(componentEntities)
            }

            mineral.id
        }
    }

    override suspend fun update(mineral: Mineral) = ioCall {
        database.withTransaction {
            mineralDao.update(mineral.toEntity())
            mineral.provenance?.let { provenanceDao.insert(it.toEntity()) }
            mineral.storage?.let { storageDao.insert(it.toEntity()) }

            photoDao.deleteByMineralId(mineral.id)
            if (mineral.photos.isNotEmpty()) {
                photoDao.insertAll(mineral.photos.map { it.toEntity() })
            }

            mineralComponentDao.deleteByAggregateId(mineral.id)
            if (mineral.components.isNotEmpty()) {
                val componentEntities = mineral.components.mapIndexed { index, component ->
                    component.toEntity(mineral.id, index)
                }
                mineralComponentDao.insertAll(componentEntities)
            }
        }
    }

    override suspend fun delete(id: String) = ioCall {
        database.withTransaction {
            provenanceDao.deleteByMineralId(id)
            storageDao.deleteByMineralId(id)
            photoDao.deleteByMineralId(id)
            mineralComponentDao.deleteByAggregateId(id)
            mineralDao.deleteById(id)
        }
    }

    override suspend fun deleteByIds(ids: List<String>) {
        if (ids.isEmpty()) return

        ioCall {
            database.withTransaction {
                provenanceDao.deleteByMineralIds(ids)
                storageDao.deleteByMineralIds(ids)
                photoDao.deleteByMineralIds(ids)
                mineralComponentDao.deleteByAggregateIds(ids)
                mineralDao.deleteByIds(ids)
            }
        }
    }

    override suspend fun deleteAll() = ioCall {
        database.withTransaction {
            mineralDao.deleteAll()
            provenanceDao.deleteAll()
            storageDao.deleteAll()
            photoDao.deleteAll()
            mineralComponentDao.deleteAll()
        }
    }

    override suspend fun getById(id: String): Mineral? = ioCall {
        val entity = mineralDao.getById(id) ?: return@ioCall null
        mapToDomain(listOf(entity)).firstOrNull()
    }

    override suspend fun getByIds(ids: List<String>): List<Mineral> {
        if (ids.isEmpty()) return emptyList()

        return ioCall {
            val entities = mineralDao.getByIds(ids)
            mapToDomain(entities)
        }
    }

    override fun getByIdFlow(id: String): Flow<Mineral?> {
        return combine(
            mineralDao.getByIdFlow(id),
            provenanceDao.getByMineralIdFlow(id),
            storageDao.getByMineralIdFlow(id),
            photoDao.getByMineralIdFlow(id),
            mineralComponentDao.getByAggregateIdFlow(id)
        ) { mineral, provenance, storage, photos, components ->
            mineral?.toDomain(provenance, storage, photos, components)
        }
    }

    override fun getAllFlow(sortOption: SortOption): Flow<List<Mineral>> {
        return mineralDao.getAllFlow().map { entities ->
            val minerals = mapToDomain(entities)
            MineralSortStrategy.sort(minerals, sortOption)
        }
    }

    override suspend fun getAll(): List<Mineral> = ioCall {
        val entities = mineralDao.getAll()
        mapToDomain(entities)
    }

    override fun searchFlow(query: String, sortOption: SortOption): Flow<List<Mineral>> {
        // Pre-format query with wildcards to prevent SQL injection
        val formattedQuery = "%$query%"
        return mineralDao.searchFlow(formattedQuery).map { entities ->
            val minerals = mapToDomain(entities)
            MineralSortStrategy.sort(minerals, sortOption)
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
            val minerals = mapToDomain(entities)
            MineralSortStrategy.sort(minerals, sortOption)
        }
    }

    override suspend fun getCount(): Int = ioCall { mineralDao.getCount() }

    override fun getCountFlow(): Flow<Int> = mineralDao.getCountFlow()

    override suspend fun insertProvenance(provenance: Provenance) = ioCall {
        provenanceDao.insert(provenance.toEntity())
    }

    override suspend fun updateProvenance(provenance: Provenance) = ioCall {
        provenanceDao.update(provenance.toEntity())
    }

    override suspend fun insertStorage(storage: Storage) = ioCall {
        storageDao.insert(storage.toEntity())
    }

    override suspend fun updateStorage(storage: Storage) = ioCall {
        storageDao.update(storage.toEntity())
    }

    override suspend fun insertPhoto(photo: Photo) = ioCall {
        photoDao.insert(photo.toEntity())
    }

    override suspend fun deletePhoto(photoId: String) = ioCall {
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
                    mineralComponentDao = mineralComponentDao,
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
                    mineralComponentDao = mineralComponentDao,
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
                    mineralComponentDao = mineralComponentDao,
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
    override suspend fun getAllUniqueTags(): List<String> = ioCall {
        val allTagStrings = mineralDao.getAllTags()
        allTagStrings
            .flatMap { tagString -> tagString.split(",").map { it.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
}
