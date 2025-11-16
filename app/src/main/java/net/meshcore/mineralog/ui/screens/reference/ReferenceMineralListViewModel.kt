package net.meshcore.mineralog.ui.screens.reference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pag

er
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.data.repository.ReferenceMineralRepository

/**
 * ViewModel for the Reference Mineral Library list screen.
 *
 * Manages the paginated list of reference minerals, search functionality,
 * and filter operations.
 */
class ReferenceMineralListViewModel(
    private val referenceMineralRepository: ReferenceMineralRepository
) : ViewModel() {

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Paginated minerals list
    val mineralsPaged = referenceMineralRepository.getAllPaged()
        .cachedIn(viewModelScope)

    // Filter metadata
    val distinctGroups = referenceMineralRepository.getDistinctGroups()
    val distinctCrystalSystems = referenceMineralRepository.getDistinctCrystalSystems()

    // Total count
    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    init {
        loadTotalCount()
    }

    /**
     * Update search query.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        // Note: For now we're using simple pagination.
        // Search functionality will be enhanced in a future iteration.
    }

    /**
     * Clear search query.
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Refresh the list.
     */
    fun refresh() {
        loadTotalCount()
    }

    private fun loadTotalCount() {
        viewModelScope.launch {
            _totalCount.value = referenceMineralRepository.count()
        }
    }
}

/**
 * Factory for creating ReferenceMineralListViewModel instances.
 */
class ReferenceMineralListViewModelFactory(
    private val referenceMineralRepository: ReferenceMineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReferenceMineralListViewModel::class.java)) {
            return ReferenceMineralListViewModel(referenceMineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
