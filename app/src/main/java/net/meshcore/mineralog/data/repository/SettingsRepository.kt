package net.meshcore.mineralog.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
}

class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COPY_PHOTOS = booleanPreferencesKey("copy_photos_to_internal")
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
}
