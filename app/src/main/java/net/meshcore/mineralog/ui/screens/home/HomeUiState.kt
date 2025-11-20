package net.meshcore.mineralog.ui.screens.home

import android.net.Uri
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.domain.model.Mineral

/**
 * Unified UI state for HomeScreen.
 *
 * Replaces 14 scattered StateFlow collections with a single structured state.
 * Centralizes dialog state management previously scattered across Composables.
 *
 * Phase 1 - State Centralization (Sprint 2)
 * Benefits:
 * - Single source of truth for UI state
 * - Easier testing (state is data class)
 * - Better state preservation during recomposition
 * - Reduced boilerplate in HomeScreen composable
 */
data class HomeUiState(
    // Core Data
    val minerals: List<Mineral> = emptyList(), // For bulk operations and export, not for paged list
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_NEWEST,
    val filterCriteria: FilterCriteria = FilterCriteria.EMPTY,
    val isFilterActive: Boolean = false,

    // Selection State
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),

    // Dialog States (Migrated from local remember states)
    val activeDialog: DialogType = DialogType.None,
    val selectedCsvUri: Uri? = null,
    val csvExportWarningShown: Boolean = false,

    // Operation States
    val exportState: ExportState = ExportState.Idle,
    val importState: ImportState = ImportState.Idle,
    val labelGenerationState: LabelGenerationState = LabelGenerationState.Idle,
    val catalogGenerationState: CatalogGenerationState = CatalogGenerationState.Idle,
    val bulkOperationProgress: BulkOperationProgress = BulkOperationProgress.Idle
) {
    /**
     * Derived property: number of selected minerals.
     * Replaces separate selectionCount StateFlow.
     */
    val selectionCount: Int get() = selectedIds.size

    /**
     * Helper: check if any async operation is in progress.
     */
    val isOperationInProgress: Boolean
        get() = exportState is ExportState.Exporting ||
                importState is ImportState.Importing ||
                labelGenerationState is LabelGenerationState.Generating ||
                catalogGenerationState is CatalogGenerationState.Generating ||
                bulkOperationProgress is BulkOperationProgress.InProgress
}

/**
 * Sealed hierarchy for dialog state management.
 *
 * Replaces 7 local `var showXxxDialog by remember { mutableStateOf(false) }`.
 * Advantages:
 * - Type-safe dialog state
 * - State survives recomposition
 * - Clear dialog lifecycle (None → Active → None)
 * - Single field in UiState instead of multiple booleans
 */
sealed class DialogType {
    /**
     * No dialog is shown.
     */
    data object None : DialogType()

    /**
     * Filter bottom sheet is shown.
     */
    data object Filter : DialogType()

    /**
     * Sort options bottom sheet is shown.
     */
    data object Sort : DialogType()

    /**
     * Bulk actions bottom sheet is shown.
     */
    data object BulkActions : DialogType()

    /**
     * CSV export warning dialog (first-time user notice).
     */
    data object CsvExportWarning : DialogType()

    /**
     * CSV export file picker dialog.
     */
    data object ExportCsv : DialogType()

    /**
     * CSV import dialog with column mapping.
     * @param uri The selected CSV file URI (null if not yet selected)
     */
    data class ImportCsv(val uri: Uri? = null) : DialogType()
}
