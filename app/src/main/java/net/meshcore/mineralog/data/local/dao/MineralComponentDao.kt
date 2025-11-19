package net.meshcore.mineralog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.MineralComponentEntity
import net.meshcore.mineralog.data.local.entity.MineralEntity

/**
 * Data Access Object for mineral components in aggregates.
 *
 * Provides CRUD operations for the mineral_components table, which stores
 * information about individual minerals that make up an aggregate.
 *
 * Each aggregate mineral can have multiple components (e.g., Granite has
 * Quartz, Feldspath, Mica components).
 */
@Dao
interface MineralComponentDao {

    /**
     * Insert a new mineral component.
     * If a component with the same ID already exists, it will be replaced.
     *
     * @param component The MineralComponentEntity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(component: MineralComponentEntity)

    /**
     * Insert multiple mineral components in a single transaction.
     * Useful when creating or updating an aggregate with all its components.
     *
     * @param components List of MineralComponentEntity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(components: List<MineralComponentEntity>)

    /**
     * Update an existing mineral component.
     *
     * @param component The MineralComponentEntity to update.
     */
    @Update
    suspend fun update(component: MineralComponentEntity)

    /**
     * Delete a specific mineral component.
     *
     * @param component The MineralComponentEntity to delete.
     */
    @Delete
    suspend fun delete(component: MineralComponentEntity)

    /**
     * Get all components for a specific aggregate mineral (one-shot).
     * Components are returned in display order.
     *
     * @param aggregateId The ID of the aggregate mineral.
     * @return A list of MineralComponentEntity ordered by displayOrder.
     */
    @Query("SELECT * FROM mineral_components WHERE aggregateId = :aggregateId ORDER BY displayOrder ASC")
    suspend fun getByAggregateId(aggregateId: String): List<MineralComponentEntity>

    /**
     * Batch load all components for the provided aggregate IDs.
     */
    @Query("SELECT * FROM mineral_components WHERE aggregateId IN (:aggregateIds) ORDER BY aggregateId, displayOrder ASC")
    suspend fun getByAggregateIds(aggregateIds: List<String>): List<MineralComponentEntity>

    /**
     * Get all components for a specific aggregate mineral (reactive Flow).
     * Components are returned in display order.
     * Useful for observing component changes in the UI.
     *
     * @param aggregateId The ID of the aggregate mineral.
     * @return A Flow emitting a list of MineralComponentEntity ordered by displayOrder.
     */
    @Query("SELECT * FROM mineral_components WHERE aggregateId = :aggregateId ORDER BY displayOrder ASC")
    fun getByAggregateIdFlow(aggregateId: String): Flow<List<MineralComponentEntity>>

    /**
     * Delete all components for a specific aggregate mineral.
     * This is typically called when deleting an aggregate (CASCADE handles automatic deletion)
     * or when replacing all components of an aggregate.
     *
     * @param aggregateId The ID of the aggregate mineral.
     */
    @Query("DELETE FROM mineral_components WHERE aggregateId = :aggregateId")
    suspend fun deleteByAggregateId(aggregateId: String)

    @Query("DELETE FROM mineral_components WHERE aggregateId IN (:aggregateIds)")
    suspend fun deleteByAggregateIds(aggregateIds: List<String>)

    /**
     * Delete a specific component by its ID.
     *
     * @param componentId The ID of the component to delete.
     */
    @Query("DELETE FROM mineral_components WHERE id = :componentId")
    suspend fun deleteById(componentId: String)

    /**
     * Search for aggregates containing a specific mineral component.
     * Useful for queries like "find all aggregates containing Quartz".
     *
     * @param componentName The name of the mineral component to search for (supports LIKE pattern).
     * @return A Flow emitting a list of MineralEntity aggregates containing the component.
     */
    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        INNER JOIN mineral_components c ON m.id = c.aggregateId
        WHERE c.mineralName LIKE :componentName
        ORDER BY m.name ASC
    """)
    fun searchAggregatesByComponent(componentName: String): Flow<List<MineralEntity>>

    /**
     * Get all principal components across all aggregates.
     * Useful for statistics and analysis.
     *
     * @return A Flow emitting all components with role='PRINCIPAL'.
     */
    @Query("SELECT * FROM mineral_components WHERE role = 'PRINCIPAL' ORDER BY mineralName ASC")
    fun getAllPrincipalComponents(): Flow<List<MineralComponentEntity>>

    /**
     * Get components by role.
     *
     * @param role The ComponentRole value ("PRINCIPAL", "ACCESSORY", or "TRACE").
     * @return A Flow emitting all components with the specified role.
     */
    @Query("SELECT * FROM mineral_components WHERE role = :role ORDER BY mineralName ASC")
    fun getByRole(role: String): Flow<List<MineralComponentEntity>>

    /**
     * Count the number of components for a specific aggregate.
     *
     * @param aggregateId The ID of the aggregate mineral.
     * @return The count of components.
     */
    @Query("SELECT COUNT(*) FROM mineral_components WHERE aggregateId = :aggregateId")
    suspend fun countByAggregateId(aggregateId: String): Int

    /**
     * Get the most common mineral components across all aggregates.
     * Returns component names with their occurrence count.
     *
     * @param limit Maximum number of results to return.
     * @return A list of pairs (mineralName, count) ordered by frequency descending.
     */
    @Query("""
        SELECT mineralName, COUNT(*) as count
        FROM mineral_components
        GROUP BY mineralName
        ORDER BY count DESC
        LIMIT :limit
    """)
    suspend fun getMostFrequentComponents(limit: Int = 10): List<ComponentFrequency>

    /**
     * Delete all components (for testing or data reset).
     */
    @Query("DELETE FROM mineral_components")
    suspend fun deleteAll()

    /**
     * Get all components (one-shot, for migration).
     * Use this for batch operations where Flow is not needed.
     *
     * @return A list of all MineralComponentEntity entries.
     */
    @Query("SELECT * FROM mineral_components ORDER BY mineralName ASC")
    suspend fun getAllDirect(): List<MineralComponentEntity>

    /**
     * Update the reference mineral ID for a component.
     * Used during automatic migration to link components to references.
     *
     * @param componentId The ID of the component
     * @param referenceMineralId The ID of the reference mineral to link to
     */
    @Query("UPDATE mineral_components SET referenceMineralId = :referenceMineralId WHERE id = :componentId")
    suspend fun updateReferenceMineralId(componentId: String, referenceMineralId: String?)

    /**
     * Data class for component frequency results.
     */
    data class ComponentFrequency(
        val mineralName: String,
        val count: Int
    )
}
