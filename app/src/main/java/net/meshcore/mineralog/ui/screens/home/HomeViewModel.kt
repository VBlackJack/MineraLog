package net.meshcore.mineralog.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.data.repository.BackupRepository
import net.meshcore.mineralog.data.repository.CsvImportMode
import net.meshcore.mineralog.data.repository.FilterPresetRepository
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.data.repository.SettingsRepository
import net.meshcore.mineralog.data.util.QrLabelPdfGenerator
import net.meshcore.mineralog.domain.model.FilterPreset
import net.meshcore.mineralog.domain.model.Mineral

class HomeViewModel(
    private val context: Context,
    private val mineralRepository: MineralRepository,
    private val filterPresetRepository: FilterPresetRepository,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Export state
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // Import state
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    // Label generation state (v1.5.0)
    private val _labelGenerationState = MutableStateFlow<LabelGenerationState>(LabelGenerationState.Idle)
    val labelGenerationState: StateFlow<LabelGenerationState> = _labelGenerationState.asStateFlow()

    // Bulk operation progress state (v1.7.0 - Quick Win #6)
    private val _bulkOperationProgress = MutableStateFlow<BulkOperationProgress>(BulkOperationProgress.Idle)
    val bulkOperationProgress: StateFlow<BulkOperationProgress> = _bulkOperationProgress.asStateFlow()

    // CSV export warning state
    val csvExportWarningShown: StateFlow<Boolean> = settingsRepository.getCsvExportWarningShown()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCriteria = MutableStateFlow(FilterCriteria.EMPTY)
    val filterCriteria: StateFlow<FilterCriteria> = _filterCriteria.asStateFlow()

    private val _isFilterActive = MutableStateFlow(false)
    val isFilterActive: StateFlow<Boolean> = _isFilterActive.asStateFlow()

    // Bulk selection state (v1.3.0)
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    // Undo delete state
    private var deletedMinerals: List<Mineral> = emptyList()

    val selectionCount: StateFlow<Int> = _selectedIds.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Filter presets from database
    val filterPresets: StateFlow<List<FilterPreset>> = filterPresetRepository.getAllFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Refresh trigger to invalidate PagingData cache after mineral creation/deletion
    private val _refreshTrigger = MutableStateFlow(0)

    // Paged minerals for efficient large dataset handling (v1.5.0)
    val mineralsPaged: Flow<PagingData<Mineral>> = combine(
        _searchQuery.debounce(300),
        _filterCriteria,
        _isFilterActive,
        _refreshTrigger  // BUGFIX: Trigger re-collection after insert/delete
    ) { query, criteria, filterActive, _ ->
        Triple(query, criteria, filterActive)
    }.flatMapLatest { (query, criteria, filterActive) ->
        when {
            // Search takes precedence
            query.isNotBlank() -> mineralRepository.searchPaged(query)
            // Apply filters if active
            filterActive && !criteria.isEmpty() -> mineralRepository.filterAdvancedPaged(criteria)
            // Default: show all
            else -> mineralRepository.getAllPaged()
        }
    }.cachedIn(viewModelScope)

    /**
     * Refresh the minerals list by invalidating the PagingData cache.
     * Call this after creating, updating, or deleting minerals.
     */
    fun refreshMineralsList() {
        _refreshTrigger.value += 1
    }

    // Legacy non-paged flow for bulk operations that need full list access
    val minerals: StateFlow<List<Mineral>> = combine(
        _searchQuery.debounce(300),
        _filterCriteria,
        _isFilterActive
    ) { query, criteria, filterActive ->
        Triple(query, criteria, filterActive)
    }.flatMapLatest { (query, criteria, filterActive) ->
        when {
            // Search takes precedence
            query.isNotBlank() -> mineralRepository.searchFlow(query)
            // Apply filters if active
            filterActive && !criteria.isEmpty() -> mineralRepository.filterAdvancedFlow(criteria)
            // Default: show all
            else -> mineralRepository.getAllFlow()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterCriteriaChange(criteria: FilterCriteria) {
        _filterCriteria.value = criteria
        _isFilterActive.value = !criteria.isEmpty()
    }

    fun clearFilter() {
        _filterCriteria.value = FilterCriteria.EMPTY
        _isFilterActive.value = false
    }

    fun applyPreset(preset: FilterPreset) {
        onFilterCriteriaChange(preset.criteria)
    }

    fun savePreset(preset: FilterPreset) {
        viewModelScope.launch {
            filterPresetRepository.save(preset)
        }
    }

    fun deletePreset(presetId: String) {
        viewModelScope.launch {
            filterPresetRepository.delete(presetId)
        }
    }

    // Bulk selection methods (v1.3.0)
    fun enterSelectionMode() {
        _selectionMode.value = true
        _selectedIds.value = emptySet()
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(mineralId: String) {
        _selectedIds.value = if (mineralId in _selectedIds.value) {
            _selectedIds.value - mineralId
        } else {
            _selectedIds.value + mineralId
        }
    }

    fun selectAll() {
        _selectedIds.value = minerals.value.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            // Store minerals for undo functionality
            deletedMinerals = getSelectedMinerals()

            val idsToDelete = _selectedIds.value.toList()
            val total = idsToDelete.size

            // Quick Win #6: Track progress for bulk delete operations
            if (total > 10) {
                // Show progress for larger operations
                try {
                    _bulkOperationProgress.value = BulkOperationProgress.InProgress(0, total, "delete")

                    // Delete in batches to allow progress updates
                    val batchSize = 10
                    idsToDelete.chunked(batchSize).forEachIndexed { index, batch ->
                        mineralRepository.deleteByIds(batch)
                        val current = minOf((index + 1) * batchSize, total)
                        _bulkOperationProgress.value = BulkOperationProgress.InProgress(current, total, "delete")

                        // Small delay to prevent UI blocking
                        if (current < total) {
                            kotlinx.coroutines.delay(50)
                        }
                    }

                    _bulkOperationProgress.value = BulkOperationProgress.Complete(total, "delete")
                    kotlinx.coroutines.delay(2000) // Show completion for 2s
                    _bulkOperationProgress.value = BulkOperationProgress.Idle
                } catch (e: Exception) {
                    _bulkOperationProgress.value = BulkOperationProgress.Error(e.message ?: "Delete failed")
                    kotlinx.coroutines.delay(3000)
                    _bulkOperationProgress.value = BulkOperationProgress.Idle
                }
            } else {
                // Small operations - no progress tracking needed
                mineralRepository.deleteByIds(idsToDelete)
            }

            exitSelectionMode()
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            // Restore previously deleted minerals
            deletedMinerals.forEach { mineral ->
                mineralRepository.insert(mineral)
            }
            deletedMinerals = emptyList()
        }
    }

    fun getSelectedMinerals(): List<Mineral> {
        return minerals.value.filter { it.id in _selectedIds.value }
    }

    // Export functionality (v1.4.0)
    fun exportSelectedToCsv(uri: Uri) {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            try {
                val mineralsToExport = getSelectedMinerals()
                if (mineralsToExport.isEmpty()) {
                    _exportState.value = ExportState.Error("No minerals selected for export")
                    return@launch
                }

                val result = backupRepository.exportCsv(uri, mineralsToExport)

                if (result.isSuccess) {
                    _exportState.value = ExportState.Success(mineralsToExport.size)
                } else {
                    _exportState.value = ExportState.Error(
                        result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    fun markCsvExportWarningShown() {
        viewModelScope.launch {
            settingsRepository.setCsvExportWarningShown(true)
        }
    }

    // Import functionality
    fun importCsvFile(uri: Uri, columnMapping: Map<String, String>, mode: CsvImportMode) {
        viewModelScope.launch {
            _importState.value = ImportState.Importing
            try {
                val result = backupRepository.importCsv(uri, columnMapping, mode)

                if (result.isSuccess) {
                    val importResult = result.getOrThrow()
                    _importState.value = ImportState.Success(
                        imported = importResult.imported,
                        skipped = importResult.skipped,
                        errors = importResult.errors
                    )
                } else {
                    _importState.value = ImportState.Error(
                        result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    // QR Label generation (v1.5.0)
    fun generateLabelsForSelected(outputUri: Uri) {
        viewModelScope.launch {
            _labelGenerationState.value = LabelGenerationState.Generating
            try {
                // Get selected minerals
                val selectedMinerals = getSelectedMinerals()

                if (selectedMinerals.isEmpty()) {
                    _labelGenerationState.value = LabelGenerationState.Error("No minerals selected")
                    return@launch
                }

                // Generate PDF with QR labels
                val generator = QrLabelPdfGenerator(context)
                val result = generator.generate(selectedMinerals, outputUri)

                if (result.isSuccess) {
                    _labelGenerationState.value = LabelGenerationState.Success(selectedMinerals.size)
                } else {
                    _labelGenerationState.value = LabelGenerationState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to generate labels"
                    )
                }
            } catch (e: Exception) {
                _labelGenerationState.value = LabelGenerationState.Error(
                    e.message ?: "Unknown error generating labels"
                )
            }
        }
    }

    fun resetLabelGenerationState() {
        _labelGenerationState.value = LabelGenerationState.Idle
    }

    fun resetBulkOperationProgress() {
        _bulkOperationProgress.value = BulkOperationProgress.Idle
    }
}

sealed class ExportState {
    data object Idle : ExportState()
    data object Exporting : ExportState()
    data class Success(val count: Int) : ExportState()
    data class Error(val message: String) : ExportState()
}

sealed class ImportState {
    data object Idle : ImportState()
    data object Importing : ImportState()
    data class Success(val imported: Int, val skipped: Int, val errors: List<String>) : ImportState()
    data class Error(val message: String) : ImportState()
}

sealed class LabelGenerationState {
    data object Idle : LabelGenerationState()
    data object Generating : LabelGenerationState()
    data class Success(val count: Int) : LabelGenerationState()
    data class Error(val message: String) : LabelGenerationState()
}

// Quick Win #6: Bulk operations progress tracking (v1.7.0)
sealed class BulkOperationProgress {
    data object Idle : BulkOperationProgress()
    data class InProgress(val current: Int, val total: Int, val operation: String) : BulkOperationProgress()
    data class Complete(val count: Int, val operation: String) : BulkOperationProgress()
    data class Error(val message: String) : BulkOperationProgress()
}

class HomeViewModelFactory(
    private val context: Context,
    private val mineralRepository: MineralRepository,
    private val filterPresetRepository: FilterPresetRepository,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(context, mineralRepository, filterPresetRepository, backupRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
