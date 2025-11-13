package net.meshcore.mineralog.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.data.repository.FilterPresetRepository
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.domain.model.FilterPreset
import net.meshcore.mineralog.domain.model.Mineral

class HomeViewModel(
    private val mineralRepository: MineralRepository,
    private val filterPresetRepository: FilterPresetRepository
) : ViewModel() {

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
            _selectedIds.value.forEach { id ->
                mineralRepository.delete(id)
            }
            exitSelectionMode()
        }
    }

    fun getSelectedMinerals(): List<Mineral> {
        return minerals.value.filter { it.id in _selectedIds.value }
    }
}

class HomeViewModelFactory(
    private val mineralRepository: MineralRepository,
    private val filterPresetRepository: FilterPresetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(mineralRepository, filterPresetRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
