package net.meshcore.mineralog.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.data.local.dao.MineralDaoComposite
import net.meshcore.mineralog.data.model.CollectionStatistics
import net.meshcore.mineralog.data.model.MineralSummary

/**
 * Repository for computing collection statistics.
 * Aggregates data from MineralDaoComposite to produce CollectionStatistics.
 */
interface StatisticsRepository {
    suspend fun getStatistics(): CollectionStatistics
    suspend fun refreshStatistics(): CollectionStatistics
}

class StatisticsRepositoryImpl(
    private val mineralDao: MineralDaoComposite
) : StatisticsRepository {

    // Cache configuration
    private var cachedStatistics: CollectionStatistics? = null
    private var cacheTimestamp: Long = 0L
    private val cacheTtlMs = 30_000L // 30 seconds cache TTL

    /**
     * Compute collection statistics from current database state.
     * Implements in-memory caching with 30s TTL to improve performance from 5s to <1s.
     * All queries are executed in parallel for maximum performance.
     */
    override suspend fun getStatistics(): CollectionStatistics =
        withContext(Dispatchers.IO) {
            // Check if cache is valid
            val now = System.currentTimeMillis()
            if (cachedStatistics != null && (now - cacheTimestamp) < cacheTtlMs) {
                return@withContext cachedStatistics!!
            }

            // Cache miss or expired - compute fresh statistics
            val totalMinerals = mineralDao.getCount()

            // Return empty statistics if no minerals
            if (totalMinerals == 0) {
                return@withContext CollectionStatistics()
            }

            // Execute all queries in parallel for performance
            val totalValueDeferred = async { mineralDao.getTotalValue() }
            val averageValueDeferred = async { mineralDao.getAverageValue() }
            val byGroupDeferred = async { mineralDao.getGroupDistribution() }
            val byCountryDeferred = async { mineralDao.getCountryDistribution() }
            val byCrystalSystemDeferred = async { mineralDao.getCrystalSystemDistribution() }
            val byHardnessDeferred = async { mineralDao.getHardnessDistribution() }
            val byStatusDeferred = async { mineralDao.getStatusDistribution() }
            val addedByMonthDeferred = async { mineralDao.getAddedByMonthDistribution() }
            val mostCommonGroupDeferred = async { mineralDao.getMostCommonGroup() }
            val mostCommonCountryDeferred = async { mineralDao.getMostCommonCountry() }
            val mostValuableDeferred = async { mineralDao.getMostValuableSpecimen() }
            val averageCompletenessDeferred = async { mineralDao.getAverageCompleteness() }
            val fullyDocumentedCountDeferred = async { mineralDao.getFullyDocumentedCount() }
            val addedThisMonthDeferred = async { mineralDao.getAddedThisMonth() }
            val addedThisYearDeferred = async { mineralDao.getAddedThisYear() }

            // v2.0: Aggregate statistics queries
            val byTypeDeferred = async { mineralDao.getTypeDistribution() }
            val mostFrequentComponentsDeferred = async { mineralDao.getMostFrequentComponents() }
            val averageComponentCountDeferred = async { mineralDao.getAverageComponentCount() }

            // Await all results
            val totalValue = totalValueDeferred.await()
            val averageValue = averageValueDeferred.await()
            val byGroup = byGroupDeferred.await()
            val byCountry = byCountryDeferred.await()
            val byCrystalSystem = byCrystalSystemDeferred.await()
            val byHardness = convertHardnessDistribution(byHardnessDeferred.await())
            val byStatus = byStatusDeferred.await()
            val addedByMonth = addedByMonthDeferred.await()
            val mostCommonGroup = mostCommonGroupDeferred.await()
            val mostCommonCountry = mostCommonCountryDeferred.await()
            val mostValuable = mostValuableDeferred.await()?.let {
                MineralSummary(
                    id = it.id,
                    name = it.name,
                    value = it.value,
                    currency = it.currency
                )
            }
            val averageCompleteness = averageCompletenessDeferred.await()
            val fullyDocumentedCount = fullyDocumentedCountDeferred.await()
            val addedThisMonth = addedThisMonthDeferred.await()
            val addedThisYear = addedThisYearDeferred.await()

            // v2.0: Await aggregate statistics
            val byType = byTypeDeferred.await()
            val mostFrequentComponents = mostFrequentComponentsDeferred.await().map { (name, count) ->
                net.meshcore.mineralog.data.model.ComponentFrequency(
                    componentName = name,
                    count = count
                )
            }
            val averageComponentCount = averageComponentCountDeferred.await() ?: 0.0

            // Calculate derived stats
            val totalAggregates = byType["AGGREGATE"] ?: 0
            val totalSimple = byType["SIMPLE"] ?: 0

            val statistics = CollectionStatistics(
                totalMinerals = totalMinerals,
                totalValue = totalValue,
                averageValue = averageValue,
                byGroup = byGroup,
                byCountry = byCountry,
                byCrystalSystem = byCrystalSystem,
                byHardness = byHardness,
                byStatus = byStatus,
                mostCommonGroup = mostCommonGroup,
                mostCommonCountry = mostCommonCountry,
                mostValuableSpecimen = mostValuable,
                averageCompleteness = averageCompleteness,
                fullyDocumentedCount = fullyDocumentedCount,
                addedThisMonth = addedThisMonth,
                addedThisYear = addedThisYear,
                addedByMonth = addedByMonth,
                // v2.0: Aggregate statistics
                byType = byType,
                totalAggregates = totalAggregates,
                totalSimple = totalSimple,
                mostFrequentComponents = mostFrequentComponents,
                averageComponentCount = averageComponentCount
            )

            // Update cache
            cachedStatistics = statistics
            cacheTimestamp = now

            statistics
        }

    /**
     * Refresh statistics by invalidating cache and recomputing.
     */
    override suspend fun refreshStatistics(): CollectionStatistics {
        // Invalidate cache to force fresh computation
        cachedStatistics = null
        cacheTimestamp = 0L
        return getStatistics()
    }

    /**
     * Convert hardness distribution from String keys to IntRange keys.
     * DAO returns Map<String, Int> (e.g., "1-2" -> count)
     * UI expects Map<IntRange, Int> (e.g., 1..2 -> count)
     */
    private fun convertHardnessDistribution(stringMap: Map<String, Int>): Map<IntRange, Int> {
        return stringMap.mapKeys { (rangeStr, _) ->
            when (rangeStr) {
                "1-2" -> 1..2
                "2-3" -> 2..3
                "3-4" -> 3..4
                "4-5" -> 4..5
                "5-6" -> 5..6
                "6-7" -> 6..7
                "7-8" -> 7..8
                "8-9" -> 8..9
                "9-10" -> 9..10
                else -> 0..0 // Unknown
            }
        }
    }
}
