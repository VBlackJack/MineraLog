package net.meshcore.mineralog.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import net.meshcore.mineralog.data.local.entity.MineralEntity

/**
 * Paging DAO for Mineral paginated queries.
 * Handles all paginated queries using AndroidX Paging 3 library.
 *
 * Part of the refactored DAO architecture to improve maintainability.
 * @see MineralBasicDao for CRUD operations
 * @see MineralQueryDao for filtering and search operations
 * @see MineralStatisticsDao for aggregations and statistics
 */
@Dao
interface MineralPagingDao {

    // ========== BASIC PAGED QUERIES ==========

    /**
     * Get all minerals as paged data source.
     * Returns PagingSource for efficient loading of large datasets.
     */
    @Query("SELECT * FROM minerals ORDER BY updatedAt DESC")
    fun getAllPaged(): PagingSource<Int, MineralEntity>

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

    // ========== TYPE-BASED PAGED QUERIES ==========

    /**
     * Get minerals by type (paged).
     */
    @Query("SELECT * FROM minerals WHERE type IN (:types) ORDER BY updatedAt DESC")
    fun getMineralsByTypePaged(types: List<String>): PagingSource<Int, MineralEntity>

    // ========== SEARCH PAGED QUERIES ==========

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

    // ========== ADVANCED FILTER PAGED QUERIES ==========

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
}
