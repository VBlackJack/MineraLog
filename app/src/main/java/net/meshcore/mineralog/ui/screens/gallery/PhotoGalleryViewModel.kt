package net.meshcore.mineralog.ui.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.domain.model.Photo

/**
 * ViewModel for photo gallery screen.
 * Manages photo list and deletion.
 */
class PhotoGalleryViewModel(
    private val mineralId: String,
    private val mineralRepository: MineralRepository
) : ViewModel() {

    val photos: StateFlow<List<Photo>> = mineralRepository
        .getPhotosFlow(mineralId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            mineralRepository.deletePhoto(photoId)
        }
    }
}

class PhotoGalleryViewModelFactory(
    private val mineralId: String,
    private val mineralRepository: MineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoGalleryViewModel::class.java)) {
            return PhotoGalleryViewModel(mineralId, mineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
