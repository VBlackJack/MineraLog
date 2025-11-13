package net.meshcore.mineralog.ui.screens.home

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    onImport: (Uri, CsvImportMode) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var parseResult by remember { mutableStateOf<CsvParser.ParseResult?>(null) }
    var selectedMode by remember { mutableStateOf(CsvImportMode.MERGE) }
    var isLoading by remember { mutableStateOf(true) }
    var parseError by remember { mutableStateOf<String?>(null) }

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
            } else {
                parseError = "Failed to open CSV file"
            }
        } catch (e: Exception) {
            parseError = e.message ?: "Unknown error parsing CSV"
        } finally {
            isLoading = false
        }
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
                        val result = parseResult!!
                        val columnMapping = CsvColumnMapper.mapHeaders(result.headers)

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
                                val unmappedCount = result.headers.size - columnMapping.size
                                if (unmappedCount > 0) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.import_csv_unmapped, unmappedCount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
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

                                    // Data rows
                                    items(result.rows) { row ->
                                        Text(
                                            text = result.headers.joinToString(" | ") { header ->
                                                row[header]?.take(20) ?: ""
                                            },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // Errors/Warnings
                        if (result.errors.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
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
                    onImport(csvUri, selectedMode)
                    onDismiss()
                },
                enabled = !isLoading && parseError == null && parseResult != null
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
