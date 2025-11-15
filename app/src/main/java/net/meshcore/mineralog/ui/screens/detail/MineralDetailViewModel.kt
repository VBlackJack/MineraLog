package net.meshcore.mineralog.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.domain.model.Mineral

sealed class DeleteState {
    data object Idle : DeleteState()
    data object Deleting : DeleteState()
    data object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}

class MineralDetailViewModel(
    private val mineralId: String,
    private val mineralRepository: MineralRepository
) : ViewModel() {

    val mineral: StateFlow<Mineral?> = mineralRepository.getByIdFlow(mineralId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    /**
     * Delete this mineral and all associated data (photos, provenance, storage).
     */
    fun deleteMineral() {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            try {
                mineralRepository.delete(mineralId)
                _deleteState.value = DeleteState.Success
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error(e.message ?: "Failed to delete mineral")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }
}

class MineralDetailViewModelFactory(
    private val mineralId: String,
    private val mineralRepository: MineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MineralDetailViewModel::class.java)) {
            return MineralDetailViewModel(mineralId, mineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
