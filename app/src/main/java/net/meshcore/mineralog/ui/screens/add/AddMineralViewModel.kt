package net.meshcore.mineralog.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.data.repository.SettingsRepository
import net.meshcore.mineralog.domain.model.Mineral
import java.time.Instant
import java.util.UUID

sealed class SaveMineralState {
    data object Idle : SaveMineralState()
    data object Saving : SaveMineralState()
    data class Success(val mineralId: String) : SaveMineralState()
    data class Error(val message: String) : SaveMineralState()
}

@OptIn(FlowPreview::class)
class AddMineralViewModel(
    private val mineralRepository: MineralRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _group = MutableStateFlow("")
    val group: StateFlow<String> = _group.asStateFlow()

    private val _formula = MutableStateFlow("")
    val formula: StateFlow<String> = _formula.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    // Technical mineral properties with tooltips
    private val _diaphaneity = MutableStateFlow("")
    val diaphaneity: StateFlow<String> = _diaphaneity.asStateFlow()

    private val _cleavage = MutableStateFlow("")
    val cleavage: StateFlow<String> = _cleavage.asStateFlow()

    private val _fracture = MutableStateFlow("")
    val fracture: StateFlow<String> = _fracture.asStateFlow()

    private val _luster = MutableStateFlow("")
    val luster: StateFlow<String> = _luster.asStateFlow()

    private val _streak = MutableStateFlow("")
    val streak: StateFlow<String> = _streak.asStateFlow()

    private val _habit = MutableStateFlow("")
    val habit: StateFlow<String> = _habit.asStateFlow()

    private val _crystalSystem = MutableStateFlow("")
    val crystalSystem: StateFlow<String> = _crystalSystem.asStateFlow()

    // Quick Win #8: Tags with autocomplete (v1.7.0)
    private val _tags = MutableStateFlow("")
    val tags: StateFlow<String> = _tags.asStateFlow()

    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val availableTags: StateFlow<List<String>> = _availableTags.asStateFlow()

    private val _tagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val tagSuggestions: StateFlow<List<String>> = _tagSuggestions.asStateFlow()

    private val _saveState = MutableStateFlow<SaveMineralState>(SaveMineralState.Idle)
    val saveState: StateFlow<SaveMineralState> = _saveState.asStateFlow()

    private val _draftSavedIndicator = MutableStateFlow(false)
    val draftSavedIndicator: StateFlow<Boolean> = _draftSavedIndicator.asStateFlow()

    init {
        // Load draft on initialization
        loadDraft()

        // Quick Win #8: Load available tags for autocomplete
        viewModelScope.launch {
            _availableTags.value = mineralRepository.getAllUniqueTags()
        }

        // Quick Win #8: Update tag suggestions based on input
        viewModelScope.launch {
            _tags.debounce(300).collect { input ->
                updateTagSuggestions(input)
            }
        }

        // Auto-save with debounce (500ms)
        viewModelScope.launch {
            combine(
                _name,
                _group,
                _formula,
                _notes,
                _diaphaneity,
                _cleavage,
                _fracture,
                _luster,
                _streak,
                _habit,
                _crystalSystem,
                _tags
            ) { fields -> fields }
                .debounce(500) // Wait 500ms after last change
                .collect {
                    // Only save if any field has content
                    val hasContent = it.any { field -> field.toString().isNotBlank() }
                    if (hasContent) {
                        saveDraft()
                    }
                }
        }
    }

    /**
     * Load draft data from settings.
     * Optimized: Single coroutine instead of 11 separate launches for better performance.
     */
    private fun loadDraft() {
        viewModelScope.launch {
            // Load all draft fields in parallel within a single coroutine context
            val name = settingsRepository.getDraftName().first()
            val group = settingsRepository.getDraftGroup().first()
            val formula = settingsRepository.getDraftFormula().first()
            val notes = settingsRepository.getDraftNotes().first()
            val diaphaneity = settingsRepository.getDraftDiaphaneity().first()
            val cleavage = settingsRepository.getDraftCleavage().first()
            val fracture = settingsRepository.getDraftFracture().first()
            val luster = settingsRepository.getDraftLuster().first()
            val streak = settingsRepository.getDraftStreak().first()
            val habit = settingsRepository.getDraftHabit().first()
            val crystalSystem = settingsRepository.getDraftCrystalSystem().first()

            // Update state only if values are non-empty
            if (name.isNotEmpty()) _name.value = name
            if (group.isNotEmpty()) _group.value = group
            if (formula.isNotEmpty()) _formula.value = formula
            if (notes.isNotEmpty()) _notes.value = notes
            if (diaphaneity.isNotEmpty()) _diaphaneity.value = diaphaneity
            if (cleavage.isNotEmpty()) _cleavage.value = cleavage
            if (fracture.isNotEmpty()) _fracture.value = fracture
            if (luster.isNotEmpty()) _luster.value = luster
            if (streak.isNotEmpty()) _streak.value = streak
            if (habit.isNotEmpty()) _habit.value = habit
            if (crystalSystem.isNotEmpty()) _crystalSystem.value = crystalSystem
        }
    }

    private suspend fun saveDraft() {
        settingsRepository.setDraftName(_name.value)
        settingsRepository.setDraftGroup(_group.value)
        settingsRepository.setDraftFormula(_formula.value)
        settingsRepository.setDraftNotes(_notes.value)
        settingsRepository.setDraftDiaphaneity(_diaphaneity.value)
        settingsRepository.setDraftCleavage(_cleavage.value)
        settingsRepository.setDraftFracture(_fracture.value)
        settingsRepository.setDraftLuster(_luster.value)
        settingsRepository.setDraftStreak(_streak.value)
        settingsRepository.setDraftHabit(_habit.value)
        settingsRepository.setDraftCrystalSystem(_crystalSystem.value)
        settingsRepository.setDraftTimestamp(System.currentTimeMillis())

        // Show "Draft saved" indicator briefly
        _draftSavedIndicator.value = true
        kotlinx.coroutines.delay(2000)
        _draftSavedIndicator.value = false
    }

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

    fun onDiaphaneityChange(value: String) {
        _diaphaneity.value = value
    }

    fun onCleavageChange(value: String) {
        _cleavage.value = value
    }

    fun onFractureChange(value: String) {
        _fracture.value = value
    }

    fun onLusterChange(value: String) {
        _luster.value = value
    }

    fun onStreakChange(value: String) {
        _streak.value = value
    }

    fun onHabitChange(value: String) {
        _habit.value = value
    }

    fun onCrystalSystemChange(value: String) {
        _crystalSystem.value = value
    }

    // Quick Win #8: Tag management
    fun onTagsChange(value: String) {
        _tags.value = value
    }

    private fun updateTagSuggestions(input: String) {
        if (input.isBlank()) {
            _tagSuggestions.value = emptyList()
            return
        }

        // Get the last tag being typed (after the last comma)
        val lastTag = input.split(",").lastOrNull()?.trim() ?: ""

        if (lastTag.length < 2) {
            _tagSuggestions.value = emptyList()
            return
        }

        // Filter available tags and return top 5 matches
        _tagSuggestions.value = _availableTags.value
            .filter { it.contains(lastTag, ignoreCase = true) }
            .take(5)
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
                // Quick Win #8: Parse tags from comma-separated string
                val tagsList = _tags.value.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val mineral = Mineral(
                    id = mineralId,
                    name = _name.value.trim(),
                    group = _group.value.trim().takeIf { it.isNotBlank() },
                    formula = _formula.value.trim().takeIf { it.isNotBlank() },
                    notes = _notes.value.trim().takeIf { it.isNotBlank() },
                    diaphaneity = _diaphaneity.value.trim().takeIf { it.isNotBlank() },
                    cleavage = _cleavage.value.trim().takeIf { it.isNotBlank() },
                    fracture = _fracture.value.trim().takeIf { it.isNotBlank() },
                    luster = _luster.value.trim().takeIf { it.isNotBlank() },
                    streak = _streak.value.trim().takeIf { it.isNotBlank() },
                    habit = _habit.value.trim().takeIf { it.isNotBlank() },
                    crystalSystem = _crystalSystem.value.trim().takeIf { it.isNotBlank() },
                    tags = tagsList,
                    status = "incomplete",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                mineralRepository.insert(mineral)
                _saveState.value = SaveMineralState.Success(mineralId)

                // Clear draft after successful save
                settingsRepository.clearDraft()

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
    private val mineralRepository: MineralRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddMineralViewModel::class.java)) {
            return AddMineralViewModel(mineralRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
