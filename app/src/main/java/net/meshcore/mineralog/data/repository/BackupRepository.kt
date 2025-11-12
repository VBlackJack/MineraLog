package net.meshcore.mineralog.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.mapper.toDomain
import net.meshcore.mineralog.domain.model.Mineral
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

interface BackupRepository {
    suspend fun exportZip(uri: Uri, password: String? = null): Result<Unit>
    suspend fun importZip(uri: Uri, password: String? = null, mode: ImportMode = ImportMode.MERGE): Result<ImportResult>
    suspend fun createBackup(password: String? = null): Result<File>
    suspend fun restoreBackup(file: File, password: String? = null): Result<Unit>
}

enum class ImportMode {
    MERGE,      // Upsert by ID
    REPLACE,    // Drop all and import
    MAP_IDS     // Remap conflicting UUIDs
}

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val errors: List<String>
)

class BackupRepositoryImpl(
    private val context: Context,
    private val database: MineraLogDatabase
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportZip(uri: Uri, password: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get all data
            val minerals = database.mineralDao().getAll().map { mineralEntity ->
                val provenance = database.provenanceDao().getByMineralId(mineralEntity.id)
                val storage = database.storageDao().getByMineralId(mineralEntity.id)
                val photos = database.photoDao().getByMineralId(mineralEntity.id)
                mineralEntity.toDomain(provenance, storage, photos)
            }

            // Create ZIP
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zip ->
                    // manifest.json
                    val manifest = mapOf(
                        "app" to "MineraLog",
                        "schemaVersion" to "1.0.0",
                        "exportedAt" to Instant.now().toString(),
                        "counts" to mapOf(
                            "minerals" to minerals.size,
                            "photos" to minerals.sumOf { it.photos.size }
                        ),
                        "encrypted" to (password != null)
                    )
                    zip.putNextEntry(ZipEntry("manifest.json"))
                    zip.write(json.encodeToString(manifest).toByteArray())
                    zip.closeEntry()

                    // minerals.json
                    zip.putNextEntry(ZipEntry("minerals.json"))
                    zip.write(json.encodeToString(minerals).toByteArray())
                    zip.closeEntry()

                    // media files
                    minerals.forEach { mineral ->
                        mineral.photos.forEach { photo ->
                            val photoFile = File(context.filesDir, photo.fileName)
                            if (photoFile.exists()) {
                                zip.putNextEntry(ZipEntry(photo.fileName))
                                photoFile.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importZip(uri: Uri, password: String?, mode: ImportMode): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val errors = mutableListOf<String>()
            var imported = 0
            var skipped = 0

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    var mineralsJson: String? = null

                    while (entry != null) {
                        when {
                            entry.name == "minerals.json" -> {
                                mineralsJson = zip.readBytes().toString(Charsets.UTF_8)
                            }
                            entry.name.startsWith("media/") -> {
                                val file = File(context.filesDir, entry.name)
                                file.parentFile?.mkdirs()
                                file.outputStream().use { zip.copyTo(it) }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }

                    // Import minerals
                    mineralsJson?.let { jsonStr ->
                        val minerals = json.decodeFromString<List<Mineral>>(jsonStr)

                        if (mode == ImportMode.REPLACE) {
                            database.mineralDao().deleteAll()
                        }

                        minerals.forEach { mineral ->
                            try {
                                database.mineralDao().insert(mineral.toEntity())
                                mineral.provenance?.let { database.provenanceDao().insert(it.toEntity()) }
                                mineral.storage?.let { database.storageDao().insert(it.toEntity()) }
                                mineral.photos.forEach { database.photoDao().insert(it.toEntity()) }
                                imported++
                            } catch (e: Exception) {
                                errors.add("Failed to import ${mineral.name}: ${e.message}")
                                skipped++
                            }
                        }
                    }
                }
            }

            Result.success(ImportResult(imported, skipped, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createBackup(password: String?): Result<File> = withContext(Dispatchers.IO) {
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

    override suspend fun restoreBackup(file: File, password: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.fromFile(file)
            importZip(uri, password, ImportMode.REPLACE).getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun Mineral.toEntity() = net.meshcore.mineralog.data.mapper.toEntity()
    private fun net.meshcore.mineralog.domain.model.Provenance.toEntity() = net.meshcore.mineralog.data.mapper.toEntity()
    private fun net.meshcore.mineralog.domain.model.Storage.toEntity() = net.meshcore.mineralog.data.mapper.toEntity()
    private fun net.meshcore.mineralog.domain.model.Photo.toEntity() = net.meshcore.mineralog.data.mapper.toEntity()
}
