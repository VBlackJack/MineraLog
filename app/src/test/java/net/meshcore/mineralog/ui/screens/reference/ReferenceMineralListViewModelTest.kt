package net.meshcore.mineralog.ui.screens.reference

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
 * Unit tests for ReferenceMineralListViewModel.
 *
 * Tests cover:
 * - Search functionality (query changes, clear search)
 * - User-defined filter toggle
 * - Counts loading (total, user-defined)
 * - Search result handling
 * - Refresh operation
 * - isSearching computed property
 *
 * Sprint 3: ViewModel Tests - Target 70%+ coverage
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceMineralListViewModelTest {

    private lateinit var referenceMineralRepository: ReferenceMineralRepository
    private lateinit var viewModel: ReferenceMineralListViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        referenceMineralRepository = mockk(relaxed = true)

        // Setup default returns
        coEvery { referenceMineralRepository.count() } returns 100
        coEvery { referenceMineralRepository.countUserDefined() } returns 25
        every { referenceMineralRepository.getDistinctGroups() } returns flowOf(emptyList())
        every { referenceMineralRepository.getDistinctCrystalSystems() } returns flowOf(emptyList())
        every { referenceMineralRepository.filterPaged(any()) } returns mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== INITIALIZATION TESTS ==========

    @Test
    @DisplayName("init - loads counts successfully")
    fun `init - counts loading - total and user-defined`() = runTest {
        // Arrange & Act
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.totalCount.test {
            assertEquals(100, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.userDefinedCount.test {
            assertEquals(25, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { referenceMineralRepository.count() }
        coVerify(exactly = 1) { referenceMineralRepository.countUserDefined() }
    }

    @Test
    @DisplayName("init - initializes with empty search")
    fun `init - search query - empty initially`() = runTest {
        // Arrange & Act
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.searchQuery.test {
            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.isSearching)
    }

    @Test
    @DisplayName("init - user-defined filter off by default")
    fun `init - user-defined filter - false initially`() = runTest {
        // Arrange & Act
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.showOnlyUserDefined.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== SEARCH TESTS ==========

    @Test
    @DisplayName("onSearchQueryChange - updates query")
    fun `onSearchQueryChange - query update - search performed`() = runTest {
        // Arrange
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        val searchResults = listOf(
            createTestReferencMineral("1", "Quartz"),
            createTestReferencMineral("2", "Quartzite")
        )
        every { referenceMineralRepository.searchByName("quartz") } returns flowOf(searchResults)

        // Act
        viewModel.onSearchQueryChange("quartz")
        advanceUntilIdle()

        // Assert
        viewModel.searchQuery.test {
            assertEquals("quartz", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(viewModel.isSearching)

        viewModel.searchResults.test {
            val results = awaitItem()
            assertEquals(2, results.size)
            assertEquals("Quartz", results[0].name)
            assertEquals("Quartzite", results[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("onSearchQueryChange - empty query clears results")
    fun `onSearchQueryChange - empty query - clears search results`() = runTest {
        // Arrange
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        val searchResults = listOf(createTestReferencMineral("1", "Quartz"))
        every { referenceMineralRepository.searchByName("quartz") } returns flowOf(searchResults)

        // Search first
        viewModel.onSearchQueryChange("quartz")
        advanceUntilIdle()

        // Act - Clear by setting empty query
        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        // Assert
        viewModel.searchQuery.test {
            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.isSearching)

        viewModel.searchResults.test {
            val results = awaitItem()
            assertTrue(results.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("clearSearch - resets query and results")
    fun `clearSearch - query and results - reset to empty`() = runTest {
        // Arrange
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        val searchResults = listOf(createTestReferencMineral("1", "Quartz"))
        every { referenceMineralRepository.searchByName("quartz") } returns flowOf(searchResults)

        viewModel.onSearchQueryChange("quartz")
        advanceUntilIdle()

        // Act
        viewModel.clearSearch()

        // Assert
        viewModel.searchQuery.test {
            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.searchResults.test {
            val results = awaitItem()
            assertTrue(results.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        assertFalse(viewModel.isSearching)
    }

    @Test
    @DisplayName("onSearchQueryChange - no results - empty list")
    fun `onSearchQueryChange - no matches - empty search results`() = runTest {
        // Arrange
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        every { referenceMineralRepository.searchByName("xyz123") } returns flowOf(emptyList())

        // Act
        viewModel.onSearchQueryChange("xyz123")
        advanceUntilIdle()

        // Assert
        viewModel.searchResults.test {
            val results = awaitItem()
            assertTrue(results.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(viewModel.isSearching) // Still searching, just no results
    }

    // ========== USER-DEFINED FILTER TESTS ==========

    @Test
    @DisplayName("toggleUserDefinedFilter - toggles state")
    fun `toggleUserDefinedFilter - state toggle - false to true to false`() = runTest {
        // Arrange
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        // Act & Assert - First toggle (false -> true)
        viewModel.toggleUserDefinedFilter()

        viewModel.showOnlyUserDefined.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Act & Assert - Second toggle (true -> false)
        viewModel.toggleUserDefinedFilter()

        viewModel.showOnlyUserDefined.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("toggleUserDefinedFilter - multiple toggles")
    fun `toggleUserDefinedFilter - multiple toggles - alternates correctly`() = runTest {
        // Arrange
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        // Act
        viewModel.toggleUserDefinedFilter() // -> true
        viewModel.toggleUserDefinedFilter() // -> false
        viewModel.toggleUserDefinedFilter() // -> true

        // Assert
        viewModel.showOnlyUserDefined.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== REFRESH TESTS ==========

    @Test
    @DisplayName("refresh - reloads counts")
    fun `refresh - counts reload - repository called again`() = runTest {
        // Arrange
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        // Update repository to return new counts
        coEvery { referenceMineralRepository.count() } returns 150
        coEvery { referenceMineralRepository.countUserDefined() } returns 40

        // Act
        viewModel.refresh()
        advanceUntilIdle()

        // Assert
        viewModel.totalCount.test {
            assertEquals(150, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.userDefinedCount.test {
            assertEquals(40, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Should be called twice: once in init, once in refresh
        coVerify(exactly = 2) { referenceMineralRepository.count() }
        coVerify(exactly = 2) { referenceMineralRepository.countUserDefined() }
    }

    // ========== COUNTS LOADING TESTS ==========

    @Test
    @DisplayName("init - repository exception - counts remain 0")
    fun `init - repository error - counts default to zero`() = runTest {
        // Arrange
        coEvery { referenceMineralRepository.count() } throws Exception("Database error")
        coEvery { referenceMineralRepository.countUserDefined() } throws Exception("Database error")

        // Act
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        // Assert - Should remain at default 0 if exception occurs
        viewModel.totalCount.test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.userDefinedCount.test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - zero counts - handled correctly")
    fun `init - zero counts - repository returns zero`() = runTest {
        // Arrange
        coEvery { referenceMineralRepository.count() } returns 0
        coEvery { referenceMineralRepository.countUserDefined() } returns 0

        // Act
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.totalCount.test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.userDefinedCount.test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== IS SEARCHING PROPERTY TESTS ==========

    @Test
    @DisplayName("isSearching - false when query is empty")
    fun `isSearching - empty query - returns false`() = runTest {
        // Arrange
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        // Assert
        assertFalse(viewModel.isSearching)
    }

    @Test
    @DisplayName("isSearching - true when query is not empty")
    fun `isSearching - non-empty query - returns true`() = runTest {
        // Arrange
        viewModel = ReferenceMineralListViewModel(referenceMineralRepository)
        advanceUntilIdle()

        every { referenceMineralRepository.searchByName(any()) } returns flowOf(emptyList())

        // Act
        viewModel.onSearchQueryChange("test")
        advanceUntilIdle()

        // Assert
        assertTrue(viewModel.isSearching)
    }

    // ========== HELPER METHODS ==========

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
