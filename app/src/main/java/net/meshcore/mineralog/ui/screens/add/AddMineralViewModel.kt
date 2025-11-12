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

    fun onNameChange(value: String) {
        _name.value = value
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
            val mineralId = UUID.randomUUID().toString()
            val mineral = Mineral(
                id = mineralId,
                name = _name.value,
                group = _group.value.takeIf { it.isNotBlank() },
                formula = _formula.value.takeIf { it.isNotBlank() },
                notes = _notes.value.takeIf { it.isNotBlank() },
                status = "incomplete",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            mineralRepository.insert(mineral)
            onSuccess(mineralId)
        }
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
