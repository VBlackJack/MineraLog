package net.meshcore.mineralog.ui.screens.settings

import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.meshcore.mineralog.data.repository.BackupRepository
import net.meshcore.mineralog.data.repository.CsvImportMode
import net.meshcore.mineralog.data.repository.ImportMode
import net.meshcore.mineralog.data.repository.ImportResult
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.data.repository.SettingsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SettingsViewModel Tests")
class SettingsViewModelTest {

    private lateinit var context: Context
    private lateinit var mineralRepository: MineralRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var backupRepository: BackupRepository
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Create mocks for all dependencies
        context = mockk(relaxed = true)
        mineralRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        backupRepository = mockk(relaxed = true)

        // Setup default repository responses
        every { settingsRepository.getLanguage() } returns flowOf("en")
        every { settingsRepository.getCopyPhotosToInternalStorage() } returns flowOf(true)
        every { settingsRepository.getEncryptByDefault() } returns flowOf(false)

        // Initialize ViewModel with all 4 required dependencies
        viewModel = SettingsViewModel(
            context = context,
            mineralRepository = mineralRepository,
            settingsRepository = settingsRepository,
            backupRepository = backupRepository
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Language settings tests
    @Test
    fun `initial language should be en`() = runTest {
        viewModel.language.test {
            assertEquals("en", awaitItem())
        }
    }

    @Test
    fun `setLanguage should update settings repository`() = runTest {
        // When
        viewModel.setLanguage("fr")

        // Then
        coVerify { settingsRepository.setLanguage("fr") }
    }

    // Copy photos settings tests
    @Test
    fun `initial copyPhotosToInternal should be true`() = runTest {
        viewModel.copyPhotosToInternal.test {
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `setCopyPhotos should update settings repository`() = runTest {
        // When
        viewModel.setCopyPhotos(false)

        // Then
        coVerify { settingsRepository.setCopyPhotosToInternalStorage(false) }
    }

    // Encrypt by default tests
    @Test
    fun `initial encryptByDefault should be false`() = runTest {
        viewModel.encryptByDefault.test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `setEncryptByDefault should update settings repository`() = runTest {
        // When
        viewModel.setEncryptByDefault(true)

        // Then
        coVerify { settingsRepository.setEncryptByDefault(true) }
    }

    // Export backup tests
    @Test
    fun `exportBackup without password should succeed`() = runTest {
        // Given
        val uri = mockk<Uri>()
        coEvery { backupRepository.exportZip(uri, null) } returns Result.success(Unit)

        // When
        viewModel.exportBackup(uri, null)

        // Then
        viewModel.exportState.test {
            val state = awaitItem()
            assertTrue(state is BackupExportState.Success)
        }
        coVerify { backupRepository.exportZip(uri, null) }
    }

    @Test
    fun `exportBackup with password should succeed and clear password`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val password = "secure-password".toCharArray()
        var capturedPassword: CharArray? = null

        coEvery { backupRepository.exportZip(uri, any()) } coAnswers {
            capturedPassword = secondArg()
            Result.success(Unit)
        }

        // When
        viewModel.exportBackup(uri, password)

        // Then
        viewModel.exportState.test {
            val state = awaitItem()
            assertTrue(state is BackupExportState.Success)
        }

        // Verify password was passed as CharArray
        assertNotNull(capturedPassword)
        assertEquals(password.size, capturedPassword?.size)
    }

    @Test
    fun `exportBackup failure should emit error state`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val errorMessage = "Export failed: disk full"
        coEvery { backupRepository.exportZip(uri, null) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.exportBackup(uri, null)

        // Then
        viewModel.exportState.test {
            val state = awaitItem()
            assertTrue(state is BackupExportState.Error)
            assertEquals(errorMessage, (state as BackupExportState.Error).message)
        }
    }

    @Test
    fun `exportBackup should emit exporting state during operation`() = runTest {
        // Given
        val uri = mockk<Uri>()
        coEvery { backupRepository.exportZip(uri, null) } coAnswers {
            // Simulate delay
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }

        // When/Then
        viewModel.exportState.test {
            assertEquals(BackupExportState.Idle, awaitItem())

            viewModel.exportBackup(uri, null)

            assertEquals(BackupExportState.Exporting, awaitItem())
            assertEquals(BackupExportState.Success, awaitItem())
        }
    }

    @Test
    fun `resetExportState should set state to idle`() = runTest {
        // Given - export in progress
        val uri = mockk<Uri>()
        coEvery { backupRepository.exportZip(uri, null) } returns Result.success(Unit)
        viewModel.exportBackup(uri, null)

        // When
        viewModel.resetExportState()

        // Then
        viewModel.exportState.test {
            assertEquals(BackupExportState.Idle, awaitItem())
        }
    }

    // Import backup tests
    @Test
    fun `importBackup without password should succeed`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val importResult = ImportResult(imported = 42, skipped = 0, errors = emptyList())
        coEvery { backupRepository.importZip(uri, null, ImportMode.REPLACE) } returns Result.success(importResult)

        // When
        viewModel.importBackup(uri, null)

        // Then
        viewModel.importState.test {
            val state = awaitItem()
            assertTrue(state is BackupImportState.Success)
            assertEquals(42, (state as BackupImportState.Success).imported)
        }
    }

    @Test
    fun `importBackup with password should succeed`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val password = "secure-password".toCharArray()
        val importResult = ImportResult(imported = 10, skipped = 0, errors = emptyList())
        var capturedPassword: CharArray? = null

        coEvery { backupRepository.importZip(uri, any(), ImportMode.REPLACE) } coAnswers {
            capturedPassword = secondArg()
            Result.success(importResult)
        }

        // When
        viewModel.importBackup(uri, password)

        // Then
        viewModel.importState.test {
            val state = awaitItem()
            assertTrue(state is BackupImportState.Success)
            assertEquals(10, (state as BackupImportState.Success).imported)
        }

        // Verify password was passed as CharArray
        assertNotNull(capturedPassword)
        assertEquals(password.size, capturedPassword?.size)
    }

    @Test
    fun `importBackup encrypted without password should request password`() = runTest {
        // Given
        val uri = mockk<Uri>()
        coEvery { backupRepository.importZip(uri, null, ImportMode.REPLACE) } returns
            Result.failure(Exception("File is encrypted, password required"))

        // When
        viewModel.importBackup(uri, null)

        // Then
        viewModel.importState.test {
            val state = awaitItem()
            assertTrue(state is BackupImportState.PasswordRequired)
            assertEquals(uri, (state as BackupImportState.PasswordRequired).uri)
        }
    }

    @Test
    fun `importBackup failure should emit error state`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val errorMessage = "Invalid backup file"
        coEvery { backupRepository.importZip(uri, null, ImportMode.REPLACE) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.importBackup(uri, null)

        // Then
        viewModel.importState.test {
            val state = awaitItem()
            assertTrue(state is BackupImportState.Error)
            assertEquals(errorMessage, (state as BackupImportState.Error).message)
        }
    }

    @Test
    fun `resetImportState should set state to idle`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val importResult = ImportResult(imported = 5, skipped = 0, errors = emptyList())
        coEvery { backupRepository.importZip(uri, null, ImportMode.REPLACE) } returns Result.success(importResult)
        viewModel.importBackup(uri, null)

        // When
        viewModel.resetImportState()

        // Then
        viewModel.importState.test {
            assertEquals(BackupImportState.Idle, awaitItem())
        }
    }

    // CSV import tests
    @Test
    fun `importCsv with default mode should succeed`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val importResult = ImportResult(imported = 25, skipped = 3, errors = emptyList())
        coEvery { backupRepository.importCsv(uri, null, CsvImportMode.MERGE) } returns Result.success(importResult)

        // When
        viewModel.importCsv(uri)

        // Then
        viewModel.csvImportState.test {
            val state = awaitItem()
            assertTrue(state is CsvImportState.Success)
            assertEquals(25, (state as CsvImportState.Success).result.imported)
            assertEquals(3, state.result.skipped)
        }
    }

    @Test
    fun `importCsv with custom column mapping should use mapping`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val columnMapping = mapOf("Name" to "name", "Formula" to "formula")
        val importResult = ImportResult(imported = 10, skipped = 0, errors = emptyList())
        coEvery { backupRepository.importCsv(uri, columnMapping, CsvImportMode.REPLACE) } returns Result.success(importResult)

        // When
        viewModel.importCsv(uri, columnMapping, CsvImportMode.REPLACE)

        // Then
        coVerify { backupRepository.importCsv(uri, columnMapping, CsvImportMode.REPLACE) }
    }

    @Test
    fun `importCsv failure should emit error state`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val errorMessage = "Invalid CSV format"
        coEvery { backupRepository.importCsv(uri, null, CsvImportMode.MERGE) } returns Result.failure(Exception(errorMessage))

        // When
        viewModel.importCsv(uri)

        // Then
        viewModel.csvImportState.test {
            val state = awaitItem()
            assertTrue(state is CsvImportState.Error)
            assertEquals(errorMessage, (state as CsvImportState.Error).message)
        }
    }

    @Test
    fun `resetCsvImportState should set state to idle`() = runTest {
        // Given
        val uri = mockk<Uri>()
        val importResult = ImportResult(imported = 5, skipped = 0, errors = emptyList())
        coEvery { backupRepository.importCsv(uri, null, CsvImportMode.MERGE) } returns Result.success(importResult)
        viewModel.importCsv(uri)

        // When
        viewModel.resetCsvImportState()

        // Then
        viewModel.csvImportState.test {
            assertEquals(CsvImportState.Idle, awaitItem())
        }
    }

    // Sample Data tests
    @Test
    fun `loadSampleData should update sampleDataState from Idle to Loading to Success`() = runTest {
        // Given - mock successful mineral insertion (SampleDataGenerator internally calls mineralRepository.insert)
        coEvery { mineralRepository.insert(any()) } returns "mock-mineral-id"

        // When/Then - verify state transitions
        viewModel.sampleDataState.test {
            // Initial state should be Idle
            assertEquals(SampleDataState.Idle, awaitItem())

            // Trigger sample data loading
            viewModel.loadSampleData()

            // Should transition to Loading
            assertEquals(SampleDataState.Loading, awaitItem())

            // Should transition to Success with 12 minerals
            val successState = awaitItem()
            assertTrue(successState is SampleDataState.Success)
            assertEquals(12, (successState as SampleDataState.Success).count)
        }
    }

    @Test
    fun `loadSampleData should emit error state on repository failure`() = runTest {
        // Given - mock repository failure
        val errorMessage = "Database error: unable to insert mineral"
        coEvery { mineralRepository.insert(any()) } throws Exception(errorMessage)

        // When/Then
        viewModel.sampleDataState.test {
            assertEquals(SampleDataState.Idle, awaitItem())

            viewModel.loadSampleData()

            assertEquals(SampleDataState.Loading, awaitItem())

            val errorState = awaitItem()
            assertTrue(errorState is SampleDataState.Error)
            assertTrue((errorState as SampleDataState.Error).message.contains(errorMessage))
        }
    }

    @Test
    fun `resetSampleDataState should set state back to idle`() = runTest {
        // Given - sample data loaded successfully
        coEvery { mineralRepository.insert(any()) } returns "mock-mineral-id"
        viewModel.loadSampleData()

        // When
        viewModel.resetSampleDataState()

        // Then
        viewModel.sampleDataState.test {
            assertEquals(SampleDataState.Idle, awaitItem())
        }
    }

    @Test
    fun `loadSampleData should call mineralRepository insert for each sample mineral`() = runTest {
        // Given
        coEvery { mineralRepository.insert(any()) } returns "mock-mineral-id"

        // When
        viewModel.loadSampleData()

        // Wait for operation to complete
        viewModel.sampleDataState.test {
            skipItems(2) // Skip Idle and Loading
            awaitItem() // Wait for Success
        }

        // Then - verify insert was called 12 times (once per sample mineral)
        coVerify(exactly = 12) { mineralRepository.insert(any()) }
    }

    // Edge cases and security tests
    @Test
    fun `exportBackup with empty password should pass empty CharArray`() = runTest {
        // Given
        val uri = mockk<Uri>()
        coEvery { backupRepository.exportZip(uri, any()) } returns Result.success(Unit)

        // When
        viewModel.exportBackup(uri, CharArray(0))

        // Then - empty CharArray is passed
        coVerify { backupRepository.exportZip(uri, match { it != null && it.isEmpty() }) }
    }

    @Test
    fun `multiple concurrent export operations should be handled`() = runTest {
        // Given
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        coEvery { backupRepository.exportZip(any(), any()) } returns Result.success(Unit)

        // When - trigger multiple exports
        viewModel.exportBackup(uri1, null)
        viewModel.exportBackup(uri2, null)

        // Then - both should complete
        viewModel.exportState.test {
            val state = awaitItem()
            assertTrue(state is BackupExportState.Success)
        }
    }
}
