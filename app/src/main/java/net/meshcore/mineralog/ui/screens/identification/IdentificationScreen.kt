package net.meshcore.mineralog.ui.screens.identification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.R

// Common color options for filtering
val COMMON_COLORS = listOf(
    "White", "Black", "Gray", "Red", "Pink",
    "Orange", "Yellow", "Green", "Blue", "Purple",
    "Brown", "Colorless"
)

// Common streak colors
val COMMON_STREAKS = listOf(
    "White", "Black", "Gray", "Red", "Brown",
    "Yellow", "Green", "Blue", "Colorless"
)

// Common luster types
val COMMON_LUSTERS = listOf(
    "Vitreous", "Metallic", "Pearly", "Silky",
    "Resinous", "Adamantine", "Greasy", "Dull", "Earthy"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentificationScreen(
    onNavigateBack: () -> Unit,
    onMineralClick: (String) -> Unit,
    viewModel: IdentificationViewModel = viewModel(
        factory = IdentificationViewModelFactory(
            referenceMineralRepository = (LocalContext.current.applicationContext as MineraLogApplication).referenceMineralRepository
        )
    )
) {
    val filter by viewModel.filter.collectAsState()
    val results by viewModel.filteredResults.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    var filterExpanded by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.identification_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    // Toggle filter visibility
                    IconButton(onClick = { filterExpanded = !filterExpanded }) {
                        Icon(
                            if (filterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (filterExpanded) "Collapse filters" else "Expand filters"
                        )
                    }
                    // Clear all filters
                    if (!filter.isEmpty()) {
                        IconButton(onClick = { viewModel.clearFilters() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear filters")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter section (collapsible)
            AnimatedVisibility(
                visible = filterExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FilterSection(
                    filter = filter,
                    onColorToggle = { viewModel.toggleColor(it) },
                    onHardnessChange = { min, max -> viewModel.setHardnessRange(min, max) },
                    onStreakChange = { viewModel.setStreak(it) },
                    onLusterChange = { viewModel.setLuster(it) },
                    onMagneticChange = { viewModel.setMagnetic(it) }
                )
            }

            HorizontalDivider()

            // Results section
            when (loadingState) {
                is LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is LoadingState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (loadingState as LoadingState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is LoadingState.Success -> {
                    ResultsSection(
                        results = results,
                        isEmpty = filter.isEmpty(),
                        onMineralClick = onMineralClick
                    )
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    filter: IdentificationFilter,
    onColorToggle: (String) -> Unit,
    onHardnessChange: (Float?, Float?) -> Unit,
    onStreakChange: (String?) -> Unit,
    onLusterChange: (String?) -> Unit,
    onMagneticChange: (Boolean?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Color filter (primary criterion)
        item {
            Text(
                text = stringResource(R.string.identification_filter_color),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(COMMON_COLORS) { color ->
                    FilterChip(
                        selected = filter.selectedColors.contains(color),
                        onClick = { onColorToggle(color) },
                        label = { Text(color) }
                    )
                }
            }
        }

        // Hardness filter
        item {
            HardnessFilter(
                currentMin = filter.mohsMin,
                currentMax = filter.mohsMax,
                onRangeChange = onHardnessChange
            )
        }

        // Streak filter
        item {
            DropdownFilter(
                label = stringResource(R.string.identification_filter_streak),
                options = COMMON_STREAKS,
                selected = filter.selectedStreak,
                onSelect = onStreakChange
            )
        }

        // Luster filter
        item {
            DropdownFilter(
                label = stringResource(R.string.identification_filter_luster),
                options = COMMON_LUSTERS,
                selected = filter.selectedLuster,
                onSelect = onLusterChange
            )
        }

        // Magnetism filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.identification_filter_magnetic),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filter.isMagnetic == true,
                        onClick = {
                            onMagneticChange(if (filter.isMagnetic == true) null else true)
                        },
                        label = { Text(stringResource(R.string.yes)) }
                    )
                    FilterChip(
                        selected = filter.isMagnetic == false,
                        onClick = {
                            onMagneticChange(if (filter.isMagnetic == false) null else false)
                        },
                        label = { Text(stringResource(R.string.no)) }
                    )
                }
            }
        }
    }
}

@Composable
fun HardnessFilter(
    currentMin: Float?,
    currentMax: Float?,
    onRangeChange: (Float?, Float?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.identification_filter_hardness),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { showDialog = true }) {
                Text(
                    text = if (currentMin != null || currentMax != null) {
                        "${currentMin ?: 1.0} - ${currentMax ?: 10.0}"
                    } else {
                        stringResource(R.string.identification_set_range)
                    }
                )
            }
        }

        // Preset buttons
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = currentMin == null && currentMax != null && currentMax <= 2.5f,
                    onClick = { onRangeChange(null, 2.5f) },
                    label = { Text(stringResource(R.string.identification_hardness_soft)) }
                )
            }
            item {
                FilterChip(
                    selected = currentMin != null && currentMin >= 5.5f && currentMax == null,
                    onClick = { onRangeChange(5.5f, null) },
                    label = { Text(stringResource(R.string.identification_hardness_hard)) }
                )
            }
            item {
                FilterChip(
                    selected = currentMin == null && currentMax == null,
                    onClick = { onRangeChange(null, null) },
                    label = { Text(stringResource(R.string.identification_hardness_any)) }
                )
            }
        }
    }

    // Custom range dialog
    if (showDialog) {
        HardnessRangeDialog(
            currentMin = currentMin,
            currentMax = currentMax,
            onDismiss = { showDialog = false },
            onConfirm = { min, max ->
                onRangeChange(min, max)
                showDialog = false
            }
        )
    }
}

@Composable
fun HardnessRangeDialog(
    currentMin: Float?,
    currentMax: Float?,
    onDismiss: () -> Unit,
    onConfirm: (Float?, Float?) -> Unit
) {
    var minValue by remember { mutableFloatStateOf(currentMin ?: 1f) }
    var maxValue by remember { mutableFloatStateOf(currentMax ?: 10f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.identification_hardness_range_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "${stringResource(R.string.identification_min)}: ${String.format("%.1f", minValue)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = minValue,
                    onValueChange = { minValue = it.coerceAtMost(maxValue) },
                    valueRange = 1f..10f,
                    steps = 17 // 0.5 steps
                )

                Text(
                    text = "${stringResource(R.string.identification_max)}: ${String.format("%.1f", maxValue)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = maxValue,
                    onValueChange = { maxValue = it.coerceAtLeast(minValue) },
                    valueRange = 1f..10f,
                    steps = 17
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minValue, maxValue) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun DropdownFilter(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = selected ?: stringResource(R.string.identification_any),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.identification_any)) },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    leadingIcon = if (selected == option) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun ResultsSection(
    results: List<MineralResult>,
    isEmpty: Boolean,
    onMineralClick: (String) -> Unit
) {
    when {
        isEmpty -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.identification_select_criteria),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        results.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.identification_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.identification_try_different),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "${results.size} ${stringResource(R.string.identification_results_found)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(results) { result ->
                    MineralResultCard(
                        result = result,
                        onClick = { onMineralClick(result.mineral.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MineralResultCard(
    result: MineralResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.mineral.nameFr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!result.mineral.formula.isNullOrEmpty()) {
                    Text(
                        text = result.mineral.formula,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (result.mineral.mohsMin != null || result.mineral.mohsMax != null) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "${stringResource(R.string.identification_mohs)}: ${result.mineral.mohsMin ?: "?"}-${result.mineral.mohsMax ?: "?"}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                    // Relevance score badge
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "${stringResource(R.string.identification_match)}: ${result.relevanceScore}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Extension function to check if filter is empty
private fun IdentificationFilter.isEmpty(): Boolean {
    return selectedColors.isEmpty() &&
            mohsMin == null &&
            mohsMax == null &&
            selectedStreak == null &&
            selectedLuster == null &&
            isMagnetic == null
}
