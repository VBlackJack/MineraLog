package net.meshcore.mineralog.data.service

import net.meshcore.mineralog.domain.model.Mineral

/**
 * Service responsible for mapping CSV rows to Mineral domain objects.
 * Extracted from BackupRepository for better separation of concerns.
 */
class MineralCsvMapper {

    /**
     * Parse a Mineral object from a CSV row using column mapping.
     *
     * @param row The CSV row data
     * @param columnMapping Mapping of CSV headers to domain fields
     * @param existingMineral If provided (MERGE mode), reuse its ID and related entity IDs
     */
    fun parseMineralFromCsvRow(
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

    /**
     * Escape CSV values containing special characters.
     */
    fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
