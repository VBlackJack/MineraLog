package net.meshcore.mineralog.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.domain.model.Mineral
import java.time.Instant
import java.util.UUID

sealed class SaveMineralState {
    data object Idle : SaveMineralState()
    data object Saving : SaveMineralState()
    data class Success(val mineralId: String) : SaveMineralState()
    data class Error(val message: String) : SaveMineralState()
}

class AddMineralViewModel(
    private val mineralRepository: MineralRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _group = MutableStateFlow("")
    val group: StateFlow<String> = _group.asStateFlow()

    private val _formula = MutableStateFlow("")
    val formula: StateFlow<String> = _formula.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _saveState = MutableStateFlow<SaveMineralState>(SaveMineralState.Idle)
    val saveState: StateFlow<SaveMineralState> = _saveState.asStateFlow()

    fun onNameChange(value: String) {
        _name.value = value
        _saveState.value = SaveMineralState.Idle // Reset error state on input change
    }

    fun onGroupChange(value: String) {
        _group.value = value
    }

    fun onFormulaChange(value: String) {
        _formula.value = value
    }

    fun onNotesChange(value: String) {
        _notes.value = value
    }

    fun saveMineral(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            // Validation
            if (_name.value.isBlank()) {
                _saveState.value = SaveMineralState.Error("Mineral name is required")
                return@launch
            }

            if (_name.value.length < 2) {
                _saveState.value = SaveMineralState.Error("Mineral name must be at least 2 characters")
                return@launch
            }

            _saveState.value = SaveMineralState.Saving

            try {
                val mineralId = UUID.randomUUID().toString()
                val mineral = Mineral(
                    id = mineralId,
                    name = _name.value.trim(),
                    group = _group.value.trim().takeIf { it.isNotBlank() },
                    formula = _formula.value.trim().takeIf { it.isNotBlank() },
                    notes = _notes.value.trim().takeIf { it.isNotBlank() },
                    status = "incomplete",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                mineralRepository.insert(mineral)
                _saveState.value = SaveMineralState.Success(mineralId)
                onSuccess(mineralId)
            } catch (e: Exception) {
                _saveState.value = SaveMineralState.Error(e.message ?: "Failed to save mineral")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveMineralState.Idle
    }
}

class AddMineralViewModelFactory(
    private val mineralRepository: MineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddMineralViewModel::class.java)) {
            return AddMineralViewModel(mineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
