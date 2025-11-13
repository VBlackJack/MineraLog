package net.meshcore.mineralog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Text field with inline help tooltip for technical/complex fields.
 * Provides accessibility-friendly explanations for mineral properties.
 *
 * @param value Current field value
 * @param onValueChange Callback when value changes
 * @param label Field label
 * @param tooltipText Explanation text shown in tooltip
 * @param modifier Modifier for the field
 * @param singleLine Whether field is single line (default true)
 * @param placeholder Optional placeholder text
 * @param keyboardOptions Keyboard options for IME actions
 * @param keyboardActions Keyboard actions for IME submit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tooltipText: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var tooltipVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            trailingIcon = {
                IconButton(
                    onClick = { tooltipVisible = !tooltipVisible },
                    modifier = Modifier.semantics {
                        contentDescription = "Information about $label: $tooltipText"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Help for $label",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )

        // Tooltip card (shown when info icon clicked)
        if (tooltipVisible) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = tooltipText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

/**
 * Technical property explanations for mineral fields.
 */
object MineralFieldTooltips {
    const val DIAPHANEITY = "Transparency level: transparent (light passes through), " +
            "translucent (light passes through but not clearly), or opaque (no light passes through)"

    const val CLEAVAGE = "The tendency of a mineral to break along flat planes. " +
            "Examples: perfect (mica), good (feldspar), poor (quartz), none (olivine)"

    const val FRACTURE = "How a mineral breaks when not along cleavage planes. " +
            "Types: conchoidal (smooth curves like glass), uneven, splintery, hackly (jagged)"

    const val LUSTER = "How the surface reflects light. " +
            "Types: metallic (shiny like metal), vitreous (glassy), pearly, silky, resinous, greasy, dull"

    const val STREAK = "The color of the mineral when powdered (may differ from surface color). " +
            "Determined by rubbing mineral on unglazed porcelain plate"

    const val HABIT = "The general shape or form of the crystal. " +
            "Examples: prismatic (elongated), tabular (flat), cubic, massive (no regular shape), fibrous"

    const val SPECIFIC_GRAVITY = "Density relative to water (water = 1.0). " +
            "Most minerals: 2.5-4.5. Heavy minerals: >4.5. Example: Quartz = 2.65, Gold = 19.3"

    const val FLUORESCENCE = "The ability to glow under UV light. " +
            "Describe the color and intensity (e.g., 'bright yellow under shortwave UV')"

    const val MOHS_HARDNESS = "Resistance to scratching on a scale of 1-10. " +
            "1=Talc (softest), 5=Apatite, 7=Quartz, 10=Diamond (hardest)"

    const val CRYSTAL_SYSTEM = "The internal arrangement of atoms that determines crystal shape. " +
            "7 systems: Cubic, Tetragonal, Orthorhombic, Hexagonal, Trigonal, Monoclinic, Triclinic"
}
