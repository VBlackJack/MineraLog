package net.meshcore.mineralog.ui.screens.home

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
import net.meshcore.mineralog.domain.model.FilterPreset
import net.meshcore.mineralog.domain.model.Mineral

class HomeViewModel(
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

    // Paged minerals for efficient large dataset handling (v1.5.0)
    val mineralsPaged: Flow<PagingData<Mineral>> = combine(
        _searchQuery.debounce(300),
        _filterCriteria,
        _isFilterActive
    ) { query, criteria, filterActive ->
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
            // Batch delete all selected minerals efficiently
            val idsToDelete = _selectedIds.value.toList()
            mineralRepository.deleteByIds(idsToDelete)
            exitSelectionMode()
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
    fun importCsvFile(uri: Uri, mode: CsvImportMode) {
        viewModelScope.launch {
            _importState.value = ImportState.Importing
            try {
                val result = backupRepository.importCsv(uri, mode)

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

class HomeViewModelFactory(
    private val mineralRepository: MineralRepository,
    private val filterPresetRepository: FilterPresetRepository,
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(mineralRepository, filterPresetRepository, backupRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
