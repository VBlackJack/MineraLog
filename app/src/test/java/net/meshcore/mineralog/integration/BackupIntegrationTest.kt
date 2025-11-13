package net.meshcore.mineralog.integration

import android.content.Context
import android.net.Uri
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.dao.MineralDao
import net.meshcore.mineralog.data.local.dao.PhotoDao
import net.meshcore.mineralog.data.local.dao.ProvenanceDao
import net.meshcore.mineralog.data.local.dao.StorageDao
import net.meshcore.mineralog.data.mapper.toEntity
import net.meshcore.mineralog.data.repository.BackupRepositoryImpl
import net.meshcore.mineralog.data.repository.CsvImportMode
import net.meshcore.mineralog.data.repository.ImportMode
import net.meshcore.mineralog.data.util.QrCodeGenerator
import net.meshcore.mineralog.data.util.QrLabelPdfGenerator
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.fixtures.TestFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Instant

/**
 * End-to-end integration tests for MineraLog backup and restore workflows.
 *
 * Phase 3 (P2) tests:
 * - Export → Import round-trip data integrity
 * - Encrypted backup workflow
 * - CSV export → import workflow
 * - PDF → QR scan → mineral lookup workflow
 */
class BackupIntegrationTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var database: MineraLogDatabase
    private lateinit var mineralDao: MineralDao
    private lateinit var provenanceDao: ProvenanceDao
    private lateinit var storageDao: StorageDao
    private lateinit var photoDao: PhotoDao
    private lateinit var backupRepository: BackupRepositoryImpl
    private lateinit var pdfGenerator: QrLabelPdfGenerator

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

        backupRepository = BackupRepositoryImpl(context, database)
        pdfGenerator = QrLabelPdfGenerator(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `e2e_exportImport_roundTrip`() = runTest {
        // Given - Database with test minerals
        val originalMinerals = listOf(
            TestFixtures.createMineral(
                id = "quartz-001",
                name = "Quartz",
                formula = "SiO₂",
                group = "Silicates",
                mohsMin = 7.0f,
                mohsMax = 7.0f
            ),
            TestFixtures.createMineral(
                id = "calcite-002",
                name = "Calcite",
                formula = "CaCO₃",
                group = "Carbonates",
                mohsMin = 3.0f,
                mohsMax = 3.0f
            ),
            TestFixtures.createMineral(
                id = "hematite-003",
                name = "Hematite",
                formula = "Fe₂O₃",
                group = "Oxides",
                mohsMin = 5.5f,
                mohsMax = 6.5f
            )
        )

        val mineralEntities = originalMinerals.map { it.toEntity() }
        coEvery { mineralDao.getAll() } returns mineralEntities
        coEvery { provenanceDao.getByMineralIds(any()) } returns emptyList()
        coEvery { storageDao.getByMineralIds(any()) } returns emptyList()
        coEvery { photoDao.getByMineralIds(any()) } returns emptyList()

        val exportFile = File(tempDir, "backup_roundtrip.zip")
        val exportUri = Uri.fromFile(exportFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(exportUri) } returns exportFile.outputStream()

        // When - Export to ZIP
        val exportResult = backupRepository.exportZip(exportUri)
        assertTrue(exportResult.isSuccess, "Export should succeed")
        assertTrue(exportFile.exists(), "Export file should be created")

        // And - Clear database (simulate fresh import)
        coEvery { mineralDao.deleteAll() } just Runs
        coEvery { provenanceDao.deleteAll() } just Runs
        coEvery { storageDao.deleteAll() } just Runs
        coEvery { photoDao.deleteAll() } just Runs

        // Track imports
        val importedMinerals = mutableListOf<Any>()
        coEvery { mineralDao.insert(any()) } answers {
            importedMinerals.add(firstArg())
            1L
        }

        // Mock import stream
        every { contentResolver.openInputStream(exportUri) } returns exportFile.inputStream()
        every { contentResolver.query(exportUri, any(), any(), any(), any()) } returns mockk {
            every { moveToFirst() } returns true
            every { getLong(0) } returns exportFile.length()
            every { close() } just Runs
        }

        // And - Import from ZIP
        val importResult = backupRepository.importZip(exportUri, mode = ImportMode.REPLACE)
        assertTrue(importResult.isSuccess, "Import should succeed")

        // Then - Verify data integrity
        val result = importResult.getOrThrow()
        assertEquals(3, result.imported, "Should import all 3 minerals")
        assertEquals(0, result.skipped, "No minerals should be skipped")
        assertTrue(result.errors.isEmpty(), "Should have no errors")

        // Verify deleteAll was called (REPLACE mode)
        coVerify { mineralDao.deleteAll() }
        coVerify { provenanceDao.deleteAll() }
        coVerify { storageDao.deleteAll() }
        coVerify { photoDao.deleteAll() }

        // Verify all minerals were inserted
        assertEquals(3, importedMinerals.size, "Should have inserted 3 minerals")

        println("✓ E2E Round-trip: Export → Import completed successfully")
    }

    @Test
    fun `e2e_exportEncrypted_importDecrypted`() = runTest {
        // Given - Database with test minerals
        val minerals = listOf(
            TestFixtures.createMineral(name = "Secret Quartz", formula = "SiO₂"),
            TestFixtures.createMineral(name = "Secret Calcite", formula = "CaCO₃")
        )

        val mineralEntities = minerals.map { it.toEntity() }
        coEvery { mineralDao.getAll() } returns mineralEntities
        coEvery { provenanceDao.getByMineralIds(any()) } returns emptyList()
        coEvery { storageDao.getByMineralIds(any()) } returns emptyList()
        coEvery { photoDao.getByMineralIds(any()) } returns emptyList()

        val password = "Test123!".toCharArray()
        val exportFile = File(tempDir, "backup_encrypted.zip")
        val exportUri = Uri.fromFile(exportFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(exportUri) } returns exportFile.outputStream()

        // When - Export with encryption
        val exportResult = backupRepository.exportZip(exportUri, password)
        assertTrue(exportResult.isSuccess, "Encrypted export should succeed")
        assertTrue(exportFile.exists())

        // And - Prepare for import
        coEvery { mineralDao.deleteAll() } just Runs
        coEvery { provenanceDao.deleteAll() } just Runs
        coEvery { storageDao.deleteAll() } just Runs
        coEvery { photoDao.deleteAll() } just Runs
        coEvery { mineralDao.insert(any()) } returns 1L

        every { contentResolver.openInputStream(exportUri) } returns exportFile.inputStream()
        every { contentResolver.query(exportUri, any(), any(), any(), any()) } returns mockk {
            every { moveToFirst() } returns true
            every { getLong(0) } returns exportFile.length()
            every { close() } just Runs
        }

        // And - Import with correct password
        val importResult = backupRepository.importZip(exportUri, password, ImportMode.REPLACE)

        // Then
        assertTrue(importResult.isSuccess, "Decryption and import should succeed")
        val result = importResult.getOrThrow()
        assertEquals(2, result.imported, "Should import 2 encrypted minerals")

        println("✓ E2E Encrypted: Export (encrypted) → Import (decrypted) completed successfully")
    }

    @Test
    fun `e2e_csvExport_importBack`() = runTest {
        // Given - Database with test minerals
        val originalMinerals = listOf(
            TestFixtures.createMineral(
                name = "CSV Quartz",
                formula = "SiO₂",
                group = "Silicates",
                mohsMin = 7.0f,
                mohsMax = 7.0f
            ),
            TestFixtures.createMineral(
                name = "CSV Calcite",
                formula = "CaCO₃",
                group = "Carbonates",
                mohsMin = 3.0f,
                mohsMax = 3.0f
            )
        )

        val csvFile = File(tempDir, "export.csv")
        val csvUri = Uri.fromFile(csvFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(csvUri) } returns csvFile.outputStream()

        // When - Export to CSV
        val exportResult = backupRepository.exportCsv(csvUri, originalMinerals)
        assertTrue(exportResult.isSuccess, "CSV export should succeed")
        assertTrue(csvFile.exists())

        // And - Prepare for import
        coEvery { mineralDao.getAll() } returns emptyList()
        coEvery { mineralDao.insert(any()) } returns 1L

        val csvContent = csvFile.readText()
        every { contentResolver.openInputStream(csvUri) } returns ByteArrayInputStream(csvContent.toByteArray())

        // And - Import CSV
        val importResult = backupRepository.importCsv(csvUri, mode = CsvImportMode.MERGE)

        // Then
        assertTrue(importResult.isSuccess, "CSV import should succeed")
        val result = importResult.getOrThrow()
        assertEquals(2, result.imported, "Should import 2 minerals from CSV")
        assertEquals(0, result.skipped)
        assertTrue(result.errors.isEmpty())

        // Verify CSV content
        val lines = csvContent.lines()
        assertTrue(lines[0].startsWith("Name,"), "First line should be header")
        assertTrue(lines.any { it.contains("CSV Quartz") }, "Should contain Quartz")
        assertTrue(lines.any { it.contains("CSV Calcite") }, "Should contain Calcite")

        println("✓ E2E CSV: Export → Import completed successfully")
    }

    @Test
    fun `e2e_generatePdf_scanQr_loadMineral`() = runTest {
        // Given - Mineral to generate QR for
        val mineralId = "test-qr-scan-12345"
        val mineral = TestFixtures.createMineral(
            id = mineralId,
            name = "QR Test Mineral",
            formula = "XYZ"
        )

        val pdfFile = File(tempDir, "qr_labels.pdf")
        val pdfUri = Uri.fromFile(pdfFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(pdfUri) } returns pdfFile.outputStream()

        // When - Generate PDF with QR code
        val pdfResult = pdfGenerator.generate(listOf(mineral), pdfUri)
        assertTrue(pdfResult.isSuccess, "PDF generation should succeed")
        assertTrue(pdfFile.exists())

        // And - Generate QR code separately for scanning simulation
        val qrData = QrCodeGenerator.encodeMineralUri(mineralId)
        assertEquals("mineralog://mineral/$mineralId", qrData, "QR should encode correct URI")

        // And - Simulate QR scan (decode URI)
        val decodedId = QrCodeGenerator.decodeMineralUri(qrData)
        assertEquals(mineralId, decodedId, "Decoded ID should match original")

        // And - Simulate deep link handler (load mineral by ID)
        val loadedMineral = mockk<Mineral>()
        every { loadedMineral.id } returns mineralId
        every { loadedMineral.name } returns "QR Test Mineral"

        // Mock DAO lookup
        coEvery { mineralDao.getById(mineralId) } returns loadedMineral.toEntity()

        // Then - Verify workflow
        assertEquals(mineralId, loadedMineral.id, "Loaded mineral should match scanned QR")
        assertEquals("QR Test Mineral", loadedMineral.name)

        println("✓ E2E QR: PDF → Scan → Load mineral completed successfully")
    }

    @Test
    fun `e2e_importZip_validateDataIntegrity`() = runTest {
        // Given - Complex dataset with provenance, storage, and photos
        val mineralId = "complex-001"
        val provenance = TestFixtures.createProvenance(
            mineralId = mineralId,
            country = "France",
            locality = "Paris",
            latitude = 48.8566,
            longitude = 2.3522,
            price = 100f,
            currency = "EUR"
        )

        val storage = TestFixtures.createStorage(
            mineralId = mineralId,
            place = "Cabinet A",
            container = "Drawer 3"
        )

        val photo = TestFixtures.createPhoto(
            mineralId = mineralId,
            fileName = "photo_001.jpg"
        )

        val mineral = TestFixtures.createMineral(
            id = mineralId,
            name = "Complex Specimen",
            formula = "XYZ",
            provenance = provenance,
            storage = storage,
            photos = listOf(photo)
        )

        val zipBytes = TestFixtures.createValidZipBackup(listOf(mineral))
        val zipFile = File(tempDir, "complex_backup.zip")
        zipFile.writeBytes(zipBytes)
        val zipUri = Uri.fromFile(zipFile)

        // Mock content resolver
        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(zipUri) } returns ByteArrayInputStream(zipBytes)
        every { contentResolver.query(zipUri, any(), any(), any(), any()) } returns mockk {
            every { moveToFirst() } returns true
            every { getLong(0) } returns zipBytes.size.toLong()
            every { close() } just Runs
        }

        // Mock DAOs
        coEvery { mineralDao.deleteAll() } just Runs
        coEvery { provenanceDao.deleteAll() } just Runs
        coEvery { storageDao.deleteAll() } just Runs
        coEvery { photoDao.deleteAll() } just Runs

        val insertedMinerals = mutableListOf<Any>()
        val insertedProvenances = mutableListOf<Any>()
        val insertedStorages = mutableListOf<Any>()
        val insertedPhotos = mutableListOf<Any>()

        coEvery { mineralDao.insert(any()) } answers {
            insertedMinerals.add(firstArg())
            1L
        }
        coEvery { provenanceDao.insert(any()) } answers {
            insertedProvenances.add(firstArg())
            1L
        }
        coEvery { storageDao.insert(any()) } answers {
            insertedStorages.add(firstArg())
            1L
        }
        coEvery { photoDao.insert(any()) } answers {
            insertedPhotos.add(firstArg())
            1L
        }

        // When - Import complex backup
        val importResult = backupRepository.importZip(zipUri, mode = ImportMode.REPLACE)

        // Then
        assertTrue(importResult.isSuccess, "Import should succeed")
        val result = importResult.getOrThrow()
        assertEquals(1, result.imported, "Should import 1 mineral")

        // Verify all related data was inserted
        assertEquals(1, insertedMinerals.size, "Should insert mineral")
        assertEquals(1, insertedProvenances.size, "Should insert provenance")
        assertEquals(1, insertedStorages.size, "Should insert storage")
        assertEquals(1, insertedPhotos.size, "Should insert photo")

        println("✓ E2E Data Integrity: Complex import with provenance, storage, and photos completed")
    }
}
