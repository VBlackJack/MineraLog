package net.meshcore.mineralog.data.repository

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore by preferencesDataStore(name = "settings")

interface SettingsRepository {
    fun getLanguage(): Flow<String>
    suspend fun setLanguage(language: String)
    fun getTheme(): Flow<String>
    suspend fun setTheme(theme: String)
    fun isBiometricEnabled(): Flow<Boolean>
    suspend fun setBiometricEnabled(enabled: Boolean)
    fun getCopyPhotosToInternalStorage(): Flow<Boolean>
    suspend fun setCopyPhotosToInternalStorage(copy: Boolean)
    fun getCsvExportWarningShown(): Flow<Boolean>
    suspend fun setCsvExportWarningShown(shown: Boolean)
    fun getEncryptByDefault(): Flow<Boolean>
    suspend fun setEncryptByDefault(encrypt: Boolean)

    fun getDraftName(): Flow<String>
    suspend fun setDraftName(name: String)
    fun getDraftGroup(): Flow<String>
    suspend fun setDraftGroup(group: String)
    fun getDraftFormula(): Flow<String>
    suspend fun setDraftFormula(formula: String)
    fun getDraftNotes(): Flow<String>
    suspend fun setDraftNotes(notes: String)
    fun getDraftDiaphaneity(): Flow<String>
    suspend fun setDraftDiaphaneity(diaphaneity: String)
    fun getDraftCleavage(): Flow<String>
    suspend fun setDraftCleavage(cleavage: String)
    fun getDraftFracture(): Flow<String>
    suspend fun setDraftFracture(fracture: String)
    fun getDraftLuster(): Flow<String>
    suspend fun setDraftLuster(luster: String)
    fun getDraftStreak(): Flow<String>
    suspend fun setDraftStreak(streak: String)
    fun getDraftHabit(): Flow<String>
    suspend fun setDraftHabit(habit: String)
    fun getDraftCrystalSystem(): Flow<String>
    suspend fun setDraftCrystalSystem(crystalSystem: String)
    fun getDraftTimestamp(): Flow<Long>
    suspend fun setDraftTimestamp(timestamp: Long)
    suspend fun clearDraft()
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COPY_PHOTOS = booleanPreferencesKey("copy_photos_to_internal")
        val CSV_EXPORT_WARNING_SHOWN = booleanPreferencesKey("csv_export_warning_shown")
        val ENCRYPT_BY_DEFAULT = booleanPreferencesKey("encrypt_by_default")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")

        val DRAFT_NAME = stringPreferencesKey("draft_name")
        val DRAFT_GROUP = stringPreferencesKey("draft_group")
        val DRAFT_FORMULA = stringPreferencesKey("draft_formula")
        val DRAFT_NOTES = stringPreferencesKey("draft_notes")
        val DRAFT_DIAPHANEITY = stringPreferencesKey("draft_diaphaneity")
        val DRAFT_CLEAVAGE = stringPreferencesKey("draft_cleavage")
        val DRAFT_FRACTURE = stringPreferencesKey("draft_fracture")
        val DRAFT_LUSTER = stringPreferencesKey("draft_luster")
        val DRAFT_STREAK = stringPreferencesKey("draft_streak")
        val DRAFT_HABIT = stringPreferencesKey("draft_habit")
        val DRAFT_CRYSTAL_SYSTEM = stringPreferencesKey("draft_crystal_system")
        val DRAFT_TIMESTAMP = longPreferencesKey("draft_timestamp")
    }

    private val preferencesFlow: Flow<Preferences>
        get() = context.dataStore.data.catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    private suspend fun editPreferences(block: (MutablePreferences) -> Unit) {
        withContext(ioDispatcher) {
            context.dataStore.edit(block)
        }
    }

    override fun getLanguage(): Flow<String> =
        preferencesFlow.map { it[Keys.LANGUAGE] ?: DEFAULT_LANGUAGE }

    override suspend fun setLanguage(language: String) =
        editPreferences { it[Keys.LANGUAGE] = language }

    override fun getTheme(): Flow<String> =
        preferencesFlow.map { (it[Keys.THEME_MODE] ?: DEFAULT_THEME).uppercase(Locale.US) }

    override suspend fun setTheme(theme: String) =
        editPreferences { it[Keys.THEME_MODE] = theme.uppercase(Locale.US) }

    override fun isBiometricEnabled(): Flow<Boolean> =
        preferencesFlow.map { it[Keys.BIOMETRIC_ENABLED] ?: false }

    override suspend fun setBiometricEnabled(enabled: Boolean) =
        editPreferences { it[Keys.BIOMETRIC_ENABLED] = enabled }

    override fun getCopyPhotosToInternalStorage(): Flow<Boolean> =
        preferencesFlow.map { it[Keys.COPY_PHOTOS] ?: true }

    override suspend fun setCopyPhotosToInternalStorage(copy: Boolean) =
        editPreferences { it[Keys.COPY_PHOTOS] = copy }

    override fun getCsvExportWarningShown(): Flow<Boolean> =
        preferencesFlow.map { it[Keys.CSV_EXPORT_WARNING_SHOWN] ?: false }

    override suspend fun setCsvExportWarningShown(shown: Boolean) =
        editPreferences { it[Keys.CSV_EXPORT_WARNING_SHOWN] = shown }

    override fun getEncryptByDefault(): Flow<Boolean> =
        preferencesFlow.map { it[Keys.ENCRYPT_BY_DEFAULT] ?: false }

    override suspend fun setEncryptByDefault(encrypt: Boolean) =
        editPreferences { it[Keys.ENCRYPT_BY_DEFAULT] = encrypt }

    override fun getDraftName(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_NAME] ?: "" }

    override suspend fun setDraftName(name: String) =
        editPreferences { it[Keys.DRAFT_NAME] = name }

    override fun getDraftGroup(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_GROUP] ?: "" }

    override suspend fun setDraftGroup(group: String) =
        editPreferences { it[Keys.DRAFT_GROUP] = group }

    override fun getDraftFormula(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_FORMULA] ?: "" }

    override suspend fun setDraftFormula(formula: String) =
        editPreferences { it[Keys.DRAFT_FORMULA] = formula }

    override fun getDraftNotes(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_NOTES] ?: "" }

    override suspend fun setDraftNotes(notes: String) =
        editPreferences { it[Keys.DRAFT_NOTES] = notes }

    override fun getDraftDiaphaneity(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_DIAPHANEITY] ?: "" }

    override suspend fun setDraftDiaphaneity(diaphaneity: String) =
        editPreferences { it[Keys.DRAFT_DIAPHANEITY] = diaphaneity }

    override fun getDraftCleavage(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_CLEAVAGE] ?: "" }

    override suspend fun setDraftCleavage(cleavage: String) =
        editPreferences { it[Keys.DRAFT_CLEAVAGE] = cleavage }

    override fun getDraftFracture(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_FRACTURE] ?: "" }

    override suspend fun setDraftFracture(fracture: String) =
        editPreferences { it[Keys.DRAFT_FRACTURE] = fracture }

    override fun getDraftLuster(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_LUSTER] ?: "" }

    override suspend fun setDraftLuster(luster: String) =
        editPreferences { it[Keys.DRAFT_LUSTER] = luster }

    override fun getDraftStreak(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_STREAK] ?: "" }

    override suspend fun setDraftStreak(streak: String) =
        editPreferences { it[Keys.DRAFT_STREAK] = streak }

    override fun getDraftHabit(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_HABIT] ?: "" }

    override suspend fun setDraftHabit(habit: String) =
        editPreferences { it[Keys.DRAFT_HABIT] = habit }

    override fun getDraftCrystalSystem(): Flow<String> =
        preferencesFlow.map { it[Keys.DRAFT_CRYSTAL_SYSTEM] ?: "" }

    override suspend fun setDraftCrystalSystem(crystalSystem: String) =
        editPreferences { it[Keys.DRAFT_CRYSTAL_SYSTEM] = crystalSystem }

    override fun getDraftTimestamp(): Flow<Long> =
        preferencesFlow.map { it[Keys.DRAFT_TIMESTAMP] ?: 0L }

    override suspend fun setDraftTimestamp(timestamp: Long) =
        editPreferences { it[Keys.DRAFT_TIMESTAMP] = timestamp }

    override suspend fun clearDraft() = editPreferences { preferences ->
        preferences.remove(Keys.DRAFT_NAME)
        preferences.remove(Keys.DRAFT_GROUP)
        preferences.remove(Keys.DRAFT_FORMULA)
        preferences.remove(Keys.DRAFT_NOTES)
        preferences.remove(Keys.DRAFT_DIAPHANEITY)
        preferences.remove(Keys.DRAFT_CLEAVAGE)
        preferences.remove(Keys.DRAFT_FRACTURE)
        preferences.remove(Keys.DRAFT_LUSTER)
        preferences.remove(Keys.DRAFT_STREAK)
        preferences.remove(Keys.DRAFT_HABIT)
        preferences.remove(Keys.DRAFT_CRYSTAL_SYSTEM)
        preferences.remove(Keys.DRAFT_TIMESTAMP)
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "system"  // "system", "en", "fr"
        private const val DEFAULT_THEME = "SYSTEM"
    }
}
