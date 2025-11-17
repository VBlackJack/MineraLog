package net.meshcore.mineralog.data.service

import android.content.Context
import net.meshcore.mineralog.util.AppLogger
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.meshcore.mineralog.data.crypto.DecryptionException
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.data.mapper.toDomain
import net.meshcore.mineralog.data.mapper.toEntity
import net.meshcore.mineralog.data.model.BackupManifest
import net.meshcore.mineralog.data.repository.ImportMode
import net.meshcore.mineralog.data.repository.ImportResult
import net.meshcore.mineralog.domain.model.Mineral
import java.io.File
import java.util.UUID
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

    private val referenceMineralCsvMapper = ReferenceMineralCsvMapper()

    // Security constants
    private val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100 MB compressed
    private val MAX_DECOMPRESSED_SIZE = 500 * 1024 * 1024L // 500 MB decompressed
    private val MAX_DECOMPRESSION_RATIO = 100 // Prevent ZIP bombs
    private val MAX_ENTRY_SIZE = 10 * 1024 * 1024L // 10 MB per individual entry to prevent OOM attacks

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
            val mineralEntities = database.mineralBasicDao().getAll()
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

                    // Write reference_minerals.csv if any exist
                    val referenceMinerals = database.referenceMineralDao().getAllFlow().first()
                    if (referenceMinerals.isNotEmpty()) {
                        zip.putNextEntry(ZipEntry("reference_minerals.csv"))
                        val csvContent = buildReferenceMineralsCsv(referenceMinerals)
                        zip.write(csvContent.toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                    }

                    // Write media files - BUGFIX: Photos are stored in photos/ subdirectory
                    val photosDir = File(context.filesDir, "photos")
                    minerals.forEach { mineral ->
                        mineral.photos.forEach { photo ->
                            val photoFile = File(photosDir, photo.fileName)
                            if (photoFile.exists()) {
                                // Store in ZIP with photos/ prefix to maintain structure
                                zip.putNextEntry(ZipEntry("photos/${photo.fileName}"))
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

                    var referenceMineralsCsvContent: String? = null

                    // First pass: read manifest, minerals.json, and reference_minerals.csv
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

                            // Check decompression ratio (ZIP bomb protection)
                            if (totalCompressedBytes > 0 && totalDecompressedBytes > 0) {
                                val ratio = totalDecompressedBytes.toDouble() / totalCompressedBytes.toDouble()
                                if (ratio > MAX_DECOMPRESSION_RATIO) {
                                    val message = "Potential ZIP bomb detected: decompression ratio %.1f:1 exceeds limit of %d:1"
                                        .format(ratio, MAX_DECOMPRESSION_RATIO)
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

                        // Security: Check individual entry size before reading into memory
                        if (entryUncompressedSize > MAX_ENTRY_SIZE) {
                            val entryMB = entryUncompressedSize / 1024 / 1024
                            val maxMB = MAX_ENTRY_SIZE / 1024 / 1024
                            errors.add("Skipped entry '$sanitizedPath': size ${entryMB}MB exceeds ${maxMB}MB limit")
                            zip.closeEntry()
                            entry = zip.nextEntry
                            continue
                        }

                        when {
                            sanitizedPath == "manifest.json" -> {
                                manifestJson = zip.readBytes().toString(Charsets.UTF_8)
                            }
                            sanitizedPath == "minerals.json" -> {
                                mineralsBytes = zip.readBytes()
                            }
                            sanitizedPath == "reference_minerals.csv" -> {
                                referenceMineralsCsvContent = zip.readBytes().toString(Charsets.UTF_8)
                            }
                            sanitizedPath.startsWith("photos/") || sanitizedPath.startsWith("media/") -> {
                                // BUGFIX: Support both photos/ (new format) and media/ (legacy format)
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
                    val manifest = manifestJson?.let { json.decodeFromString<BackupManifest>(it) }

                    // Security: Validate schema version
                    if (!encryptionService.validateSchemaVersion(manifest?.schemaVersion)) {
                        val message = "Incompatible backup schema version: ${manifest?.schemaVersion}. " +
                            "Only version 1.0.0 is supported."
                        return@withContext Result.failure(Exception(message))
                    }
                    val isEncrypted = manifest?.encrypted ?: false

                    // Decrypt minerals.json if necessary
                    val mineralsJson = if (isEncrypted) {
                        if (password == null) {
                            return@withContext Result.failure(Exception("This backup is encrypted. Please provide a password."))
                        }

                        // Extract encryption metadata
                        val encryptionMetadata = manifest?.encryption
                            ?: return@withContext Result.failure(Exception("Encrypted backup is missing encryption metadata"))

                        val encodedSalt = encryptionMetadata.salt
                        val encodedIv = encryptionMetadata.iv

                        // Decrypt
                        try {
                            if (mineralsBytes == null) {
                                return@withContext Result.failure(Exception("Missing minerals.json in encrypted backup"))
                            }
                            val decryptedBytes = encryptionService.decrypt(
                                ciphertext = mineralsBytes,
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
                                database.mineralBasicDao().deleteAll()
                                database.provenanceDao().deleteAll()
                                database.storageDao().deleteAll()
                                database.photoDao().deleteAll()
                            }

                            minerals.forEach { mineral ->
                                try {
                                    database.mineralBasicDao().insert(mineral.toEntity())
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

                        // Import reference minerals if present
                        referenceMineralsCsvContent?.let { csvContent ->
                            try {
                                val (referenceMinerals, csvErrors) = parseReferenceMineralsCsv(csvContent)
                                errors.addAll(csvErrors.map { "Reference minerals CSV: $it" })

                                if (referenceMinerals.isNotEmpty()) {
                                    val (refImported, refErrors) = importReferenceMinerals(referenceMinerals, mode)
                                    errors.addAll(refErrors.map { "Reference minerals: $it" })
                                    // Note: refImported is not added to main import count to keep it separate
                                    AppLogger.i("ZipBackupService", "Imported $refImported reference minerals")
                                }
                            } catch (e: Exception) {
                                errors.add("Failed to import reference minerals: ${e.message}")
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

    /**
     * Build a CSV string from a list of reference minerals.
     *
     * @param minerals The list of reference minerals to export
     * @return CSV string with headers and data rows
     */
    private fun buildReferenceMineralsCsv(minerals: List<ReferenceMineralEntity>): String {
        val csv = StringBuilder()

        // Write header row
        val headers = ReferenceMineralCsvMapper.getHeaders()
        csv.appendLine(headers.joinToString(",") { referenceMineralCsvMapper.escapeCsvValue(it) })

        // Write data rows
        minerals.forEach { mineral ->
            val row = referenceMineralCsvMapper.toCsvRow(mineral)
            csv.appendLine(row.joinToString(",") { referenceMineralCsvMapper.escapeCsvValue(it) })
        }

        return csv.toString()
    }

    /**
     * Parse a CSV string into a list of reference minerals.
     *
     * @param csvContent The CSV content to parse
     * @return Pair of (successfully parsed minerals, list of errors)
     */
    private fun parseReferenceMineralsCsv(csvContent: String): Pair<List<ReferenceMineralEntity>, List<String>> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return Pair(emptyList(), listOf("CSV file is empty"))
        }

        val errors = mutableListOf<String>()
        val minerals = mutableListOf<ReferenceMineralEntity>()

        // Parse header
        val headerLine = lines.first()
        val headers = parseCsvLine(headerLine)

        // Parse data rows
        lines.drop(1).forEachIndexed { index, line ->
            try {
                val values = parseCsvLine(line)
                val mineral = referenceMineralCsvMapper.fromCsvRow(values, headers)
                if (mineral != null) {
                    minerals.add(mineral)
                } else {
                    errors.add("Line ${index + 2}: Failed to parse mineral")
                }
            } catch (e: Exception) {
                errors.add("Line ${index + 2}: ${e.message}")
            }
        }

        return Pair(minerals, errors)
    }

    /**
     * Parse a CSV line, handling quoted values correctly.
     *
     * @param line The CSV line to parse
     * @return List of cell values
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            val char = line[i]

            when {
                char == '"' -> {
                    // Check for escaped quote ("")
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        // Skip next quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }

        // Add last field
        result.add(current.toString())

        return result.map { referenceMineralCsvMapper.unescapeCsvValue(it) }
    }

    /**
     * Import reference minerals with conflict resolution.
     *
     * @param minerals The minerals to import
     * @param mode The import mode
     * @return Pair of (imported count, list of errors)
     */
    private suspend fun importReferenceMinerals(
        minerals: List<ReferenceMineralEntity>,
        mode: ImportMode
    ): Pair<Int, List<String>> {
        val errors = mutableListOf<String>()
        var imported = 0

        try {
            database.withTransaction {
                when (mode) {
                    ImportMode.REPLACE -> {
                        // Delete all existing reference minerals
                        database.referenceMineralDao().deleteAll()
                        // Insert all imported minerals
                        minerals.forEach { mineral ->
                            try {
                                database.referenceMineralDao().insert(mineral)
                                imported++
                            } catch (e: Exception) {
                                errors.add("Failed to import ${mineral.nameFr}: ${e.message}")
                            }
                        }
                    }

                    ImportMode.MERGE -> {
                        // Upsert by ID: update existing, insert new
                        minerals.forEach { mineral ->
                            try {
                                val existing = database.referenceMineralDao().getById(mineral.id)
                                if (existing != null) {
                                    database.referenceMineralDao().update(mineral)
                                } else {
                                    database.referenceMineralDao().insert(mineral)
                                }
                                imported++
                            } catch (e: Exception) {
                                errors.add("Failed to import ${mineral.nameFr}: ${e.message}")
                            }
                        }
                    }

                    ImportMode.MAP_IDS -> {
                        // Remap conflicting IDs
                        val idMapping = mutableMapOf<String, String>()

                        minerals.forEach { mineral ->
                            try {
                                val existing = database.referenceMineralDao().getById(mineral.id)
                                val finalMineral = if (existing != null) {
                                    // ID conflict: generate new ID
                                    val newId = UUID.randomUUID().toString()
                                    idMapping[mineral.id] = newId
                                    mineral.copy(id = newId)
                                } else {
                                    mineral
                                }

                                database.referenceMineralDao().insert(finalMineral)
                                imported++
                            } catch (e: Exception) {
                                errors.add("Failed to import ${mineral.nameFr}: ${e.message}")
                            }
                        }

                        // Update linked entities (simple_properties, mineral_components)
                        // This would be done in a real implementation
                        // For now, we'll leave the linking as-is since it's complex
                        if (idMapping.isNotEmpty()) {
                            errors.add("Warning: ${idMapping.size} ID(s) were remapped. " +
                                "Links to specimens may be broken.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("Transaction failed: ${e.message}")
        }

        return Pair(imported, errors)
    }
}
