package net.meshcore.mineralog.data.model

/**
 * Aggregated statistics for the mineral collection.
 * Computed from database queries for display in Statistics screen.
 */
data class CollectionStatistics(
    // Overall metrics
    val totalMinerals: Int = 0,
    val totalValue: Double = 0.0,
    val averageValue: Double = 0.0,

    // By category
    val byGroup: Map<String, Int> = emptyMap(), // e.g., {"Silicates": 15, "Oxides": 8}
    val byCountry: Map<String, Int> = emptyMap(), // e.g., {"USA": 10, "Brazil": 5}
    val byHardness: Map<IntRange, Int> = emptyMap(), // e.g., {1..2: 3, 3..4: 7}
    val byStatus: Map<String, Int> = emptyMap(), // e.g., {"in_collection": 20, "on_display": 5}

    // Top items
    val mostCommonGroup: String? = null,
    val mostCommonCountry: String? = null,
    val mostValuableSpecimen: MineralSummary? = null,

    // Completeness
    val averageCompleteness: Double = 0.0,
    val fullyDocumentedCount: Int = 0, // completeness >= 80%

    // Time-based
    val addedThisMonth: Int = 0,
    val addedThisYear: Int = 0
)

/**
 * Minimal mineral info for statistics display.
 */
data class MineralSummary(
    val id: String,
    val name: String,
    val value: Double,
    val currency: String?
)
