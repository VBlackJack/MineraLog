package net.meshcore.mineralog.ui.screens.add

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.components.TooltipTextField
import net.meshcore.mineralog.ui.components.TooltipDropdownField
import net.meshcore.mineralog.ui.components.MineralFieldTooltips
import net.meshcore.mineralog.ui.components.MineralFieldValues
import net.meshcore.mineralog.ui.components.PhotoManager
import net.meshcore.mineralog.ui.components.v2.MineralTypeSelector
import net.meshcore.mineralog.ui.components.v2.ComponentListEditor
import net.meshcore.mineralog.domain.model.MineralType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMineralScreen(
    onNavigateBack: () -> Unit,
    onMineralAdded: (String) -> Unit,
    viewModel: AddMineralViewModel = viewModel(
        factory = AddMineralViewModelFactory(
            LocalContext.current,
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
    val photos by viewModel.photos.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val draftSavedIndicator by viewModel.draftSavedIndicator.collectAsState()

    // v2.0: Mineral type and components
    val mineralType by viewModel.mineralType.collectAsState()
    val components by viewModel.components.collectAsState()

    val context = LocalContext.current
    val photosDir = remember {
        File(context.filesDir, "photos").apply {
            if (!exists()) mkdirs()
        }
    }

    // Load localized dropdown values
    val crystalSystems = remember { MineralFieldValues.getCrystalSystems(context) }
    val lusterTypes = remember { MineralFieldValues.getLusterTypes(context) }
    val diaphaneityTypes = remember { MineralFieldValues.getDiaphaneityTypes(context) }
    val cleavageTypes = remember { MineralFieldValues.getCleavageTypes(context) }
    val fractureTypes = remember { MineralFieldValues.getFractureTypes(context) }
    val habitTypes = remember { MineralFieldValues.getHabitTypes(context) }
    val streakColors = remember { MineralFieldValues.getStreakColors(context) }

    val isSaving = saveState is SaveMineralState.Saving
    val hasUnsavedChanges = name.isNotBlank() || group.isNotBlank() || formula.isNotBlank() ||
            notes.isNotBlank() || diaphaneity.isNotBlank() || cleavage.isNotBlank() ||
            fracture.isNotBlank() || luster.isNotBlank() || streak.isNotBlank() ||
            habit.isNotBlank() || crystalSystem.isNotBlank() || tags.isNotBlank()
    var showDiscardDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(saveState) {
        if (saveState is SaveMineralState.Error) {
            snackbarHostState.showSnackbar(
                message = (saveState as SaveMineralState.Error).message,
                duration = SnackbarDuration.Long
            )
            viewModel.resetSaveState()
        }
    }

    // Save action for keyboard submit
    val saveAction: () -> Unit = {
        if (name.isNotBlank() && !isSaving) {
            focusManager.clearFocus()
            viewModel.saveMineral(
                onSuccess = { mineralId -> onMineralAdded(mineralId) },
                photosDir = photosDir
            )
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
            title = { Text(stringResource(R.string.dialog_discard_title)) },
            text = { Text(stringResource(R.string.dialog_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.button_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_mineral_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges && !isSaving) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
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
                                text = stringResource(R.string.add_mineral_draft_saved),
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
                            viewModel.saveMineral(
                                onSuccess = { mineralId -> onMineralAdded(mineralId) },
                                photosDir = photosDir
                            )
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
                        Text(stringResource(R.string.action_save))
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
            // Quick Win #7: Required field legend
            Text(
                text = stringResource(R.string.add_mineral_required_field),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // v2.0: Mineral type selector
            MineralTypeSelector(
                selectedType = mineralType,
                onTypeSelected = { viewModel.onMineralTypeChange(it) },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text(stringResource(R.string.field_name)) },
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
                    { Text(stringResource(R.string.error_name_required), color = MaterialTheme.colorScheme.error) }
                } else null
            )

            OutlinedTextField(
                value = group,
                onValueChange = { viewModel.onGroupChange(it) },
                label = { Text(stringResource(R.string.field_group)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = formula,
                onValueChange = { viewModel.onFormulaChange(it) },
                label = { Text(stringResource(R.string.field_formula)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text(stringResource(R.string.field_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8
            )

            // v2.0: Conditional content based on mineral type
            if (mineralType == MineralType.SIMPLE) {
                // Technical Properties Section (Simple minerals only)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.section_technical_properties),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.section_technical_properties_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TooltipDropdownField(
                value = diaphaneity,
                onValueChange = { viewModel.onDiaphaneityChange(it) },
                label = stringResource(R.string.field_diaphaneity_label),
                tooltipText = MineralFieldTooltips.DIAPHANEITY,
                options = diaphaneityTypes,
                placeholder = stringResource(R.string.field_diaphaneity_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipDropdownField(
                value = cleavage,
                onValueChange = { viewModel.onCleavageChange(it) },
                label = stringResource(R.string.field_cleavage_label),
                tooltipText = MineralFieldTooltips.CLEAVAGE,
                options = cleavageTypes,
                placeholder = stringResource(R.string.field_cleavage_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipDropdownField(
                value = fracture,
                onValueChange = { viewModel.onFractureChange(it) },
                label = stringResource(R.string.field_fracture_label),
                tooltipText = MineralFieldTooltips.FRACTURE,
                options = fractureTypes,
                placeholder = stringResource(R.string.field_fracture_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipDropdownField(
                value = luster,
                onValueChange = { viewModel.onLusterChange(it) },
                label = stringResource(R.string.field_luster_label),
                tooltipText = MineralFieldTooltips.LUSTER,
                options = lusterTypes,
                placeholder = stringResource(R.string.field_luster_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipDropdownField(
                value = streak,
                onValueChange = { viewModel.onStreakChange(it) },
                label = stringResource(R.string.field_streak_label),
                tooltipText = MineralFieldTooltips.STREAK,
                options = streakColors,
                placeholder = stringResource(R.string.field_streak_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipDropdownField(
                value = habit,
                onValueChange = { viewModel.onHabitChange(it) },
                label = stringResource(R.string.field_habit_label),
                tooltipText = MineralFieldTooltips.HABIT,
                options = habitTypes,
                placeholder = stringResource(R.string.field_habit_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            TooltipDropdownField(
                value = crystalSystem,
                onValueChange = { viewModel.onCrystalSystemChange(it) },
                label = stringResource(R.string.field_crystal_system_label),
                tooltipText = MineralFieldTooltips.CRYSTAL_SYSTEM,
                options = crystalSystems,
                placeholder = stringResource(R.string.field_crystal_system_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            } else {
                // v2.0: Component editor for aggregates
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Composants de l'agrégat",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Définissez les minéraux qui composent cet agrégat. Les pourcentages doivent totaliser ~100%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ComponentListEditor(
                    components = components,
                    onComponentsChange = { viewModel.onComponentsChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Quick Win #8: Tags with autocomplete
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.section_tags_organization),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.section_tags_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tags input with autocomplete dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                Column {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { viewModel.onTagsChange(it) },
                        label = { Text(stringResource(R.string.field_tags)) },
                        placeholder = { Text(stringResource(R.string.field_tags_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                this.contentDescription = "Tags field. Enter comma-separated tags. " +
                                        "Autocomplete suggestions available."
                            },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { saveAction() }),
                        supportingText = {
                            Text(stringResource(R.string.field_tags_supporting))
                        }
                    )

                    // Autocomplete suggestions dropdown
                    if (tagSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .semantics {
                                    this.contentDescription = "${tagSuggestions.size} tag suggestions available"
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
                                                this.contentDescription = "Select tag: $suggestion"
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

            // Photos Section
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.add_mineral_photos_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.add_mineral_photos_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PhotoManager(
                photos = photos,
                onAddFromGallery = { uri -> viewModel.addPhoto(uri) },
                onTakePhoto = { uri -> viewModel.addPhoto(uri) },
                onRemovePhoto = { photoId -> viewModel.removePhoto(photoId) },
                onUpdateCaption = { photoId, caption -> viewModel.updatePhotoCaption(photoId, caption) },
                onUpdateType = { photoId, type -> viewModel.updatePhotoType(photoId, type) },
                photosDir = photosDir,
                modifier = Modifier.fillMaxWidth()
            )

            // BUGFIX: Dynamic spacing that only appears when keyboard is shown
            Spacer(modifier = Modifier.imePadding())
        }
    }
}
