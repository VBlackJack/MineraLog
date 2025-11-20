package net.meshcore.mineralog.ui.screens.home

import android.content.Context
import androidx.paging.PagingData
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.data.repository.*
import net.meshcore.mineralog.domain.model.FilterPreset
import net.meshcore.mineralog.domain.model.Mineral
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Unit tests for HomeViewModel (MVI Architecture).
 *
 * Tests the unified uiState flow and all user interactions.
 * Migrated to JUnit 4 for Robolectric compatibility.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    // Test dispatcher
    private val testDispatcher = StandardTestDispatcher()

    // Rule for Main Dispatcher (JUnit 4)
    @get:Rule
    val mainDispatcherRule = object : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(testDispatcher)
        }
        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }

    private lateinit var viewModel: HomeViewModel

    // Mocks
    private val context = mockk<Context>(relaxed = true)
    private val mineralRepository = mockk<MineralRepository>(relaxed = true)
    private val filterPresetRepository = mockk<FilterPresetRepository>(relaxed = true)
    private val backupRepository = mockk<BackupRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    @Before
    fun setup() {
        // Default behaviors
        every { settingsRepository.getCsvExportWarningShown() } returns flowOf(false)
        every { filterPresetRepository.getAllFlow() } returns flowOf(emptyList())
        // Mock initial data loads to avoid crashing init block
        every { mineralRepository.getAllFlow(any()) } returns flowOf(emptyList())
        every { mineralRepository.searchFlow(any(), any()) } returns flowOf(emptyList())
        every { mineralRepository.filterAdvancedFlow(any(), any()) } returns flowOf(emptyList())
        every { mineralRepository.getAllPaged(any()) } returns flowOf(PagingData.empty())

        viewModel = HomeViewModel(
            context,
            mineralRepository,
            filterPresetRepository,
            backupRepository,
            settingsRepository
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ==================== Initial State ====================

    @Test
    fun `initial state is correct`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals("", initialState.searchQuery)
            assertFalse(initialState.isFilterActive)
            assertFalse(initialState.selectionMode)
            assertEquals(DialogType.None, initialState.activeDialog)
            assertTrue(initialState.selectedIds.isEmpty())
            assertEquals(0, initialState.selectionCount)
        }
    }

    // ==================== Search ====================

    @Test
    fun `onSearchQueryChange updates state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial state

            viewModel.onSearchQueryChange("Quartz")

            val updatedState = awaitItem()
            assertEquals("Quartz", updatedState.searchQuery)
        }
    }

    @Test
    fun `onSearchQueryChange with empty string clears search`() = runTest {
        // First set a search query
        viewModel.onSearchQueryChange("Quartz")

        viewModel.uiState.test {
            awaitItem() // State with "Quartz"

            viewModel.onSearchQueryChange("")

            val updatedState = awaitItem()
            assertEquals("", updatedState.searchQuery)
        }
    }

    // ==================== Filter ====================

    @Test
    fun `onFilterCriteriaChange updates state and activates filter`() = runTest {
        val criteria = FilterCriteria(mohsMin = 5f)

        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.onFilterCriteriaChange(criteria)

            val updatedState = awaitItem()
            assertEquals(criteria, updatedState.filterCriteria)
            assertTrue(updatedState.isFilterActive)
        }
    }

    @Test
    fun `clearFilter resets criteria and deactivates filter`() = runTest {
        // First set a filter
        val criteria = FilterCriteria(mohsMin = 5f)
        viewModel.onFilterCriteriaChange(criteria)

        viewModel.uiState.test {
            awaitItem() // State with filter

            viewModel.clearFilter()

            val updatedState = awaitItem()
            assertEquals(FilterCriteria.EMPTY, updatedState.filterCriteria)
            assertFalse(updatedState.isFilterActive)
        }
    }

    @Test
    fun `applyPreset applies preset criteria and activates filter`() = runTest {
        val preset = FilterPreset(
            id = "preset-1",
            name = "Hard Minerals",
            criteria = FilterCriteria(mohsMin = 7f)
        )

        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.applyPreset(preset)

            val updatedState = awaitItem()
            assertEquals(preset.criteria, updatedState.filterCriteria)
            assertTrue(updatedState.isFilterActive)
        }
    }

    // ==================== Sort ====================

    @Test
    fun `onSortOptionChange updates sort option`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial (default sort)

            viewModel.onSortOptionChange(SortOption.NAME_DESC)

            val updatedState = awaitItem()
            assertEquals(SortOption.NAME_DESC, updatedState.sortOption)
        }
    }

    // ==================== Selection Mode ====================

    @Test
    fun `enterSelectionMode activates selection and resets selection`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.enterSelectionMode()

            val state = awaitItem()
            assertTrue(state.selectionMode)
            assertTrue(state.selectedIds.isEmpty())
            assertEquals(0, state.selectionCount)
        }
    }

    @Test
    fun `exitSelectionMode deactivates selection and clears ids`() = runTest {
        viewModel.enterSelectionMode()
        viewModel.toggleSelection("min1")

        viewModel.uiState.test {
            awaitItem() // State with selection

            viewModel.exitSelectionMode()

            val state = awaitItem()
            assertFalse(state.selectionMode)
            assertTrue(state.selectedIds.isEmpty())
            assertEquals(0, state.selectionCount)
        }
    }

    @Test
    fun `toggleSelection adds id when not selected`() = runTest {
        viewModel.enterSelectionMode()

        viewModel.uiState.test {
            awaitItem() // Selection mode active

            viewModel.toggleSelection("min1")

            val stateWithSel = awaitItem()
            assertTrue(stateWithSel.selectedIds.contains("min1"))
            assertEquals(1, stateWithSel.selectionCount)
        }
    }

    @Test
    fun `toggleSelection removes id when already selected`() = runTest {
        viewModel.enterSelectionMode()
        viewModel.toggleSelection("min1")

        viewModel.uiState.test {
            awaitItem() // State with min1 selected

            viewModel.toggleSelection("min1")

            val stateEmpty = awaitItem()
            assertTrue(stateEmpty.selectedIds.isEmpty())
            assertEquals(0, stateEmpty.selectionCount)
        }
    }

    @Test
    fun `selectAll adds all mineral ids to selection`() = runTest {
        // Setup test minerals
        val minerals = listOf(
            Mineral(id = "min1", name = "Quartz"),
            Mineral(id = "min2", name = "Calcite"),
            Mineral(id = "min3", name = "Feldspar")
        )
        every { mineralRepository.getAllFlow(any()) } returns flowOf(minerals)

        // Re-initialize viewModel with test data
        viewModel = HomeViewModel(
            context,
            mineralRepository,
            filterPresetRepository,
            backupRepository,
            settingsRepository
        )

        // Wait for initialization
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.enterSelectionMode()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // Selection mode active

            viewModel.selectAll()
            testDispatcher.scheduler.advanceUntilIdle()

            val stateWithAll = awaitItem()
            assertEquals(3, stateWithAll.selectionCount)
            assertTrue(stateWithAll.selectedIds.containsAll(listOf("min1", "min2", "min3")))
        }
    }

    // ==================== Dialog Management ====================

    @Test
    fun `showFilterDialog updates activeDialog to Filter`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            viewModel.showFilterDialog()

            assertEquals(DialogType.Filter, awaitItem().activeDialog)
        }
    }

    @Test
    fun `showSortDialog updates activeDialog to Sort`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.showSortDialog()

            assertEquals(DialogType.Sort, awaitItem().activeDialog)
        }
    }

    @Test
    fun `showBulkActionsDialog updates activeDialog to BulkActions`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.showBulkActionsDialog()

            assertEquals(DialogType.BulkActions, awaitItem().activeDialog)
        }
    }

    @Test
    fun `dismissDialog updates activeDialog to None`() = runTest {
        viewModel.showFilterDialog()

        viewModel.uiState.test {
            awaitItem() // State with Filter dialog

            viewModel.dismissDialog()

            assertEquals(DialogType.None, awaitItem().activeDialog)
        }
    }

    @Test
    fun `showExportCsvDialog updates activeDialog to ExportCsv`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.showExportCsvDialog()

            assertEquals(DialogType.ExportCsv, awaitItem().activeDialog)
        }
    }

    @Test
    fun `showCsvExportWarning updates activeDialog to CsvExportWarning`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.showCsvExportWarning()

            assertEquals(DialogType.CsvExportWarning, awaitItem().activeDialog)
        }
    }

    // ==================== CSV Export Warning ====================

    @Test
    fun `initial csvExportWarningShown is false`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.csvExportWarningShown)
        }
    }

    @Test
    fun `markCsvExportWarningShown calls repository`() = runTest {
        coEvery { settingsRepository.setCsvExportWarningShown(true) } just Runs

        viewModel.markCsvExportWarningShown()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the repository method was called
        coVerify { settingsRepository.setCsvExportWarningShown(true) }
    }

    // ==================== Export/Import States ====================

    @Test
    fun `initial exportState is Idle`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.exportState is ExportState.Idle)
        }
    }

    @Test
    fun `initial importState is Idle`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.importState is ImportState.Idle)
        }
    }

    @Test
    fun `initial labelGenerationState is Idle`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.labelGenerationState is LabelGenerationState.Idle)
        }
    }

    @Test
    fun `resetExportState sets exportState to Idle`() = runTest {
        // Export states are managed internally, just verify reset works
        viewModel.resetExportState()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.exportState is ExportState.Idle)
        }
    }
}
