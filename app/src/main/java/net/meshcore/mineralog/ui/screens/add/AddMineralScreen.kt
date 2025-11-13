package net.meshcore.mineralog.ui.screens.add

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.ui.components.TooltipTextField
import net.meshcore.mineralog.ui.components.MineralFieldTooltips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMineralScreen(
    onNavigateBack: () -> Unit,
    onMineralAdded: (String) -> Unit,
    viewModel: AddMineralViewModel = viewModel(
        factory = AddMineralViewModelFactory(
            (LocalContext.current.applicationContext as MineraLogApplication).mineralRepository,
            (LocalContext.current.applicationContext as MineraLogApplication).settingsRepository
        )
    )
) {
    val name by viewModel.name.collectAsState()
    val group by viewModel.group.collectAsState()
    val formula by viewModel.formula.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val diaphaneity by viewModel.diaphaneity.collectAsState()
    val cleavage by viewModel.cleavage.collectAsState()
    val fracture by viewModel.fracture.collectAsState()
    val luster by viewModel.luster.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val habit by viewModel.habit.collectAsState()
    val crystalSystem by viewModel.crystalSystem.collectAsState()
    val tags by viewModel.tags.collectAsState() // Quick Win #8
    val tagSuggestions by viewModel.tagSuggestions.collectAsState() // Quick Win #8
    val saveState by viewModel.saveState.collectAsState()
    val draftSavedIndicator by viewModel.draftSavedIndicator.collectAsState()

    val isSaving = saveState is SaveMineralState.Saving
    val hasUnsavedChanges = name.isNotBlank() || group.isNotBlank() || formula.isNotBlank() ||
            notes.isNotBlank() || diaphaneity.isNotBlank() || cleavage.isNotBlank() ||
            fracture.isNotBlank() || luster.isNotBlank() || streak.isNotBlank() ||
            habit.isNotBlank() || crystalSystem.isNotBlank() || tags.isNotBlank()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Save action for keyboard submit
    val saveAction: () -> Unit = {
        if (name.isNotBlank() && !isSaving) {
            focusManager.clearFocus()
            viewModel.saveMineral { mineralId ->
                onMineralAdded(mineralId)
            }
        }
    }

    // Handle back button with unsaved changes warning
    BackHandler(enabled = hasUnsavedChanges && !isSaving) {
        showDiscardDialog = true
    }

    // Discard changes confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Mineral") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges && !isSaving) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Draft saved indicator
                    if (draftSavedIndicator) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Draft saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            viewModel.saveMineral { mineralId ->
                                onMineralAdded(mineralId)
                            }
                        },
                        enabled = name.isNotBlank() && !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("SAVE")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text("Name *") },
                isError = name.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        if (name.isBlank()) {
                            error("Name is required. This field cannot be empty.")
                            liveRegion = LiveRegionMode.Polite
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                supportingText = if (name.isBlank()) {
                    { Text("Name is required", color = MaterialTheme.colorScheme.error) }
                } else null
            )

            OutlinedTextField(
                value = group,
                onValueChange = { viewModel.onGroupChange(it) },
                label = { Text("Group") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = formula,
                onValueChange = { viewModel.onFormulaChange(it) },
                label = { Text("Chemical Formula") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8
            )

            // Technical Properties Section
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Technical Properties",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Optional fields with inline help. Tap ⓘ for explanations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TooltipTextField(
                value = diaphaneity,
                onValueChange = { viewModel.onDiaphaneityChange(it) },
                label = "Diaphaneity",
                tooltipText = MineralFieldTooltips.DIAPHANEITY,
                placeholder = "e.g., transparent, translucent, opaque",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipTextField(
                value = cleavage,
                onValueChange = { viewModel.onCleavageChange(it) },
                label = "Cleavage",
                tooltipText = MineralFieldTooltips.CLEAVAGE,
                placeholder = "e.g., perfect, good, poor, none",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipTextField(
                value = fracture,
                onValueChange = { viewModel.onFractureChange(it) },
                label = "Fracture",
                tooltipText = MineralFieldTooltips.FRACTURE,
                placeholder = "e.g., conchoidal, uneven, splintery",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipTextField(
                value = luster,
                onValueChange = { viewModel.onLusterChange(it) },
                label = "Luster",
                tooltipText = MineralFieldTooltips.LUSTER,
                placeholder = "e.g., metallic, vitreous, pearly",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipTextField(
                value = streak,
                onValueChange = { viewModel.onStreakChange(it) },
                label = "Streak",
                tooltipText = MineralFieldTooltips.STREAK,
                placeholder = "e.g., white, black, red",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipTextField(
                value = habit,
                onValueChange = { viewModel.onHabitChange(it) },
                label = "Habit",
                tooltipText = MineralFieldTooltips.HABIT,
                placeholder = "e.g., prismatic, tabular, massive",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipTextField(
                value = crystalSystem,
                onValueChange = { viewModel.onCrystalSystemChange(it) },
                label = "Crystal System",
                tooltipText = MineralFieldTooltips.CRYSTAL_SYSTEM,
                placeholder = "e.g., cubic, hexagonal, monoclinic",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Quick Win #8: Tags with autocomplete
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Tags & Organization",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Add tags separated by commas. Autocomplete suggestions will appear as you type.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tags input with autocomplete dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                Column {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { viewModel.onTagsChange(it) },
                        label = { Text("Tags") },
                        placeholder = { Text("e.g., collection, rare, beautiful") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Tags field. Enter comma-separated tags. " +
                                        "Autocomplete suggestions available."
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { saveAction() }),
                        supportingText = {
                            Text("Separate multiple tags with commas")
                        }
                    )

                    // Autocomplete suggestions dropdown
                    if (tagSuggestions.isNotEmpty()) {
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
                                tagSuggestions.forEach { suggestion ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                // Replace the last partial tag with the suggestion
                                                val currentTags = tags.split(",").dropLast(1)
                                                val newTagsText = if (currentTags.isEmpty()) {
                                                    suggestion
                                                } else {
                                                    currentTags.joinToString(", ") + ", " + suggestion
                                                }
                                                viewModel.onTagsChange(newTagsText + ", ")
                                            }
                                            .semantics {
                                                contentDescription = "Select tag: $suggestion"
                                            },
                                        color = MaterialTheme.colorScheme.surface
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
                }
            }
        }
    }
}
