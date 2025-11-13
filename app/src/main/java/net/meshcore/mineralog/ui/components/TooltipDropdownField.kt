package net.meshcore.mineralog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Quick Win #3: Dropdown field with tooltip for technical mineral properties
 * Combines predefined values with the option to enter custom values
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tooltipText: String,
    options: List<String>,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }
    var isCustomInput by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Tooltip card (shown when info icon is tapped)
        if (showTooltip) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics {
                        contentDescription = "$label tooltip: $tooltipText"
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = tooltipText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Dropdown or text field
        if (isCustomInput) {
            // Custom input mode
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("$label (Custom)") },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = keyboardOptions,
                trailingIcon = {
                    Row {
                        // Switch back to dropdown
                        IconButton(
                            onClick = { isCustomInput = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Switch to dropdown"
                            )
                        }
                        // Tooltip icon
                        IconButton(
                            onClick = { showTooltip = !showTooltip },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = if (showTooltip) "Hide tooltip" else "Show tooltip",
                                tint = if (showTooltip) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            )
        } else {
            // Dropdown mode
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(label) },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .semantics {
                            contentDescription = "$label dropdown. Current value: ${value.ifEmpty { "none" }}. Tap to select from ${options.size} options."
                        },
                    trailingIcon = {
                        Row {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            // Tooltip icon
                            IconButton(
                                onClick = { showTooltip = !showTooltip },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = if (showTooltip) "Hide tooltip" else "Show tooltip",
                                    tint = if (showTooltip) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                if (option == "Other") {
                                    isCustomInput = true
                                    onValueChange("")
                                } else {
                                    onValueChange(option)
                                }
                                expanded = false
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Select $option"
                            }
                        )
                    }
                }
            }
        }
    }
}
