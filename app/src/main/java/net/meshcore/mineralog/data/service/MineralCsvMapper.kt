package net.meshcore.mineralog.data.service

import net.meshcore.mineralog.domain.model.ComponentRole
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.MineralComponent
import net.meshcore.mineralog.domain.model.MineralType
import net.meshcore.mineralog.domain.model.Provenance
import net.meshcore.mineralog.domain.model.Storage
import java.time.Instant
import java.util.Locale
import java.util.UUID

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
        // PERFORMANCE: Invert mapping once for O(1) lookup instead of O(n) linear search
        val invertedMapping = columnMapping.entries.associate { (k, v) -> v to k }

        fun hasColumn(domainField: String): Boolean = invertedMapping.containsKey(domainField)

        // Helper to get mapped value
        fun getRaw(domainField: String): String? {
            val csvHeader = invertedMapping[domainField]  // O(1) instead of O(n)
            return csvHeader?.let { row[it] }
        }

        fun getMapped(domainField: String): String? = getRaw(domainField)?.takeIf { it.isNotBlank() }

        fun getFloat(domainField: String): Float? = getMapped(domainField)?.toFloatOrNull()

        fun getInt(domainField: String): Int? = getMapped(domainField)?.toIntOrNull()

        fun getDouble(domainField: String): Double? = getMapped(domainField)?.toDoubleOrNull()

        fun getBoolean(domainField: String): Boolean? {
            val value = getMapped(domainField)?.lowercase(Locale.ROOT) ?: return null
            return when (value) {
                "true", "yes", "1", "y", "oui" -> true
                "false", "no", "0", "n", "non" -> false
                else -> throw IllegalArgumentException("Invalid boolean value '$value' for $domainField")
            }
        }

        fun getInstant(domainField: String): Instant? {
            val value = getMapped(domainField) ?: return null
            return try {
                Instant.parse(value)
            } catch (e: java.time.format.DateTimeParseException) {
                throw IllegalArgumentException("Invalid date format for $domainField: '$value'. Expected ISO 8601 format (e.g., 2024-01-15T10:30:00Z)")
            }
        }

        // Generate IDs - reuse existing if provided (MERGE mode)
        val mineralId = existingMineral?.id ?: java.util.UUID.randomUUID().toString()

        // Parse basic mineral fields
        val name = getMapped("name") ?: existingMineral?.name ?: throw IllegalArgumentException("Name is required")
        // When merging we prefer explicit CSV values, but we fall back to the already persisted data to avoid wiping user input
        val mineralType = getMapped("mineralType")?.let { rawType ->
            runCatching { MineralType.valueOf(rawType.uppercase(Locale.ROOT)) }
                .getOrElse { throw IllegalArgumentException("Invalid mineral type '$rawType'") }
        } ?: existingMineral?.mineralType ?: MineralType.SIMPLE

        // Handle special case: single "mohs" field maps to both min and max
        val mohsMinCsv = getFloat("mohsMin") ?: getFloat("mohs")
        val mohsMaxCsv = getFloat("mohsMax") ?: getFloat("mohs")
        val mohsMin = mohsMinCsv ?: existingMineral?.mohsMin
        val mohsMax = mohsMaxCsv ?: existingMineral?.mohsMax

        // Validate Mohs hardness range (1.0 to 10.0) when provided by CSV
        if (mohsMinCsv != null && (mohsMinCsv < 1.0f || mohsMinCsv > 10.0f)) {
            throw IllegalArgumentException("Mohs Min must be between 1.0 and 10.0 (got: $mohsMinCsv)")
        }
        if (mohsMaxCsv != null && (mohsMaxCsv < 1.0f || mohsMaxCsv > 10.0f)) {
            throw IllegalArgumentException("Mohs Max must be between 1.0 and 10.0 (got: $mohsMaxCsv)")
        }

        // Parse provenance fields
        val existingProvenance = existingMineral?.provenance
        val hasProvenance = existingProvenance != null || listOf(
            "prov_country",
            "prov_locality",
            "prov_site",
            "prov_source",
            "prov_latitude",
            "prov_longitude",
            "prov_mineName",
            "prov_collectorName",
            "prov_dealer",
            "prov_catalogNumber",
            "prov_acquisitionNotes"
        ).any { getMapped(it) != null }

        val latitudeCsv = getDouble("prov_latitude")
        if (latitudeCsv != null && (latitudeCsv < -90.0 || latitudeCsv > 90.0)) {
            throw IllegalArgumentException("Latitude must be between -90.0 and 90.0 (got: $latitudeCsv)")
        }
        val longitudeCsv = getDouble("prov_longitude")
        if (longitudeCsv != null && (longitudeCsv < -180.0 || longitudeCsv > 180.0)) {
            throw IllegalArgumentException("Longitude must be between -180.0 and 180.0 (got: $longitudeCsv)")
        }

        val provenance = if (hasProvenance) {
            Provenance(
                id = existingProvenance?.id ?: java.util.UUID.randomUUID().toString(),
                mineralId = mineralId,
                country = getMapped("prov_country") ?: existingProvenance?.country,
                locality = getMapped("prov_locality") ?: existingProvenance?.locality,
                site = getMapped("prov_site") ?: existingProvenance?.site,
                latitude = latitudeCsv ?: existingProvenance?.latitude,
                longitude = longitudeCsv ?: existingProvenance?.longitude,
                acquiredAt = getInstant("prov_acquiredAt") ?: existingProvenance?.acquiredAt,
                source = getMapped("prov_source") ?: existingProvenance?.source,
                price = getFloat("prov_price") ?: existingProvenance?.price,
                estimatedValue = getFloat("prov_estimatedValue") ?: existingProvenance?.estimatedValue,
                currency = getMapped("prov_currency") ?: existingProvenance?.currency ?: "USD",
                mineName = getMapped("prov_mineName") ?: existingProvenance?.mineName,
                collectorName = getMapped("prov_collectorName") ?: existingProvenance?.collectorName,
                dealer = getMapped("prov_dealer") ?: existingProvenance?.dealer,
                catalogNumber = getMapped("prov_catalogNumber") ?: existingProvenance?.catalogNumber,
                acquisitionNotes = getMapped("prov_acquisitionNotes") ?: existingProvenance?.acquisitionNotes
            )
        } else null

        // Parse storage fields
        val existingStorage = existingMineral?.storage
        val hasStorage = existingStorage != null || listOf(
            "storage_place",
            "storage_container",
            "storage_box",
            "storage_slot",
            "storage_nfcTagId",
            "storage_qrContent"
        ).any { getMapped(it) != null }
        val storage = if (hasStorage) {
            Storage(
                id = existingStorage?.id ?: java.util.UUID.randomUUID().toString(),
                mineralId = mineralId,
                place = getMapped("storage_place") ?: existingStorage?.place,
                container = getMapped("storage_container") ?: existingStorage?.container,
                box = getMapped("storage_box") ?: existingStorage?.box,
                slot = getMapped("storage_slot") ?: existingStorage?.slot,
                nfcTagId = getMapped("storage_nfcTagId") ?: existingStorage?.nfcTagId,
                qrContent = getMapped("storage_qrContent") ?: existingStorage?.qrContent
            )
        } else null

        val tags = getMapped("tags")?.split(";")?.map { it.trim() }?.filter { it.isNotEmpty() }

        val components = parseComponents(
            namesRaw = getRaw("componentNames"),
            percentagesRaw = getRaw("componentPercentages"),
            rolesRaw = getRaw("componentRoles"),
            hasComponentColumns = hasColumn("componentNames") || hasColumn("componentPercentages") || hasColumn("componentRoles"),
            existingComponents = existingMineral?.components ?: emptyList()
        )

        return Mineral(
            id = mineralId,
            name = name,
            mineralType = mineralType,
            group = getMapped("group") ?: existingMineral?.group,
            formula = getMapped("formula") ?: existingMineral?.formula,
            crystalSystem = getMapped("crystalSystem") ?: existingMineral?.crystalSystem,
            mohsMin = mohsMin,
            mohsMax = mohsMax,
            cleavage = getMapped("cleavage") ?: existingMineral?.cleavage,
            fracture = getMapped("fracture") ?: existingMineral?.fracture,
            luster = getMapped("luster") ?: existingMineral?.luster,
            streak = getMapped("streak") ?: existingMineral?.streak,
            diaphaneity = getMapped("diaphaneity") ?: existingMineral?.diaphaneity,
            habit = getMapped("habit") ?: existingMineral?.habit,
            specificGravity = getFloat("specificGravity") ?: existingMineral?.specificGravity,
            fluorescence = getMapped("fluorescence") ?: existingMineral?.fluorescence,
            magnetic = getBoolean("magnetic") ?: existingMineral?.magnetic ?: false,
            radioactive = getBoolean("radioactive") ?: existingMineral?.radioactive ?: false,
            dimensionsMm = getMapped("dimensionsMm") ?: existingMineral?.dimensionsMm,
            weightGr = getFloat("weightGr") ?: existingMineral?.weightGr,
            rockType = getMapped("rockType") ?: existingMineral?.rockType,
            texture = getMapped("texture") ?: existingMineral?.texture,
            dominantMinerals = getMapped("dominantMinerals") ?: existingMineral?.dominantMinerals,
            interestingFeatures = getMapped("interestingFeatures") ?: existingMineral?.interestingFeatures,
            notes = getMapped("notes") ?: existingMineral?.notes,
            tags = tags ?: existingMineral?.tags ?: emptyList(),
            status = getMapped("status") ?: existingMineral?.status ?: "incomplete",
            statusType = getMapped("statusType") ?: existingMineral?.statusType ?: "in_collection",
            statusDetails = getMapped("statusDetails") ?: existingMineral?.statusDetails,
            qualityRating = getInt("qualityRating") ?: existingMineral?.qualityRating,
            completeness = getInt("completeness") ?: existingMineral?.completeness ?: 0,
            createdAt = existingMineral?.createdAt ?: Instant.now(),
            updatedAt = Instant.now(),
            provenance = provenance,
            storage = storage,
            photos = existingMineral?.photos ?: emptyList(),
            components = components
        )
    }

    private fun parseComponents(
        namesRaw: String?,
        percentagesRaw: String?,
        rolesRaw: String?,
        hasComponentColumns: Boolean,
        existingComponents: List<MineralComponent>
    ): List<MineralComponent> {
        if (!hasComponentColumns) return existingComponents

        val names = namesRaw
            ?.split(';')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        if (names.isEmpty()) return emptyList()

        val percentages = percentagesRaw?.split(';')?.map { it.trim() }
        val roles = rolesRaw?.split(';')?.map { it.trim() }

        val normalizedExisting = existingComponents
            .filter { it.mineralName.isNotBlank() }
            .associateBy { it.mineralName.trim().lowercase(Locale.ROOT) }
        val claimedFallbackIds = mutableSetOf<String>()

        return names.mapIndexed { index, name ->
            val normalized = name.lowercase(Locale.ROOT)
            val fallback = normalizedExisting[normalized]
                ?.takeUnless { claimedFallbackIds.contains(it.id) }
                ?: existingComponents.getOrNull(index)?.takeUnless { component ->
                    claimedFallbackIds.contains(component.id)
                }

            fallback?.id?.let { claimedFallbackIds.add(it) }
            val parsedPercentage = percentages?.getOrNull(index)?.toFloatOrNull()
                ?: fallback?.percentage
            val parsedRole = roles?.getOrNull(index)
                ?.takeIf { it.isNotBlank() }
                ?.let { value ->
                    runCatching { ComponentRole.valueOf(value.uppercase(Locale.ROOT)) }
                        .getOrElse { throw IllegalArgumentException("Invalid component role '$value'") }
                }
                ?: fallback?.role
                ?: ComponentRole.fromPercentage(parsedPercentage)

            MineralComponent(
                id = fallback?.id ?: UUID.randomUUID().toString(),
                mineralName = name,
                mineralGroup = fallback?.mineralGroup,
                percentage = parsedPercentage,
                role = parsedRole,
                mohsMin = fallback?.mohsMin,
                mohsMax = fallback?.mohsMax,
                density = fallback?.density,
                formula = fallback?.formula,
                crystalSystem = fallback?.crystalSystem,
                luster = fallback?.luster,
                diaphaneity = fallback?.diaphaneity,
                cleavage = fallback?.cleavage,
                fracture = fallback?.fracture,
                habit = fallback?.habit,
                streak = fallback?.streak,
                fluorescence = fallback?.fluorescence,
                notes = fallback?.notes,
                createdAt = fallback?.createdAt ?: Instant.now(),
                updatedAt = Instant.now()
            )
        }
    }

    /**
     * Escape CSV values containing special characters.
     * P1-7: Implements CSV injection protection by sanitizing formula characters.
     *
     * Protects against formula injection attacks where values starting with
     * =, +, -, @ could be interpreted as formulas by spreadsheet applications.
     *
     * Uses OWASP recommendation: prefix with single quote instead of removing characters
     * to preserve data integrity (e.g., "=CaCO3" becomes "'=CaCO3" instead of "CaCO3")
     *
     * @param value The value to escape
     * @return Escaped and sanitized CSV value
     */
    fun escapeCSV(value: String): String {
        if (value.isEmpty()) return value

        // P1-7: CSV Injection protection - prefix formula characters with single quote (OWASP standard)
        // This prevents formula injection while preserving the original data
        val sanitized = if (value.firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) {
            // Prefix with single quote to escape formula characters (OWASP recommendation)
            "'$value"
        } else {
            value
        }

        // Standard CSV escaping for special characters
        return if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n")) {
            "\"${sanitized.replace("\"", "\"\"")}\""
        } else {
            sanitized
        }
    }
}
