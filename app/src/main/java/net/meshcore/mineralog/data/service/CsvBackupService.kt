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
import java.io.IOException
import java.util.Locale

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
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(IOException("Failed to open CSV output stream for $uri"))

            outputStream.bufferedWriter().use { writer ->
                // Export the full schema (aggregates, provenance metadata, storage identifiers) to keep backups lossless over time
                writer.appendLine(
                    listOf(
                        "Mineral Type",
                        "Name",
                        "Group",
                        "Formula",
                        "Streak",
                        "Luster",
                        "Mohs Min",
                        "Mohs Max",
                        "Crystal System",
                        "Specific Gravity",
                        "Cleavage",
                        "Fracture",
                        "Diaphaneity",
                        "Habit",
                        "Fluorescence",
                        "Radioactive",
                        "Magnetic",
                        "Dimensions (mm)",
                        "Weight (g)",
                        "Rock Type",
                        "Texture",
                        "Dominant Minerals",
                        "Interesting Features",
                        "Status",
                        "Status Type",
                        "Status Details",
                        "Quality Rating",
                        "Completeness",
                        "Provenance Country",
                        "Provenance Locality",
                        "Provenance Site",
                        "Provenance Latitude",
                        "Provenance Longitude",
                        "Provenance Acquired At",
                        "Provenance Source",
                        "Price",
                        "Estimated Value",
                        "Currency",
                        "Provenance Mine Name",
                        "Provenance Collector",
                        "Provenance Dealer",
                        "Provenance Catalog Number",
                        "Provenance Acquisition Notes",
                        "Storage Place",
                        "Storage Container",
                        "Storage Box",
                        "Storage Slot",
                        "Storage NFC Tag",
                        "Storage QR Content",
                        "Notes",
                        "Tags",
                        "Component Names",
                        "Component Percentages",
                        "Component Roles"
                    ).joinToString(",")
                )

                minerals.forEach { mineral ->
                    val provenance = mineral.provenance
                    val storage = mineral.storage
                    // Persist aggregate composition so spreadsheets reflect what the user sees in-app
                    val validComponents = mineral.components.filter { it.mineralName.isNotBlank() }
                    val componentNames = validComponents.joinToString("; ") { it.mineralName.trim() }
                    val componentPercentages = validComponents.joinToString("; ") { component ->
                        component.percentage?.let { String.format(Locale.ROOT, "%.2f", it) } ?: ""
                    }
                    val componentRoles = validComponents.joinToString("; ") { component ->
                        component.role.name
                    }

                    val columns = listOf(
                        csvMapper.escapeCSV(mineral.mineralType.name),
                        csvMapper.escapeCSV(mineral.name),
                        csvMapper.escapeCSV(mineral.group ?: ""),
                        csvMapper.escapeCSV(mineral.formula ?: ""),
                        csvMapper.escapeCSV(mineral.streak ?: ""),
                        csvMapper.escapeCSV(mineral.luster ?: ""),
                        mineral.mohsMin?.toString() ?: "",
                        mineral.mohsMax?.toString() ?: "",
                        csvMapper.escapeCSV(mineral.crystalSystem ?: ""),
                        mineral.specificGravity?.toString() ?: "",
                        csvMapper.escapeCSV(mineral.cleavage ?: ""),
                        csvMapper.escapeCSV(mineral.fracture ?: ""),
                        csvMapper.escapeCSV(mineral.diaphaneity ?: ""),
                        csvMapper.escapeCSV(mineral.habit ?: ""),
                        csvMapper.escapeCSV(mineral.fluorescence ?: ""),
                        mineral.radioactive.toString(),
                        mineral.magnetic.toString(),
                        csvMapper.escapeCSV(mineral.dimensionsMm ?: ""),
                        mineral.weightGr?.toString() ?: "",
                        csvMapper.escapeCSV(mineral.rockType ?: ""),
                        csvMapper.escapeCSV(mineral.texture ?: ""),
                        csvMapper.escapeCSV(mineral.dominantMinerals ?: ""),
                        csvMapper.escapeCSV(mineral.interestingFeatures ?: ""),
                        csvMapper.escapeCSV(mineral.status),
                        csvMapper.escapeCSV(mineral.statusType),
                        csvMapper.escapeCSV(mineral.statusDetails ?: ""),
                        mineral.qualityRating?.toString() ?: "",
                        mineral.completeness.toString(),
                        csvMapper.escapeCSV(provenance?.country ?: ""),
                        csvMapper.escapeCSV(provenance?.locality ?: ""),
                        csvMapper.escapeCSV(provenance?.site ?: ""),
                        provenance?.latitude?.toString() ?: "",
                        provenance?.longitude?.toString() ?: "",
                        provenance?.acquiredAt?.toString() ?: "",
                        csvMapper.escapeCSV(provenance?.source ?: ""),
                        provenance?.price?.toString() ?: "",
                        provenance?.estimatedValue?.toString() ?: "",
                        csvMapper.escapeCSV(provenance?.currency ?: ""),
                        csvMapper.escapeCSV(provenance?.mineName ?: ""),
                        csvMapper.escapeCSV(provenance?.collectorName ?: ""),
                        csvMapper.escapeCSV(provenance?.dealer ?: ""),
                        csvMapper.escapeCSV(provenance?.catalogNumber ?: ""),
                        csvMapper.escapeCSV(provenance?.acquisitionNotes ?: ""),
                        csvMapper.escapeCSV(storage?.place ?: ""),
                        csvMapper.escapeCSV(storage?.container ?: ""),
                        csvMapper.escapeCSV(storage?.box ?: ""),
                        csvMapper.escapeCSV(storage?.slot ?: ""),
                        csvMapper.escapeCSV(storage?.nfcTagId ?: ""),
                        csvMapper.escapeCSV(storage?.qrContent ?: ""),
                        csvMapper.escapeCSV(mineral.notes ?: ""),
                        csvMapper.escapeCSV(mineral.tags.joinToString("; ")),
                        csvMapper.escapeCSV(componentNames),
                        csvMapper.escapeCSV(componentPercentages),
                        csvMapper.escapeCSV(componentRoles)
                    )

                    writer.appendLine(columns.joinToString(","))
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

            if (mode == CsvImportMode.REPLACE) {
                database.withTransaction {
                    database.mineralBasicDao().deleteAll()
                    database.provenanceDao().deleteAll()
                    database.storageDao().deleteAll()
                    database.photoDao().deleteAll()
                    database.mineralComponentDao().deleteAll()
                }
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val parser = net.meshcore.mineralog.data.util.CsvParser()
                val stagedMinerals = mutableMapOf<String, Mineral>()
                var resolvedMapping: Map<String, String>? = columnMapping

                val streamingResult = parser.parseStream(
                    inputStream = inputStream,
                    onBegin = { headers, _, _ ->
                        if (resolvedMapping == null) {
                            resolvedMapping = net.meshcore.mineralog.data.util.CsvColumnMapper.mapHeaders(headers)
                        }
                    }
                ) { _, row, lineNumber ->
                    val mapping = resolvedMapping
                        ?: throw IllegalStateException("Column mapping was not initialized before processing rows")

                    try {
                        val parsedMineral = csvMapper.parseMineralFromCsvRow(
                            row = row,
                            columnMapping = mapping,
                            existingMineral = null
                        )

                        val normalizedName = normalizeName(parsedMineral.name)
                        if (normalizedName.isEmpty()) {
                            errors.add("Line $lineNumber: Name is required")
                            skipped++
                            return@parseStream
                        }

                        val stagedExisting = stagedMinerals[normalizedName]
                        val existingEntity = when {
                            mode == CsvImportMode.REPLACE -> null
                            stagedExisting != null -> null
                            else -> database.mineralBasicDao().getByNormalizedName(normalizedName)
                        }

                        when (mode) {
                            CsvImportMode.SKIP_DUPLICATES -> {
                                if (existingEntity != null || stagedExisting != null) {
                                    skipped++
                                    return@parseStream
                                }
                            }
                            CsvImportMode.MERGE, CsvImportMode.REPLACE -> Unit
                        }

                        val existingDomain = if (mode == CsvImportMode.MERGE) {
                            when {
                                stagedExisting != null -> stagedExisting
                                existingEntity != null -> {
                                    val provenance = database.provenanceDao().getByMineralId(existingEntity.id)
                                    val storage = database.storageDao().getByMineralId(existingEntity.id)
                                    val photos = database.photoDao().getByMineralId(existingEntity.id)
                                    val components = database.mineralComponentDao().getByAggregateId(existingEntity.id)
                                    existingEntity.toDomain(
                                        provenance,
                                        storage,
                                        photos,
                                        components
                                    )
                                }
                                else -> null
                            }
                        } else {
                            null
                        }

                        val mineral = if (existingDomain != null) {
                            csvMapper.parseMineralFromCsvRow(
                                row = row,
                                columnMapping = mapping,
                                existingMineral = existingDomain
                            )
                        } else {
                            parsedMineral
                        }

                        if (mineral.name.isBlank()) {
                            errors.add("Line $lineNumber: Name is required")
                            skipped++
                            return@parseStream
                        }

                        val componentEntities = mineral.components
                            .filter { it.mineralName.isNotBlank() }
                            .mapIndexed { index, component ->
                                component.toEntity(mineral.id, index)
                            }
                        val existingComponents = existingDomain?.components ?: emptyList()

                        database.withTransaction {
                            database.mineralBasicDao().insert(mineral.toEntity())
                            mineral.provenance?.let { database.provenanceDao().insert(it.toEntity()) }
                            mineral.storage?.let { database.storageDao().insert(it.toEntity()) }

                            when {
                                componentEntities.isNotEmpty() -> {
                                    database.mineralComponentDao().deleteByAggregateId(mineral.id)
                                    database.mineralComponentDao().insertAll(componentEntities)
                                }
                                existingComponents.isNotEmpty() -> {
                                    database.mineralComponentDao().deleteByAggregateId(mineral.id)
                                }
                            }
                        }

                        stagedMinerals[normalizedName] = mineral
                        imported++
                    } catch (e: Exception) {
                        errors.add("Line $lineNumber: ${e.message}")
                        skipped++
                    }
                }

                if (streamingResult.headers.isEmpty()) {
                    return@withContext Result.failure(Exception("CSV file has no headers"))
                }

                if (resolvedMapping == null) {
                    return@withContext Result.failure(Exception("Failed to resolve CSV column mapping"))
                }

                if (streamingResult.errors.isNotEmpty()) {
                    errors.addAll(streamingResult.errors.map { "Line ${it.lineNumber}: ${it.message}" })
                }
            } ?: return@withContext Result.failure(Exception("Failed to open CSV file"))

            Result.success(ImportResult(imported, skipped, errors))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeName(name: String): String = name.trim().lowercase(Locale.ROOT)
}
