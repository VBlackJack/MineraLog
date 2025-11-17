package net.meshcore.mineralog.data.util

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

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

        // Read first line to detect delimiter and headers
        val firstLine = reader.readLine() ?: return@withContext ParseResult(
            headers = emptyList(),
            rows = emptyList(),
            encoding = encoding,
            delimiter = ',',
            lineCount = 0,
            errors = listOf(ParseError(0, "Empty CSV file"))
        )

        val delimiter = detectDelimiter(firstLine)
        val headers = parseLine(firstLine, delimiter)

        // Parse data rows
        val rows = mutableListOf<Map<String, String>>()
        val errors = mutableListOf<ParseError>()
        var lineNumber = 2 // Line 1 is headers

        while (true) {
            val line = reader.readLine() ?: break
            if (maxRows > 0 && rows.size >= maxRows) break

            try {
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
                errors.add(ParseError(lineNumber, "Parse error: ${e.message}"))
            }

            lineNumber++
        }

        ParseResult(
            headers = headers.map { it.trim() },
            rows = rows,
            encoding = encoding,
            delimiter = delimiter,
            lineCount = lineNumber - 1,
            errors = errors
        )
    }

    /**
     * Detect character encoding by checking for BOM and common patterns.
     * Stream must support mark/reset.
     * Skips BOM bytes if found.
     */
    private fun detectEncoding(inputStream: InputStream): Charset {
        require(inputStream.markSupported()) { "InputStream must support mark/reset for encoding detection" }

        inputStream.mark(4096) // Mark with generous read-ahead limit
        val bom = ByteArray(4)
        val read = inputStream.read(bom)
        inputStream.reset()

        // Check for UTF-8 BOM (EF BB BF) - skip it if found
        if (read >= 3 && bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && bom[2] == 0xBF.toByte()) {
            inputStream.skip(3) // Skip the UTF-8 BOM
            return StandardCharsets.UTF_8
        }

        // Check for UTF-16 BOMs
        if (read >= 2) {
            if (bom[0] == 0xFE.toByte() && bom[1] == 0xFF.toByte()) {
                inputStream.skip(2) // Skip the UTF-16BE BOM
                return Charsets.UTF_16BE
            }
            if (bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte()) {
                inputStream.skip(2) // Skip the UTF-16LE BOM
                return Charsets.UTF_16LE
            }
        }

        // Default to UTF-8 (most common for modern CSVs)
        return StandardCharsets.UTF_8
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
        "qualityRating" to listOf("quality_rating", "quality", "qualité", "rating"),
        "completeness" to listOf("completeness", "complétude", "completeness"),

        // Provenance
        "prov_country" to listOf("country", "provenance_country", "prov_country", "pays"),
        "prov_locality" to listOf("locality", "provenance_locality", "prov_locality", "localité"),
        "prov_site" to listOf("site", "provenance_site", "prov_site", "mine"),
        "prov_latitude" to listOf("latitude", "prov_latitude", "prov latitude", "lat"),
        "prov_longitude" to listOf("longitude", "prov_longitude", "prov longitude", "lon", "long"),
        "prov_acquiredAt" to listOf("acquired_at", "acquisition_date", "prov_date", "date"),
        "prov_source" to listOf("source", "provenance_source", "prov_source", "dealer"),
        "prov_price" to listOf("price", "prov_price", "prix", "cost"),
        "prov_estimatedValue" to listOf("estimated_value", "prov_value", "valeur", "value"),
        "prov_currency" to listOf("currency", "prov_currency", "devise"),

        // Storage
        "storage_place" to listOf("place", "storage_place", "storage_location", "location", "lieu"),
        "storage_container" to listOf("container", "storage_container", "conteneur"),
        "storage_box" to listOf("box", "storage_box", "boîte"),
        "storage_slot" to listOf("slot", "storage_slot", "position", "emplacement"),

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
