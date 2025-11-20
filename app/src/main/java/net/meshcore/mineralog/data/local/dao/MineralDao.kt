package net.meshcore.mineralog.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.MineralEntity

/**
 * DAO for Mineral entity operations.
 * Provides CRUD operations and complex queries for searching and filtering.
 */
@Dao
interface MineralDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mineral: MineralEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(minerals: List<MineralEntity>)

    @Update
    suspend fun update(mineral: MineralEntity)

    @Delete
    suspend fun delete(mineral: MineralEntity)

    @Query("DELETE FROM minerals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM minerals WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM minerals")
    suspend fun deleteAll()

    @Query("SELECT * FROM minerals WHERE id = :id")
    suspend fun getById(id: String): MineralEntity?

    @Query("SELECT * FROM minerals WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MineralEntity>

    @Query("SELECT * FROM minerals WHERE id = :id")
    fun getByIdFlow(id: String): Flow<MineralEntity?>

    @Query("SELECT * FROM minerals ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<MineralEntity>>

    @Query("SELECT * FROM minerals ORDER BY updatedAt DESC")
    suspend fun getAll(): List<MineralEntity>

    // ========== v2.0 - Mineral Type Queries ==========

    /**
     * Get all simple minerals (non-aggregates).
     */
    @Query("SELECT * FROM minerals WHERE type = 'SIMPLE' ORDER BY name ASC")
    fun getAllSimpleMinerals(): Flow<List<MineralEntity>>

    /**
     * Get all aggregates.
     */
    @Query("SELECT * FROM minerals WHERE type = 'AGGREGATE' ORDER BY name ASC")
    fun getAllAggregates(): Flow<List<MineralEntity>>

    /**
     * Get minerals by type(s).
     * @param types List of mineral types ("SIMPLE", "AGGREGATE", "ROCK")
     */
    @Query("SELECT * FROM minerals WHERE type IN (:types) ORDER BY name ASC")
    fun getMineralsByType(types: List<String>): Flow<List<MineralEntity>>

    /**
     * Get minerals by type (paged).
     */
    @Query("SELECT * FROM minerals WHERE type IN (:types) ORDER BY updatedAt DESC")
    fun getMineralsByTypePaged(types: List<String>): PagingSource<Int, MineralEntity>

    /**
     * Count minerals by type.
     */
    @Query("SELECT COUNT(*) FROM minerals WHERE type = :type")
    suspend fun countByType(type: String): Int

    // ========== Paging 3 Support (v1.5.0) ==========

    /**
     * Get all minerals as paged data source.
     * Returns PagingSource for efficient loading of large datasets.
     */
    @Query("SELECT * FROM minerals ORDER BY updatedAt DESC")
    fun getAllPaged(): PagingSource<Int, MineralEntity>

    // Sorted variants for getAllPaged
    @Query("SELECT * FROM minerals ORDER BY name ASC")
    fun getAllPagedSortedByNameAsc(): PagingSource<Int, MineralEntity>

    @Query("SELECT * FROM minerals ORDER BY name DESC")
    fun getAllPagedSortedByNameDesc(): PagingSource<Int, MineralEntity>

    @Query("SELECT * FROM minerals ORDER BY updatedAt DESC")
    fun getAllPagedSortedByDateDesc(): PagingSource<Int, MineralEntity>

    @Query("SELECT * FROM minerals ORDER BY updatedAt ASC")
    fun getAllPagedSortedByDateAsc(): PagingSource<Int, MineralEntity>

    @Query("SELECT * FROM minerals ORDER BY `group` ASC, name ASC")
    fun getAllPagedSortedByGroup(): PagingSource<Int, MineralEntity>

    @Query("SELECT * FROM minerals ORDER BY mohsMin ASC, name ASC")
    fun getAllPagedSortedByHardnessAsc(): PagingSource<Int, MineralEntity>

    @Query("SELECT * FROM minerals ORDER BY mohsMax DESC, name ASC")
    fun getAllPagedSortedByHardnessDesc(): PagingSource<Int, MineralEntity>

    /**
     * Search minerals with pagination support.
     * Note: Query parameter should be pre-formatted with wildcards (e.g., "%search%")
     * to prevent SQL injection via LIKE operator concatenation.
     */
    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        LEFT JOIN mineral_components c ON m.id = c.aggregateId
        WHERE m.name LIKE :query
           OR m.`group` LIKE :query
           OR m.formula LIKE :query
           OR m.notes LIKE :query
           OR m.tags LIKE :query
           OR c.mineralName LIKE :query
        ORDER BY m.updatedAt DESC
    """)
    fun searchPaged(query: String): PagingSource<Int, MineralEntity>

    // Sorted variants for searchPaged
    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        LEFT JOIN mineral_components c ON m.id = c.aggregateId
        WHERE m.name LIKE :query OR m.`group` LIKE :query OR m.formula LIKE :query OR m.notes LIKE :query OR m.tags LIKE :query OR c.mineralName LIKE :query
        ORDER BY m.name ASC
    """)
    fun searchPagedSortedByNameAsc(query: String): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        LEFT JOIN mineral_components c ON m.id = c.aggregateId
        WHERE m.name LIKE :query OR m.`group` LIKE :query OR m.formula LIKE :query OR m.notes LIKE :query OR m.tags LIKE :query OR c.mineralName LIKE :query
        ORDER BY m.name DESC
    """)
    fun searchPagedSortedByNameDesc(query: String): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        LEFT JOIN mineral_components c ON m.id = c.aggregateId
        WHERE m.name LIKE :query OR m.`group` LIKE :query OR m.formula LIKE :query OR m.notes LIKE :query OR m.tags LIKE :query OR c.mineralName LIKE :query
        ORDER BY m.updatedAt DESC
    """)
    fun searchPagedSortedByDateDesc(query: String): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        LEFT JOIN mineral_components c ON m.id = c.aggregateId
        WHERE m.name LIKE :query OR m.`group` LIKE :query OR m.formula LIKE :query OR m.notes LIKE :query OR m.tags LIKE :query OR c.mineralName LIKE :query
        ORDER BY m.updatedAt ASC
    """)
    fun searchPagedSortedByDateAsc(query: String): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        LEFT JOIN mineral_components c ON m.id = c.aggregateId
        WHERE m.name LIKE :query OR m.`group` LIKE :query OR m.formula LIKE :query OR m.notes LIKE :query OR m.tags LIKE :query OR c.mineralName LIKE :query
        ORDER BY m.`group` ASC, m.name ASC
    """)
    fun searchPagedSortedByGroup(query: String): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        LEFT JOIN mineral_components c ON m.id = c.aggregateId
        WHERE m.name LIKE :query OR m.`group` LIKE :query OR m.formula LIKE :query OR m.notes LIKE :query OR m.tags LIKE :query OR c.mineralName LIKE :query
        ORDER BY m.mohsMin ASC, m.name ASC
    """)
    fun searchPagedSortedByHardnessAsc(query: String): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        LEFT JOIN mineral_components c ON m.id = c.aggregateId
        WHERE m.name LIKE :query OR m.`group` LIKE :query OR m.formula LIKE :query OR m.notes LIKE :query OR m.tags LIKE :query OR c.mineralName LIKE :query
        ORDER BY m.mohsMax DESC, m.name ASC
    """)
    fun searchPagedSortedByHardnessDesc(query: String): PagingSource<Int, MineralEntity>

    /**
     * Advanced filter with pagination support.
     */
    @Query("""
        SELECT m.* FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.`group` IN (:groups))
          AND (:countries IS NULL OR p.country IN (:countries))
          AND (:crystalSystems IS NULL OR m.crystalSystem IN (:crystalSystems))
          AND (:mohsMin IS NULL OR m.mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR m.mohsMin <= :mohsMax)
          AND (:statusTypes IS NULL OR m.statusType IN (:statusTypes))
          AND (:qualityMin IS NULL OR m.qualityRating >= :qualityMin)
          AND (:qualityMax IS NULL OR m.qualityRating <= :qualityMax)
          AND (:hasPhotos IS NULL OR
               (:hasPhotos = 1 AND EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)) OR
               (:hasPhotos = 0 AND NOT EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)))
          AND (:fluorescent IS NULL OR
               (:fluorescent = 1 AND m.fluorescence IS NOT NULL AND m.fluorescence != 'none') OR
               (:fluorescent = 0 AND (m.fluorescence IS NULL OR m.fluorescence = 'none')))
          AND (:mineralTypes IS NULL OR m.type IN (:mineralTypes))
        ORDER BY m.updatedAt DESC
    """)
    fun filterAdvancedPaged(
        groups: List<String>? = null,
        countries: List<String>? = null,
        crystalSystems: List<String>? = null,
        mohsMin: Float? = null,
        mohsMax: Float? = null,
        statusTypes: List<String>? = null,
        qualityMin: Int? = null,
        qualityMax: Int? = null,
        hasPhotos: Boolean? = null,
        fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity>

    // Sorted variants for filterAdvancedPaged
    @Query("""
        SELECT m.* FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.`group` IN (:groups))
          AND (:countries IS NULL OR p.country IN (:countries))
          AND (:crystalSystems IS NULL OR m.crystalSystem IN (:crystalSystems))
          AND (:mohsMin IS NULL OR m.mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR m.mohsMin <= :mohsMax)
          AND (:statusTypes IS NULL OR m.statusType IN (:statusTypes))
          AND (:qualityMin IS NULL OR m.qualityRating >= :qualityMin)
          AND (:qualityMax IS NULL OR m.qualityRating <= :qualityMax)
          AND (:hasPhotos IS NULL OR
               (:hasPhotos = 1 AND EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)) OR
               (:hasPhotos = 0 AND NOT EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)))
          AND (:fluorescent IS NULL OR
               (:fluorescent = 1 AND m.fluorescence IS NOT NULL AND m.fluorescence != 'none') OR
               (:fluorescent = 0 AND (m.fluorescence IS NULL OR m.fluorescence = 'none')))
          AND (:mineralTypes IS NULL OR m.type IN (:mineralTypes))
        ORDER BY m.name ASC
    """)
    fun filterAdvancedPagedSortedByNameAsc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT m.* FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.`group` IN (:groups))
          AND (:countries IS NULL OR p.country IN (:countries))
          AND (:crystalSystems IS NULL OR m.crystalSystem IN (:crystalSystems))
          AND (:mohsMin IS NULL OR m.mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR m.mohsMin <= :mohsMax)
          AND (:statusTypes IS NULL OR m.statusType IN (:statusTypes))
          AND (:qualityMin IS NULL OR m.qualityRating >= :qualityMin)
          AND (:qualityMax IS NULL OR m.qualityRating <= :qualityMax)
          AND (:hasPhotos IS NULL OR
               (:hasPhotos = 1 AND EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)) OR
               (:hasPhotos = 0 AND NOT EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)))
          AND (:fluorescent IS NULL OR
               (:fluorescent = 1 AND m.fluorescence IS NOT NULL AND m.fluorescence != 'none') OR
               (:fluorescent = 0 AND (m.fluorescence IS NULL OR m.fluorescence = 'none')))
          AND (:mineralTypes IS NULL OR m.type IN (:mineralTypes))
        ORDER BY m.name DESC
    """)
    fun filterAdvancedPagedSortedByNameDesc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT m.* FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.`group` IN (:groups))
          AND (:countries IS NULL OR p.country IN (:countries))
          AND (:crystalSystems IS NULL OR m.crystalSystem IN (:crystalSystems))
          AND (:mohsMin IS NULL OR m.mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR m.mohsMin <= :mohsMax)
          AND (:statusTypes IS NULL OR m.statusType IN (:statusTypes))
          AND (:qualityMin IS NULL OR m.qualityRating >= :qualityMin)
          AND (:qualityMax IS NULL OR m.qualityRating <= :qualityMax)
          AND (:hasPhotos IS NULL OR
               (:hasPhotos = 1 AND EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)) OR
               (:hasPhotos = 0 AND NOT EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)))
          AND (:fluorescent IS NULL OR
               (:fluorescent = 1 AND m.fluorescence IS NOT NULL AND m.fluorescence != 'none') OR
               (:fluorescent = 0 AND (m.fluorescence IS NULL OR m.fluorescence = 'none')))
          AND (:mineralTypes IS NULL OR m.type IN (:mineralTypes))
        ORDER BY m.updatedAt DESC
    """)
    fun filterAdvancedPagedSortedByDateDesc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT m.* FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.`group` IN (:groups))
          AND (:countries IS NULL OR p.country IN (:countries))
          AND (:crystalSystems IS NULL OR m.crystalSystem IN (:crystalSystems))
          AND (:mohsMin IS NULL OR m.mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR m.mohsMin <= :mohsMax)
          AND (:statusTypes IS NULL OR m.statusType IN (:statusTypes))
          AND (:qualityMin IS NULL OR m.qualityRating >= :qualityMin)
          AND (:qualityMax IS NULL OR m.qualityRating <= :qualityMax)
          AND (:hasPhotos IS NULL OR
               (:hasPhotos = 1 AND EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)) OR
               (:hasPhotos = 0 AND NOT EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)))
          AND (:fluorescent IS NULL OR
               (:fluorescent = 1 AND m.fluorescence IS NOT NULL AND m.fluorescence != 'none') OR
               (:fluorescent = 0 AND (m.fluorescence IS NULL OR m.fluorescence = 'none')))
          AND (:mineralTypes IS NULL OR m.type IN (:mineralTypes))
        ORDER BY m.updatedAt ASC
    """)
    fun filterAdvancedPagedSortedByDateAsc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT m.* FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.`group` IN (:groups))
          AND (:countries IS NULL OR p.country IN (:countries))
          AND (:crystalSystems IS NULL OR m.crystalSystem IN (:crystalSystems))
          AND (:mohsMin IS NULL OR m.mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR m.mohsMin <= :mohsMax)
          AND (:statusTypes IS NULL OR m.statusType IN (:statusTypes))
          AND (:qualityMin IS NULL OR m.qualityRating >= :qualityMin)
          AND (:qualityMax IS NULL OR m.qualityRating <= :qualityMax)
          AND (:hasPhotos IS NULL OR
               (:hasPhotos = 1 AND EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)) OR
               (:hasPhotos = 0 AND NOT EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)))
          AND (:fluorescent IS NULL OR
               (:fluorescent = 1 AND m.fluorescence IS NOT NULL AND m.fluorescence != 'none') OR
               (:fluorescent = 0 AND (m.fluorescence IS NULL OR m.fluorescence = 'none')))
          AND (:mineralTypes IS NULL OR m.type IN (:mineralTypes))
        ORDER BY m.`group` ASC, m.name ASC
    """)
    fun filterAdvancedPagedSortedByGroup(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT m.* FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.`group` IN (:groups))
          AND (:countries IS NULL OR p.country IN (:countries))
          AND (:crystalSystems IS NULL OR m.crystalSystem IN (:crystalSystems))
          AND (:mohsMin IS NULL OR m.mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR m.mohsMin <= :mohsMax)
          AND (:statusTypes IS NULL OR m.statusType IN (:statusTypes))
          AND (:qualityMin IS NULL OR m.qualityRating >= :qualityMin)
          AND (:qualityMax IS NULL OR m.qualityRating <= :qualityMax)
          AND (:hasPhotos IS NULL OR
               (:hasPhotos = 1 AND EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)) OR
               (:hasPhotos = 0 AND NOT EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)))
          AND (:fluorescent IS NULL OR
               (:fluorescent = 1 AND m.fluorescence IS NOT NULL AND m.fluorescence != 'none') OR
               (:fluorescent = 0 AND (m.fluorescence IS NULL OR m.fluorescence = 'none')))
          AND (:mineralTypes IS NULL OR m.type IN (:mineralTypes))
        ORDER BY m.mohsMin ASC, m.name ASC
    """)
    fun filterAdvancedPagedSortedByHardnessAsc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity>

    @Query("""
        SELECT m.* FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.`group` IN (:groups))
          AND (:countries IS NULL OR p.country IN (:countries))
          AND (:crystalSystems IS NULL OR m.crystalSystem IN (:crystalSystems))
          AND (:mohsMin IS NULL OR m.mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR m.mohsMin <= :mohsMax)
          AND (:statusTypes IS NULL OR m.statusType IN (:statusTypes))
          AND (:qualityMin IS NULL OR m.qualityRating >= :qualityMin)
          AND (:qualityMax IS NULL OR m.qualityRating <= :qualityMax)
          AND (:hasPhotos IS NULL OR
               (:hasPhotos = 1 AND EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)) OR
               (:hasPhotos = 0 AND NOT EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)))
          AND (:fluorescent IS NULL OR
               (:fluorescent = 1 AND m.fluorescence IS NOT NULL AND m.fluorescence != 'none') OR
               (:fluorescent = 0 AND (m.fluorescence IS NULL OR m.fluorescence = 'none')))
          AND (:mineralTypes IS NULL OR m.type IN (:mineralTypes))
        ORDER BY m.mohsMax DESC, m.name ASC
    """)
    fun filterAdvancedPagedSortedByHardnessDesc(
        groups: List<String>? = null, countries: List<String>? = null, crystalSystems: List<String>? = null,
        mohsMin: Float? = null, mohsMax: Float? = null, statusTypes: List<String>? = null,
        qualityMin: Int? = null, qualityMax: Int? = null, hasPhotos: Boolean? = null, fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): PagingSource<Int, MineralEntity>

    /**
     * Search minerals with Flow.
     * Note: Query parameter should be pre-formatted with wildcards (e.g., "%search%")
     */
    @Query("""
        SELECT DISTINCT m.* FROM minerals m
        LEFT JOIN mineral_components c ON m.id = c.aggregateId
        WHERE m.name LIKE :query
           OR m.`group` LIKE :query
           OR m.formula LIKE :query
           OR m.notes LIKE :query
           OR m.tags LIKE :query
           OR c.mineralName LIKE :query
        ORDER BY m.updatedAt DESC
    """)
    fun searchFlow(query: String): Flow<List<MineralEntity>>

    @Query("""
        SELECT * FROM minerals
        WHERE (:group IS NULL OR `group` = :group)
          AND (:crystalSystem IS NULL OR crystalSystem = :crystalSystem)
          AND (:status IS NULL OR status = :status)
          AND (:mohsMin IS NULL OR mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR mohsMin <= :mohsMax)
        ORDER BY updatedAt DESC
    """)
    fun filterFlow(
        group: String? = null,
        crystalSystem: String? = null,
        status: String? = null,
        mohsMin: Float? = null,
        mohsMax: Float? = null
    ): Flow<List<MineralEntity>>

    @Query("SELECT COUNT(*) FROM minerals")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM minerals")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT DISTINCT `group` FROM minerals WHERE `group` IS NOT NULL ORDER BY `group`")
    fun getDistinctGroupsFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT crystalSystem FROM minerals WHERE crystalSystem IS NOT NULL ORDER BY crystalSystem")
    fun getDistinctCrystalSystemsFlow(): Flow<List<String>>

    // Quick Win #8: Get all unique tags for autocomplete (v1.7.0)
    @Query("SELECT tags FROM minerals WHERE tags IS NOT NULL AND tags != ''")
    suspend fun getAllTags(): List<String>

    // ========== Statistics & Aggregations (v1.2.0) ==========

    /**
     * Get distribution of minerals by group.
     * Returns map of group name to count.
     */
    @Query("""
        SELECT `group`, COUNT(*) as count
        FROM minerals
        WHERE `group` IS NOT NULL
        GROUP BY `group`
        ORDER BY count DESC
    """)
    suspend fun getGroupDistribution(): Map<@MapColumn(columnName = "group") String, @MapColumn(columnName = "count") Int>

    /**
     * Get distribution of minerals by country (from provenance).
     * Requires join with provenance table.
     */
    @Query("""
        SELECT p.country, COUNT(*) as count
        FROM minerals m
        INNER JOIN provenances p ON m.provenanceId = p.id
        WHERE p.country IS NOT NULL
        GROUP BY p.country
        ORDER BY count DESC
    """)
    suspend fun getCountryDistribution(): Map<@MapColumn(columnName = "country") String, @MapColumn(columnName = "count") Int>

    /**
     * Get distribution of minerals by crystal system.
     * Returns map of crystal system name to count.
     */
    @Query("""
        SELECT crystalSystem, COUNT(*) as count
        FROM minerals
        WHERE crystalSystem IS NOT NULL
        GROUP BY crystalSystem
        ORDER BY count DESC
    """)
    suspend fun getCrystalSystemDistribution(): Map<@MapColumn(columnName = "crystalSystem") String, @MapColumn(columnName = "count") Int>

    /**
     * Get distribution by Mohs hardness ranges.
     * Returns map of hardness range string to count.
     * Ranges: "1-2", "2-3", ..., "9-10"
     */
    @Query("""
        SELECT
            CASE
                WHEN mohsMin >= 1.0 AND mohsMin < 2.0 THEN '1-2'
                WHEN mohsMin >= 2.0 AND mohsMin < 3.0 THEN '2-3'
                WHEN mohsMin >= 3.0 AND mohsMin < 4.0 THEN '3-4'
                WHEN mohsMin >= 4.0 AND mohsMin < 5.0 THEN '4-5'
                WHEN mohsMin >= 5.0 AND mohsMin < 6.0 THEN '5-6'
                WHEN mohsMin >= 6.0 AND mohsMin < 7.0 THEN '6-7'
                WHEN mohsMin >= 7.0 AND mohsMin < 8.0 THEN '7-8'
                WHEN mohsMin >= 8.0 AND mohsMin < 9.0 THEN '8-9'
                WHEN mohsMin >= 9.0 THEN '9-10'
                ELSE 'Unknown'
            END as range,
            COUNT(*) as count
        FROM minerals
        WHERE mohsMin IS NOT NULL
        GROUP BY range
        ORDER BY range
    """)
    suspend fun getHardnessDistribution(): Map<@MapColumn(columnName = "range") String, @MapColumn(columnName = "count") Int>

    /**
     * Get distribution by status type.
     */
    @Query("""
        SELECT statusType, COUNT(*) as count
        FROM minerals
        GROUP BY statusType
        ORDER BY count DESC
    """)
    suspend fun getStatusDistribution(): Map<@MapColumn(columnName = "statusType") String, @MapColumn(columnName = "count") Int>

    /**
     * Get total estimated value of all minerals.
     * Sums the estimatedValue field from provenance.
     */
    @Query("""
        SELECT COALESCE(SUM(p.estimatedValue), 0.0)
        FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE p.estimatedValue IS NOT NULL
    """)
    suspend fun getTotalValue(): Double

    /**
     * Get average estimated value.
     */
    @Query("""
        SELECT COALESCE(AVG(p.estimatedValue), 0.0)
        FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE p.estimatedValue IS NOT NULL
    """)
    suspend fun getAverageValue(): Double

    /**
     * Get the most valuable mineral (by estimatedValue).
     * Returns null if no minerals have value set.
     */
    @Query("""
        SELECT m.id, m.name, p.estimatedValue as value, 'USD' as currency
        FROM minerals m
        INNER JOIN provenances p ON m.provenanceId = p.id
        WHERE p.estimatedValue IS NOT NULL
        ORDER BY p.estimatedValue DESC
        LIMIT 1
    """)
    suspend fun getMostValuableSpecimen(): MineralValueInfo?

    /**
     * Get average completeness percentage.
     */
    @Query("SELECT COALESCE(AVG(completeness), 0.0) FROM minerals")
    suspend fun getAverageCompleteness(): Double

    /**
     * Get count of fully documented minerals (completeness >= 80%).
     */
    @Query("SELECT COUNT(*) FROM minerals WHERE completeness >= 80")
    suspend fun getFullyDocumentedCount(): Int

    /**
     * Get count of minerals added this month.
     * Note: createdAt is stored as milliseconds since epoch (Java/Kotlin standard),
     * divided by 1000 to convert to seconds for SQLite's unixepoch format.
     */
    @Query("""
        SELECT COUNT(*)
        FROM minerals
        WHERE strftime('%Y-%m', CAST(createdAt / 1000 AS INTEGER), 'unixepoch') = strftime('%Y-%m', 'now')
    """)
    suspend fun getAddedThisMonth(): Int

    /**
     * Get count of minerals added this year.
     * Note: createdAt is stored as milliseconds since epoch, converted to seconds for SQLite.
     */
    @Query("""
        SELECT COUNT(*)
        FROM minerals
        WHERE strftime('%Y', CAST(createdAt / 1000 AS INTEGER), 'unixepoch') = strftime('%Y', 'now')
    """)
    suspend fun getAddedThisYear(): Int

    /**
     * Get distribution of minerals added by month.
     * Returns map of "YYYY-MM" format to count.
     * Note: createdAt is stored as milliseconds since epoch, converted to seconds for SQLite.
     */
    @Query("""
        SELECT strftime('%Y-%m', CAST(createdAt / 1000 AS INTEGER), 'unixepoch') as month, COUNT(*) as count
        FROM minerals
        WHERE createdAt IS NOT NULL
        GROUP BY month
        ORDER BY month DESC
    """)
    suspend fun getAddedByMonthDistribution(): Map<@MapColumn(columnName = "month") String, @MapColumn(columnName = "count") Int>

    /**
     * Get most common group (by count).
     */
    @Query("""
        SELECT `group`
        FROM minerals
        WHERE `group` IS NOT NULL
        GROUP BY `group`
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """)
    suspend fun getMostCommonGroup(): String?

    /**
     * Get most common country (by count).
     */
    @Query("""
        SELECT p.country
        FROM minerals m
        INNER JOIN provenances p ON m.provenanceId = p.id
        WHERE p.country IS NOT NULL
        GROUP BY p.country
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """)
    suspend fun getMostCommonCountry(): String?

    // ========== Advanced Filtering (v1.2.0) ==========

    /**
     * Advanced filter with multiple criteria.
     * All parameters are optional; null means "don't filter by this criterion".
     */
    @Query("""
        SELECT m.* FROM minerals m
        LEFT JOIN provenances p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.`group` IN (:groups))
          AND (:countries IS NULL OR p.country IN (:countries))
          AND (:mohsMin IS NULL OR m.mohsMax >= :mohsMin)
          AND (:mohsMax IS NULL OR m.mohsMin <= :mohsMax)
          AND (:statusTypes IS NULL OR m.statusType IN (:statusTypes))
          AND (:qualityMin IS NULL OR m.qualityRating >= :qualityMin)
          AND (:qualityMax IS NULL OR m.qualityRating <= :qualityMax)
          AND (:hasPhotos IS NULL OR
               (:hasPhotos = 1 AND EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)) OR
               (:hasPhotos = 0 AND NOT EXISTS (SELECT 1 FROM photos WHERE mineralId = m.id)))
          AND (:fluorescent IS NULL OR
               (:fluorescent = 1 AND m.fluorescence IS NOT NULL AND m.fluorescence != 'none') OR
               (:fluorescent = 0 AND (m.fluorescence IS NULL OR m.fluorescence = 'none')))
          AND (:mineralTypes IS NULL OR m.type IN (:mineralTypes))
        ORDER BY m.updatedAt DESC
    """)
    fun filterAdvanced(
        groups: List<String>? = null,
        countries: List<String>? = null,
        mohsMin: Float? = null,
        mohsMax: Float? = null,
        statusTypes: List<String>? = null,
        qualityMin: Int? = null,
        qualityMax: Int? = null,
        hasPhotos: Boolean? = null,
        fluorescent: Boolean? = null,
        mineralTypes: List<String>? = null
    ): Flow<List<MineralEntity>>

    // ========== v2.0 Statistics Queries for Aggregates ==========

    /**
     * Get type distribution (SIMPLE vs AGGREGATE).
     * Returns map of type to count.
     */
    @Query("""
        SELECT type, COUNT(*) as count
        FROM minerals
        GROUP BY type
        ORDER BY count DESC
    """)
    suspend fun getTypeDistribution(): Map<@MapColumn(columnName = "type") String, @MapColumn(columnName = "count") Int>

    /**
     * Get most frequent components across all aggregates.
     * Returns list of component names ordered by frequency.
     */
    @Query("""
        SELECT mineralName, COUNT(*) as count
        FROM mineral_components
        GROUP BY mineralName
        ORDER BY count DESC
        LIMIT 10
    """)
    suspend fun getMostFrequentComponents(): Map<@MapColumn(columnName = "mineralName") String, @MapColumn(columnName = "count") Int>

    /**
     * Get average number of components per aggregate.
     */
    @Query("""
        SELECT AVG(componentCount) as avgCount
        FROM (
            SELECT aggregateId, COUNT(*) as componentCount
            FROM mineral_components
            GROUP BY aggregateId
        )
    """)
    suspend fun getAverageComponentCount(): Double?
}

