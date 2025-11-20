package net.meshcore.mineralog.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity

/**
 * Data Access Object for reference minerals library.
 *
 * Provides CRUD operations for the reference_minerals table, which stores
 * a library of mineral templates with standardized properties. These reference
 * minerals can be used to auto-fill properties when creating specimens.
 *
 * The library contains both pre-populated minerals (from initial dataset) and
 * user-defined custom minerals.
 */
@Dao
interface ReferenceMineralDao {

    // ============================================================
    // CRUD Operations
    // ============================================================

    /**
     * Insert a new reference mineral.
     * If a mineral with the same ID already exists, it will be replaced.
     *
     * @param mineral The ReferenceMineralEntity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mineral: ReferenceMineralEntity)

    /**
     * Insert multiple reference minerals in a single transaction.
     * Useful for initial dataset population or batch imports.
     *
     * @param minerals List of ReferenceMineralEntity to insert.
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(minerals: List<ReferenceMineralEntity>)

    /**
     * Update an existing reference mineral.
     *
     * @param mineral The ReferenceMineralEntity to update.
     */
    @Update
    suspend fun update(mineral: ReferenceMineralEntity)

    /**
     * Delete a specific reference mineral.
     *
     * @param mineral The ReferenceMineralEntity to delete.
     */
    @Delete
    suspend fun delete(mineral: ReferenceMineralEntity)

    /**
     * Delete a reference mineral by its ID.
     *
     * @param id The ID of the reference mineral to delete.
     */
    @Query("DELETE FROM reference_minerals WHERE id = :id")
    suspend fun deleteById(id: String)

    // ============================================================
    // Basic Queries
    // ============================================================

    /**
     * Get a reference mineral by its ID (one-shot).
     *
     * @param id The ID of the reference mineral.
     * @return The ReferenceMineralEntity, or null if not found.
     */
    @Query("SELECT * FROM reference_minerals WHERE id = :id")
    suspend fun getById(id: String): ReferenceMineralEntity?

    /**
     * Get a reference mineral by its ID (reactive Flow).
     * Useful for observing changes in the UI.
     *
     * @param id The ID of the reference mineral.
     * @return A Flow emitting the ReferenceMineralEntity, or null if not found.
     */
    @Query("SELECT * FROM reference_minerals WHERE id = :id")
    fun getByIdFlow(id: String): Flow<ReferenceMineralEntity?>

    /**
     * Get all reference minerals (reactive Flow).
     * Results are ordered by French name.
     *
     * @return A Flow emitting a list of all ReferenceMineralEntity.
     */
    @Query("SELECT * FROM reference_minerals ORDER BY nameFr ASC")
    fun getAllFlow(): Flow<List<ReferenceMineralEntity>>

    /**
     * Get all reference minerals with pagination support.
     * Results are ordered by French name.
     *
     * @return A PagingSource for efficient pagination.
     */
    @Query("SELECT * FROM reference_minerals ORDER BY nameFr ASC")
    fun getAllPaged(): PagingSource<Int, ReferenceMineralEntity>

    // ============================================================
    // Search Queries
    // ============================================================

    /**
     * Search reference minerals by name (French or English).
     * Uses LIKE pattern matching with ranking by exact match priority.
     *
     * @param query The search query (will be wrapped with % wildcards).
     * @return A Flow emitting matching ReferenceMineralEntity, ordered by relevance.
     */
    @Query("""
        SELECT * FROM reference_minerals
        WHERE nameFr LIKE '%' || :query || '%' COLLATE NOCASE
           OR nameEn LIKE '%' || :query || '%' COLLATE NOCASE
           OR synonyms LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY
            CASE
                WHEN LOWER(nameFr) = LOWER(:query) THEN 1
                WHEN LOWER(nameEn) = LOWER(:query) THEN 2
                WHEN nameFr LIKE :query || '%' COLLATE NOCASE THEN 3
                WHEN nameEn LIKE :query || '%' COLLATE NOCASE THEN 4
                ELSE 5
            END,
            nameFr ASC
    """)
    fun searchByName(query: String): Flow<List<ReferenceMineralEntity>>

    /**
     * Search reference minerals by name with result limit.
     * Useful for autocomplete suggestions.
     *
     * @param query The search query (will be wrapped with % wildcards).
     * @param limit Maximum number of results to return.
     * @return A Flow emitting matching ReferenceMineralEntity (limited), ordered by relevance.
     */
    @Query("""
        SELECT * FROM reference_minerals
        WHERE nameFr LIKE '%' || :query || '%' COLLATE NOCASE
           OR nameEn LIKE '%' || :query || '%' COLLATE NOCASE
           OR synonyms LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY
            CASE
                WHEN LOWER(nameFr) = LOWER(:query) THEN 1
                WHEN LOWER(nameEn) = LOWER(:query) THEN 2
                WHEN nameFr LIKE :query || '%' COLLATE NOCASE THEN 3
                WHEN nameEn LIKE :query || '%' COLLATE NOCASE THEN 4
                ELSE 5
            END,
            nameFr ASC
        LIMIT :limit
    """)
    fun searchByNameLimit(query: String, limit: Int = 10): Flow<List<ReferenceMineralEntity>>

    // ============================================================
    // Filter Queries
    // ============================================================

    /**
     * Filter reference minerals with pagination support.
     * All filter parameters are optional (null = no filter).
     *
     * @param groups List of mineral groups to filter by (OR logic), or null for no group filter.
     * @param crystalSystems List of crystal systems to filter by (OR logic), or null for no system filter.
     * @param mohsMin Minimum Mohs hardness filter, or null for no minimum.
     * @param mohsMax Maximum Mohs hardness filter, or null for no maximum.
     * @param isUserDefined Filter by user-defined flag, or null for all minerals.
     * @return A PagingSource for efficient pagination.
     */
    @Query("""
        SELECT * FROM reference_minerals
        WHERE (:groups IS NULL OR mineralGroup IN (:groups))
          AND (:crystalSystems IS NULL OR crystalSystem IN (:crystalSystems))
          AND (:mohsMin IS NULL OR mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR mohsMin <= :mohsMax)
          AND (:isUserDefined IS NULL OR isUserDefined = :isUserDefined)
        ORDER BY nameFr ASC
    """)
    fun filterPaged(
        groups: List<String>?,
        crystalSystems: List<String>?,
        mohsMin: Float?,
        mohsMax: Float?,
        isUserDefined: Boolean?
    ): PagingSource<Int, ReferenceMineralEntity>

    // ============================================================
    // Statistics and Usage Queries
    // ============================================================

    /**
     * Count how many simple mineral specimens are using this reference mineral.
     *
     * @param referenceMineralId The ID of the reference mineral.
     * @return The count of simple specimens linked to this reference.
     */
    @Query("""
        SELECT COUNT(*) FROM simple_properties
        WHERE referenceMineralId = :referenceMineralId
    """)
    suspend fun countSimpleSpecimensUsingReference(referenceMineralId: String): Int

    /**
     * Count how many aggregate components are using this reference mineral.
     *
     * @param referenceMineralId The ID of the reference mineral.
     * @return The count of aggregate components linked to this reference.
     */
    @Query("""
        SELECT COUNT(*) FROM mineral_components
        WHERE referenceMineralId = :referenceMineralId
    """)
    suspend fun countComponentsUsingReference(referenceMineralId: String): Int

    /**
     * Get total usage count (simple specimens + components) for a reference mineral.
     *
     * @param referenceMineralId The ID of the reference mineral.
     * @return The total count of entities using this reference.
     */
    @Query("""
        SELECT
            (SELECT COUNT(*) FROM simple_properties WHERE referenceMineralId = :referenceMineralId) +
            (SELECT COUNT(*) FROM mineral_components WHERE referenceMineralId = :referenceMineralId)
        AS totalUsage
    """)
    suspend fun getTotalUsageCount(referenceMineralId: String): Int

    // ============================================================
    // Distinct Values (for filters)
    // ============================================================

    /**
     * Get all distinct mineral groups in the library.
     * Useful for populating filter options.
     *
     * @return A Flow emitting a list of distinct, non-null mineral groups (sorted).
     */
    @Query("""
        SELECT DISTINCT mineralGroup FROM reference_minerals
        WHERE mineralGroup IS NOT NULL
        ORDER BY mineralGroup ASC
    """)
    fun getDistinctGroups(): Flow<List<String>>

    /**
     * Get all distinct crystal systems in the library.
     * Useful for populating filter options.
     *
     * @return A Flow emitting a list of distinct, non-null crystal systems (sorted).
     */
    @Query("""
        SELECT DISTINCT crystalSystem FROM reference_minerals
        WHERE crystalSystem IS NOT NULL
        ORDER BY crystalSystem ASC
    """)
    fun getDistinctCrystalSystems(): Flow<List<String>>

    // ============================================================
    // Utility Queries
    // ============================================================

    /**
     * Get all user-defined reference minerals.
     *
     * @return A Flow emitting all custom minerals created by the user.
     */
    @Query("SELECT * FROM reference_minerals WHERE isUserDefined = 1 ORDER BY nameFr ASC")
    fun getUserDefinedMinerals(): Flow<List<ReferenceMineralEntity>>

    /**
     * Get all standard library minerals (pre-populated).
     *
     * @return A Flow emitting all standard minerals from the initial dataset.
     */
    @Query("SELECT * FROM reference_minerals WHERE isUserDefined = 0 ORDER BY nameFr ASC")
    fun getStandardMinerals(): Flow<List<ReferenceMineralEntity>>

    /**
     * Count the total number of reference minerals.
     *
     * @return The total count of reference minerals.
     */
    @Query("SELECT COUNT(*) FROM reference_minerals")
    suspend fun count(): Int

    /**
     * Count user-defined minerals.
     *
     * @return The count of user-defined reference minerals.
     */
    @Query("SELECT COUNT(*) FROM reference_minerals WHERE isUserDefined = 1")
    suspend fun countUserDefined(): Int

    /**
     * Check if a mineral name already exists (case-insensitive).
     * Useful for validation when creating new reference minerals.
     *
     * @param nameFr French name to check.
     * @param nameEn English name to check.
     * @return True if a mineral with this name exists.
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM reference_minerals
        WHERE LOWER(nameFr) = LOWER(:nameFr) OR LOWER(nameEn) = LOWER(:nameEn)
    """)
    suspend fun existsByName(nameFr: String, nameEn: String): Boolean

    /**
     * Delete all reference minerals (for testing or data reset).
     */
    @Query("DELETE FROM reference_minerals")
    suspend fun deleteAll()
}
