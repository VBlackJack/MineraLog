package net.meshcore.mineralog.ui.screens.detail

import android.content.Context
import android.net.Uri
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
import net.meshcore.mineralog.data.util.QrLabelPdfGenerator
import net.meshcore.mineralog.domain.model.Mineral

sealed class DeleteState {
    data object Idle : DeleteState()
    data object Deleting : DeleteState()
    data object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}

sealed class QrGenerationState {
    data object Idle : QrGenerationState()
    data object Generating : QrGenerationState()
    data class Success(val uri: Uri) : QrGenerationState()
    data class Error(val message: String) : QrGenerationState()
}

class MineralDetailViewModel(
    private val context: Context,
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

    private val _qrGenerationState = MutableStateFlow<QrGenerationState>(QrGenerationState.Idle)
    val qrGenerationState: StateFlow<QrGenerationState> = _qrGenerationState.asStateFlow()

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

    /**
     * Generate QR code label PDF for this mineral.
     */
    fun generateQrLabel(outputUri: Uri) {
        viewModelScope.launch {
            _qrGenerationState.value = QrGenerationState.Generating
            try {
                val currentMineral = mineral.value
                if (currentMineral == null) {
                    _qrGenerationState.value = QrGenerationState.Error("Mineral not loaded")
                    return@launch
                }

                // Generate PDF with QR label
                val generator = QrLabelPdfGenerator(context)
                val result = generator.generate(listOf(currentMineral), outputUri)

                if (result.isSuccess) {
                    _qrGenerationState.value = QrGenerationState.Success(outputUri)
                } else {
                    _qrGenerationState.value = QrGenerationState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to generate QR label"
                    )
                }
            } catch (e: Exception) {
                _qrGenerationState.value = QrGenerationState.Error(
                    e.message ?: "Unknown error generating QR label"
                )
            }
        }
    }

    fun resetQrGenerationState() {
        _qrGenerationState.value = QrGenerationState.Idle
    }
}

class MineralDetailViewModelFactory(
    private val context: Context,
    private val mineralId: String,
    private val mineralRepository: MineralRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MineralDetailViewModel::class.java)) {
            return MineralDetailViewModel(context, mineralId, mineralRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
