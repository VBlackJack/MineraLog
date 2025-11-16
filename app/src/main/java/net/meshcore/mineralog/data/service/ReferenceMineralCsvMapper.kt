package net.meshcore.mineralog.data.service

import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import java.time.Instant

/**
 * Mapper for converting ReferenceMineralEntity to/from CSV format.
 *
 * CSV Format (24 columns + metadata):
 * id, nameFr, nameEn, synonyms, mineralGroup, formula,
 * mohsMin, mohsMax, density, crystalSystem, cleavage, fracture, habit,
 * luster, streak, diaphaneity, fluorescence, magnetism, radioactivity,
 * notes, isUserDefined, source, createdAt, updatedAt
 */
class ReferenceMineralCsvMapper {

    companion object {
        // CSV column order (must match)
        private val HEADERS = listOf(
            "id",
            "nameFr",
            "nameEn",
            "synonyms",
            "mineralGroup",
            "formula",
            "mohsMin",
            "mohsMax",
            "density",
            "crystalSystem",
            "cleavage",
            "fracture",
            "habit",
            "luster",
            "streak",
            "diaphaneity",
            "fluorescence",
            "magnetism",
            "radioactivity",
            "notes",
            "isUserDefined",
            "source",
            "createdAt",
            "updatedAt"
        )

        /**
         * Get CSV header row.
         */
        fun getHeaders(): List<String> = HEADERS
    }

    /**
     * Convert a ReferenceMineralEntity to a CSV row.
     *
     * @param entity The entity to convert
     * @return A list of values representing a CSV row
     */
    fun toCsvRow(entity: ReferenceMineralEntity): List<String> {
        return listOf(
            entity.id,
            entity.nameFr,
            entity.nameEn,
            entity.synonyms ?: "",
            entity.mineralGroup ?: "",
            entity.formula ?: "",
            entity.mohsMin?.toString() ?: "",
            entity.mohsMax?.toString() ?: "",
            entity.density?.toString() ?: "",
            entity.crystalSystem ?: "",
            entity.cleavage ?: "",
            entity.fracture ?: "",
            entity.habit ?: "",
            entity.luster ?: "",
            entity.streak ?: "",
            entity.diaphaneity ?: "",
            entity.fluorescence ?: "",
            entity.magnetism ?: "",
            entity.radioactivity ?: "",
            entity.notes ?: "",
            entity.isUserDefined.toString(),
            entity.source ?: "",
            entity.createdAt.toString(),
            entity.updatedAt.toString()
        )
    }

    /**
     * Parse a CSV row into a ReferenceMineralEntity.
     *
     * @param row The CSV row values
     * @param headers Optional header mapping (if column order differs)
     * @return The parsed entity, or null if parsing failed
     */
    fun fromCsvRow(row: List<String>, headers: List<String> = HEADERS): ReferenceMineralEntity? {
        return try {
            // Create a map for easy column lookup
            val columnMap = row.mapIndexed { index, value -> headers.getOrNull(index) to value }
                .filter { it.first != null }
                .associate { it.first!! to it.second }

            // Required fields
            val id = columnMap["id"] ?: return null
            val nameFr = columnMap["nameFr"] ?: return null
            val nameEn = columnMap["nameEn"] ?: return null

            // Parse timestamps
            val createdAt = columnMap["createdAt"]?.let {
                try { Instant.parse(it) } catch (e: Exception) { Instant.now() }
            } ?: Instant.now()

            val updatedAt = columnMap["updatedAt"]?.let {
                try { Instant.parse(it) } catch (e: Exception) { Instant.now() }
            } ?: Instant.now()

            // Parse boolean
            val isUserDefined = columnMap["isUserDefined"]?.toBooleanStrictOrNull() ?: false

            ReferenceMineralEntity(
                id = id,
                nameFr = nameFr,
                nameEn = nameEn,
                synonyms = columnMap["synonyms"]?.takeIf { it.isNotBlank() },
                mineralGroup = columnMap["mineralGroup"]?.takeIf { it.isNotBlank() },
                formula = columnMap["formula"]?.takeIf { it.isNotBlank() },
                mohsMin = columnMap["mohsMin"]?.toFloatOrNull(),
                mohsMax = columnMap["mohsMax"]?.toFloatOrNull(),
                density = columnMap["density"]?.toFloatOrNull(),
                crystalSystem = columnMap["crystalSystem"]?.takeIf { it.isNotBlank() },
                cleavage = columnMap["cleavage"]?.takeIf { it.isNotBlank() },
                fracture = columnMap["fracture"]?.takeIf { it.isNotBlank() },
                habit = columnMap["habit"]?.takeIf { it.isNotBlank() },
                luster = columnMap["luster"]?.takeIf { it.isNotBlank() },
                streak = columnMap["streak"]?.takeIf { it.isNotBlank() },
                diaphaneity = columnMap["diaphaneity"]?.takeIf { it.isNotBlank() },
                fluorescence = columnMap["fluorescence"]?.takeIf { it.isNotBlank() },
                magnetism = columnMap["magnetism"]?.takeIf { it.isNotBlank() },
                radioactivity = columnMap["radioactivity"]?.takeIf { it.isNotBlank() },
                notes = columnMap["notes"]?.takeIf { it.isNotBlank() },
                isUserDefined = isUserDefined,
                source = columnMap["source"]?.takeIf { it.isNotBlank() },
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            android.util.Log.e("RefMineralCsvMapper", "Failed to parse CSV row", e)
            null
        }
    }

    /**
     * Escape a CSV value to handle commas, quotes, and newlines.
     *
     * @param value The value to escape
     * @return The escaped value
     */
    fun escapeCsvValue(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Unescape a CSV value.
     *
     * @param value The value to unescape
     * @return The unescaped value
     */
    fun unescapeCsvValue(value: String): String {
        return if (value.startsWith("\"") && value.endsWith("\"")) {
            value.substring(1, value.length - 1).replace("\"\"", "\"")
        } else {
            value
        }
    }
}
