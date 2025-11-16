package net.meshcore.mineralog.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Information icon button with tooltip.
 * Displays a "?" icon that shows a helpful tooltip when clicked.
 *
 * @param tooltipText The text to display in the tooltip.
 * @param modifier Modifier for the icon button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoIconButton(
    tooltipText: String,
    modifier: Modifier = Modifier
) {
    var showTooltip by remember { mutableStateOf(false) }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(tooltipText)
            }
        },
        state = rememberTooltipState(isPersistent = true)
    ) {
        IconButton(
            onClick = { showTooltip = !showTooltip },
            modifier = modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Information: $tooltipText",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
