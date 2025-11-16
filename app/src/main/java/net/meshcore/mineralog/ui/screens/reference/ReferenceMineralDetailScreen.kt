package net.meshcore.mineralog.ui.screens.reference

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import net.meshcore.mineralog.ui.components.SkeletonMineralDetail

/**
 * Reference Mineral Detail screen - displays complete information about a reference mineral.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceMineralDetailScreen(
    referenceMineralId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit = {},
    viewModel: ReferenceMineralDetailViewModel = viewModel(
        factory = ReferenceMineralDetailViewModelFactory(
            referenceMineralId = referenceMineralId,
            referenceMineralRepository = (LocalContext.current.applicationContext as MineraLogApplication).referenceMineralRepository
        )
    )
) {
    val mineral by viewModel.mineral.collectAsState()
    val simpleSpecimensCount by viewModel.simpleSpecimensCount.collectAsState()
    val componentsCount by viewModel.componentsCount.collectAsState()
    val totalUsageCount by viewModel.totalUsageCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Handle delete state changes
    LaunchedEffect(deleteState) {
        when (deleteState) {
            is ReferenceMineralDetailViewModel.DeleteState.ConfirmRequired -> {
                showDeleteConfirmDialog = true
            }
            is ReferenceMineralDetailViewModel.DeleteState.Success -> {
                onNavigateBack()
            }
            is ReferenceMineralDetailViewModel.DeleteState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (deleteState as ReferenceMineralDetailViewModel.DeleteState.Error).message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetDeleteState()
            }
            else -> {}
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && deleteState is ReferenceMineralDetailViewModel.DeleteState.ConfirmRequired) {
        val usageCount = (deleteState as ReferenceMineralDetailViewModel.DeleteState.ConfirmRequired).usageCount
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                viewModel.cancelDelete()
            },
            title = { Text("Confirmer la suppression") },
            text = {
                Column {
                    Text("Ce minéral est utilisé par $usageCount spécimen(s).")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "La suppression de ce minéral de référence supprimera également le lien de tous les spécimens qui l'utilisent.",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Êtes-vous sûr de vouloir continuer ?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.confirmDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    viewModel.cancelDelete()
                }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = mineral?.nameFr ?: "Détail",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    // Edit button - only for user-defined minerals
                    if (mineral?.isUserDefined == true) {
                        IconButton(onClick = { onNavigateToEdit(referenceMineralId) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifier",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    // Delete button - only for user-defined minerals
                    if (mineral?.isUserDefined == true) {
                        IconButton(
                            onClick = { viewModel.initiateDelete() },
                            enabled = deleteState !is ReferenceMineralDetailViewModel.DeleteState.Deleting
                        ) {
                            if (deleteState is ReferenceMineralDetailViewModel.DeleteState.Deleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                // Show skeleton loading
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    SkeletonMineralDetail()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Erreur",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error ?: "Erreur inconnue",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(
                            onClick = { viewModel.refresh() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Réessayer")
                        }
                    }
                }
            }
            mineral != null -> {
                ReferenceMineralDetailContent(
                    mineral = mineral!!,
                    simpleSpecimensCount = simpleSpecimensCount,
                    componentsCount = componentsCount,
                    totalUsageCount = totalUsageCount,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun ReferenceMineralDetailContent(
    mineral: ReferenceMineralEntity,
    simpleSpecimensCount: Int,
    componentsCount: Int,
    totalUsageCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = mineral.nameFr,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (mineral.nameEn != mineral.nameFr) {
                    Text(
                        text = mineral.nameEn,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                mineral.synonyms?.let {
                    Text(
                        text = "Synonymes: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Usage Statistics Section
        if (totalUsageCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Utilisation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Utilisé par $totalUsageCount spécimen(s):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (simpleSpecimensCount > 0) {
                        Text(
                            text = "• $simpleSpecimensCount spécimen(s) simple(s)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (componentsCount > 0) {
                        Text(
                            text = "• $componentsCount composant(s) d'agrégat(s)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Identification Section
        DetailSection(title = "Identification") {
            mineral.mineralGroup?.let {
                DetailItem(label = "Groupe", value = it)
            }
            mineral.formula?.let {
                DetailItem(label = "Formule chimique", value = it)
            }
            if (mineral.isUserDefined) {
                DetailItem(label = "Type", value = "Personnalisé", highlight = true)
            }
            mineral.source?.let {
                DetailItem(label = "Source", value = it)
            }
        }

        // Physical Properties Section
        DetailSection(title = "Propriétés Physiques") {
            if (mineral.mohsMin != null && mineral.mohsMax != null) {
                val hardnessText = if (mineral.mohsMin == mineral.mohsMax) {
                    "${mineral.mohsMin}"
                } else {
                    "${mineral.mohsMin} - ${mineral.mohsMax}"
                }
                DetailItem(label = "Dureté Mohs", value = hardnessText)
            }
            mineral.density?.let {
                DetailItem(label = "Densité", value = "$it g/cm³")
            }
        }

        // Crystallographic Properties Section
        DetailSection(title = "Propriétés Cristallographiques") {
            mineral.crystalSystem?.let {
                DetailItem(label = "Système cristallin", value = it)
            }
            mineral.cleavage?.let {
                DetailItem(label = "Clivage", value = it)
            }
            mineral.fracture?.let {
                DetailItem(label = "Fracture", value = it)
            }
            mineral.habit?.let {
                DetailItem(label = "Habitus", value = it)
            }
        }

        // Optical Properties Section
        DetailSection(title = "Propriétés Optiques") {
            mineral.luster?.let {
                DetailItem(label = "Éclat", value = it)
            }
            mineral.streak?.let {
                DetailItem(label = "Trace", value = it)
            }
            mineral.diaphaneity?.let {
                DetailItem(label = "Diaphanéité", value = it)
            }
        }

        // Special Properties Section
        if (mineral.fluorescence != null || mineral.magnetism != null || mineral.radioactivity != null) {
            DetailSection(title = "Propriétés Spéciales") {
                mineral.fluorescence?.let {
                    DetailItem(label = "Fluorescence", value = it)
                }
                mineral.magnetism?.let {
                    DetailItem(label = "Magnétisme", value = it)
                }
                mineral.radioactivity?.let {
                    DetailItem(label = "Radioactivité", value = it)
                }
            }
        }

        // Notes Section
        mineral.notes?.let { notes ->
            DetailSection(title = "Notes") {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            content()
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
