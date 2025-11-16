package net.meshcore.mineralog.ui.components.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
 * A read-only card for displaying a single mineral component in an aggregate.
 *
 * This v2.0 component provides a clean view of component properties in the detail screen.
 *
 * @param component The mineral component to display.
 * @param modifier Modifier for the card.
 * @param showExpandedByDefault Whether to show all fields by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentCard(
    component: MineralComponent,
    modifier: Modifier = Modifier,
    showExpandedByDefault: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(showExpandedByDefault) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with name, percentage, and expand button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = component.mineralName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = component.percentageFormatted ?: "-%",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Role badge
                        val roleColor = when (component.role) {
                            ComponentRole.PRINCIPAL -> MaterialTheme.colorScheme.primaryContainer
                            ComponentRole.ACCESSORY -> MaterialTheme.colorScheme.secondaryContainer
                            ComponentRole.TRACE -> MaterialTheme.colorScheme.tertiaryContainer
                        }

                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = component.roleDisplayName,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = roleColor
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Masquer les détails" else "Afficher les détails"
                    )
                }
            }

            // Basic properties (always visible)
            if (component.mineralGroup != null || component.formula != null) {
                HorizontalDivider()

                component.mineralGroup?.let { group ->
                    PropertyRow(label = "Groupe", value = group)
                }

                component.formula?.let { formula ->
                    PropertyRow(label = "Formule", value = formula)
                }
            }

            // Expanded properties
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (component.mohsMin != null || component.mohsMax != null ||
                        component.density != null || component.crystalSystem != null ||
                        component.luster != null || component.color != null ||
                        component.notes != null) {
                        HorizontalDivider()
                    }

                    component.hardnessRange?.let { range ->
                        PropertyRow(label = "Dureté (Mohs)", value = range)
                    }

                    component.density?.let { density ->
                        PropertyRow(label = "Densité", value = String.format("%.2f", density))
                    }

                    component.crystalSystem?.let { system ->
                        PropertyRow(label = "Système cristallin", value = system)
                    }

                    component.luster?.let { luster ->
                        PropertyRow(label = "Éclat", value = luster)
                    }

                    component.color?.let { color ->
                        PropertyRow(label = "Couleur", value = color)
                    }

                    component.notes?.let { notes ->
                        HorizontalDivider()
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentCardPreview() {
    MaterialTheme {
        Surface {
            ComponentCard(
                component = MineralComponent(
                    id = UUID.randomUUID().toString(),
                    mineralName = "Quartz",
                    mineralGroup = "Silicates",
                    percentage = 35f,
                    role = ComponentRole.PRINCIPAL,
                    formula = "SiO₂",
                    mohsMin = 7f,
                    mohsMax = 7f,
                    density = 2.65f,
                    crystalSystem = "Hexagonal",
                    luster = "Vitreux",
                    color = "Incolore à blanc"
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentCardExpandedPreview() {
    MaterialTheme {
        Surface {
            ComponentCard(
                component = MineralComponent(
                    id = UUID.randomUUID().toString(),
                    mineralName = "Feldspath",
                    mineralGroup = "Tectosilicates",
                    percentage = 40f,
                    role = ComponentRole.PRINCIPAL,
                    formula = "(K,Na)AlSi₃O₈",
                    mohsMin = 6f,
                    mohsMax = 6.5f,
                    density = 2.56f,
                    crystalSystem = "Triclinique",
                    luster = "Vitreux",
                    color = "Rose à blanc",
                    notes = "Le feldspath est un minéral très commun dans les roches granitiques."
                ),
                modifier = Modifier.padding(16.dp),
                showExpandedByDefault = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentCardMinimalPreview() {
    MaterialTheme {
        Surface {
            ComponentCard(
                component = MineralComponent(
                    id = UUID.randomUUID().toString(),
                    mineralName = "Mica",
                    percentage = 5f,
                    role = ComponentRole.ACCESSORY
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
