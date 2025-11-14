package net.meshcore.mineralog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.data.util.CsvColumnMapper

/**
 * Dialog for manual CSV column mapping.
 *
 * Features:
 * - Auto-mapping with CsvColumnMapper pre-filled
 * - Dropdown per CSV header to select domain field
 * - Preview of first 3 rows with mapping applied
 * - Skip unmapped columns
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnMappingDialog(
    csvHeaders: List<String>,
    previewRows: List<Map<String, String>> = emptyList(),
    autoMapping: Map<String, String> = emptyMap(),
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    // State for column mapping (CSV header → domain field)
    var columnMapping by remember {
        mutableStateOf(autoMapping.toMutableMap())
    }

    // Available domain fields for mapping
    val domainFields = remember {
        listOf(
            "" to "Skip (unmapped)",
            "name" to "Name *",
            "group" to "Group",
            "formula" to "Formula",
            "crystalSystem" to "Crystal System",
            "mohs" to "Mohs Hardness",
            "mohsMin" to "Mohs Min",
            "mohsMax" to "Mohs Max",
            "cleavage" to "Cleavage",
            "fracture" to "Fracture",
            "luster" to "Luster",
            "streak" to "Streak",
            "diaphaneity" to "Diaphaneity",
            "habit" to "Habit",
            "specificGravity" to "Specific Gravity",
            "fluorescence" to "Fluorescence",
            "magnetic" to "Magnetic",
            "radioactive" to "Radioactive",
            "dimensionsMm" to "Dimensions (mm)",
            "weightGr" to "Weight (g)",
            "status" to "Status",
            "statusType" to "Status Type",
            "qualityRating" to "Quality Rating",
            "completeness" to "Completeness",
            "prov_country" to "Country",
            "prov_locality" to "Locality",
            "prov_site" to "Site",
            "prov_latitude" to "Latitude",
            "prov_longitude" to "Longitude",
            "prov_source" to "Source",
            "prov_price" to "Price",
            "prov_estimatedValue" to "Estimated Value",
            "prov_currency" to "Currency",
            "storage_place" to "Storage Place",
            "storage_container" to "Container",
            "storage_box" to "Box",
            "storage_slot" to "Slot",
            "notes" to "Notes",
            "tags" to "Tags"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.TableChart,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Map CSV Columns",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Map each CSV column to a field. Required fields are marked with *.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Column mapping list
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(csvHeaders) { csvHeader ->
                            ColumnMappingRow(
                                csvHeader = csvHeader,
                                currentMapping = columnMapping[csvHeader] ?: "",
                                domainFields = domainFields,
                                onMappingChange = { newMapping ->
                                    columnMapping = columnMapping.toMutableMap().apply {
                                        if (newMapping.isEmpty()) {
                                            remove(csvHeader)
                                        } else {
                                            put(csvHeader, newMapping)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Preview section (if rows provided)
                if (previewRows.isNotEmpty()) {
                    HorizontalDivider()

                    Text(
                        text = "Preview (first ${previewRows.size} rows)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            previewRows.forEachIndexed { index, row ->
                                Text(
                                    text = "Row ${index + 1}: " + columnMapping.entries
                                        .take(3)
                                        .joinToString(", ") { (csvHeader, domainField) ->
                                            "$domainField=${row[csvHeader]?.take(20) ?: ""}"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // Validation warning
                if (!columnMapping.values.contains("name")) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠️ Required: You must map a column to 'Name'",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(columnMapping.filterValues { it.isNotEmpty() })
                },
                enabled = columnMapping.values.contains("name")
            ) {
                Text("Import with Mapping")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnMappingRow(
    csvHeader: String,
    currentMapping: String,
    domainFields: List<Pair<String, String>>,
    onMappingChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // CSV header (source)
        Text(
            text = csvHeader,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        // Arrow
        Text(
            text = "→",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Domain field dropdown (target)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1.5f)
        ) {
            OutlinedTextField(
                value = domainFields.find { it.first == currentMapping }?.second ?: "Skip (unmapped)",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(),
                textStyle = MaterialTheme.typography.bodySmall
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                domainFields.forEach { (fieldKey, fieldLabel) ->
                    DropdownMenuItem(
                        text = { Text(fieldLabel, style = MaterialTheme.typography.bodySmall) },
                        onClick = {
                            onMappingChange(fieldKey)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
