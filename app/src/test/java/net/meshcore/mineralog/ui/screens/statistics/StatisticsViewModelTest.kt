package net.meshcore.mineralog.ui.screens.statistics

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import net.meshcore.mineralog.data.model.CollectionStatistics
import net.meshcore.mineralog.data.repository.StatisticsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for StatisticsViewModel.
 *
 * Tests cover:
 * - Initial loading state
 * - Successful statistics loading
 * - Error handling
 * - Statistics refresh
 * - State transitions
 *
 * Sprint 3: ViewModel Tests - Target 70%+ coverage
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private lateinit var statisticsRepository: StatisticsRepository
    private lateinit var viewModel: StatisticsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        statisticsRepository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // ========== INITIALIZATION TESTS ==========

    @Test
    @DisplayName("ViewModel initializes in Loading state")
    fun `init - sets initial state to Loading`() = runTest {
        // Arrange
        coEvery { statisticsRepository.getStatistics() } coAnswers {
            // Delay to keep Loading state visible
            kotlinx.coroutines.delay(100)
            createTestStatistics()
        }

        // Act
        viewModel = StatisticsViewModel(statisticsRepository)

        // Assert
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState is StatisticsUiState.Loading)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    @DisplayName("ViewModel loads statistics on initialization")
    fun `init - automatically loads statistics`() = runTest {
        // Arrange
        val testStats = createTestStatistics()
        coEvery { statisticsRepository.getStatistics() } returns testStats

        // Act
        viewModel = StatisticsViewModel(statisticsRepository)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { statisticsRepository.getStatistics() }
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is StatisticsUiState.Success)
            assertEquals(testStats, (state as StatisticsUiState.Success).statistics)
        }
    }

    // ========== LOAD STATISTICS TESTS ==========

    @Test
    @DisplayName("loadStatistics - success flow")
    fun `loadStatistics - success - updates state correctly`() = runTest {
        // Arrange
        val testStats = createTestStatistics(
            totalMinerals = 100,
            totalValue = 5000.0
        )
        coEvery { statisticsRepository.getStatistics() } returns testStats

        viewModel = StatisticsViewModel(statisticsRepository)
        advanceUntilIdle()

        // Act
        viewModel.loadStatistics()
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is StatisticsUiState.Success)
            val successState = state as StatisticsUiState.Success
            assertEquals(100, successState.statistics.totalMinerals)
            assertEquals(5000.0, successState.statistics.totalValue, 0.001)
        }
    }

    @Test
    @DisplayName("loadStatistics - error flow")
    fun `loadStatistics - error - sets Error state`() = runTest {
        // Arrange
        val errorMessage = "Database connection failed"
        coEvery { statisticsRepository.getStatistics() } throws Exception(errorMessage)

        viewModel = StatisticsViewModel(statisticsRepository)
        advanceUntilIdle()

        // Act
        viewModel.loadStatistics()
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is StatisticsUiState.Error)
            assertEquals(errorMessage, (state as StatisticsUiState.Error).message)
        }
    }

    @Test
    @DisplayName("loadStatistics - unknown error - uses default message")
    fun `loadStatistics - exception without message - uses default error`() = runTest {
        // Arrange
        coEvery { statisticsRepository.getStatistics() } throws Exception()

        viewModel = StatisticsViewModel(statisticsRepository)
        advanceUntilIdle()

        // Act
        viewModel.loadStatistics()
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is StatisticsUiState.Error)
            assertEquals("Unknown error", (state as StatisticsUiState.Error).message)
        }
    }

    @Test
    @DisplayName("loadStatistics - state transitions correctly")
    fun `loadStatistics - state transitions - Loading to Success`() = runTest {
        // Arrange
        val testStats = createTestStatistics()
        coEvery { statisticsRepository.getStatistics() } coAnswers {
            kotlinx.coroutines.delay(50)
            testStats
        }

        viewModel = StatisticsViewModel(statisticsRepository)

        // Act & Assert
        viewModel.uiState.test {
            // Initial Loading state
            val loadingState1 = awaitItem()
            assertTrue(loadingState1 is StatisticsUiState.Loading)

            advanceTimeBy(60)

            // Success state after load completes
            val successState = awaitItem()
            assertTrue(successState is StatisticsUiState.Success)

            cancelAndConsumeRemainingEvents()
        }
    }

    // ========== REFRESH STATISTICS TESTS ==========

    @Test
    @DisplayName("refreshStatistics - success flow")
    fun `refreshStatistics - success - updates state with new data`() = runTest {
        // Arrange
        val initialStats = createTestStatistics(totalMinerals = 100)
        val refreshedStats = createTestStatistics(totalMinerals = 150)

        coEvery { statisticsRepository.getStatistics() } returns initialStats
        coEvery { statisticsRepository.refreshStatistics() } returns refreshedStats

        viewModel = StatisticsViewModel(statisticsRepository)
        advanceUntilIdle()

        // Act
        viewModel.refreshStatistics()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { statisticsRepository.refreshStatistics() }
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is StatisticsUiState.Success)
            assertEquals(150, (state as StatisticsUiState.Success).statistics.totalMinerals)
        }
    }

    @Test
    @DisplayName("refreshStatistics - error flow")
    fun `refreshStatistics - error - sets Error state`() = runTest {
        // Arrange
        val initialStats = createTestStatistics()
        coEvery { statisticsRepository.getStatistics() } returns initialStats
        coEvery { statisticsRepository.refreshStatistics() } throws Exception("Refresh failed")

        viewModel = StatisticsViewModel(statisticsRepository)
        advanceUntilIdle()

        // Act
        viewModel.refreshStatistics()
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is StatisticsUiState.Error)
            assertEquals("Refresh failed", (state as StatisticsUiState.Error).message)
        }
    }

    @Test
    @DisplayName("refreshStatistics - does not set Loading state")
    fun `refreshStatistics - state - does not transition to Loading`() = runTest {
        // Arrange
        val initialStats = createTestStatistics()
        val refreshedStats = createTestStatistics()

        coEvery { statisticsRepository.getStatistics() } returns initialStats
        coEvery { statisticsRepository.refreshStatistics() } coAnswers {
            kotlinx.coroutines.delay(50)
            refreshedStats
        }

        viewModel = StatisticsViewModel(statisticsRepository)
        advanceUntilIdle()

        // Clear initial states
        viewModel.uiState.test {
            awaitItem() // Consume Success state from init
        }

        // Act
        viewModel.refreshStatistics()

        // Assert - Should go directly to Success without Loading
        viewModel.uiState.test {
            advanceTimeBy(60)
            val state = awaitItem()
            assertTrue(state is StatisticsUiState.Success)
            cancelAndConsumeRemainingEvents()
        }
    }

    // ========== MULTIPLE REFRESH TESTS ==========

    @Test
    @DisplayName("Multiple refreshes - all succeed")
    fun `refreshStatistics - multiple calls - all succeed`() = runTest {
        // Arrange
        val stats1 = createTestStatistics(totalMinerals = 100)
        val stats2 = createTestStatistics(totalMinerals = 150)
        val stats3 = createTestStatistics(totalMinerals = 200)

        coEvery { statisticsRepository.getStatistics() } returns stats1
        coEvery { statisticsRepository.refreshStatistics() } returnsMany listOf(stats2, stats3)

        viewModel = StatisticsViewModel(statisticsRepository)
        advanceUntilIdle()

        // Act
        viewModel.refreshStatistics()
        advanceUntilIdle()
        viewModel.refreshStatistics()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { statisticsRepository.refreshStatistics() }
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is StatisticsUiState.Success)
            assertEquals(200, (state as StatisticsUiState.Success).statistics.totalMinerals)
        }
    }

    // ========== HELPER METHODS ==========

    private fun createTestStatistics(
        totalMinerals: Int = 42,
        totalValue: Double = 1234.56,
        mostCommonGroup: String = "Silicates",
        mostCommonCountry: String = "France"
    ) = CollectionStatistics(
        totalMinerals = totalMinerals,
        totalValue = totalValue,
        averageValue = totalValue / totalMinerals,
        groupDistribution = mapOf("Silicates" to 20, "Oxides" to 15, "Carbonates" to 7),
        countryDistribution = mapOf("France" to 15, "Brazil" to 10, "USA" to 8),
        crystalSystemDistribution = mapOf("Hexagonal" to 12, "Cubic" to 10),
        hardnessDistribution = mapOf("7" to 15, "5-6" to 12),
        statusDistribution = mapOf("COLLECTION" to 30, "DISPLAY" to 12),
        typeDistribution = mapOf("SIMPLE" to 35, "AGGREGATE" to 7),
        mostValuableSpecimen = null,
        averageCompleteness = 0.75,
        fullyDocumentedCount = 30,
        addedThisMonth = 5,
        addedThisYear = 25,
        addedByMonthDistribution = mapOf("2025-11" to 5, "2025-10" to 8),
        mostCommonGroup = mostCommonGroup,
        mostCommonCountry = mostCommonCountry,
        mostFrequentComponents = mapOf("Si" to 25, "O" to 40),
        averageComponentCount = 3.5
    )
}
