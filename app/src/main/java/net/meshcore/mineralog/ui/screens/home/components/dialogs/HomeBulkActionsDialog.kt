package net.meshcore.mineralog.ui.screens.home.components.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.meshcore.mineralog.ui.screens.home.BulkActionsBottomSheet

/**
 * Container for bulk actions dialog used in HomeScreen.
 *
 * Responsibilities:
 * - Bulk actions bottom sheet (delete, export CSV, generate labels, compare)
 * - File picker launcher for PDF label generation
 * - File picker launcher for PDF catalog export
 * - Coordination between bulk actions and related dialogs
 *
 * Part of Sprint 3 refactoring to reduce prop drilling and improve modularity.
 */
@Composable
fun HomeBulkActionsDialog(
    // Dialog state
    showBulkActionsSheet: Boolean,

    // Data
    selectionCount: Int,
    selectedMineralNames: List<String>,
    csvExportWarningShown: Boolean,

    // Actions
    onDeleteSelected: () -> Unit,
    onGenerateLabels: (Uri) -> Unit,
    onExportCatalog: (Uri) -> Unit,
    onCompareClick: (() -> Unit)?,

    // Navigation to other dialogs
    onShowExportCsvDialog: () -> Unit,
    onShowCsvExportWarningDialog: () -> Unit,

    // Dismiss callback
    onDismissBulkActionsSheet: () -> Unit,

    modifier: Modifier = Modifier
) {
    // File picker for PDF label generation (v1.5.0)
    val pdfLabelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            onGenerateLabels(selectedUri)
        }
    }

    // File picker for PDF catalog export
    val pdfCatalogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            onExportCatalog(selectedUri)
        }
    }

    // Bulk actions bottom sheet
    if (showBulkActionsSheet) {
        BulkActionsBottomSheet(
            selectedCount = selectionCount,
            selectedMineralNames = selectedMineralNames,
            onDelete = onDeleteSelected,
            onExportCsv = {
                onDismissBulkActionsSheet()
                // Check if warning has been shown before
                if (csvExportWarningShown) {
                    onShowExportCsvDialog()
                } else {
                    onShowCsvExportWarningDialog()
                }
            },
            onGenerateLabels = {
                onDismissBulkActionsSheet()
                // Generate filename with timestamp
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                    .format(java.util.Date())
                pdfLabelLauncher.launch("mineralog_labels_$timestamp.pdf")
            },
            onExportCatalog = {
                onDismissBulkActionsSheet()
                // Generate filename with date
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
                pdfCatalogLauncher.launch("MineraLog_Catalog_$timestamp.pdf")
            },
            onCompare = onCompareClick,
            onDismiss = onDismissBulkActionsSheet
        )
    }
}
