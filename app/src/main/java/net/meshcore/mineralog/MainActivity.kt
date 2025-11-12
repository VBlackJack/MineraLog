package net.meshcore.mineralog

import android.os.Bundle
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

        // Get deep link URI if present
        val deepLinkMineralId = intent?.data?.lastPathSegment

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
