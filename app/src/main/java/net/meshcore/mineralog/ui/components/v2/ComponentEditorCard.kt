package net.meshcore.mineralog.ui.components.v2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.domain.model.ComponentRole
import net.meshcore.mineralog.domain.model.MineralComponent
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.ui.components.reference.ReferenceMineralAutocomplete
import java.util.UUID

/**
 * A card for editing a single mineral component in an aggregate.
 *
 * This v2.0/v3.0 component provides a comprehensive form for specifying all properties
 * of a component mineral within an aggregate rock, with optional reference mineral autocomplete.
 *
 * @param component The mineral component to edit.
 * @param onComponentChange Callback when the component is modified.
 * @param onDeleteClick Callback when the delete button is clicked.
 * @param modifier Modifier for the card.
 * @param showExpandedByDefault Whether to show all fields by default.
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ComponentEditorCard(
    component: MineralComponent,
    onComponentChange: (MineralComponent) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showExpandedByDefault: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(showExpandedByDefault) }

    // v3.0: Reference mineral autocomplete
    val context = LocalContext.current
    val referenceMineralRepository = remember {
        (context.applicationContext as? MineraLogApplication)?.referenceMineralRepository
    }

    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<ReferenceMineralEntity>>(emptyList()) }
    var isLoadingReferenceMinerals by remember { mutableStateOf(false) }
    var selectedReferenceMineral by remember { mutableStateOf<ReferenceMineralEntity?>(null) }

    // Search with debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2 && referenceMineralRepository != null) {
            delay(300)
            isLoadingReferenceMinerals = true
            referenceMineralRepository.searchByNameLimit(searchQuery, 10).collect { results ->
                suggestions = results
                isLoadingReferenceMinerals = false
            }
        } else {
            suggestions = emptyList()
            isLoadingReferenceMinerals = false
        }
    }

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

                        // v3.0: Reference link indicator
                        if (component.referenceMineralId != null || selectedReferenceMineral != null) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = "Lié à la bibliothèque",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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

            // v3.0: Reference mineral autocomplete or manual name entry
            if (referenceMineralRepository != null && selectedReferenceMineral == null && component.referenceMineralId == null) {
                ReferenceMineralAutocomplete(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    suggestions = suggestions,
                    onMineralSelected = { mineral ->
                        selectedReferenceMineral = mineral
                        searchQuery = mineral.nameFr
                        // Auto-fill properties from reference
                        onComponentChange(component.copy(
                            referenceMineralId = mineral.id,
                            mineralName = mineral.nameFr,
                            mineralGroup = mineral.mineralGroup,
                            formula = mineral.formula,
                            mohsMin = mineral.mohsMin,
                            mohsMax = mineral.mohsMax,
                            density = mineral.density,
                            crystalSystem = mineral.crystalSystem,
                            luster = mineral.luster,
                            diaphaneity = mineral.diaphaneity,
                            cleavage = mineral.cleavage,
                            fracture = mineral.fracture,
                            habit = mineral.habit,
                            streak = mineral.streak,
                            fluorescence = mineral.fluorescence
                        ))
                    },
                    selectedMineral = null,
                    onClearSelection = { },
                    isLoading = isLoadingReferenceMinerals,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (selectedReferenceMineral == null && component.referenceMineralId == null) {
                // Fallback to manual entry if repository not available
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
            } else {
                // Reference mineral selected - show locked name with change button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✓ ${component.mineralName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Propriétés depuis la bibliothèque",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        TextButton(
                            onClick = {
                                selectedReferenceMineral = null
                                searchQuery = ""
                                onComponentChange(component.copy(
                                    referenceMineralId = null,
                                    mineralName = "",
                                    mineralGroup = null,
                                    formula = null,
                                    mohsMin = null,
                                    mohsMax = null,
                                    density = null,
                                    crystalSystem = null,
                                    luster = null,
                                    diaphaneity = null,
                                    cleavage = null,
                                    fracture = null,
                                    habit = null,
                                    streak = null,
                                    fluorescence = null
                                ))
                            }
                        ) {
                            Text("Changer")
                        }
                    }
                }
            }

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

            // Expanded fields (with smooth expand/collapse animation)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                        expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut(animationSpec = tween(durationMillis = 200)) +
                       shrinkVertically(animationSpec = tween(durationMillis = 200))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider()

                    // v3.0: Show locked properties or editable fields based on reference mineral selection
                    if (component.referenceMineralId != null || selectedReferenceMineral != null) {
                        Text(
                            text = "Propriétés héritées de la bibliothèque",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (component.mineralGroup != null) {
                                    ComponentPropertyDisplay("Groupe", component.mineralGroup)
                                }
                                if (component.formula != null) {
                                    ComponentPropertyDisplay("Formule", component.formula)
                                }
                                component.hardnessRange?.let { hardness ->
                                    ComponentPropertyDisplay("Dureté", hardness)
                                }
                                if (component.density != null) {
                                    ComponentPropertyDisplay("Densité", "${component.density} g/cm³")
                                }
                                if (component.crystalSystem != null) {
                                    ComponentPropertyDisplay("Système", component.crystalSystem)
                                }
                                if (component.luster != null) {
                                    ComponentPropertyDisplay("Éclat", component.luster)
                                }
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Notes spécifiques à ce composant",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = component.notes ?: "",
                            onValueChange = { onComponentChange(component.copy(notes = it.takeIf { it.isNotBlank() })) },
                            label = { Text("Notes") },
                            placeholder = { Text("Notes spécifiques pour ce composant dans l'agrégat") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                    } else {
                        // Manual entry mode - all fields editable
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

/**
 * v3.0: Displays a read-only property from reference mineral in component card.
 */
@Composable
private fun ComponentPropertyDisplay(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
