package net.meshcore.mineralog

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.meshcore.mineralog.ui.navigation.MineraLogNavHost
import net.meshcore.mineralog.ui.theme.MineraLogTheme
import java.util.Locale
import java.util.UUID

/**
 * Main Activity for MineraLog application.
 * Handles deep links (mineralapp://mineral/{uuid}) and sets up Compose UI.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved language preference
        applyLanguagePreference()

        // Enable edge-to-edge display
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Get deep link URI if present, validate UUID to prevent injection attacks
        val deepLinkMineralId = intent?.data?.lastPathSegment?.let { id ->
            try {
                // Validate that the ID is a valid UUID
                UUID.fromString(id)
                id // Return the valid ID
            } catch (e: IllegalArgumentException) {
                // Log security event and ignore invalid deep link
                Log.w("MainActivity", "Invalid deep link UUID rejected: $id", e)
                null
            }
        }

        setContent {
            MineraLogTheme {
                MineraLogApp(
                    deepLinkMineralId = deepLinkMineralId
                )
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // Use SharedPreferences for synchronous language access
        val prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language_sync", "en") ?: "en"

        val locale = Locale(language)
        val config = Configuration(newBase.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        Locale.setDefault(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    private fun applyLanguagePreference() {
        // Language is now applied in attachBaseContext
    }
}

@Composable
fun MineraLogApp(
    deepLinkMineralId: String? = null
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        MineraLogNavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            deepLinkMineralId = deepLinkMineralId
        )
    }
}
