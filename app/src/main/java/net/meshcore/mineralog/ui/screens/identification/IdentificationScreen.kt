package net.meshcore.mineralog.ui.screens.identification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.screens.identification.utils.ImageAnalyzer

// Common color options for filtering
val COMMON_COLORS = listOf(
    "White", "Black", "Gray", "Red", "Pink",
    "Orange", "Yellow", "Green", "Blue", "Purple",
    "Brown", "Colorless"
)

// Common streak colors
val COMMON_STREAKS = listOf(
    "White", "Black", "Gray", "Red", "Brown",
    "Yellow", "Green", "Blue", "Colorless"
)

// Common luster types
val COMMON_LUSTERS = listOf(
    "Vitreous", "Metallic", "Pearly", "Silky",
    "Resinous", "Adamantine", "Greasy", "Dull", "Earthy"
)

/**
 * Maps color names to Compose Color values for visual indicators.
 * Used to display color chips with accurate visual representation.
 *
 * @param name The color name from COMMON_COLORS
 * @return Compose Color corresponding to the color name
 */
private fun getColorForName(name: String): Color {
    return when (name) {
        "White" -> Color.White
        "Black" -> Color.Black
        "Gray" -> Color.Gray
        "Red" -> Color(0xFFDC143C)      // Crimson red
        "Pink" -> Color(0xFFFFC0CB)     // Pink
        "Orange" -> Color(0xFFFF8C00)   // Dark orange
        "Yellow" -> Color(0xFFFFD700)   // Gold
        "Green" -> Color(0xFF228B22)    // Forest green
        "Blue" -> Color(0xFF1E90FF)     // Dodger blue
        "Purple" -> Color(0xFF8A2BE2)   // Blue violet
        "Brown" -> Color(0xFF8B4513)    // Saddle brown
        "Colorless" -> Color(0xFFF5F5F5) // Very light gray (almost transparent)
        else -> Color.Gray
    }
}

/**
 * Converts an image URI to a Bitmap, handling different Android API versions.
 * Uses ImageDecoder for API 28+ and MediaStore for older versions.
 *
 * @param context Android context for content resolver access
 * @param uri URI of the image to convert
 * @return Bitmap representation of the image, or null if conversion fails
 * @throws Exception if the URI is invalid or image cannot be decoded
 */
suspend fun uriToBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // API 28+ uses ImageDecoder
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            // CRITICAL FIX: Force SOFTWARE allocator to allow CPU pixel access (getPixel)
            // Default HARDWARE allocator stores bitmap in GPU memory, making pixels unreadable
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            // Legacy API uses MediaStore
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        // Return null if conversion fails (invalid URI, file deleted, etc.)
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentificationScreen(
    onNavigateBack: () -> Unit,
    onMineralClick: (String) -> Unit,
    viewModel: IdentificationViewModel = viewModel(
        factory = IdentificationViewModelFactory(
            referenceMineralRepository = (LocalContext.current.applicationContext as MineraLogApplication).referenceMineralRepository
        )
    )
) {
    val filter by viewModel.filter.collectAsState()
    val results by viewModel.filteredResults.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()
    var filterExpanded by remember { mutableStateOf(true) }

    // Snackbar state for user feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Get string resources for messages
    val context = LocalContext.current
    val colorDetectedMsg = stringResource(R.string.identification_color_detected)
    val colorAlreadySelectedMsg = stringResource(R.string.identification_color_already_selected)
    val detectFailedMsg = stringResource(R.string.identification_color_detect_failed)
    val analysisErrorMsg = stringResource(R.string.identification_analysis_error)

    // Common color analysis logic (DRY principle)
    val analyzeColorFromBitmap: (Bitmap) -> Unit = { bitmap ->
        scope.launch {
            try {
                // Perform image analysis on Default dispatcher (background thread)
                val detectedColor = withContext(Dispatchers.Default) {
                    ImageAnalyzer.detectDominantColorName(bitmap)
                }

                if (detectedColor != null) {
                    // Check if color is already selected
                    if (!filter.selectedColors.contains(detectedColor)) {
                        // Auto-select the detected color
                        viewModel.toggleColor(detectedColor)

                        // Show success feedback
                        snackbarHostState.showSnackbar(
                            message = colorDetectedMsg.format(detectedColor),
                            duration = SnackbarDuration.Short
                        )
                    } else {
                        // Color already selected
                        snackbarHostState.showSnackbar(
                            message = colorAlreadySelectedMsg.format(detectedColor),
                            duration = SnackbarDuration.Short
                        )
                    }
                } else {
                    // Detection failed
                    snackbarHostState.showSnackbar(
                        message = detectFailedMsg,
                        duration = SnackbarDuration.Short
                    )
                }
            } catch (e: Exception) {
                // Error during analysis
                snackbarHostState.showSnackbar(
                    message = analysisErrorMsg.format(e.message ?: "Unknown error"),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Camera launcher for photo-assisted identification
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { analyzeColorFromBitmap(it) }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, launch camera
            cameraLauncher.launch(null)
        } else {
            // Permission denied, show message
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.camera_permission_denied),
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Gallery launcher for photo-assisted identification
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // Convert URI to Bitmap and analyze
            scope.launch {
                try {
                    val bitmap = uriToBitmap(context, uri)
                    if (bitmap != null) {
                        analyzeColorFromBitmap(bitmap)
                        // Note: Don't recycle here - analyzeColorFromBitmap runs async
                        // ImageAnalyzer handles its own bitmap cleanup
                    } else {
                        // URI conversion failed
                        snackbarHostState.showSnackbar(
                            message = detectFailedMsg,
                            duration = SnackbarDuration.Short
                        )
                    }
                } catch (e: Exception) {
                    // Error loading image from gallery
                    snackbarHostState.showSnackbar(
                        message = analysisErrorMsg.format(e.message ?: "Unknown error"),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.identification_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    // Photo-assisted identification button (Camera)
                    IconButton(onClick = {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = stringResource(R.string.identification_photo_assist)
                        )
                    }
                    // Gallery picker for photo-assisted identification
                    IconButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.identification_gallery_assist)
                        )
                    }
                    // Toggle filter visibility
                    IconButton(onClick = { filterExpanded = !filterExpanded }) {
                        Icon(
                            if (filterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (filterExpanded) "Collapse filters" else "Expand filters"
                        )
                    }
                    // Clear all filters
                    if (!filter.isEmpty()) {
                        IconButton(onClick = { viewModel.clearFilters() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear filters")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter section (collapsible)
            AnimatedVisibility(
                visible = filterExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FilterSection(
                    filter = filter,
                    onColorToggle = { viewModel.toggleColor(it) },
                    onHardnessChange = { min, max -> viewModel.setHardnessRange(min, max) },
                    onStreakChange = { viewModel.setStreak(it) },
                    onLusterChange = { viewModel.setLuster(it) },
                    onMagneticChange = { viewModel.setMagnetic(it) }
                )
            }

            HorizontalDivider()

            // Results section
            when (loadingState) {
                is LoadingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is LoadingState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (loadingState as LoadingState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is LoadingState.Success -> {
                    ResultsSection(
                        results = results,
                        isEmpty = filter.isEmpty(),
                        onMineralClick = onMineralClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSection(
    filter: IdentificationFilter,
    onColorToggle: (String) -> Unit,
    onHardnessChange: (Float?, Float?) -> Unit,
    onStreakChange: (String?) -> Unit,
    onLusterChange: (String?) -> Unit,
    onMagneticChange: (Boolean?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Color filter (primary criterion)
        item {
            Column {
                Text(
                    text = stringResource(R.string.identification_filter_color),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    COMMON_COLORS.forEach { color ->
                        FilterChip(
                            selected = filter.selectedColors.contains(color),
                            onClick = { onColorToggle(color) },
                            label = { Text(color) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(getColorForName(color))
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFF9E9E9E), // Gray border
                                            shape = CircleShape
                                        )
                                )
                            }
                        )
                    }
                }
            }
        }

        // Hardness filter
        item {
            HardnessFilter(
                currentMin = filter.mohsMin,
                currentMax = filter.mohsMax,
                onRangeChange = onHardnessChange
            )
        }

        // Streak filter
        item {
            DropdownFilter(
                label = stringResource(R.string.identification_filter_streak),
                options = COMMON_STREAKS,
                selected = filter.selectedStreak,
                onSelect = onStreakChange
            )
        }

        // Luster filter
        item {
            DropdownFilter(
                label = stringResource(R.string.identification_filter_luster),
                options = COMMON_LUSTERS,
                selected = filter.selectedLuster,
                onSelect = onLusterChange
            )
        }

        // Magnetism filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.identification_filter_magnetic),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filter.isMagnetic == true,
                        onClick = {
                            onMagneticChange(if (filter.isMagnetic == true) null else true)
                        },
                        label = { Text(stringResource(R.string.yes)) }
                    )
                    FilterChip(
                        selected = filter.isMagnetic == false,
                        onClick = {
                            onMagneticChange(if (filter.isMagnetic == false) null else false)
                        },
                        label = { Text(stringResource(R.string.no)) }
                    )
                }
            }
        }
    }
}

@Composable
fun HardnessFilter(
    currentMin: Float?,
    currentMax: Float?,
    onRangeChange: (Float?, Float?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.identification_filter_hardness),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { showDialog = true }) {
                Text(
                    text = if (currentMin != null || currentMax != null) {
                        "${currentMin ?: 1.0} - ${currentMax ?: 10.0}"
                    } else {
                        stringResource(R.string.identification_set_range)
                    }
                )
            }
        }

        // Preset buttons
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = currentMin == null && currentMax != null && currentMax <= 2.5f,
                    onClick = { onRangeChange(null, 2.5f) },
                    label = { Text(stringResource(R.string.identification_hardness_soft)) }
                )
            }
            item {
                FilterChip(
                    selected = currentMin != null && currentMin >= 5.5f && currentMax == null,
                    onClick = { onRangeChange(5.5f, null) },
                    label = { Text(stringResource(R.string.identification_hardness_hard)) }
                )
            }
            item {
                FilterChip(
                    selected = currentMin == null && currentMax == null,
                    onClick = { onRangeChange(null, null) },
                    label = { Text(stringResource(R.string.identification_hardness_any)) }
                )
            }
        }
    }

    // Custom range dialog
    if (showDialog) {
        HardnessRangeDialog(
            currentMin = currentMin,
            currentMax = currentMax,
            onDismiss = { showDialog = false },
            onConfirm = { min, max ->
                onRangeChange(min, max)
                showDialog = false
            }
        )
    }
}

@Composable
fun HardnessRangeDialog(
    currentMin: Float?,
    currentMax: Float?,
    onDismiss: () -> Unit,
    onConfirm: (Float?, Float?) -> Unit
) {
    var minValue by remember { mutableFloatStateOf(currentMin ?: 1f) }
    var maxValue by remember { mutableFloatStateOf(currentMax ?: 10f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.identification_hardness_range_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "${stringResource(R.string.identification_min)}: ${String.format("%.1f", minValue)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = minValue,
                    onValueChange = { minValue = it.coerceAtMost(maxValue) },
                    valueRange = 1f..10f,
                    steps = 17 // 0.5 steps
                )

                Text(
                    text = "${stringResource(R.string.identification_max)}: ${String.format("%.1f", maxValue)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = maxValue,
                    onValueChange = { maxValue = it.coerceAtLeast(minValue) },
                    valueRange = 1f..10f,
                    steps = 17
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(minValue, maxValue) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun DropdownFilter(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = selected ?: stringResource(R.string.identification_any),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.identification_any)) },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    leadingIcon = if (selected == option) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun ResultsSection(
    results: List<MineralResult>,
    isEmpty: Boolean,
    onMineralClick: (String) -> Unit
) {
    when {
        isEmpty -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.identification_select_criteria),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        results.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.identification_no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.identification_try_different),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "${results.size} ${stringResource(R.string.identification_results_found)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(results) { result ->
                    MineralResultCard(
                        result = result,
                        onClick = { onMineralClick(result.mineral.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MineralResultCard(
    result: MineralResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.mineral.nameFr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!result.mineral.formula.isNullOrEmpty()) {
                    Text(
                        text = result.mineral.formula,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (result.mineral.mohsMin != null || result.mineral.mohsMax != null) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "${stringResource(R.string.identification_mohs)}: ${result.mineral.mohsMin ?: "?"}-${result.mineral.mohsMax ?: "?"}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                    // Relevance score badge
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "${stringResource(R.string.identification_match)}: ${result.relevanceScore}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Extension function to check if filter is empty
private fun IdentificationFilter.isEmpty(): Boolean {
    return selectedColors.isEmpty() &&
            mohsMin == null &&
            mohsMax == null &&
            selectedStreak == null &&
            selectedLuster == null &&
            isMagnetic == null
}
