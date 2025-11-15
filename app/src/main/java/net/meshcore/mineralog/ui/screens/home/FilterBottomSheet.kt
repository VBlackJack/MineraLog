package net.meshcore.mineralog.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.domain.model.FilterPreset
import net.meshcore.mineralog.ui.components.MineralFieldValues
import java.time.Instant

/**
 * Bottom sheet for advanced mineral filtering.
 * Supports multiple filter criteria and preset management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    criteria: FilterCriteria,
    presets: List<FilterPreset>,
    onCriteriaChange: (FilterCriteria) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onSavePreset: (FilterPreset) -> Unit,
    onLoadPreset: (FilterPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentCriteria by remember(criteria) { mutableStateOf(criteria) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var expandedSections by remember { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Active filter count badge
            if (currentCriteria.activeCount() > 0) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text("${currentCriteria.activeCount()} ${stringResource(R.string.filter_active_count)}")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.FilterList, contentDescription = null)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Presets section
            if (presets.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.filter_presets),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = { onLoadPreset(preset) },
                            label = { Text(preset.name) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Bookmark,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Groups filter
            FilterSection(
                title = stringResource(R.string.filter_by_group),
                expanded = "groups" in expandedSections,
                onExpandChange = { expanded ->
                    expandedSections = if (expanded) {
                        expandedSections + "groups"
                    } else {
                        expandedSections - "groups"
                    }
                }
            ) {
                MultiSelectChips(
                    selected = currentCriteria.groups,
                    options = listOf(
                        "Silicates", "Oxides", "Sulfides", "Carbonates",
                        "Halides", "Sulfates", "Phosphates", "Native Elements"
                    ),
                    onSelectionChange = { selected ->
                        currentCriteria = currentCriteria.copy(groups = selected)
                    }
                )
            }

            // Countries filter
            FilterSection(
                title = stringResource(R.string.filter_by_country),
                expanded = "countries" in expandedSections,
                onExpandChange = { expanded ->
                    expandedSections = if (expanded) {
                        expandedSections + "countries"
                    } else {
                        expandedSections - "countries"
                    }
                }
            ) {
                MultiSelectChips(
                    selected = currentCriteria.countries,
                    options = listOf(
                        "USA", "Canada", "Brazil", "Peru", "Mexico",
                        "Morocco", "Madagascar", "Namibia", "Pakistan",
                        "China", "Russia", "Australia", "Germany", "France"
                    ),
                    onSelectionChange = { selected ->
                        currentCriteria = currentCriteria.copy(countries = selected)
                    }
                )
            }

            // Crystal systems filter
            val context = LocalContext.current
            val crystalSystemsOptions = remember { MineralFieldValues.getCrystalSystems(context) }
            FilterSection(
                title = stringResource(R.string.filter_by_crystal_system),
                expanded = "crystalSystems" in expandedSections,
                onExpandChange = { expanded ->
                    expandedSections = if (expanded) {
                        expandedSections + "crystalSystems"
                    } else {
                        expandedSections - "crystalSystems"
                    }
                }
            ) {
                MultiSelectChips(
                    selected = currentCriteria.crystalSystems,
                    options = crystalSystemsOptions,
                    onSelectionChange = { selected ->
                        currentCriteria = currentCriteria.copy(crystalSystems = selected)
                    }
                )
            }

            // Mohs hardness filter
            FilterSection(
                title = stringResource(R.string.filter_by_hardness),
                expanded = "hardness" in expandedSections,
                onExpandChange = { expanded ->
                    expandedSections = if (expanded) {
                        expandedSections + "hardness"
                    } else {
                        expandedSections - "hardness"
                    }
                }
            ) {
                var mohsMin by remember(currentCriteria.mohsMin) {
                    mutableFloatStateOf(currentCriteria.mohsMin ?: 1f)
                }
                var mohsMax by remember(currentCriteria.mohsMax) {
                    mutableFloatStateOf(currentCriteria.mohsMax ?: 10f)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.filter_hardness_range, mohsMin, mohsMax),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .semantics { this.contentDescription = "" } // Decorative - slider has stateDescription
                    )

                    RangeSlider(
                        value = mohsMin..mohsMax,
                        onValueChange = { range ->
                            mohsMin = range.start
                            mohsMax = range.endInclusive
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        onValueChangeFinished = {
                            currentCriteria = currentCriteria.copy(
                                mohsMin = if (mohsMin > 1f) mohsMin else null,
                                mohsMax = if (mohsMax < 10f) mohsMax else null
                            )
                        },
                        modifier = Modifier.semantics {
                            stateDescription = "Hardness range from ${mohsMin.toInt()} to ${mohsMax.toInt()}"
                        }
                    )
                }
            }

            // Status filter
            FilterSection(
                title = stringResource(R.string.filter_by_status),
                expanded = "status" in expandedSections,
                onExpandChange = { expanded ->
                    expandedSections = if (expanded) {
                        expandedSections + "status"
                    } else {
                        expandedSections - "status"
                    }
                }
            ) {
                MultiSelectChips(
                    selected = currentCriteria.statusTypes,
                    options = listOf("Collection", "Display", "Loaned", "Restoration", "For Sale"),
                    onSelectionChange = { selected ->
                        currentCriteria = currentCriteria.copy(statusTypes = selected)
                    }
                )
            }

            // Quality filter
            FilterSection(
                title = stringResource(R.string.filter_by_quality),
                expanded = "quality" in expandedSections,
                onExpandChange = { expanded ->
                    expandedSections = if (expanded) {
                        expandedSections + "quality"
                    } else {
                        expandedSections - "quality"
                    }
                }
            ) {
                var qualityMin by remember(currentCriteria.qualityMin) {
                    mutableIntStateOf(currentCriteria.qualityMin ?: 1)
                }
                var qualityMax by remember(currentCriteria.qualityMax) {
                    mutableIntStateOf(currentCriteria.qualityMax ?: 5)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.filter_quality_range, qualityMin, qualityMax),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .semantics { this.contentDescription = "" } // Decorative - slider has stateDescription
                    )

                    RangeSlider(
                        value = qualityMin.toFloat()..qualityMax.toFloat(),
                        onValueChange = { range ->
                            qualityMin = range.start.toInt()
                            qualityMax = range.endInclusive.toInt()
                        },
                        valueRange = 1f..5f,
                        steps = 3,
                        onValueChangeFinished = {
                            currentCriteria = currentCriteria.copy(
                                qualityMin = if (qualityMin > 1) qualityMin else null,
                                qualityMax = if (qualityMax < 5) qualityMax else null
                            )
                        },
                        modifier = Modifier.semantics {
                            stateDescription = "Quality range from $qualityMin to $qualityMax"
                        }
                    )
                }
            }

            // Photo filter
            FilterSection(
                title = stringResource(R.string.filter_by_photos),
                expanded = "photos" in expandedSections,
                onExpandChange = { expanded ->
                    expandedSections = if (expanded) {
                        expandedSections + "photos"
                    } else {
                        expandedSections - "photos"
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentCriteria.hasPhotos == true,
                        onClick = {
                            currentCriteria = currentCriteria.copy(
                                hasPhotos = if (currentCriteria.hasPhotos == true) null else true
                            )
                        },
                        label = { Text(stringResource(R.string.filter_has_photos)) }
                    )
                    FilterChip(
                        selected = currentCriteria.hasPhotos == false,
                        onClick = {
                            currentCriteria = currentCriteria.copy(
                                hasPhotos = if (currentCriteria.hasPhotos == false) null else false
                            )
                        },
                        label = { Text(stringResource(R.string.filter_no_photos)) }
                    )
                }
            }

            // Fluorescence filter
            FilterSection(
                title = stringResource(R.string.filter_by_fluorescence),
                expanded = "fluorescence" in expandedSections,
                onExpandChange = { expanded ->
                    expandedSections = if (expanded) {
                        expandedSections + "fluorescence"
                    } else {
                        expandedSections - "fluorescence"
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentCriteria.fluorescent == true,
                        onClick = {
                            currentCriteria = currentCriteria.copy(
                                fluorescent = if (currentCriteria.fluorescent == true) null else true
                            )
                        },
                        label = { Text(stringResource(R.string.filter_fluorescent)) }
                    )
                    FilterChip(
                        selected = currentCriteria.fluorescent == false,
                        onClick = {
                            currentCriteria = currentCriteria.copy(
                                fluorescent = if (currentCriteria.fluorescent == false) null else false
                            )
                        },
                        label = { Text(stringResource(R.string.filter_not_fluorescent)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        currentCriteria = FilterCriteria.EMPTY
                        onClear()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.filter_clear))
                }

                if (currentCriteria.activeCount() > 0) {
                    OutlinedButton(
                        onClick = { showSavePresetDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.filter_save_preset))
                    }
                }

                Button(
                    onClick = {
                        onCriteriaChange(currentCriteria)
                        onApply()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = currentCriteria.activeCount() > 0
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.filter_apply))
                }
            }
        }
    }

    // Save preset dialog
    if (showSavePresetDialog) {
        var presetName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text(stringResource(R.string.filter_save_preset)) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text(stringResource(R.string.filter_preset_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSavePreset(
                                FilterPreset(
                                    name = presetName,
                                    criteria = currentCriteria,
                                    createdAt = Instant.now(),
                                    updatedAt = Instant.now()
                                )
                            )
                            showSavePresetDialog = false
                        }
                    },
                    enabled = presetName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Expandable filter section with title and content.
 */
@Composable
private fun FilterSection(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (expanded) {
                IconButton(onClick = { onExpandChange(false) }) {
                    Icon(Icons.Default.ExpandLess, contentDescription = "Collapse")
                }
            } else {
                IconButton(onClick = { onExpandChange(true) }) {
                    Icon(Icons.Default.ExpandMore, contentDescription = "Expand")
                }
            }
        }

        if (expanded) {
            content()
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

/**
 * Multi-select chips for list-based filters.
 */
@Composable
private fun MultiSelectChips(
    selected: List<String>,
    options: List<String>,
    onSelectionChange: (List<String>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        options.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { option ->
                    FilterChip(
                        selected = option in selected,
                        onClick = {
                            onSelectionChange(
                                if (option in selected) {
                                    selected - option
                                } else {
                                    selected + option
                                }
                            )
                        },
                        label = { Text(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Add empty spacers for remaining cells in row
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
