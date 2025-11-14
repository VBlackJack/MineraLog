package net.meshcore.mineralog.data.service

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.mapper.toDomain
import net.meshcore.mineralog.data.mapper.toEntity
import net.meshcore.mineralog.data.repository.CsvImportMode
import net.meshcore.mineralog.data.repository.ImportResult
import net.meshcore.mineralog.domain.model.Mineral

/**
 * Service responsible for CSV backup operations (export and import).
 * Extracted from BackupRepository for better separation of concerns.
 */
class CsvBackupService(
    private val context: Context,
    private val database: MineraLogDatabase,
    private val csvMapper: MineralCsvMapper
) {

    /**
     * Export minerals to CSV file.
     *
     * @param uri The URI to write the CSV file to
     * @param minerals The list of minerals to export
     * @return Result indicating success or failure
     */
    suspend fun exportCsv(uri: Uri, minerals: List<Mineral>): Result<Unit> = withContext(Dispatchers.IO) {
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
                        writer.write(csvMapper.escapeCSV(mineral.name))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.group ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.formula ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.streak ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.luster ?: ""))
                        writer.write(",")
                        writer.write(mineral.mohsMin?.toString() ?: "")
                        writer.write(",")
                        writer.write(mineral.mohsMax?.toString() ?: "")
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.crystalSystem ?: ""))
                        writer.write(",")
                        writer.write(mineral.specificGravity?.toString() ?: "")
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.cleavage ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.fracture ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.diaphaneity ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.habit ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.fluorescence ?: ""))
                        writer.write(",")
                        writer.write(mineral.radioactive.toString())
                        writer.write(",")
                        writer.write(mineral.magnetic.toString())
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.dimensionsMm ?: ""))
                        writer.write(",")
                        writer.write(mineral.weightGr?.toString() ?: "")
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.status))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.statusType))
                        writer.write(",")
                        writer.write(mineral.qualityRating?.toString() ?: "")
                        writer.write(",")
                        writer.write(mineral.completeness.toString())
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.provenance?.country ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.provenance?.locality ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.provenance?.site ?: ""))
                        writer.write(",")
                        writer.write(mineral.provenance?.acquiredAt?.toString() ?: "")
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.provenance?.source ?: ""))
                        writer.write(",")
                        writer.write(mineral.provenance?.price?.toString() ?: "")
                        writer.write(",")
                        writer.write(mineral.provenance?.estimatedValue?.toString() ?: "")
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.provenance?.currency ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.storage?.place ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.storage?.container ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.storage?.box ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.storage?.slot ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.notes ?: ""))
                        writer.write(",")
                        writer.write(csvMapper.escapeCSV(mineral.tags.joinToString("; ")))
                        writer.write("\n")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import minerals from CSV file.
     *
     * @param uri The URI of the CSV file to import
     * @param columnMapping Optional mapping of CSV headers to domain fields
     * @param mode The import mode (MERGE, REPLACE, SKIP_DUPLICATES)
     * @return Result containing import statistics
     */
    suspend fun importCsv(
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
                            val parsedMineral = csvMapper.parseMineralFromCsvRow(
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

                                csvMapper.parseMineralFromCsvRow(
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
}
