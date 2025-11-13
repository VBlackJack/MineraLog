package net.meshcore.mineralog.ui.screens.comparator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.domain.model.Mineral

/**
 * ViewModel for Comparator screen.
 * Loads selected minerals and manages comparison UI state.
 */
class ComparatorViewModel(
    private val mineralIds: List<String>,
    private val mineralRepository: MineralRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ComparatorUiState>(ComparatorUiState.Loading)
    val uiState: StateFlow<ComparatorUiState> = _uiState.asStateFlow()

    init {
        loadMinerals()
    }

    private fun loadMinerals() {
        viewModelScope.launch {
            try {
                // Validate input
                if (mineralIds.size < 2) {
                    _uiState.value = ComparatorUiState.Error("At least 2 minerals required for comparison")
                    return@launch
                }

                if (mineralIds.size > 3) {
                    _uiState.value = ComparatorUiState.Error("Maximum 3 minerals can be compared")
                    return@launch
                }

                // Load minerals
                val minerals = mineralIds.mapNotNull { id ->
                    mineralRepository.getById(id)
                }

                if (minerals.size != mineralIds.size) {
                    _uiState.value = ComparatorUiState.Error("Some minerals could not be loaded")
                    return@launch
                }

                _uiState.value = ComparatorUiState.Success(minerals)
            } catch (e: Exception) {
                _uiState.value = ComparatorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

/**
 * UI state for Comparator screen.
 */
sealed class ComparatorUiState {
    data object Loading : ComparatorUiState()
    data class Success(val minerals: List<Mineral>) : ComparatorUiState()
    data class Error(val message: String) : ComparatorUiState()
}

/**
 * Factory for creating ComparatorViewModel with dependencies.
 */
class ComparatorViewModelFactory(
    private val mineralIds: List<String>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ComparatorViewModel::class.java)) {
            // This is a placeholder - actual repository injection happens in NavHost
            throw IllegalStateException("Use factory with repository injection")
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
