package net.meshcore.mineralog.ui.screens.add

import android.content.Context
import net.meshcore.mineralog.util.AppLogger
import android.net.Uri
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
import net.meshcore.mineralog.data.repository.MineralRepositoryImpl
import net.meshcore.mineralog.data.repository.SettingsRepository
import net.meshcore.mineralog.data.repository.SimpleMineralData
import net.meshcore.mineralog.data.repository.AggregateMineralData
import net.meshcore.mineralog.data.repository.insertSimpleMineral
import net.meshcore.mineralog.data.repository.insertAggregate
import net.meshcore.mineralog.data.repository.ReferenceMineralRepository
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.MineralComponent
import net.meshcore.mineralog.domain.model.MineralType
import net.meshcore.mineralog.domain.model.SimpleProperties
import net.meshcore.mineralog.domain.model.Photo
import net.meshcore.mineralog.ui.screens.edit.PhotoItem
import java.io.File
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
    private val context: Context,
    private val mineralRepository: MineralRepository,
    private val settingsRepository: SettingsRepository,
    private val referenceMineralRepository: ReferenceMineralRepository
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

    private val _mohsHardness = MutableStateFlow("")
    val mohsHardness: StateFlow<String> = _mohsHardness.asStateFlow()

    // Quick Win #8: Tags with autocomplete (v1.7.0)
    private val _tags = MutableStateFlow("")
    val tags: StateFlow<String> = _tags.asStateFlow()

    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val availableTags: StateFlow<List<String>> = _availableTags.asStateFlow()

    private val _tagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val tagSuggestions: StateFlow<List<String>> = _tagSuggestions.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos: StateFlow<List<PhotoItem>> = _photos.asStateFlow()

    private val _saveState = MutableStateFlow<SaveMineralState>(SaveMineralState.Idle)
    val saveState: StateFlow<SaveMineralState> = _saveState.asStateFlow()

    private val _draftSavedIndicator = MutableStateFlow(false)
    val draftSavedIndicator: StateFlow<Boolean> = _draftSavedIndicator.asStateFlow()

    // v2.0: Mineral type selection (SIMPLE or AGGREGATE)
    private val _mineralType = MutableStateFlow(MineralType.SIMPLE)
    val mineralType: StateFlow<MineralType> = _mineralType.asStateFlow()

    // v2.0: Components for aggregate minerals
    private val _components = MutableStateFlow<List<MineralComponent>>(emptyList())
    val components: StateFlow<List<MineralComponent>> = _components.asStateFlow()

    // v3.0: Reference mineral autocomplete
    private val _selectedReferenceMineral = MutableStateFlow<ReferenceMineralEntity?>(null)
    val selectedReferenceMineral: StateFlow<ReferenceMineralEntity?> = _selectedReferenceMineral.asStateFlow()

    private val _referenceMineralSearchQuery = MutableStateFlow("")
    val referenceMineralSearchQuery: StateFlow<String> = _referenceMineralSearchQuery.asStateFlow()

    private val _referenceMineralSuggestions = MutableStateFlow<List<ReferenceMineralEntity>>(emptyList())
    val referenceMineralSuggestions: StateFlow<List<ReferenceMineralEntity>> = _referenceMineralSuggestions.asStateFlow()

    private val _isLoadingReferenceMinerals = MutableStateFlow(false)
    val isLoadingReferenceMinerals: StateFlow<Boolean> = _isLoadingReferenceMinerals.asStateFlow()

    // v3.0: Specimen-specific fields (override reference properties)
    private val _colorVariety = MutableStateFlow("")
    val colorVariety: StateFlow<String> = _colorVariety.asStateFlow()

    private val _actualDiaphaneity = MutableStateFlow("")
    val actualDiaphaneity: StateFlow<String> = _actualDiaphaneity.asStateFlow()

    private val _qualityNotes = MutableStateFlow("")
    val qualityNotes: StateFlow<String> = _qualityNotes.asStateFlow()

    // Sprint 5: Price and Weight fields
    private val _price = MutableStateFlow("")
    val price: StateFlow<String> = _price.asStateFlow()

    private val _currency = MutableStateFlow("USD")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _weightGr = MutableStateFlow("")
    val weightGr: StateFlow<String> = _weightGr.asStateFlow()

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

        // v3.0: Reference mineral autocomplete search with debounce
        viewModelScope.launch {
            _referenceMineralSearchQuery.debounce(300).collect { query ->
                if (query.length >= 2) {
                    _isLoadingReferenceMinerals.value = true
                    referenceMineralRepository.searchByNameLimit(query, 10).collect { results ->
                        _referenceMineralSuggestions.value = results
                        _isLoadingReferenceMinerals.value = false
                    }
                } else {
                    _referenceMineralSuggestions.value = emptyList()
                    _isLoadingReferenceMinerals.value = false
                }
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

    fun onMohsHardnessChange(value: String) {
        _mohsHardness.value = value
    }

    // Quick Win #8: Tag management
    fun onTagsChange(value: String) {
        _tags.value = value
    }

    // v2.0: Mineral type selection
    fun onMineralTypeChange(type: MineralType) {
        _mineralType.value = type
        // Clear components when switching to SIMPLE
        if (type == MineralType.SIMPLE) {
            _components.value = emptyList()
        }
    }

    // v2.0: Component management
    fun onComponentsChange(components: List<MineralComponent>) {
        _components.value = components
    }

    // v3.0: Reference mineral selection
    fun onReferenceMineralSearchQueryChange(query: String) {
        _referenceMineralSearchQuery.value = query
    }

    fun selectReferenceMineral(mineral: ReferenceMineralEntity) {
        _selectedReferenceMineral.value = mineral

        // Auto-fill technical properties from reference
        mineral.mineralGroup?.let { _group.value = it }
        mineral.formula?.let { _formula.value = it }
        mineral.crystalSystem?.let { _crystalSystem.value = it }
        mineral.cleavage?.let { _cleavage.value = it }
        mineral.fracture?.let { _fracture.value = it }
        mineral.luster?.let { _luster.value = it }
        mineral.streak?.let { _streak.value = it }
        mineral.diaphaneity?.let { _diaphaneity.value = it }
        mineral.habit?.let { _habit.value = it }

        // Clear suggestions
        _referenceMineralSuggestions.value = emptyList()
    }

    fun clearReferenceMineral() {
        _selectedReferenceMineral.value = null
        _referenceMineralSearchQuery.value = ""
        _referenceMineralSuggestions.value = emptyList()

        // Clear auto-filled properties
        _group.value = ""
        _formula.value = ""
        _crystalSystem.value = ""
        _cleavage.value = ""
        _fracture.value = ""
        _luster.value = ""
        _streak.value = ""
        _diaphaneity.value = ""
        _habit.value = ""
    }

    // v3.0: Specimen-specific fields
    fun onColorVarietyChange(value: String) {
        _colorVariety.value = value
    }

    fun onActualDiaphaneityChange(value: String) {
        _actualDiaphaneity.value = value
    }

    fun onQualityNotesChange(value: String) {
        _qualityNotes.value = value
    }

    // Sprint 5: Price and Weight updates
    fun onPriceChange(value: String) {
        _price.value = value
    }

    fun onCurrencyChange(value: String) {
        _currency.value = value
    }

    fun onWeightChange(value: String) {
        _weightGr.value = value
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

    fun addPhoto(uri: Uri, type: String = "NORMAL", caption: String? = null) {
        val photoItem = PhotoItem(
            uri = uri,
            fileName = "photo_${System.currentTimeMillis()}.jpg",
            type = type,
            caption = caption,
            isExisting = false
        )
        _photos.value = _photos.value + photoItem
    }

    fun removePhoto(photoId: String) {
        _photos.value = _photos.value.filter { it.id != photoId }
    }

    fun updatePhotoCaption(photoId: String, caption: String) {
        _photos.value = _photos.value.map { photo ->
            if (photo.id == photoId) {
                photo.copy(caption = caption)
            } else {
                photo
            }
        }
    }

    fun updatePhotoType(photoId: String, type: String) {
        _photos.value = _photos.value.map { photo ->
            if (photo.id == photoId) {
                photo.copy(type = type)
            } else {
                photo
            }
        }
    }

    fun saveMineral(onSuccess: (String) -> Unit, photosDir: File) {
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

            // v2.0: Validate aggregates
            if (_mineralType.value == MineralType.AGGREGATE) {
                if (_components.value.size < 2) {
                    _saveState.value = SaveMineralState.Error("Un agrégat doit avoir au moins 2 composants")
                    return@launch
                }

                // Check that all components have names
                if (_components.value.any { it.mineralName.isBlank() }) {
                    _saveState.value = SaveMineralState.Error("Tous les composants doivent avoir un nom")
                    return@launch
                }

                // Only validate percentages if at least one is provided
                val componentsWithPercentage = _components.value.filter { it.percentage != null }
                if (componentsWithPercentage.isNotEmpty()) {
                    val totalPercentage = componentsWithPercentage.mapNotNull { it.percentage }.sum()
                    if (totalPercentage !in 95f..105f) {
                        _saveState.value = SaveMineralState.Error("Les pourcentages doivent totaliser environ 100% (actuellement ${totalPercentage.toInt()}%)")
                        return@launch
                    }
                }
            }

            _saveState.value = SaveMineralState.Saving

            try {
                val mineralId: String

                // Quick Win #8: Parse tags from comma-separated string
                val tagsList = _tags.value.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                // v2.0: Save based on mineral type
                if (_mineralType.value == MineralType.SIMPLE) {
                    // Save as simple mineral using v2.0+ API
                    val (mohsMin, mohsMax) = parseMohsHardness(_mohsHardness.value)
                    val properties = SimpleProperties(
                        referenceMineralId = _selectedReferenceMineral.value?.id,
                        group = _group.value.trim().takeIf { it.isNotBlank() },
                        formula = _formula.value.trim().takeIf { it.isNotBlank() },
                        crystalSystem = _crystalSystem.value.trim().takeIf { it.isNotBlank() },
                        cleavage = _cleavage.value.trim().takeIf { it'isNotBlank() },
                        fracture = _fracture.value.trim().takeIf { it.isNotBlank() },
                        luster = _luster.value.trim().takeIf { it.isNotBlank() },
                        streak = _streak.value.trim().takeIf { it.isNotBlank() },
                        diaphaneity = _diaphaneity.value.trim().takeIf { it.isNotBlank() },
                        habit = _habit.value.trim().takeIf { it.isNotBlank() },
                        colorVariety = _colorVariety.value.trim().takeIf { it.isNotBlank() },
                        actualDiaphaneity = _actualDiaphaneity.value.trim().takeIf { it.isNotBlank() },
                        qualityNotes = _qualityNotes.value.trim().takeIf { it.isNotBlank() },
                        mohsMin = mohsMin,
                        mohsMax = mohsMax
                    )

                    val simpleMineralData = SimpleMineralData(
                        name = _name.value.trim(),
                        properties = properties,
                        notes = _notes.value.trim().takeIf { it.isNotBlank() },
                        tags = tagsList,
                        statusType = "in_collection",
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )

                    mineralId = (mineralRepository as MineralRepositoryImpl).insertSimpleMineral(simpleMineralData)
                } else {
                    // Save as aggregate using v2.0 API
                    val aggregateData = AggregateMineralData(
                        name = _name.value.trim(),
                        components = _components.value,
                        notes = _notes.value.trim().takeIf { it.isNotBlank() },
                        tags = tagsList,
                        statusType = "in_collection",
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )

                    mineralId = (mineralRepository as MineralRepositoryImpl).insertAggregate(aggregateData)
                }

                // Sprint 5: Update mineral with weightGr and/or provenance if provided
                if (_weightGr.value.isNotBlank() || _price.value.isNotBlank()) {
                    // Load the just-created mineral
                    val createdMineral = mineralRepository.getById(mineralId)
                    if (createdMineral != null) {
                        // Create provenance if price or currency is provided
                        val provenance = if (_price.value.isNotBlank()) {
                            net.meshcore.mineralog.domain.model.Provenance(
                                id = java.util.UUID.randomUUID().toString(),
                                mineralId = mineralId,
                                site = null,
                                locality = null,
                                country = null,
                                latitude = null,
                                longitude = null,
                                acquiredAt = null,
                                source = null,
                                price = _price.value.toFloatOrNull(),
                                estimatedValue = null,
                                currency = _currency.value,
                                mineName = null,
                                dealer = null,
                                catalogNumber = null,
                                collectorName = null,
                                acquisitionNotes = null
                            )
                        } else null

                        // Update mineral with weightGr and provenance
                        val updatedMineral = createdMineral.copy(
                            weightGr = _weightGr.value.toFloatOrNull(),
                            provenance = provenance
                        )
                        mineralRepository.update(updatedMineral)
                    }
                }

                // Insert photos - BUGFIX: Actually copy the file from URI
                _photos.value.forEach { photoItem ->
                    photoItem.uri?.let { uri ->
                        try {
                            // Copy file from URI to app's photos directory
                            val photoFile = File(photosDir, photoItem.fileName)
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                photoFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            // Create Photo entity after successful file copy
                            val photo = Photo(
                                id = photoItem.id,
                                mineralId = mineralId,
                                type = photoItem.type,
                                caption = photoItem.caption,
                                takenAt = Instant.now(),
                                fileName = photoItem.fileName
                            )
                            mineralRepository.insertPhoto(photo)
                        } catch (e: Exception) {
                            // Log error but continue with other photos
                            AppLogger.e("AddMineralViewModel", "Failed to copy photo: ${e.message}", e)
                        }
                    }
                }

                _saveState.value = SaveMineralState.Success(mineralId)

                // Clear draft and photos after successful save
                settingsRepository.clearDraft()
                _photos.value = emptyList()
                _components.value = emptyList()

                onSuccess(mineralId)
            } catch (e: Exception) {
                _saveState.value = SaveMineralState.Error(e.message ?: "Failed to save mineral")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveMineralState.Idle
    }

    private fun parseMohsHardness(hardness: String): Pair<Float?, Float?> {
        if (hardness.isBlank()) return null to null
        return try {
            if (hardness.contains("-")) {
                val parts = hardness.split("-").map { it.trim().toFloat() }
                parts.getOrNull(0) to parts.getOrNull(1)
            } else {
                val value = hardness.toFloat()
                value to value
            }
        } catch (e: NumberFormatException) {
            null to null // Invalid format
        }
    }
}

class AddMineralViewModelFactory(
    private val context: Context,
    private val mineralRepository: MineralRepository,
    private val settingsRepository: SettingsRepository,
    private val referenceMineralRepository: ReferenceMineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddMineralViewModel::class.java)) {
            return AddMineralViewModel(context, mineralRepository, settingsRepository, referenceMineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
