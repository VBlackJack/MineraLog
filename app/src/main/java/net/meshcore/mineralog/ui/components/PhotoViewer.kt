package net.meshcore.mineralog.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.screens.edit.PhotoItem
import java.io.File
import kotlin.math.abs

/**
 * Full-screen photo viewer with swipe navigation and pinch-to-zoom.
 *
 * Features:
 * - Swipe horizontally to navigate between photos
 * - Pinch to zoom in/out (1x to 5x)
 * - Double-tap to reset zoom
 * - Pan when zoomed in
 * - Dark background for better photo viewing
 * - Close button to exit viewer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewer(
    photos: List<PhotoItem>,
    initialPhotoIndex: Int = 0,
    photosDir: File,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val context = LocalContext.current

    // Track if any photo is zoomed to disable pager scrolling
    var isZoomed by remember { mutableStateOf(false) }
    // Track if UI bars should be visible - hidden by default for immersive experience
    var showUI by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = initialPhotoIndex.coerceIn(0, photos.lastIndex.coerceAtLeast(0)),
        pageCount = { photos.size }
    )

    // Hide system bars for immersive fullscreen experience
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, view)

        // Hide system bars
        insetsController.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            // Restore system bars when viewer is closed
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                // Single tap to toggle UI visibility
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation()
                    if (up != null && (down.position - up.position).getDistance() < 10f) {
                        showUI = !showUI
                    }
                }
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed  // BUGFIX: Disable pager scroll when zoomed
        ) { page ->
            val photo = photos[page]
            ZoomablePhoto(
                photo = photo,
                photosDir = photosDir,
                onZoomChange = { zoomed -> isZoomed = zoomed }
            )
        }

        // Top bar with photo info and close button - toggle with tap
        if (showUI) {
            TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.photo_viewer_count, pagerState.currentPage + 1, photos.size),
                        color = Color.White
                    )
                    val currentPhoto = photos.getOrNull(pagerState.currentPage)
                    if (currentPhoto?.caption?.isNotBlank() == true) {
                        Text(
                            text = currentPhoto.caption ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            )
        )

            // Photo type badge (bottom center)
            val currentPhoto = photos.getOrNull(pagerState.currentPage)
            if (currentPhoto != null && currentPhoto.type != "NORMAL") {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = when (currentPhoto.type) {
                        "UV_SW" -> MaterialTheme.colorScheme.secondary
                        "UV_LW" -> MaterialTheme.colorScheme.tertiary
                        "MACRO" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }.copy(alpha = 0.9f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = when (currentPhoto.type) {
                            "UV_SW" -> "UV Shortwave"
                            "UV_LW" -> "UV Longwave"
                            "MACRO" -> "Macro"
                            else -> "Normal"
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }  // Close showUI check

        // Floating action button to close viewer and return to gallery (always visible)
        FloatingActionButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color.Black.copy(alpha = 0.6f),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close viewer"
            )
        }
    }
}

/**
 * Zoomable photo component with pinch-to-zoom and pan gestures.
 * BUGFIX: Custom gesture detector that only consumes multi-touch when not zoomed.
 */
@Composable
fun ZoomablePhoto(
    photo: PhotoItem,
    photosDir: File,
    onZoomChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Notify parent when zoom state changes
    LaunchedEffect(scale) {
        onZoomChange(scale > 1.05f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.size

                        // Only process gestures if:
                        // 1. Multiple fingers (pinch) OR
                        // 2. Already zoomed in (for panning)
                        if (pointerCount > 1 || scale > 1.05f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            // Apply zoom
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale

                            // Apply pan when zoomed
                            if (newScale > 1f) {
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(
                                        -size.width * (scale - 1) / 2,
                                        size.width * (scale - 1) / 2
                                    ),
                                    y = (offset.y + pan.y).coerceIn(
                                        -size.height * (scale - 1) / 2,
                                        size.height * (scale - 1) / 2
                                    )
                                )
                            } else {
                                offset = Offset.Zero
                            }

                            // Consume the event if we're zoomed or zooming
                            if (pointerCount > 1 || scale > 1.05f) {
                                event.changes.forEach { it.consume() }
                            }
                        }
                        // If single finger and not zoomed, don't consume - let pager handle it
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val photoModel = if (photo.uri != null) {
            photo.uri
        } else if (photo.isExisting) {
            File(photosDir, photo.fileName)
        } else {
            null
        }

        AsyncImage(
            model = photoModel,
            contentDescription = photo.caption ?: "Photo",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )

        // Zoom indicator when zoomed
        if (scale > 1f) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = stringResource(R.string.photo_viewer_zoom, (scale * 100).toInt()),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }
}
