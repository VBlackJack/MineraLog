package net.meshcore.mineralog

import android.os.Bundle
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
import net.meshcore.mineralog.ui.navigation.MineraLogNavHost
import net.meshcore.mineralog.ui.theme.MineraLogTheme
import java.util.UUID

/**
 * Main Activity for MineraLog application.
 * Handles deep links (mineralapp://mineral/{uuid}) and sets up Compose UI.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
