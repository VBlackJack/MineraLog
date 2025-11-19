package net.meshcore.mineralog.ui.screens.camera

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.data.local.entity.PhotoType
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.domain.model.Photo
import net.meshcore.mineralog.util.AppLogger

sealed interface PhotoSaveState {
    data object Idle : PhotoSaveState
    data object Saving : PhotoSaveState
    data object Success : PhotoSaveState
    data class Error(val message: String) : PhotoSaveState
}

class CameraViewModel(
    application: Application,
    private val mineralRepository: MineralRepository
) : AndroidViewModel(application) {

    private val _saveState = MutableStateFlow<PhotoSaveState>(PhotoSaveState.Idle)
    val saveState: StateFlow<PhotoSaveState> = _saveState.asStateFlow()

    fun saveCapturedPhoto(mineralId: String, sourceUri: Uri, photoType: PhotoType) {
        if (_saveState.value is PhotoSaveState.Saving) return

        _saveState.value = PhotoSaveState.Saving
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val destinationFile = prepareDestinationFile()
                copyPhotoToDestination(sourceUri, destinationFile)

                val photo = Photo(
                    id = UUID.randomUUID().toString(),
                    mineralId = mineralId,
                    type = photoType.name,
                    caption = null,
                    takenAt = Instant.now(),
                    fileName = destinationFile.name
                )
                mineralRepository.insertPhoto(photo)

                _saveState.value = PhotoSaveState.Success
            } catch (ioException: IOException) {
                AppLogger.e("CameraViewModel", "I/O error saving photo", ioException)
                _saveState.value = PhotoSaveState.Error(
                    ioException.message ?: "Unable to save captured photo"
                )
            } catch (exception: Exception) {
                AppLogger.e("CameraViewModel", "Unexpected error saving photo", exception)
                _saveState.value = PhotoSaveState.Error("Unexpected error while saving photo")
            }
        }
    }

    private fun prepareDestinationFile(): File {
        val photosDir = File(getApplication<Application>().filesDir, "photos")
        if (!photosDir.exists() && !photosDir.mkdirs()) {
            throw IOException("Failed to create photos directory")
        }
        val fileName = "photo_${System.currentTimeMillis()}.jpg"
        return File(photosDir, fileName)
    }

    private fun copyPhotoToDestination(sourceUri: Uri, destinationFile: File) {
        val resolver = getApplication<Application>().contentResolver
        resolver.openInputStream(sourceUri)?.use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Unable to open input stream for $sourceUri")
    }

    fun consumeSaveState() {
        _saveState.value = PhotoSaveState.Idle
    }

    companion object {
        fun provideFactory(application: MineraLogApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return CameraViewModel(application, application.mineralRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
