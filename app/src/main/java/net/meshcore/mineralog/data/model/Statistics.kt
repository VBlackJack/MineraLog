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
    val byCrystalSystem: Map<String, Int> = emptyMap(), // e.g., {"Cubic": 12, "Hexagonal": 8}
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
    val addedThisYear: Int = 0,
    val addedByMonth: Map<String, Int> = emptyMap(), // e.g., {"2025-01": 5, "2025-02": 8}

    // v2.0: Aggregate statistics
    val byType: Map<String, Int> = emptyMap(), // e.g., {"SIMPLE": 45, "AGGREGATE": 12}
    val totalAggregates: Int = 0,
    val totalSimple: Int = 0,
    val mostFrequentComponents: List<ComponentFrequency> = emptyList(), // Top 10 components
    val averageComponentCount: Double = 0.0 // Average number of components per aggregate
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

/**
 * Component frequency for aggregate statistics (v2.0).
 * Tracks how often each mineral appears as a component in aggregates.
 */
data class ComponentFrequency(
    val componentName: String,
    val count: Int,
    val aggregateIds: List<String> = emptyList() // Optional: IDs of aggregates containing this component
)
