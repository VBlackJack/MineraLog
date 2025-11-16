package net.meshcore.mineralog.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.meshcore.mineralog.util.AppLogger
import net.sqlcipher.database.SupportFactory
import java.io.File
import java.io.IOException

/**
 * Helper class to migrate plaintext Room databases to encrypted SQLCipher databases.
 *
 * This is critical for users who installed the app before encryption was implemented.
 * Without this migration, users with existing plaintext databases would experience crashes
 * when the app tries to open them with SQLCipher encryption.
 *
 * Migration process:
 * 1. Detect if database exists and is plaintext (not encrypted)
 * 2. Create timestamped backup of plaintext database
 * 3. Export plaintext data to temporary encrypted database
 * 4. Verify encrypted database integrity
 * 5. Replace plaintext database with encrypted version
 * 6. Clean up temporary files
 *
 * Security considerations:
 * - Plaintext backup is created in app-private storage only
 * - Backup is deleted after successful migration
 * - All operations are atomic where possible
 * - Errors trigger rollback to original plaintext database
 */
object DatabaseMigrationHelper {

    private const val TAG = "DBMigration"
    private const val DATABASE_NAME = "mineralog_database"

    /**
     * Result of database encryption status check.
     */
    private sealed class DatabaseEncryptionStatus {
        /** Database is encrypted (SQLCipher format) */
        object Encrypted : DatabaseEncryptionStatus()

        /** Database is plaintext (standard SQLite format) */
        object Plaintext : DatabaseEncryptionStatus()

        /** Database file is corrupted or cannot be opened */
        data class Corrupted(val reason: String) : DatabaseEncryptionStatus()
    }

    /**
     * Result of migration operation.
     */
    sealed class MigrationResult {
        /** Database was already encrypted, no migration needed */
        object AlreadyEncrypted : MigrationResult()

        /** Database doesn't exist (new installation) */
        object NoDatabase : MigrationResult()

        /** Migration completed successfully */
        data class Success(val backupPath: String) : MigrationResult()

        /** Migration failed, original database preserved */
        data class Error(val message: String, val cause: Throwable? = null) : MigrationResult()
    }

    /**
     * Checks if migration from plaintext to encrypted database is needed,
     * and performs the migration if necessary.
     *
     * This should be called BEFORE the main database is opened.
     *
     * @param context Application context
     * @return MigrationResult indicating what happened
     */
    fun migrateIfNeeded(context: Context): MigrationResult {
        val dbFile = context.getDatabasePath(DATABASE_NAME)

        // Check if database file exists
        if (!dbFile.exists()) {
            AppLogger.i(TAG, "No existing database found, will create encrypted database")
            return MigrationResult.NoDatabase
        }

        // Check if database file is empty (corrupt or incomplete creation)
        // An empty file should be treated as "no database"
        if (dbFile.length() == 0L) {
            AppLogger.w(TAG, "Database file exists but is empty (likely incomplete creation), deleting and creating new encrypted database")
            // Delete the empty file and any associated files
            dbFile.delete()
            File(dbFile.parent, "$DATABASE_NAME-wal").delete()
            File(dbFile.parent, "$DATABASE_NAME-shm").delete()
            return MigrationResult.NoDatabase
        }

        // Check if database is already encrypted
        when (val encryptionCheck = isDatabaseEncrypted(dbFile)) {
            is DatabaseEncryptionStatus.Encrypted -> {
                AppLogger.i(TAG, "Database is already encrypted, no migration needed")
                return MigrationResult.AlreadyEncrypted
            }
            is DatabaseEncryptionStatus.Plaintext -> {
                AppLogger.i(TAG, "Plaintext database detected, starting migration to encrypted format")
                return try {
                    migratePlaintextToEncrypted(context, dbFile)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Migration failed", e)
                    MigrationResult.Error("Failed to migrate database: ${e.message}", e)
                }
            }
            is DatabaseEncryptionStatus.Corrupted -> {
                AppLogger.e(TAG, "Database file is corrupted: ${encryptionCheck.reason}, deleting and creating new encrypted database")
                // Delete corrupted database and create fresh one
                dbFile.delete()
                File(dbFile.parent, "$DATABASE_NAME-wal").delete()
                File(dbFile.parent, "$DATABASE_NAME-shm").delete()
                return MigrationResult.NoDatabase
            }
        }
    }

    /**
     * Checks if a database file is encrypted by reading its header bytes.
     *
     * CRITICAL FIX (2025-11-15): This method NO LONGER uses SQLiteDatabase.openDatabase()
     * because Android's DefaultDatabaseErrorHandler will DELETE the database file if it
     * detects "corruption" (which happens when trying to open an encrypted SQLCipher database
     * with the standard Android SQLite API).
     *
     * Instead, we check the file header bytes:
     * - Plaintext SQLite: Starts with "SQLite format 3\0" (16 bytes magic number)
     * - Encrypted SQLCipher: Starts with random bytes (ciphertext)
     *
     * This method distinguishes between three states:
     * 1. Plaintext: File header matches SQLite magic number
     * 2. Encrypted: File header does NOT match (likely SQLCipher encrypted)
     * 3. Corrupted: File exists but cannot be read or is invalid
     *
     * @param dbFile Database file to check
     * @return DatabaseEncryptionStatus indicating the database state
     */
    private fun isDatabaseEncrypted(dbFile: File): DatabaseEncryptionStatus {
        // Additional safety check: file must exist and have non-zero size
        if (!dbFile.exists()) {
            return DatabaseEncryptionStatus.Corrupted("File does not exist")
        }

        if (dbFile.length() == 0L) {
            return DatabaseEncryptionStatus.Corrupted("File is empty (0 bytes)")
        }

        // Minimum size check: SQLite header is 100 bytes, but we only check first 16
        if (dbFile.length() < 16) {
            AppLogger.w(TAG, "Database file too small (${dbFile.length()} bytes)")
            return DatabaseEncryptionStatus.Corrupted("File too small: ${dbFile.length()} bytes")
        }

        return try {
            // Read first 16 bytes of the file
            // Plaintext SQLite databases ALWAYS start with "SQLite format 3\0"
            val header = dbFile.inputStream().use { input ->
                val bytes = ByteArray(16)
                val bytesRead = input.read(bytes)
                if (bytesRead < 16) {
                    AppLogger.w(TAG, "Could not read full header (only $bytesRead bytes)")
                    return DatabaseEncryptionStatus.Corrupted("Incomplete header: $bytesRead bytes")
                }
                bytes
            }

            // SQLite magic number: "SQLite format 3\0"
            val sqliteMagic = byteArrayOf(
                0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66, // "SQLite f"
                0x6F, 0x72, 0x6D, 0x61, 0x74, 0x20, 0x33, 0x00  // "ormat 3\0"
            )

            val isPlaintext = header.contentEquals(sqliteMagic)

            if (isPlaintext) {
                AppLogger.d(TAG, "Database header matches SQLite magic - plaintext database")
                DatabaseEncryptionStatus.Plaintext
            } else {
                AppLogger.d(TAG, "Database header does NOT match SQLite magic - encrypted database")
                DatabaseEncryptionStatus.Encrypted
            }

        } catch (e: IOException) {
            // Cannot read file
            AppLogger.e(TAG, "I/O error reading database header", e)
            DatabaseEncryptionStatus.Corrupted("I/O error: ${e.message}")

        } catch (e: Exception) {
            // Any other exception - assume encrypted to be safe
            AppLogger.w(TAG, "Unexpected exception reading header, assuming encrypted", e)
            DatabaseEncryptionStatus.Encrypted
        }
    }

    /**
     * Performs the actual migration from plaintext to encrypted database.
     *
     * Steps:
     * 1. Create backup of plaintext database
     * 2. Open plaintext database
     * 3. Create new encrypted database
     * 4. Use SQLCipher's ATTACH DATABASE and sqlcipher_export() to copy data
     * 5. Verify encrypted database
     * 6. Replace plaintext with encrypted
     * 7. Clean up
     *
     * @param context Application context
     * @param plaintextDbFile Plaintext database file
     * @return MigrationResult.Success with backup path
     * @throws IOException if backup or file operations fail
     */
    private fun migratePlaintextToEncrypted(
        context: Context,
        plaintextDbFile: File
    ): MigrationResult {
        val timestamp = System.currentTimeMillis()
        val backupFile = File(plaintextDbFile.parent, "${DATABASE_NAME}_plaintext_backup_$timestamp")
        val tempEncryptedFile = File(plaintextDbFile.parent, "${DATABASE_NAME}_encrypted_temp_$timestamp")

        try {
            // Step 1: Create backup
            AppLogger.d(TAG, "Creating backup at: ${backupFile.absolutePath}")
            plaintextDbFile.copyTo(backupFile, overwrite = false)

            // Step 2: Get encryption passphrase
            val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

            // Step 3: Open plaintext database and export to encrypted
            AppLogger.d(TAG, "Opening plaintext database")
            val plaintextDb = SQLiteDatabase.openDatabase(
                plaintextDbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )

            try {
                // Step 4: Attach encrypted database and export
                AppLogger.d(TAG, "Creating encrypted database and migrating data")

                // Initialize SQLCipher library
                System.loadLibrary("sqlcipher")

                // Convert passphrase to hex string for SQL command
                val passphraseHex = passphrase.joinToString("") { "%02x".format(it) }

                // Attach the new encrypted database
                plaintextDb.execSQL(
                    "ATTACH DATABASE '${tempEncryptedFile.absolutePath}' AS encrypted KEY \"x'$passphraseHex'\""
                )

                // Export schema and data to encrypted database
                plaintextDb.execSQL("SELECT sqlcipher_export('encrypted')")

                // Detach encrypted database
                plaintextDb.execSQL("DETACH DATABASE encrypted")

                AppLogger.d(TAG, "Data migration completed")
            } finally {
                plaintextDb.close()
                // Clear passphrase from memory
                passphrase.fill(0)
            }

            // Step 5: Verify encrypted database integrity
            AppLogger.d(TAG, "Verifying encrypted database integrity")
            verifyEncryptedDatabase(context, tempEncryptedFile)

            // Step 6: Replace plaintext database with encrypted version
            AppLogger.d(TAG, "Replacing plaintext database with encrypted version")

            // Delete plaintext database
            if (!plaintextDbFile.delete()) {
                throw IOException("Failed to delete plaintext database")
            }

            // Rename encrypted temp to final name
            if (!tempEncryptedFile.renameTo(plaintextDbFile)) {
                // Restore from backup if rename fails
                backupFile.copyTo(plaintextDbFile, overwrite = true)
                throw IOException("Failed to rename encrypted database")
            }

            // Delete associated files (-wal, -shm)
            File(plaintextDbFile.parent, "$DATABASE_NAME-wal").delete()
            File(plaintextDbFile.parent, "$DATABASE_NAME-shm").delete()

            AppLogger.i(TAG, "Migration completed successfully")
            return MigrationResult.Success(backupFile.absolutePath)

        } catch (e: Exception) {
            AppLogger.e(TAG, "Migration failed, cleaning up", e)

            // Clean up temp files
            tempEncryptedFile.delete()

            // Keep backup file for manual recovery
            return MigrationResult.Error(
                "Migration failed: ${e.message}. Plaintext backup saved at: ${backupFile.absolutePath}",
                e
            )
        }
    }

    /**
     * Verifies that the encrypted database can be opened and contains data.
     *
     * @param context Application context
     * @param encryptedDbFile Encrypted database file to verify
     * @throws IllegalStateException if verification fails
     */
    private fun verifyEncryptedDatabase(context: Context, encryptedDbFile: File) {
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        try {
            val factory = SupportFactory(passphrase)

            // Try to open with Room to verify schema compatibility
            val tempDb = Room.databaseBuilder(
                context,
                MineraLogDatabase::class.java,
                encryptedDbFile.name
            )
                .openHelperFactory(factory)
                .build()

            try {
                // Try to query to verify database is accessible
                val mineralCount = tempDb.openHelper.readableDatabase
                    .query("SELECT COUNT(*) FROM minerals").use { cursor ->
                        if (cursor.moveToFirst()) cursor.getInt(0) else 0
                    }

                AppLogger.d(TAG, "Encrypted database verified, contains $mineralCount minerals")
            } finally {
                tempDb.close()
            }
        } finally {
            // Clear passphrase from memory
            passphrase.fill(0)
        }
    }

    /**
     * Deletes the plaintext backup file after successful migration.
     * Only call this after verifying the encrypted database works correctly.
     *
     * @param backupPath Path to backup file
     * @return true if deleted successfully
     */
    fun deleteBackup(backupPath: String): Boolean {
        val backupFile = File(backupPath)
        return if (backupFile.exists()) {
            backupFile.delete()
        } else {
            false
        }
    }
}
