package net.meshcore.mineralog.data.service

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.meshcore.mineralog.data.crypto.DecryptionException
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.mapper.toDomain
import net.meshcore.mineralog.data.mapper.toEntity
import net.meshcore.mineralog.data.repository.ImportMode
import net.meshcore.mineralog.data.repository.ImportResult
import net.meshcore.mineralog.domain.model.Mineral
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Service responsible for ZIP backup operations (export and import).
 * Extracted from BackupRepository for better separation of concerns.
 */
class ZipBackupService(
    private val context: Context,
    private val database: MineraLogDatabase,
    private val encryptionService: BackupEncryptionService
) {

    // Security constants
    private val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100 MB compressed
    private val MAX_DECOMPRESSED_SIZE = 500 * 1024 * 1024L // 500 MB decompressed
    private val MAX_DECOMPRESSION_RATIO = 100 // Prevent ZIP bombs

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Export database to ZIP file with optional encryption.
     *
     * @param uri The URI to write the ZIP file to
     * @param password Optional password for encryption
     * @return Result indicating success or failure
     */
    suspend fun exportZip(uri: Uri, password: CharArray?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get all data with batch queries to avoid N+1 problem
            val mineralEntities = database.mineralDao().getAll()
            if (mineralEntities.isEmpty()) {
                return@withContext Result.failure(Exception("No minerals to export"))
            }

            val mineralIds = mineralEntities.map { it.id }
            val provenances = database.provenanceDao().getByMineralIds(mineralIds).associateBy { it.mineralId }
            val storages = database.storageDao().getByMineralIds(mineralIds).associateBy { it.mineralId }
            val photos = database.photoDao().getByMineralIds(mineralIds).groupBy { it.mineralId }

            val minerals = mineralEntities.map { mineralEntity ->
                mineralEntity.toDomain(
                    provenances[mineralEntity.id],
                    storages[mineralEntity.id],
                    photos[mineralEntity.id] ?: emptyList()
                )
            }

            // Create ZIP
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zip ->
                    // Prepare minerals.json data
                    val mineralsJsonBytes = json.encodeToString(minerals).toByteArray()

                    // Encrypt if password provided
                    val encryptionResult = if (password != null) {
                        encryptionService.encrypt(mineralsJsonBytes, password)
                    } else null

                    // Create manifest
                    val manifest = encryptionService.createManifest(
                        mineralCount = minerals.size,
                        photoCount = minerals.sumOf { it.photos.size },
                        encrypted = password != null,
                        encryptionMetadata = encryptionResult?.let { encryptionService.createEncryptionMetadata(it) }
                    )

                    // Write manifest.json
                    zip.putNextEntry(ZipEntry("manifest.json"))
                    zip.write(json.encodeToString(manifest).toByteArray())
                    zip.closeEntry()

                    // Write minerals.json (encrypted or plaintext)
                    zip.putNextEntry(ZipEntry("minerals.json"))
                    if (encryptionResult != null) {
                        zip.write(encryptionResult.ciphertext)
                    } else {
                        zip.write(mineralsJsonBytes)
                    }
                    zip.closeEntry()

                    // Write media files
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

    /**
     * Import database from ZIP file with optional decryption.
     *
     * @param uri The URI of the ZIP file to import
     * @param password Optional password for decryption
     * @param mode The import mode (MERGE, REPLACE, MAP_IDS)
     * @return Result containing import statistics
     */
    suspend fun importZip(
        uri: Uri,
        password: CharArray?,
        mode: ImportMode
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        var totalCompressedBytes = 0L
        var totalDecompressedBytes = 0L

        try {
            val errors = mutableListOf<String>()
            var imported = 0
            var skipped = 0

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileSize = context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else 0L
                } ?: 0L

                if (fileSize > MAX_FILE_SIZE) {
                    return@withContext Result.failure(Exception("File too large. Maximum size is 100 MB."))
                }

                ZipInputStream(inputStream).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    var mineralsBytes: ByteArray? = null
                    var manifestJson: String? = null

                    // First pass: read manifest and minerals.json
                    while (entry != null) {
                        // Security: Sanitize entry path
                        val sanitizedPath = sanitizeZipEntryPath(entry.name)
                        if (sanitizedPath == null) {
                            errors.add("Skipped malicious ZIP entry: ${entry.name}")
                            zip.closeEntry()
                            entry = zip.nextEntry
                            continue
                        }

                        // Security: Track decompression ratio (ZIP bomb protection)
                        val entryCompressedSize = entry.compressedSize
                        val entryUncompressedSize = entry.size

                        if (entryUncompressedSize > 0) {
                            totalCompressedBytes += if (entryCompressedSize > 0) entryCompressedSize else entryUncompressedSize
                            totalDecompressedBytes += entryUncompressedSize

                            // Check decompression ratio
                            if (totalCompressedBytes > 0) {
                                val ratio = totalDecompressedBytes / totalCompressedBytes
                                if (ratio > MAX_DECOMPRESSION_RATIO) {
                                    val message = "Potential ZIP bomb detected: decompression ratio $ratio:1 " +
                                        "exceeds limit of $MAX_DECOMPRESSION_RATIO:1"
                                    return@withContext Result.failure(Exception(message))
                                }
                            }

                            // Check total decompressed size
                            if (totalDecompressedBytes > MAX_DECOMPRESSED_SIZE) {
                                val decompressedMB = totalDecompressedBytes / 1024 / 1024
                                val maxMB = MAX_DECOMPRESSED_SIZE / 1024 / 1024
                                val message = "Decompressed size ${decompressedMB}MB exceeds maximum ${maxMB}MB"
                                return@withContext Result.failure(Exception(message))
                            }
                        }

                        when {
                            sanitizedPath == "manifest.json" -> {
                                manifestJson = zip.readBytes().toString(Charsets.UTF_8)
                            }
                            sanitizedPath == "minerals.json" -> {
                                mineralsBytes = zip.readBytes()
                            }
                            sanitizedPath.startsWith("media/") -> {
                                val file = File(context.filesDir, sanitizedPath)
                                // Additional safety: ensure file is within filesDir
                                if (file.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
                                    file.parentFile?.mkdirs()
                                    file.outputStream().use { zip.copyTo(it) }
                                } else {
                                    errors.add("Skipped file outside allowed directory: $sanitizedPath")
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }

                    // Parse manifest to check for encryption
                    val manifest = manifestJson?.let { json.decodeFromString<Map<String, Any>>(it) }

                    // Security: Validate schema version
                    val schemaVersion = manifest?.get("schemaVersion") as? String
                    if (!encryptionService.validateSchemaVersion(schemaVersion)) {
                        val message = "Incompatible backup schema version: $schemaVersion. " +
                            "Only version 1.0.0 is supported."
                        return@withContext Result.failure(Exception(message))
                    }
                    val isEncrypted = manifest?.get("encrypted") as? Boolean ?: false

                    // Decrypt minerals.json if necessary
                    val mineralsJson = if (isEncrypted) {
                        if (password == null) {
                            return@withContext Result.failure(Exception("This backup is encrypted. Please provide a password."))
                        }

                        // Extract encryption metadata
                        val encryptionMap = manifest?.get("encryption") as? Map<*, *>
                            ?: return@withContext Result.failure(Exception("Encrypted backup is missing encryption metadata"))

                        val encodedSalt = encryptionMap["salt"] as? String
                            ?: return@withContext Result.failure(Exception("Missing encryption salt"))
                        val encodedIv = encryptionMap["iv"] as? String
                            ?: return@withContext Result.failure(Exception("Missing encryption IV"))

                        // Decrypt
                        try {
                            val decryptedBytes = encryptionService.decrypt(
                                ciphertext = mineralsBytes!!,
                                password = password,
                                encodedSalt = encodedSalt,
                                encodedIv = encodedIv
                            )
                            String(decryptedBytes, Charsets.UTF_8)
                        } catch (e: DecryptionException) {
                            return@withContext Result.failure(Exception("Failed to decrypt backup. Wrong password or corrupted data.", e))
                        }
                    } else {
                        mineralsBytes?.toString(Charsets.UTF_8)
                    }

                    // Import minerals with transaction for data integrity
                    mineralsJson?.let { jsonStr ->
                        val minerals = json.decodeFromString<List<Mineral>>(jsonStr)

                        // Use transaction to ensure atomicity
                        database.withTransaction {
                            if (mode == ImportMode.REPLACE) {
                                database.mineralDao().deleteAll()
                                database.provenanceDao().deleteAll()
                                database.storageDao().deleteAll()
                                database.photoDao().deleteAll()
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
            }

            Result.success(ImportResult(imported, skipped, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sanitize ZIP entry paths to prevent path traversal attacks.
     *
     * @param entryName The ZIP entry name to sanitize
     * @return The sanitized path, or null if the path is malicious
     */
    private fun sanitizeZipEntryPath(entryName: String): String? {
        // Reject absolute paths
        if (entryName.startsWith("/") || entryName.contains(":")) {
            return null
        }

        // Reject path traversal attempts
        if (entryName.contains("..")) {
            return null
        }

        // Normalize and validate
        val normalized = entryName.replace("\\", "/")
        val parts = normalized.split("/")

        // Check each part for dangerous characters
        if (parts.any { it.isEmpty() || it == "." || it == ".." }) {
            return null
        }

        return normalized
    }
}
