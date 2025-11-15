package net.meshcore.mineralog.ui.screens.home

import android.content.Context
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
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.data.repository.BackupRepository
import net.meshcore.mineralog.data.repository.FilterPresetRepository
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.data.repository.SettingsRepository
import net.meshcore.mineralog.domain.model.FilterPreset
import net.meshcore.mineralog.domain.model.Mineral
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for HomeViewModel.
 * Tests search, filtering, bulk selection, and preset management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var mineralRepository: MineralRepository
    private lateinit var filterPresetRepository: FilterPresetRepository
    private lateinit var backupRepository: BackupRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: HomeViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        mineralRepository = mockk(relaxed = true)
        filterPresetRepository = mockk(relaxed = true)
        backupRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        // Mock repository flows
        every { mineralRepository.getAllFlow() } returns flowOf(emptyList())
        every { mineralRepository.searchFlow(any()) } returns flowOf(emptyList())
        every { mineralRepository.filterAdvancedFlow(any()) } returns flowOf(emptyList())
        every { filterPresetRepository.getAllFlow() } returns flowOf(emptyList())
        every { settingsRepository.getCsvExportWarningShown() } returns flowOf(false)

        viewModel = HomeViewModel(
            context,
            mineralRepository,
            filterPresetRepository,
            backupRepository,
            settingsRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSearchQueryChange updates search query state`() = runTest {
        // When
        viewModel.onSearchQueryChange("quartz")
        advanceUntilIdle()

        // Then
        viewModel.searchQuery.test {
            assertEquals("quartz", awaitItem())
        }
    }

    @Test
    fun `onFilterCriteriaChange updates filter criteria and activates filter`() = runTest {
        // Given
        val criteria = FilterCriteria(
            groups = listOf("Silicates"),
            countries = listOf("France")
        )

        // When
        viewModel.onFilterCriteriaChange(criteria)
        advanceUntilIdle()

        // Then
        viewModel.filterCriteria.test {
            assertEquals(criteria, awaitItem())
        }

        viewModel.isFilterActive.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `clearFilter resets filter criteria to empty and deactivates filter`() = runTest {
        // Given - set a filter first
        val criteria = FilterCriteria(groups = listOf("Silicates"))
        viewModel.onFilterCriteriaChange(criteria)
        advanceUntilIdle()

        // When
        viewModel.clearFilter()
        advanceUntilIdle()

        // Then
        viewModel.filterCriteria.test {
            assertEquals(FilterCriteria.EMPTY, awaitItem())
        }

        viewModel.isFilterActive.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `applyPreset applies preset criteria`() = runTest {
        // Given
        val preset = FilterPreset(
            id = "preset-1",
            name = "French Silicates",
            criteria = FilterCriteria(
                groups = listOf("Silicates"),
                countries = listOf("France")
            )
        )

        // When
        viewModel.applyPreset(preset)
        advanceUntilIdle()

        // Then
        viewModel.filterCriteria.test {
            assertEquals(preset.criteria, awaitItem())
        }

        viewModel.isFilterActive.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `savePreset saves preset to repository`() = runTest {
        // Given
        val preset = FilterPreset(
            id = "preset-1",
            name = "My Preset",
            criteria = FilterCriteria(groups = listOf("Silicates"))
        )

        // When
        viewModel.savePreset(preset)
        advanceUntilIdle()

        // Then
        coVerify { filterPresetRepository.save(preset) }
    }

    @Test
    fun `enterSelectionMode activates selection mode`() = runTest {
        // When
        viewModel.enterSelectionMode()

        // Then
        viewModel.selectionMode.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `exitSelectionMode deactivates selection mode and clears selection`() = runTest {
        // Given
        viewModel.enterSelectionMode()
        viewModel.toggleSelection("mineral-1")
        advanceUntilIdle()

        // When
        viewModel.exitSelectionMode()

        // Then
        viewModel.selectionMode.test {
            assertFalse(awaitItem())
        }

        viewModel.selectedIds.test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `toggleSelection adds id when not selected`() = runTest {
        // Given
        viewModel.enterSelectionMode()

        // When
        viewModel.toggleSelection("mineral-1")

        // Then
        viewModel.selectedIds.test {
            assertTrue(awaitItem().contains("mineral-1"))
        }
    }

    @Test
    fun `toggleSelection removes id when already selected`() = runTest {
        // Given
        viewModel.enterSelectionMode()
        viewModel.toggleSelection("mineral-1")
        advanceUntilIdle()

        // When
        viewModel.toggleSelection("mineral-1")

        // Then
        viewModel.selectedIds.test {
            assertFalse(awaitItem().contains("mineral-1"))
        }
    }

    @Test
    fun `selectAll selects all minerals`() = runTest {
        // Given
        val minerals = listOf(
            Mineral(id = "1", name = "Quartz"),
            Mineral(id = "2", name = "Calcite"),
            Mineral(id = "3", name = "Fluorite")
        )
        every { mineralRepository.getAllFlow() } returns flowOf(minerals)

        // Recreate viewModel to pick up new flow
        viewModel = HomeViewModel(
            context,
            mineralRepository,
            filterPresetRepository,
            backupRepository,
            settingsRepository
        )
        advanceUntilIdle()

        viewModel.enterSelectionMode()

        // When
        viewModel.selectAll()
        advanceUntilIdle()

        // Then
        viewModel.selectedIds.test {
            val selected = awaitItem()
            assertEquals(3, selected.size)
            assertTrue(selected.containsAll(listOf("1", "2", "3")))
        }
    }

    @Test
    fun `clearSelection clears all selected ids`() = runTest {
        // Given
        viewModel.enterSelectionMode()
        viewModel.toggleSelection("mineral-1")
        viewModel.toggleSelection("mineral-2")
        advanceUntilIdle()

        // When
        viewModel.deselectAll()

        // Then
        viewModel.selectedIds.test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `selectionCount reflects number of selected minerals`() = runTest {
        // Given
        viewModel.enterSelectionMode()

        // When
        viewModel.toggleSelection("mineral-1")
        viewModel.toggleSelection("mineral-2")
        viewModel.toggleSelection("mineral-3")
        advanceUntilIdle()

        // Then
        viewModel.selectionCount.test {
            assertEquals(3, awaitItem())
        }
    }

    @Test
    fun `deleteSelected calls repository deleteByIds with selected ids`() = runTest {
        // Given
        viewModel.enterSelectionMode()
        viewModel.toggleSelection("mineral-1")
        viewModel.toggleSelection("mineral-2")
        advanceUntilIdle()

        coEvery { mineralRepository.deleteByIds(any()) } returns Unit

        // When
        viewModel.deleteSelected()
        advanceUntilIdle()

        // Then
        coVerify { mineralRepository.deleteByIds(setOf("mineral-1", "mineral-2").toList()) }
    }

    @Test
    fun `deleteSelected exits selection mode after deletion`() = runTest {
        // Given
        viewModel.enterSelectionMode()
        viewModel.toggleSelection("mineral-1")
        advanceUntilIdle()

        coEvery { mineralRepository.deleteByIds(any()) } returns Unit

        // When
        viewModel.deleteSelected()
        advanceUntilIdle()

        // Then
        viewModel.selectionMode.test {
            assertFalse(awaitItem())
        }
    }
}
