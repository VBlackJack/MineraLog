package net.meshcore.mineralog.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.domain.model.Mineral

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineralDetailScreen(
    mineralId: String,
    onNavigateBack: () -> Unit,
    viewModel: MineralDetailViewModel = viewModel(
        factory = MineralDetailViewModelFactory(
            mineralId,
            (LocalContext.current.applicationContext as MineraLogApplication).mineralRepository
        )
    )
) {
    val mineral by viewModel.mineral.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mineral?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Edit */ }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = { /* TODO: Share */ }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                }
            )
        }
    ) { padding ->
        mineral?.let { m ->
            MineralDetailContent(
                mineral = m,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun MineralDetailContent(
    mineral: Mineral,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basic info
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Basic Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                mineral.group?.let { DetailRow("Group", it) }
                mineral.formula?.let { DetailRow("Formula", it) }
                mineral.crystalSystem?.let { DetailRow("Crystal System", it) }
            }
        }

        // Physical properties
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Physical Properties", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                mineral.mohsMin?.let { min ->
                    val max = mineral.mohsMax ?: min
                    DetailRow("Mohs Hardness", if (min == max) "$min" else "$min - $max")
                }
                mineral.luster?.let { DetailRow("Luster", it) }
                mineral.streak?.let { DetailRow("Streak", it) }
                mineral.cleavage?.let { DetailRow("Cleavage", it) }
                mineral.fracture?.let { DetailRow("Fracture", it) }
                mineral.diaphaneity?.let { DetailRow("Diaphaneity", it) }
                mineral.habit?.let { DetailRow("Habit", it) }
                mineral.specificGravity?.let { DetailRow("Specific Gravity", it.toString()) }
            }
        }

        // Special properties
        if (mineral.fluorescence != null || mineral.magnetic || mineral.radioactive) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Special Properties", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    mineral.fluorescence?.let { DetailRow("Fluorescence", it) }
                    if (mineral.magnetic) DetailRow("Magnetic", "Yes")
                    if (mineral.radioactive) DetailRow("Radioactive", "Yes")
                }
            }
        }

        // Measurements
        if (mineral.dimensionsMm != null || mineral.weightGr != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Measurements", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    mineral.dimensionsMm?.let { DetailRow("Dimensions", "$it mm") }
                    mineral.weightGr?.let { DetailRow("Weight", "$it g") }
                }
            }
        }

        // Provenance
        mineral.provenance?.let { prov ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Provenance", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    prov.country?.let { DetailRow("Country", it) }
                    prov.locality?.let { DetailRow("Locality", it) }
                    prov.site?.let { DetailRow("Site", it) }
                    prov.source?.let { DetailRow("Source", it) }
                    prov.acquiredAt?.let { DetailRow("Acquired", it.toString().substring(0, 10)) }
                }
            }
        }

        // Storage
        mineral.storage?.let { stor ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Storage Location", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    stor.place?.let { DetailRow("Place", it) }
                    stor.container?.let { DetailRow("Container", it) }
                    stor.box?.let { DetailRow("Box", it) }
                    stor.slot?.let { DetailRow("Slot", it) }
                }
            }
        }

        // Notes
        mineral.notes?.let { notes ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notes", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(notes)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
