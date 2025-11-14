package net.meshcore.mineralog.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.domain.model.Mineral
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineralDetailScreen(
    mineralId: String,
    onNavigateBack: () -> Unit,
    onEditClick: (String) -> Unit = {},
    onCameraClick: (String) -> Unit = {},
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
                    // M2: Camera button for photo capture
                    IconButton(
                        onClick = { onCameraClick(mineralId) },
                        enabled = mineral != null
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Take photo",
                            tint = if (mineral != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
                    }
                    // Quick Win #1: Edit button for quick access
                    IconButton(
                        onClick = { onEditClick(mineralId) },
                        enabled = mineral != null
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit mineral",
                            tint = if (mineral != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
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
                .padding(padding)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Loading mineral details"
                },
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading mineral details...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MineralDetailContent(
    mineral: Mineral,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photosDir = remember {
        File(context.filesDir, "photos")
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Photos gallery
        if (mineral.photos.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Photos (${mineral.photos.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mineral.photos, key = { it.id }) { photo ->
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(200.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    ) {
                                        AsyncImage(
                                            model = File(photosDir, photo.fileName),
                                            contentDescription = photo.caption ?: "Mineral photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Type badge
                                        Surface(
                                            modifier = Modifier
                                                .align(androidx.compose.ui.Alignment.TopStart)
                                                .padding(8.dp),
                                            color = when (photo.type) {
                                                "UV_SW" -> MaterialTheme.colorScheme.secondary
                                                "UV_LW" -> MaterialTheme.colorScheme.tertiary
                                                "MACRO" -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = when (photo.type) {
                                                    "UV_SW" -> "UV-SW"
                                                    "UV_LW" -> "UV-LW"
                                                    "MACRO" -> "MACRO"
                                                    else -> "NORMAL"
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = when (photo.type) {
                                                    "UV_SW" -> MaterialTheme.colorScheme.onSecondary
                                                    "UV_LW" -> MaterialTheme.colorScheme.onTertiary
                                                    "MACRO" -> MaterialTheme.colorScheme.onPrimary
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }

                                    // Caption
                                    if (photo.caption != null) {
                                        Text(
                                            text = photo.caption,
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

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
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $value"
            },
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
