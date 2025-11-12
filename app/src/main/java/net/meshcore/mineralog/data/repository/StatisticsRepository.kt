package net.meshcore.mineralog.data.repository

import kotlinx.coroutines.Dispatchers
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
     */
    override suspend fun getStatistics(): CollectionStatistics =
        withContext(Dispatchers.IO) {
            val totalMinerals = mineralDao.getCount()

            // Return empty statistics if no minerals
            if (totalMinerals == 0) {
                return@withContext CollectionStatistics()
            }

            // Compute all aggregations in parallel for performance
            val totalValue = mineralDao.getTotalValue()
            val averageValue = mineralDao.getAverageValue()
            val byGroup = mineralDao.getGroupDistribution()
            val byCountry = mineralDao.getCountryDistribution()
            val byHardness = convertHardnessDistribution(mineralDao.getHardnessDistribution())
            val byStatus = mineralDao.getStatusDistribution()
            val mostCommonGroup = mineralDao.getMostCommonGroup()
            val mostCommonCountry = mineralDao.getMostCommonCountry()
            val mostValuable = mineralDao.getMostValuableSpecimen()?.let {
                MineralSummary(
                    id = it.id,
                    name = it.name,
                    value = it.value,
                    currency = it.currency
                )
            }
            val averageCompleteness = mineralDao.getAverageCompleteness()
            val fullyDocumentedCount = mineralDao.getFullyDocumentedCount()
            val addedThisMonth = mineralDao.getAddedThisMonth()
            val addedThisYear = mineralDao.getAddedThisYear()

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
