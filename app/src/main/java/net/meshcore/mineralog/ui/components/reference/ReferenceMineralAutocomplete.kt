package net.meshcore.mineralog.ui.components.reference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity

/**
 * Autocomplete component for selecting a reference mineral.
 *
 * Features:
 * - Search with 300ms debounce
 * - Dropdown suggestions showing key properties
 * - Loading and empty states
 * - Callback when mineral is selected
 */
@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun ReferenceMineralAutocomplete(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    suggestions: List<ReferenceMineralEntity>,
    onMineralSelected: (ReferenceMineralEntity) -> Unit,
    selectedMineral: ReferenceMineralEntity?,
    onClearSelection: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                onSearchQueryChange(query)
                showDropdown = query.isNotBlank()
            },
            label = { Text("Rechercher un minéral de référence") },
            placeholder = { Text("Ex: Quartz, Calcite, Pyrite...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        onSearchQueryChange("")
                        showDropdown = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Effacer"
                        )
                    }
                }
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Champ de recherche de minéral de référence. " +
                            "Entrez le nom d'un minéral pour voir les suggestions."
                },
            singleLine = true,
            supportingText = {
                when {
                    selectedMineral != null -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✓ Sélectionné : ${selectedMineral.nameFr}",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall
                            )
                            TextButton(
                                onClick = onClearSelection,
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "Changer",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    isLoading -> {
                        Text("Recherche en cours...")
                    }
                    searchQuery.isNotBlank() && suggestions.isEmpty() -> {
                        Text("Aucun minéral trouvé")
                    }
                    else -> {
                        Text("Recherchez un minéral pour auto-remplir les propriétés techniques")
                    }
                }
            }
        )

        // Dropdown suggestions
        if (showDropdown && searchQuery.isNotBlank() && suggestions.isNotEmpty() && selectedMineral == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .padding(top = 4.dp)
                    .semantics {
                        contentDescription = "${suggestions.size} suggestions de minéraux disponibles"
                        liveRegion = LiveRegionMode.Polite
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn {
                    items(suggestions) { mineral ->
                        ReferenceMineralSuggestionItem(
                            mineral = mineral,
                            onClick = {
                                onMineralSelected(mineral)
                                onSearchQueryChange(mineral.nameFr)
                                showDropdown = false
                            }
                        )
                        if (mineral != suggestions.last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // Empty state with link to add mineral
        if (showDropdown && searchQuery.length >= 2 && suggestions.isEmpty() && !isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Aucun minéral trouvé",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Vous pouvez continuer la saisie manuelle ou ajouter ce minéral à la bibliothèque pour une utilisation future.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceMineralSuggestionItem(
    mineral: ReferenceMineralEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Sélectionner ${mineral.nameFr}"
            }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mineral.nameFr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (mineral.nameEn != mineral.nameFr) {
                        Text(
                            text = mineral.nameEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // User-defined badge
                if (mineral.isUserDefined) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Personnalisé",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Key properties (compact display)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Group
                mineral.mineralGroup?.let { group ->
                    PropertyChip(label = "Groupe", value = group)
                }

                // Crystal System
                mineral.crystalSystem?.let { system ->
                    PropertyChip(label = "Système", value = system)
                }

                // Hardness
                if (mineral.mohsMin != null && mineral.mohsMax != null) {
                    val hardness = if (mineral.mohsMin == mineral.mohsMax) {
                        "${mineral.mohsMin}"
                    } else {
                        "${mineral.mohsMin}-${mineral.mohsMax}"
                    }
                    PropertyChip(label = "Dureté", value = hardness)
                }
            }
        }
    }
}

@Composable
private fun PropertyChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
