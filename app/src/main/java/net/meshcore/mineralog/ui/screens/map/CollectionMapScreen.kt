package net.meshcore.mineralog.ui.screens.map

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import net.meshcore.mineralog.R

/**
 * Collection Map Screen - Displays all geolocated minerals on a Google Map.
 *
 * Features:
 * - Shows markers for each mineral with valid latitude/longitude
 * - Custom info window with mineral name and photo preview
 * - Auto-centers camera to show all markers using LatLngBounds
 * - Click on info window to navigate to mineral detail
 * - Empty state message when no geolocated minerals exist
 *
 * @param onNavigateBack Callback to return to previous screen
 * @param onMineralClick Callback with mineral ID to navigate to detail screen
 * @param viewModel Injected ViewModel managing geolocated minerals data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionMapScreen(
    onNavigateBack: () -> Unit,
    onMineralClick: (String) -> Unit,
    viewModel: CollectionMapViewModel = hiltViewModel()
) {
    val minerals by viewModel.geolocatedMinerals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.collection_map_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // Loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                minerals.isEmpty() -> {
                    // Empty state
                    EmptyMapState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // Map with markers
                    MineralCollectionMap(
                        minerals = minerals,
                        onMineralClick = onMineralClick
                    )
                }
            }
        }
    }
}

/**
 * Google Map displaying mineral markers with custom info windows.
 *
 * Camera behavior:
 * - If only one mineral: centers on that mineral with zoom 12
 * - If multiple minerals: calculates LatLngBounds to show all markers with 100dp padding
 *
 * @param minerals List of geolocated minerals to display
 * @param onMineralClick Callback invoked when info window is clicked
 */
@Composable
private fun MineralCollectionMap(
    minerals: List<MapMineralItem>,
    onMineralClick: (String) -> Unit
) {
    // Calculate initial camera position to show all markers
    val initialCameraPosition = remember(minerals) {
        if (minerals.isEmpty()) {
            // Default to Europe center if no minerals
            CameraPosition.fromLatLngZoom(LatLng(48.8566, 2.3522), 5f)
        } else if (minerals.size == 1) {
            // Single mineral: center on it with moderate zoom
            val mineral = minerals.first()
            CameraPosition.fromLatLngZoom(
                LatLng(mineral.latitude, mineral.longitude),
                12f
            )
        } else {
            // Multiple minerals: calculate bounds to show all
            val boundsBuilder = LatLngBounds.Builder()
            minerals.forEach { mineral ->
                boundsBuilder.include(LatLng(mineral.latitude, mineral.longitude))
            }
            val bounds = boundsBuilder.build()

            // Start at first mineral's position, then animate to show all
            CameraPosition.fromLatLngZoom(
                LatLng(minerals.first().latitude, minerals.first().longitude),
                5f
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = initialCameraPosition
    }

    // Animate camera to show all markers on first load
    LaunchedEffect(minerals) {
        if (minerals.size > 1) {
            val boundsBuilder = LatLngBounds.Builder()
            minerals.forEach { mineral ->
                boundsBuilder.include(LatLng(mineral.latitude, mineral.longitude))
            }
            val bounds = boundsBuilder.build()

            // Animate camera to show all markers with padding
            cameraPositionState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(
                    bounds,
                    100 // Padding in pixels
                )
            )
        }
    }

    // Track which marker's info window is currently open
    var selectedMineralId by remember { mutableStateOf<String?>(null) }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = false,
            mapStyleOptions = null
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false,
            compassEnabled = true,
            mapToolbarEnabled = false
        )
    ) {
        // Display marker for each mineral
        minerals.forEach { mineral ->
            val position = LatLng(mineral.latitude, mineral.longitude)

            Marker(
                state = MarkerState(position = position),
                title = mineral.name,
                onClick = {
                    // Open info window for this marker
                    selectedMineralId = mineral.id
                    false // Return false to show default info window behavior
                }
            )

            // Custom info window displayed when marker is selected
            if (selectedMineralId == mineral.id) {
                MarkerInfoWindowContent(
                    state = MarkerState(position = position)
                ) {
                    MineralInfoWindow(
                        mineral = mineral,
                        onClick = { onMineralClick(mineral.id) }
                    )
                }
            }
        }
    }
}

/**
 * Custom info window composable for mineral markers.
 * Shows mineral name and photo preview in a card-style layout.
 *
 * @param mineral The mineral data to display
 * @param onClick Callback invoked when the info window is clicked
 */
@Composable
private fun MineralInfoWindow(
    mineral: MapMineralItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mineral photo (if available)
            if (mineral.mainPhotoUri != null) {
                AsyncImage(
                    model = Uri.parse(mineral.mainPhotoUri),
                    contentDescription = stringResource(R.string.mineral_photo_description, mineral.name),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Mineral name
            Text(
                text = mineral.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Click hint
            Text(
                text = stringResource(R.string.collection_map_tap_for_details),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Empty state displayed when no geolocated minerals exist in the collection.
 */
@Composable
private fun EmptyMapState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.collection_map_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.collection_map_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
