package net.meshcore.mineralog.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.meshcore.mineralog.data.local.entity.MineralEntity

/**
 * Query DAO for Mineral filtering and search operations.
 * Handles complex queries with filters, search, and type-based retrieval.
 *
 * Part of the refactored DAO architecture to improve maintainability.
 * @see MineralBasicDao for CRUD operations
 * @see MineralStatisticsDao for aggregations and statistics
 * @see MineralPagingDao for paginated queries
 */
@Dao
interface MineralQueryDao {

    // ========== TYPE-BASED QUERIES ==========

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
     * Count minerals by type.
     */
    @Query("SELECT COUNT(*) FROM minerals WHERE type = :type")
    suspend fun countByType(type: String): Int

    // ========== SEARCH OPERATIONS ==========

    /**
     * Search minerals with Flow.
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
    fun searchFlow(query: String): Flow<List<MineralEntity>>

    // ========== SIMPLE FILTER OPERATIONS ==========

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

    // ========== ADVANCED FILTER OPERATIONS ==========

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

    // ========== DISTINCT VALUES FOR FILTERS ==========

    @Query("SELECT DISTINCT `group` FROM minerals WHERE `group` IS NOT NULL ORDER BY `group`")
    fun getDistinctGroupsFlow(): Flow<List<String>>

    @Query("SELECT DISTINCT crystalSystem FROM minerals WHERE crystalSystem IS NOT NULL ORDER BY crystalSystem")
    fun getDistinctCrystalSystemsFlow(): Flow<List<String>>

    /**
     * Get all unique tags for autocomplete.
     */
    @Query("SELECT tags FROM minerals WHERE tags IS NOT NULL AND tags != ''")
    suspend fun getAllTags(): List<String>
}
