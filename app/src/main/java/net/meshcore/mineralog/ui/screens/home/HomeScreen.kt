package net.meshcore.mineralog.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.R
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.data.repository.CsvImportMode
import net.meshcore.mineralog.domain.model.FilterPreset
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.ui.screens.home.components.*

/**
 * HomeScreen - Main screen displaying the mineral collection.
 *
 * Phase 1 State Centralization (Sprint 2):
 * - Uses single HomeUiState instead of 14 individual StateFlows
 * - Dialog state managed via DialogType sealed class
 * - Reduced recompositions and improved testability
 *
 * Previously: 14 collectAsState() calls, 7 remember { mutableStateOf } for dialogs
 * Now: 1 collectAsState() for uiState, 2 for paging/presets
 *
 * Decomposed from 919 lines into specialized components:
 * - HomeScreenTopBar: Top app bar (normal + selection modes)
 * - SearchFilterBar: Search, sort, and filter controls
 * - BulkOperationProgressCard: Bulk operation progress indicator
 * - MineralPagingList: Paginated mineral list with empty states
 * - HomeScreenDialogs: All dialogs and bottom sheets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMineralClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    onCompareClick: (List<String>) -> Unit = {},
    onQrScanClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            context = LocalContext.current.applicationContext,
            mineralRepository = (LocalContext.current.applicationContext as MineraLogApplication).mineralRepository,
            filterPresetRepository = (LocalContext.current.applicationContext as MineraLogApplication).filterPresetRepository,
            backupRepository = (LocalContext.current.applicationContext as MineraLogApplication).backupRepository,
            settingsRepository = (LocalContext.current.applicationContext as MineraLogApplication).settingsRepository
        )
    )
) {
    // BUGFIX: Refresh list when returning to HomeScreen to show newly created/edited minerals
    LaunchedEffect(Unit) {
        viewModel.refreshMineralsList()
    }

    // Phase 1: Unified state collection (replaces 14 individual collectAsState calls)
    val uiState by viewModel.uiState.collectAsState()
    val filterPresets by viewModel.filterPresets.collectAsState()
    val mineralsPaged = viewModel.mineralsPaged.collectAsLazyPagingItems()

    // Derived dialog visibility from activeDialog (replaces 7 local remember states)
    val showFilterSheet = uiState.activeDialog is DialogType.Filter
    val showSortSheet = uiState.activeDialog is DialogType.Sort
    val showBulkActionsSheet = uiState.activeDialog is DialogType.BulkActions
    val showCsvExportWarningDialog = uiState.activeDialog is DialogType.CsvExportWarning
    val showExportCsvDialog = uiState.activeDialog is DialogType.ExportCsv
    val showImportCsvDialog = uiState.activeDialog is DialogType.ImportCsv

    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // File picker for CSV import
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.showImportCsvDialog(it) }
    }

    // Handle export state changes
    LaunchedEffect(uiState.exportState) {
        when (val state = uiState.exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Exported ${state.count} minerals successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetExportState()
                viewModel.exitSelectionMode()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Export failed: ${state.message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    // Handle import state changes
    LaunchedEffect(uiState.importState) {
        when (val state = uiState.importState) {
            is ImportState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Imported ${state.imported} minerals. Skipped: ${state.skipped}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetImportState()
            }
            is ImportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Import failed: ${state.message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetImportState()
            }
            else -> {}
        }
    }

    // Handle label generation state changes (v1.5.0)
    LaunchedEffect(uiState.labelGenerationState) {
        when (val state = uiState.labelGenerationState) {
            is LabelGenerationState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Generated ${state.count} QR labels successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetLabelGenerationState()
                viewModel.exitSelectionMode()
            }
            is LabelGenerationState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Label generation failed: ${state.message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetLabelGenerationState()
            }
            else -> {}
        }
    }

    // Quick Win #6: Handle bulk operation progress announcements
    LaunchedEffect(uiState.bulkOperationProgress) {
        when (val state = uiState.bulkOperationProgress) {
            is BulkOperationProgress.Complete -> {
                val operationName = state.operation.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
                }
                snackbarHostState.showSnackbar(
                    message = "$operationName completed: ${state.count} items",
                    duration = SnackbarDuration.Short
                )
            }
            is BulkOperationProgress.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Operation failed: ${state.message}",
                    duration = SnackbarDuration.Long
                )
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeScreenTopBar(
                selectionMode = uiState.selectionMode,
                selectionCount = uiState.selectionCount,
                totalCount = uiState.minerals.size,
                onExitSelectionMode = { viewModel.exitSelectionMode() },
                onSelectAll = { viewModel.selectAll() },
                onShowBulkActionsSheet = { viewModel.showBulkActionsDialog() },
                onLibraryClick = onLibraryClick,
                onQrScanClick = onQrScanClick,
                onEnterSelectionMode = { viewModel.enterSelectionMode() },
                onStatisticsClick = onStatisticsClick,
                onSettingsClick = onSettingsClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddClick()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add mineral")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search and filter bar
            SearchFilterBar(
                searchQuery = uiState.searchQuery,
                sortOption = uiState.sortOption,
                filterCriteria = uiState.filterCriteria,
                isFilterActive = uiState.isFilterActive,
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                onShowSortSheet = { viewModel.showSortDialog() },
                onShowFilterSheet = { viewModel.showFilterDialog() },
                onClearFilter = { viewModel.clearFilter() }
            )

            // Quick Win #6: Bulk operation progress indicator
            if (uiState.bulkOperationProgress is BulkOperationProgress.InProgress) {
                BulkOperationProgressCard(
                    progress = uiState.bulkOperationProgress as BulkOperationProgress.InProgress
                )
            }

            // Mineral list with pagination (v1.5.0)
            MineralPagingList(
                mineralsPaged = mineralsPaged,
                searchQuery = uiState.searchQuery,
                isFilterActive = uiState.isFilterActive,
                selectionMode = uiState.selectionMode,
                selectedIds = uiState.selectedIds,
                onMineralClick = onMineralClick,
                onToggleSelection = { viewModel.toggleSelection(it) },
                onClearSearch = { viewModel.onSearchQueryChange("") },
                onClearFilter = { viewModel.clearFilter() }
            )
        }
    }

    // All dialogs and bottom sheets
    HomeScreenDialogs(
        showFilterSheet = showFilterSheet,
        showSortSheet = showSortSheet,
        showBulkActionsSheet = showBulkActionsSheet,
        showCsvExportWarningDialog = showCsvExportWarningDialog,
        showExportCsvDialog = showExportCsvDialog,
        showImportCsvDialog = showImportCsvDialog,
        filterCriteria = uiState.filterCriteria,
        filterPresets = filterPresets,
        sortOption = uiState.sortOption,
        selectedCsvUri = uiState.selectedCsvUri,
        selectionCount = uiState.selectionCount,
        selectedMineralNames = viewModel.getSelectedMinerals().map { it.name },
        csvExportWarningShown = uiState.csvExportWarningShown,
        exportState = uiState.exportState,
        labelGenerationState = uiState.labelGenerationState,
        onFilterCriteriaChange = { viewModel.onFilterCriteriaChange(it) },
        onClearFilter = { viewModel.clearFilter() },
        onSavePreset = { viewModel.savePreset(it) },
        onLoadPreset = { viewModel.applyPreset(it) },
        onDeletePreset = { viewModel.deletePreset(it) },
        onSortSelected = { viewModel.onSortOptionChange(it) },
        onDeleteSelected = {
            val count = uiState.selectionCount
            viewModel.deleteSelected()
            // Quick Win #4: Indefinite Undo snackbar with haptic feedback
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Deleted $count mineral${if (count > 1) "s" else ""}",
                    actionLabel = "Undo",
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.undoDelete()
                    }
                    SnackbarResult.Dismissed -> {
                        // User dismissed, deletion is permanent
                    }
                }
            }
        },
        onExportCsv = { viewModel.exportSelectedToCsv(it) },
        onImportCsv = { uri, columnMapping, mode ->
            viewModel.importCsvFile(uri, columnMapping, mode)
        },
        onGenerateLabels = { viewModel.generateLabelsForSelected(it) },
        onCompareClick = if (uiState.selectionCount in 2..3) {
            {
                val selectedMinerals = viewModel.getSelectedMinerals()
                onCompareClick(selectedMinerals.map { it.id })
                viewModel.exitSelectionMode()
            }
        } else null,
        onMarkCsvExportWarningShown = { viewModel.markCsvExportWarningShown() },
        onDismissFilterSheet = { viewModel.dismissDialog() },
        onDismissSortSheet = { viewModel.dismissDialog() },
        onDismissBulkActionsSheet = { viewModel.dismissDialog() },
        onDismissCsvExportWarningDialog = { viewModel.dismissDialog() },
        onDismissExportCsvDialog = { viewModel.dismissDialog() },
        onDismissImportCsvDialog = { viewModel.dismissDialog() },
        onShowExportCsvDialog = {
            if (!uiState.csvExportWarningShown) {
                viewModel.showCsvExportWarning()
            } else {
                viewModel.showExportCsvDialog()
            }
        },
        onShowCsvExportWarningDialog = { viewModel.showCsvExportWarning() }
    )
}

/**
 * Mineral list item component.
 *
 * Displays a single mineral in the list with selection support.
 * v2.0: Shows aggregate badge for mineral aggregates.
 */
@Composable
fun MineralListItem(
    mineral: Mineral,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = if (selectionMode) {
                    val selectedState = if (isSelected) "Selected" else "Not selected"
                    val action = if (isSelected) "deselect" else "select"
                    "${mineral.name}. $selectedState. Tap to $action."
                } else {
                    "${mineral.name}. Tap to view details."
                }
            },
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Checkbox in selection mode
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null, // Handled by card click
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = mineral.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    // v2.0: Aggregate badge
                    if (mineral.mineralType == net.meshcore.mineralog.domain.model.MineralType.AGGREGATE) {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = "Agrégat",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }
                mineral.group?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                mineral.formula?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!selectionMode) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}