package net.meshcore.mineralog.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.R
import net.meshcore.mineralog.data.repository.CsvImportMode
import net.meshcore.mineralog.data.util.CsvColumnMapper
import net.meshcore.mineralog.data.util.CsvParser

/**
 * Dialog for CSV import with preview, mode selection, and validation.
 *
 * Features:
 * - Automatic CSV parsing with encoding detection
 * - Preview of first 5 rows
 * - Import mode selection (MERGE, REPLACE, SKIP_DUPLICATES)
 * - Column mapping display
 * - Validation warnings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCsvDialog(
    csvUri: Uri,
    onDismiss: () -> Unit,
    onImport: (Uri, Map<String, String>, CsvImportMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var parseResult by remember { mutableStateOf<CsvParser.ParseResult?>(null) }
    var selectedMode by remember { mutableStateOf(CsvImportMode.MERGE) }
    var isLoading by remember { mutableStateOf(true) }
    var parseError by remember { mutableStateOf<String?>(null) }

    // Column mapping state
    val columnMappingState = remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val useAutoMappingState = remember { mutableStateOf(true) }
    var showMappingSection by remember { mutableStateOf(false) }

    // Quick Win #4: Cell value dialog for truncated content
    var selectedCellValue by remember { mutableStateOf<String?>(null) }
    var selectedCellHeader by remember { mutableStateOf<String?>(null) }

    // Parse CSV on dialog open
    LaunchedEffect(csvUri) {
        isLoading = true
        parseError = null

        try {
            val result = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(csvUri)?.use { inputStream ->
                    val parser = CsvParser()
                    parser.parse(inputStream, maxRows = 5) // Preview first 5 rows
                }
            }

            if (result != null) {
                parseResult = result
                // Initialize with auto-mapping
                columnMappingState.value = CsvColumnMapper.mapHeaders(result.headers)
            } else {
                parseError = "Failed to open CSV file"
            }
        } catch (e: Exception) {
            parseError = e.message ?: "Unknown error parsing CSV"
        } finally {
            isLoading = false
        }
    }

    // Quick Win #4: Full-value dialog for truncated cells
    if (selectedCellValue != null && selectedCellHeader != null) {
        AlertDialog(
            onDismissRequest = {
                selectedCellValue = null
                selectedCellHeader = null
            },
            title = {
                Text(
                    text = selectedCellHeader ?: "",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                SelectionContainer {
                    Text(
                        text = selectedCellValue ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics {
                            contentDescription = "Full cell value. Text is selectable for copying."
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedCellValue = null
                        selectedCellHeader = null
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.import_csv_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    isLoading -> {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Analyzing CSV file…",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    parseError != null -> {
                        // Error state
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = parseError ?: "Unknown error",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    parseResult != null -> {
                        val result = parseResult

                        // File info
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.import_csv_detected, result.headers.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = stringResource(R.string.import_csv_encoding, result.encoding.name()),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = stringResource(
                                        R.string.import_csv_delimiter,
                                        when (result.delimiter) {
                                            ',' -> "Comma (,)"
                                            ';' -> "Semicolon (;)"
                                            '\t' -> "Tab"
                                            else -> result.delimiter.toString()
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )

                                // Mapping stats
                                val unmappedCount = result.headers.size - columnMappingState.value.size
                                val mappedCount = columnMappingState.value.size
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (columnMappingState.value.values.contains("name")) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (columnMappingState.value.values.contains("name")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = stringResource(R.string.import_csv_mapped_columns, mappedCount, result.headers.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (!columnMappingState.value.values.contains("name")) {
                                    Text(
                                        text = stringResource(R.string.import_csv_name_required),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Auto-mapping toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.import_csv_auto_mapping),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = useAutoMappingState.value,
                                onCheckedChange = { enabled ->
                                    useAutoMappingState.value = enabled
                                    if (enabled) {
                                        // Re-apply auto mapping
                                        columnMappingState.value = CsvColumnMapper.mapHeaders(result.headers)
                                    }
                                }
                            )
                        }

                        // Column mapping section (collapsible)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showMappingSection = !showMappingSection }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.import_csv_column_mapping),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (showMappingSection) "▼" else "▶",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }

                        // Column mapping details (when expanded)
                        if (showMappingSection) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp)
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(result.headers) { csvHeader ->
                                        ColumnMappingRow(
                                            csvHeader = csvHeader,
                                            currentMapping = columnMappingState.value[csvHeader],
                                            onMappingChange = { newMapping ->
                                                columnMappingState.value = if (newMapping == null) {
                                                    columnMappingState.value - csvHeader
                                                } else {
                                                    columnMappingState.value + (csvHeader to newMapping)
                                                }
                                                // Disable auto-mapping when manual changes are made
                                                useAutoMappingState.value = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Import mode selector
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.import_csv_mode_label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )

                            CsvImportMode.entries.forEach { mode ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedMode == mode,
                                        onClick = { selectedMode = mode }
                                    )
                                    Text(
                                        text = when (mode) {
                                            CsvImportMode.MERGE -> stringResource(R.string.import_csv_mode_merge)
                                            CsvImportMode.REPLACE -> stringResource(R.string.import_csv_mode_replace)
                                            CsvImportMode.SKIP_DUPLICATES -> stringResource(R.string.import_csv_mode_skip)
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // CSV Preview
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.import_csv_preview_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = stringResource(R.string.import_csv_preview_rows, result.rows.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                shape = MaterialTheme.shapes.small
                            ) {
                                LazyColumn(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Headers
                                    item {
                                        Text(
                                            text = result.headers.joinToString(" | "),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    }

                                    // Data rows - Quick Win #4: Clickable cells for truncated content
                                    items(result.rows) { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            result.headers.forEachIndexed { index, header ->
                                                val cellValue = row[header] ?: ""
                                                val isTruncated = cellValue.length > 20
                                                val displayValue = if (isTruncated) cellValue.take(20) + "…" else cellValue

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .then(
                                                            if (isTruncated) {
                                                                Modifier
                                                                    .clickable {
                                                                        selectedCellValue = cellValue
                                                                        selectedCellHeader = header
                                                                    }
                                                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                                                                    .semantics {
                                                                        contentDescription = "$header: $displayValue Truncated. Tap to expand"
                                                                    }
                                                            } else {
                                                                Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                                                            }
                                                        )
                                                ) {
                                                    Text(
                                                        text = displayValue,
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }

                                                if (index < result.headers.size - 1) {
                                                    Text(" | ", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Errors/Warnings
                        if (result.errors.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.semantics {
                                    liveRegion = LiveRegionMode.Polite
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = stringResource(R.string.import_csv_errors, result.errors.size),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    result.errors.take(3).forEach { error ->
                                        Text(
                                            text = "• ${error.message}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onImport(csvUri, columnMappingState.value, selectedMode)
                    onDismiss()
                },
                enabled = !isLoading && parseError == null && parseResult != null && columnMappingState.value.values.contains("name")
            ) {
                Text(stringResource(R.string.import_csv_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Row component for mapping a CSV column to a Mineral field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnMappingRow(
    csvHeader: String,
    currentMapping: String?,
    onMappingChange: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Available mineral fields for mapping
    val availableFields = remember {
        listOf(
            "name", "group", "formula", "crystalSystem",
            "mohsMin", "mohsMax", "mohs", "cleavage", "fracture",
            "luster", "streak", "diaphaneity", "habit",
            "specificGravity", "fluorescence", "magnetic", "radioactive",
            "dimensionsMm", "weightGr", "status", "statusType",
            "qualityRating", "completeness", "notes", "tags",
            "prov_country", "prov_locality", "prov_site",
            "prov_latitude", "prov_longitude", "prov_source",
            "prov_price", "prov_estimatedValue", "prov_currency",
            "storage_place", "storage_container", "storage_box", "storage_slot"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // CSV column name
        Text(
            text = csvHeader,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )

        // Arrow
        Text(
            text = "→",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Mapping dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = currentMapping ?: "(Skip)",
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                // Skip option
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "(Skip column)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onMappingChange(null)
                        expanded = false
                    }
                )

                HorizontalDivider()

                // Field options
                availableFields.forEach { field ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = field,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (field == currentMapping) FontWeight.Bold else FontWeight.Normal,
                                color = if (field == "name") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onMappingChange(field)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
