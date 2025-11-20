package net.meshcore.mineralog.ui.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R
import net.meshcore.mineralog.data.model.CollectionStatistics
import net.meshcore.mineralog.ui.components.charts.BarChart
import net.meshcore.mineralog.ui.components.charts.PieChart

/**
 * Statistics screen displaying collection analytics.
 * Shows total counts, value metrics, and distribution charts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshStatistics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is StatisticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Loading statistics"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is StatisticsUiState.Success -> {
                StatisticsContent(
                    statistics = state.statistics,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is StatisticsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadStatistics() }) {
                            Text(stringResource(R.string.statistics_retry))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    statistics: CollectionStatistics,
    modifier: Modifier = Modifier
) {
    if (statistics.totalMinerals == 0) {
        // Empty state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(32.dp)
                    .semantics {
                        contentDescription = "No statistics available. Your collection is empty. " +
                            "Add minerals to see statistics and analytics."
                    }
            ) {
                Text(
                    text = stringResource(R.string.statistics_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.statistics_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Overall Metrics
        Text(
            text = stringResource(R.string.statistics_overview),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricRow("Total Minerals", statistics.totalMinerals.toString())
                MetricRow("Total Value", "$${String.format("%.2f", statistics.totalValue)}")
                MetricRow("Average Value", "$${String.format("%.2f", statistics.averageValue)}")
                MetricRow(
                    "Avg. Completeness",
                    "${statistics.averageCompleteness.toInt()}%"
                )
                MetricRow(
                    "Fully Documented",
                    "${statistics.fullyDocumentedCount} (${
                        if (statistics.totalMinerals > 0) {
                            (statistics.fullyDocumentedCount * 100 / statistics.totalMinerals)
                        } else {
                            0
                        }
                    }%)"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time-based Stats
        Text(
            text = stringResource(R.string.statistics_time_based),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                MetricRow("Added This Month", statistics.addedThisMonth.toString())
                MetricRow("Added This Year", statistics.addedThisYear.toString())
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Top Items
        if (statistics.mostCommonGroup != null || statistics.mostValuableSpecimen != null) {
            Text(
                text = stringResource(R.string.statistics_highlights),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    statistics.mostCommonGroup?.let {
                        MetricRow("Most Common Group", it)
                    }
                    statistics.mostCommonCountry?.let {
                        MetricRow("Most Common Country", it)
                    }
                    statistics.mostValuableSpecimen?.let {
                        MetricRow(
                            "Most Valuable",
                            "${it.name} - $${String.format("%.2f", it.value)}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Distribution by Group
        if (statistics.byGroup.isNotEmpty()) {
            Text(
                text = stringResource(R.string.statistics_by_group),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Limit to top 10 for pie chart clarity
                val topGroups = statistics.byGroup.entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .associate { it.key to it.value }

                PieChart(
                    data = topGroups,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                        .semantics {
                            contentDescription = buildString {
                                append("Pie chart showing distribution by group. ")
                                append("${topGroups.size} groups displayed. ")
                                topGroups.entries.forEachIndexed { index, entry ->
                                    if (index < 3) {
                                        append("${entry.key}: ${entry.value} minerals. ")
                                    }
                                }
                                if (topGroups.size > 3) {
                                    append("And ${topGroups.size - 3} more groups.")
                                }
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Distribution by Dominant Color
        if (statistics.byDominantColor.isNotEmpty()) {
            Text(
                text = stringResource(R.string.statistics_by_color),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Map color names to actual colors
                val colorMapping = getMineralColorMapping()
                val pieColors = statistics.byDominantColor.keys.map { colorName ->
                    colorMapping[colorName] ?: MaterialTheme.colorScheme.primary
                }

                PieChart(
                    data = statistics.byDominantColor,
                    colors = pieColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                        .semantics {
                            contentDescription = buildString {
                                append("Pie chart showing distribution by dominant color. ")
                                append("${statistics.byDominantColor.size} colors displayed. ")
                                statistics.byDominantColor.entries.forEachIndexed { index, entry ->
                                    if (index < 3) {
                                        append("${entry.key}: ${entry.value} minerals. ")
                                    }
                                }
                                if (statistics.byDominantColor.size > 3) {
                                    append("And ${statistics.byDominantColor.size - 3} more colors.")
                                }
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Distribution by Country (Top 5 + Others)
        if (statistics.byCountry.isNotEmpty()) {
            Text(
                text = stringResource(R.string.statistics_by_country),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Process data for Top 5 + Others
                val sortedByCountry = statistics.byCountry.entries
                    .sortedByDescending { it.value }
                    .toList()

                val displayData = if (sortedByCountry.size > 5) {
                    val top5 = sortedByCountry.take(5)
                    val othersCount = sortedByCountry.drop(5).sumOf { it.value }
                    top5.associate { it.key to it.value } + ("Others" to othersCount)
                } else {
                    statistics.byCountry
                }

                BarChart(
                    data = displayData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = buildString {
                                append("Bar chart showing top origins by country. ")
                                val topCountries = displayData.entries
                                    .sortedByDescending { it.value }
                                    .take(3)
                                topCountries.forEach { entry ->
                                    append("${entry.key}: ${entry.value} minerals. ")
                                }
                                if (displayData.size > 3) {
                                    append("And ${displayData.size - 3} more entries.")
                                }
                            }
                        },
                    maxBars = 6  // Top 5 + Others
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Distribution by Crystal System
        if (statistics.byCrystalSystem.isNotEmpty()) {
            Text(
                text = stringResource(R.string.statistics_by_crystal_system),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Limit to top 10 for pie chart clarity
                val topSystems = statistics.byCrystalSystem.entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .associate { it.key to it.value }

                PieChart(
                    data = topSystems,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                        .semantics {
                            contentDescription = buildString {
                                append("Pie chart showing distribution by crystal system. ")
                                append("${topSystems.size} systems displayed. ")
                                topSystems.entries.forEachIndexed { index, entry ->
                                    if (index < 3) {
                                        append("${entry.key}: ${entry.value} minerals. ")
                                    }
                                }
                                if (topSystems.size > 3) {
                                    append("And ${topSystems.size - 3} more systems.")
                                }
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Distribution by Hardness
        if (statistics.byHardness.isNotEmpty()) {
            Text(
                text = stringResource(R.string.statistics_by_hardness),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Convert IntRange keys to String for BarChart
                val hardnessData = statistics.byHardness.mapKeys { (range, _) ->
                    "${range.first}-${range.last}"
                }
                BarChart(
                    data = hardnessData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = buildString {
                                append("Bar chart showing distribution by hardness ranges. ")
                                val topRanges = hardnessData.entries
                                    .sortedByDescending { it.value }
                                    .take(3)
                                topRanges.forEach { entry ->
                                    append("Hardness ${entry.key}: ${entry.value} minerals. ")
                                }
                                if (hardnessData.size > 3) {
                                    append("And ${hardnessData.size - 3} more ranges.")
                                }
                            }
                        },
                    maxBars = 10
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Distribution by Status
        if (statistics.byStatus.isNotEmpty()) {
            Text(
                text = stringResource(R.string.statistics_by_status),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                BarChart(
                    data = statistics.byStatus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = buildString {
                                append("Bar chart showing distribution by status. ")
                                statistics.byStatus.entries.forEach { entry ->
                                    append("${entry.key}: ${entry.value} minerals. ")
                                }
                            }
                        },
                    maxBars = 10
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Collection Evolution Over Time
        if (statistics.addedByMonth.isNotEmpty()) {
            Text(
                text = stringResource(R.string.statistics_collection_evolution),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sort by month chronologically and limit to last 12 months
                val sortedByMonth = statistics.addedByMonth.entries
                    .sortedBy { it.key }
                    .toList()
                    .takeLast(12)
                    .associate { it.key to it.value }

                BarChart(
                    data = sortedByMonth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = buildString {
                                append("Bar chart showing collection evolution over time. ")
                                append("${sortedByMonth.size} months displayed. ")
                                val recentMonths = sortedByMonth.entries.toList().takeLast(3)
                                recentMonths.forEach { entry ->
                                    append("${entry.key}: ${entry.value} minerals added. ")
                                }
                            }
                        },
                    maxBars = 12
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Maps mineral color names to actual Color values for pie chart rendering.
 * Ensures that color segments in the chart use realistic colors.
 */
@Composable
private fun getMineralColorMapping(): Map<String, Color> {
    return mapOf(
        // Basic colors
        "Red" to Color(0xFFE53935),         // Material Red 600
        "Orange" to Color(0xFFFB8C00),      // Material Orange 600
        "Yellow" to Color(0xFFFDD835),      // Material Yellow 600
        "Green" to Color(0xFF43A047),       // Material Green 600
        "Blue" to Color(0xFF1E88E5),        // Material Blue 600
        "Purple" to Color(0xFF8E24AA),      // Material Purple 600
        "Pink" to Color(0xFFD81B60),        // Material Pink 600
        "Brown" to Color(0xFF6D4C41),       // Material Brown 600

        // Achromatic colors
        "White" to Color(0xFFFAFAFA),       // Off-white
        "Black" to Color(0xFF212121),       // Near-black
        "Gray" to Color(0xFF757575),        // Material Gray 600
        "Grey" to Color(0xFF757575),        // Alternative spelling
        "Colorless" to Color(0xFFE0E0E0),   // Light gray

        // Variations
        "Light Red" to Color(0xFFEF5350),   // Material Red 400
        "Dark Red" to Color(0xFFC62828),    // Material Red 800
        "Light Blue" to Color(0xFF42A5F5),  // Material Blue 400
        "Dark Blue" to Color(0xFF1565C0),   // Material Blue 800
        "Light Green" to Color(0xFF66BB6A), // Material Green 400
        "Dark Green" to Color(0xFF2E7D32),  // Material Green 800

        // Special colors
        "Gold" to Color(0xFFFFD700),        // Gold
        "Silver" to Color(0xFFC0C0C0),      // Silver
        "Bronze" to Color(0xFFCD7F32),      // Bronze
        "Copper" to Color(0xFFB87333)       // Copper
    )
}
