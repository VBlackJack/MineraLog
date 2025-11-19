package net.meshcore.mineralog

import android.app.Application
import com.google.crypto.tink.aead.AeadConfig

/**
 * Test-only Application class for unit tests.
 *
 * Prevents WorkManager initialization which causes "already initialized" errors
 * in Robolectric tests. Skips Tink and WorkManager initialization to avoid
 * Android Keystore dependencies.
 *
 * Usage: Add @Config(application = TestMineraLogApplication::class) to test classes.
 */
class TestMineraLogApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Tink config (without Android Keystore dependency)
        try {
            AeadConfig.register()
        } catch (e: Exception) {
            // Ignore Tink initialization errors in tests
        }

        // NOTE: WorkManager.initialize() is intentionally skipped for tests
        // NOTE: Database and repositories are not initialized - tests create them as needed
    }
}
