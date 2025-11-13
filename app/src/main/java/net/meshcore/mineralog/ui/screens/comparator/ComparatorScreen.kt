package net.meshcore.mineralog.ui.screens.comparator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.R
import net.meshcore.mineralog.domain.model.Mineral

/**
 * Comparator screen for side-by-side mineral comparison.
 * Supports 2-3 minerals with diff highlighting for different values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparatorScreen(
    mineralIds: List<String>,
    onNavigateBack: () -> Unit,
    viewModel: ComparatorViewModel = viewModel(
        factory = ComparatorViewModelFactory(mineralIds)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compare_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ComparatorUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Loading comparison"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ComparatorUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            is ComparatorUiState.Success -> {
                ComparisonContent(
                    minerals = state.minerals,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
private fun ComparisonContent(
    minerals: List<Mineral>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // Header row with mineral names
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            // Property label column
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.compare_property),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Mineral columns
            minerals.forEach { mineral ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mineral.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider()

        // Basic properties section
        ComparisonSection(
            title = stringResource(R.string.detail_section_basic),
            minerals = minerals
        ) { mineral ->
            listOf(
                stringResource(R.string.field_name) to mineral.name,
                stringResource(R.string.field_group) to (mineral.group ?: "-"),
                stringResource(R.string.field_formula) to (mineral.formula ?: "-")
            )
        }

        // Physical properties section
        ComparisonSection(
            title = stringResource(R.string.detail_section_physical),
            minerals = minerals
        ) { mineral ->
            listOf(
                stringResource(R.string.field_hardness) to
                    if (mineral.mohsMin != null && mineral.mohsMax != null) {
                        "${mineral.mohsMin} - ${mineral.mohsMax}"
                    } else {
                        "-"
                    },
                stringResource(R.string.field_streak) to (mineral.streak ?: "-"),
                stringResource(R.string.field_luster) to (mineral.luster ?: "-"),
                stringResource(R.string.field_crystal_system) to (mineral.crystalSystem ?: "-"),
                stringResource(R.string.field_specific_gravity) to (mineral.specificGravity?.toString() ?: "-"),
                stringResource(R.string.field_cleavage) to (mineral.cleavage ?: "-"),
                stringResource(R.string.field_fracture) to (mineral.fracture ?: "-")
            )
        }

        // Special properties section
        ComparisonSection(
            title = stringResource(R.string.detail_section_special),
            minerals = minerals
        ) { mineral ->
            listOf(
                stringResource(R.string.field_fluorescence) to (mineral.fluorescence ?: "-"),
                stringResource(R.string.field_radioactivity) to if (mineral.radioactive) "Yes" else "No",
                stringResource(R.string.field_magnetism) to if (mineral.magnetic) "Yes" else "No"
            )
        }

        // Status section
        ComparisonSection(
            title = stringResource(R.string.status_section_title),
            minerals = minerals
        ) { mineral ->
            listOf(
                stringResource(R.string.status_type) to mineral.statusType,
                stringResource(R.string.status_quality_rating) to (mineral.qualityRating?.toString() ?: "-"),
                stringResource(R.string.status_completeness) to "${mineral.completeness}%"
            )
        }

        // Provenance section
        ComparisonSection(
            title = stringResource(R.string.provenance_title),
            minerals = minerals
        ) { mineral ->
            listOf(
                stringResource(R.string.provenance_country) to (mineral.provenance?.country ?: "-"),
                stringResource(R.string.provenance_locality) to (mineral.provenance?.locality ?: "-"),
                stringResource(R.string.provenance_acquisition_date) to (mineral.provenance?.acquiredAt?.toString() ?: "-"),
                stringResource(R.string.provenance_estimated_value) to
                    if (mineral.provenance?.estimatedValue != null) {
                        "${mineral.provenance.estimatedValue} ${mineral.provenance.currency ?: ""}"
                    } else {
                        "-"
                    }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ComparisonSection(
    title: String,
    minerals: List<Mineral>,
    modifier: Modifier = Modifier,
    properties: (Mineral) -> List<Pair<String, String>>
) {
    Column(modifier = modifier) {
        // Section header
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Get all properties for each mineral
        val mineralProperties = minerals.map { properties(it) }
        val propertyCount = mineralProperties.firstOrNull()?.size ?: 0

        // For each property row
        for (propertyIndex in 0 until propertyCount) {
            val propertyName = mineralProperties.first()[propertyIndex].first
            val values = mineralProperties.map { it[propertyIndex].second }

            // Check if values differ
            val valuesDiffer = values.distinct().size > 1

            ComparisonRow(
                propertyName = propertyName,
                values = values,
                highlight = valuesDiffer
            )

            if (propertyIndex < propertyCount - 1) {
                HorizontalDivider()
            }
        }

        HorizontalDivider(thickness = 2.dp)
    }
}

@Composable
private fun ComparisonRow(
    propertyName: String,
    values: List<String>,
    highlight: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (highlight) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                if (highlight) {
                    contentDescription = "$propertyName: Different values - ${values.joinToString(", ")}"
                }
            }
    ) {
        // Property name with difference indicator
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (highlight) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Different values",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = propertyName,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Values for each mineral
        values.forEach { value ->
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
