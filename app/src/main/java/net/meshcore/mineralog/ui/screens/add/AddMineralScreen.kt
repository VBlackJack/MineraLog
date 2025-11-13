package net.meshcore.mineralog.ui.screens.add

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMineralScreen(
    onNavigateBack: () -> Unit,
    onMineralAdded: (String) -> Unit,
    viewModel: AddMineralViewModel = viewModel(
        factory = AddMineralViewModelFactory(
            (LocalContext.current.applicationContext as MineraLogApplication).mineralRepository
        )
    )
) {
    val name by viewModel.name.collectAsState()
    val group by viewModel.group.collectAsState()
    val formula by viewModel.formula.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val isSaving = saveState is SaveMineralState.Saving
    val hasUnsavedChanges = name.isNotBlank() || group.isNotBlank() || formula.isNotBlank() || notes.isNotBlank()
    var showDiscardDialog by remember { mutableStateOf(false) }

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
                supportingText = if (name.isBlank()) {
                    { Text("Name is required", color = MaterialTheme.colorScheme.error) }
                } else null
            )

            OutlinedTextField(
                value = group,
                onValueChange = { viewModel.onGroupChange(it) },
                label = { Text("Group") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = formula,
                onValueChange = { viewModel.onFormulaChange(it) },
                label = { Text("Chemical Formula") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.onNotesChange(it) },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8
            )
        }
    }
}
