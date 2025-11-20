package net.meshcore.mineralog.ui.screens.reference

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.data.repository.ReferenceMineralRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

/**
 * Unit tests for ReferenceMineralDetailViewModel.
 *
 * Tests cover:
 * - Mineral loading (success, not found, error)
 * - Usage statistics loading (simple specimens, components, total)
 * - Loading and error state management
 * - Refresh operation
 * - Delete operations (with/without dependencies, confirmation flow)
 * - State transitions
 *
 * Sprint 3: ViewModel Tests - Target 70%+ coverage
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceMineralDetailViewModelTest {

    private lateinit var referenceMineralRepository: ReferenceMineralRepository
    private lateinit var viewModel: ReferenceMineralDetailViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val referenceMineralId = "test-ref-mineral-id"

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        referenceMineralRepository = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== MINERAL LOADING TESTS ==========

    @Test
    @DisplayName("init - loads mineral successfully")
    fun `init - mineral loading - Success with mineral data`() = runTest {
        // Arrange
        val testMineral = createTestReferencMineral(referenceMineralId, "Quartz")
        coEvery { referenceMineralRepository.getById(referenceMineralId) } returns testMineral
        coEvery { referenceMineralRepository.countSimpleSpecimensUsingReference(referenceMineralId) } returns 5
        coEvery { referenceMineralRepository.countComponentsUsingReference(referenceMineralId) } returns 3
        coEvery { referenceMineralRepository.getTotalUsageCount(referenceMineralId) } returns 8

        // Act
        viewModel = ReferenceMineralDetailViewModel(referenceMineralId, referenceMineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.mineral.test {
            val mineral = awaitItem()
            assertNotNull(mineral)
            assertEquals("Quartz", mineral?.name)
            assertEquals(referenceMineralId, mineral?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - mineral not found - sets error")
    fun `init - mineral not found - Error state with message`() = runTest {
        // Arrange
        coEvery { referenceMineralRepository.getById(referenceMineralId) } returns null
        coEvery { referenceMineralRepository.countSimpleSpecimensUsingReference(any()) } returns 0
        coEvery { referenceMineralRepository.countComponentsUsingReference(any()) } returns 0
        coEvery { referenceMineralRepository.getTotalUsageCount(any()) } returns 0

        // Act
        viewModel = ReferenceMineralDetailViewModel(referenceMineralId, referenceMineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.error.test {
            val error = awaitItem()
            assertEquals("Minéral de référence introuvable", error)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.mineral.test {
            val mineral = awaitItem()
            assertNull(mineral)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - loading state - transitions correctly")
    fun `init - loading state - true during load then false`() = runTest {
        // Arrange
        val testMineral = createTestReferencMineral(referenceMineralId, "Quartz")
        coEvery { referenceMineralRepository.getById(referenceMineralId) } returns testMineral
        coEvery { referenceMineralRepository.countSimpleSpecimensUsingReference(any()) } returns 0
        coEvery { referenceMineralRepository.countComponentsUsingReference(any()) } returns 0
        coEvery { referenceMineralRepository.getTotalUsageCount(any()) } returns 0

        // Act & Assert
        viewModel = ReferenceMineralDetailViewModel(referenceMineralId, referenceMineralRepository)

        viewModel.isLoading.test {
            // Initial state
            val initial = awaitItem()
            assertTrue(initial)

            advanceUntilIdle()

            // After loading
            val afterLoad = awaitItem()
            assertFalse(afterLoad)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - repository exception - sets error")
    fun `init - repository error - Error state with exception message`() = runTest {
        // Arrange
        val errorMessage = "Database connection failed"
        coEvery { referenceMineralRepository.getById(referenceMineralId) } throws Exception(errorMessage)
        coEvery { referenceMineralRepository.countSimpleSpecimensUsingReference(any()) } returns 0
        coEvery { referenceMineralRepository.countComponentsUsingReference(any()) } returns 0
        coEvery { referenceMineralRepository.getTotalUsageCount(any()) } returns 0

        // Act
        viewModel = ReferenceMineralDetailViewModel(referenceMineralId, referenceMineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.error.test {
            val error = awaitItem()
            assertTrue(error?.contains(errorMessage) == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== USAGE STATISTICS TESTS ==========

    @Test
    @DisplayName("init - loads usage statistics")
    fun `init - usage statistics - all counts loaded correctly`() = runTest {
        // Arrange
        val testMineral = createTestReferencMineral(referenceMineralId, "Quartz")
        coEvery { referenceMineralRepository.getById(referenceMineralId) } returns testMineral
        coEvery { referenceMineralRepository.countSimpleSpecimensUsingReference(referenceMineralId) } returns 12
        coEvery { referenceMineralRepository.countComponentsUsingReference(referenceMineralId) } returns 5
        coEvery { referenceMineralRepository.getTotalUsageCount(referenceMineralId) } returns 17

        // Act
        viewModel = ReferenceMineralDetailViewModel(referenceMineralId, referenceMineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.simpleSpecimensCount.test {
            assertEquals(12, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.componentsCount.test {
            assertEquals(5, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.totalUsageCount.test {
            assertEquals(17, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - statistics error - does not affect mineral loading")
    fun `init - statistics error - silently handled`() = runTest {
        // Arrange
        val testMineral = createTestReferencMineral(referenceMineralId, "Quartz")
        coEvery { referenceMineralRepository.getById(referenceMineralId) } returns testMineral
        coEvery { referenceMineralRepository.countSimpleSpecimensUsingReference(any()) } throws Exception("Stats error")
        coEvery { referenceMineralRepository.countComponentsUsingReference(any()) } throws Exception("Stats error")
        coEvery { referenceMineralRepository.getTotalUsageCount(any()) } throws Exception("Stats error")

        // Act
        viewModel = ReferenceMineralDetailViewModel(referenceMineralId, referenceMineralRepository)
        advanceUntilIdle()

        // Assert - Mineral should still load
        viewModel.mineral.test {
            val mineral = awaitItem()
            assertNotNull(mineral)
            assertEquals("Quartz", mineral?.name)
            cancelAndIgnoreRemainingEvents()
        }

        // Error should be null (statistics errors are silent)
        viewModel.error.test {
            val error = awaitItem()
            assertNull(error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== REFRESH TESTS ==========

    @Test
    @DisplayName("refresh - reloads mineral and statistics")
    fun `refresh - reloads all data - mineral and stats`() = runTest {
        // Arrange
        val testMineral = createTestReferencMineral(referenceMineralId, "Quartz")
        coEvery { referenceMineralRepository.getById(referenceMineralId) } returns testMineral
        coEvery { referenceMineralRepository.countSimpleSpecimensUsingReference(any()) } returns 5
        coEvery { referenceMineralRepository.countComponentsUsingReference(any()) } returns 3
        coEvery { referenceMineralRepository.getTotalUsageCount(any()) } returns 8

        viewModel = ReferenceMineralDetailViewModel(referenceMineralId, referenceMineralRepository)
        advanceUntilIdle()

        // Update repository to return new data
        val updatedMineral = createTestReferencMineral(referenceMineralId, "Updated Quartz")
        coEvery { referenceMineralRepository.getById(referenceMineralId) } returns updatedMineral
        coEvery { referenceMineralRepository.countSimpleSpecimensUsingReference(any()) } returns 10
        coEvery { referenceMineralRepository.getTotalUsageCount(any()) } returns 15

        // Act
        viewModel.refresh()
        advanceUntilIdle()

        // Assert
        viewModel.mineral.test {
            val mineral = awaitItem()
            assertEquals("Updated Quartz", mineral?.name)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.simpleSpecimensCount.test {
            assertEquals(10, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== DELETE OPERATION TESTS ==========

    @Test
    @DisplayName("initiateDelete - no dependencies - deletes directly")
    fun `initiateDelete - no usage - confirm delete called automatically`() = runTest {
        // Arrange
        setupViewModel(usageCount = 0)
        coEvery { referenceMineralRepository.canDelete(referenceMineralId) } returns true
        coEvery { referenceMineralRepository.deleteById(referenceMineralId) } just Runs

        // Act
        viewModel.initiateDelete()
        advanceUntilIdle()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is ReferenceMineralDetailViewModel.DeleteState.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { referenceMineralRepository.canDelete(referenceMineralId) }
        coVerify(exactly = 1) { referenceMineralRepository.deleteById(referenceMineralId) }
    }

    @Test
    @DisplayName("initiateDelete - has dependencies - requires confirmation")
    fun `initiateDelete - has usage - ConfirmRequired state`() = runTest {
        // Arrange
        setupViewModel(usageCount = 5)

        // Act
        viewModel.initiateDelete()
        advanceUntilIdle()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is ReferenceMineralDetailViewModel.DeleteState.ConfirmRequired)
            assertEquals(5, (state as ReferenceMineralDetailViewModel.DeleteState.ConfirmRequired).usageCount)
            cancelAndIgnoreRemainingEvents()
        }

        // Should NOT call delete yet
        coVerify(exactly = 0) { referenceMineralRepository.deleteById(any()) }
    }

    @Test
    @DisplayName("confirmDelete - success - deletes mineral")
    fun `confirmDelete - can delete - Success state`() = runTest {
        // Arrange
        setupViewModel(usageCount = 0)
        coEvery { referenceMineralRepository.canDelete(referenceMineralId) } returns true
        coEvery { referenceMineralRepository.deleteById(referenceMineralId) } just Runs

        // Act
        viewModel.confirmDelete()
        advanceUntilIdle()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is ReferenceMineralDetailViewModel.DeleteState.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { referenceMineralRepository.canDelete(referenceMineralId) }
        coVerify(exactly = 1) { referenceMineralRepository.deleteById(referenceMineralId) }
    }

    @Test
    @DisplayName("confirmDelete - cannot delete - Error state")
    fun `confirmDelete - cannot delete - Error state with message`() = runTest {
        // Arrange
        setupViewModel(usageCount = 0)
        coEvery { referenceMineralRepository.canDelete(referenceMineralId) } returns false

        // Act
        viewModel.confirmDelete()
        advanceUntilIdle()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is ReferenceMineralDetailViewModel.DeleteState.Error)
            assertTrue((state as ReferenceMineralDetailViewModel.DeleteState.Error).message.contains("Impossible"))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { referenceMineralRepository.deleteById(any()) }
    }

    @Test
    @DisplayName("confirmDelete - repository exception - Error state")
    fun `confirmDelete - repository error - Error state with exception message`() = runTest {
        // Arrange
        setupViewModel(usageCount = 0)
        val errorMessage = "Foreign key constraint"
        coEvery { referenceMineralRepository.canDelete(referenceMineralId) } returns true
        coEvery { referenceMineralRepository.deleteById(referenceMineralId) } throws Exception(errorMessage)

        // Act
        viewModel.confirmDelete()
        advanceUntilIdle()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is ReferenceMineralDetailViewModel.DeleteState.Error)
            assertTrue((state as ReferenceMineralDetailViewModel.DeleteState.Error).message.contains(errorMessage))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("confirmDelete - state transitions - Idle to Deleting to Success")
    fun `confirmDelete - state transitions - correct flow`() = runTest {
        // Arrange
        setupViewModel(usageCount = 0)
        coEvery { referenceMineralRepository.canDelete(referenceMineralId) } returns true
        coEvery { referenceMineralRepository.deleteById(referenceMineralId) } just Runs

        // Act & Assert
        viewModel.deleteState.test {
            // Initial Idle state
            val initial = awaitItem()
            assertTrue(initial is ReferenceMineralDetailViewModel.DeleteState.Idle)

            // Trigger delete
            viewModel.confirmDelete()

            // Deleting state
            val deleting = awaitItem()
            assertTrue(deleting is ReferenceMineralDetailViewModel.DeleteState.Deleting)

            advanceUntilIdle()

            // Success state
            val success = awaitItem()
            assertTrue(success is ReferenceMineralDetailViewModel.DeleteState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("cancelDelete - resets state to Idle")
    fun `cancelDelete - state reset - to Idle`() = runTest {
        // Arrange
        setupViewModel(usageCount = 5)
        viewModel.initiateDelete()
        advanceUntilIdle()

        // Act
        viewModel.cancelDelete()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is ReferenceMineralDetailViewModel.DeleteState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("resetDeleteState - resets state to Idle")
    fun `resetDeleteState - state reset - to Idle`() = runTest {
        // Arrange
        setupViewModel(usageCount = 0)
        coEvery { referenceMineralRepository.canDelete(referenceMineralId) } returns true
        coEvery { referenceMineralRepository.deleteById(referenceMineralId) } just Runs
        viewModel.confirmDelete()
        advanceUntilIdle()

        // Act
        viewModel.resetDeleteState()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is ReferenceMineralDetailViewModel.DeleteState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== HELPER METHODS ==========

    private fun setupViewModel(usageCount: Int = 0) {
        val testMineral = createTestReferencMineral(referenceMineralId, "Quartz")
        coEvery { referenceMineralRepository.getById(referenceMineralId) } returns testMineral
        coEvery { referenceMineralRepository.countSimpleSpecimensUsingReference(referenceMineralId) } returns usageCount
        coEvery { referenceMineralRepository.countComponentsUsingReference(referenceMineralId) } returns 0
        coEvery { referenceMineralRepository.getTotalUsageCount(referenceMineralId) } returns usageCount

        viewModel = ReferenceMineralDetailViewModel(referenceMineralId, referenceMineralRepository)
        advanceUntilIdle()
    }

    private fun createTestReferencMineral(
        id: String,
        name: String
    ) = ReferenceMineralEntity(
        id = id,
        name = name,
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
        group = "Silicates",
        description = "Test description",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
