package net.meshcore.mineralog.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.service.BackupEncryptionService
import net.meshcore.mineralog.data.service.CsvBackupService
import net.meshcore.mineralog.data.service.MineralCsvMapper
import net.meshcore.mineralog.data.service.ZipBackupService
import net.meshcore.mineralog.domain.model.Mineral
import java.io.File

interface BackupRepository {
    suspend fun exportZip(uri: Uri, password: CharArray? = null): Result<Unit>
    suspend fun exportCsv(uri: Uri, minerals: List<Mineral>): Result<Unit>
    suspend fun importZip(
        uri: Uri,
        password: CharArray? = null,
        mode: ImportMode = ImportMode.MERGE
    ): Result<ImportResult>
    suspend fun importCsv(
        uri: Uri,
        columnMapping: Map<String, String>? = null,
        mode: CsvImportMode = CsvImportMode.MERGE
    ): Result<ImportResult>
    suspend fun createBackup(password: CharArray? = null): Result<File>
    suspend fun restoreBackup(file: File, password: CharArray? = null): Result<Unit>
}

enum class ImportMode {
    MERGE,      // Upsert by ID
    REPLACE,    // Drop all and import
    MAP_IDS     // Remap conflicting UUIDs
}

enum class CsvImportMode {
    MERGE,             // Update existing by name match, insert new
    REPLACE,           // Drop all and import
    SKIP_DUPLICATES    // Skip rows with duplicate names
}

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val errors: List<String>
)

/**
 * Facade/Coordinator for backup operations.
 * Delegates to specialized services for better separation of concerns.
 *
 * Refactored from 744 LOC god class to clean facade pattern (now ~80 LOC).
 *
 * Services:
 * - ZipBackupService: Handles ZIP export/import operations
 * - CsvBackupService: Handles CSV export/import operations
 * - BackupEncryptionService: Handles encryption/decryption
 * - MineralCsvMapper: Handles CSV row parsing
 */
class BackupRepositoryImpl(
    private val context: Context,
    private val database: MineraLogDatabase
) : BackupRepository {

    // Lazy initialization of services
    private val csvMapper by lazy { MineralCsvMapper() }
    private val encryptionService by lazy { BackupEncryptionService() }
    private val csvBackupService by lazy { CsvBackupService(context, database, csvMapper) }
    private val zipBackupService by lazy { ZipBackupService(context, database, encryptionService) }

    override suspend fun exportZip(uri: Uri, password: CharArray?): Result<Unit> {
        return zipBackupService.exportZip(uri, password)
    }

    override suspend fun importZip(uri: Uri, password: CharArray?, mode: ImportMode): Result<ImportResult> {
        return zipBackupService.importZip(uri, password, mode)
    }

    override suspend fun exportCsv(uri: Uri, minerals: List<Mineral>): Result<Unit> {
        return csvBackupService.exportCsv(uri, minerals)
    }

    override suspend fun importCsv(
        uri: Uri,
        columnMapping: Map<String, String>?,
        mode: CsvImportMode
    ): Result<ImportResult> {
        return csvBackupService.importCsv(uri, columnMapping, mode)
    }

    override suspend fun createBackup(password: CharArray?): Result<File> = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "backups")
            backupDir.mkdirs()
            val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}.zip")

            val uri = Uri.fromFile(backupFile)
            exportZip(uri, password).getOrThrow()

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreBackup(file: File, password: CharArray?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.fromFile(file)
            importZip(uri, password, ImportMode.REPLACE).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
