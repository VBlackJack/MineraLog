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
import net.meshcore.mineralog.data.repository.MineralRepositoryImpl
import net.meshcore.mineralog.data.repository.getMineralType
import net.meshcore.mineralog.data.repository.getAggregateComponentsFlow
import net.meshcore.mineralog.data.util.QrLabelPdfGenerator
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.MineralComponent
import net.meshcore.mineralog.domain.model.MineralType
import net.meshcore.mineralog.domain.provider.ResourceProvider
import net.meshcore.mineralog.R

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
    private val mineralRepository: MineralRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    val mineral: StateFlow<Mineral?> = mineralRepository.getByIdFlow(mineralId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // v2.0: Mineral type (SIMPLE or AGGREGATE)
    private val _mineralType = MutableStateFlow(MineralType.SIMPLE)
    val mineralType: StateFlow<MineralType> = _mineralType.asStateFlow()

    // v2.0: Components for aggregate minerals (as Flow)
    val components: StateFlow<List<MineralComponent>> =
        (mineralRepository as MineralRepositoryImpl).getAggregateComponentsFlow(mineralId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    private val _qrGenerationState = MutableStateFlow<QrGenerationState>(QrGenerationState.Idle)
    val qrGenerationState: StateFlow<QrGenerationState> = _qrGenerationState.asStateFlow()

    init {
        // v2.0: Load mineral type
        viewModelScope.launch {
            _mineralType.value = (mineralRepository as MineralRepositoryImpl).getMineralType(mineralId)
        }
    }

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
                _deleteState.value = DeleteState.Error(e.message ?: resourceProvider.getString(R.string.error_failed_to_delete_mineral))
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
                    _qrGenerationState.value = QrGenerationState.Error(resourceProvider.getString(R.string.error_mineral_not_loaded))
                    return@launch
                }

                // Generate PDF with QR label
                val generator = QrLabelPdfGenerator(context)
                val result = generator.generate(listOf(currentMineral), outputUri)

                if (result.isSuccess) {
                    _qrGenerationState.value = QrGenerationState.Success(outputUri)
                } else {
                    _qrGenerationState.value = QrGenerationState.Error(
                        result.exceptionOrNull()?.message ?: resourceProvider.getString(R.string.error_failed_to_generate_qr)
                    )
                }
            } catch (e: Exception) {
                _qrGenerationState.value = QrGenerationState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_unknown_generating_qr)
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
    private val mineralRepository: MineralRepository,
    private val resourceProvider: ResourceProvider
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MineralDetailViewModel::class.java)) {
            return MineralDetailViewModel(context, mineralId, mineralRepository, resourceProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
