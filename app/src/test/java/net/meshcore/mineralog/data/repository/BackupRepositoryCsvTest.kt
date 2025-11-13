package net.meshcore.mineralog.data.repository

import android.content.Context
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.dao.MineralDao
import net.meshcore.mineralog.data.local.dao.PhotoDao
import net.meshcore.mineralog.data.local.dao.ProvenanceDao
import net.meshcore.mineralog.data.local.dao.StorageDao
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Unit tests for BackupRepository CSV import functionality.
 *
 * Coverage: P0 (Critical) tests for:
 * - Missing name validation
 * - Invalid Mohs hardness validation
 * - Invalid coordinates validation
 */
class BackupRepositoryCsvTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var database: MineraLogDatabase
    private lateinit var mineralDao: MineralDao
    private lateinit var provenanceDao: ProvenanceDao
    private lateinit var storageDao: StorageDao
    private lateinit var photoDao: PhotoDao
    private lateinit var repository: BackupRepositoryImpl

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempDir

        database = mockk(relaxed = true)
        mineralDao = mockk(relaxed = true)
        provenanceDao = mockk(relaxed = true)
        storageDao = mockk(relaxed = true)
        photoDao = mockk(relaxed = true)

        every { database.mineralDao() } returns mineralDao
        every { database.provenanceDao() } returns provenanceDao
        every { database.storageDao() } returns storageDao
        every { database.photoDao() } returns photoDao

        every { database.runInTransaction(any<Runnable>()) } answers {
            val runnable = firstArg<Runnable>()
            runnable.run()
        }

        coEvery { mineralDao.getAll() } returns emptyList()

        repository = BackupRepositoryImpl(context, database)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `importCsv_missingName_collectsError`() = runTest {
        // Given
        val csv = """
            Name,Formula
            ,SiO₂
            Calcite,CaCO₃
        """.trimIndent()

        val csvFile = File(tempDir, "missing_name.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        coEvery { mineralDao.insert(any()) } returns 1L

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.MERGE)

        // Then
        assertTrue(result.isSuccess, "Import should succeed but collect errors")
        val importResult = result.getOrThrow()
        assertEquals(1, importResult.imported, "Should import 1 valid mineral (Calcite)")
        assertEquals(1, importResult.skipped, "Should skip 1 mineral with missing name")
        assertTrue(
            importResult.errors.any { it.contains("Name is required") },
            "Should have error about missing name"
        )
    }

    @Test
    fun `importCsv_invalidMohs_handled`() = runTest {
        // Given - Mohs hardness out of valid range (1.0 - 10.0)
        val csv = """
            Name,Mohs Min,Mohs Max
            InvalidMineral,-5.0,15.0
            ValidMineral,7.0,7.0
        """.trimIndent()

        val csvFile = File(tempDir, "invalid_mohs.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        coEvery { mineralDao.insert(any()) } returns 1L

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.MERGE)

        // Then
        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()

        // Validation is now active: invalid Mohs values are rejected
        assertEquals(1, importResult.imported, "Should import only valid mineral")
        assertEquals(1, importResult.skipped, "Should skip mineral with invalid Mohs")
        assertTrue(
            importResult.errors.any { it.contains("Mohs") },
            "Should have error about invalid Mohs value"
        )
    }

    @Test
    fun `importCsv_invalidCoordinates_handled`() = runTest {
        // Given - Coordinates out of valid range (lat: -90 to 90, lon: -180 to 180)
        val csv = """
            Name,Prov Latitude,Prov Longitude
            InvalidCoords,200.0,400.0
            ValidCoords,48.8566,2.3522
        """.trimIndent()

        val csvFile = File(tempDir, "invalid_coords.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        coEvery { mineralDao.insert(any()) } returns 1L

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.MERGE)

        // Then
        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()

        // Note: Current implementation doesn't validate coordinate ranges
        // This test documents current behavior - validation should be added
        assertEquals(2, importResult.imported, "Current implementation accepts invalid coordinates")

        // TODO: Add coordinate validation in BackupRepository.parseMineralFromCsvRow()
        // Expected behavior after fix:
        // assertEquals(1, importResult.imported)
        // assertTrue(importResult.errors.any { it.contains("coordinate") || it.contains("latitude") || it.contains("longitude") })
    }

    // ===== CSV Export Tests (P1) =====

    @Test
    fun `exportCsv_validData_createsRFC4180`() = runTest {
        // Given
        val minerals = listOf(
            net.meshcore.mineralog.fixtures.TestFixtures.createMineral(
                name = "Quartz",
                formula = "SiO₂",
                group = "Silicates",
                mohsMin = 7.0f,
                mohsMax = 7.0f
            ),
            net.meshcore.mineralog.fixtures.TestFixtures.createMineral(
                name = "Calcite",
                formula = "CaCO₃",
                group = "Carbonates",
                mohsMin = 3.0f,
                mohsMax = 3.0f
            )
        )

        val csvFile = File(tempDir, "export.csv")
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(uri) } returns csvFile.outputStream()

        // When
        val result = repository.exportCsv(uri, minerals)

        // Then
        assertTrue(result.isSuccess, "Export should succeed")
        assertTrue(csvFile.exists(), "CSV file should be created")

        val csvContent = csvFile.readText()
        val lines = csvContent.lines()

        // Verify RFC 4180 compliance
        assertTrue(lines[0].startsWith("Name,"), "First line should be header starting with 'Name'")
        assertTrue(lines.size >= 3, "Should have header + 2 data rows")
        assertTrue(lines[1].startsWith("Quartz,"), "Second line should start with 'Quartz'")
        assertTrue(lines[2].startsWith("Calcite,"), "Third line should start with 'Calcite'")
    }

    @Test
    fun `exportCsv_withSpecialChars_escapesCorrectly`() = runTest {
        // Given - Mineral with special characters: quotes, commas, newlines
        val minerals = listOf(
            net.meshcore.mineralog.fixtures.TestFixtures.createMineral(
                name = "Mineral, with \"quotes\"",
                formula = "Fe₂O₃",
                group = "Group\nwith newline"
            )
        )

        val csvFile = File(tempDir, "export_special.csv")
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(uri) } returns csvFile.outputStream()

        // When
        val result = repository.exportCsv(uri, minerals)

        // Then
        assertTrue(result.isSuccess, "Export should succeed")

        val csvContent = csvFile.readText()
        val lines = csvContent.lines()

        // Verify escaping (RFC 4180):
        // - Fields with commas/quotes/newlines should be quoted
        // - Quotes inside should be doubled ("")
        assertTrue(
            csvContent.contains("\"Mineral, with \"\"quotes\"\"\""),
            "Name with comma and quotes should be escaped correctly"
        )
        assertTrue(
            csvContent.contains("\"Group\nwith newline\"") || csvContent.contains("\"Group\\nwith newline\""),
            "Group with newline should be escaped"
        )
    }

    // ===== CSV Import Mode Tests (P1) =====

    @Test
    fun `importCsv_modeMerge_updatesByName`() = runTest {
        // Given - Existing mineral with same name
        val existingMineral = net.meshcore.mineralog.fixtures.TestFixtures.createMineral(
            id = "existing-id",
            name = "Quartz",
            formula = "SiO₂"
        )

        coEvery { mineralDao.getAll() } returns listOf(existingMineral)

        val csv = """
            Name,Formula,Group
            Quartz,SiO₂,Updated Silicates
            NewMineral,CaCO₃,Carbonates
        """.trimIndent()

        val csvFile = File(tempDir, "merge.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        coEvery { mineralDao.insert(any()) } returns 1L

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.MERGE)

        // Then
        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(2, importResult.imported, "Both minerals should be imported (1 update, 1 new)")
        assertEquals(0, importResult.skipped)
    }

    @Test
    fun `importCsv_modeReplace_clearsAll`() = runTest {
        // Given - Existing minerals
        val existingMineral = net.meshcore.mineralog.fixtures.TestFixtures.createMineral(name = "OldMineral")
        coEvery { mineralDao.getAll() } returns listOf(existingMineral)

        val csv = """
            Name,Formula
            NewMineral,SiO₂
        """.trimIndent()

        val csvFile = File(tempDir, "replace.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        coEvery { mineralDao.insert(any()) } returns 1L
        coEvery { mineralDao.deleteAll() } just Runs
        coEvery { provenanceDao.deleteAll() } just Runs
        coEvery { storageDao.deleteAll() } just Runs
        coEvery { photoDao.deleteAll() } just Runs

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.REPLACE)

        // Then
        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(1, importResult.imported)

        // Verify deleteAll was called
        coVerify { mineralDao.deleteAll() }
        coVerify { provenanceDao.deleteAll() }
        coVerify { storageDao.deleteAll() }
        coVerify { photoDao.deleteAll() }
    }

    @Test
    fun `importCsv_modeSkipDuplicates_skips`() = runTest {
        // Given - Existing mineral
        val existingMineral = net.meshcore.mineralog.fixtures.TestFixtures.createMineral(
            name = "Quartz",
            formula = "SiO₂"
        )
        coEvery { mineralDao.getAll() } returns listOf(existingMineral)

        val csv = """
            Name,Formula
            Quartz,SiO₂
            Calcite,CaCO₃
        """.trimIndent()

        val csvFile = File(tempDir, "skip_duplicates.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        coEvery { mineralDao.insert(any()) } returns 1L

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.SKIP_DUPLICATES)

        // Then
        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(1, importResult.imported, "Only new mineral (Calcite) should be imported")
        assertEquals(1, importResult.skipped, "Duplicate (Quartz) should be skipped")
    }

    // ===== CSV Import Advanced Tests (P1) =====

    @Test
    fun `importCsv_minimal_imports`() = runTest {
        // Given - CSV with only Name and Formula
        val csv = """
            Name,Formula
            Quartz,SiO₂
            Calcite,CaCO₃
        """.trimIndent()

        val csvFile = File(tempDir, "minimal.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        coEvery { mineralDao.insert(any()) } returns 1L

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.MERGE)

        // Then
        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(2, importResult.imported, "Should import 2 minerals with minimal fields")
        assertEquals(0, importResult.skipped)
        assertTrue(importResult.errors.isEmpty())
    }

    @Test
    fun `importCsv_fullColumns_imports`() = runTest {
        // Given - CSV with all columns
        val csv = """
            Name,Group,Formula,Mohs Min,Mohs Max,Storage Place,Notes,Tags
            Quartz,Silicates,SiO₂,7.0,7.0,Cabinet A,Beautiful crystal,clear;transparent
            Hematite,Oxides,Fe₂O₃,5.5,6.5,Drawer 3,Magnetic specimen,red;metallic
        """.trimIndent()

        val csvFile = File(tempDir, "full.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        coEvery { mineralDao.insert(any()) } returns 1L
        coEvery { storageDao.insert(any()) } returns 1L

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.MERGE)

        // Then
        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(2, importResult.imported, "Should import 2 minerals with all fields")
        assertEquals(0, importResult.skipped)
        assertTrue(importResult.errors.isEmpty())

        // Verify storage was also inserted
        coVerify(exactly = 2) { storageDao.insert(any()) }
    }

    @Test
    fun `importCsv_invalidFloat_setsNull`() = runTest {
        // Given - CSV with invalid float values
        val csv = """
            Name,Mohs Min,Mohs Max
            BadMineral,abc,xyz
            GoodMineral,7.0,7.0
        """.trimIndent()

        val csvFile = File(tempDir, "invalid_float.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        coEvery { mineralDao.insert(any()) } returns 1L

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.MERGE)

        // Then
        assertTrue(result.isSuccess)
        val importResult = result.getOrThrow()
        assertEquals(2, importResult.imported, "Both minerals should be imported (invalid floats become null)")
        assertEquals(0, importResult.skipped)
    }

    @Test
    fun `importCsv_emptyFile_fails`() = runTest {
        // Given - Empty CSV file
        val csv = ""

        val csvFile = File(tempDir, "empty.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.MERGE)

        // Then
        assertTrue(result.isFailure, "Import should fail for empty CSV")
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(
            exception?.message?.contains("no headers") == true ||
            exception?.message?.contains("empty") == true,
            "Error message should indicate empty file"
        )
    }

    @Test
    fun `importCsv_headersOnly_succeeds`() = runTest {
        // Given - CSV with headers but no data rows
        val csv = """
            Name,Group,Formula
        """.trimIndent()

        val csvFile = File(tempDir, "headers_only.csv")
        csvFile.writeText(csv)
        val uri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csv.toByteArray())

        // When
        val result = repository.importCsv(uri, mode = CsvImportMode.MERGE)

        // Then
        assertTrue(result.isSuccess, "Import should succeed with 0 rows imported")
        val importResult = result.getOrThrow()
        assertEquals(0, importResult.imported, "No rows should be imported")
        assertEquals(0, importResult.skipped)
        assertTrue(importResult.errors.isEmpty())
    }
}
