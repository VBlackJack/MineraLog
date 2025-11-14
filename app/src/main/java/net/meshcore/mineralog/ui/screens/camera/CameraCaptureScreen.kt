package net.meshcore.mineralog.ui.screens.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import net.meshcore.mineralog.data.local.entity.PhotoType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

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
                title = { Text("Take Photo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Photo type selector
                    Box {
                        OutlinedButton(
                            onClick = { showPhotoTypeMenu = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(getPhotoTypeLabel(selectedPhotoType))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select photo type"
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
                                                text = getPhotoTypeLabel(type),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = getPhotoTypeDescription(type),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPhotoType = type
                                        showPhotoTypeMenu = false
                                    },
                                    leadingIcon = {
                                        if (selectedPhotoType == type) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected"
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
                                contentDescription = if (torchEnabled) "Disable flash" else "Enable flash"
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
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Camera permission is required to take photos",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Text("Grant Permission")
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
                        modifier = Modifier.fillMaxSize()
                    )

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
                                text = getPhotoTypeLabel(selectedPhotoType),
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
                                .clickable(enabled = !isCapturing) {
                                    isCapturing = true
                                    capturePhoto(
                                        context = context,
                                        imageCapture = imageCapture,
                                        outputDirectory = outputDirectory,
                                        photoType = selectedPhotoType,
                                        onSuccess = { uri ->
                                            onPhotoCaptured(uri, selectedPhotoType)
                                        },
                                        onError = {
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
                                    contentDescription = "Capture photo",
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
                .setTargetRotation(previewView.display.rotation)
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
                android.util.Log.e("CameraCapture", "Camera binding failed", e)
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
                    android.util.Log.d("CameraCapture", "Photo saved: $savedUri")
                    onSuccess(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    android.util.Log.e("CameraCapture", "Photo capture failed", exception)
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

private fun getPhotoTypeLabel(type: PhotoType): String {
    return when (type) {
        PhotoType.NORMAL -> "Normal"
        PhotoType.UV_SW -> "UV Shortwave"
        PhotoType.UV_LW -> "UV Longwave"
        PhotoType.MACRO -> "Macro"
    }
}

private fun getPhotoTypeDescription(type: PhotoType): String {
    return when (type) {
        PhotoType.NORMAL -> "Standard photo"
        PhotoType.UV_SW -> "Shortwave UV fluorescence"
        PhotoType.UV_LW -> "Longwave UV fluorescence"
        PhotoType.MACRO -> "Close-up macro shot"
    }
}
