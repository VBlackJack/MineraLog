package net.meshcore.mineralog.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R

data class CsvColumn(
    val id: String,
    val nameResId: Int,
    val category: CsvColumnCategory
)

enum class CsvColumnCategory {
    BASIC,
    PHYSICAL,
    SPECIAL,
    STATUS,
    PROVENANCE,
    STORAGE,
    OTHER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportCsvDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onExport: (Set<String>) -> Unit
) {
    // All available columns
    val availableColumns = remember {
        listOf(
            // Basic
            CsvColumn("name", R.string.detail_label_name, CsvColumnCategory.BASIC),
            CsvColumn("group", R.string.detail_label_group, CsvColumnCategory.BASIC),
            CsvColumn("formula", R.string.detail_label_formula, CsvColumnCategory.BASIC),
            CsvColumn("streak", R.string.detail_label_streak, CsvColumnCategory.BASIC),
            CsvColumn("luster", R.string.detail_label_luster, CsvColumnCategory.BASIC),

            // Physical
            CsvColumn("mohs", R.string.detail_label_hardness, CsvColumnCategory.PHYSICAL),
            CsvColumn("crystal_system", R.string.detail_label_crystal_system, CsvColumnCategory.PHYSICAL),
            CsvColumn("specific_gravity", R.string.detail_label_specific_gravity, CsvColumnCategory.PHYSICAL),
            CsvColumn("cleavage", R.string.detail_label_cleavage, CsvColumnCategory.PHYSICAL),
            CsvColumn("fracture", R.string.detail_label_fracture, CsvColumnCategory.PHYSICAL),
            CsvColumn("diaphaneity", R.string.field_diaphaneity, CsvColumnCategory.PHYSICAL),

            // Special Properties
            CsvColumn("fluorescence", R.string.field_fluorescence, CsvColumnCategory.SPECIAL),
            CsvColumn("radioactivity", R.string.field_radioactivity, CsvColumnCategory.SPECIAL),
            CsvColumn("magnetism", R.string.field_magnetism, CsvColumnCategory.SPECIAL),

            // Measurements
            CsvColumn("dimensions", R.string.detail_label_dimensions, CsvColumnCategory.OTHER),
            CsvColumn("weight", R.string.detail_label_weight, CsvColumnCategory.OTHER),

            // Status
            CsvColumn("status_type", R.string.status_type, CsvColumnCategory.STATUS),
            CsvColumn("status_date", R.string.status_loaned_date, CsvColumnCategory.STATUS),
            CsvColumn("quality", R.string.status_quality_rating, CsvColumnCategory.STATUS),
            CsvColumn("completeness", R.string.status_completeness, CsvColumnCategory.STATUS),

            // Provenance
            CsvColumn("prov_country", R.string.provenance_country, CsvColumnCategory.PROVENANCE),
            CsvColumn("prov_locality", R.string.provenance_locality, CsvColumnCategory.PROVENANCE),
            CsvColumn("prov_date", R.string.provenance_acquisition_date, CsvColumnCategory.PROVENANCE),
            CsvColumn("prov_value", R.string.provenance_estimated_value, CsvColumnCategory.PROVENANCE),
            CsvColumn("prov_currency", R.string.field_value_currency, CsvColumnCategory.PROVENANCE),

            // Storage
            CsvColumn("storage_location", R.string.storage_location, CsvColumnCategory.STORAGE),
            CsvColumn("storage_box", R.string.storage_box, CsvColumnCategory.STORAGE),
            CsvColumn("storage_position", R.string.storage_slot, CsvColumnCategory.STORAGE),

            // Other
            CsvColumn("notes", R.string.field_notes, CsvColumnCategory.OTHER),
            CsvColumn("tags", R.string.field_tags, CsvColumnCategory.OTHER)
        )
    }

    // Start with all columns selected by default
    var selectedColumns by remember { mutableStateOf(availableColumns.map { it.id }.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.export_csv_title))
                Text(
                    text = stringResource(R.string.export_csv_subtitle, selectedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Quick Win #10: Column counter
                Text(
                    text = "${selectedColumns.size} of ${availableColumns.size} columns selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Select All / Deselect All
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { selectedColumns = availableColumns.map { it.id }.toSet() }
                    ) {
                        Text(stringResource(R.string.action_select_all))
                    }

                    TextButton(
                        onClick = { selectedColumns = emptySet() }
                    ) {
                        Text(stringResource(R.string.action_deselect_all))
                    }
                }

                HorizontalDivider()

                // Column selection list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    // Group by category
                    CsvColumnCategory.entries.forEach { category ->
                        val columnsInCategory = availableColumns.filter { it.category == category }

                        if (columnsInCategory.isNotEmpty()) {
                            // Quick Win #6: Category header with select/deselect all
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(getCategoryNameResId(category)),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Quick Win #6: Category select/deselect button
                                    val allCategorySelected = columnsInCategory.all { it.id in selectedColumns }
                                    TextButton(
                                        onClick = {
                                            val categoryIds = columnsInCategory.map { it.id }
                                            selectedColumns = if (allCategorySelected) {
                                                selectedColumns - categoryIds.toSet()
                                            } else {
                                                selectedColumns + categoryIds.toSet()
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = if (allCategorySelected) "Deselect" else "Select",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }

                            items(columnsInCategory) { column ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = column.id in selectedColumns,
                                        onCheckedChange = { checked ->
                                            selectedColumns = if (checked) {
                                                selectedColumns + column.id
                                            } else {
                                                selectedColumns - column.id
                                            }
                                        }
                                    )
                                    Text(
                                        text = stringResource(column.nameResId),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onExport(selectedColumns)
                    onDismiss()
                },
                enabled = selectedColumns.isNotEmpty()
            ) {
                Text(stringResource(R.string.action_export))
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
private fun getCategoryNameResId(category: CsvColumnCategory): Int {
    return when (category) {
        CsvColumnCategory.BASIC -> R.string.detail_section_basic
        CsvColumnCategory.PHYSICAL -> R.string.detail_section_physical
        CsvColumnCategory.SPECIAL -> R.string.detail_section_special
        CsvColumnCategory.STATUS -> R.string.status_section_title
        CsvColumnCategory.PROVENANCE -> R.string.provenance_title
        CsvColumnCategory.STORAGE -> R.string.storage_title
        CsvColumnCategory.OTHER -> R.string.detail_section_notes
    }
}
