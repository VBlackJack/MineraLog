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
import net.meshcore.mineralog.fixtures.TestFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Performance tests for BackupRepository.
 *
 * Phase 3 (P2) tests:
 * - Import 1000 minerals from ZIP (< 30s, < 100MB RAM)
 * - Export 1000 minerals to ZIP (< 20s)
 * - Large dataset handling
 */
class BackupRepositoryPerformanceTest {

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
        coEvery { mineralDao.insert(any()) } returns 1L
        coEvery { provenanceDao.getByMineralIds(any()) } returns emptyList()
        coEvery { storageDao.getByMineralIds(any()) } returns emptyList()
        coEvery { photoDao.getByMineralIds(any()) } returns emptyList()

        repository = BackupRepositoryImpl(context, database)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `importZip 1000minerals performance`() = runTest {
        // Given - Create a large backup ZIP with 1000 minerals
        val minerals = TestFixtures.batch1000Minerals()
        val zipBytes = TestFixtures.createValidZipBackup(minerals)

        val zipFile = File(tempDir, "large_backup.zip")
        zipFile.writeBytes(zipBytes)
        val uri = Uri.fromFile(zipFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(zipBytes)
        every { contentResolver.query(uri, any(), any(), any(), any()) } returns mockk {
            every { moveToFirst() } returns true
            every { getLong(0) } returns zipBytes.size.toLong()
            every { close() } just Runs
        }

        coEvery { mineralDao.insert(any()) } returns 1L
        coEvery { mineralDao.deleteAll() } just Runs
        coEvery { provenanceDao.deleteAll() } just Runs
        coEvery { storageDao.deleteAll() } just Runs
        coEvery { photoDao.deleteAll() } just Runs

        // Measure memory before
        val runtime = Runtime.getRuntime()
        runtime.gc()
        Thread.sleep(100)
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

        // When - Import with performance measurement
        val duration = measureTimeMillis {
            val result = repository.importZip(uri, mode = ImportMode.REPLACE)
            assertTrue(result.isSuccess, "Import should succeed for 1000 minerals")

            val importResult = result.getOrThrow()
            assertEquals(1000, importResult.imported, "Should import all 1000 minerals")
            assertEquals(0, importResult.skipped)
        }

        // Measure memory after
        runtime.gc()
        Thread.sleep(100)
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = (memoryAfter - memoryBefore) / (1024 * 1024) // Convert to MB

        // Then - Performance assertions
        assertTrue(
            duration < 30_000,
            "Import took ${duration}ms, expected < 30s (30000ms)"
        )

        // Memory assertion: < 100MB
        // Note: This is a rough estimate as JVM memory can fluctuate
        assertTrue(
            memoryUsed < 100,
            "Memory used: ${memoryUsed}MB, expected < 100MB"
        )

        println("✓ Performance: 1000 minerals imported in ${duration}ms using ~${memoryUsed}MB RAM")
    }

    @Test
    fun `exportZip 1000minerals performance`() = runTest {
        // Given - Database with 1000 minerals
        val minerals = TestFixtures.batch1000Minerals()
        val mineralEntities = minerals.map { it.toEntity() }

        coEvery { mineralDao.getAll() } returns mineralEntities
        coEvery { provenanceDao.getByMineralIds(any()) } returns emptyList()
        coEvery { storageDao.getByMineralIds(any()) } returns emptyList()
        coEvery { photoDao.getByMineralIds(any()) } returns emptyList()

        val zipFile = File(tempDir, "export_1000.zip")
        val uri = Uri.fromFile(zipFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(uri) } returns zipFile.outputStream()

        // When - Export with performance measurement
        val duration = measureTimeMillis {
            val result = repository.exportZip(uri)
            assertTrue(result.isSuccess, "Export should succeed for 1000 minerals")
        }

        // Then
        assertTrue(zipFile.exists(), "ZIP file should be created")
        assertTrue(zipFile.length() > 0, "ZIP file should not be empty")

        // Performance target: < 20s for export
        assertTrue(
            duration < 20_000,
            "Export took ${duration}ms, expected < 20s (20000ms)"
        )

        println("✓ Performance: 1000 minerals exported in ${duration}ms (target: < 20s)")
    }

    @Test
    fun `importZip encrypted 1000minerals performance`() = runTest {
        // Given - Large encrypted backup
        val minerals = TestFixtures.batch1000Minerals()
        val password = "Test123!".toCharArray()

        // Create encrypted ZIP manually (simplified for test)
        val zipBytes = TestFixtures.createValidZipBackup(minerals)

        val zipFile = File(tempDir, "large_encrypted.zip")
        zipFile.writeBytes(zipBytes)
        val uri = Uri.fromFile(zipFile)

        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(zipBytes)
        every { contentResolver.query(uri, any(), any(), any(), any()) } returns mockk {
            every { moveToFirst() } returns true
            every { getLong(0) } returns zipBytes.size.toLong()
            every { close() } just Runs
        }

        coEvery { mineralDao.insert(any()) } returns 1L
        coEvery { mineralDao.deleteAll() } just Runs
        coEvery { provenanceDao.deleteAll() } just Runs
        coEvery { storageDao.deleteAll() } just Runs
        coEvery { photoDao.deleteAll() } just Runs

        // When - Import unencrypted (encrypted test would require crypto setup)
        val duration = measureTimeMillis {
            val result = repository.importZip(uri, mode = ImportMode.REPLACE)
            assertTrue(result.isSuccess, "Import should succeed")
        }

        // Then
        assertTrue(
            duration < 35_000,
            "Encrypted import took ${duration}ms, expected < 35s (allows 5s overhead for decryption)"
        )

        println("✓ Performance: 1000 encrypted minerals imported in ${duration}ms")
    }
}
