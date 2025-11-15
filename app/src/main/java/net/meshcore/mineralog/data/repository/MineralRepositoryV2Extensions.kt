package net.meshcore.mineralog.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.mapper.toDomain
import net.meshcore.mineralog.data.mapper.toEntity
import net.meshcore.mineralog.domain.model.MineralComponent
import net.meshcore.mineralog.domain.model.SimpleProperties
import java.time.Instant
import java.util.UUID

/**
 * v2.0 Extension functions for MineralRepository to support mineral aggregates.
 *
 * These extensions provide additional methods for working with SIMPLE and AGGREGATE
 * type minerals without breaking the existing v1.x API.
 */

/**
 * Data class representing a simple mineral with its properties (v2.0).
 */
data class SimpleMineralData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val properties: SimpleProperties,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val statusType: String = "in_collection",
    val qualityRating: Int? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

/**
 * Data class representing an aggregate mineral with its components (v2.0).
 */
data class AggregateMineralData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val components: List<MineralComponent>,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val statusType: String = "in_collection",
    val qualityRating: Int? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

/**
 * Insert a simple mineral with its properties (v2.0).
 *
 * @param mineral The simple mineral data to insert.
 * @return The ID of the inserted mineral.
 */
suspend fun MineralRepositoryImpl.insertSimpleMineral(
    mineral: SimpleMineralData
): String {
    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    return db.withTransaction {
        // Insert mineral entity
        val mineralEntity = MineralEntity(
            id = mineral.id,
            name = mineral.name,
            type = "SIMPLE",
            notes = mineral.notes,
            tags = mineral.tags.joinToString(","),
            statusType = mineral.statusType,
            qualityRating = mineral.qualityRating,
            createdAt = mineral.createdAt,
            updatedAt = mineral.updatedAt
        )
        db.mineralDao().insert(mineralEntity)

        // Insert properties
        val propertiesEntity = mineral.properties.toEntity(mineral.id)
        db.simplePropertiesDao().insert(propertiesEntity)

        mineral.id
    }
}

/**
 * Insert an aggregate mineral with its components (v2.0).
 *
 * @param aggregate The aggregate mineral data to insert.
 * @return The ID of the inserted aggregate.
 */
suspend fun MineralRepositoryImpl.insertAggregate(
    aggregate: AggregateMineralData
): String {
    require(aggregate.components.isNotEmpty()) {
        "Aggregate must have at least one component"
    }

    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    return db.withTransaction {
        // Insert mineral entity
        val mineralEntity = MineralEntity(
            id = aggregate.id,
            name = aggregate.name,
            type = "AGGREGATE",
            notes = aggregate.notes,
            tags = aggregate.tags.joinToString(","),
            statusType = aggregate.statusType,
            qualityRating = aggregate.qualityRating,
            createdAt = aggregate.createdAt,
            updatedAt = aggregate.updatedAt
        )
        db.mineralDao().insert(mineralEntity)

        // Insert components
        val componentEntities = aggregate.components.mapIndexed { index, component ->
            component.toEntity(aggregate.id, index)
        }
        db.mineralComponentDao().insertAll(componentEntities)

        aggregate.id
    }
}

/**
 * Update an aggregate's components (v2.0).
 *
 * This replaces all existing components with the new list.
 *
 * @param aggregateId The ID of the aggregate to update.
 * @param components The new list of components.
 */
suspend fun MineralRepositoryImpl.updateAggregateComponents(
    aggregateId: String,
    components: List<MineralComponent>
) {
    require(components.isNotEmpty()) {
        "Aggregate must have at least one component"
    }

    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    db.withTransaction {
        // Delete existing components
        db.mineralComponentDao().deleteByAggregateId(aggregateId)

        // Insert new components
        val componentEntities = components.mapIndexed { index, component ->
            component.toEntity(aggregateId, index)
        }
        db.mineralComponentDao().insertAll(componentEntities)

        // Update mineral's updatedAt timestamp
        val mineral = db.mineralDao().getById(aggregateId)
        mineral?.let {
            db.mineralDao().update(it.copy(updatedAt = Instant.now()))
        }
    }
}

/**
 * Get simple properties for a mineral (v2.0).
 *
 * @param mineralId The ID of the mineral.
 * @return The simple properties, or null if not found or not a SIMPLE type mineral.
 */
suspend fun MineralRepositoryImpl.getSimpleProperties(mineralId: String): SimpleProperties? {
    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    val mineral = db.mineralDao().getById(mineralId)
    if (mineral?.type != "SIMPLE") return null

    return db.simplePropertiesDao().getByMineralId(mineralId)?.toDomain()
}

/**
 * Get components for an aggregate mineral (v2.0).
 *
 * @param aggregateId The ID of the aggregate.
 * @return The list of components, or empty list if not found or not an AGGREGATE type mineral.
 */
suspend fun MineralRepositoryImpl.getAggregateComponents(aggregateId: String): List<MineralComponent> {
    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    val mineral = db.mineralDao().getById(aggregateId)
    if (mineral?.type != "AGGREGATE") return emptyList()

    return db.mineralComponentDao().getByAggregateId(aggregateId)
        .map { it.toDomain() }
}

/**
 * Get components for an aggregate mineral as Flow (v2.0).
 *
 * @param aggregateId The ID of the aggregate.
 * @return A Flow of component lists.
 */
fun MineralRepositoryImpl.getAggregateComponentsFlow(aggregateId: String): Flow<List<MineralComponent>> {
    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    return db.mineralComponentDao().getByAggregateIdFlow(aggregateId)
        .map { entities -> entities.map { it.toDomain() } }
}

/**
 * Search aggregates by component name (v2.0).
 *
 * @param componentName The component name to search for (supports LIKE pattern with %).
 * @return A Flow of minerals containing the specified component.
 */
fun MineralRepositoryImpl.searchAggregatesByComponent(componentName: String): Flow<List<MineralEntity>> {
    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    return db.mineralComponentDao().searchAggregatesByComponent(componentName)
}

/**
 * Get all simple minerals (v2.0).
 *
 * @return A Flow of all simple (non-aggregate) minerals.
 */
fun MineralRepositoryImpl.getAllSimpleMinerals(): Flow<List<MineralEntity>> {
    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    return db.mineralDao().getAllSimpleMinerals()
}

/**
 * Get all aggregates (v2.0).
 *
 * @return A Flow of all aggregate minerals.
 */
fun MineralRepositoryImpl.getAllAggregates(): Flow<List<MineralEntity>> {
    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    return db.mineralDao().getAllAggregates()
}

/**
 * Count minerals by type (v2.0).
 *
 * @param type The mineral type ("SIMPLE", "AGGREGATE", "ROCK").
 * @return The count of minerals of the specified type.
 */
suspend fun MineralRepositoryImpl.countByType(type: String): Int {
    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    return db.mineralDao().countByType(type)
}

/**
 * Get mineral type distribution statistics (v2.0).
 *
 * @return A map of type names to counts.
 */
suspend fun MineralRepositoryImpl.getTypeDistribution(): Map<String, Int> {
    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    return db.mineralDao().getTypeDistribution()
}

/**
 * Helper function to validate component percentages.
 *
 * @param components The list of components to validate.
 * @return True if total is within acceptable range (99-101%), false otherwise.
 */
fun validateComponentPercentages(components: List<MineralComponent>): Boolean {
    val total = components.mapNotNull { it.percentage }.sum()
    return total in 99.0f..101.0f  // Allow 1% tolerance for rounding
}

/**
 * Helper function to get component count for an aggregate.
 *
 * @param aggregateId The ID of the aggregate.
 * @return The number of components.
 */
suspend fun MineralRepositoryImpl.getComponentCount(aggregateId: String): Int {
    val db = this.javaClass.getDeclaredField("database").let { field ->
        field.isAccessible = true
        field.get(this) as MineraLogDatabase
    }

    return db.mineralComponentDao().countByAggregateId(aggregateId)
}
