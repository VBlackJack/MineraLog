package net.meshcore.mineralog.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R

/**
 * Warning dialog shown on first CSV export to inform users about CSV limitations.
 *
 * This dialog appears only once per installation (controlled by user preference)
 * and educates users that:
 * - CSV exports are for data analysis only
 * - CSV cannot be re-imported
 * - ZIP exports should be used for complete backups
 *
 * @param onDismiss Callback when dialog is dismissed without proceeding
 * @param onProceed Callback when user acknowledges the warning and wants to proceed
 * @param onDontShowAgain Callback when user checks "don't show again" - should update preference
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvExportWarningDialog(
    onDismiss: () -> Unit,
    onProceed: (dontShowAgain: Boolean) -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.csv_export_warning_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.csv_export_warning_message),
                    style = MaterialTheme.typography.bodyMedium
                )

                // "Don't show this again" checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it }
                    )
                    Text(
                        text = stringResource(R.string.csv_export_warning_checkbox),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onProceed(dontShowAgain) }
            ) {
                Text(stringResource(R.string.csv_export_warning_understand))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
