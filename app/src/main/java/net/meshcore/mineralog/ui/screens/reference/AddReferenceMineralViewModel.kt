package net.meshcore.mineralog.ui.screens.reference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.data.repository.ReferenceMineralRepository
import java.time.Instant
import java.util.UUID

/**
 * ViewModel for adding a new reference mineral to the library.
 *
 * Manages form state, validation, and saving of user-defined reference minerals.
 */
class AddReferenceMineralViewModel(
    private val referenceMineralRepository: ReferenceMineralRepository
) : ViewModel() {

    // Identification fields
    private val _nameFr = MutableStateFlow("")
    val nameFr: StateFlow<String> = _nameFr.asStateFlow()

    private val _nameEn = MutableStateFlow("")
    val nameEn: StateFlow<String> = _nameEn.asStateFlow()

    private val _synonyms = MutableStateFlow("")
    val synonyms: StateFlow<String> = _synonyms.asStateFlow()

    private val _mineralGroup = MutableStateFlow("")
    val mineralGroup: StateFlow<String> = _mineralGroup.asStateFlow()

    // Chemistry
    private val _formula = MutableStateFlow("")
    val formula: StateFlow<String> = _formula.asStateFlow()

    // Physical properties
    private val _mohsMin = MutableStateFlow("")
    val mohsMin: StateFlow<String> = _mohsMin.asStateFlow()

    private val _mohsMax = MutableStateFlow("")
    val mohsMax: StateFlow<String> = _mohsMax.asStateFlow()

    private val _density = MutableStateFlow("")
    val density: StateFlow<String> = _density.asStateFlow()

    // Crystallographic properties
    private val _crystalSystem = MutableStateFlow("")
    val crystalSystem: StateFlow<String> = _crystalSystem.asStateFlow()

    private val _cleavage = MutableStateFlow("")
    val cleavage: StateFlow<String> = _cleavage.asStateFlow()

    private val _fracture = MutableStateFlow("")
    val fracture: StateFlow<String> = _fracture.asStateFlow()

    private val _habit = MutableStateFlow("")
    val habit: StateFlow<String> = _habit.asStateFlow()

    // Optical properties
    private val _luster = MutableStateFlow("")
    val luster: StateFlow<String> = _luster.asStateFlow()

    private val _streak = MutableStateFlow("")
    val streak: StateFlow<String> = _streak.asStateFlow()

    private val _diaphaneity = MutableStateFlow("")
    val diaphaneity: StateFlow<String> = _diaphaneity.asStateFlow()

    // Special properties
    private val _fluorescence = MutableStateFlow("")
    val fluorescence: StateFlow<String> = _fluorescence.asStateFlow()

    private val _magnetism = MutableStateFlow("")
    val magnetism: StateFlow<String> = _magnetism.asStateFlow()

    private val _radioactivity = MutableStateFlow("")
    val radioactivity: StateFlow<String> = _radioactivity.asStateFlow()

    // Notes
    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _source = MutableStateFlow("")
    val source: StateFlow<String> = _source.asStateFlow()

    // Save state
    sealed class SaveState {
        data object Idle : SaveState()
        data object Saving : SaveState()
        data object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // Validation errors
    private val _nameFrError = MutableStateFlow<String?>(null)
    val nameFrError: StateFlow<String?> = _nameFrError.asStateFlow()

    private val _nameEnError = MutableStateFlow<String?>(null)
    val nameEnError: StateFlow<String?> = _nameEnError.asStateFlow()

    // Field change handlers
    fun onNameFrChange(value: String) {
        _nameFr.value = value
        _nameFrError.value = null
    }

    fun onNameEnChange(value: String) {
        _nameEn.value = value
        _nameEnError.value = null
    }

    fun onSynonymsChange(value: String) { _synonyms.value = value }
    fun onMineralGroupChange(value: String) { _mineralGroup.value = value }
    fun onFormulaChange(value: String) { _formula.value = value }
    fun onMohsMinChange(value: String) { _mohsMin.value = value }
    fun onMohsMaxChange(value: String) { _mohsMax.value = value }
    fun onDensityChange(value: String) { _density.value = value }
    fun onCrystalSystemChange(value: String) { _crystalSystem.value = value }
    fun onCleavageChange(value: String) { _cleavage.value = value }
    fun onFractureChange(value: String) { _fracture.value = value }
    fun onHabitChange(value: String) { _habit.value = value }
    fun onLusterChange(value: String) { _luster.value = value }
    fun onStreakChange(value: String) { _streak.value = value }
    fun onDiaphaneityChange(value: String) { _diaphaneity.value = value }
    fun onFluorescenceChange(value: String) { _fluorescence.value = value }
    fun onMagnetismChange(value: String) { _magnetism.value = value }
    fun onRadioactivityChange(value: String) { _radioactivity.value = value }
    fun onNotesChange(value: String) { _notes.value = value }
    fun onSourceChange(value: String) { _source.value = value }

    /**
     * Validate and save the reference mineral.
     */
    fun save() {
        viewModelScope.launch {
            // Validation
            var hasErrors = false

            if (_nameFr.value.isBlank()) {
                _nameFrError.value = "Le nom français est obligatoire"
                hasErrors = true
            }

            if (_nameEn.value.isBlank()) {
                _nameEnError.value = "Le nom anglais est obligatoire"
                hasErrors = true
            }

            if (hasErrors) {
                _saveState.value = SaveState.Error("Veuillez corriger les erreurs")
                return@launch
            }

            try {
                _saveState.value = SaveState.Saving

                // Check for duplicate names
                val existingByNameFr = referenceMineralRepository.searchByNameLimit(_nameFr.value, 1).first()
                if (existingByNameFr.isNotEmpty()) {
                    _nameFrError.value = "Ce nom français existe déjà dans la bibliothèque"
                    _saveState.value = SaveState.Error("Nom déjà existant")
                    return@launch
                }

                val mineral = ReferenceMineralEntity(
                    id = UUID.randomUUID().toString(),
                    nameFr = _nameFr.value.trim(),
                    nameEn = _nameEn.value.trim(),
                    synonyms = _synonyms.value.trim().takeIf { it.isNotBlank() },
                    mineralGroup = _mineralGroup.value.trim().takeIf { it.isNotBlank() },
                    formula = _formula.value.trim().takeIf { it.isNotBlank() },
                    mohsMin = _mohsMin.value.toFloatOrNull(),
                    mohsMax = _mohsMax.value.toFloatOrNull(),
                    density = _density.value.toFloatOrNull(),
                    crystalSystem = _crystalSystem.value.trim().takeIf { it.isNotBlank() },
                    cleavage = _cleavage.value.trim().takeIf { it.isNotBlank() },
                    fracture = _fracture.value.trim().takeIf { it.isNotBlank() },
                    habit = _habit.value.trim().takeIf { it.isNotBlank() },
                    luster = _luster.value.trim().takeIf { it.isNotBlank() },
                    streak = _streak.value.trim().takeIf { it.isNotBlank() },
                    diaphaneity = _diaphaneity.value.trim().takeIf { it.isNotBlank() },
                    fluorescence = _fluorescence.value.trim().takeIf { it.isNotBlank() },
                    magnetism = _magnetism.value.trim().takeIf { it.isNotBlank() },
                    radioactivity = _radioactivity.value.trim().takeIf { it.isNotBlank() },
                    notes = _notes.value.trim().takeIf { it.isNotBlank() },
                    isUserDefined = true,
                    source = _source.value.trim().takeIf { it.isNotBlank() } ?: "Ajouté par l'utilisateur",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                referenceMineralRepository.insert(mineral)
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Erreur lors de la sauvegarde")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}

/**
 * Factory for creating AddReferenceMineralViewModel instances.
 */
class AddReferenceMineralViewModelFactory(
    private val referenceMineralRepository: ReferenceMineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddReferenceMineralViewModel::class.java)) {
            return AddReferenceMineralViewModel(referenceMineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
