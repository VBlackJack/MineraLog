package net.meshcore.mineralog.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R

/**
 * Bottom sheet for bulk actions on selected minerals.
 * Supports delete, export, compare, and label generation operations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkActionsBottomSheet(
    selectedCount: Int,
    selectedMineralNames: List<String> = emptyList(),
    onDelete: () -> Unit,
    onExportCsv: () -> Unit,
    onGenerateLabels: (() -> Unit)? = null,
    onCompare: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
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
                Column {
                    Text(
                        text = stringResource(R.string.bulk_actions_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = stringResource(R.string.bulk_selected_count, selectedCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Compare action (only for 2-3 minerals)
            if (onCompare != null && selectedCount in 2..3) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.bulk_action_compare))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.bulk_action_compare_desc))
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.CompareArrows,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCompare()
                            onDismiss()
                        }
                )
            }

            // Delete action
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.bulk_action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteConfirmation = true }
            )

            // Export CSV action
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.bulk_action_export_csv))
                },
                supportingContent = {
                    Text(stringResource(R.string.bulk_action_export_csv_desc))
                },
                leadingContent = {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onExportCsv()
                        onDismiss()
                    }
            )

            // Generate QR Labels action (v1.5.0)
            onGenerateLabels?.let { generateLabels ->
                ListItem(
                    headlineContent = {
                        Text("Generate QR Labels")
                    },
                    supportingContent = {
                        Text("Create printable PDF with QR codes")
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            generateLabels()
                            onDismiss()
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(stringResource(R.string.bulk_delete_confirm_title))
            },
            text = {
                Column {
                    Text(stringResource(R.string.bulk_delete_confirm_message, selectedCount))

                    // Show preview of mineral names if available
                    if (selectedMineralNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val displayNames = selectedMineralNames.take(5)
                        displayNames.forEach { name ->
                            Text(
                                text = "• $name",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                        if (selectedMineralNames.size > 5) {
                            Text(
                                text = "• and ${selectedMineralNames.size - 5} more...",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
