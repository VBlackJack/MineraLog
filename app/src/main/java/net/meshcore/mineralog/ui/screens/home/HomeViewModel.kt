package net.meshcore.mineralog.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.data.repository.BackupRepository
import net.meshcore.mineralog.data.repository.CsvImportMode
import net.meshcore.mineralog.data.repository.FilterPresetRepository
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.data.repository.SettingsRepository
import net.meshcore.mineralog.data.util.PdfCatalogGenerator
import net.meshcore.mineralog.data.util.QrLabelPdfGenerator
import net.meshcore.mineralog.domain.model.FilterPreset
import net.meshcore.mineralog.domain.model.Mineral

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val context: Context,
    private val mineralRepository: MineralRepository,
    private val filterPresetRepository: FilterPresetRepository,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val filterPresets: StateFlow<List<FilterPreset>> = filterPresetRepository.getAllFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var deletedMinerals: List<Mineral> = emptyList()
    private val deletedMineralsMutex = Mutex()

    private val _refreshTrigger = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            settingsRepository.getCsvExportWarningShown().collect { shown ->
                _uiState.update { it.copy(csvExportWarningShown = shown) }
            }
        }

        viewModelScope.launch {
            combine(
                _uiState.map { it.searchQuery }.distinctUntilChanged().debounce(300),
                _uiState.map { it.sortOption }.distinctUntilChanged(),
                _uiState.map { it.filterCriteria }.distinctUntilChanged(),
                _uiState.map { it.isFilterActive }.distinctUntilChanged()
            ) { query, sort, criteria, filterActive ->
                Triple(query, Pair(sort, criteria), filterActive)
            }.flatMapLatest { (query, sortAndCriteria, filterActive) ->
                val (sort, criteria) = sortAndCriteria
                when {
                    query.isNotBlank() -> mineralRepository.searchFlow(query, sort)
                    filterActive && !criteria.isEmpty() -> mineralRepository.filterAdvancedFlow(criteria, sort)
                    else -> mineralRepository.getAllFlow(sort)
                }
            }.collect { minerals ->
                _uiState.update { it.copy(minerals = minerals) }
            }
        }
    }

    val mineralsPaged: Flow<PagingData<Mineral>> = combine(
        _uiState.map { it.searchQuery }.distinctUntilChanged().debounce(300),
        _uiState.map { it.sortOption }.distinctUntilChanged(),
        _uiState.map { it.filterCriteria }.distinctUntilChanged(),
        _uiState.map { it.isFilterActive }.distinctUntilChanged(),
        _refreshTrigger
    ) { query, sort, criteria, filterActive, _ ->
        Triple(query, Pair(sort, criteria), filterActive)
    }.flatMapLatest { (query, sortAndCriteria, filterActive) ->
        val (sort, criteria) = sortAndCriteria
        when {
            query.isNotBlank() -> mineralRepository.searchPaged(query, sort)
            filterActive && !criteria.isEmpty() -> mineralRepository.filterAdvancedPaged(criteria, sort)
            else -> mineralRepository.getAllPaged(sort)
        }
    }.cachedIn(viewModelScope)

    fun refreshMineralsList() {
        _refreshTrigger.value += 1
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSortOptionChange(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun onFilterCriteriaChange(criteria: FilterCriteria) {
        _uiState.update {
            it.copy(
                filterCriteria = criteria,
                isFilterActive = !criteria.isEmpty()
            )
        }
    }

    fun clearFilter() {
        _uiState.update {
            it.copy(
                filterCriteria = FilterCriteria.EMPTY,
                isFilterActive = false
            )
        }
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

    fun enterSelectionMode() {
        _uiState.update { it.copy(selectionMode = true, selectedIds = emptySet()) }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun toggleSelection(mineralId: String) {
        _uiState.update { state ->
            val currentIds = state.selectedIds
            val newIds = if (mineralId in currentIds) {
                currentIds - mineralId
            } else {
                currentIds + mineralId
            }
            state.copy(selectedIds = newIds)
        }
    }

    fun selectAll() {
        val allIds = _uiState.value.minerals.map { it.id }.toSet()
        _uiState.update { it.copy(selectedIds = allIds) }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val mineralsToDelete = currentState.minerals.filter { it.id in currentState.selectedIds }

            deletedMineralsMutex.withLock {
                deletedMinerals = mineralsToDelete
            }

            val idsToDelete = currentState.selectedIds.toList()
            val total = idsToDelete.size

            if (total > 10) {
                processBulkDeleteWithProgress(idsToDelete, total)
            } else {
                mineralRepository.deleteByIds(idsToDelete)
            }

            exitSelectionMode()
            refreshMineralsList()
        }
    }

    private suspend fun processBulkDeleteWithProgress(ids: List<String>, total: Int) {
        try {
            _uiState.update { it.copy(bulkOperationProgress = BulkOperationProgress.InProgress(0, total, "delete")) }

            val batchSize = 10
            ids.chunked(batchSize).forEachIndexed { index, batch ->
                mineralRepository.deleteByIds(batch)
                val current = minOf((index + 1) * batchSize, total)

                _uiState.update { it.copy(bulkOperationProgress = BulkOperationProgress.InProgress(current, total, "delete")) }

                if (current < total) delay(50)
            }

            _uiState.update { it.copy(bulkOperationProgress = BulkOperationProgress.Complete(total, "delete")) }
            delay(2000)
            _uiState.update { it.copy(bulkOperationProgress = BulkOperationProgress.Idle) }
        } catch (e: Exception) {
            _uiState.update { it.copy(bulkOperationProgress = BulkOperationProgress.Error(e.message ?: "Delete failed")) }
            delay(3000)
            _uiState.update { it.copy(bulkOperationProgress = BulkOperationProgress.Idle) }
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            val mineralsToRestore = deletedMineralsMutex.withLock {
                val minerals = deletedMinerals
                deletedMinerals = emptyList()
                minerals
            }

            mineralsToRestore.forEach { mineralRepository.insert(it) }
            refreshMineralsList()
        }
    }

    fun getSelectedMinerals(): List<Mineral> {
        val state = _uiState.value
        return state.minerals.filter { it.id in state.selectedIds }
    }

    fun showFilterDialog() = _uiState.update { it.copy(activeDialog = DialogType.Filter) }
    fun showSortDialog() = _uiState.update { it.copy(activeDialog = DialogType.Sort) }
    fun showBulkActionsDialog() = _uiState.update { it.copy(activeDialog = DialogType.BulkActions) }

    fun showCsvExportWarning() = _uiState.update { it.copy(activeDialog = DialogType.CsvExportWarning) }

    fun showExportCsvDialog() {
        _uiState.update { it.copy(
            activeDialog = DialogType.ExportCsv,
            selectedCsvUri = null
        )}
    }

    fun showImportCsvDialog(uri: Uri? = null) {
         _uiState.update { it.copy(
            activeDialog = DialogType.ImportCsv(uri),
            selectedCsvUri = uri
         )}
    }

    fun dismissDialog() = _uiState.update { it.copy(activeDialog = DialogType.None) }

    fun exportSelectedToCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(exportState = ExportState.Exporting) }
            try {
                val mineralsToExport = getSelectedMinerals()
                if (mineralsToExport.isEmpty()) {
                    _uiState.update { it.copy(exportState = ExportState.Error("No minerals selected")) }
                    return@launch
                }

                val result = backupRepository.exportCsv(uri, mineralsToExport)
                if (result.isSuccess) {
                    _uiState.update { it.copy(exportState = ExportState.Success(mineralsToExport.size)) }
                } else {
                    _uiState.update { it.copy(exportState = ExportState.Error(result.exceptionOrNull()?.message ?: "Unknown error")) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportState = ExportState.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    fun resetExportState() {
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }

    fun markCsvExportWarningShown() {
        viewModelScope.launch {
            settingsRepository.setCsvExportWarningShown(true)
        }
    }

    fun importCsvFile(uri: Uri, columnMapping: Map<String, String>, mode: CsvImportMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(importState = ImportState.Importing) }
            try {
                val result = backupRepository.importCsv(uri, columnMapping, mode)
                if (result.isSuccess) {
                    val importResult = result.getOrThrow()
                    _uiState.update { it.copy(importState = ImportState.Success(importResult.imported, importResult.skipped, importResult.errors)) }
                } else {
                    _uiState.update { it.copy(importState = ImportState.Error(result.exceptionOrNull()?.message ?: "Unknown error")) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importState = ImportState.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    fun resetImportState() {
         _uiState.update { it.copy(importState = ImportState.Idle) }
    }

    fun generateLabelsForSelected(outputUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(labelGenerationState = LabelGenerationState.Generating) }
            try {
                val selectedMinerals = getSelectedMinerals()
                if (selectedMinerals.isEmpty()) {
                     _uiState.update { it.copy(labelGenerationState = LabelGenerationState.Error("No minerals selected")) }
                    return@launch
                }
                val generator = QrLabelPdfGenerator(context)
                val result = generator.generate(selectedMinerals, outputUri)
                if (result.isSuccess) {
                     _uiState.update { it.copy(labelGenerationState = LabelGenerationState.Success(selectedMinerals.size)) }
                } else {
                     _uiState.update { it.copy(labelGenerationState = LabelGenerationState.Error(result.exceptionOrNull()?.message ?: "Error")) }
                }
            } catch (e: Exception) {
                 _uiState.update { it.copy(labelGenerationState = LabelGenerationState.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    fun resetLabelGenerationState() {
        _uiState.update { it.copy(labelGenerationState = LabelGenerationState.Idle) }
    }

    /**
     * Generates a PDF catalog of the entire mineral collection.
     * Includes all minerals (not just selected ones) with photos, names, formulas, and provenance.
     *
     * @param outputUri URI where the PDF will be saved (from CreateDocument contract)
     */
    fun generateCatalogPdf(outputUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(catalogGenerationState = CatalogGenerationState.Generating) }
            try {
                // Get all minerals (not filtered, not paged)
                val allMinerals = mineralRepository.getAll()

                if (allMinerals.isEmpty()) {
                    _uiState.update { it.copy(catalogGenerationState = CatalogGenerationState.Error("No minerals in collection")) }
                    return@launch
                }

                val generator = PdfCatalogGenerator(context)
                val result = generator.generateCatalog(allMinerals, outputUri)

                if (result.isSuccess) {
                    _uiState.update { it.copy(catalogGenerationState = CatalogGenerationState.Success(allMinerals.size)) }
                } else {
                    _uiState.update { it.copy(catalogGenerationState = CatalogGenerationState.Error(result.exceptionOrNull()?.message ?: "Error")) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(catalogGenerationState = CatalogGenerationState.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    /**
     * Resets the catalog generation state to Idle.
     */
    fun resetCatalogGenerationState() {
        _uiState.update { it.copy(catalogGenerationState = CatalogGenerationState.Idle) }
    }

    fun resetBulkOperationProgress() {
        _uiState.update { it.copy(bulkOperationProgress = BulkOperationProgress.Idle) }
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

sealed class CatalogGenerationState {
    data object Idle : CatalogGenerationState()
    data object Generating : CatalogGenerationState()
    data class Success(val count: Int) : CatalogGenerationState()
    data class Error(val message: String) : CatalogGenerationState()
}

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