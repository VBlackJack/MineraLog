package net.meshcore.mineralog.ui.components.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.domain.model.ComponentRole
import net.meshcore.mineralog.domain.model.MineralComponent
import java.util.UUID

/**
 * A card for editing a single mineral component in an aggregate.
 *
 * This v2.0 component provides a comprehensive form for specifying all properties
 * of a component mineral within an aggregate rock.
 *
 * @param component The mineral component to edit.
 * @param onComponentChange Callback when the component is modified.
 * @param onDeleteClick Callback when the delete button is clicked.
 * @param modifier Modifier for the card.
 * @param showExpandedByDefault Whether to show all fields by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentEditorCard(
    component: MineralComponent,
    onComponentChange: (MineralComponent) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showExpandedByDefault: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(showExpandedByDefault) }

    // Validation states
    val isNameValid = component.mineralName.isNotBlank()
    val isPercentageValid = component.percentage != null && component.percentage in 0f..100f

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with name, percentage, and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = component.mineralName.ifBlank { "Nouveau composant" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = component.percentageFormatted ?: "-%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Role badge
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = component.roleDisplayName,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Réduire" else "Étendre"
                        )
                    }

                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Required fields (always visible)
            OutlinedTextField(
                value = component.mineralName,
                onValueChange = { onComponentChange(component.copy(mineralName = it)) },
                label = { Text("Nom du minéral *") },
                isError = !isNameValid,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = if (!isNameValid) {
                    { Text("Le nom est obligatoire", color = MaterialTheme.colorScheme.error) }
                } else null
            )

            OutlinedTextField(
                value = component.percentage?.toString() ?: "",
                onValueChange = { value ->
                    val percentage = value.toFloatOrNull()
                    onComponentChange(component.copy(
                        percentage = percentage,
                        // Auto-calculate role from percentage
                        role = percentage?.let { ComponentRole.fromPercentage(it) } ?: ComponentRole.TRACE
                    ))
                },
                label = { Text("Pourcentage (%) *") },
                isError = !isPercentageValid,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = if (!isPercentageValid) {
                    { Text("Entre 0 et 100", color = MaterialTheme.colorScheme.error) }
                } else null
            )

            // Expanded fields
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()

                    Text(
                        text = "Propriétés optionnelles",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = component.mineralGroup ?: "",
                        onValueChange = { onComponentChange(component.copy(mineralGroup = it.takeIf { it.isNotBlank() })) },
                        label = { Text("Groupe") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = component.formula ?: "",
                        onValueChange = { onComponentChange(component.copy(formula = it.takeIf { it.isNotBlank() })) },
                        label = { Text("Formule chimique") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = component.mohsMin?.toString() ?: "",
                            onValueChange = { value ->
                                onComponentChange(component.copy(mohsMin = value.toFloatOrNull()))
                            },
                            label = { Text("Dureté min") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        OutlinedTextField(
                            value = component.mohsMax?.toString() ?: "",
                            onValueChange = { value ->
                                onComponentChange(component.copy(mohsMax = value.toFloatOrNull()))
                            },
                            label = { Text("Dureté max") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }

                    OutlinedTextField(
                        value = component.density?.toString() ?: "",
                        onValueChange = { value ->
                            onComponentChange(component.copy(density = value.toFloatOrNull()))
                        },
                        label = { Text("Densité") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = component.crystalSystem ?: "",
                        onValueChange = { onComponentChange(component.copy(crystalSystem = it.takeIf { it.isNotBlank() })) },
                        label = { Text("Système cristallin") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = component.luster ?: "",
                        onValueChange = { onComponentChange(component.copy(luster = it.takeIf { it.isNotBlank() })) },
                        label = { Text("Éclat") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = component.color ?: "",
                        onValueChange = { onComponentChange(component.copy(color = it.takeIf { it.isNotBlank() })) },
                        label = { Text("Couleur") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = component.notes ?: "",
                        onValueChange = { onComponentChange(component.copy(notes = it.takeIf { it.isNotBlank() })) },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentEditorCardPreview() {
    MaterialTheme {
        Surface {
            ComponentEditorCard(
                component = MineralComponent(
                    id = UUID.randomUUID().toString(),
                    mineralName = "Quartz",
                    mineralGroup = "Silicates",
                    percentage = 35f,
                    role = ComponentRole.PRINCIPAL,
                    formula = "SiO₂",
                    mohsMin = 7f,
                    mohsMax = 7f,
                    density = 2.65f
                ),
                onComponentChange = {},
                onDeleteClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentEditorCardExpandedPreview() {
    MaterialTheme {
        Surface {
            ComponentEditorCard(
                component = MineralComponent(
                    id = UUID.randomUUID().toString(),
                    mineralName = "Feldspath",
                    percentage = 40f,
                    role = ComponentRole.PRINCIPAL
                ),
                onComponentChange = {},
                onDeleteClick = {},
                modifier = Modifier.padding(16.dp),
                showExpandedByDefault = true
            )
        }
    }
}
