package net.meshcore.mineralog.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.R
import net.meshcore.mineralog.domain.model.Mineral

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMineralClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    onCompareClick: (List<String>) -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            mineralRepository = (LocalContext.current.applicationContext as MineraLogApplication).mineralRepository,
            filterPresetRepository = (LocalContext.current.applicationContext as MineraLogApplication).filterPresetRepository,
            backupRepository = (LocalContext.current.applicationContext as MineraLogApplication).backupRepository,
            settingsRepository = (LocalContext.current.applicationContext as MineraLogApplication).settingsRepository
        )
    )
) {
    val minerals by viewModel.minerals.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterCriteria by viewModel.filterCriteria.collectAsState()
    val isFilterActive by viewModel.isFilterActive.collectAsState()
    val filterPresets by viewModel.filterPresets.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val selectionCount by viewModel.selectionCount.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val csvExportWarningShown by viewModel.csvExportWarningShown.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showBulkActionsSheet by remember { mutableStateOf(false) }
    var showCsvExportWarningDialog by remember { mutableStateOf(false) }
    var showExportCsvDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }
    var selectedCsvUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // File picker for CSV export
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Start export
            viewModel.exportSelectedToCsv(selectedUri)
        }
    }

    // File picker for CSV import
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            selectedCsvUri = selectedUri
            showImportCsvDialog = true
        }
    }

    // Handle export state changes
    LaunchedEffect(exportState) {
        when (exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Exported ${(exportState as ExportState.Success).count} minerals successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetExportState()
                viewModel.exitSelectionMode()
            }
            is ExportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Export failed: ${(exportState as ExportState.Error).message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetExportState()
            }
            else -> {}
        }
    }

    // Handle import state changes
    LaunchedEffect(importState) {
        when (importState) {
            is ImportState.Success -> {
                val success = importState as ImportState.Success
                snackbarHostState.showSnackbar(
                    message = "Imported ${success.imported} minerals. Skipped: ${success.skipped}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetImportState()
            }
            is ImportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Import failed: ${(importState as ImportState.Error).message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetImportState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (selectionMode) {
                // Selection mode top bar
                TopAppBar(
                    title = { Text("$selectionCount selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        if (selectionCount < minerals.size) {
                            IconButton(onClick = { viewModel.selectAll() }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                            }
                        }
                        if (selectionCount > 0) {
                            IconButton(onClick = { showBulkActionsSheet = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                            }
                        }
                    }
                )
            } else {
                // Normal top bar
                TopAppBar(
                    title = { Text("MineraLog") },
                    actions = {
                        // Import CSV button
                        IconButton(onClick = { csvImportLauncher.launch("text/*") }) {
                            Icon(Icons.Default.Upload, contentDescription = stringResource(R.string.action_import_csv))
                        }
                        // Bulk edit button
                        IconButton(onClick = { viewModel.enterSelectionMode() }) {
                            Icon(Icons.Default.Checklist, contentDescription = "Bulk edit")
                        }
                        IconButton(onClick = onStatisticsClick) {
                            Icon(Icons.Default.BarChart, contentDescription = "Statistics")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add mineral")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search minerals...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    BadgedBox(
                        badge = {
                            if (isFilterActive) {
                                Badge { Text("${filterCriteria.activeCount()}") }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (isFilterActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                singleLine = true
            )

            // Active filter chip
            if (isFilterActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { showFilterSheet = true },
                        label = { Text(filterCriteria.toSummary()) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    IconButton(
                        onClick = { viewModel.clearFilter() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear filter",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Mineral list
            if (minerals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No minerals yet. Add your first one!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(minerals, key = { it.id }) { mineral ->
                        MineralListItem(
                            mineral = mineral,
                            selectionMode = selectionMode,
                            isSelected = mineral.id in selectedIds,
                            onClick = {
                                if (selectionMode) {
                                    viewModel.toggleSelection(mineral.id)
                                } else {
                                    onMineralClick(mineral.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            criteria = filterCriteria,
            presets = filterPresets,
            onCriteriaChange = { viewModel.onFilterCriteriaChange(it) },
            onApply = { },
            onClear = { viewModel.clearFilter() },
            onSavePreset = { viewModel.savePreset(it) },
            onLoadPreset = { viewModel.applyPreset(it) },
            onDeletePreset = { viewModel.deletePreset(it) },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Bulk actions bottom sheet
    if (showBulkActionsSheet) {
        BulkActionsBottomSheet(
            selectedCount = selectionCount,
            onDelete = {
                viewModel.deleteSelected()
            },
            onExportCsv = {
                showBulkActionsSheet = false
                // Check if warning has been shown before
                if (csvExportWarningShown) {
                    showExportCsvDialog = true
                } else {
                    showCsvExportWarningDialog = true
                }
            },
            onCompare = if (selectionCount in 2..3) {
                {
                    val selectedMinerals = viewModel.getSelectedMinerals()
                    onCompareClick(selectedMinerals.map { it.id })
                    viewModel.exitSelectionMode()
                }
            } else null,
            onDismiss = { showBulkActionsSheet = false }
        )
    }

    // CSV export warning dialog (shown only once)
    if (showCsvExportWarningDialog) {
        CsvExportWarningDialog(
            onDismiss = {
                showCsvExportWarningDialog = false
            },
            onProceed = { dontShowAgain ->
                if (dontShowAgain) {
                    viewModel.markCsvExportWarningShown()
                }
                showCsvExportWarningDialog = false
                showExportCsvDialog = true
            }
        )
    }

    // CSV export dialog
    if (showExportCsvDialog) {
        ExportCsvDialog(
            selectedCount = selectionCount,
            onDismiss = { showExportCsvDialog = false },
            onExport = { selectedColumns ->
                showExportCsvDialog = false
                // Generate filename with timestamp
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                    .format(java.util.Date())
                csvExportLauncher.launch("mineralog_export_$timestamp.csv")
            }
        )
    }

    // CSV import dialog
    if (showImportCsvDialog && selectedCsvUri != null) {
        ImportCsvDialog(
            csvUri = selectedCsvUri!!,
            onDismiss = {
                showImportCsvDialog = false
                selectedCsvUri = null
            },
            onImport = { uri, mode ->
                viewModel.importCsvFile(uri, mode)
            }
        )
    }

    // Loading indicator for export
    if (exportState is ExportState.Exporting) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
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
            .clickable(onClick = onClick),
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
            verticalAlignment = Alignment.CenterVertically
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
                Text(
                    text = mineral.name,
                    style = MaterialTheme.typography.titleMedium
                )
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
