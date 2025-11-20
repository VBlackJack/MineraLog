package net.meshcore.mineralog.ui.screens.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.R
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.MineralType
import net.meshcore.mineralog.ui.components.PhotoViewer
import net.meshcore.mineralog.ui.components.ProvenanceSection
import net.meshcore.mineralog.ui.components.AggregatePropertiesSection
import net.meshcore.mineralog.ui.components.ComponentsSynthesisSection
import net.meshcore.mineralog.ui.components.v2.ComponentCard
import net.meshcore.mineralog.ui.screens.edit.PhotoItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineralDetailScreen(
    mineralId: String,
    onNavigateBack: () -> Unit,
    onEditClick: (String) -> Unit = {},
    onCameraClick: (String) -> Unit = {},
    onShowGallery: (String) -> Unit = {},
    viewModel: MineralDetailViewModel = viewModel(
        factory = MineralDetailViewModelFactory(
            LocalContext.current.applicationContext,
            mineralId,
            (LocalContext.current.applicationContext as MineraLogApplication).mineralRepository,
            (LocalContext.current.applicationContext as MineraLogApplication).resourceProvider
        )
    )
) {
    val mineral by viewModel.mineral.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val qrGenerationState by viewModel.qrGenerationState.collectAsState()

    // v2.0: Mineral type and components
    val mineralType by viewModel.mineralType.collectAsState()
    val components by viewModel.components.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Snackbar host state for showing messages
    val snackbarHostState = remember { SnackbarHostState() }

    // PDF creation launcher for QR labels
    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { viewModel.generateQrLabel(it) }
    }

    // Handle QR generation state
    LaunchedEffect(qrGenerationState) {
        when (val state = qrGenerationState) {
            is QrGenerationState.Success -> {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.qr_label_generated_success)
                )
                viewModel.resetQrGenerationState()
            }
            is QrGenerationState.Error -> {
                snackbarHostState.showSnackbar(
                    state.message
                )
                viewModel.resetQrGenerationState()
            }
            else -> {}
        }
    }

    // Photo viewer state - at screen level to cover everything including TopAppBar
    var showPhotoViewer by rememberSaveable { mutableStateOf(false) }
    var selectedPhotoIndex by rememberSaveable { mutableIntStateOf(0) }

    val photosDir = remember {
        File(context.filesDir, "photos")
    }

    // Convert Photos to PhotoItems for the viewer
    val photoItems = remember(mineral?.photos) {
        mineral?.photos?.map { photo ->
            PhotoItem(
                id = photo.id,
                fileName = photo.fileName,
                type = photo.type,
                caption = photo.caption,
                isExisting = true
            )
        } ?: emptyList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(mineral?.name ?: stringResource(R.string.detail_loading)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
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
                            contentDescription = stringResource(R.string.cd_take_photo),
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
                            contentDescription = stringResource(R.string.cd_edit),
                            tint = if (mineral != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
                    }
                    // QR Code generation button
                    IconButton(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val fileName = "qr_label_${mineral?.name?.replace(" ", "_") ?: "mineral"}_$timestamp.pdf"
                            createPdfLauncher.launch(fileName)
                        },
                        enabled = mineral != null && qrGenerationState !is QrGenerationState.Generating
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = stringResource(R.string.cd_generate_qr_code),
                            tint = if (mineral != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            }
                        )
                    }
                    // Delete button with confirmation
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = mineral != null
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete),
                            tint = if (mineral != null) {
                                MaterialTheme.colorScheme.error
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
                onPhotoClick = { index ->
                    selectedPhotoIndex = index
                    showPhotoViewer = true
                },
                onShowGallery = { onShowGallery(mineralId) },
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
                    text = stringResource(R.string.detail_loading_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

        // Full-screen photo viewer - covers everything including TopAppBar
        if (showPhotoViewer && photoItems.isNotEmpty()) {
            PhotoViewer(
                photos = photoItems,
                initialPhotoIndex = selectedPhotoIndex,
                photosDir = photosDir,
                onDismiss = { showPhotoViewer = false }
            )
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.dialog_delete_title)) },
                text = {
                    Text(stringResource(R.string.dialog_delete_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteMineral()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // Handle delete state changes
        LaunchedEffect(deleteState) {
            when (deleteState) {
                is DeleteState.Success -> {
                    viewModel.resetDeleteState()
                    onNavigateBack()
                }
                is DeleteState.Error -> {
                    // Error will be shown via Snackbar
                    viewModel.resetDeleteState()
                }
                else -> {}
            }
        }
    }  // Close Box
}

@Composable
fun MineralDetailContent(
    mineral: Mineral,
    onPhotoClick: (Int) -> Unit,
    onShowGallery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val application = context.applicationContext as net.meshcore.mineralog.MineraLogApplication
    val photosDir = remember {
        File(context.filesDir, "photos")
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Photos gallery
        if (mineral.photos.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.detail_photos_count, mineral.photos.size),
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = onShowGallery) {
                            Text(stringResource(R.string.action_view_all))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(mineral.photos, key = { _, photo -> photo.id }) { index, photo ->
                            // Per-photo menu state
                            var showContextMenu by remember(photo.id) { mutableStateOf(false) }
                            var showPropertiesDialog by remember(photo.id) { mutableStateOf(false) }
                            var showDeleteConfirmation by remember(photo.id) { mutableStateOf(false) }

                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(200.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            onPhotoClick(index)
                                        },
                                        onLongClick = {
                                            showContextMenu = true
                                        }
                                    )
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
                                    }  // Close Box with combinedClickable
                                }  // Close Card

                            // Context menu - placed after Card in LazyRow item
                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { showContextMenu = false }
                            ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.detail_menu_properties)) },
                                        onClick = {
                                            showContextMenu = false
                                            showPropertiesDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Info, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.detail_menu_edit_external)) },
                                        onClick = {
                                            showContextMenu = false
                                            val photoFile = File(photosDir, photo.fileName)
                                            if (photoFile.exists()) {
                                                val photoUri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "net.meshcore.mineralog.fileprovider",
                                                    photoFile
                                                )
                                                val intent = android.content.Intent(android.content.Intent.ACTION_EDIT).apply {
                                                    setDataAndType(photoUri, "image/*")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                                }
                                                try {
                                                    context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.detail_menu_edit_external)))
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, context.getString(R.string.toast_no_photo_editor), android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.detail_menu_delete), color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showContextMenu = false
                                            showDeleteConfirmation = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    )
                                }

                                // Properties dialog
                                if (showPropertiesDialog) {
                                    val photoFile = File(photosDir, photo.fileName)
                                    val fileSize = if (photoFile.exists()) photoFile.length() else 0L
                                    val fileSizeKB = fileSize / 1024.0
                                    val fileSizeMB = fileSizeKB / 1024.0
                                    val fileSizeText = when {
                                        fileSizeMB >= 1.0 -> String.format("%.2f MB", fileSizeMB)
                                        fileSizeKB >= 1.0 -> String.format("%.2f KB", fileSizeKB)
                                        else -> "$fileSize bytes"
                                    }

                                    val photoTypeLabel = when (photo.type) {
                                        "UV_SW" -> context.getString(R.string.camera_photo_type_uv_sw)
                                        "UV_LW" -> context.getString(R.string.camera_photo_type_uv_lw)
                                        "MACRO" -> context.getString(R.string.camera_photo_type_macro)
                                        else -> context.getString(R.string.camera_photo_type_normal)
                                    }

                                    AlertDialog(
                                        onDismissRequest = { showPropertiesDialog = false },
                                        title = { Text(stringResource(R.string.detail_photo_properties_title)) },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                DetailRow(stringResource(R.string.detail_photo_property_type), photoTypeLabel)
                                                DetailRow(stringResource(R.string.detail_photo_property_filename), photo.fileName)
                                                if (photoFile.exists()) {
                                                    DetailRow(stringResource(R.string.detail_photo_property_size), fileSizeText)
                                                }
                                                if (photo.caption?.isNotBlank() == true) {
                                                    DetailRow(stringResource(R.string.detail_photo_property_caption), photo.caption!!)
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showPropertiesDialog = false }) {
                                                Text(stringResource(R.string.action_close))
                                            }
                                        }
                                    )
                                }

                                // Delete confirmation dialog
                                if (showDeleteConfirmation) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteConfirmation = false },
                                        title = { Text(stringResource(R.string.detail_delete_photo_title)) },
                                        text = {
                                            Text(stringResource(R.string.detail_delete_photo_message))
                                        },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    showDeleteConfirmation = false
                                                    // Delete photo using repository
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        application.mineralRepository.deletePhoto(photo.id)
                                                        // Delete physical file
                                                        val photoFile = File(photosDir, photo.fileName)
                                                        if (photoFile.exists()) {
                                                            photoFile.delete()
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                )
                                            ) {
                                                Text(stringResource(R.string.action_delete))
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDeleteConfirmation = false }) {
                                                Text(stringResource(R.string.action_cancel))
                                            }
                                        }
                                    )
                                }
                        }
                    }
                }
            }
        }

        // v3.1: Provenance Section (prominently displayed)
        ProvenanceSection(
            provenance = mineral.provenance,
            modifier = Modifier.fillMaxWidth()
        )

        // v3.1: Aggregate Properties Section (only for aggregates)
        AggregatePropertiesSection(
            mineral = mineral,
            modifier = Modifier.fillMaxWidth()
        )

        // v3.1: Components Synthesis Section (only for aggregates with components)
        ComponentsSynthesisSection(
            mineral = mineral,
            components = mineral.components,
            modifier = Modifier.fillMaxWidth()
        )

        // v2.0: Aggregate components section (only for aggregates)
        if (mineral.mineralType == MineralType.AGGREGATE && mineral.components.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Composition de l'agrégat",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${mineral.components.size} composant${if (mineral.components.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    mineral.components.forEach { component ->
                        ComponentCard(
                            component = component,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Basic info
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.detail_section_basic_info), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                mineral.group?.let { DetailRow(stringResource(R.string.detail_property_group), it) }
                mineral.formula?.let { DetailRow(stringResource(R.string.detail_property_formula), it) }
                mineral.crystalSystem?.let { DetailRow(stringResource(R.string.detail_property_crystal_system), it) }
            }
        }

        // Physical properties
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.detail_section_physical_props), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                mineral.mohsMin?.let { min ->
                    val max = mineral.mohsMax ?: min
                    DetailRow(stringResource(R.string.detail_property_mohs_hardness), if (min == max) "$min" else "$min - $max")
                }
                mineral.luster?.let { DetailRow(stringResource(R.string.detail_property_luster), it) }
                mineral.streak?.let { DetailRow(stringResource(R.string.detail_property_streak), it) }
                mineral.cleavage?.let { DetailRow(stringResource(R.string.detail_property_cleavage), it) }
                mineral.fracture?.let { DetailRow(stringResource(R.string.detail_property_fracture), it) }
                mineral.diaphaneity?.let { DetailRow(stringResource(R.string.detail_property_diaphaneity), it) }
                mineral.habit?.let { DetailRow(stringResource(R.string.detail_property_habit), it) }
                mineral.specificGravity?.let { DetailRow(stringResource(R.string.detail_property_specific_gravity), it.toString()) }
            }
        }

        // Special properties
        if (mineral.fluorescence != null || mineral.magnetic || mineral.radioactive) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.detail_section_special_props), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    mineral.fluorescence?.let { DetailRow(stringResource(R.string.detail_property_fluorescence), it) }
                    if (mineral.magnetic) DetailRow(stringResource(R.string.detail_property_magnetic), stringResource(R.string.detail_property_magnetic_yes))
                    if (mineral.radioactive) DetailRow(stringResource(R.string.detail_property_radioactive), stringResource(R.string.detail_property_radioactive_yes))
                }
            }
        }

        // Measurements
        if (mineral.dimensionsMm != null || mineral.weightGr != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.detail_section_measurements_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    mineral.dimensionsMm?.let { DetailRow(stringResource(R.string.detail_property_dimensions), stringResource(R.string.detail_property_dimensions_mm, it)) }
                    mineral.weightGr?.let { DetailRow(stringResource(R.string.detail_property_weight), stringResource(R.string.detail_property_weight_g, it)) }
                }
            }
        }

        // Provenance
        mineral.provenance?.let { prov ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.detail_section_provenance_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    prov.country?.let { DetailRow(stringResource(R.string.detail_property_country), it) }
                    prov.locality?.let { DetailRow(stringResource(R.string.detail_property_locality), it) }
                    prov.site?.let { DetailRow(stringResource(R.string.detail_property_site), it) }
                    prov.source?.let { DetailRow(stringResource(R.string.detail_property_source), it) }
                    prov.acquiredAt?.let { DetailRow(stringResource(R.string.detail_property_acquired), it.toString().substring(0, 10)) }
                }
            }
        }

        // Storage
        mineral.storage?.let { stor ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.detail_section_storage_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    stor.place?.let { DetailRow(stringResource(R.string.detail_property_place), it) }
                    stor.container?.let { DetailRow(stringResource(R.string.detail_property_container), it) }
                    stor.box?.let { DetailRow(stringResource(R.string.detail_property_box), it) }
                    stor.slot?.let { DetailRow(stringResource(R.string.detail_property_slot), it) }
                }
            }
        }

        // Notes
        mineral.notes?.let { notes ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.detail_section_notes_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(notes)
                }
            }
        }
    }
    }  // Close Box
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
