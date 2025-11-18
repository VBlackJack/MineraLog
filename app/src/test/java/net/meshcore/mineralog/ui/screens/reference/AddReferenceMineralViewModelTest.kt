package net.meshcore.mineralog.ui.screens.reference

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.data.repository.ReferenceMineralRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for AddReferenceMineralViewModel.
 *
 * Tests cover:
 * - Field updates
 * - Save operation (validation, duplicate checking)
 * - Required fields validation (nameFr, nameEn)
 * - Duplicate name detection
 * - Save state transitions
 *
 * Sprint 3: ViewModel Tests - Target 70%+ coverage
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddReferenceMineralViewModelTest {

    private lateinit var referenceMineralRepository: ReferenceMineralRepository
    private lateinit var viewModel: AddReferenceMineralViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        referenceMineralRepository = mockk(relaxed = true)

        viewModel = AddReferenceMineralViewModel(referenceMineralRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== INITIALIZATION TESTS ==========

    @Test
    @DisplayName("init - fields empty by default")
    fun `init - all fields - empty initially`() = runTest {
        // Assert
        viewModel.nameFr.test {
            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.nameEn.test {
            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is AddReferenceMineralViewModel.SaveState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== FIELD UPDATE TESTS ==========

    @Test
    @DisplayName("onNameFrChange - updates field and clears error")
    fun `onNameFrChange - field update - clears validation error`() = runTest {
        // Act
        viewModel.onNameFrChange("Quartz")

        // Assert
        viewModel.nameFr.test {
            assertEquals("Quartz", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.nameFrError.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("onNameEnChange - updates field and clears error")
    fun `onNameEnChange - field update - clears validation error`() = runTest {
        // Act
        viewModel.onNameEnChange("Quartz")

        // Assert
        viewModel.nameEn.test {
            assertEquals("Quartz", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.nameEnError.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("onFormulaChange - updates formula field")
    fun `onFormulaChange - field update - formula set correctly`() = runTest {
        // Act
        viewModel.onFormulaChange("SiO2")

        // Assert
        viewModel.formula.test {
            assertEquals("SiO2", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== SAVE TESTS ==========

    @Test
    @DisplayName("save - valid fields - creates mineral successfully")
    fun `save - valid fields - Success state and insert called`() = runTest {
        // Arrange
        coEvery { referenceMineralRepository.searchByNameLimit(any(), any()) } returns flowOf(emptyList())
        coEvery { referenceMineralRepository.insert(any()) } just Runs

        viewModel.onNameFrChange("Quartz")
        viewModel.onNameEnChange("Quartz EN")
        viewModel.onFormulaChange("SiO2")

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is AddReferenceMineralViewModel.SaveState.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { referenceMineralRepository.insert(match { it.nameFr == "Quartz" && it.isUserDefined }) }
    }

    @Test
    @DisplayName("save - missing nameFr - validation error")
    fun `save - missing nameFr - error set and save fails`() = runTest {
        // Arrange
        viewModel.onNameFrChange("")
        viewModel.onNameEnChange("Valid Name")

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        viewModel.nameFrError.test {
            val error = awaitItem()
            assertTrue(error?.contains("obligatoire") == true)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is AddReferenceMineralViewModel.SaveState.Error)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { referenceMineralRepository.insert(any()) }
    }

    @Test
    @DisplayName("save - missing nameEn - validation error")
    fun `save - missing nameEn - error set and save fails`() = runTest {
        // Arrange
        viewModel.onNameFrChange("Valid Name")
        viewModel.onNameEnChange("")

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        viewModel.nameEnError.test {
            val error = awaitItem()
            assertTrue(error?.contains("obligatoire") == true)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { referenceMineralRepository.insert(any()) }
    }

    @Test
    @DisplayName("save - both names missing - both errors set")
    fun `save - both names missing - both errors set`() = runTest {
        // Arrange
        viewModel.onNameFrChange("")
        viewModel.onNameEnChange("")

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        viewModel.nameFrError.test {
            val error = awaitItem()
            assertTrue(error?.contains("obligatoire") == true)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.nameEnError.test {
            val error = awaitItem()
            assertTrue(error?.contains("obligatoire") == true)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { referenceMineralRepository.insert(any()) }
    }

    @Test
    @DisplayName("save - duplicate name - error set")
    fun `save - duplicate name - error duplicate exists`() = runTest {
        // Arrange
        val existingMineral = mockk<ReferenceMineralEntity>()
        coEvery { referenceMineralRepository.searchByNameLimit("Existing", 1) } returns flowOf(listOf(existingMineral))

        viewModel.onNameFrChange("Existing")
        viewModel.onNameEnChange("Valid Name EN")

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        viewModel.nameFrError.test {
            val error = awaitItem()
            assertTrue(error?.contains("existe déjà") == true)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is AddReferenceMineralViewModel.SaveState.Error)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { referenceMineralRepository.insert(any()) }
    }

    @Test
    @DisplayName("save - repository exception - Error state")
    fun `save - repository error - Error state with exception message`() = runTest {
        // Arrange
        val errorMessage = "Database constraint violation"
        coEvery { referenceMineralRepository.searchByNameLimit(any(), any()) } returns flowOf(emptyList())
        coEvery { referenceMineralRepository.insert(any()) } throws Exception(errorMessage)

        viewModel.onNameFrChange("Quartz")
        viewModel.onNameEnChange("Quartz EN")

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is AddReferenceMineralViewModel.SaveState.Error)
            assertTrue((state as AddReferenceMineralViewModel.SaveState.Error).message.contains(errorMessage))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("save - state transitions - Idle to Saving to Success")
    fun `save - state transitions - correct flow`() = runTest {
        // Arrange
        coEvery { referenceMineralRepository.searchByNameLimit(any(), any()) } returns flowOf(emptyList())
        coEvery { referenceMineralRepository.insert(any()) } just Runs

        viewModel.onNameFrChange("Quartz")
        viewModel.onNameEnChange("Quartz EN")

        // Act & Assert
        viewModel.saveState.test {
            // Initial Idle state
            val initial = awaitItem()
            assertTrue(initial is AddReferenceMineralViewModel.SaveState.Idle)

            // Trigger save
            viewModel.save()

            // Saving state
            val saving = awaitItem()
            assertTrue(saving is AddReferenceMineralViewModel.SaveState.Saving)

            advanceUntilIdle()

            // Success state
            val success = awaitItem()
            assertTrue(success is AddReferenceMineralViewModel.SaveState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("resetSaveState - resets to Idle")
    fun `resetSaveState - state reset - to Idle`() = runTest {
        // Arrange
        coEvery { referenceMineralRepository.searchByNameLimit(any(), any()) } returns flowOf(emptyList())
        coEvery { referenceMineralRepository.insert(any()) } just Runs

        viewModel.onNameFrChange("Quartz")
        viewModel.onNameEnChange("Quartz EN")
        viewModel.save()
        advanceUntilIdle()

        // Act
        viewModel.resetSaveState()

        // Assert
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is AddReferenceMineralViewModel.SaveState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("save - trims whitespace from fields")
    fun `save - field trimming - whitespace removed before save`() = runTest {
        // Arrange
        coEvery { referenceMineralRepository.searchByNameLimit(any(), any()) } returns flowOf(emptyList())
        coEvery { referenceMineralRepository.insert(any()) } just Runs

        viewModel.onNameFrChange("  Quartz  ")
        viewModel.onNameEnChange("  Quartz EN  ")

        // Act
        viewModel.save()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            referenceMineralRepository.insert(match {
                it.nameFr == "Quartz" && it.nameEn == "Quartz EN"
            })
        }
    }
}
