package net.meshcore.mineralog.ui.screens.settings

import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import net.meshcore.mineralog.data.repository.BackupRepository
import net.meshcore.mineralog.data.repository.CsvImportMode
import net.meshcore.mineralog.data.repository.ImportMode
import net.meshcore.mineralog.data.repository.ImportResult
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.data.repository.SettingsRepository
import net.meshcore.mineralog.data.sample.SampleDataGenerator
import net.meshcore.mineralog.ui.screens.identification.utils.ImageAnalyzer
import net.meshcore.mineralog.util.AppLogger
import java.io.File

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

sealed class CsvImportState {
    data object Idle : CsvImportState()
    data object Importing : CsvImportState()
    data class Success(val result: ImportResult) : CsvImportState()
    data class Error(val message: String) : CsvImportState()
}

sealed class SampleDataState {
    data object Idle : SampleDataState()
    data object Loading : SampleDataState()
    data class Success(val count: Int) : SampleDataState()
    data class Error(val message: String) : SampleDataState()
}

sealed class ReanalysisState {
    data object Idle : ReanalysisState()
    data class Processing(val progress: Int, val total: Int) : ReanalysisState()
    data class Success(val count: Int) : ReanalysisState()
    data class Error(val message: String) : ReanalysisState()
}

class SettingsViewModel(
    private val context: Context,
    private val mineralRepository: MineralRepository,
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _exportState = MutableStateFlow<BackupExportState>(BackupExportState.Idle)
    val exportState: StateFlow<BackupExportState> = _exportState.asStateFlow()

    private val _importState = MutableStateFlow<BackupImportState>(BackupImportState.Idle)
    val importState: StateFlow<BackupImportState> = _importState.asStateFlow()

    private val _csvImportState = MutableStateFlow<CsvImportState>(CsvImportState.Idle)
    val csvImportState: StateFlow<CsvImportState> = _csvImportState.asStateFlow()

    private val _sampleDataState = MutableStateFlow<SampleDataState>(SampleDataState.Idle)
    val sampleDataState: StateFlow<SampleDataState> = _sampleDataState.asStateFlow()

    private val _reanalysisState = MutableStateFlow<ReanalysisState>(ReanalysisState.Idle)
    val reanalysisState: StateFlow<ReanalysisState> = _reanalysisState.asStateFlow()

    val language: StateFlow<String> = settingsRepository.getLanguage()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "system"
        )

    val copyPhotosToInternal: StateFlow<Boolean> = settingsRepository.getCopyPhotosToInternalStorage()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val encryptByDefault: StateFlow<Boolean> = settingsRepository.getEncryptByDefault()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Update app language and apply locale immediately.
     * @param lang "system", "en", or "fr"
     */
    fun setLanguage(lang: String) {
        // Save preference first
        viewModelScope.launch {
            settingsRepository.setLanguage(lang)
        }

        // Apply locale using AppCompatDelegate immediately (on main thread)
        // This will automatically recreate the activity with the new locale
        val localeList = when (lang) {
            "system" -> LocaleListCompat.getEmptyLocaleList()
            else -> LocaleListCompat.forLanguageTags(lang)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun setCopyPhotos(copy: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCopyPhotosToInternalStorage(copy)
        }
    }

    fun setEncryptByDefault(encrypt: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEncryptByDefault(encrypt)
        }
    }

    /**
     * Export full backup to ZIP with optional encryption.
     * Uses CharArray for password to prevent it from lingering in memory.
     */
    fun exportBackup(uri: Uri, password: CharArray?) {
        viewModelScope.launch {
            _exportState.value = BackupExportState.Exporting

            try {
                val result = backupRepository.exportZip(uri, password)

                _exportState.value = result.fold(
                    onSuccess = { BackupExportState.Success },
                    onFailure = { BackupExportState.Error(it.message ?: "Export failed") }
                )
            } finally {
                // Clear password from memory
                password?.fill('\u0000')
            }
        }
    }

    /**
     * Import full backup from ZIP. If encrypted, caller must provide password.
     * Uses CharArray for password to prevent it from lingering in memory.
     */
    fun importBackup(uri: Uri, password: CharArray? = null) {
        viewModelScope.launch {
            _importState.value = BackupImportState.Importing

            try {
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
            } finally {
                // Clear password from memory
                password?.fill('\u0000')
            }
        }
    }

    fun resetExportState() {
        _exportState.value = BackupExportState.Idle
    }

    fun resetImportState() {
        _importState.value = BackupImportState.Idle
    }

    /**
     * Import minerals from CSV file.
     *
     * @param uri URI of the CSV file
     * @param columnMapping Optional manual column mapping (null = auto-detect)
     * @param mode Import mode (MERGE/REPLACE/SKIP_DUPLICATES)
     */
    fun importCsv(
        uri: Uri,
        columnMapping: Map<String, String>? = null,
        mode: CsvImportMode = CsvImportMode.MERGE
    ) {
        viewModelScope.launch {
            _csvImportState.value = CsvImportState.Importing

            val result = backupRepository.importCsv(uri, columnMapping, mode)

            _csvImportState.value = result.fold(
                onSuccess = { importResult ->
                    CsvImportState.Success(importResult)
                },
                onFailure = { error ->
                    CsvImportState.Error(error.message ?: "CSV import failed")
                }
            )
        }
    }

    fun resetCsvImportState() {
        _csvImportState.value = CsvImportState.Idle
    }

    /**
     * Load sample mineral data for testing purposes.
     * Generates 12 common minerals with realistic properties.
     */
    fun loadSampleData() {
        viewModelScope.launch {
            _sampleDataState.value = SampleDataState.Loading

            try {
                val generator = SampleDataGenerator(context, mineralRepository)
                val minerals = generator.generateSampleMinerals()
                _sampleDataState.value = SampleDataState.Success(minerals.size)
            } catch (e: Exception) {
                _sampleDataState.value = SampleDataState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetSampleDataState() {
        _sampleDataState.value = SampleDataState.Idle
    }

    /**
     * Reanalyze dominant colors for all minerals that have photos but no dominantColor.
     * v3.2.0: Batch maintenance function for existing collections.
     */
    fun reanalyzeMineralColors() {
        viewModelScope.launch(Dispatchers.Default) {
            _reanalysisState.value = ReanalysisState.Processing(0, 0)

            try {
                // Get all minerals
                val allMinerals = mineralRepository.getAll()

                // Filter minerals that have photos but no dominant color
                val candidateMinerals = allMinerals.filter { mineral ->
                    mineral.photos.isNotEmpty() && mineral.dominantColor.isNullOrBlank()
                }

                val totalCount = candidateMinerals.size
                var processedCount = 0
                var updatedCount = 0

                AppLogger.d("ColorReanalysis", "Found $totalCount minerals to reanalyze")

                // Process each candidate
                candidateMinerals.forEach { mineral ->
                    try {
                        // Get first photo
                        val firstPhoto = mineral.photos.firstOrNull()
                        if (firstPhoto != null) {
                            // Load photo bitmap
                            val photoFile = File(context.filesDir, "photos/${firstPhoto.fileName}")
                            if (photoFile.exists()) {
                                // Force software bitmap (ARGB_8888) to allow CPU pixel access
                                val options = BitmapFactory.Options().apply {
                                    inPreferredConfig = Bitmap.Config.ARGB_8888
                                }
                                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath, options)
                                if (bitmap != null) {
                                    // Analyze color
                                    val detectedColor = ImageAnalyzer.detectDominantColorName(bitmap)
                                    bitmap.recycle()

                                    if (detectedColor != null) {
                                        // Update mineral with detected color
                                        val updatedMineral = mineral.copy(dominantColor = detectedColor)
                                        mineralRepository.update(updatedMineral)
                                        updatedCount++

                                        AppLogger.d("ColorReanalysis", "Updated ${mineral.name} with color: $detectedColor")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Log error but continue processing
                        AppLogger.e("ColorReanalysis", "Failed to process mineral ${mineral.name}", e)
                    }

                    processedCount++
                    _reanalysisState.value = ReanalysisState.Processing(processedCount, totalCount)

                    // Small delay to prevent UI freezing
                    delay(50)
                }

                AppLogger.i("ColorReanalysis", "Completed: $updatedCount/$totalCount minerals updated")
                _reanalysisState.value = ReanalysisState.Success(updatedCount)

            } catch (e: Exception) {
                AppLogger.e("ColorReanalysis", "Batch reanalysis failed", e)
                _reanalysisState.value = ReanalysisState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetReanalysisState() {
        _reanalysisState.value = ReanalysisState.Idle
    }
}

class SettingsViewModelFactory(
    private val context: Context,
    private val mineralRepository: MineralRepository,
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(context, mineralRepository, settingsRepository, backupRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
