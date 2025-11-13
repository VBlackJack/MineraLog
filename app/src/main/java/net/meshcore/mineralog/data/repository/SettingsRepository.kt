package net.meshcore.mineralog.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

interface SettingsRepository {
    fun getLanguage(): Flow<String>
    suspend fun setLanguage(language: String)
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(mode: String)
    fun getCopyPhotosToInternalStorage(): Flow<Boolean>
    suspend fun setCopyPhotosToInternalStorage(copy: Boolean)
    fun getCsvExportWarningShown(): Flow<Boolean>
    suspend fun setCsvExportWarningShown(shown: Boolean)

    // Draft auto-save functionality
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

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COPY_PHOTOS = booleanPreferencesKey("copy_photos_to_internal")
        val CSV_EXPORT_WARNING_SHOWN = booleanPreferencesKey("csv_export_warning_shown")

        // Draft keys
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

    override fun getLanguage(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.LANGUAGE] ?: "en"
        }
    }

    override suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LANGUAGE] = language
        }
    }

    override fun getThemeMode(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.THEME_MODE] ?: "system"
        }
    }

    override suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode
        }
    }

    override fun getCopyPhotosToInternalStorage(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.COPY_PHOTOS] ?: true
        }
    }

    override suspend fun setCopyPhotosToInternalStorage(copy: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.COPY_PHOTOS] = copy
        }
    }

    override fun getCsvExportWarningShown(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.CSV_EXPORT_WARNING_SHOWN] ?: false
        }
    }

    override suspend fun setCsvExportWarningShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.CSV_EXPORT_WARNING_SHOWN] = shown
        }
    }

    // Draft auto-save implementation
    override fun getDraftName(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_NAME] ?: "" }

    override suspend fun setDraftName(name: String) {
        context.dataStore.edit { it[Keys.DRAFT_NAME] = name }
    }

    override fun getDraftGroup(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_GROUP] ?: "" }

    override suspend fun setDraftGroup(group: String) {
        context.dataStore.edit { it[Keys.DRAFT_GROUP] = group }
    }

    override fun getDraftFormula(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_FORMULA] ?: "" }

    override suspend fun setDraftFormula(formula: String) {
        context.dataStore.edit { it[Keys.DRAFT_FORMULA] = formula }
    }

    override fun getDraftNotes(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_NOTES] ?: "" }

    override suspend fun setDraftNotes(notes: String) {
        context.dataStore.edit { it[Keys.DRAFT_NOTES] = notes }
    }

    override fun getDraftDiaphaneity(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_DIAPHANEITY] ?: "" }

    override suspend fun setDraftDiaphaneity(diaphaneity: String) {
        context.dataStore.edit { it[Keys.DRAFT_DIAPHANEITY] = diaphaneity }
    }

    override fun getDraftCleavage(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_CLEAVAGE] ?: "" }

    override suspend fun setDraftCleavage(cleavage: String) {
        context.dataStore.edit { it[Keys.DRAFT_CLEAVAGE] = cleavage }
    }

    override fun getDraftFracture(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_FRACTURE] ?: "" }

    override suspend fun setDraftFracture(fracture: String) {
        context.dataStore.edit { it[Keys.DRAFT_FRACTURE] = fracture }
    }

    override fun getDraftLuster(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_LUSTER] ?: "" }

    override suspend fun setDraftLuster(luster: String) {
        context.dataStore.edit { it[Keys.DRAFT_LUSTER] = luster }
    }

    override fun getDraftStreak(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_STREAK] ?: "" }

    override suspend fun setDraftStreak(streak: String) {
        context.dataStore.edit { it[Keys.DRAFT_STREAK] = streak }
    }

    override fun getDraftHabit(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_HABIT] ?: "" }

    override suspend fun setDraftHabit(habit: String) {
        context.dataStore.edit { it[Keys.DRAFT_HABIT] = habit }
    }

    override fun getDraftCrystalSystem(): Flow<String> =
        context.dataStore.data.map { it[Keys.DRAFT_CRYSTAL_SYSTEM] ?: "" }

    override suspend fun setDraftCrystalSystem(crystalSystem: String) {
        context.dataStore.edit { it[Keys.DRAFT_CRYSTAL_SYSTEM] = crystalSystem }
    }

    override fun getDraftTimestamp(): Flow<Long> =
        context.dataStore.data.map { it[Keys.DRAFT_TIMESTAMP] ?: 0L }

    override suspend fun setDraftTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.DRAFT_TIMESTAMP] = timestamp }
    }

    override suspend fun clearDraft() {
        context.dataStore.edit { preferences ->
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
    }
}
