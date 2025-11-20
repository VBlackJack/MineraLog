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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.MineralType
import net.meshcore.mineralog.ui.screens.home.components.*
import net.meshcore.mineralog.ui.screens.home.components.dialogs.*

/**
 * HomeScreen - Main screen displaying the mineral collection.
 *
 * Refactored to follow Single Responsibility Principle (SRP) & Unidirectional Data Flow (UDF).
 * Now consumes a single [HomeUiState] instead of multiple individual flows.
 * * V3 Architecture: Uses specialized dialog components from `components/dialogs/`.
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
    // Refresh list when returning to HomeScreen
    LaunchedEffect(Unit) {
        viewModel.refreshMineralsList()
    }

    // Unified State Collection
    val uiState by viewModel.uiState.collectAsState()
    val filterPresets by viewModel.filterPresets.collectAsState()
    val mineralsPaged = viewModel.mineralsPaged.collectAsLazyPagingItems()

    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // File picker for CSV import
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.showImportCsvDialog(it) }
    }

    // Handle Side Effects (Snackbars)
    LaunchedEffect(uiState.exportState) {
        when (val state = uiState.exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar("Exported ${state.count} minerals successfully")
                viewModel.resetExportState()
                viewModel.exitSelectionMode()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar("Export failed: ${state.message}")
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    LaunchedEffect(uiState.importState) {
        when (val state = uiState.importState) {
            is ImportState.Success -> {
                snackbarHostState.showSnackbar("Imported ${state.imported} minerals. Skipped: ${state.skipped}")
                viewModel.resetImportState()
            }
            is ImportState.Error -> {
                snackbarHostState.showSnackbar("Import failed: ${state.message}")
                viewModel.resetImportState()
            }
            else -> {}
        }
    }

    LaunchedEffect(uiState.labelGenerationState) {
        when (val state = uiState.labelGenerationState) {
            is LabelGenerationState.Success -> {
                snackbarHostState.showSnackbar("Generated ${state.count} QR labels successfully")
                viewModel.resetLabelGenerationState()
                viewModel.exitSelectionMode()
            }
            is LabelGenerationState.Error -> {
                snackbarHostState.showSnackbar("Label generation failed: ${state.message}")
                viewModel.resetLabelGenerationState()
            }
            else -> {}
        }
    }

    LaunchedEffect(uiState.bulkOperationProgress) {
        when (val state = uiState.bulkOperationProgress) {
            is BulkOperationProgress.Complete -> {
                val opName = state.operation.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                snackbarHostState.showSnackbar("$opName completed: ${state.count} items")
            }
            is BulkOperationProgress.Error -> {
                snackbarHostState.showSnackbar("Operation failed: ${state.message}")
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

            if (uiState.bulkOperationProgress is BulkOperationProgress.InProgress) {
                BulkOperationProgressCard(
                    progress = uiState.bulkOperationProgress as BulkOperationProgress.InProgress
                )
            }

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

    // --- Specialized Dialog Components ---

    HomeFilterDialogs(
        showFilterSheet = uiState.activeDialog is DialogType.Filter,
        showSortSheet = uiState.activeDialog is DialogType.Sort,
        filterCriteria = uiState.filterCriteria,
        filterPresets = filterPresets,
        sortOption = uiState.sortOption,
        onFilterCriteriaChange = { viewModel.onFilterCriteriaChange(it) },
        onClearFilter = { viewModel.clearFilter() },
        onSavePreset = { viewModel.savePreset(it) },
        onLoadPreset = { viewModel.applyPreset(it) },
        onDeletePreset = { viewModel.deletePreset(it) },
        onSortSelected = { viewModel.onSortOptionChange(it) },
        onDismissFilterSheet = { viewModel.dismissDialog() },
        onDismissSortSheet = { viewModel.dismissDialog() }
    )

    HomeCsvDialogs(
        showCsvExportWarningDialog = uiState.activeDialog is DialogType.CsvExportWarning,
        showExportCsvDialog = uiState.activeDialog is DialogType.ExportCsv,
        showImportCsvDialog = uiState.activeDialog is DialogType.ImportCsv,
        selectedCsvUri = uiState.selectedCsvUri,
        selectionCount = uiState.selectionCount,
        csvExportWarningShown = uiState.csvExportWarningShown,
        onMarkCsvExportWarningShown = { viewModel.markCsvExportWarningShown() },
        onExportCsv = { viewModel.exportSelectedToCsv(it) },
        onImportCsv = { uri, colMap, mode -> viewModel.importCsvFile(uri, colMap, mode) },
        onDismissCsvExportWarningDialog = { viewModel.dismissDialog() },
        onDismissExportCsvDialog = { viewModel.dismissDialog() },
        onDismissImportCsvDialog = { viewModel.dismissDialog() },
        onShowExportCsvDialog = { viewModel.showExportCsvDialog() }
    )

    HomeBulkActionsDialog(
        showBulkActionsSheet = uiState.activeDialog is DialogType.BulkActions,
        selectionCount = uiState.selectionCount,
        selectedMineralNames = viewModel.getSelectedMinerals().map { it.name },
        csvExportWarningShown = uiState.csvExportWarningShown,
        onDeleteSelected = {
            val count = uiState.selectionCount
            viewModel.deleteSelected()
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Deleted $count mineral${if (count > 1) "s" else ""}",
                    actionLabel = "Undo",
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.undoDelete()
                }
            }
        },
        onGenerateLabels = { viewModel.generateLabelsForSelected(it) },
        onCompareClick = if (uiState.selectionCount in 2..3) {
            {
                val selectedMinerals = viewModel.getSelectedMinerals()
                onCompareClick(selectedMinerals.map { it.id })
                viewModel.exitSelectionMode()
            }
        } else null,
        onShowExportCsvDialog = { viewModel.showExportCsvDialog() },
        onShowCsvExportWarningDialog = { viewModel.showCsvExportWarning() },
        onDismissBulkActionsSheet = { viewModel.dismissDialog() }
    )

    HomeLoadingIndicators(
        exportState = uiState.exportState,
        labelGenerationState = uiState.labelGenerationState
    )
}

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
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(text = mineral.name, style = MaterialTheme.typography.titleMedium)
                    if (mineral.mineralType == MineralType.AGGREGATE) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Agrégat", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }
                mineral.group?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                mineral.formula?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (!selectionMode) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
