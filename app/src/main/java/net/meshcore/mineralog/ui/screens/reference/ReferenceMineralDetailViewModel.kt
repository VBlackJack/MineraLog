package net.meshcore.mineralog.ui.screens.reference

import androidx.lifecycle.ViewModel
import net.meshcore.mineralog.util.AppLogger
import androidx.lifecycle.ViewModelProvider
import net.meshcore.mineralog.util.AppLogger
import androidx.lifecycle.viewModelScope
import net.meshcore.mineralog.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import net.meshcore.mineralog.util.AppLogger
import kotlinx.coroutines.flow.StateFlow
import net.meshcore.mineralog.util.AppLogger
import kotlinx.coroutines.flow.asStateFlow
import net.meshcore.mineralog.util.AppLogger
import kotlinx.coroutines.launch
import net.meshcore.mineralog.util.AppLogger
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.util.AppLogger
import net.meshcore.mineralog.data.repository.ReferenceMineralRepository
import net.meshcore.mineralog.util.AppLogger

/**
 * ViewModel for the Reference Mineral detail screen.
 *
 * Manages the display of a single reference mineral's complete information
 * and usage statistics.
 */
class ReferenceMineralDetailViewModel(
    private val referenceMineralId: String,
    private val referenceMineralRepository: ReferenceMineralRepository
) : ViewModel() {

    private val _mineral = MutableStateFlow<ReferenceMineralEntity?>(null)
    val mineral: StateFlow<ReferenceMineralEntity?> = _mineral.asStateFlow()

    private val _simpleSpecimensCount = MutableStateFlow(0)
    val simpleSpecimensCount: StateFlow<Int> = _simpleSpecimensCount.asStateFlow()

    private val _componentsCount = MutableStateFlow(0)
    val componentsCount: StateFlow<Int> = _componentsCount.asStateFlow()

    private val _totalUsageCount = MutableStateFlow(0)
    val totalUsageCount: StateFlow<Int> = _totalUsageCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // v3.0: Delete state
    sealed class DeleteState {
        data object Idle : DeleteState()
        data class ConfirmRequired(val usageCount: Int) : DeleteState()
        data object Deleting : DeleteState()
        data object Success : DeleteState()
        data class Error(val message: String) : DeleteState()
    }

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    init {
        loadMineral()
        loadUsageStatistics()
    }

    private fun loadMineral() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val loadedMineral = referenceMineralRepository.getById(referenceMineralId)
                if (loadedMineral != null) {
                    _mineral.value = loadedMineral
                } else {
                    _error.value = "Minéral de référence introuvable"
                }
            } catch (e: Exception) {
                _error.value = "Erreur de chargement: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadUsageStatistics() {
        viewModelScope.launch {
            try {
                _simpleSpecimensCount.value = referenceMineralRepository.countSimpleSpecimensUsingReference(referenceMineralId)
                _componentsCount.value = referenceMineralRepository.countComponentsUsingReference(referenceMineralId)
                _totalUsageCount.value = referenceMineralRepository.getTotalUsageCount(referenceMineralId)
            } catch (e: Exception) {
                // Silently fail for statistics - not critical
                AppLogger.e("RefMineralDetailVM", "Failed to load usage stats", e)
            }
        }
    }

    /**
     * Refresh the mineral data and statistics.
     */
    fun refresh() {
        loadMineral()
        loadUsageStatistics()
    }

    /**
     * Initiate deletion of the reference mineral.
     * Checks for dependencies and requests confirmation if needed.
     */
    fun initiateDelete() {
        viewModelScope.launch {
            try {
                val usageCount = _totalUsageCount.value
                if (usageCount > 0) {
                    _deleteState.value = DeleteState.ConfirmRequired(usageCount)
                } else {
                    // No dependencies, can delete directly
                    confirmDelete()
                }
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error("Erreur lors de la vérification: ${e.message}")
            }
        }
    }

    /**
     * Confirm and execute the deletion.
     * Sets referenceMineralId to NULL for all linked entities, then deletes the mineral.
     */
    fun confirmDelete() {
        viewModelScope.launch {
            try {
                _deleteState.value = DeleteState.Deleting

                // The repository should handle setting referenceMineralId to NULL
                // via canDelete() check and proper deletion logic
                val canDelete = referenceMineralRepository.canDelete(referenceMineralId)

                if (canDelete) {
                    referenceMineralRepository.deleteById(referenceMineralId)
                    _deleteState.value = DeleteState.Success
                } else {
                    // This shouldn't happen if we checked properly, but handle it
                    _deleteState.value = DeleteState.Error("Impossible de supprimer ce minéral")
                }
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error("Erreur lors de la suppression: ${e.message}")
            }
        }
    }

    /**
     * Cancel the deletion.
     */
    fun cancelDelete() {
        _deleteState.value = DeleteState.Idle
    }

    /**
     * Reset delete state after handling.
     */
    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }
}

/**
 * Factory for creating ReferenceMineralDetailViewModel instances.
 */
class ReferenceMineralDetailViewModelFactory(
    private val referenceMineralId: String,
    private val referenceMineralRepository: ReferenceMineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReferenceMineralDetailViewModel::class.java)) {
            return ReferenceMineralDetailViewModel(referenceMineralId, referenceMineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
