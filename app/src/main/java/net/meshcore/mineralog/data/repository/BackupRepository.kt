package net.meshcore.mineralog.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.meshcore.mineralog.data.crypto.DecryptionException
import net.meshcore.mineralog.data.crypto.PasswordBasedCrypto
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.mapper.toDomain
import net.meshcore.mineralog.data.mapper.toEntity
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

class BackupRepositoryImpl(
    private val context: Context,
    private val database: MineraLogDatabase
) : BackupRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun exportZip(uri: Uri, password: CharArray?): Result<Unit> = withContext(Dispatchers.IO) {
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
                        PasswordBasedCrypto.encrypt(mineralsJsonBytes, password)
                    } else null

                    // manifest.json
                    val manifest = mutableMapOf<String, Any>(
                        "app" to "MineraLog",
                        "schemaVersion" to "1.0.0",
                        "exportedAt" to Instant.now().toString(),
                        "counts" to mapOf(
                            "minerals" to minerals.size,
                            "photos" to minerals.sumOf { it.photos.size }
                        ),
                        "encrypted" to (password != null)
                    )

                    // Add encryption metadata if encrypted
                    if (encryptionResult != null) {
                        manifest["encryption"] = mapOf(
                            "algorithm" to "Argon2id+AES-256-GCM",
                            "salt" to encryptionResult.encodedSalt,
                            "iv" to encryptionResult.encodedIv
                        )
                    }

                    zip.putNextEntry(ZipEntry("manifest.json"))
                    zip.write(json.encodeToString(manifest).toByteArray())
                    zip.closeEntry()

                    // minerals.json (encrypted or plaintext)
                    zip.putNextEntry(ZipEntry("minerals.json"))
                    if (encryptionResult != null) {
                        zip.write(encryptionResult.ciphertext)
                    } else {
                        zip.write(mineralsJsonBytes)
                    }
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

    override suspend fun importZip(uri: Uri, password: CharArray?, mode: ImportMode): Result<ImportResult> = withContext(Dispatchers.IO) {
        // Security constants
        val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100 MB compressed
        val MAX_DECOMPRESSED_SIZE = 500 * 1024 * 1024L // 500 MB decompressed
        val MAX_DECOMPRESSION_RATIO = 100 // Prevent ZIP bombs

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

                // Helper to sanitize ZIP entry paths (prevent path traversal)
                fun sanitizeZipEntryPath(entryName: String): String? {
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

                // Helper to validate schema version
                fun validateSchemaVersion(version: String?): Boolean {
                    // Currently only support v1.0.0
                    return version == "1.0.0"
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
                    if (!validateSchemaVersion(schemaVersion)) {
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

                        // Decrypt - FIXED: Removed double Base64 encoding bug
                        try {
                            val encodedCiphertext = Base64.encodeToString(mineralsBytes, Base64.NO_WRAP)
                            val decryptedBytes = PasswordBasedCrypto.decryptFromBase64(
                                encodedCiphertext = encodedCiphertext,
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

    override suspend fun exportCsv(uri: Uri, minerals: List<Mineral>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    // Write CSV header
                    writer.write("Name,Group,Formula,Streak,Luster,Mohs Min,Mohs Max,")
                    writer.write("Crystal System,Specific Gravity,Cleavage,Fracture,")
                    writer.write("Diaphaneity,Habit,Fluorescence,Radioactive,Magnetic,")
                    writer.write("Dimensions (mm),Weight (g),Status,Status Type,Quality Rating,Completeness,")
                    writer.write("Provenance Country,Provenance Locality,Provenance Site,")
                    writer.write("Provenance Acquired At,Provenance Source,Price,Estimated Value,Currency,")
                    writer.write("Storage Place,Storage Container,Storage Box,Storage Slot,Notes,Tags\n")

                    // Write data rows
                    minerals.forEach { mineral ->
                        writer.write(escapeCSV(mineral.name))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.group ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.formula ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.streak ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.luster ?: ""))
                        writer.write(",")
                        writer.write(mineral.mohsMin?.toString() ?: "")
                        writer.write(",")
                        writer.write(mineral.mohsMax?.toString() ?: "")
                        writer.write(",")
                        writer.write(escapeCSV(mineral.crystalSystem ?: ""))
                        writer.write(",")
                        writer.write(mineral.specificGravity?.toString() ?: "")
                        writer.write(",")
                        writer.write(escapeCSV(mineral.cleavage ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.fracture ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.diaphaneity ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.habit ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.fluorescence ?: ""))
                        writer.write(",")
                        writer.write(mineral.radioactive.toString())
                        writer.write(",")
                        writer.write(mineral.magnetic.toString())
                        writer.write(",")
                        writer.write(escapeCSV(mineral.dimensionsMm ?: ""))
                        writer.write(",")
                        writer.write(mineral.weightGr?.toString() ?: "")
                        writer.write(",")
                        writer.write(escapeCSV(mineral.status))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.statusType))
                        writer.write(",")
                        writer.write(mineral.qualityRating?.toString() ?: "")
                        writer.write(",")
                        writer.write(mineral.completeness.toString())
                        writer.write(",")
                        writer.write(escapeCSV(mineral.provenance?.country ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.provenance?.locality ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.provenance?.site ?: ""))
                        writer.write(",")
                        writer.write(mineral.provenance?.acquiredAt?.toString() ?: "")
                        writer.write(",")
                        writer.write(escapeCSV(mineral.provenance?.source ?: ""))
                        writer.write(",")
                        writer.write(mineral.provenance?.price?.toString() ?: "")
                        writer.write(",")
                        writer.write(mineral.provenance?.estimatedValue?.toString() ?: "")
                        writer.write(",")
                        writer.write(escapeCSV(mineral.provenance?.currency ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.storage?.place ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.storage?.container ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.storage?.box ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.storage?.slot ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.notes ?: ""))
                        writer.write(",")
                        writer.write(escapeCSV(mineral.tags.joinToString("; ")))
                        writer.write("\n")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importCsv(
        uri: Uri,
        columnMapping: Map<String, String>?,
        mode: CsvImportMode
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val errors = mutableListOf<String>()
            var imported = 0
            var skipped = 0

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Parse CSV
                val parser = net.meshcore.mineralog.data.util.CsvParser()
                val parseResult = parser.parse(inputStream)

                if (parseResult.errors.isNotEmpty()) {
                    errors.addAll(parseResult.errors.map { "Line ${it.lineNumber}: ${it.message}" })
                }

                if (parseResult.headers.isEmpty()) {
                    return@withContext Result.failure(Exception("CSV file has no headers"))
                }

                // Use provided column mapping or auto-map headers to domain fields
                val mapping = columnMapping ?: net.meshcore.mineralog.data.util.CsvColumnMapper.mapHeaders(parseResult.headers)

                // Get existing minerals for name-based lookups
                val existingMinerals = database.mineralDao().getAll()
                val existingByName = existingMinerals.associateBy { it.name.lowercase() }

                // Use transaction for atomicity
                database.withTransaction {
                    // Handle REPLACE mode
                    if (mode == CsvImportMode.REPLACE) {
                        database.mineralDao().deleteAll()
                        database.provenanceDao().deleteAll()
                        database.storageDao().deleteAll()
                        database.photoDao().deleteAll()
                    }

                    // Process each row
                    parseResult.rows.forEachIndexed { index, row ->
                        try {
                            // Parse mineral first to get the name
                            val parsedMineral = parseMineralFromCsvRow(
                                row = row,
                                columnMapping = mapping,
                                existingMineral = null
                            )

                            // Check for existing mineral by name
                            val existing = existingByName[parsedMineral.name.lowercase()]

                            // Check for duplicates based on mode
                            when (mode) {
                                CsvImportMode.SKIP_DUPLICATES -> {
                                    if (existing != null) {
                                        skipped++
                                        return@forEachIndexed
                                    }
                                }
                                CsvImportMode.MERGE -> {
                                    // Will reuse existing ID below
                                }
                                CsvImportMode.REPLACE -> {
                                    // Already cleared, just insert
                                }
                            }

                            // For MERGE mode, reuse existing IDs if found
                            val mineral = if (mode == CsvImportMode.MERGE && existing != null) {
                                // Load related entities for existing mineral
                                val existingProvenance = database.provenanceDao().getByMineralId(existing.id)
                                val existingStorage = database.storageDao().getByMineralId(existing.id)
                                val existingPhotos = database.photoDao().getByMineralId(existing.id)

                                parseMineralFromCsvRow(
                                    row = row,
                                    columnMapping = mapping,
                                    existingMineral = existing.toDomain(
                                        existingProvenance?.toDomain(),
                                        existingStorage?.toDomain(),
                                        existingPhotos.map { it.toDomain() }
                                    )
                                )
                            } else {
                                parsedMineral
                            }

                            // Validate required fields
                            if (mineral.name.isBlank()) {
                                errors.add("Row ${index + 2}: Name is required")
                                skipped++
                                return@forEachIndexed
                            }

                            // Insert or update mineral
                            database.mineralDao().insert(mineral.toEntity())

                            // Insert provenance if present
                            mineral.provenance?.let { prov ->
                                database.provenanceDao().insert(prov.toEntity())
                            }

                            // Insert storage if present
                            mineral.storage?.let { storage ->
                                database.storageDao().insert(storage.toEntity())
                            }

                            imported++
                        } catch (e: Exception) {
                            errors.add("Row ${index + 2}: ${e.message}")
                            skipped++
                        }
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to open CSV file"))

            Result.success(ImportResult(imported, skipped, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse a Mineral object from a CSV row using column mapping.
     *
     * @param row The CSV row data
     * @param columnMapping Mapping of CSV headers to domain fields
     * @param existingMineral If provided (MERGE mode), reuse its ID and related entity IDs
     */
    private fun parseMineralFromCsvRow(
        row: Map<String, String>,
        columnMapping: Map<String, String>,
        existingMineral: Mineral?
    ): Mineral {
        // Helper to get mapped value
        fun getMapped(domainField: String): String? {
            val csvHeader = columnMapping.entries.find { it.value == domainField }?.key
            return csvHeader?.let { row[it] }?.takeIf { it.isNotBlank() }
        }

        fun getFloat(domainField: String): Float? {
            return getMapped(domainField)?.toFloatOrNull()
        }

        fun getInt(domainField: String): Int? {
            return getMapped(domainField)?.toIntOrNull()
        }

        fun getDouble(domainField: String): Double? {
            return getMapped(domainField)?.toDoubleOrNull()
        }

        fun getBoolean(domainField: String): Boolean {
            val value = getMapped(domainField)?.lowercase() ?: return false
            return value in listOf("true", "yes", "1", "y", "oui")
        }

        // Generate IDs - reuse existing if provided (MERGE mode)
        val mineralId = existingMineral?.id ?: java.util.UUID.randomUUID().toString()

        // Parse basic mineral fields
        val name = getMapped("name") ?: throw IllegalArgumentException("Name is required")

        // Handle special case: single "mohs" field maps to both min and max
        val mohsMin = getFloat("mohsMin") ?: getFloat("mohs")
        val mohsMax = getFloat("mohsMax") ?: getFloat("mohs")

        // Validate Mohs hardness range (1.0 to 10.0)
        if (mohsMin != null && (mohsMin < 1.0f || mohsMin > 10.0f)) {
            throw IllegalArgumentException("Mohs Min must be between 1.0 and 10.0 (got: $mohsMin)")
        }
        if (mohsMax != null && (mohsMax < 1.0f || mohsMax > 10.0f)) {
            throw IllegalArgumentException("Mohs Max must be between 1.0 and 10.0 (got: $mohsMax)")
        }

        // Parse provenance fields
        val hasProvenance = listOf("prov_country", "prov_locality", "prov_site", "prov_source").any { getMapped(it) != null }
        val provenance = if (hasProvenance) {
            // Parse and validate coordinates
            val latitude = getDouble("prov_latitude")
            val longitude = getDouble("prov_longitude")

            // Validate latitude range (-90.0 to 90.0)
            if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
                throw IllegalArgumentException("Latitude must be between -90.0 and 90.0 (got: $latitude)")
            }

            // Validate longitude range (-180.0 to 180.0)
            if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
                throw IllegalArgumentException("Longitude must be between -180.0 and 180.0 (got: $longitude)")
            }

            net.meshcore.mineralog.domain.model.Provenance(
                id = existingMineral?.provenance?.id ?: java.util.UUID.randomUUID().toString(),
                mineralId = mineralId,
                country = getMapped("prov_country"),
                locality = getMapped("prov_locality"),
                site = getMapped("prov_site"),
                latitude = latitude,
                longitude = longitude,
                source = getMapped("prov_source"),
                price = getFloat("prov_price"),
                estimatedValue = getFloat("prov_estimatedValue"),
                currency = getMapped("prov_currency") ?: "USD"
            )
        } else null

        // Parse storage fields
        val hasStorage = listOf("storage_place", "storage_container", "storage_box", "storage_slot").any { getMapped(it) != null }
        val storage = if (hasStorage) {
            net.meshcore.mineralog.domain.model.Storage(
                id = existingMineral?.storage?.id ?: java.util.UUID.randomUUID().toString(),
                mineralId = mineralId,
                place = getMapped("storage_place"),
                container = getMapped("storage_container"),
                box = getMapped("storage_box"),
                slot = getMapped("storage_slot")
            )
        } else null

        return Mineral(
            id = mineralId,
            name = name,
            group = getMapped("group"),
            formula = getMapped("formula"),
            crystalSystem = getMapped("crystalSystem"),
            mohsMin = mohsMin,
            mohsMax = mohsMax,
            cleavage = getMapped("cleavage"),
            fracture = getMapped("fracture"),
            luster = getMapped("luster"),
            streak = getMapped("streak"),
            diaphaneity = getMapped("diaphaneity"),
            habit = getMapped("habit"),
            specificGravity = getFloat("specificGravity"),
            fluorescence = getMapped("fluorescence"),
            magnetic = getBoolean("magnetic"),
            radioactive = getBoolean("radioactive"),
            dimensionsMm = getMapped("dimensionsMm"),
            weightGr = getFloat("weightGr"),
            status = getMapped("status") ?: "incomplete",
            statusType = getMapped("statusType") ?: "in_collection",
            qualityRating = getInt("qualityRating"),
            completeness = getInt("completeness") ?: 0,
            notes = getMapped("notes"),
            tags = getMapped("tags")?.split(";")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            provenance = provenance,
            storage = storage,
            photos = emptyList() // CSV doesn't include photos
        )
    }

    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
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
