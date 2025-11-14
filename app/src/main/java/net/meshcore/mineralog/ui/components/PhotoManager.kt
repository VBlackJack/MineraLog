package net.meshcore.mineralog.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
        // Add photo buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gallery")
            }

            OutlinedButton(
                onClick = {
                    // Create temp file for camera
                    val photoFile = File(photosDir, "temp_${System.currentTimeMillis()}.jpg")
                    val photoUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "net.meshcore.mineralog.fileprovider",
                        photoFile
                    )
                    onTakePhoto(photoUri)
                    cameraLauncher.launch(photoUri)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Camera")
            }
        }

        // Photos grid
        if (photos.isNotEmpty()) {
            Text(
                text = "${photos.size} photo(s)",
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
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No photos yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add photos from gallery or camera",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // Photo type dialog
    if (showPhotoTypeDialog && selectedPhotoForTypeChange != null) {
        AlertDialog(
            onDismissRequest = { showPhotoTypeDialog = false },
            title = { Text("Photo Type") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoTypeOption(
                        type = "NORMAL",
                        label = "Normal",
                        description = "Standard photo",
                        selected = selectedPhotoForTypeChange?.type == "NORMAL",
                        onClick = {
                            onUpdateType(selectedPhotoForTypeChange!!.id, "NORMAL")
                            showPhotoTypeDialog = false
                        }
                    )
                    PhotoTypeOption(
                        type = "UV_SW",
                        label = "UV Shortwave",
                        description = "Shortwave UV photo",
                        selected = selectedPhotoForTypeChange?.type == "UV_SW",
                        onClick = {
                            onUpdateType(selectedPhotoForTypeChange!!.id, "UV_SW")
                            showPhotoTypeDialog = false
                        }
                    )
                    PhotoTypeOption(
                        type = "UV_LW",
                        label = "UV Longwave",
                        description = "Longwave UV photo",
                        selected = selectedPhotoForTypeChange?.type == "UV_LW",
                        onClick = {
                            onUpdateType(selectedPhotoForTypeChange!!.id, "UV_LW")
                            showPhotoTypeDialog = false
                        }
                    )
                    PhotoTypeOption(
                        type = "MACRO",
                        label = "Macro",
                        description = "Close-up macro photo",
                        selected = selectedPhotoForTypeChange?.type == "MACRO",
                        onClick = {
                            onUpdateType(selectedPhotoForTypeChange!!.id, "MACRO")
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

    // Caption dialog
    if (showCaptionDialog && selectedPhotoForCaption != null) {
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
                        onUpdateCaption(selectedPhotoForCaption!!.id, editingCaption)
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

@Composable
fun PhotoCard(
    photo: PhotoItem,
    onRemove: () -> Unit,
    onChangeType: () -> Unit,
    onEditCaption: () -> Unit,
    photosDir: File
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(240.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column(modifier = Modifier.fillMaxSize()) {
                // Photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                ) {
                    if (photo.uri != null) {
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = "Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (photo.isExisting) {
                        AsyncImage(
                            model = File(photosDir, photo.fileName),
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
                            onClick = onRemove,
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
