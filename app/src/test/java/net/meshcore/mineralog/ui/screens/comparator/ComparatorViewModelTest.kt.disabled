package net.meshcore.mineralog.ui.screens.comparator

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.domain.model.Mineral
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

/**
 * Unit tests for ComparatorViewModel.
 *
 * Tests cover:
 * - Successful loading of 2-3 minerals
 * - Validation errors (too few, too many minerals)
 * - Missing mineral error (not all found)
 * - Repository exceptions
 * - State transitions (Loading → Success/Error)
 *
 * Sprint 3: ViewModel Tests - Target 70%+ coverage
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ComparatorViewModelTest {

    private lateinit var mineralRepository: MineralRepository
    private lateinit var viewModel: ComparatorViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mineralRepository = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== SUCCESS TESTS ==========

    @Test
    @DisplayName("loadMinerals - 2 minerals - loads successfully")
    fun `loadMinerals - 2 minerals - Success state with both minerals`() = runTest {
        // Arrange
        val mineralIds = listOf("mineral-1", "mineral-2")
        val minerals = listOf(
            createTestMineral("mineral-1", "Quartz"),
            createTestMineral("mineral-2", "Calcite")
        )
        coEvery { mineralRepository.getByIds(mineralIds) } returns minerals

        // Act
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ComparatorUiState.Success)
            val successState = state as ComparatorUiState.Success
            assertEquals(2, successState.minerals.size)
            assertEquals("Quartz", successState.minerals[0].name)
            assertEquals("Calcite", successState.minerals[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("loadMinerals - 3 minerals - loads successfully")
    fun `loadMinerals - 3 minerals - Success state with all three`() = runTest {
        // Arrange
        val mineralIds = listOf("mineral-1", "mineral-2", "mineral-3")
        val minerals = listOf(
            createTestMineral("mineral-1", "Quartz"),
            createTestMineral("mineral-2", "Calcite"),
            createTestMineral("mineral-3", "Feldspar")
        )
        coEvery { mineralRepository.getByIds(mineralIds) } returns minerals

        // Act
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ComparatorUiState.Success)
            val successState = state as ComparatorUiState.Success
            assertEquals(3, successState.minerals.size)
            assertEquals("Quartz", successState.minerals[0].name)
            assertEquals("Calcite", successState.minerals[1].name)
            assertEquals("Feldspar", successState.minerals[2].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("loadMinerals - state transitions from Loading to Success")
    fun `loadMinerals - state transition - Loading to Success`() = runTest {
        // Arrange
        val mineralIds = listOf("mineral-1", "mineral-2")
        val minerals = listOf(
            createTestMineral("mineral-1", "Quartz"),
            createTestMineral("mineral-2", "Calcite")
        )
        coEvery { mineralRepository.getByIds(mineralIds) } returns minerals

        // Act & Assert
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)

        viewModel.uiState.test {
            // Initial state should be Loading
            val loading = awaitItem()
            assertTrue(loading is ComparatorUiState.Loading)

            advanceUntilIdle()

            // Should transition to Success
            val success = awaitItem()
            assertTrue(success is ComparatorUiState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== VALIDATION ERROR TESTS ==========

    @Test
    @DisplayName("loadMinerals - less than 2 minerals - Error state")
    fun `loadMinerals - 1 mineral - Error requiring at least 2`() = runTest {
        // Arrange
        val mineralIds = listOf("mineral-1")

        // Act
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ComparatorUiState.Error)
            assertEquals("At least 2 minerals required for comparison", (state as ComparatorUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("loadMinerals - 0 minerals - Error state")
    fun `loadMinerals - 0 minerals - Error requiring at least 2`() = runTest {
        // Arrange
        val mineralIds = emptyList<String>()

        // Act
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ComparatorUiState.Error)
            assertEquals("At least 2 minerals required for comparison", (state as ComparatorUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("loadMinerals - more than 3 minerals - Error state")
    fun `loadMinerals - 4 minerals - Error maximum 3 allowed`() = runTest {
        // Arrange
        val mineralIds = listOf("mineral-1", "mineral-2", "mineral-3", "mineral-4")

        // Act
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ComparatorUiState.Error)
            assertEquals("Maximum 3 minerals can be compared", (state as ComparatorUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== MISSING MINERAL TESTS ==========

    @Test
    @DisplayName("loadMinerals - some minerals not found - Error state")
    fun `loadMinerals - 1 mineral missing - Error some not loaded`() = runTest {
        // Arrange
        val mineralIds = listOf("mineral-1", "mineral-2")
        val minerals = listOf(
            createTestMineral("mineral-1", "Quartz")
            // mineral-2 not found
        )
        coEvery { mineralRepository.getByIds(mineralIds) } returns minerals

        // Act
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ComparatorUiState.Error)
            assertEquals("Some minerals could not be loaded", (state as ComparatorUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("loadMinerals - all minerals not found - Error state")
    fun `loadMinerals - all minerals missing - Error some not loaded`() = runTest {
        // Arrange
        val mineralIds = listOf("mineral-1", "mineral-2")
        coEvery { mineralRepository.getByIds(mineralIds) } returns emptyList()

        // Act
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ComparatorUiState.Error)
            assertEquals("Some minerals could not be loaded", (state as ComparatorUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== EXCEPTION TESTS ==========

    @Test
    @DisplayName("loadMinerals - repository exception - Error state with message")
    fun `loadMinerals - repository error - Error state with exception message`() = runTest {
        // Arrange
        val mineralIds = listOf("mineral-1", "mineral-2")
        val errorMessage = "Database connection failed"
        coEvery { mineralRepository.getByIds(mineralIds) } throws Exception(errorMessage)

        // Act
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ComparatorUiState.Error)
            assertEquals(errorMessage, (state as ComparatorUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("loadMinerals - unknown exception - Error state with fallback message")
    fun `loadMinerals - unknown exception - Error with Unknown error message`() = runTest {
        // Arrange
        val mineralIds = listOf("mineral-1", "mineral-2")
        coEvery { mineralRepository.getByIds(mineralIds) } throws Exception()

        // Act
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is ComparatorUiState.Error)
            assertEquals("Unknown error", (state as ComparatorUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("loadMinerals - state transitions from Loading to Error")
    fun `loadMinerals - state transition - Loading to Error`() = runTest {
        // Arrange
        val mineralIds = emptyList<String>()

        // Act & Assert
        viewModel = ComparatorViewModel(mineralIds, mineralRepository)

        viewModel.uiState.test {
            // Initial state should be Loading
            val loading = awaitItem()
            assertTrue(loading is ComparatorUiState.Loading)

            advanceUntilIdle()

            // Should transition to Error
            val error = awaitItem()
            assertTrue(error is ComparatorUiState.Error)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== HELPER METHODS ==========

    private fun createTestMineral(
        id: String,
        name: String
    ) = Mineral(
        id = id,
        name = name,
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
        notes = "Test mineral",
        provenance = null,
        storage = null,
        status = "COLLECTION",
        quality = 4,
        estimatedValue = 100.0,
        acquisitionDate = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        mineralType = "SIMPLE",
        qrCode = null,
        photos = emptyList()
    )
}
