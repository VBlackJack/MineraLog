package net.meshcore.mineralog.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.screens.edit.PhotoItem
import java.io.File

@Composable
fun PhotoManager(
    photos: List<PhotoItem>,
    onAddFromGallery: (Uri) -> Unit,
    onTakePhoto: (Uri) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onUpdateCaption: (String, String) -> Unit,
    onUpdateType: (String, String) -> Unit,
    photosDir: File,
    modifier: Modifier = Modifier
) {
    var showPhotoTypeDialog by remember { mutableStateOf(false) }
    var selectedPhotoForTypeChange by remember { mutableStateOf<PhotoItem?>(null) }
    var showCaptionDialog by remember { mutableStateOf(false) }
    var selectedPhotoForCaption by remember { mutableStateOf<PhotoItem?>(null) }
    var editingCaption by remember { mutableStateOf("") }

    // Get context at composable scope
    val context = androidx.compose.ui.platform.LocalContext.current

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAddFromGallery(it) }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Photo was saved to the URI we provided
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add photo buttons - BUGFIX: Added explicit elevation and enabled state
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp), // BUGFIX: Add spacing from content below
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(  // BUGFIX: Changed from OutlinedButton to Button for better visibility
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                enabled = true  // BUGFIX: Explicitly enabled
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Open gallery")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery")
            }

            Button(  // BUGFIX: Changed from OutlinedButton to Button for better visibility
                onClick = {
                    try {
                        // Ensure photos directory exists
                        if (!photosDir.exists()) {
                            photosDir.mkdirs()
                        }

                        // Create temp file for camera
                        val photoFile = File(photosDir, "temp_${System.currentTimeMillis()}.jpg")
                        val photoUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "net.meshcore.mineralog.fileprovider",
                            photoFile
                        )
                        onTakePhoto(photoUri)
                        cameraLauncher.launch(photoUri)
                    } catch (e: Exception) {
                        // Log error but don't crash - could show a toast to user
                        android.util.Log.e("PhotoManager", "Failed to launch camera", e)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = true  // BUGFIX: Explicitly enabled
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Take photo")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Camera")
            }
        }

        // Photos grid
        if (photos.isNotEmpty()) {
            Text(
                text = pluralStringResource(R.plurals.photo_count, photos.size, photos.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    PhotoCard(
                        photo = photo,
                        onRemove = { onRemovePhoto(photo.id) },
                        onChangeType = {
                            selectedPhotoForTypeChange = photo
                            showPhotoTypeDialog = true
                        },
                        onEditCaption = {
                            selectedPhotoForCaption = photo
                            editingCaption = photo.caption ?: ""
                            showCaptionDialog = true
                        },
                        photosDir = photosDir
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "No photos",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.photo_manager_no_photos_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.photo_manager_no_photos_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // Photo type dialog
    selectedPhotoForTypeChange?.let { photo ->
        if (showPhotoTypeDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoTypeDialog = false },
                title = { Text("Photo Type") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PhotoTypeOption(
                            type = "NORMAL",
                            label = "Normal",
                            description = "Standard photo",
                            selected = photo.type == "NORMAL",
                            onClick = {
                                onUpdateType(photo.id, "NORMAL")
                                showPhotoTypeDialog = false
                            }
                        )
                        PhotoTypeOption(
                            type = "UV_SW",
                            label = "UV Shortwave",
                            description = "Shortwave UV photo",
                            selected = photo.type == "UV_SW",
                            onClick = {
                                onUpdateType(photo.id, "UV_SW")
                                showPhotoTypeDialog = false
                            }
                        )
                        PhotoTypeOption(
                            type = "UV_LW",
                            label = "UV Longwave",
                            description = "Longwave UV photo",
                            selected = photo.type == "UV_LW",
                            onClick = {
                                onUpdateType(photo.id, "UV_LW")
                                showPhotoTypeDialog = false
                            }
                        )
                        PhotoTypeOption(
                            type = "MACRO",
                            label = "Macro",
                            description = "Close-up macro photo",
                            selected = photo.type == "MACRO",
                            onClick = {
                                onUpdateType(photo.id, "MACRO")
                                showPhotoTypeDialog = false
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPhotoTypeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    // Caption dialog
    selectedPhotoForCaption?.let { photo ->
        if (showCaptionDialog) {
            AlertDialog(
                onDismissRequest = { showCaptionDialog = false },
                title = { Text("Edit Caption") },
                text = {
                    OutlinedTextField(
                        value = editingCaption,
                        onValueChange = { editingCaption = it },
                        label = { Text("Caption") },
                        placeholder = { Text("Add a caption for this photo") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onUpdateCaption(photo.id, editingCaption)
                            showCaptionDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCaptionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun PhotoCard(
    photo: PhotoItem,
    onRemove: () -> Unit,
    onChangeType: () -> Unit,
    onEditCaption: () -> Unit,
    photosDir: File
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val photoTypeLabel = when (photo.type) {
        "UV_SW" -> "UV Shortwave"
        "UV_LW" -> "UV Longwave"
        "MACRO" -> "Macro"
        else -> "Normal"
    }

    val photoDescription = buildString {
        append("Photo: $photoTypeLabel type")
        if (photo.caption != null && photo.caption.isNotBlank()) {
            append(". Caption: ${photo.caption}")
        } else {
            append(". No caption")
        }
    }

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(240.dp)
            .semantics {
                contentDescription = photoDescription
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.combinedClickable(
                onClick = { /* Single click - do nothing or show full screen */ },
                onLongClick = { showContextMenu = true }
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                ) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    if (photo.uri != null) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(photo.uri)
                                .crossfade(true)
                                .size(600, 600) // Constrain size for memory efficiency
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                .build(),
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (photo.isExisting) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(File(photosDir, photo.fileName))
                                .crossfade(true)
                                .size(600, 600) // Constrain size for memory efficiency
                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                .build(),
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Type badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
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

                // Caption and actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = photo.caption ?: "No caption",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (photo.caption == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onEditCaption,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit caption",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onChangeType,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = "Change type",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove photo",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // Context menu (long-press)
    DropdownMenu(
        expanded = showContextMenu,
        onDismissRequest = { showContextMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Properties") },
            onClick = {
                showContextMenu = false
                showPropertiesDialog = true
            },
            leadingIcon = {
                Icon(Icons.Default.Info, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text("Edit with external app") },
            onClick = {
                showContextMenu = false
                // Open photo in external editor
                val photoFile = if (photo.uri != null) {
                    // For new photos with URI, get the file from content resolver
                    null // Will use URI directly
                } else if (photo.isExisting) {
                    File(photosDir, photo.fileName)
                } else {
                    null
                }

                photoFile?.let { file ->
                    val photoUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "net.meshcore.mineralog.fileprovider",
                        file
                    )
                    val intent = android.content.Intent(android.content.Intent.ACTION_EDIT).apply {
                        setDataAndType(photoUri, "image/*")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(android.content.Intent.createChooser(intent, "Edit photo with"))
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "No photo editor found", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null)
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
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
        val photoFile = if (photo.isExisting) File(photosDir, photo.fileName) else null
        val fileSize = photoFile?.let { if (it.exists()) it.length() else 0L } ?: 0L
        val fileSizeKB = fileSize / 1024.0
        val fileSizeMB = fileSizeKB / 1024.0
        val fileSizeText = when {
            fileSizeMB >= 1.0 -> String.format("%.2f MB", fileSizeMB)
            fileSizeKB >= 1.0 -> String.format("%.2f KB", fileSizeKB)
            else -> "$fileSize bytes"
        }

        AlertDialog(
            onDismissRequest = { showPropertiesDialog = false },
            title = { Text("Photo Properties") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropertyRow("Type", photoTypeLabel)
                    PropertyRow("File name", photo.fileName)
                    if (photoFile?.exists() == true) {
                        PropertyRow("Size", fileSizeText)
                    }
                    if (photo.caption?.isNotBlank() == true) {
                        PropertyRow("Caption", photo.caption)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPropertiesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Photo?") },
            text = {
                Text("Are you sure you want to delete this photo? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onRemove()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PhotoTypeOption(
    type: String,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
