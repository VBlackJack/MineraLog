package net.meshcore.mineralog.ui.screens.camera

import android.Manifest
import net.meshcore.mineralog.util.AppLogger
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.R
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.data.local.entity.PhotoType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * Sealed class representing camera capture states (P1-4: Enhanced error handling).
 */
sealed class CameraState {
    data object Idle : CameraState()
    data object Loading : CameraState()
    data class Success(val photoUri: Uri) : CameraState()
    data class Error(val message: String, val retry: Boolean = true) : CameraState()
}

/**
 * Camera capture screen using CameraX.
 * Allows capturing photos with type selection (Normal, UV SW, UV LW, Macro).
 * Performance target: capture < 2s (Rule R3).
 * P1-4: Enhanced error handling with sealed state classes.
 * P1-5: Lifecycle cleanup with DisposableEffect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraCaptureScreen(
    mineralId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = viewModel(
        factory = CameraViewModel.provideFactory(
            LocalContext.current.applicationContext as MineraLogApplication
        )
    )
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activity = context as? Activity

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    var selectedPhotoType by remember { mutableStateOf(PhotoType.NORMAL) }
    var torchEnabled by remember { mutableStateOf(false) }
    var showPhotoTypeMenu by remember { mutableStateOf(false) }
    var captureStatusMessage by remember { mutableStateOf("") }
    var photoTypeChangeMessage by remember { mutableStateOf("") }

    // P1-4: Enhanced state management
    var cameraState by remember { mutableStateOf<CameraState>(CameraState.Idle) }
    var cameraInitError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val saveState by viewModel.saveState.collectAsState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            // Check if permission was permanently denied
            permissionDeniedPermanently = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.CAMERA
            ) == false
        }
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

    // P1-4: Handle camera state changes and show errors
    LaunchedEffect(cameraState) {
        when (val state = cameraState) {
            is CameraState.Success -> {
                viewModel.saveCapturedPhoto(mineralId, state.photoUri, selectedPhotoType)
                cameraState = CameraState.Idle
            }
            is CameraState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    actionLabel = if (state.retry) context.getString(R.string.retry) else null
                )
            }
            else -> {}
        }
    }

    LaunchedEffect(saveState) {
        when (val state = saveState) {
            is PhotoSaveState.Success -> {
                captureStatusMessage = context.getString(R.string.camera_capture_success)
                viewModel.consumeSaveState()
                onNavigateBack()
            }
            is PhotoSaveState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.consumeSaveState()
            }
            else -> Unit
        }
    }

    // P1-4: Show camera initialization errors
    LaunchedEffect(cameraInitError) {
        cameraInitError?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.camera_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // P1-6: Migrated to AutoMirrored icon
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                        if (permissionDeniedPermanently) {
                            // Permission was permanently denied, show settings button
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.camera_open_settings))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.camera_permission_denied_help),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // First time denial or can still request
                            Button(onClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }) {
                                Text(stringResource(R.string.camera_grant_permission))
                            }
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
                        onError = { errorMessage ->
                            // P1-4: Enhanced error handling for camera init failures
                            cameraInitError = errorMessage
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
                                        when (cameraState) {
                                            is CameraState.Loading -> R.string.camera_capturing
                                            else -> R.string.camera_capture_photo
                                        }
                                    )
                                }
                                .clickable(enabled = cameraState !is CameraState.Loading) {
                                    // P1-4: Use sealed state class
                                    cameraState = CameraState.Loading
                                    captureStatusMessage = context.getString(R.string.camera_capturing_status)
                                    capturePhoto(
                                        context = context,
                                        imageCapture = imageCapture,
                                        outputDirectory = outputDirectory,
                                        photoType = selectedPhotoType,
                                        onSuccess = { uri ->
                                            captureStatusMessage = context.getString(R.string.camera_capture_success)
                                            cameraState = CameraState.Success(uri)
                                        },
                                        onError = { errorMsg ->
                                            // P1-4: Enhanced error messages with retry option
                                            captureStatusMessage = context.getString(R.string.camera_capture_failed)
                                            cameraState = CameraState.Error(errorMsg, retry = true)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // P1-4: Use sealed state for UI
                            if (cameraState is CameraState.Loading) {
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
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

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
            // P1-4: Enhanced error handling for camera provider
            val provider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                AppLogger.e("CameraCapture", "Failed to get camera provider", e)
                onError(context.getString(R.string.camera_init_failed))
                return@AndroidView
            }
            cameraProvider = provider

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
                provider.unbindAll()

                // Bind use cases to camera
                val camera = provider.bindToLifecycle(
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
                // P1-4: Enhanced error reporting
                AppLogger.e("CameraCapture", "Camera binding failed", e)
                val errorMessage = when {
                    e.message?.contains("camera", ignoreCase = true) == true ->
                        context.getString(R.string.camera_binding_failed)
                    e is IllegalArgumentException ->
                        context.getString(R.string.camera_invalid_config)
                    else ->
                        context.getString(R.string.camera_unknown_error, e.message ?: "Unknown")
                }
                onError(errorMessage)
            }
        },
        modifier = modifier
    )

    // P1-5: Lifecycle cleanup with DisposableEffect
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProvider?.unbindAll()
                AppLogger.d("CameraCapture", "Camera resources released")
            } catch (e: Exception) {
                AppLogger.e("CameraCapture", "Error releasing camera", e)
            }
        }
    }
}

/**
 * Capture photo to file.
 * Performance: Target < 2s (Rule R3).
 * P1-4: Enhanced error handling with specific error messages.
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    outputDirectory: File,
    photoType: PhotoType,
    onSuccess: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val executor = Executors.newSingleThreadExecutor()

    if (imageCapture == null) {
        onError(context.getString(R.string.camera_not_initialized))
        return
    }

    // P1-4: Check output directory exists
    if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
        onError(context.getString(R.string.camera_storage_error))
        return
    }

    val photoFile = try {
        File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )
    } catch (e: Exception) {
        AppLogger.e("CameraCapture", "Failed to create photo file", e)
        onError(context.getString(R.string.camera_file_creation_error))
        return
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                AppLogger.d("CameraCapture", "Photo saved: $savedUri")
                onSuccess(savedUri)
                // BUGFIX: Shutdown executor to prevent resource leak
                executor.shutdown()
            }

            override fun onError(exception: ImageCaptureException) {
                // P1-4: Provide specific error messages based on error code
                AppLogger.e("CameraCapture", "Photo capture failed", exception)
                val errorMessage = when (exception.imageCaptureError) {
                    ImageCapture.ERROR_CAMERA_CLOSED ->
                        context.getString(R.string.camera_closed_error)
                    ImageCapture.ERROR_CAPTURE_FAILED ->
                        context.getString(R.string.camera_capture_failed_error)
                    ImageCapture.ERROR_FILE_IO ->
                        context.getString(R.string.camera_file_io_error)
                    ImageCapture.ERROR_INVALID_CAMERA ->
                        context.getString(R.string.camera_invalid_camera_error)
                    else ->
                        context.getString(R.string.camera_capture_failed)
                }
                onError(errorMessage)
                // BUGFIX: Shutdown executor to prevent resource leak
                executor.shutdown()
            }
        }
    )
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
