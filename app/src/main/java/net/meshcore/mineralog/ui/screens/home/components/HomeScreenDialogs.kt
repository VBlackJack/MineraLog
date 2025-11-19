package net.meshcore.mineralog.ui.screens.home.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.meshcore.mineralog.R
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.data.repository.CsvImportMode
import net.meshcore.mineralog.domain.model.FilterPreset
import net.meshcore.mineralog.ui.screens.home.*

/**
 * Container for all dialogs and bottom sheets used in HomeScreen.
 *
 * Responsibilities:
 * - Filter bottom sheet
 * - Sort bottom sheet
 * - Bulk actions bottom sheet
 * - CSV export/import dialogs
 * - Label generation dialog
 * - Loading indicators
 *
 * Extracted from HomeScreen.kt to follow Single Responsibility Principle.
 */
@Composable
fun HomeScreenDialogs(
    // Bottom sheet states
    showFilterSheet: Boolean,
    showSortSheet: Boolean,
    showBulkActionsSheet: Boolean,
    showCsvExportWarningDialog: Boolean,
    showExportCsvDialog: Boolean,
    showImportCsvDialog: Boolean,

    // Data
    filterCriteria: FilterCriteria,
    filterPresets: List<FilterPreset>,
    sortOption: SortOption,
    selectedCsvUri: Uri?,
    selectionCount: Int,
    selectedMineralNames: List<String>,
    csvExportWarningShown: Boolean,

    // Loading states
    exportState: ExportState,
    labelGenerationState: LabelGenerationState,

    // Actions
    onFilterCriteriaChange: (FilterCriteria) -> Unit,
    onClearFilter: () -> Unit,
    onSavePreset: (FilterPreset) -> Unit,
    onLoadPreset: (FilterPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onDeleteSelected: () -> Unit,
    onExportCsv: (Uri) -> Unit,
    onImportCsv: (Uri, Map<String, String>, CsvImportMode) -> Unit,
    onGenerateLabels: (Uri) -> Unit,
    onCompareClick: (() -> Unit)?,
    onMarkCsvExportWarningShown: () -> Unit,

    // Dismiss callbacks
    onDismissFilterSheet: () -> Unit,
    onDismissSortSheet: () -> Unit,
    onDismissBulkActionsSheet: () -> Unit,
    onDismissCsvExportWarningDialog: () -> Unit,
    onDismissExportCsvDialog: () -> Unit,
    onDismissImportCsvDialog: () -> Unit,
    onShowExportCsvDialog: () -> Unit,
    onShowCsvExportWarningDialog: () -> Unit,

    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // File picker for CSV export
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            onExportCsv(selectedUri)
        }
    }

    // File picker for PDF label generation (v1.5.0)
    val pdfLabelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            onGenerateLabels(selectedUri)
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            criteria = filterCriteria,
            presets = filterPresets,
            onCriteriaChange = onFilterCriteriaChange,
            onApply = { },
            onClear = onClearFilter,
            onSavePreset = onSavePreset,
            onLoadPreset = onLoadPreset,
            onDeletePreset = onDeletePreset,
            onDismiss = onDismissFilterSheet
        )
    }

    // Sort bottom sheet (Quick Win #7)
    if (showSortSheet) {
        SortBottomSheet(
            currentSort = sortOption,
            onSortSelected = onSortSelected,
            onDismiss = onDismissSortSheet
        )
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
            onCompare = onCompareClick,
            onDismiss = onDismissBulkActionsSheet
        )
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

    // Loading indicator for export
    if (exportState is ExportState.Exporting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Exporting minerals"
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Loading indicator for label generation (Quick Win #1)
    if (labelGenerationState is LabelGenerationState.Generating) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Generating PDF labels"
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.home_generating_labels),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
