package net.meshcore.mineralog.ui.screens.edit

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.components.TooltipDropdownField
import net.meshcore.mineralog.ui.components.MineralFieldTooltips
import net.meshcore.mineralog.ui.components.MineralFieldValues
import net.meshcore.mineralog.ui.components.PhotoManager
import net.meshcore.mineralog.ui.components.v2.ComponentListEditor
import net.meshcore.mineralog.domain.model.MineralType
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMineralScreen(
    mineralId: String,
    onNavigateBack: () -> Unit,
    onMineralUpdated: (String) -> Unit,
    viewModel: EditMineralViewModel = viewModel(
        factory = EditMineralViewModelFactory(
            LocalContext.current,
            mineralId,
            (LocalContext.current.applicationContext as MineraLogApplication).mineralRepository
        )
    )
) {
    val context = LocalContext.current
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
    val tags by viewModel.tags.collectAsState()
    val tagSuggestions by viewModel.tagSuggestions.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    // v2.0: Mineral type and components
    val mineralType by viewModel.mineralType.collectAsState()
    val components by viewModel.components.collectAsState()

    // v3.1: Provenance fields
    val mineName by viewModel.mineName.collectAsState()
    val dealer by viewModel.dealer.collectAsState()
    val catalogNumber by viewModel.catalogNumber.collectAsState()
    val collectorName by viewModel.collectorName.collectAsState()
    val acquisitionNotes by viewModel.acquisitionNotes.collectAsState()

    // v3.1: Aggregate fields
    val rockType by viewModel.rockType.collectAsState()
    val texture by viewModel.texture.collectAsState()
    val dominantMinerals by viewModel.dominantMinerals.collectAsState()
    val interestingFeatures by viewModel.interestingFeatures.collectAsState()

    val isSaving = updateState is UpdateMineralState.Saving
    val isLoading = updateState is UpdateMineralState.Loading
    var showDiscardDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Get photos directory
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

    // Save action for keyboard submit
    val saveAction: () -> Unit = {
        if (name.isNotBlank() && !isSaving && !isLoading) {
            focusManager.clearFocus()
            viewModel.updateMineral(
                onSuccess = { id -> onMineralUpdated(id) },
                photosDir = photosDir
            )
        }
    }

    // Handle success state
    LaunchedEffect(updateState) {
        if (updateState is UpdateMineralState.Success) {
            val successState = updateState as UpdateMineralState.Success
            onMineralUpdated(successState.mineralId)
        }
    }

    // Handle back button with unsaved changes warning
    BackHandler(enabled = !isSaving && !isLoading) {
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

    // Error dialog
    if (updateState is UpdateMineralState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetUpdateState() },
            title = { Text(stringResource(R.string.error_generic)) },
            text = { Text((updateState as UpdateMineralState.Error).message) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetUpdateState() }) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_mineral_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isSaving && !isLoading) {
                                showDiscardDialog = true
                            }
                        },
                        enabled = !isSaving && !isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = saveAction,
                        enabled = name.isNotBlank() && !isSaving && !isLoading
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Required field legend
                Text(
                    text = stringResource(R.string.field_required_legend),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // v2.0: Mineral type indicator (read-only)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Type:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = if (mineralType == MineralType.AGGREGATE) "Agrégat" else "Minéral Simple",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            enabled = false
                        )
                        Text(
                            text = "(non modifiable)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

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
                                error(context.getString(R.string.error_name_required_detail))
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
                        text = "Modifiez les minéraux qui composent cet agrégat. Les pourcentages doivent totaliser ~100%.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ComponentListEditor(
                        components = components,
                        onComponentsChange = { viewModel.onComponentsChange(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // v3.1: Aggregate Properties Section
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Text(
                        text = "Propriétés de l'Agrégat",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Caractéristiques spécifiques à cet agrégat rocheux",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = rockType,
                        onValueChange = { viewModel.onRockTypeChange(it) },
                        label = { Text("Type de roche") },
                        placeholder = { Text("Ex: Ignée, Sédimentaire, Métamorphique") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = texture,
                        onValueChange = { viewModel.onTextureChange(it) },
                        label = { Text("Texture") },
                        placeholder = { Text("Ex: Grenue, Porphyrique, Vitreuse") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = dominantMinerals,
                        onValueChange = { viewModel.onDominantMineralsChange(it) },
                        label = { Text("Minéraux dominants visibles") },
                        placeholder = { Text("Ex: Quartz, Feldspath, Mica") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = interestingFeatures,
                        onValueChange = { viewModel.onInterestingFeaturesChange(it) },
                        label = { Text("Caractéristiques intéressantes") },
                        placeholder = { Text("Ex: Veines de quartz, inclusion de grenat") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                }

                // v3.1: Provenance Section
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Provenance & Acquisition",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Informations sur la provenance et l'acquisition du spécimen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = mineName,
                    onValueChange = { viewModel.onMineNameChange(it) },
                    label = { Text("Nom de la mine") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = dealer,
                    onValueChange = { viewModel.onDealerChange(it) },
                    label = { Text("Fournisseur/Dealer") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = catalogNumber,
                    onValueChange = { viewModel.onCatalogNumberChange(it) },
                    label = { Text("Numéro de catalogue") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = collectorName,
                    onValueChange = { viewModel.onCollectorNameChange(it) },
                    label = { Text("Nom du collectionneur") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = acquisitionNotes,
                    onValueChange = { viewModel.onAcquisitionNotesChange(it) },
                    label = { Text("Notes d'acquisition") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )

                // Tags Section
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
                                    this.contentDescription = context.getString(R.string.field_tags_description)
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
                                        this.contentDescription = context.getString(R.string.tag_suggestions_available, tagSuggestions.size)
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
                                                    this.contentDescription = context.getString(R.string.tag_select, suggestion)
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

                // Photos Section - BUGFIX: Moved inside scrollable Column
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = stringResource(R.string.section_photos),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = stringResource(R.string.section_photos_hint),
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
}
