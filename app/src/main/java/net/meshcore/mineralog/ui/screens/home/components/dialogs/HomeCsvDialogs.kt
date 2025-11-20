package net.meshcore.mineralog.ui.screens.home.components.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.meshcore.mineralog.data.repository.CsvImportMode
import net.meshcore.mineralog.ui.screens.home.CsvExportWarningDialog
import net.meshcore.mineralog.ui.screens.home.ExportCsvDialog
import net.meshcore.mineralog.ui.screens.home.ImportCsvDialog

/**
 * Container for CSV export and import related dialogs used in HomeScreen.
 *
 * Responsibilities:
 * - CSV export warning dialog (first-time user education)
 * - CSV export dialog with column selection
 * - CSV import dialog with preview and mapping
 * - File picker launchers for CSV operations
 *
 * Part of Sprint 3 refactoring to reduce prop drilling and improve modularity.
 */
@Composable
fun HomeCsvDialogs(
    // Dialog states
    showCsvExportWarningDialog: Boolean,
    showExportCsvDialog: Boolean,
    showImportCsvDialog: Boolean,
    csvExportWarningShown: Boolean,

    // Data
    selectedCsvUri: Uri?,
    selectionCount: Int,

    // Actions
    onExportCsv: (Uri) -> Unit,
    onImportCsv: (Uri, Map<String, String>, CsvImportMode) -> Unit,
    onMarkCsvExportWarningShown: () -> Unit,

    // Dismiss callbacks
    onDismissCsvExportWarningDialog: () -> Unit,
    onDismissExportCsvDialog: () -> Unit,
    onDismissImportCsvDialog: () -> Unit,
    onShowExportCsvDialog: () -> Unit,

    modifier: Modifier = Modifier
) {
    // File picker for CSV export
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            onExportCsv(selectedUri)
        }
    }

    // CSV export warning dialog (shown only once)
    if (showCsvExportWarningDialog) {
        CsvExportWarningDialog(
            onDismiss = onDismissCsvExportWarningDialog,
            onProceed = { dontShowAgain ->
                if (dontShowAgain) {
                    onMarkCsvExportWarningShown()
                }
                onDismissCsvExportWarningDialog()
                onShowExportCsvDialog()
            }
        )
    }

    // CSV export dialog
    if (showExportCsvDialog) {
        ExportCsvDialog(
            selectedCount = selectionCount,
            onDismiss = onDismissExportCsvDialog,
            onExport = { selectedColumns ->
                onDismissExportCsvDialog()
                // Generate filename with timestamp
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                    .format(java.util.Date())
                csvExportLauncher.launch("mineralog_export_$timestamp.csv")
            }
        )
    }

    // CSV import dialog
    selectedCsvUri?.let { uri ->
        if (showImportCsvDialog) {
            ImportCsvDialog(
                csvUri = uri,
                onDismiss = onDismissImportCsvDialog,
                onImport = onImportCsv
            )
        }
    }
}
