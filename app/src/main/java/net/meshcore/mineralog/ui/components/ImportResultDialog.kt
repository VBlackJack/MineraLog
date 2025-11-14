package net.meshcore.mineralog.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.data.repository.ImportResult

/**
 * Dialog to display CSV import results with statistics and error details.
 *
 * Features:
 * - Statistics summary (imported/skipped counts)
 * - Scrollable error list with line numbers
 * - Copy to clipboard functionality
 * - Color-coded success/warning/error states
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportResultDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasErrors = result.errors.isNotEmpty()
    val isFullSuccess = result.imported > 0 && result.skipped == 0 && !hasErrors

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when {
                    isFullSuccess -> Icons.Default.CheckCircle
                    hasErrors -> Icons.Default.Warning
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = when {
                    isFullSuccess -> MaterialTheme.colorScheme.primary
                    hasErrors -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        },
        title = {
            Text(
                text = "Import Complete",
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
                // Statistics summary
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isFullSuccess -> MaterialTheme.colorScheme.primaryContainer
                            hasErrors -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ Imported:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${result.imported}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (result.skipped > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ Skipped:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${result.skipped}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        if (hasErrors) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "❌ Errors:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${result.errors.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Error list (if any)
                if (hasErrors) {
                    HorizontalDivider()

                    Text(
                        text = "Error Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 250.dp)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(result.errors.take(100)) { error ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            if (result.errors.size > 100) {
                                item {
                                    Text(
                                        text = "... and ${result.errors.size - 100} more errors (click Copy to see all)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Italic
                                    )
                                }
                            }
                        }
                    }
                }

                // Success message (if no errors)
                if (isFullSuccess) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "All minerals imported successfully!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (hasErrors) {
                TextButton(
                    onClick = {
                        copyErrorsToClipboard(context, result)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Errors")
                }
            }
        }
    )
}

/**
 * Copy import errors to clipboard for sharing/debugging.
 */
private fun copyErrorsToClipboard(context: Context, result: ImportResult) {
    val errorText = buildString {
        appendLine("MineraLog CSV Import Results")
        appendLine("============================")
        appendLine("Imported: ${result.imported}")
        appendLine("Skipped: ${result.skipped}")
        appendLine("Errors: ${result.errors.size}")
        appendLine()
        appendLine("Error Details:")
        appendLine("--------------")
        result.errors.forEach { error ->
            appendLine("• $error")
        }
    }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("CSV Import Errors", errorText)
    clipboard.setPrimaryClip(clip)

    // Note: Snackbar notification should be handled by the caller
}
