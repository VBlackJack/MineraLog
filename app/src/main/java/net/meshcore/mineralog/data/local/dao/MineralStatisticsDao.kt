package net.meshcore.mineralog.data.local.dao

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query

/**
 * Statistics DAO for Mineral aggregations and analytics.
 * Handles statistical queries, distributions, and aggregate data.
 *
 * Part of the refactored DAO architecture to improve maintainability.
 * @see MineralBasicDao for CRUD operations
 * @see MineralQueryDao for filtering and search operations
 * @see MineralPagingDao for paginated queries
 */
@Dao
interface MineralStatisticsDao {

    // ========== DISTRIBUTION STATISTICS ==========

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
     * Get distribution of minerals by dominant color.
     * Returns map of color name to count.
     */
    @Query("""
        SELECT dominantColor, COUNT(*) as count
        FROM minerals
        WHERE dominantColor IS NOT NULL
        GROUP BY dominantColor
        ORDER BY count DESC
    """)
    suspend fun getDominantColorDistribution(): Map<@MapColumn(columnName = "dominantColor") String, @MapColumn(columnName = "count") Int>

    /**
     * Get type distribution (SIMPLE vs AGGREGATE vs ROCK).
     * Returns map of type to count.
     */
    @Query("""
        SELECT type, COUNT(*) as count
        FROM minerals
        GROUP BY type
        ORDER BY count DESC
    """)
    suspend fun getTypeDistribution(): Map<@MapColumn(columnName = "type") String, @MapColumn(columnName = "count") Int>

    // ========== VALUE STATISTICS ==========

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

    // ========== COMPLETENESS STATISTICS ==========

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

    // ========== TIME-BASED STATISTICS ==========

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

    // ========== MOST COMMON STATISTICS ==========

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

    // ========== AGGREGATE COMPONENT STATISTICS ==========

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

/**
 * Data class for most valuable mineral query result.
 */
data class MineralValueInfo(
    val id: String,
    val name: String,
    val value: Double,
    val currency: String?
)
