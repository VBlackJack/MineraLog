package net.meshcore.mineralog.ui.screens.edit

import android.content.Context
import net.meshcore.mineralog.util.AppLogger
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.data.repository.MineralRepositoryImpl
import net.meshcore.mineralog.data.repository.getMineralType
import net.meshcore.mineralog.data.repository.getAggregateComponents
import net.meshcore.mineralog.data.repository.updateAggregateComponents
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.MineralComponent
import net.meshcore.mineralog.domain.model.MineralType
import net.meshcore.mineralog.domain.model.Photo
import java.io.File
import java.time.Instant
import java.util.UUID

sealed class UpdateMineralState {
    data object Idle : UpdateMineralState()
    data object Loading : UpdateMineralState()
    data object Saving : UpdateMineralState()
    data class Success(val mineralId: String) : UpdateMineralState()
    data class Error(val message: String) : UpdateMineralState()
}

data class PhotoItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri? = null,
    val fileName: String = "",
    val type: String = "NORMAL",
    val caption: String? = null,
    val isExisting: Boolean = false
)

class EditMineralViewModel(
    private val context: Context,
    private val mineralId: String,
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

    private val _tags = MutableStateFlow("")
    val tags: StateFlow<String> = _tags.asStateFlow()

    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val availableTags: StateFlow<List<String>> = _availableTags.asStateFlow()

    private val _tagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val tagSuggestions: StateFlow<List<String>> = _tagSuggestions.asStateFlow()

    private val _photos = MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos: StateFlow<List<PhotoItem>> = _photos.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateMineralState>(UpdateMineralState.Idle)
    val updateState: StateFlow<UpdateMineralState> = _updateState.asStateFlow()

    // v2.0: Mineral type (read-only, loaded from database)
    private val _mineralType = MutableStateFlow(MineralType.SIMPLE)
    val mineralType: StateFlow<MineralType> = _mineralType.asStateFlow()

    // v2.0: Components for aggregate minerals
    private val _components = MutableStateFlow<List<MineralComponent>>(emptyList())
    val components: StateFlow<List<MineralComponent>> = _components.asStateFlow()


    // v3.1: Provenance fields
    private val _mineName = MutableStateFlow("")
    val mineName: StateFlow<String> = _mineName.asStateFlow()

    private val _dealer = MutableStateFlow("")
    val dealer: StateFlow<String> = _dealer.asStateFlow()

    private val _catalogNumber = MutableStateFlow("")
    val catalogNumber: StateFlow<String> = _catalogNumber.asStateFlow()

    private val _collectorName = MutableStateFlow("")
    val collectorName: StateFlow<String> = _collectorName.asStateFlow()

    private val _acquisitionNotes = MutableStateFlow("")
    val acquisitionNotes: StateFlow<String> = _acquisitionNotes.asStateFlow()

    // v3.1: Aggregate-specific fields
    private val _rockType = MutableStateFlow("")
    val rockType: StateFlow<String> = _rockType.asStateFlow()

    private val _texture = MutableStateFlow("")
    val texture: StateFlow<String> = _texture.asStateFlow()

    private val _dominantMinerals = MutableStateFlow("")
    val dominantMinerals: StateFlow<String> = _dominantMinerals.asStateFlow()

    private val _interestingFeatures = MutableStateFlow("")
    val interestingFeatures: StateFlow<String> = _interestingFeatures.asStateFlow()

    // Store original mineral as StateFlow to prevent race conditions
    private val _originalMineral = MutableStateFlow<Mineral?>(null)
    private val originalMineral: StateFlow<Mineral?> = _originalMineral.asStateFlow()

    init {
        loadMineral()

        // Load available tags for autocomplete
        viewModelScope.launch {
            _availableTags.value = mineralRepository.getAllUniqueTags()
        }

        // Update tag suggestions based on input
        viewModelScope.launch {
            _tags.debounce(300).collect { input ->
                updateTagSuggestions(input)
            }
        }
    }

    private fun loadMineral() {
        viewModelScope.launch {
            _updateState.value = UpdateMineralState.Loading
            try {
                // v2.0: Load mineral type first
                _mineralType.value = (mineralRepository as MineralRepositoryImpl).getMineralType(mineralId)

                mineralRepository.getByIdFlow(mineralId).collect { mineral ->
                    if (mineral != null) {
                        _originalMineral.value = mineral
                        _name.value = mineral.name
                        _group.value = mineral.group ?: ""
                        _formula.value = mineral.formula ?: ""
                        _notes.value = mineral.notes ?: ""
                        _diaphaneity.value = mineral.diaphaneity ?: ""
                        _cleavage.value = mineral.cleavage ?: ""
                        _fracture.value = mineral.fracture ?: ""
                        _luster.value = mineral.luster ?: ""
                        _streak.value = mineral.streak ?: ""
                        _habit.value = mineral.habit ?: ""
                        _crystalSystem.value = mineral.crystalSystem ?: ""
                        _tags.value = mineral.tags.joinToString(", ")

                        // Load existing photos
                        _photos.value = mineral.photos.map { photo ->
                            PhotoItem(
                                id = photo.id,
                                fileName = photo.fileName,
                                type = photo.type,
                                caption = photo.caption,
                                isExisting = true
                            )
                        }

                        // v3.1: Load provenance fields
                        _mineName.value = mineral.provenance?.mineName ?: ""
                        _dealer.value = mineral.provenance?.dealer ?: ""
                        _catalogNumber.value = mineral.provenance?.catalogNumber ?: ""
                        _collectorName.value = mineral.provenance?.collectorName ?: ""
                        _acquisitionNotes.value = mineral.provenance?.acquisitionNotes ?: ""

                        // v3.1: Load aggregate fields
                        _rockType.value = mineral.rockType ?: ""
                        _texture.value = mineral.texture ?: ""
                        _dominantMinerals.value = mineral.dominantMinerals ?: ""
                        _interestingFeatures.value = mineral.interestingFeatures ?: ""

                        // v2.0: Load components if this is an aggregate
                        if (_mineralType.value == MineralType.AGGREGATE) {
                            _components.value = (mineralRepository as MineralRepositoryImpl)
                                .getAggregateComponents(mineralId)
                        }

                        _updateState.value = UpdateMineralState.Idle
                    } else {
                        _updateState.value = UpdateMineralState.Error("Mineral not found")
                    }
                }
            } catch (e: Exception) {
                _updateState.value = UpdateMineralState.Error(e.message ?: "Failed to load mineral")
            }
        }
    }

    fun onNameChange(value: String) {
        _name.value = value
        _updateState.value = UpdateMineralState.Idle
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

    fun onTagsChange(value: String) {
        _tags.value = value
    }

    // v3.1: Provenance field updates
    fun onMineNameChange(value: String) {
        _mineName.value = value
    }

    fun onDealerChange(value: String) {
        _dealer.value = value
    }

    fun onCatalogNumberChange(value: String) {
        _catalogNumber.value = value
    }

    fun onCollectorNameChange(value: String) {
        _collectorName.value = value
    }

    fun onAcquisitionNotesChange(value: String) {
        _acquisitionNotes.value = value
    }

    // v3.1: Aggregate field updates
    fun onRockTypeChange(value: String) {
        _rockType.value = value
    }

    fun onTextureChange(value: String) {
        _texture.value = value
    }

    fun onDominantMineralsChange(value: String) {
        _dominantMinerals.value = value
    }

    fun onInterestingFeaturesChange(value: String) {
        _interestingFeatures.value = value
    }

    // v2.0: Component management
    fun onComponentsChange(components: List<MineralComponent>) {
        _components.value = components
    }

    private fun updateTagSuggestions(input: String) {
        if (input.isBlank()) {
            _tagSuggestions.value = emptyList()
            return
        }

        val lastTag = input.split(",").lastOrNull()?.trim() ?: ""

        if (lastTag.length < 2) {
            _tagSuggestions.value = emptyList()
            return
        }

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

    fun updateMineral(onSuccess: (String) -> Unit, photosDir: File) {
        viewModelScope.launch {
            // Validation
            if (_name.value.isBlank()) {
                _updateState.value = UpdateMineralState.Error("Mineral name is required")
                return@launch
            }

            if (_name.value.length < 2) {
                _updateState.value = UpdateMineralState.Error("Mineral name must be at least 2 characters")
                return@launch
            }

            // v2.0: Validate aggregates
            if (_mineralType.value == MineralType.AGGREGATE) {
                if (_components.value.size < 2) {
                    _updateState.value = UpdateMineralState.Error("Un agrégat doit avoir au moins 2 composants")
                    return@launch
                }

                // Check that all components have names
                if (_components.value.any { it.mineralName.isBlank() }) {
                    _updateState.value = UpdateMineralState.Error("Tous les composants doivent avoir un nom")
                    return@launch
                }

                // Only validate percentages if at least one is provided
                val componentsWithPercentage = _components.value.filter { it.percentage != null }
                if (componentsWithPercentage.isNotEmpty()) {
                    val totalPercentage = componentsWithPercentage.mapNotNull { it.percentage }.sum()
                    if (totalPercentage !in 95f..105f) {
                        _updateState.value = UpdateMineralState.Error("Les pourcentages doivent totaliser environ 100% (actuellement ${totalPercentage.toInt()}%)")
                        return@launch
                    }
                }
            }

            _updateState.value = UpdateMineralState.Saving

            try {
                val tagsList = _tags.value.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                // v3.1: Create or update Provenance object
                // Capture current state to prevent race conditions
                val currentOriginal = originalMineral.value

                val updatedProvenance = if (
                    _mineName.value.isNotBlank() ||
                    _dealer.value.isNotBlank() ||
                    _catalogNumber.value.isNotBlank() ||
                    _collectorName.value.isNotBlank() ||
                    _acquisitionNotes.value.isNotBlank() ||
                    currentOriginal?.provenance != null
                ) {
                    net.meshcore.mineralog.domain.model.Provenance(
                        id = currentOriginal?.provenance?.id ?: UUID.randomUUID().toString(),
                        mineralId = mineralId,
                        site = currentOriginal?.provenance?.site,
                        locality = currentOriginal?.provenance?.locality,
                        country = currentOriginal?.provenance?.country,
                        latitude = currentOriginal?.provenance?.latitude,
                        longitude = currentOriginal?.provenance?.longitude,
                        acquiredAt = currentOriginal?.provenance?.acquiredAt,
                        source = currentOriginal?.provenance?.source,
                        price = currentOriginal?.provenance?.price,
                        estimatedValue = currentOriginal?.provenance?.estimatedValue,
                        currency = currentOriginal?.provenance?.currency,
                        mineName = _mineName.value.trim().takeIf { it.isNotBlank() },
                        dealer = _dealer.value.trim().takeIf { it.isNotBlank() },
                        catalogNumber = _catalogNumber.value.trim().takeIf { it.isNotBlank() },
                        collectorName = _collectorName.value.trim().takeIf { it.isNotBlank() },
                        acquisitionNotes = _acquisitionNotes.value.trim().takeIf { it.isNotBlank() }
                    )
                } else {
                    null
                }

                val updatedMineral = Mineral(
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
                    // v3.1: Aggregate fields
                    rockType = _rockType.value.trim().takeIf { it.isNotBlank() },
                    texture = _texture.value.trim().takeIf { it.isNotBlank() },
                    dominantMinerals = _dominantMinerals.value.trim().takeIf { it.isNotBlank() },
                    interestingFeatures = _interestingFeatures.value.trim().takeIf { it.isNotBlank() },
                    tags = tagsList,
                    status = currentOriginal?.status ?: "incomplete",
                    statusType = currentOriginal?.statusType ?: "in_collection",
                    statusDetails = currentOriginal?.statusDetails,
                    qualityRating = currentOriginal?.qualityRating,
                    completeness = currentOriginal?.completeness ?: 0,
                    createdAt = currentOriginal?.createdAt ?: Instant.now(),
                    updatedAt = Instant.now(),
                    provenance = updatedProvenance,
                    storage = currentOriginal?.storage
                )

                mineralRepository.update(updatedMineral)

                // v2.0: Update components if this is an aggregate
                if (_mineralType.value == MineralType.AGGREGATE) {
                    (mineralRepository as MineralRepositoryImpl).updateAggregateComponents(
                        aggregateId = mineralId,
                        components = _components.value
                    )
                }

                // Handle photos
                // Delete removed photos
                val existingPhotoIds = _photos.value.filter { it.isExisting }.map { it.id }.toSet()
                val originalPhotoIds = currentOriginal?.photos?.map { it.id }?.toSet() ?: emptySet()
                val removedPhotoIds = originalPhotoIds - existingPhotoIds

                removedPhotoIds.forEach { photoId ->
                    mineralRepository.deletePhoto(photoId)
                }

                // Add new photos - BUGFIX: Actually copy the file from URI
                val newPhotos = _photos.value.filter { !it.isExisting && it.uri != null }
                newPhotos.forEach { photoItem ->
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
                            AppLogger.e("EditMineralViewModel", "Failed to copy photo: ${e.message}", e)
                        }
                    }
                }

                // Update captions for existing photos
                _photos.value.filter { it.isExisting }.forEach { photoItem ->
                    val originalPhoto = currentOriginal?.photos?.find { it.id == photoItem.id }
                    if (originalPhoto != null && originalPhoto.caption != photoItem.caption) {
                        val updatedPhoto = originalPhoto.copy(caption = photoItem.caption)
                        mineralRepository.insertPhoto(updatedPhoto)
                    }
                }

                _updateState.value = UpdateMineralState.Success(mineralId)
                onSuccess(mineralId)
            } catch (e: Exception) {
                _updateState.value = UpdateMineralState.Error(e.message ?: "Failed to update mineral")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateMineralState.Idle
    }
}

class EditMineralViewModelFactory(
    private val context: Context,
    private val mineralId: String,
    private val mineralRepository: MineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditMineralViewModel::class.java)) {
            return EditMineralViewModel(context, mineralId, mineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
