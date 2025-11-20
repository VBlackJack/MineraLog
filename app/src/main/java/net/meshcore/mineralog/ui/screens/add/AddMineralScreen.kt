package net.meshcore.mineralog.ui.screens.add

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import net.meshcore.mineralog.ui.components.InfoIconButton
import net.meshcore.mineralog.ui.components.v2.MineralTypeSelector
import net.meshcore.mineralog.ui.components.v2.ComponentListEditor
import net.meshcore.mineralog.ui.components.reference.ReferenceMineralAutocomplete
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
            (LocalContext.current.applicationContext as MineraLogApplication).settingsRepository,
            (LocalContext.current.applicationContext as MineraLogApplication).referenceMineralRepository,
            (LocalContext.current.applicationContext as MineraLogApplication).resourceProvider
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
    val mohsHardness by viewModel.mohsHardness.collectAsState()
    val tags by viewModel.tags.collectAsState() // Quick Win #8
    val tagSuggestions by viewModel.tagSuggestions.collectAsState() // Quick Win #8
    val photos by viewModel.photos.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val draftSavedIndicator by viewModel.draftSavedIndicator.collectAsState()

    // v2.0: Mineral type and components
    val mineralType by viewModel.mineralType.collectAsState()
    val components by viewModel.components.collectAsState()

    // v3.0: Reference mineral autocomplete
    val selectedReferenceMineral by viewModel.selectedReferenceMineral.collectAsState()
    val referenceMineralSearchQuery by viewModel.referenceMineralSearchQuery.collectAsState()
    val referenceMineralSuggestions by viewModel.referenceMineralSuggestions.collectAsState()
    val isLoadingReferenceMinerals by viewModel.isLoadingReferenceMinerals.collectAsState()
    val colorVariety by viewModel.colorVariety.collectAsState()
    val actualDiaphaneity by viewModel.actualDiaphaneity.collectAsState()
    val qualityNotes by viewModel.qualityNotes.collectAsState()

    // Sprint 5: Price and Weight fields
    val price by viewModel.price.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val weightGr by viewModel.weightGr.collectAsState()

    // v3.0.0: Provenance fields
    val provenanceCountry by viewModel.provenanceCountry.collectAsState()
    val provenanceLocality by viewModel.provenanceLocality.collectAsState()
    val provenanceMine by viewModel.provenanceMine.collectAsState()
    val provenanceDate by viewModel.provenanceDate.collectAsState()

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

    // Show snackbar on photo added
    var previousPhotoCount by remember { mutableStateOf(photos.size) }
    LaunchedEffect(photos.size) {
        if (photos.size > previousPhotoCount) {
            val photoAddedMessage = context.resources.getQuantityString(
                R.plurals.photo_added_snackbar_message,
                photos.size,
                photos.size
            )
            snackbarHostState.showSnackbar(
                message = photoAddedMessage,
                duration = SnackbarDuration.Short
            )
        }
        previousPhotoCount = photos.size
    }

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

    // Show toast when reference mineral is selected
    LaunchedEffect(selectedReferenceMineral) {
        selectedReferenceMineral?.let { mineral ->
            snackbarHostState.showSnackbar(
                message = "✓ Propriétés de ${mineral.nameFr} chargées depuis la bibliothèque",
                duration = SnackbarDuration.Short
            )
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

            // v3.0: Reference mineral autocomplete (only for SIMPLE minerals)
            if (mineralType == MineralType.SIMPLE) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bibliothèque de référence (optionnel)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    InfoIconButton(
                        tooltipText = "La bibliothèque de référence contient les propriétés techniques standard de chaque minéral. Sélectionnez un minéral pour auto-remplir automatiquement ces propriétés."
                    )
                }

                Text(
                    text = "Sélectionnez un minéral de référence pour auto-remplir les propriétés techniques",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ReferenceMineralAutocomplete(
                    searchQuery = referenceMineralSearchQuery,
                    onSearchQueryChange = { viewModel.onReferenceMineralSearchQueryChange(it) },
                    suggestions = referenceMineralSuggestions,
                    onMineralSelected = { viewModel.selectReferenceMineral(it) },
                    selectedMineral = selectedReferenceMineral,
                    onClearSelection = { viewModel.clearReferenceMineral() },
                    isLoading = isLoadingReferenceMinerals,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

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

            // v3.0: Show different title based on whether reference is selected
            Text(
                text = selectedReferenceMineral?.let { mineral ->
                    "Propriétés de référence (depuis ${mineral.nameFr})"
                } ?: stringResource(R.string.section_technical_properties),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (selectedReferenceMineral != null) {
                    "Ces propriétés sont héritées de la bibliothèque de référence"
                } else {
                    stringResource(R.string.section_technical_properties_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // v3.0: Card with grey background when properties are locked (with fade-in animation)
            AnimatedVisibility(
                visible = selectedReferenceMineral != null,
                enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                        expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                       shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Propriétés verrouillées depuis la référence",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Technical properties - read-only display
                        if (group.isNotBlank()) {
                            PropertyDisplay(label = stringResource(R.string.field_group), value = group)
                        }
                        if (formula.isNotBlank()) {
                            PropertyDisplay(label = stringResource(R.string.field_formula), value = formula)
                        }
                        if (crystalSystem.isNotBlank()) {
                            PropertyDisplay(label = stringResource(R.string.field_crystal_system_label), value = crystalSystem)
                        }
                        if (diaphaneity.isNotBlank()) {
                            PropertyDisplay(label = stringResource(R.string.field_diaphaneity_label), value = diaphaneity)
                        }
                        if (cleavage.isNotBlank()) {
                            PropertyDisplay(label = stringResource(R.string.field_cleavage_label), value = cleavage)
                        }
                        if (fracture.isNotBlank()) {
                            PropertyDisplay(label = stringResource(R.string.field_fracture_label), value = fracture)
                        }
                        if (luster.isNotBlank()) {
                            PropertyDisplay(label = stringResource(R.string.field_luster_label), value = luster)
                        }
                        if (streak.isNotBlank()) {
                            PropertyDisplay(label = stringResource(R.string.field_streak_label), value = streak)
                        }
                        if (habit.isNotBlank()) {
                            PropertyDisplay(label = stringResource(R.string.field_habit_label), value = habit)
                        }
                    }
                }
            }

            // v3.0: Specimen-specific properties section (with fade-in animation)
            AnimatedVisibility(
                visible = selectedReferenceMineral != null,
                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 200)) +
                        expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
                       shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Propriétés de ce spécimen",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        InfoIconButton(
                            tooltipText = "Ces propriétés décrivent votre spécimen spécifique. Par exemple, le quartz de référence est incolore, mais votre améthyste (variété de quartz) est violette."
                        )
                    }

                    Text(
                        text = "Ces propriétés sont spécifiques à votre spécimen et peuvent différer de la référence",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = colorVariety,
                        onValueChange = { viewModel.onColorVarietyChange(it) },
                        label = { Text("Variété de couleur") },
                        placeholder = { Text("Ex: Améthyste, Citrine, Fumé...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    TooltipDropdownField(
                        value = actualDiaphaneity,
                        onValueChange = { viewModel.onActualDiaphaneityChange(it) },
                        label = "Diaphanéité réelle",
                        tooltipText = "La diaphanéité de ce spécimen peut différer de la référence",
                        options = diaphaneityTypes,
                        placeholder = "Ex: Transparent, Translucide...",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = qualityNotes,
                        onValueChange = { viewModel.onQualityNotesChange(it) },
                        label = { Text("Notes de qualité") },
                        placeholder = { Text("Ex: Cristaux bien formés, inclusions...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }

            // v3.0: Editable fields when no reference mineral selected (with fade-in animation)
            AnimatedVisibility(
                visible = selectedReferenceMineral == null,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)) +
                        expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut(animationSpec = tween(durationMillis = 200)) +
                       shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // No reference mineral selected - show editable fields
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

                val isMohsError = remember(mohsHardness) {
                    if (mohsHardness.isBlank()) {
                        false // Not an error if blank
                    } else {
                        try {
                            if (mohsHardness.contains("-")) {
                                val parts = mohsHardness.split("-").map { it.trim().toFloat() }
                                val min = parts.getOrNull(0)
                                val max = parts.getOrNull(1)
                                min == null || max == null || min < 1f || max > 10f || min > max
                            } else {
                                val value = mohsHardness.toFloat()
                                value < 1f || value > 10f
                            }
                        } catch (e: NumberFormatException) {
                            true // Error if not a number
                        }
                    }
                }

                OutlinedTextField(
                    value = mohsHardness,
                    onValueChange = { viewModel.onMohsHardnessChange(it) },
                    label = { Text(stringResource(R.string.field_hardness)) },
                    placeholder = { Text(stringResource(R.string.field_hardness_placeholder)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    isError = isMohsError,
                    supportingText = if (isMohsError) {
                        { Text(stringResource(R.string.field_hardness_error), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                }
            }
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

            // Sprint 5: Measurements Section
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(R.string.detail_section_measurements_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Dimensions et poids du spécimen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = weightGr,
                onValueChange = { viewModel.onWeightChange(it) },
                label = { Text(stringResource(R.string.field_weight)) },
                placeholder = { Text("Ex: 125.5") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )

            // Sprint 5: Price and Currency Section
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Valeur et Acquisition",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { viewModel.onPriceChange(it) },
                    label = { Text(stringResource(R.string.field_value)) },
                    placeholder = { Text("Ex: 150.00") },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { viewModel.onCurrencyChange(it) },
                    label = { Text(stringResource(R.string.field_value_currency)) },
                    placeholder = { Text("USD") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            // v3.0.0: Provenance Section
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.field_provenance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = provenanceCountry,
                onValueChange = { viewModel.onProvenanceCountryChange(it) },
                label = { Text(stringResource(R.string.field_provenance_country)) },
                placeholder = { Text("Ex: France") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = provenanceLocality,
                onValueChange = { viewModel.onProvenanceLocalityChange(it) },
                label = { Text(stringResource(R.string.field_provenance_locality)) },
                placeholder = { Text("Ex: Chamonix, Haute-Savoie") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = provenanceMine,
                onValueChange = { viewModel.onProvenanceMineChange(it) },
                label = { Text(stringResource(R.string.field_provenance_mine)) },
                placeholder = { Text("Ex: Mine du Mont-Blanc") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = provenanceDate,
                onValueChange = { viewModel.onProvenanceDateChange(it) },
                label = { Text(stringResource(R.string.field_provenance_date)) },
                placeholder = { Text("Ex: 2024-12-15") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

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

/**
 * v3.0: Displays a read-only property from reference mineral.
 */
@Composable
private fun PropertyDisplay(
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
