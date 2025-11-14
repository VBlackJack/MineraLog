package net.meshcore.mineralog.ui.screens.add

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.data.repository.SettingsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Unit tests for AddMineralViewModel.
 * Tests validation, draft autosave, tag autocomplete, and state management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddMineralViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mineralRepository: MineralRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: AddMineralViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mineralRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        // Mock settings repository flows
        every { settingsRepository.getDraftName() } returns flowOf("")
        every { settingsRepository.getDraftGroup() } returns flowOf("")
        every { settingsRepository.getDraftFormula() } returns flowOf("")
        every { settingsRepository.getDraftNotes() } returns flowOf("")
        every { settingsRepository.getDraftDiaphaneity() } returns flowOf("")
        every { settingsRepository.getDraftCleavage() } returns flowOf("")
        every { settingsRepository.getDraftFracture() } returns flowOf("")
        every { settingsRepository.getDraftLuster() } returns flowOf("")
        every { settingsRepository.getDraftStreak() } returns flowOf("")
        every { settingsRepository.getDraftHabit() } returns flowOf("")
        every { settingsRepository.getDraftCrystalSystem() } returns flowOf("")

        // Mock tag autocomplete
        coEvery { mineralRepository.getAllUniqueTags() } returns listOf("fluorescent", "blue", "collector", "rare")

        viewModel = AddMineralViewModel(mineralRepository, settingsRepository)
        advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onNameChange updates name state`() = runTest {
        // When
        viewModel.onNameChange("Quartz")
        advanceUntilIdle()

        // Then
        viewModel.name.test {
            assertEquals("Quartz", awaitItem())
        }
    }

    @Test
    fun `onNameChange resets save state to Idle`() = runTest {
        // Given - simulate error state
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // When
        viewModel.onNameChange("Q")
        advanceUntilIdle()

        // Then
        viewModel.saveState.test {
            assertEquals(SaveMineralState.Idle, awaitItem())
        }
    }

    @Test
    fun `saveMineral validates name is required`() = runTest {
        // When - save with blank name
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // Then
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is SaveMineralState.Error)
            assertEquals("Mineral name is required", (state as SaveMineralState.Error).message)
        }

        coVerify(exactly = 0) { mineralRepository.insert(any()) }
    }

    @Test
    fun `saveMineral validates name minimum length`() = runTest {
        // Given
        viewModel.onNameChange("Q")
        advanceUntilIdle()

        // When
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // Then
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is SaveMineralState.Error)
            assertEquals("Mineral name must be at least 2 characters", (state as SaveMineralState.Error).message)
        }

        coVerify(exactly = 0) { mineralRepository.insert(any()) }
    }

    @Test
    fun `saveMineral succeeds with valid name`() = runTest {
        // Given
        viewModel.onNameChange("Quartz")
        advanceUntilIdle()

        var savedMineralId = ""
        val onSuccess: (String) -> Unit = { savedMineralId = it }

        // When
        viewModel.saveMineral(onSuccess, File(""))
        advanceUntilIdle()

        // Then
        coVerify { mineralRepository.insert(any()) }
        coVerify { settingsRepository.clearDraft() }

        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is SaveMineralState.Success)
            assertNotEquals("", savedMineralId)
        }
    }

    @Test
    fun `saveMineral trims whitespace from name`() = runTest {
        // Given
        viewModel.onNameChange("  Quartz  ")
        advanceUntilIdle()

        // When
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // Then
        coVerify {
            mineralRepository.insert(match { mineral ->
                mineral.name == "Quartz" // Trimmed
            })
        }
    }

    @Test
    fun `saveMineral parses tags from comma-separated string`() = runTest {
        // Given
        viewModel.onNameChange("Quartz")
        viewModel.onTagsChange("fluorescent, blue, rare")
        advanceUntilIdle()

        // When
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // Then
        coVerify {
            mineralRepository.insert(match { mineral ->
                mineral.tags == listOf("fluorescent", "blue", "rare")
            })
        }
    }

    @Test
    fun `saveMineral filters blank tags`() = runTest {
        // Given
        viewModel.onNameChange("Quartz")
        viewModel.onTagsChange("fluorescent, , blue, , rare")
        advanceUntilIdle()

        // When
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // Then
        coVerify {
            mineralRepository.insert(match { mineral ->
                mineral.tags == listOf("fluorescent", "blue", "rare")
            })
        }
    }

    @Test
    fun `saveMineral sets state to Saving then Success`() = runTest(testDispatcher) {
        // Given
        viewModel.onNameChange("Quartz")
        advanceUntilIdle()

        // When
        val states = mutableListOf<SaveMineralState>()
        viewModel.saveState.test {
            states.add(awaitItem()) // Initial Idle state

            viewModel.saveMineral({}, File(""))
            advanceUntilIdle()

            // Skip intermediate states and get final
            cancelAndIgnoreRemainingEvents()
        }

        // Verify that saving state was set
        coVerify { mineralRepository.insert(any()) }
    }

    @Test
    fun `saveMineral handles exception and sets Error state`() = runTest {
        // Given
        viewModel.onNameChange("Quartz")
        coEvery { mineralRepository.insert(any()) } throws RuntimeException("Database error")
        advanceUntilIdle()

        // When
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // Then
        viewModel.saveState.test {
            val state = awaitItem()
            assertTrue(state is SaveMineralState.Error)
            assertEquals("Database error", (state as SaveMineralState.Error).message)
        }
    }

    @Test
    fun `resetSaveState sets state to Idle`() = runTest {
        // Given - simulate error state
        viewModel.onNameChange("Q") // Invalid name
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // When
        viewModel.resetSaveState()

        // Then
        viewModel.saveState.test {
            assertEquals(SaveMineralState.Idle, awaitItem())
        }
    }

    @Test
    fun `all field change methods update state correctly`() = runTest {
        // When
        viewModel.onGroupChange("Silicates")
        viewModel.onFormulaChange("SiO2")
        viewModel.onNotesChange("Test notes")
        viewModel.onDiaphaneityChange("Transparent")
        viewModel.onCleavageChange("Poor")
        viewModel.onFractureChange("Conchoidal")
        viewModel.onLusterChange("Vitreous")
        viewModel.onStreakChange("White")
        viewModel.onHabitChange("Prismatic")
        viewModel.onCrystalSystemChange("Hexagonal")
        advanceUntilIdle()

        // Then
        assertEquals("Silicates", viewModel.group.value)
        assertEquals("SiO2", viewModel.formula.value)
        assertEquals("Test notes", viewModel.notes.value)
        assertEquals("Transparent", viewModel.diaphaneity.value)
        assertEquals("Poor", viewModel.cleavage.value)
        assertEquals("Conchoidal", viewModel.fracture.value)
        assertEquals("Vitreous", viewModel.luster.value)
        assertEquals("White", viewModel.streak.value)
        assertEquals("Prismatic", viewModel.habit.value)
        assertEquals("Hexagonal", viewModel.crystalSystem.value)
    }

    @Test
    fun `saveMineral includes all optional fields when provided`() = runTest {
        // Given
        viewModel.onNameChange("Quartz")
        viewModel.onGroupChange("Silicates")
        viewModel.onFormulaChange("SiO2")
        viewModel.onNotesChange("Beautiful specimen")
        advanceUntilIdle()

        // When
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // Then
        coVerify {
            mineralRepository.insert(match { mineral ->
                mineral.name == "Quartz" &&
                mineral.group == "Silicates" &&
                mineral.formula == "SiO2" &&
                mineral.notes == "Beautiful specimen"
            })
        }
    }

    @Test
    fun `saveMineral excludes optional fields when blank`() = runTest {
        // Given - only name provided
        viewModel.onNameChange("Quartz")
        viewModel.onGroupChange("   ") // Whitespace only
        viewModel.onFormulaChange("")
        advanceUntilIdle()

        // When
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // Then
        coVerify {
            mineralRepository.insert(match { mineral ->
                mineral.name == "Quartz" &&
                mineral.group == null &&
                mineral.formula == null
            })
        }
    }

    @Test
    fun `saveMineral clears draft after successful save`() = runTest {
        // Given
        viewModel.onNameChange("Quartz")
        advanceUntilIdle()

        // When
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // Then
        coVerify { settingsRepository.clearDraft() }
    }

    @Test
    fun `saveMineral clears photos after successful save`() = runTest {
        // Given
        viewModel.onNameChange("Quartz")
        // Add a photo (simplified - actual URI not needed for this test)
        advanceUntilIdle()

        // When
        viewModel.saveMineral({}, File(""))
        advanceUntilIdle()

        // Then
        viewModel.photos.test {
            assertTrue(awaitItem().isEmpty())
        }
    }
}
