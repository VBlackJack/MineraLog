package net.meshcore.mineralog.ui.components.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.domain.model.ComponentRole
import net.meshcore.mineralog.domain.model.MineralComponent
import java.util.UUID

/**
 * A composable for editing a list of mineral components in an aggregate.
 *
 * This v2.0 component provides:
 * - Adding new components
 * - Editing existing components
 * - Deleting components
 * - Real-time percentage validation
 * - Minimum component count validation
 *
 * @param components The list of components to edit.
 * @param onComponentsChange Callback when the component list changes.
 * @param modifier Modifier for the container.
 */
@Composable
fun ComponentListEditor(
    components: List<MineralComponent>,
    onComponentsChange: (List<MineralComponent>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate total percentage
    val totalPercentage = components.mapNotNull { it.percentage }.sum()
    val isPercentageValid = totalPercentage in 99f..101f

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with validation status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Composants (${components.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Total : ${String.format("%.1f", totalPercentage)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPercentageValid) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            Button(
                onClick = {
                    // Add a new empty component
                    val newComponent = MineralComponent(
                        id = UUID.randomUUID().toString(),
                        mineralName = "",
                        percentage = null,
                        role = ComponentRole.TRACE
                    )
                    onComponentsChange(components + newComponent)
                },
                enabled = components.size < 20 // Reasonable limit
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter")
            }
        }

        // Validation warnings
        AnimatedVisibility(visible = components.size < 2) {
            PercentageValidationCard(
                message = "Un agrégat doit avoir au moins 2 composants",
                isError = true
            )
        }

        AnimatedVisibility(visible = !isPercentageValid && components.size >= 2) {
            val message = when {
                totalPercentage < 99f -> "Le total est trop faible (${String.format("%.1f", totalPercentage)}%). Il doit être proche de 100%."
                totalPercentage > 101f -> "Le total est trop élevé (${String.format("%.1f", totalPercentage)}%). Il doit être proche de 100%."
                else -> ""
            }
            PercentageValidationCard(
                message = message,
                isError = true
            )
        }

        AnimatedVisibility(visible = isPercentageValid && components.size >= 2) {
            PercentageValidationCard(
                message = "Composition valide ✓",
                isError = false
            )
        }

        // Component cards
        if (components.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Aucun composant",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Cliquez sur 'Ajouter' pour commencer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            components.forEachIndexed { index, component ->
                ComponentEditorCard(
                    component = component,
                    onComponentChange = { updatedComponent ->
                        val updatedComponents = components.toMutableList()
                        updatedComponents[index] = updatedComponent
                        onComponentsChange(updatedComponents)
                    },
                    onDeleteClick = {
                        val updatedComponents = components.toMutableList()
                        updatedComponents.removeAt(index)
                        onComponentsChange(updatedComponents)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Helper text
        if (components.isNotEmpty()) {
            Text(
                text = "Astuce : Les composants avec >20% sont PRINCIPAUX, 5-20% sont ACCESSOIRES, <5% sont TRACES",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * A card showing percentage validation status.
 */
@Composable
private fun PercentageValidationCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isError) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentListEditorPreview() {
    MaterialTheme {
        Surface {
            ComponentListEditor(
                components = listOf(
                    MineralComponent(
                        id = UUID.randomUUID().toString(),
                        mineralName = "Quartz",
                        mineralGroup = "Silicates",
                        percentage = 35f,
                        role = ComponentRole.PRINCIPAL,
                        formula = "SiO₂"
                    ),
                    MineralComponent(
                        id = UUID.randomUUID().toString(),
                        mineralName = "Feldspath",
                        percentage = 40f,
                        role = ComponentRole.PRINCIPAL
                    ),
                    MineralComponent(
                        id = UUID.randomUUID().toString(),
                        mineralName = "Mica",
                        percentage = 20f,
                        role = ComponentRole.ACCESSORY
                    )
                ),
                onComponentsChange = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentListEditorEmptyPreview() {
    MaterialTheme {
        Surface {
            ComponentListEditor(
                components = emptyList(),
                onComponentsChange = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentListEditorInvalidPreview() {
    MaterialTheme {
        Surface {
            ComponentListEditor(
                components = listOf(
                    MineralComponent(
                        id = UUID.randomUUID().toString(),
                        mineralName = "Quartz",
                        percentage = 50f,
                        role = ComponentRole.PRINCIPAL
                    ),
                    MineralComponent(
                        id = UUID.randomUUID().toString(),
                        mineralName = "Feldspath",
                        percentage = 30f,
                        role = ComponentRole.PRINCIPAL
                    )
                    // Total = 80% (invalid)
                ),
                onComponentsChange = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
