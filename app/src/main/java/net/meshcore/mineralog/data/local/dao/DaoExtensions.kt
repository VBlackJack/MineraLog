package net.meshcore.mineralog.data.local.dao

/**
 * Data class for group distribution results.
 */
data class CountByGroup(
    val `group`: String,
    val count: Int
)

/**
 * Data class for country distribution results.
 */
data class CountByCountry(
    val country: String,
    val count: Int
)

/**
 * Data class for hardness range distribution results.
 */
data class CountByRange(
    val range: String,
    val count: Int
)

/**
 * Data class for status distribution results.
 */
data class CountByStatus(
    val statusType: String,
    val count: Int
)

/**
 * Extension to convert list of CountByGroup to Map.
 */
fun List<CountByGroup>.toMap(): Map<String, Int> = associate { it.`group` to it.count }

/**
 * Extension to convert list of CountByCountry to Map.
 */
fun List<CountByCountry>.toMap(): Map<String, Int> = associate { it.country to it.count }

/**
 * Extension to convert list of CountByRange to Map.
 */
fun List<CountByRange>.toMap(): Map<String, Int> = associate { it.range to it.count }

/**
 * Extension to convert list of CountByStatus to Map.
 */
fun List<CountByStatus>.toMap(): Map<String, Int> = associate { it.statusType to it.count }
