package net.meshcore.mineralog.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.dao.MineralDao
import net.meshcore.mineralog.data.local.dao.MineralValueInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for StatisticsRepository.
 * Tests statistics computation from DAO aggregation queries.
 */
class StatisticsRepositoryTest {

    private val mineralDao = mockk<MineralDao>()
    private val repository = StatisticsRepositoryImpl(mineralDao)

    @Test
    fun `getStatistics returns empty stats when no minerals`() = runTest {
        // Given
        coEvery { mineralDao.getCount() } returns 0

        // When
        val stats = repository.getStatistics()

        // Then
        assertEquals(0, stats.totalMinerals)
        assertEquals(0.0, stats.totalValue, 0.001)
        assertEquals(0.0, stats.averageValue, 0.001)
        assertTrue(stats.byGroup.isEmpty())
        assertTrue(stats.byCountry.isEmpty())
    }

    @Test
    fun `getStatistics computes correct total value and average`() = runTest {
        // Given
        coEvery { mineralDao.getCount() } returns 3
        coEvery { mineralDao.getTotalValue() } returns 150.0
        coEvery { mineralDao.getAverageValue() } returns 50.0
        coEvery { mineralDao.getGroupDistribution() } returns emptyMap()
        coEvery { mineralDao.getCountryDistribution() } returns emptyMap()
        coEvery { mineralDao.getHardnessDistribution() } returns emptyMap()
        coEvery { mineralDao.getStatusDistribution() } returns emptyMap()
        coEvery { mineralDao.getMostCommonGroup() } returns null
        coEvery { mineralDao.getMostCommonCountry() } returns null
        coEvery { mineralDao.getMostValuableSpecimen() } returns null
        coEvery { mineralDao.getAverageCompleteness() } returns 75.0
        coEvery { mineralDao.getFullyDocumentedCount() } returns 2
        coEvery { mineralDao.getAddedThisMonth() } returns 1
        coEvery { mineralDao.getAddedThisYear() } returns 3

        // When
        val stats = repository.getStatistics()

        // Then
        assertEquals(3, stats.totalMinerals)
        assertEquals(150.0, stats.totalValue, 0.001)
        assertEquals(50.0, stats.averageValue, 0.001)
        assertEquals(75.0, stats.averageCompleteness, 0.001)
        assertEquals(2, stats.fullyDocumentedCount)
        assertEquals(1, stats.addedThisMonth)
        assertEquals(3, stats.addedThisYear)
    }

    @Test
    fun `getStatistics includes group distribution`() = runTest {
        // Given
        val groupDist = mapOf("Silicates" to 10, "Oxides" to 5, "Carbonates" to 3)
        coEvery { mineralDao.getCount() } returns 18
        coEvery { mineralDao.getTotalValue() } returns 0.0
        coEvery { mineralDao.getAverageValue() } returns 0.0
        coEvery { mineralDao.getGroupDistribution() } returns groupDist
        coEvery { mineralDao.getCountryDistribution() } returns emptyMap()
        coEvery { mineralDao.getHardnessDistribution() } returns emptyMap()
        coEvery { mineralDao.getStatusDistribution() } returns emptyMap()
        coEvery { mineralDao.getMostCommonGroup() } returns "Silicates"
        coEvery { mineralDao.getMostCommonCountry() } returns null
        coEvery { mineralDao.getMostValuableSpecimen() } returns null
        coEvery { mineralDao.getAverageCompleteness() } returns 0.0
        coEvery { mineralDao.getFullyDocumentedCount() } returns 0
        coEvery { mineralDao.getAddedThisMonth() } returns 0
        coEvery { mineralDao.getAddedThisYear() } returns 0

        // When
        val stats = repository.getStatistics()

        // Then
        assertEquals(groupDist, stats.byGroup)
        assertEquals("Silicates", stats.mostCommonGroup)
    }

    @Test
    fun `getStatistics includes most valuable specimen`() = runTest {
        // Given
        val mostValuable = MineralValueInfo(
            id = "123",
            name = "Ruby",
            value = 1500.0,
            currency = "USD"
        )
        coEvery { mineralDao.getCount() } returns 10
        coEvery { mineralDao.getTotalValue() } returns 3000.0
        coEvery { mineralDao.getAverageValue() } returns 300.0
        coEvery { mineralDao.getGroupDistribution() } returns emptyMap()
        coEvery { mineralDao.getCountryDistribution() } returns emptyMap()
        coEvery { mineralDao.getHardnessDistribution() } returns emptyMap()
        coEvery { mineralDao.getStatusDistribution() } returns emptyMap()
        coEvery { mineralDao.getMostCommonGroup() } returns null
        coEvery { mineralDao.getMostCommonCountry() } returns null
        coEvery { mineralDao.getMostValuableSpecimen() } returns mostValuable
        coEvery { mineralDao.getAverageCompleteness() } returns 0.0
        coEvery { mineralDao.getFullyDocumentedCount() } returns 0
        coEvery { mineralDao.getAddedThisMonth() } returns 0
        coEvery { mineralDao.getAddedThisYear() } returns 0

        // When
        val stats = repository.getStatistics()

        // Then
        assertNotNull(stats.mostValuableSpecimen)
        assertEquals("Ruby", stats.mostValuableSpecimen?.name)
        assertEquals(1500.0, stats.mostValuableSpecimen?.value)
    }

    @Test
    fun `refreshStatistics returns same result as getStatistics`() = runTest {
        // Given
        coEvery { mineralDao.getCount() } returns 5
        coEvery { mineralDao.getTotalValue() } returns 250.0
        coEvery { mineralDao.getAverageValue() } returns 50.0
        coEvery { mineralDao.getGroupDistribution() } returns emptyMap()
        coEvery { mineralDao.getCountryDistribution() } returns emptyMap()
        coEvery { mineralDao.getHardnessDistribution() } returns emptyMap()
        coEvery { mineralDao.getStatusDistribution() } returns emptyMap()
        coEvery { mineralDao.getMostCommonGroup() } returns null
        coEvery { mineralDao.getMostCommonCountry() } returns null
        coEvery { mineralDao.getMostValuableSpecimen() } returns null
        coEvery { mineralDao.getAverageCompleteness() } returns 60.0
        coEvery { mineralDao.getFullyDocumentedCount() } returns 3
        coEvery { mineralDao.getAddedThisMonth() } returns 2
        coEvery { mineralDao.getAddedThisYear() } returns 5

        // When
        val stats1 = repository.getStatistics()
        val stats2 = repository.refreshStatistics()

        // Then
        assertEquals(stats1, stats2)
    }
}
