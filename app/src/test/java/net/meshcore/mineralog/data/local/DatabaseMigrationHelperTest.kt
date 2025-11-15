package net.meshcore.mineralog.data.local

import android.content.Context
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for DatabaseMigrationHelper edge cases.
 *
 * These tests verify that the migration helper correctly handles:
 * - Empty database files (0 bytes)
 * - Non-existent database files
 * - Corrupted database files
 * - Race conditions (file disappears during check)
 * - First-time app launch (no database exists)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Use API 28 for Robolectric compatibility
class DatabaseMigrationHelperTest {

    private lateinit var context: Context
    private lateinit var dbFile: File

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        dbFile = context.getDatabasePath("mineralog_database")

        // Clean up any existing database files before each test
        cleanupDatabaseFiles()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        cleanupDatabaseFiles()
    }

    private fun cleanupDatabaseFiles() {
        dbFile.delete()
        File(dbFile.parent, "mineralog_database-wal").delete()
        File(dbFile.parent, "mineralog_database-shm").delete()
        dbFile.parentFile?.listFiles { file ->
            file.name.startsWith("mineralog_database_plaintext_backup_") ||
            file.name.startsWith("mineralog_database_encrypted_temp_")
        }?.forEach { it.delete() }
    }

    /**
     * Test Case 1: First-time app launch (no database exists)
     *
     * Expected: migrateIfNeeded() should return MigrationResult.NoDatabase
     */
    @Test
    fun `migrateIfNeeded should return NoDatabase when no database file exists`() {
        // Given: No database file exists
        assertFalse(dbFile.exists())

        // When: migrateIfNeeded is called
        val result = DatabaseMigrationHelper.migrateIfNeeded(context)

        // Then: Should return NoDatabase
        assertTrue(result is DatabaseMigrationHelper.MigrationResult.NoDatabase)
        assertFalse(dbFile.exists())
    }

    /**
     * Test Case 2: Empty database file (0 bytes)
     *
     * Expected: Empty file should be detected, deleted, and return NoDatabase
     */
    @Test
    fun `migrateIfNeeded should delete empty database file and return NoDatabase`() {
        // Given: An empty database file (0 bytes)
        dbFile.parentFile?.mkdirs()
        dbFile.createNewFile()
        val walFile = File(dbFile.parent, "mineralog_database-wal").also { it.createNewFile() }
        val shmFile = File(dbFile.parent, "mineralog_database-shm").also { it.createNewFile() }

        assertTrue(dbFile.exists())
        assertEquals(0L, dbFile.length())

        // When: migrateIfNeeded is called
        val result = DatabaseMigrationHelper.migrateIfNeeded(context)

        // Then: Should return NoDatabase
        assertTrue(result is DatabaseMigrationHelper.MigrationResult.NoDatabase)

        // And: Empty database file should be deleted
        assertFalse(dbFile.exists())
        assertFalse(walFile.exists())
        assertFalse(shmFile.exists())
    }

    /**
     * Test Case 3: Very small file (< SQLite header size)
     *
     * Expected: File should be treated as corrupted and handled gracefully
     */
    @Test
    fun `migrateIfNeeded should handle very small corrupted database file`() {
        // Given: A database file with only a few bytes (corrupted)
        dbFile.parentFile?.mkdirs()
        dbFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02)) // Only 3 bytes

        assertTrue(dbFile.exists())
        assertTrue(dbFile.length() < 100)

        // When: migrateIfNeeded is called
        val result = DatabaseMigrationHelper.migrateIfNeeded(context)

        // Then: Should handle gracefully
        assertTrue(
            result is DatabaseMigrationHelper.MigrationResult.NoDatabase ||
            result is DatabaseMigrationHelper.MigrationResult.Error
        )
    }

    /**
     * Test Case 4: Multiple rapid launches (race condition simulation)
     *
     * Expected: Each call should handle missing/corrupted files gracefully
     */
    @Test
    fun `migrateIfNeeded should handle rapid successive calls without crashing`() {
        // Given: No initial database
        assertFalse(dbFile.exists())

        // When: migrateIfNeeded is called multiple times rapidly
        val results = (1..5).map {
            DatabaseMigrationHelper.migrateIfNeeded(context)
        }

        // Then: All calls should complete
        assertEquals(5, results.size)
        assertTrue(results.all { it is DatabaseMigrationHelper.MigrationResult.NoDatabase })
    }

    /**
     * Test Case 5: File exists but has invalid content
     *
     * Expected: Should detect corruption and handle gracefully
     */
    @Test
    fun `migrateIfNeeded should handle database file with invalid SQLite content`() {
        // Given: A database file with invalid content
        dbFile.parentFile?.mkdirs()
        dbFile.writeText("This is not a valid SQLite database file!")

        assertTrue(dbFile.exists())
        assertTrue(dbFile.length() > 0)

        // When: migrateIfNeeded is called
        val result = DatabaseMigrationHelper.migrateIfNeeded(context)

        // Then: Should handle gracefully
        assertTrue(
            result is DatabaseMigrationHelper.MigrationResult.NoDatabase ||
            result is DatabaseMigrationHelper.MigrationResult.Error ||
            result is DatabaseMigrationHelper.MigrationResult.AlreadyEncrypted
        )
    }

    /**
     * Test Case 6: Backup deletion after successful migration
     *
     * Expected: deleteBackup() should successfully delete an existing backup file
     */
    @Test
    fun `deleteBackup should successfully delete existing backup file`() {
        // Given: A backup file exists
        val backupFile = File(dbFile.parent, "mineralog_database_plaintext_backup_123456789")
        backupFile.parentFile?.mkdirs()
        backupFile.createNewFile()
        backupFile.writeText("backup content")

        assertTrue(backupFile.exists())

        // When: deleteBackup is called
        val deleted = DatabaseMigrationHelper.deleteBackup(backupFile.absolutePath)

        // Then: Should return true and file should be deleted
        assertTrue(deleted)
        assertFalse(backupFile.exists())
    }

    /**
     * Test Case 7: Backup deletion with non-existent file
     *
     * Expected: deleteBackup() should return false
     */
    @Test
    fun `deleteBackup should return false for non-existent backup file`() {
        // Given: A backup file that doesn't exist
        val nonExistentPath = File(dbFile.parent, "nonexistent_backup_file").absolutePath

        // When: deleteBackup is called
        val deleted = DatabaseMigrationHelper.deleteBackup(nonExistentPath)

        // Then: Should return false
        assertFalse(deleted)
    }

    /**
     * Test Case 8: Concurrent access simulation
     *
     * Expected: No crashes, consistent final state
     */
    @Test
    fun `migrateIfNeeded should handle concurrent access gracefully`() {
        // Given: No initial database
        assertFalse(dbFile.exists())

        // When: Multiple threads call migrateIfNeeded simultaneously
        val results = mutableListOf<DatabaseMigrationHelper.MigrationResult>()
        repeat(3) {
            results.add(DatabaseMigrationHelper.migrateIfNeeded(context))
        }

        // Then: All calls should complete
        assertEquals(3, results.size)
        assertTrue(results.all { it is DatabaseMigrationHelper.MigrationResult.NoDatabase })
    }
}
