package net.meshcore.mineralog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow as ComposeFlowRow

/**
 * Quick Win #9: Interactive tag chips with visual feedback
 * Replaces plain text tag input with chip-based UI
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagChipsInput(
    tags: String,
    onTagsChange: (String) -> Unit,
    tagSuggestions: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var newTagText by remember { mutableStateOf("") }

    // Parse tags from comma-separated string
    val tagList = remember(tags) {
        tags.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    Column(modifier = modifier) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Chips display
        if (tagList.isNotEmpty()) {
            ComposeFlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics {
                        contentDescription = "${tagList.size} tags: ${tagList.joinToString(", ")}"
                        liveRegion = LiveRegionMode.Polite
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tagList.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { },
                        label = { Text(tag) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    // Remove this tag
                                    val newTags = tagList.filter { it != tag }
                                    onTagsChange(newTags.joinToString(", "))
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove tag $tag",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Tag: $tag. Tap X to remove"
                        }
                    )
                }
            }
        } else {
            Text(
                text = "No tags yet. Add tags below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Input field for new tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTagText,
                onValueChange = { newTagText = it },
                label = { Text("Add tag") },
                placeholder = { Text("e.g., rare, collection") },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Add new tag field"
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (newTagText.isNotBlank()) {
                            val updatedTags = if (tags.isEmpty()) {
                                newTagText.trim()
                            } else {
                                "$tags, ${newTagText.trim()}"
                            }
                            onTagsChange(updatedTags)
                            newTagText = ""
                        }
                    }
                )
            )

            IconButton(
                onClick = {
                    if (newTagText.isNotBlank()) {
                        val updatedTags = if (tags.isEmpty()) {
                            newTagText.trim()
                        } else {
                            "$tags, ${newTagText.trim()}"
                        }
                        onTagsChange(updatedTags)
                        newTagText = ""
                    }
                },
                enabled = newTagText.isNotBlank(),
                modifier = Modifier.semantics {
                    contentDescription = "Add tag button"
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add tag",
                    tint = if (newTagText.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        }

        // Autocomplete suggestions
        if (tagSuggestions.isNotEmpty() && newTagText.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics {
                        contentDescription = "${tagSuggestions.size} tag suggestions available"
                        liveRegion = LiveRegionMode.Polite
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    tagSuggestions.take(5).forEach { suggestion ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                newTagText = suggestion
                            }
                        ) {
                            Text(
                                text = suggestion,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (suggestion != tagSuggestions.last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        Text(
            text = "Separate multiple tags with commas or add them one at a time",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
