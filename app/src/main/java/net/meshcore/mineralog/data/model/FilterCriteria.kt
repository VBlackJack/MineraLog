package net.meshcore.mineralog.data.model

import kotlinx.serialization.Serializable

/**
 * Filter criteria for advanced mineral searching.
 * All fields are optional; null/empty means "don't filter by this criterion".
 */
@Serializable
data class FilterCriteria(
    /** Filter by mineral groups (OR logic: any match) */
    val groups: List<String> = emptyList(),

    /** Filter by countries (OR logic: any match) */
    val countries: List<String> = emptyList(),

    /** Filter by crystal systems (OR logic: any match) */
    val crystalSystems: List<String> = emptyList(),

    /** Minimum Mohs hardness (inclusive) */
    val mohsMin: Float? = null,

    /** Maximum Mohs hardness (inclusive) */
    val mohsMax: Float? = null,

    /** Filter by status types (OR logic: any match) */
    val statusTypes: List<String> = emptyList(),

    /** Minimum quality rating (1-5, inclusive) */
    val qualityMin: Int? = null,

    /** Maximum quality rating (1-5, inclusive) */
    val qualityMax: Int? = null,

    /** Filter by photo presence: true = has photos, false = no photos, null = don't filter */
    val hasPhotos: Boolean? = null,

    /** Filter by fluorescence: true = fluorescent, false = not fluorescent, null = don't filter */
    val fluorescent: Boolean? = null,

    /** Filter by mineral types (v2.0) - OR logic: any match (SIMPLE, AGGREGATE) */
    val mineralTypes: List<String> = emptyList()
) {
    /**
     * Returns true if all criteria are empty/null (no filtering).
     */
    fun isEmpty(): Boolean =
        groups.isEmpty() &&
        countries.isEmpty() &&
        crystalSystems.isEmpty() &&
        mohsMin == null &&
        mohsMax == null &&
        statusTypes.isEmpty() &&
        qualityMin == null &&
        qualityMax == null &&
        hasPhotos == null &&
        fluorescent == null &&
        mineralTypes.isEmpty()

    /**
     * Returns count of active filter criteria.
     */
    fun activeCount(): Int {
        var count = 0
        if (groups.isNotEmpty()) count++
        if (countries.isNotEmpty()) count++
        if (crystalSystems.isNotEmpty()) count++
        if (mohsMin != null || mohsMax != null) count++
        if (statusTypes.isNotEmpty()) count++
        if (qualityMin != null || qualityMax != null) count++
        if (hasPhotos != null) count++
        if (fluorescent != null) count++
        if (mineralTypes.isNotEmpty()) count++
        return count
    }

    /**
     * Returns a summary string of active filters (for UI display).
     */
    fun toSummary(): String {
        val parts = mutableListOf<String>()
        if (groups.isNotEmpty()) parts.add("Groups: ${groups.size}")
        if (countries.isNotEmpty()) parts.add("Countries: ${countries.size}")
        if (crystalSystems.isNotEmpty()) parts.add("Crystal Systems: ${crystalSystems.size}")
        if (mohsMin != null || mohsMax != null) {
            val range = when {
                mohsMin != null && mohsMax != null -> "$mohsMin-$mohsMax"
                mohsMin != null -> ">=$mohsMin"
                mohsMax != null -> "<=$mohsMax"
                else -> ""
            }
            parts.add("Mohs: $range")
        }
        if (statusTypes.isNotEmpty()) parts.add("Status: ${statusTypes.size}")
        if (qualityMin != null || qualityMax != null) parts.add("Quality: ${qualityMin ?: 1}-${qualityMax ?: 5}")
        if (hasPhotos == true) parts.add("Has Photos")
        if (hasPhotos == false) parts.add("No Photos")
        if (fluorescent == true) parts.add("Fluorescent")
        if (fluorescent == false) parts.add("Non-Fluorescent")
        if (mineralTypes.isNotEmpty()) parts.add("Types: ${mineralTypes.size}")
        return parts.joinToString(", ")
    }

    companion object {
        /** Empty filter criteria (no filtering) */
        val EMPTY = FilterCriteria()
    }
}
