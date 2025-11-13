package net.meshcore.mineralog.ui.screens.settings

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
import net.meshcore.mineralog.data.repository.BackupRepository
import net.meshcore.mineralog.data.repository.ImportMode
import net.meshcore.mineralog.data.repository.SettingsRepository

sealed class BackupExportState {
    data object Idle : BackupExportState()
    data object Exporting : BackupExportState()
    data object Success : BackupExportState()
    data class Error(val message: String) : BackupExportState()
}

sealed class BackupImportState {
    data object Idle : BackupImportState()
    data object Importing : BackupImportState()
    data class Success(val imported: Int) : BackupImportState()
    data class Error(val message: String) : BackupImportState()
    data class PasswordRequired(val uri: Uri) : BackupImportState()
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _exportState = MutableStateFlow<BackupExportState>(BackupExportState.Idle)
    val exportState: StateFlow<BackupExportState> = _exportState.asStateFlow()

    private val _importState = MutableStateFlow<BackupImportState>(BackupImportState.Idle)
    val importState: StateFlow<BackupImportState> = _importState.asStateFlow()

    val language: StateFlow<String> = settingsRepository.getLanguage()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en"
        )

    val copyPhotosToInternal: StateFlow<Boolean> = settingsRepository.getCopyPhotosToInternalStorage()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(lang)
        }
    }

    fun setCopyPhotos(copy: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCopyPhotosToInternalStorage(copy)
        }
    }

    /**
     * Export full backup to ZIP with optional encryption.
     */
    fun exportBackup(uri: Uri, password: String?) {
        viewModelScope.launch {
            _exportState.value = BackupExportState.Exporting

            val result = backupRepository.exportZip(uri, password)

            _exportState.value = result.fold(
                onSuccess = { BackupExportState.Success },
                onFailure = { BackupExportState.Error(it.message ?: "Export failed") }
            )
        }
    }

    /**
     * Import full backup from ZIP. If encrypted, caller must provide password.
     */
    fun importBackup(uri: Uri, password: String? = null) {
        viewModelScope.launch {
            _importState.value = BackupImportState.Importing

            val result = backupRepository.importZip(uri, password, ImportMode.REPLACE)

            _importState.value = result.fold(
                onSuccess = { importResult ->
                    BackupImportState.Success(importResult.imported)
                },
                onFailure = { error ->
                    // Check if this is an encrypted backup that needs a password
                    if (error.message?.contains("encrypted", ignoreCase = true) == true && password == null) {
                        BackupImportState.PasswordRequired(uri)
                    } else {
                        BackupImportState.Error(error.message ?: "Import failed")
                    }
                }
            )
        }
    }

    fun resetExportState() {
        _exportState.value = BackupExportState.Idle
    }

    fun resetImportState() {
        _importState.value = BackupImportState.Idle
    }
}

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(settingsRepository, backupRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
