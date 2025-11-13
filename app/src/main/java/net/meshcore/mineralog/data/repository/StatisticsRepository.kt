package net.meshcore.mineralog.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.data.local.dao.MineralDao
import net.meshcore.mineralog.data.model.CollectionStatistics
import net.meshcore.mineralog.data.model.MineralSummary

/**
 * Repository for computing collection statistics.
 * Aggregates data from MineralDao to produce CollectionStatistics.
 */
interface StatisticsRepository {
    suspend fun getStatistics(): CollectionStatistics
    suspend fun refreshStatistics(): CollectionStatistics
}

class StatisticsRepositoryImpl(
    private val mineralDao: MineralDao
) : StatisticsRepository {

    /**
     * Compute collection statistics from current database state.
     * This is a potentially expensive operation; cache results in ViewModel.
     * All queries are executed in parallel for maximum performance.
     */
    override suspend fun getStatistics(): CollectionStatistics =
        withContext(Dispatchers.IO) {
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
            val byHardnessDeferred = async { mineralDao.getHardnessDistribution() }
            val byStatusDeferred = async { mineralDao.getStatusDistribution() }
            val mostCommonGroupDeferred = async { mineralDao.getMostCommonGroup() }
            val mostCommonCountryDeferred = async { mineralDao.getMostCommonCountry() }
            val mostValuableDeferred = async { mineralDao.getMostValuableSpecimen() }
            val averageCompletenessDeferred = async { mineralDao.getAverageCompleteness() }
            val fullyDocumentedCountDeferred = async { mineralDao.getFullyDocumentedCount() }
            val addedThisMonthDeferred = async { mineralDao.getAddedThisMonth() }
            val addedThisYearDeferred = async { mineralDao.getAddedThisYear() }

            // Await all results
            val totalValue = totalValueDeferred.await()
            val averageValue = averageValueDeferred.await()
            val byGroup = byGroupDeferred.await()
            val byCountry = byCountryDeferred.await()
            val byHardness = convertHardnessDistribution(byHardnessDeferred.await())
            val byStatus = byStatusDeferred.await()
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

            CollectionStatistics(
                totalMinerals = totalMinerals,
                totalValue = totalValue,
                averageValue = averageValue,
                byGroup = byGroup,
                byCountry = byCountry,
                byHardness = byHardness,
                byStatus = byStatus,
                mostCommonGroup = mostCommonGroup,
                mostCommonCountry = mostCommonCountry,
                mostValuableSpecimen = mostValuable,
                averageCompleteness = averageCompleteness,
                fullyDocumentedCount = fullyDocumentedCount,
                addedThisMonth = addedThisMonth,
                addedThisYear = addedThisYear
            )
        }

    /**
     * Refresh statistics (same as getStatistics, but explicit intent).
     */
    override suspend fun refreshStatistics(): CollectionStatistics = getStatistics()

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
