package net.meshcore.mineralog.ui.screens.camera

import android.Manifest
import net.meshcore.mineralog.util.AppLogger
import android.content.Context
import net.meshcore.mineralog.util.AppLogger
import android.content.pm.PackageManager
import net.meshcore.mineralog.util.AppLogger
import android.net.Uri
import net.meshcore.mineralog.util.AppLogger
import android.view.ViewGroup
import net.meshcore.mineralog.util.AppLogger
import androidx.activity.compose.rememberLauncherForActivityResult
import net.meshcore.mineralog.util.AppLogger
import androidx.activity.result.contract.ActivityResultContracts
import net.meshcore.mineralog.util.AppLogger
import androidx.camera.core.*
import net.meshcore.mineralog.util.AppLogger
import androidx.camera.lifecycle.ProcessCameraProvider
import net.meshcore.mineralog.util.AppLogger
import androidx.camera.view.PreviewView
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.foundation.background
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.foundation.border
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.foundation.clickable
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.foundation.layout.*
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.foundation.shape.CircleShape
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.foundation.shape.RoundedCornerShape
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.material.icons.Icons
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.material.icons.filled.*
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.material3.*
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.runtime.*
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.Alignment
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.Modifier
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.draw.clip
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.graphics.Color
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.platform.LocalContext
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.platform.LocalLifecycleOwner
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.semantics.LiveRegionMode
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.semantics.Role
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.semantics.contentDescription
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.semantics.liveRegion
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.semantics.role
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.semantics.semantics
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.text.style.TextAlign
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.res.stringResource
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.util.AppLogger
import androidx.compose.ui.viewinterop.AndroidView
import net.meshcore.mineralog.util.AppLogger
import androidx.core.content.ContextCompat
import net.meshcore.mineralog.util.AppLogger
import net.meshcore.mineralog.R
import net.meshcore.mineralog.util.AppLogger
import net.meshcore.mineralog.data.local.entity.PhotoType
import net.meshcore.mineralog.util.AppLogger
import java.io.File
import net.meshcore.mineralog.util.AppLogger
import java.text.SimpleDateFormat
import net.meshcore.mineralog.util.AppLogger
import java.util.*
import net.meshcore.mineralog.util.AppLogger
import java.util.concurrent.Executors
import net.meshcore.mineralog.util.AppLogger

/**
 * Camera capture screen using CameraX.
 * Allows capturing photos with type selection (Normal, UV SW, UV LW, Macro).
 * Performance target: capture < 2s (Rule R3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureScreen(
    mineralId: String,
    onPhotoCaptured: (Uri, PhotoType) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var selectedPhotoType by remember { mutableStateOf(PhotoType.NORMAL) }
    var torchEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var showPhotoTypeMenu by remember { mutableStateOf(false) }
    var captureStatusMessage by remember { mutableStateOf("") }
    var photoTypeChangeMessage by remember { mutableStateOf("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Request permission on first composition
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Output directory for photos
    val outputDirectory = remember { getOutputDirectory(context, mineralId) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.camera_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    // Photo type selector
                    Box {
                        OutlinedButton(
                            onClick = { showPhotoTypeMenu = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(getPhotoTypeLabel(context, selectedPhotoType))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.camera_select_photo_type)
                            )
                        }

                        DropdownMenu(
                            expanded = showPhotoTypeMenu,
                            onDismissRequest = { showPhotoTypeMenu = false }
                        ) {
                            PhotoType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = getPhotoTypeLabel(context, type),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = getPhotoTypeDescription(context, type),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPhotoType = type
                                        showPhotoTypeMenu = false
                                        photoTypeChangeMessage = context.getString(R.string.camera_photo_type_changed, getPhotoTypeLabel(context, type))
                                    },
                                    leadingIcon = {
                                        if (selectedPhotoType == type) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = stringResource(R.string.camera_photo_type_selected)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Torch toggle
                    if (hasCameraPermission) {
                        IconButton(onClick = { torchEnabled = !torchEnabled }) {
                            Icon(
                                if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = stringResource(if (torchEnabled) R.string.camera_disable_flash else R.string.camera_enable_flash)
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !hasCameraPermission -> {
                    // Permission denied state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.camera_permission_required),
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.camera_permission_required),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Text(stringResource(R.string.camera_grant_permission))
                        }
                    }
                }
                else -> {
                    // Camera preview
                    CameraPreviewWithCapture(
                        torchEnabled = torchEnabled,
                        onImageCaptureReady = { capture ->
                            imageCapture = capture
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics {
                                contentDescription = context.getString(R.string.camera_preview_ready, getPhotoTypeLabel(context, selectedPhotoType))
                            }
                    )

                    // Live region for capture status announcements (invisible)
                    if (captureStatusMessage.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(0.dp)
                                .semantics {
                                    liveRegion = LiveRegionMode.Polite
                                    contentDescription = captureStatusMessage
                                }
                        )
                    }

                    // Live region for photo type change announcements (invisible)
                    if (photoTypeChangeMessage.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(0.dp)
                                .semantics {
                                    liveRegion = LiveRegionMode.Polite
                                    contentDescription = photoTypeChangeMessage
                                }
                        )
                    }

                    // Capture button at bottom
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Photo type indicator
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = getPhotoTypeLabel(context, selectedPhotoType),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Capture button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = context.getString(
                                        if (isCapturing) R.string.camera_capturing else R.string.camera_capture_photo
                                    )
                                }
                                .clickable(enabled = !isCapturing) {
                                    isCapturing = true
                                    captureStatusMessage = context.getString(R.string.camera_capturing_status)
                                    capturePhoto(
                                        context = context,
                                        imageCapture = imageCapture,
                                        outputDirectory = outputDirectory,
                                        photoType = selectedPhotoType,
                                        onSuccess = { uri ->
                                            captureStatusMessage = context.getString(R.string.camera_capture_success)
                                            onPhotoCaptured(uri, selectedPhotoType)
                                        },
                                        onError = {
                                            captureStatusMessage = context.getString(R.string.camera_capture_failed)
                                            isCapturing = false
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null, // Handled by parent Box semantics
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreviewWithCapture(
    torchEnabled: Boolean,
    onImageCaptureReady: (ImageCapture) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Image capture use case
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // BUGFIX: Use safe call - display may be null before view is attached
                .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                .build()

            // Notify parent that ImageCapture is ready
            onImageCaptureReady(imageCapture)

            // Camera selector (back camera)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                // Enable/disable torch
                if (camera.cameraInfo.hasFlashUnit()) {
                    camera.cameraControl.enableTorch(torchEnabled)
                }

            } catch (e: Exception) {
                AppLogger.e("CameraCapture", "Camera binding failed", e)
            }
        },
        modifier = modifier
    )
}

/**
 * Capture photo to file.
 * Performance: Target < 2s (Rule R3).
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    outputDirectory: File,
    photoType: PhotoType,
    onSuccess: (Uri) -> Unit,
    onError: () -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()

    imageCapture?.let { capture ->
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    AppLogger.d("CameraCapture", "Photo saved: $savedUri")
                    onSuccess(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    AppLogger.e("CameraCapture", "Photo capture failed", exception)
                    onError()
                }
            }
        )
    }
}

/**
 * Get output directory for photos.
 * Uses app-specific directory (no permissions needed on API 29+).
 */
private fun getOutputDirectory(context: Context, mineralId: String): File {
    val mediaDir = context.getExternalFilesDir(null)?.let {
        File(it, "media/$mineralId").apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
}

private fun getPhotoTypeLabel(context: android.content.Context, type: PhotoType): String {
    return when (type) {
        PhotoType.NORMAL -> context.getString(R.string.camera_photo_type_normal)
        PhotoType.UV_SW -> context.getString(R.string.camera_photo_type_uv_sw)
        PhotoType.UV_LW -> context.getString(R.string.camera_photo_type_uv_lw)
        PhotoType.MACRO -> context.getString(R.string.camera_photo_type_macro)
    }
}

private fun getPhotoTypeDescription(context: android.content.Context, type: PhotoType): String {
    return when (type) {
        PhotoType.NORMAL -> context.getString(R.string.camera_photo_type_normal_desc)
        PhotoType.UV_SW -> context.getString(R.string.camera_photo_type_uv_sw_desc)
        PhotoType.UV_LW -> context.getString(R.string.camera_photo_type_uv_lw_desc)
        PhotoType.MACRO -> context.getString(R.string.camera_photo_type_macro_desc)
    }
}
