package net.meshcore.mineralog.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch
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
            context = LocalContext.current.applicationContext,
            mineralRepository = (LocalContext.current.applicationContext as MineraLogApplication).mineralRepository,
            filterPresetRepository = (LocalContext.current.applicationContext as MineraLogApplication).filterPresetRepository,
            backupRepository = (LocalContext.current.applicationContext as MineraLogApplication).backupRepository,
            settingsRepository = (LocalContext.current.applicationContext as MineraLogApplication).settingsRepository
        )
    )
) {
    // Use paged minerals for efficient loading of large datasets (v1.5.0)
    val mineralsPaged = viewModel.mineralsPaged.collectAsLazyPagingItems()
    // Keep non-paged minerals for bulk operations
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
    val labelGenerationState by viewModel.labelGenerationState.collectAsState()
    val bulkOperationProgress by viewModel.bulkOperationProgress.collectAsState() // Quick Win #6
    val csvExportWarningShown by viewModel.csvExportWarningShown.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showBulkActionsSheet by remember { mutableStateOf(false) }
    var showCsvExportWarningDialog by remember { mutableStateOf(false) }
    var showExportCsvDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }
    var selectedCsvUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

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

    // File picker for PDF label generation (v1.5.0)
    val pdfLabelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            viewModel.generateLabelsForSelected(selectedUri)
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

    // Handle label generation state changes (v1.5.0)
    LaunchedEffect(labelGenerationState) {
        when (labelGenerationState) {
            is LabelGenerationState.Success -> {
                val count = (labelGenerationState as LabelGenerationState.Success).count
                snackbarHostState.showSnackbar(
                    message = "Generated $count QR labels successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetLabelGenerationState()
                viewModel.exitSelectionMode()
            }
            is LabelGenerationState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Label generation failed: ${(labelGenerationState as LabelGenerationState.Error).message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetLabelGenerationState()
            }
            else -> {}
        }
    }

    // Quick Win #6: Handle bulk operation progress announcements
    LaunchedEffect(bulkOperationProgress) {
        when (bulkOperationProgress) {
            is BulkOperationProgress.Complete -> {
                val state = bulkOperationProgress as BulkOperationProgress.Complete
                snackbarHostState.showSnackbar(
                    message = "${state.operation.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }} completed: ${state.count} items",
                    duration = SnackbarDuration.Short
                )
            }
            is BulkOperationProgress.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Operation failed: ${(bulkOperationProgress as BulkOperationProgress.Error).message}",
                    duration = SnackbarDuration.Long
                )
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search minerals...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    Row {
                        // Clear search button
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                        // Filter button with badge
                        BadgedBox(
                            badge = {
                                if (isFilterActive) {
                                    Badge { Text("${filterCriteria.activeCount()}") }
                                }
                            },
                            modifier = Modifier.semantics {
                                contentDescription = if (isFilterActive) {
                                    "${filterCriteria.activeCount()} active filters"
                                } else {
                                    "No active filters"
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
                                contentDescription = "Filter icon",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    IconButton(
                        onClick = { viewModel.clearFilter() }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear filter",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Quick Win #6: Bulk operation progress indicator
            if (bulkOperationProgress is BulkOperationProgress.InProgress) {
                val progress = bulkOperationProgress as BulkOperationProgress.InProgress
                val percentage = (progress.current.toFloat() / progress.total.toFloat())

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "${progress.operation} in progress: ${progress.current} of ${progress.total} items"
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                                text = "${progress.operation.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }} in progress...",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "${progress.current}/${progress.total}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        LinearProgressIndicator(
                            progress = percentage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // Mineral list with pagination (v1.5.0)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show loading indicator at the top when refreshing
                when (mineralsPaged.loadState.refresh) {
                    is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .semantics {
                                        liveRegion = LiveRegionMode.Polite
                                        contentDescription = "Loading minerals"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is LoadState.Error -> {
                        val error = (mineralsPaged.loadState.refresh as LoadState.Error).error
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Error loading minerals: ${error.localizedMessage}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    is LoadState.NotLoading -> {
                        if (mineralsPaged.itemCount == 0) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Quick Win #2: Different states for empty collection vs. no search results
                                    if (searchQuery.isNotEmpty() || isFilterActive) {
                                        // No search results state
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .padding(32.dp)
                                                .semantics {
                                                    contentDescription = "No search results found for '$searchQuery'. Try different keywords or clear filters to see all minerals."
                                                }
                                        ) {
                                            Icon(
                                                Icons.Default.SearchOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "No Results Found",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (searchQuery.isNotEmpty()) {
                                                Text(
                                                    text = "No minerals match \"$searchQuery\"",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                            if (isFilterActive) {
                                                Text(
                                                    text = "with the current filters",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (searchQuery.isNotEmpty()) {
                                                    OutlinedButton(
                                                        onClick = { viewModel.onSearchQueryChange("") }
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Clear,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Clear Search")
                                                    }
                                                }
                                                if (isFilterActive) {
                                                    OutlinedButton(
                                                        onClick = { viewModel.clearFilter() }
                                                    ) {
                                                        Icon(
                                                            Icons.Default.FilterListOff,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Clear Filters")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Empty collection state
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .padding(32.dp)
                                                .semantics {
                                                    contentDescription = "Empty collection state. Your collection is empty. Start building your mineral collection by adding your first specimen. Tap the add button below to get started."
                                                }
                                        ) {
                                            Icon(
                                                Icons.Default.Inventory,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = "Your Collection is Empty",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Start building your mineral collection by adding your first specimen",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Text(
                                                text = "Tap the + button below to get started",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Paged items
                items(
                    count = mineralsPaged.itemCount,
                    key = mineralsPaged.itemKey { it.id }
                ) { index ->
                    val mineral = mineralsPaged[index]
                    mineral?.let {
                        MineralListItem(
                            mineral = it,
                            selectionMode = selectionMode,
                            isSelected = it.id in selectedIds,
                            onClick = {
                                if (selectionMode) {
                                    viewModel.toggleSelection(it.id)
                                } else {
                                    onMineralClick(it.id)
                                }
                            }
                        )
                    }
                }

                // Show loading indicator at the bottom when loading more
                when (mineralsPaged.loadState.append) {
                    is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .semantics {
                                        liveRegion = LiveRegionMode.Polite
                                        contentDescription = "Loading more minerals"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is LoadState.Error -> {
                        val error = (mineralsPaged.loadState.append as LoadState.Error).error
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Error loading more: ${error.localizedMessage}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    else -> {}
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
            selectedMineralNames = viewModel.getSelectedMinerals().map { it.name },
            onDelete = {
                val count = selectionCount
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
            onExportCsv = {
                showBulkActionsSheet = false
                // Check if warning has been shown before
                if (csvExportWarningShown) {
                    showExportCsvDialog = true
                } else {
                    showCsvExportWarningDialog = true
                }
            },
            onGenerateLabels = {
                showBulkActionsSheet = false
                // Generate filename with timestamp
                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                    .format(java.util.Date())
                pdfLabelLauncher.launch("mineralog_labels_$timestamp.pdf")
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
            onImport = { uri, columnMapping, mode ->
                viewModel.importCsvFile(uri, columnMapping, mode)
            }
        )
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
                    text = "Generating labels...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = if (selectionMode) {
                    "${mineral.name}. ${if (isSelected) "Selected" else "Not selected"}. Tap to ${if (isSelected) "deselect" else "select"}."
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
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
