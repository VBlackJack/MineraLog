package net.meshcore.mineralog.ui.screens.main

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import net.meshcore.mineralog.data.migration.AutoReferenceCreator
import net.meshcore.mineralog.data.migration.MigrationReport
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for MigrationViewModel.
 *
 * Tests cover:
 * - Migration check and execution (needed, not needed)
 * - Migration report generation and display
 * - Dialog state management
 * - Error handling during migration
 * - Report retrieval
 *
 * Sprint 3: ViewModel Tests - Target 70%+ coverage
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MigrationViewModelTest {

    private lateinit var autoReferenceCreator: AutoReferenceCreator
    private lateinit var viewModel: MigrationViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        autoReferenceCreator = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== MIGRATION CHECK TESTS ==========

    @Test
    @DisplayName("checkAndRunMigration - migration needed - runs and shows report")
    fun `checkAndRunMigration - needed - migration runs and dialog shown`() = runTest {
        // Arrange
        val report = MigrationReport(
            referencesCreated = 10,
            simpleSpecimensLinked = 5,
            componentsLinked = 3,
            divergentMinerals = emptyList()
        )
        every { autoReferenceCreator.isMigrationDone() } returns false
        coEvery { autoReferenceCreator.run() } returns report

        viewModel = MigrationViewModel(autoReferenceCreator)

        // Act
        viewModel.checkAndRunMigration()
        advanceUntilIdle()

        // Assert
        viewModel.migrationReport.test {
            val reportResult = awaitItem()
            assertNotNull(reportResult)
            assertEquals(10, reportResult?.referencesCreated)
            assertEquals(5, reportResult?.simpleSpecimensLinked)
            assertEquals(3, reportResult?.componentsLinked)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.showMigrationDialog.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { autoReferenceCreator.run() }
    }

    @Test
    @DisplayName("checkAndRunMigration - already done - does not run")
    fun `checkAndRunMigration - already done - migration not run`() = runTest {
        // Arrange
        every { autoReferenceCreator.isMigrationDone() } returns true

        viewModel = MigrationViewModel(autoReferenceCreator)

        // Act
        viewModel.checkAndRunMigration()
        advanceUntilIdle()

        // Assert
        viewModel.migrationReport.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.showMigrationDialog.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { autoReferenceCreator.run() }
    }

    @Test
    @DisplayName("checkAndRunMigration - no changes - does not show dialog")
    fun `checkAndRunMigration - no changes - dialog not shown`() = runTest {
        // Arrange
        val emptyReport = MigrationReport(
            referencesCreated = 0,
            simpleSpecimensLinked = 0,
            componentsLinked = 0,
            divergentMinerals = emptyList()
        )
        every { autoReferenceCreator.isMigrationDone() } returns false
        coEvery { autoReferenceCreator.run() } returns emptyReport

        viewModel = MigrationViewModel(autoReferenceCreator)

        // Act
        viewModel.checkAndRunMigration()
        advanceUntilIdle()

        // Assert - Report is set but dialog is not shown
        viewModel.showMigrationDialog.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("checkAndRunMigration - divergent minerals - shows dialog")
    fun `checkAndRunMigration - divergent minerals - dialog shown`() = runTest {
        // Arrange
        val reportWithDivergent = MigrationReport(
            referencesCreated = 0,
            simpleSpecimensLinked = 0,
            componentsLinked = 0,
            divergentMinerals = listOf("Mineral1", "Mineral2")
        )
        every { autoReferenceCreator.isMigrationDone() } returns false
        coEvery { autoReferenceCreator.run() } returns reportWithDivergent

        viewModel = MigrationViewModel(autoReferenceCreator)

        // Act
        viewModel.checkAndRunMigration()
        advanceUntilIdle()

        // Assert
        viewModel.showMigrationDialog.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.migrationReport.test {
            val report = awaitItem()
            assertEquals(2, report?.divergentMinerals?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @DisplayName("checkAndRunMigration - exception - does not crash")
    fun `checkAndRunMigration - migration error - handled gracefully`() = runTest {
        // Arrange
        every { autoReferenceCreator.isMigrationDone() } returns false
        coEvery { autoReferenceCreator.run() } throws Exception("Migration failed")

        viewModel = MigrationViewModel(autoReferenceCreator)

        // Act & Assert - Should not throw
        assertDoesNotThrow {
            viewModel.checkAndRunMigration()
            advanceUntilIdle()
        }

        // Dialog should not be shown
        viewModel.showMigrationDialog.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Report should be null
        viewModel.migrationReport.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== DIALOG MANAGEMENT TESTS ==========

    @Test
    @DisplayName("dismissMigrationDialog - hides dialog")
    fun `dismissMigrationDialog - dialog state - set to false`() = runTest {
        // Arrange
        val report = MigrationReport(
            referencesCreated = 5,
            simpleSpecimensLinked = 0,
            componentsLinked = 0,
            divergentMinerals = emptyList()
        )
        every { autoReferenceCreator.isMigrationDone() } returns false
        coEvery { autoReferenceCreator.run() } returns report

        viewModel = MigrationViewModel(autoReferenceCreator)
        viewModel.checkAndRunMigration()
        advanceUntilIdle()

        // Act
        viewModel.dismissMigrationDialog()

        // Assert
        viewModel.showMigrationDialog.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("getMigrationReport - returns current report")
    fun `getMigrationReport - report retrieval - returns correct report`() = runTest {
        // Arrange
        val report = MigrationReport(
            referencesCreated = 7,
            simpleSpecimensLinked = 2,
            componentsLinked = 1,
            divergentMinerals = listOf("Mineral1")
        )
        every { autoReferenceCreator.isMigrationDone() } returns false
        coEvery { autoReferenceCreator.run() } returns report

        viewModel = MigrationViewModel(autoReferenceCreator)
        viewModel.checkAndRunMigration()
        advanceUntilIdle()

        // Act
        val retrievedReport = viewModel.getMigrationReport()

        // Assert
        assertNotNull(retrievedReport)
        assertEquals(7, retrievedReport?.referencesCreated)
        assertEquals(2, retrievedReport?.simpleSpecimensLinked)
        assertEquals(1, retrievedReport?.componentsLinked)
        assertEquals(1, retrievedReport?.divergentMinerals?.size)
    }

    @Test
    @DisplayName("getMigrationReport - no migration - returns null")
    fun `getMigrationReport - no migration run - returns null`() = runTest {
        // Arrange
        every { autoReferenceCreator.isMigrationDone() } returns true

        viewModel = MigrationViewModel(autoReferenceCreator)

        // Act
        val report = viewModel.getMigrationReport()

        // Assert
        assertNull(report)
    }

    // ========== INITIAL STATE TESTS ==========

    @Test
    @DisplayName("init - default state - report null and dialog hidden")
    fun `init - initial state - report null and dialog false`() = runTest {
        // Arrange & Act
        viewModel = MigrationViewModel(autoReferenceCreator)

        // Assert
        viewModel.migrationReport.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.showMigrationDialog.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
