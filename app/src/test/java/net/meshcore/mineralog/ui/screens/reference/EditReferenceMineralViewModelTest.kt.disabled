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
import java.time.Instant

/**
 * Unit tests for EditReferenceMineralViewModel.
 *
 * Tests cover:
 * - Mineral loading (success, not found, error)
 * - Field updates
 * - Update operation (user-defined minerals only)
 * - Create custom copy (for standard minerals)
 * - Validation (required fields, duplicates)
 * - State management (load, save states)
 *
 * Sprint 3: ViewModel Tests - Target 70%+ coverage
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditReferenceMineralViewModelTest {

    private lateinit var referenceMineralRepository: ReferenceMineralRepository
    private lateinit var viewModel: EditReferenceMineralViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val mineralId = "test-mineral-id"

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        referenceMineralRepository = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== LOADING TESTS ==========

    @Test
    @DisplayName("init - loads mineral and populates fields")
    fun `init - mineral loading - Success with fields populated`() = runTest {
        // Arrange
        val testMineral = createTestMineral(mineralId, "Quartz", isUserDefined = true)
        coEvery { referenceMineralRepository.getById(mineralId) } returns testMineral

        // Act
        viewModel = EditReferenceMineralViewModel(referenceMineralRepository, mineralId)
        advanceUntilIdle()

        // Assert
        viewModel.loadState.test {
            val state = awaitItem()
            assertTrue(state is EditReferenceMineralViewModel.LoadState.Success)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.nameFr.test {
            assertEquals("Quartz", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - mineral not found - Error state")
    fun `init - mineral not found - Error with message`() = runTest {
        // Arrange
        coEvery { referenceMineralRepository.getById(mineralId) } returns null

        // Act
        viewModel = EditReferenceMineralViewModel(referenceMineralRepository, mineralId)
        advanceUntilIdle()

        // Assert
        viewModel.loadState.test {
            val state = awaitItem()
            assertTrue(state is EditReferenceMineralViewModel.LoadState.Error)
            assertEquals("Minéral non trouvé", (state as EditReferenceMineralViewModel.LoadState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - standard mineral - shows warning")
    fun `init - standard mineral - showStandardMineralWarning true`() = runTest {
        // Arrange
        val standardMineral = createTestMineral(mineralId, "Quartz", isUserDefined = false)
        coEvery { referenceMineralRepository.getById(mineralId) } returns standardMineral

        // Act
        viewModel = EditReferenceMineralViewModel(referenceMineralRepository, mineralId)
        advanceUntilIdle()

        // Assert
        viewModel.showStandardMineralWarning.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - user-defined mineral - no warning")
    fun `init - user-defined mineral - showStandardMineralWarning false`() = runTest {
        // Arrange
        val userMineral = createTestMineral(mineralId, "Quartz", isUserDefined = true)
        coEvery { referenceMineralRepository.getById(mineralId) } returns userMineral

        // Act
        viewModel = EditReferenceMineralViewModel(referenceMineralRepository, mineralId)
        advanceUntilIdle()

        // Assert
        viewModel.showStandardMineralWarning.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== FIELD UPDATE TESTS ==========

    @Test
    @DisplayName("onNameFrChange - updates field and clears error")
    fun `onNameFrChange - field update - clears validation error`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = true)

        // Act
        viewModel.onNameFrChange("New Name")

        // Assert
        viewModel.nameFr.test {
            assertEquals("New Name", awaitItem())
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
        // Arrange
        setupViewModel(isUserDefined = true)

        // Act
        viewModel.onNameEnChange("New English Name")

        // Assert
        viewModel.nameEn.test {
            assertEquals("New English Name", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.nameEnError.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== UPDATE TESTS (User-Defined Minerals) ==========

    @Test
    @DisplayName("update - user-defined mineral - updates successfully")
    fun `update - user-defined mineral - Success state`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = true)
        coEvery { referenceMineralRepository.searchByNameLimit(any(), any()) } returns flowOf(emptyList())
        coEvery { referenceMineralRepository.update(any()) } just Runs

        viewModel.onNameFrChange("Updated Quartz")
        viewModel.onNameEnChange("Updated Quartz EN")

        // Act
        viewModel.update()
        advanceUntilIdle()

        // Assert
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is EditReferenceMineralViewModel.SaveState.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { referenceMineralRepository.update(any()) }
    }

    @Test
    @DisplayName("update - standard mineral - Error state")
    fun `update - standard mineral - Error cannot modify`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = false)

        // Act
        viewModel.update()
        advanceUntilIdle()

        // Assert
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is EditReferenceMineralViewModel.SaveState.Error)
            assertTrue((state as EditReferenceMineralViewModel.SaveState.Error).message.contains("standard"))
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { referenceMineralRepository.update(any()) }
    }

    @Test
    @DisplayName("update - missing name French - validation error")
    fun `update - missing nameFr - validation error set`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = true)
        viewModel.onNameFrChange("")
        viewModel.onNameEnChange("Valid Name")

        // Act
        viewModel.update()
        advanceUntilIdle()

        // Assert
        viewModel.nameFrError.test {
            val error = awaitItem()
            assertTrue(error?.contains("obligatoire") == true)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is EditReferenceMineralViewModel.SaveState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("update - missing name English - validation error")
    fun `update - missing nameEn - validation error set`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = true)
        viewModel.onNameFrChange("Valid Name")
        viewModel.onNameEnChange("")

        // Act
        viewModel.update()
        advanceUntilIdle()

        // Assert
        viewModel.nameEnError.test {
            val error = awaitItem()
            assertTrue(error?.contains("obligatoire") == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("update - duplicate name - validation error")
    fun `update - duplicate name - Error duplicate exists`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = true)
        val existingMineral = createTestMineral("other-id", "Existing Name")
        coEvery { referenceMineralRepository.searchByNameLimit("Existing Name", 1) } returns flowOf(listOf(existingMineral))

        viewModel.onNameFrChange("Existing Name")

        // Act
        viewModel.update()
        advanceUntilIdle()

        // Assert
        viewModel.nameFrError.test {
            val error = awaitItem()
            assertTrue(error?.contains("existe déjà") == true)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is EditReferenceMineralViewModel.SaveState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== CREATE CUSTOM COPY TESTS ==========

    @Test
    @DisplayName("createCustomCopy - valid fields - creates new mineral")
    fun `createCustomCopy - valid fields - Success state and insert called`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = false) // Standard mineral
        coEvery { referenceMineralRepository.searchByNameLimit(any(), any()) } returns flowOf(emptyList())
        coEvery { referenceMineralRepository.insert(any()) } just Runs

        viewModel.onNameFrChange("Custom Quartz")
        viewModel.onNameEnChange("Custom Quartz EN")

        // Act
        viewModel.createCustomCopy()
        advanceUntilIdle()

        // Assert
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is EditReferenceMineralViewModel.SaveState.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { referenceMineralRepository.insert(match { it.isUserDefined }) }
    }

    @Test
    @DisplayName("createCustomCopy - missing fields - validation error")
    fun `createCustomCopy - missing nameFr - validation error`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = false)
        viewModel.onNameFrChange("")
        viewModel.onNameEnChange("Valid Name")

        // Act
        viewModel.createCustomCopy()
        advanceUntilIdle()

        // Assert
        viewModel.nameFrError.test {
            val error = awaitItem()
            assertTrue(error?.contains("obligatoire") == true)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { referenceMineralRepository.insert(any()) }
    }

    @Test
    @DisplayName("createCustomCopy - duplicate name - error")
    fun `createCustomCopy - duplicate name - Error duplicate exists`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = false)
        val existing = createTestMineral("existing-id", "Duplicate Name")
        coEvery { referenceMineralRepository.searchByNameLimit("Duplicate Name", 1) } returns flowOf(listOf(existing))

        viewModel.onNameFrChange("Duplicate Name")
        viewModel.onNameEnChange("Valid Name EN")

        // Act
        viewModel.createCustomCopy()
        advanceUntilIdle()

        // Assert
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is EditReferenceMineralViewModel.SaveState.Error)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { referenceMineralRepository.insert(any()) }
    }

    // ========== UTILITY METHODS TESTS ==========

    @Test
    @DisplayName("resetSaveState - resets to Idle")
    fun `resetSaveState - state reset - to Idle`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = true)
        coEvery { referenceMineralRepository.searchByNameLimit(any(), any()) } returns flowOf(emptyList())
        coEvery { referenceMineralRepository.update(any()) } just Runs

        viewModel.update()
        advanceUntilIdle()

        // Act
        viewModel.resetSaveState()

        // Assert
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is EditReferenceMineralViewModel.SaveState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("dismissStandardMineralWarning - hides warning")
    fun `dismissStandardMineralWarning - warning flag - set to false`() = runTest {
        // Arrange
        setupViewModel(isUserDefined = false)

        // Act
        viewModel.dismissStandardMineralWarning()

        // Assert
        viewModel.showStandardMineralWarning.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== HELPER METHODS ==========

    private fun setupViewModel(isUserDefined: Boolean) {
        val testMineral = createTestMineral(mineralId, "Quartz", isUserDefined)
        coEvery { referenceMineralRepository.getById(mineralId) } returns testMineral

        viewModel = EditReferenceMineralViewModel(referenceMineralRepository, mineralId)
        advanceUntilIdle()
    }

    private fun createTestMineral(
        id: String,
        name: String,
        isUserDefined: Boolean = true
    ) = ReferenceMineralEntity(
        id = id,
        nameFr = name,
        nameEn = "$name EN",
        synonyms = null,
        mineralGroup = "Silicates",
        formula = "SiO2",
        mohsMin = 7.0f,
        mohsMax = 7.0f,
        density = 2.65f,
        crystalSystem = "Hexagonal",
        cleavage = "None",
        fracture = "Conchoidal",
        habit = null,
        luster = "Vitreous",
        streak = "White",
        diaphaneity = "Transparent",
        fluorescence = null,
        magnetism = null,
        radioactivity = null,
        notes = null,
        isUserDefined = isUserDefined,
        source = "Test source",
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
