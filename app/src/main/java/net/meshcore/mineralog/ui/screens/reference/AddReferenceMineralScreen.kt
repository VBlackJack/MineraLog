package net.meshcore.mineralog.ui.screens.reference

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.components.TooltipDropdownField
import net.meshcore.mineralog.ui.components.MineralFieldTooltips
import net.meshcore.mineralog.ui.components.MineralFieldValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReferenceMineralScreen(
    onNavigateBack: () -> Unit,
    onMineralAdded: () -> Unit,
    viewModel: AddReferenceMineralViewModel = viewModel(
        factory = AddReferenceMineralViewModelFactory(
            (LocalContext.current.applicationContext as MineraLogApplication).referenceMineralRepository
        )
    )
) {
    val nameFr by viewModel.nameFr.collectAsState()
    val nameEn by viewModel.nameEn.collectAsState()
    val synonyms by viewModel.synonyms.collectAsState()
    val mineralGroup by viewModel.mineralGroup.collectAsState()
    val formula by viewModel.formula.collectAsState()
    val mohsMin by viewModel.mohsMin.collectAsState()
    val mohsMax by viewModel.mohsMax.collectAsState()
    val density by viewModel.density.collectAsState()
    val crystalSystem by viewModel.crystalSystem.collectAsState()
    val cleavage by viewModel.cleavage.collectAsState()
    val fracture by viewModel.fracture.collectAsState()
    val habit by viewModel.habit.collectAsState()
    val luster by viewModel.luster.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val diaphaneity by viewModel.diaphaneity.collectAsState()
    val fluorescence by viewModel.fluorescence.collectAsState()
    val magnetism by viewModel.magnetism.collectAsState()
    val radioactivity by viewModel.radioactivity.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val source by viewModel.source.collectAsState()
    
    val saveState by viewModel.saveState.collectAsState()
    val nameFrError by viewModel.nameFrError.collectAsState()
    val nameEnError by viewModel.nameEnError.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Load dropdown values
    val crystalSystems = remember { MineralFieldValues.getCrystalSystems(context) }
    val lusterTypes = remember { MineralFieldValues.getLusterTypes(context) }
    val diaphaneityTypes = remember { MineralFieldValues.getDiaphaneityTypes(context) }
    val cleavageTypes = remember { MineralFieldValues.getCleavageTypes(context) }
    val fractureTypes = remember { MineralFieldValues.getFractureTypes(context) }
    val habitTypes = remember { MineralFieldValues.getHabitTypes(context) }
    val streakColors = remember { MineralFieldValues.getStreakColors(context) }

    // Handle save state
    LaunchedEffect(saveState) {
        when (saveState) {
            is AddReferenceMineralViewModel.SaveState.Success -> {
                onMineralAdded()
            }
            is AddReferenceMineralViewModel.SaveState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (saveState as AddReferenceMineralViewModel.SaveState.Error).message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un minéral de référence") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = saveState !is AddReferenceMineralViewModel.SaveState.Saving
                    ) {
                        if (saveState is AddReferenceMineralViewModel.SaveState.Saving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Enregistrer")
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
            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Ce minéral sera ajouté à votre bibliothèque personnelle et marqué comme 'Personnalisé'",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Section: Identification
            Text(
                text = "Identification",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = nameFr,
                onValueChange = { viewModel.onNameFrChange(it) },
                label = { Text("Nom français *") },
                isError = nameFrError != null,
                supportingText = nameFrError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = nameEn,
                onValueChange = { viewModel.onNameEnChange(it) },
                label = { Text("Nom anglais *") },
                isError = nameEnError != null,
                supportingText = nameEnError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = synonyms,
                onValueChange = { viewModel.onSynonymsChange(it) },
                label = { Text("Synonymes") },
                placeholder = { Text("Séparés par des virgules") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = mineralGroup,
                onValueChange = { viewModel.onMineralGroupChange(it) },
                label = { Text("Groupe minéralogique") },
                placeholder = { Text("Ex: Silicates, Carbonates...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            HorizontalDivider()

            // Section: Chemistry
            Text(
                text = "Chimie",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = formula,
                onValueChange = { viewModel.onFormulaChange(it) },
                label = { Text(stringResource(R.string.field_formula)) },
                placeholder = { Text("Ex: SiO₂, CaCO₃...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            HorizontalDivider()

            // Section: Physical Properties
            Text(
                text = "Propriétés physiques",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = mohsMin,
                    onValueChange = { viewModel.onMohsMinChange(it) },
                    label = { Text("Dureté min") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                OutlinedTextField(
                    value = mohsMax,
                    onValueChange = { viewModel.onMohsMaxChange(it) },
                    label = { Text("Dureté max") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )
            }

            OutlinedTextField(
                value = density,
                onValueChange = { viewModel.onDensityChange(it) },
                label = { Text("Densité (g/cm³)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                )
            )

            HorizontalDivider()

            // Section: Crystallographic Properties
            Text(
                text = "Propriétés cristallographiques",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
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
                value = habit,
                onValueChange = { viewModel.onHabitChange(it) },
                label = stringResource(R.string.field_habit_label),
                tooltipText = MineralFieldTooltips.HABIT,
                options = habitTypes,
                placeholder = stringResource(R.string.field_habit_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            HorizontalDivider()

            // Section: Optical Properties
            Text(
                text = "Propriétés optiques",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
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
                value = diaphaneity,
                onValueChange = { viewModel.onDiaphaneityChange(it) },
                label = stringResource(R.string.field_diaphaneity_label),
                tooltipText = MineralFieldTooltips.DIAPHANEITY,
                options = diaphaneityTypes,
                placeholder = stringResource(R.string.field_diaphaneity_placeholder),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            HorizontalDivider()

            // Section: Special Properties
            Text(
                text = "Propriétés spéciales",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = fluorescence,
                onValueChange = { viewModel.onFluorescenceChange(it) },
                label = { Text("Fluorescence") },
                placeholder = { Text("Ex: UV courte, UV longue...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = magnetism,
                onValueChange = { viewModel.onMagnetismChange(it) },
                label = { Text("Magnétisme") },
                placeholder = { Text("Ex: Non magnétique, Faible...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = radioactivity,
                onValueChange = { viewModel.onRadioactivityChange(it) },
                label = { Text("Radioactivité") },
                placeholder = { Text("Ex: Non radioactif, Faible...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            HorizontalDivider()

            // Section: Notes
            Text(
                text = "Notes et source",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text(stringResource(R.string.field_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            OutlinedTextField(
                value = source,
                onValueChange = { viewModel.onSourceChange(it) },
                label = { Text("Source des données") },
                placeholder = { Text("Ex: mindat.org, webmineral.com...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
