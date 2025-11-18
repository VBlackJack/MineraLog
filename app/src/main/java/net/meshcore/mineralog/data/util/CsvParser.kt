package net.meshcore.mineralog.data.util

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.text.Charsets

/**
 * Robust CSV parser with encoding detection and flexible column mapping.
 *
 * Features:
 * - Auto-detects UTF-8, Latin-1, and Windows-1252 encodings
 * - Handles quoted fields with embedded commas, quotes, and newlines
 * - RFC 4180 compliant parsing
 * - Flexible delimiter detection (comma, semicolon, tab)
 * - Memory-efficient streaming for large files
 */
class CsvParser {

    /**
     * Parse result containing headers, data rows, and metadata.
     */
    data class ParseResult(
        val headers: List<String>,
        val rows: List<Map<String, String>>,
        val encoding: Charset,
        val delimiter: Char,
        val lineCount: Int,
        val errors: List<ParseError> = emptyList()
    )

    data class ParseError(
        val lineNumber: Int,
        val message: String
    )

    /**
     * Parse CSV from input stream.
     *
     * @param inputStream The CSV input stream (must support mark/reset)
     * @param maxRows Maximum rows to parse (0 = all rows). Useful for preview.
     * @return ParseResult with parsed data
     */
    suspend fun parse(inputStream: InputStream, maxRows: Int = 0): ParseResult = withContext(Dispatchers.IO) {
        // Ensure stream supports mark/reset
        val bufferedStream = if (inputStream.markSupported()) {
            inputStream
        } else {
            inputStream.buffered()
        }

        // Detect encoding (uses mark/reset internally)
        val encoding = detectEncoding(bufferedStream)

        // Create reader with detected encoding
        val reader = BufferedReader(InputStreamReader(bufferedStream, encoding))

        // Read first record (headers) using RFC-compliant reader to support multiline headers
        val headerRecord = readRecord(reader) ?: return@withContext ParseResult(
            headers = emptyList(),
            rows = emptyList(),
            encoding = encoding,
            delimiter = ',',
            lineCount = 0,
            errors = listOf(ParseError(0, "Empty CSV file"))
        )

        val delimiter = detectDelimiter(headerRecord.text)
        val headers = parseLine(headerRecord.text, delimiter)

        // Parse data rows
        val rows = mutableListOf<Map<String, String>>()
        val errors = mutableListOf<ParseError>()
        var consumedLines = headerRecord.physicalLines

        while (true) {
            val record = try {
                readRecord(reader)
            } catch (e: IllegalArgumentException) {
                errors.add(ParseError(consumedLines + 1, e.message ?: "Malformed CSV record"))
                break
            } ?: break

            val recordStartLine = consumedLines + 1
            val line = record.text

            try {
                if (line.isEmpty()) {
                    consumedLines += record.physicalLines
                    continue
                }

                val values = parseLine(line, delimiter)

                // Create map from headers to values
                // For duplicate headers, first occurrence wins
                val rowMap = mutableMapOf<String, String>()
                headers.forEachIndexed { index, header ->
                    if (!rowMap.containsKey(header)) {
                        rowMap[header] = values.getOrNull(index) ?: ""
                    }
                }

                rows.add(rowMap)
            } catch (e: Exception) {
                errors.add(ParseError(recordStartLine, "Parse error: ${e.message}"))
            }

            consumedLines += record.physicalLines
            if (maxRows > 0 && rows.size >= maxRows) break
        }

        ParseResult(
            headers = headers.map { it.trim() },
            rows = rows,
            encoding = encoding,
            delimiter = delimiter,
            lineCount = consumedLines,
            errors = errors
        )
    }

    private data class CsvRecord(val text: String, val physicalLines: Int)

    private fun readRecord(reader: BufferedReader): CsvRecord? {
        val firstLine = reader.readLine() ?: return null
        var linesRead = 1
        val builder = StringBuilder(firstLine)
        var inQuotes = updateQuoteState(firstLine)

        while (inQuotes) {
            val nextLine = reader.readLine()
                ?: throw IllegalArgumentException("Unclosed quoted field at end of file")
            builder.append('\n').append(nextLine)
            linesRead++
            inQuotes = updateQuoteState(nextLine, inQuotes)
        }

        return CsvRecord(builder.toString(), linesRead)
    }

    private fun updateQuoteState(chunk: CharSequence, initialState: Boolean = false): Boolean {
        var inQuotes = initialState
        var i = 0
        while (i < chunk.length) {
            val ch = chunk[i]
            if (ch == '"') {
                if (i + 1 < chunk.length && chunk[i + 1] == '"') {
                    i++ // Skip escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            }
            i++
        }
        return inQuotes
    }

    /**
     * Detect character encoding by checking for BOM and common patterns.
     * Stream must support mark/reset.
     * Skips BOM bytes if found.
     */
    private fun detectEncoding(inputStream: InputStream): Charset {
        require(inputStream.markSupported()) { "InputStream must support mark/reset for encoding detection" }

        // First, look for a BOM and skip it if present
        inputStream.mark(4096)
        val bom = ByteArray(4)
        val read = inputStream.read(bom)
        inputStream.reset()

        val bomEncoding = when {
            read >= 3 && bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && bom[2] == 0xBF.toByte() -> StandardCharsets.UTF_8 to 3
            read >= 2 && bom[0] == 0xFE.toByte() && bom[1] == 0xFF.toByte() -> Charsets.UTF_16BE to 2
            read >= 2 && bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte() -> Charsets.UTF_16LE to 2
            else -> null
        }

        if (bomEncoding != null) {
            val (encoding, bomLength) = bomEncoding
            inputStream.skip(bomLength.toLong())
            return encoding
        }

        // No BOM: sample the stream to differentiate UTF-8 from Latin-1/Windows-1252
        inputStream.mark(8192)
        val sampleBuffer = ByteArray(4096)
        val sampleLength = inputStream.read(sampleBuffer)
        inputStream.reset()

        if (sampleLength > 0) {
            val looksUtf8 = looksLikeUtf8(sampleBuffer, sampleLength)
            if (looksUtf8) {
                return StandardCharsets.UTF_8
            }

            if (containsExtendedAscii(sampleBuffer, sampleLength)) {
                // Prefer Windows-1252 over ISO-8859-1 for broader glyph coverage
                return Charsets.WINDOWS_1252
            }
        }

        // Default to UTF-8 (most common for modern CSVs)
        return StandardCharsets.UTF_8
    }

    private fun looksLikeUtf8(buffer: ByteArray, length: Int): Boolean {
        var i = 0
        while (i < length) {
            val byte = buffer[i].toInt() and 0xFF
            when {
                byte < 0x80 -> i++ // ASCII
                byte in 0xC2..0xDF -> {
                    if (i + 1 >= length || !isContinuationByte(buffer[i + 1])) return false
                    i += 2
                }
                byte in 0xE0..0xEF -> {
                    if (i + 2 >= length || !isContinuationByte(buffer[i + 1]) || !isContinuationByte(buffer[i + 2])) return false
                    i += 3
                }
                byte in 0xF0..0xF4 -> {
                    if (
                        i + 3 >= length ||
                        !isContinuationByte(buffer[i + 1]) ||
                        !isContinuationByte(buffer[i + 2]) ||
                        !isContinuationByte(buffer[i + 3])
                    ) return false
                    i += 4
                }
                else -> return false
            }
        }
        return true
    }

    private fun isContinuationByte(byte: Byte): Boolean {
        val value = byte.toInt() and 0xFF
        return value in 0x80..0xBF
    }

    private fun containsExtendedAscii(buffer: ByteArray, length: Int): Boolean {
        for (i in 0 until length) {
            if (buffer[i].toInt() and 0xFF >= 0x80) {
                return true
            }
        }
        return false
    }

    /**
     * Detect CSV delimiter by analyzing the first line.
     * Checks for comma, semicolon, tab in order of preference.
     */
    private fun detectDelimiter(firstLine: String): Char {
        val delimiters = listOf(',', ';', '\t')

        // Count occurrences of each delimiter (ignoring quoted sections)
        val counts = delimiters.associateWith { delimiter ->
            countDelimiterOccurrences(firstLine, delimiter)
        }

        // Return delimiter with highest count (prefer comma in case of tie)
        return counts.maxByOrNull { it.value }?.key ?: ','
    }

    /**
     * Count delimiter occurrences outside of quoted sections.
     */
    private fun countDelimiterOccurrences(line: String, delimiter: Char): Int {
        var count = 0
        var inQuotes = false

        for (i in line.indices) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    // Handle escaped quotes ("")
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        // Skip next quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> count++
            }
        }

        return count
    }

    /**
     * Parse a single CSV line respecting quoted fields.
     * Handles RFC 4180 rules:
     * - Fields with commas must be quoted
     * - Quotes within fields are escaped as ""
     * - Newlines within quoted fields are preserved
     * - Quoted fields preserve whitespace, unquoted fields are trimmed
     */
    private fun parseLine(line: String, delimiter: Char): List<String> {
        val fields = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var wasQuoted = false
        var i = 0

        while (i < line.length) {
            val ch = line[i]

            when {
                // Handle quotes
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        // Escaped quote: ""
                        currentField.append('"')
                        i++ // Skip next quote
                    } else {
                        // Toggle quote mode
                        inQuotes = !inQuotes
                        wasQuoted = true
                    }
                }

                // Handle delimiter
                ch == delimiter && !inQuotes -> {
                    // Trim unquoted fields, preserve quoted fields
                    val fieldValue = if (wasQuoted) {
                        currentField.toString()
                    } else {
                        currentField.toString().trim()
                    }
                    fields.add(fieldValue)
                    currentField.clear()
                    wasQuoted = false
                }

                // Regular character
                else -> {
                    currentField.append(ch)
                }
            }

            i++
        }

        // Add final field (trim if unquoted)
        val finalField = if (wasQuoted) {
            currentField.toString()
        } else {
            currentField.toString().trim()
        }
        fields.add(finalField)

        return fields
    }
}

/**
 * Column mapper for flexible CSV import.
 * Maps CSV headers to domain model fields with fuzzy matching.
 */
object CsvColumnMapper {

    /**
     * Standard column mappings from various CSV header formats to domain fields.
     * Supports multiple variations of the same field name.
     */
    private val columnMappings = mapOf(
        // Basic fields
        "name" to listOf("name", "mineral_name", "specimen_name", "mineral"),
        "group" to listOf("group", "mineral_group", "classification"),
        "formula" to listOf("formula", "chemical_formula", "composition"),
        "mineralType" to listOf("mineral_type", "mineral type", "type de minéral", "specimen_type"),
        "crystalSystem" to listOf("crystal_system", "crystal system", "crystallography", "system"),

        // Physical properties
        "mohsMin" to listOf("mohs_min", "mohs min", "hardness_min", "hardness min"),
        "mohsMax" to listOf("mohs_max", "mohs max", "hardness_max", "hardness max"),
        "mohs" to listOf("mohs", "hardness"),
        "cleavage" to listOf("cleavage"),
        "fracture" to listOf("fracture"),
        "luster" to listOf("luster", "lustre"),
        "streak" to listOf("streak"),
        "diaphaneity" to listOf("diaphaneity", "transparency", "diaphanéité"),
        "habit" to listOf("habit", "habitus", "crystal_habit"),
        "specificGravity" to listOf("specific_gravity", "specific gravity", "density", "sg"),

        // Special properties
        "fluorescence" to listOf("fluorescence", "fluorescent", "uv"),
        "magnetic" to listOf("magnetic", "magnetism", "magnétisme"),
        "radioactive" to listOf("radioactive", "radioactivity", "radioactivité"),

        // Measurements
        "dimensionsMm" to listOf("dimensions", "dimensions (mm)", "dimensions_mm", "size", "taille"),
        "weightGr" to listOf("weight", "weight (g)", "weight_gr", "mass", "poids"),

        // Status
        "status" to listOf("status", "statut"),
        "statusType" to listOf("status_type", "status type", "type"),
        "statusDetails" to listOf("status_details", "status details", "statut detail", "lifecycle_notes"),
        "qualityRating" to listOf("quality_rating", "quality", "qualité", "rating"),
        "completeness" to listOf("completeness", "complétude", "completeness"),

        // Aggregate specifics
        "rockType" to listOf("rock_type", "rock type", "lithology", "type de roche"),
        "texture" to listOf("texture", "grain", "fabric"),
        "dominantMinerals" to listOf("dominant_minerals", "dominant minerals", "principal minerals"),
        "interestingFeatures" to listOf("interesting_features", "interesting features", "features", "remarques"),
        "componentNames" to listOf("component_names", "component names", "components", "component list"),
        "componentPercentages" to listOf("component_percentages", "component percentages", "component %", "component percents"),
        "componentRoles" to listOf("component_roles", "component roles", "component role"),

        // Provenance
        "prov_country" to listOf("country", "provenance_country", "prov_country", "pays"),
        "prov_locality" to listOf("locality", "provenance_locality", "prov_locality", "localité"),
        "prov_site" to listOf("site", "provenance_site", "prov_site", "mine"),
        "prov_latitude" to listOf("latitude", "prov_latitude", "prov latitude", "lat", "provenance latitude"),
        "prov_longitude" to listOf("longitude", "prov_longitude", "prov longitude", "lon", "long", "provenance longitude"),
        "prov_acquiredAt" to listOf("acquired_at", "acquisition_date", "prov_date", "date"),
        "prov_source" to listOf("source", "provenance_source", "prov_source", "dealer"),
        "prov_price" to listOf("price", "prov_price", "prix", "cost"),
        "prov_estimatedValue" to listOf("estimated_value", "prov_value", "valeur", "value"),
        "prov_currency" to listOf("currency", "prov_currency", "devise"),
        "prov_mineName" to listOf("mine_name", "mine name", "provenance mine", "mine"),
        "prov_collectorName" to listOf("collector", "collector_name", "collector name", "provenance collector"),
        "prov_dealer" to listOf("dealer", "vendor", "marchand"),
        "prov_catalogNumber" to listOf("catalog_number", "catalog number", "inventory", "reference"),
        "prov_acquisitionNotes" to listOf("acquisition_notes", "acquisition notes", "notes acquisition", "provenance notes"),

        // Storage
        "storage_place" to listOf("place", "storage_place", "storage_location", "location", "lieu"),
        "storage_container" to listOf("container", "storage_container", "conteneur"),
        "storage_box" to listOf("box", "storage_box", "boîte"),
        "storage_slot" to listOf("slot", "storage_slot", "position", "emplacement"),
        "storage_nfcTagId" to listOf("storage_nfc", "storage_nfc_tag", "nfc tag", "nfc"),
        "storage_qrContent" to listOf("storage_qr", "qr_content", "qr content", "qr code"),

        // Other
        "notes" to listOf("notes", "note", "comments", "description"),
        "tags" to listOf("tags", "tag", "keywords", "étiquettes")
    )

    /**
     * Reversed mapping for O(1) lookup performance.
     * Maps normalized variation name to domain field.
     */
    private val reversedMappings by lazy {
        columnMappings
            .flatMap { (domainField, variations) ->
                variations.map { normalizeHeaderName(it) to domainField }
            }
            .toMap()
    }

    /**
     * Map CSV headers to domain field names.
     * Uses fuzzy matching (case-insensitive, ignores spaces/underscores).
     *
     * PERFORMANCE: O(n) instead of O(n*m) thanks to reversed mapping.
     *
     * @param csvHeaders List of headers from CSV file
     * @return Map of CSV header to domain field name
     */
    fun mapHeaders(csvHeaders: List<String>): Map<String, String> {
        return csvHeaders.mapNotNull { csvHeader ->
            val normalized = normalizeHeaderName(csvHeader)
            val domainField = reversedMappings[normalized]
            if (domainField != null) csvHeader to domainField else null
        }.toMap()
    }

    /**
     * Normalize header name for comparison (lowercase, no spaces/underscores).
     */
    private fun normalizeHeaderName(name: String): String {
        return name.lowercase()
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .trim()
    }

    /**
     * Get suggested domain field for unmapped CSV header.
     * Returns null if no good match found.
     */
    fun suggestMapping(csvHeader: String): String? {
        val normalized = normalizeHeaderName(csvHeader)

        // Find closest match using simple string similarity
        return columnMappings.entries
            .filter { (_, variations) ->
                variations.any { normalizeHeaderName(it).contains(normalized) || normalized.contains(normalizeHeaderName(it)) }
            }
            .maxByOrNull { (_, variations) ->
                variations.map { levenshteinDistance(normalized, normalizeHeaderName(it)) }.minOrNull() ?: Int.MAX_VALUE
            }
            ?.key
    }

    /**
     * Calculate Levenshtein distance for fuzzy matching.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }
}
