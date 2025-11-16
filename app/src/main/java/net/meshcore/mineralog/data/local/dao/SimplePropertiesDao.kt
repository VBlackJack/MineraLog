package net.meshcore.mineralog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity

/**
 * Data Access Object for simple mineral properties.
 *
 * Provides CRUD operations for the simple_properties table, which stores
 * physical and chemical properties of simple (non-aggregate) minerals.
 *
 * Each mineral of type SIMPLE has exactly one SimplePropertiesEntity.
 */
@Dao
interface SimplePropertiesDao {

    /**
     * Insert a new simple properties entry.
     * If a properties entry with the same ID already exists, it will be replaced.
     *
     * @param properties The SimplePropertiesEntity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(properties: SimplePropertiesEntity)

    /**
     * Update an existing simple properties entry.
     *
     * @param properties The SimplePropertiesEntity to update.
     */
    @Update
    suspend fun update(properties: SimplePropertiesEntity)

    /**
     * Get the properties for a specific mineral by its ID (one-shot).
     *
     * @param mineralId The ID of the mineral.
     * @return The SimplePropertiesEntity for this mineral, or null if not found.
     */
    @Query("SELECT * FROM simple_properties WHERE mineralId = :mineralId")
    suspend fun getByMineralId(mineralId: String): SimplePropertiesEntity?

    /**
     * Get the properties for a specific mineral by its ID (reactive Flow).
     * Useful for observing property changes in the UI.
     *
     * @param mineralId The ID of the mineral.
     * @return A Flow emitting the SimplePropertiesEntity, or null if not found.
     */
    @Query("SELECT * FROM simple_properties WHERE mineralId = :mineralId")
    fun getByMineralIdFlow(mineralId: String): Flow<SimplePropertiesEntity?>

    /**
     * Delete properties for a specific mineral.
     * This is typically called when converting a SIMPLE mineral to AGGREGATE
     * or when deleting a mineral (CASCADE handles automatic deletion).
     *
     * @param mineralId The ID of the mineral whose properties should be deleted.
     */
    @Query("DELETE FROM simple_properties WHERE mineralId = :mineralId")
    suspend fun deleteByMineralId(mineralId: String)

    /**
     * Get all simple properties (for debugging or batch operations).
     *
     * @return A Flow emitting a list of all SimplePropertiesEntity entries.
     */
    @Query("SELECT * FROM simple_properties ORDER BY mineralId ASC")
    fun getAll(): Flow<List<SimplePropertiesEntity>>

    /**
     * Count the number of simple properties entries.
     * Useful for statistics and validation.
     *
     * @return The total count of simple properties entries.
     */
    @Query("SELECT COUNT(*) FROM simple_properties")
    suspend fun count(): Int

    /**
     * Get all simple properties entries (one-shot, for migration).
     * Use this for batch operations where Flow is not needed.
     *
     * @return A list of all SimplePropertiesEntity entries.
     */
    @Query("SELECT * FROM simple_properties ORDER BY mineralId ASC")
    suspend fun getAllDirect(): List<SimplePropertiesEntity>

    /**
     * Update the reference mineral ID for a simple specimen.
     * Used during automatic migration to link specimens to references.
     *
     * @param mineralId The ID of the simple mineral specimen
     * @param referenceMineralId The ID of the reference mineral to link to
     */
    @Query("UPDATE simple_properties SET referenceMineralId = :referenceMineralId WHERE mineralId = :mineralId")
    suspend fun updateReferenceMineralId(mineralId: String, referenceMineralId: String?)
}
