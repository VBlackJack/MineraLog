package com.argumentor.ui.backup

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.argumentor.R
import com.argumentor.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


data class BackupUiState(
    val isWorking: Boolean = false,
    @StringRes val messageRes: Int? = null,
    val messageText: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState

    fun exportDatabase(directory: File) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isWorking = true)
            val file = backupRepository.exportDatabaseToJson(directory)
            _uiState.value = BackupUiState(messageText = file.absolutePath)
        }
    }

    fun importDatabase(json: String) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isWorking = true)
            backupRepository.importDatabaseFromJson(json)
            _uiState.value = BackupUiState(messageRes = R.string.import_success)
        }
    }

    fun exportTopicMarkdown(topicId: Long, onReady: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isWorking = true)
            val markdown = backupRepository.exportTopicToMarkdown(topicId)
            _uiState.value = BackupUiState(isWorking = false, messageRes = R.string.markdown_ready)
            onReady(markdown)
        }
    }

    fun exportTopicPdf(topicId: Long, file: File, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = BackupUiState(isWorking = true)
            val pdf = backupRepository.exportTopicToPdf(topicId, file)
            _uiState.value = BackupUiState(messageText = pdf.absolutePath)
            onReady(pdf)
        }
    }
}
