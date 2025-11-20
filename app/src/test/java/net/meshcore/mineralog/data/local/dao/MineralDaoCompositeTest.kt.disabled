package net.meshcore.mineralog.data.local.dao

import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.entity.MineralEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Tests for MineralDaoComposite delegation pattern.
 *
 * Tests verify that the composite correctly delegates to specialized DAOs:
 * - MineralBasicDao (CRUD operations)
 * - MineralQueryDao (search, filter, type queries)
 * - MineralStatisticsDao (aggregations, distributions)
 * - MineralPagingDao (paginated queries)
 *
 * Sprint 3: DAO Tests - Target 70%+ coverage
 */
class MineralDaoCompositeTest {

    private lateinit var basicDao: MineralBasicDao
    private lateinit var queryDao: MineralQueryDao
    private lateinit var statisticsDao: MineralStatisticsDao
    private lateinit var pagingDao: MineralPagingDao
    private lateinit var compositeDao: MineralDaoComposite

    @BeforeEach
    fun setup() {
        basicDao = mockk()
        queryDao = mockk()
        statisticsDao = mockk()
        pagingDao = mockk()

        compositeDao = MineralDaoComposite(
            basicDao = basicDao,
            queryDao = queryDao,
            statisticsDao = statisticsDao,
            pagingDao = pagingDao
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ========== BASIC CRUD DELEGATION TESTS ==========

    @Test
    @DisplayName("insert delegates to MineralBasicDao")
    fun `insert - delegates to basicDao`() = runTest {
        // Arrange
        val mineral = createTestMineral("test-id")
        coEvery { basicDao.insert(mineral) } returns 1L

        // Act
        val result = compositeDao.insert(mineral)

        // Assert
        assertEquals(1L, result)
        coVerify(exactly = 1) { basicDao.insert(mineral) }
    }

    @Test
    @DisplayName("getById delegates to MineralBasicDao")
    fun `getById - delegates to basicDao`() = runTest {
        // Arrange
        val mineralId = "test-id"
        val expectedMineral = createTestMineral(mineralId)
        coEvery { basicDao.getById(mineralId) } returns expectedMineral

        // Act
        val result = compositeDao.getById(mineralId)

        // Assert
        assertEquals(expectedMineral, result)
        coVerify(exactly = 1) { basicDao.getById(mineralId) }
    }

    @Test
    @DisplayName("deleteById delegates to MineralBasicDao")
    fun `deleteById - delegates to basicDao`() = runTest {
        // Arrange
        val mineralId = "test-id"
        coEvery { basicDao.deleteById(mineralId) } just Runs

        // Act
        compositeDao.deleteById(mineralId)

        // Assert
        coVerify(exactly = 1) { basicDao.deleteById(mineralId) }
    }

    @Test
    @DisplayName("getAllFlow delegates to MineralBasicDao")
    fun `getAllFlow - delegates to basicDao`() {
        // Arrange
        val minerals = listOf(
            createTestMineral("1"),
            createTestMineral("2")
        )
        every { basicDao.getAllFlow() } returns flowOf(minerals)

        // Act
        val result = compositeDao.getAllFlow()

        // Assert
        assertEquals(flowOf(minerals), result)
        verify(exactly = 1) { basicDao.getAllFlow() }
    }

    @Test
    @DisplayName("getCount delegates to MineralBasicDao")
    fun `getCount - delegates to basicDao`() = runTest {
        // Arrange
        coEvery { basicDao.getCount() } returns 42

        // Act
        val result = compositeDao.getCount()

        // Assert
        assertEquals(42, result)
        coVerify(exactly = 1) { basicDao.getCount() }
    }

    // ========== QUERY DELEGATION TESTS ==========

    @Test
    @DisplayName("searchFlow delegates to MineralQueryDao")
    fun `searchFlow - delegates to queryDao`() {
        // Arrange
        val query = "quartz"
        val minerals = listOf(createTestMineral("quartz-1"))
        every { queryDao.searchFlow(query) } returns flowOf(minerals)

        // Act
        val result = compositeDao.searchFlow(query)

        // Assert
        assertEquals(flowOf(minerals), result)
        verify(exactly = 1) { queryDao.searchFlow(query) }
    }

    @Test
    @DisplayName("getAllSimpleMinerals delegates to MineralQueryDao")
    fun `getAllSimpleMinerals - delegates to queryDao`() {
        // Arrange
        val minerals = listOf(createTestMineral("simple-1"))
        every { queryDao.getAllSimpleMinerals() } returns flowOf(minerals)

        // Act
        val result = compositeDao.getAllSimpleMinerals()

        // Assert
        assertEquals(flowOf(minerals), result)
        verify(exactly = 1) { queryDao.getAllSimpleMinerals() }
    }

    @Test
    @DisplayName("filterAdvanced delegates to MineralQueryDao")
    fun `filterAdvanced - delegates to queryDao with all parameters`() {
        // Arrange
        val groups = listOf("Silicates")
        val countries = listOf("France")
        val mohsMin = 5.0f
        val mohsMax = 7.0f
        val statusTypes = listOf("COLLECTION")
        val qualityMin = 3
        val qualityMax = 5
        val hasPhotos = true
        val fluorescent = false
        val mineralTypes = listOf("SIMPLE")

        val minerals = listOf(createTestMineral("filtered-1"))
        every {
            queryDao.filterAdvanced(
                groups, countries, mohsMin, mohsMax, statusTypes,
                qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
            )
        } returns flowOf(minerals)

        // Act
        val result = compositeDao.filterAdvanced(
            groups, countries, mohsMin, mohsMax, statusTypes,
            qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
        )

        // Assert
        assertEquals(flowOf(minerals), result)
        verify(exactly = 1) {
            queryDao.filterAdvanced(
                groups, countries, mohsMin, mohsMax, statusTypes,
                qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes
            )
        }
    }

    @Test
    @DisplayName("getDistinctGroupsFlow delegates to MineralQueryDao")
    fun `getDistinctGroupsFlow - delegates to queryDao`() {
        // Arrange
        val groups = listOf("Silicates", "Oxides", "Carbonates")
        every { queryDao.getDistinctGroupsFlow() } returns flowOf(groups)

        // Act
        val result = compositeDao.getDistinctGroupsFlow()

        // Assert
        assertEquals(flowOf(groups), result)
        verify(exactly = 1) { queryDao.getDistinctGroupsFlow() }
    }

    // ========== STATISTICS DELEGATION TESTS ==========

    @Test
    @DisplayName("getGroupDistribution delegates to MineralStatisticsDao")
    fun `getGroupDistribution - delegates to statisticsDao`() = runTest {
        // Arrange
        val distribution = mapOf(
            "Silicates" to 42,
            "Oxides" to 23,
            "Carbonates" to 15
        )
        coEvery { statisticsDao.getGroupDistribution() } returns distribution

        // Act
        val result = compositeDao.getGroupDistribution()

        // Assert
        assertEquals(distribution, result)
        coVerify(exactly = 1) { statisticsDao.getGroupDistribution() }
    }

    @Test
    @DisplayName("getTotalValue delegates to MineralStatisticsDao")
    fun `getTotalValue - delegates to statisticsDao`() = runTest {
        // Arrange
        coEvery { statisticsDao.getTotalValue() } returns 12345.67

        // Act
        val result = compositeDao.getTotalValue()

        // Assert
        assertEquals(12345.67, result, 0.001)
        coVerify(exactly = 1) { statisticsDao.getTotalValue() }
    }

    @Test
    @DisplayName("getAddedThisMonth delegates to MineralStatisticsDao")
    fun `getAddedThisMonth - delegates to statisticsDao`() = runTest {
        // Arrange
        coEvery { statisticsDao.getAddedThisMonth() } returns 15

        // Act
        val result = compositeDao.getAddedThisMonth()

        // Assert
        assertEquals(15, result)
        coVerify(exactly = 1) { statisticsDao.getAddedThisMonth() }
    }

    @Test
    @DisplayName("getMostCommonGroup delegates to MineralStatisticsDao")
    fun `getMostCommonGroup - delegates to statisticsDao`() = runTest {
        // Arrange
        coEvery { statisticsDao.getMostCommonGroup() } returns "Silicates"

        // Act
        val result = compositeDao.getMostCommonGroup()

        // Assert
        assertEquals("Silicates", result)
        coVerify(exactly = 1) { statisticsDao.getMostCommonGroup() }
    }

    // ========== PAGING DELEGATION TESTS ==========

    @Test
    @DisplayName("getAllPaged delegates to MineralPagingDao")
    fun `getAllPaged - delegates to pagingDao`() {
        // Arrange
        val pagingSource = mockk<androidx.paging.PagingSource<Int, MineralEntity>>()
        every { pagingDao.getAllPaged() } returns pagingSource

        // Act
        val result = compositeDao.getAllPaged()

        // Assert
        assertEquals(pagingSource, result)
        verify(exactly = 1) { pagingDao.getAllPaged() }
    }

    @Test
    @DisplayName("getAllPagedSortedByNameAsc delegates to MineralPagingDao")
    fun `getAllPagedSortedByNameAsc - delegates to pagingDao`() {
        // Arrange
        val pagingSource = mockk<androidx.paging.PagingSource<Int, MineralEntity>>()
        every { pagingDao.getAllPagedSortedByNameAsc() } returns pagingSource

        // Act
        val result = compositeDao.getAllPagedSortedByNameAsc()

        // Assert
        assertEquals(pagingSource, result)
        verify(exactly = 1) { pagingDao.getAllPagedSortedByNameAsc() }
    }

    @Test
    @DisplayName("searchPaged delegates to MineralPagingDao")
    fun `searchPaged - delegates to pagingDao`() {
        // Arrange
        val query = "quartz"
        val pagingSource = mockk<androidx.paging.PagingSource<Int, MineralEntity>>()
        every { pagingDao.searchPaged(query) } returns pagingSource

        // Act
        val result = compositeDao.searchPaged(query)

        // Assert
        assertEquals(pagingSource, result)
        verify(exactly = 1) { pagingDao.searchPaged(query) }
    }

    // ========== HELPER METHODS ==========

    private fun createTestMineral(id: String) = MineralEntity(
        id = id,
        name = "Test Mineral $id",
        group = "Silicates",
        formula = "SiO2",
        crystalSystem = "Hexagonal",
        hardness = "7",
        mohsMin = 7.0f,
        mohsMax = 7.0f,
        specificGravity = "2.65",
        color = "Clear",
        luster = "Vitreous",
        transparency = "Transparent",
        cleavage = "None",
        fracture = "Conchoidal",
        streak = "White",
        fluorescence = "None",
        magnetism = "Non-magnetic",
        radioactivity = "None",
        notes = "Test notes",
        provenanceId = null,
        storageId = null,
        status = "COLLECTION",
        quality = 4,
        estimatedValue = 100.0,
        acquisitionDate = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        mineralType = "SIMPLE",
        qrCode = null
    )
}
