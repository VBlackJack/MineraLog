package net.meshcore.mineralog

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.repository.*
import net.meshcore.mineralog.util.AppLogger

/**
 * Main Application class for MineraLog.
 * Initializes Tink crypto, Room database, repositories, and WorkManager.
 */
class MineraLogApplication : Application(), Configuration.Provider {

    // Database
    val database: MineraLogDatabase by lazy {
        MineraLogDatabase.getDatabase(this)
    }

    // Repositories
    val mineralRepository: MineralRepository by lazy {
        MineralRepositoryImpl(
            database = database,
            mineralDao = database.mineralDaoComposite(),
            provenanceDao = database.provenanceDao(),
            storageDao = database.storageDao(),
            photoDao = database.photoDao()
        )
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(
            context = this,
            database = database
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(this)
    }

    // v1.2.0 Statistics Repository
    val statisticsRepository: StatisticsRepository by lazy {
        StatisticsRepositoryImpl(
            mineralDao = database.mineralDaoComposite()
        )
    }

    // v1.2.0 Filter Preset Repository
    val filterPresetRepository: FilterPresetRepository by lazy {
        FilterPresetRepositoryImpl(
            filterPresetDao = database.filterPresetDao()
        )
    }

    // v3.0.0 Reference Mineral Repository
    val referenceMineralRepository: ReferenceMineralRepository by lazy {
        ReferenceMineralRepositoryImpl(
            referenceMineralDao = database.referenceMineralDao(),
            context = this
        )
    }

    // Tink AEAD for local encryption (app-level key)
    val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(this, "mineralog_keyset", "mineralog_pref")
            .withKeyTemplate(com.google.crypto.tink.aead.AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://mineralog_master_key")
            .build()
            .keysetHandle
        keysetHandle.getPrimitive(Aead::class.java)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize Tink for encryption
        try {
            AeadConfig.register()
        } catch (e: Exception) {
            AppLogger.e("MineraLogApp", "Failed to initialize Tink", e)
        }

        // Initialize WorkManager
        WorkManager.initialize(this, workManagerConfiguration)
    }
}
