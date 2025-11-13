package net.meshcore.mineralog.ui.screens.edit

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
import net.meshcore.mineralog.domain.model.Mineral
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

    private var originalMineral: Mineral? = null

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
                mineralRepository.getMineralFlow(mineralId).collect { mineral ->
                    if (mineral != null) {
                        originalMineral = mineral
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

            _updateState.value = UpdateMineralState.Saving

            try {
                val tagsList = _tags.value.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

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
                    tags = tagsList,
                    status = originalMineral?.status ?: "incomplete",
                    statusType = originalMineral?.statusType ?: "in_collection",
                    statusDetails = originalMineral?.statusDetails,
                    qualityRating = originalMineral?.qualityRating,
                    completeness = originalMineral?.completeness ?: 0,
                    createdAt = originalMineral?.createdAt ?: Instant.now(),
                    updatedAt = Instant.now(),
                    provenance = originalMineral?.provenance,
                    storage = originalMineral?.storage
                )

                mineralRepository.update(updatedMineral)

                // Handle photos
                // Delete removed photos
                val existingPhotoIds = _photos.value.filter { it.isExisting }.map { it.id }.toSet()
                val originalPhotoIds = originalMineral?.photos?.map { it.id }?.toSet() ?: emptySet()
                val removedPhotoIds = originalPhotoIds - existingPhotoIds

                removedPhotoIds.forEach { photoId ->
                    mineralRepository.deletePhoto(photoId)
                }

                // Add new photos
                val newPhotos = _photos.value.filter { !it.isExisting && it.uri != null }
                newPhotos.forEach { photoItem ->
                    // Copy file to app's photos directory
                    val photoFile = File(photosDir, photoItem.fileName)
                    photoItem.uri?.let { uri ->
                        // In a real app, you would copy the file here
                        // For now, we'll just create the Photo entity
                        val photo = Photo(
                            id = photoItem.id,
                            mineralId = mineralId,
                            type = photoItem.type,
                            caption = photoItem.caption,
                            takenAt = Instant.now(),
                            fileName = photoItem.fileName
                        )
                        mineralRepository.insertPhoto(photo)
                    }
                }

                // Update captions for existing photos
                _photos.value.filter { it.isExisting }.forEach { photoItem ->
                    val originalPhoto = originalMineral?.photos?.find { it.id == photoItem.id }
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
    private val mineralId: String,
    private val mineralRepository: MineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditMineralViewModel::class.java)) {
            return EditMineralViewModel(mineralId, mineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
