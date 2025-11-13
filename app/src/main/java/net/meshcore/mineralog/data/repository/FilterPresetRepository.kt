package net.meshcore.mineralog.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.meshcore.mineralog.data.local.dao.FilterPresetDao
import net.meshcore.mineralog.data.local.entity.FilterPresetEntity
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.domain.model.FilterPreset
import java.time.Instant

/**
 * Repository for managing filter presets.
 * Handles serialization/deserialization of FilterCriteria.
 */
interface FilterPresetRepository {
    suspend fun save(preset: FilterPreset): String
    suspend fun update(preset: FilterPreset)
    suspend fun delete(id: String)
    suspend fun deleteAll()
    suspend fun getById(id: String): FilterPreset?
    fun getByIdFlow(id: String): Flow<FilterPreset?>
    fun getAllFlow(): Flow<List<FilterPreset>>
    suspend fun getAll(): List<FilterPreset>
    suspend fun getCount(): Int
}

class FilterPresetRepositoryImpl(
    private val filterPresetDao: FilterPresetDao
) : FilterPresetRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun save(preset: FilterPreset): String {
        filterPresetDao.insert(preset.toEntity())
        return preset.id
    }

    override suspend fun update(preset: FilterPreset) {
        filterPresetDao.update(preset.toEntity())
    }

    override suspend fun delete(id: String) {
        filterPresetDao.deleteById(id)
    }

    override suspend fun deleteAll() {
        filterPresetDao.deleteAll()
    }

    override suspend fun getById(id: String): FilterPreset? {
        return filterPresetDao.getById(id)?.toDomain()
    }

    override fun getByIdFlow(id: String): Flow<FilterPreset?> {
        return filterPresetDao.getByIdFlow(id).map { it?.toDomain() }
    }

    override fun getAllFlow(): Flow<List<FilterPreset>> {
        return filterPresetDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAll(): List<FilterPreset> {
        return filterPresetDao.getAll().map { it.toDomain() }
    }

    override suspend fun getCount(): Int = filterPresetDao.getCount()

    // Mappers
    private fun FilterPreset.toEntity(): FilterPresetEntity {
        return FilterPresetEntity(
            id = id,
            name = name,
            icon = icon,
            criteriaJson = json.encodeToString(criteria),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun FilterPresetEntity.toDomain(): FilterPreset {
        return FilterPreset(
            id = id,
            name = name,
            icon = icon,
            criteria = json.decodeFromString<FilterCriteria>(criteriaJson),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
