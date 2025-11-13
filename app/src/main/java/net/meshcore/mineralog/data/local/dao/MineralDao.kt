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

    // ========== Paging 3 Support (v1.5.0) ==========

    /**
     * Get all minerals as paged data source.
     * Returns PagingSource for efficient loading of large datasets.
     */
    @Query("SELECT * FROM minerals ORDER BY updatedAt DESC")
    fun getAllPaged(): PagingSource<Int, MineralEntity>

    /**
     * Search minerals with pagination support.
     * Note: Query parameter should be pre-formatted with wildcards (e.g., "%search%")
     * to prevent SQL injection via LIKE operator concatenation.
     */
    @Query("""
        SELECT * FROM minerals
        WHERE name LIKE :query
           OR group LIKE :query
           OR formula LIKE :query
           OR notes LIKE :query
           OR tags LIKE :query
        ORDER BY updatedAt DESC
    """)
    fun searchPaged(query: String): PagingSource<Int, MineralEntity>

    /**
     * Advanced filter with pagination support.
     */
    @Query("""
        SELECT m.* FROM minerals m
        LEFT JOIN provenance p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.group IN (:groups))
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
        ORDER BY m.updatedAt DESC
    """)
    fun filterAdvancedPaged(
        groups: List<String>? = null,
        countries: List<String>? = null,
        mohsMin: Float? = null,
        mohsMax: Float? = null,
        statusTypes: List<String>? = null,
        qualityMin: Int? = null,
        qualityMax: Int? = null,
        hasPhotos: Boolean? = null,
        fluorescent: Boolean? = null
    ): PagingSource<Int, MineralEntity>

    /**
     * Search minerals with Flow.
     * Note: Query parameter should be pre-formatted with wildcards (e.g., "%search%")
     */
    @Query("""
        SELECT * FROM minerals
        WHERE name LIKE :query
           OR group LIKE :query
           OR formula LIKE :query
           OR notes LIKE :query
           OR tags LIKE :query
        ORDER BY updatedAt DESC
    """)
    fun searchFlow(query: String): Flow<List<MineralEntity>>

    @Query("""
        SELECT * FROM minerals
        WHERE (:group IS NULL OR group = :group)
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

    @Query("SELECT DISTINCT group FROM minerals WHERE group IS NOT NULL ORDER BY group")
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
        SELECT group, COUNT(*) as count
        FROM minerals
        WHERE group IS NOT NULL
        GROUP BY group
        ORDER BY count DESC
    """)
    suspend fun getGroupDistribution(): Map<String, Int>

    /**
     * Get distribution of minerals by country (from provenance).
     * Requires join with provenance table.
     */
    @Query("""
        SELECT p.country, COUNT(*) as count
        FROM minerals m
        INNER JOIN provenance p ON m.provenanceId = p.id
        WHERE p.country IS NOT NULL
        GROUP BY p.country
        ORDER BY count DESC
    """)
    suspend fun getCountryDistribution(): Map<String, Int>

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
    suspend fun getHardnessDistribution(): Map<String, Int>

    /**
     * Get distribution by status type.
     */
    @Query("""
        SELECT statusType, COUNT(*) as count
        FROM minerals
        GROUP BY statusType
        ORDER BY count DESC
    """)
    suspend fun getStatusDistribution(): Map<String, Int>

    /**
     * Get total estimated value of all minerals.
     * Sums the estimatedValue field from provenance.
     */
    @Query("""
        SELECT COALESCE(SUM(p.estimatedValue), 0.0)
        FROM minerals m
        LEFT JOIN provenance p ON m.provenanceId = p.id
        WHERE p.estimatedValue IS NOT NULL
    """)
    suspend fun getTotalValue(): Double

    /**
     * Get average estimated value.
     */
    @Query("""
        SELECT COALESCE(AVG(p.estimatedValue), 0.0)
        FROM minerals m
        LEFT JOIN provenance p ON m.provenanceId = p.id
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
        INNER JOIN provenance p ON m.provenanceId = p.id
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
     * Uses current timestamp to determine the month.
     */
    @Query("""
        SELECT COUNT(*)
        FROM minerals
        WHERE strftime('%Y-%m', createdAt / 1000, 'unixepoch') = strftime('%Y-%m', 'now')
    """)
    suspend fun getAddedThisMonth(): Int

    /**
     * Get count of minerals added this year.
     */
    @Query("""
        SELECT COUNT(*)
        FROM minerals
        WHERE strftime('%Y', createdAt / 1000, 'unixepoch') = strftime('%Y', 'now')
    """)
    suspend fun getAddedThisYear(): Int

    /**
     * Get most common group (by count).
     */
    @Query("""
        SELECT group
        FROM minerals
        WHERE group IS NOT NULL
        GROUP BY group
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
        INNER JOIN provenance p ON m.provenanceId = p.id
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
        LEFT JOIN provenance p ON m.provenanceId = p.id
        WHERE (:groups IS NULL OR m.group IN (:groups))
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
        fluorescent: Boolean? = null
    ): Flow<List<MineralEntity>>
}

/**
 * Data class for most valuable mineral query result.
 */
data class MineralValueInfo(
    val id: String,
    val name: String,
    val value: Double,
    val currency: String?
)
