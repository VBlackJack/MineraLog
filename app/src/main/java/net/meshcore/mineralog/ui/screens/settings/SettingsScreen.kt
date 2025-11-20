package net.meshcore.mineralog.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.components.ImportResultDialog
import net.meshcore.mineralog.ui.screens.home.DecryptPasswordDialog
import net.meshcore.mineralog.ui.screens.home.EncryptPasswordDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            context = LocalContext.current.applicationContext,
            mineralRepository = (LocalContext.current.applicationContext as MineraLogApplication).mineralRepository,
            settingsRepository = (LocalContext.current.applicationContext as MineraLogApplication).settingsRepository,
            backupRepository = (LocalContext.current.applicationContext as MineraLogApplication).backupRepository
        )
    )
) {
    val activity = LocalContext.current as? ComponentActivity
    val copyPhotos by viewModel.copyPhotosToInternal.collectAsState()
    val encryptByDefault by viewModel.encryptByDefault.collectAsState()
    val language by viewModel.language.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val csvImportState by viewModel.csvImportState.collectAsState()
    val sampleDataState by viewModel.sampleDataState.collectAsState()
    val reanalysisState by viewModel.reanalysisState.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showEncryptDialog by remember { mutableStateOf(false) }
    var showDecryptDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showImportResultDialog by remember { mutableStateOf(false) }
    var showEncryptWarningDialog by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var lastImportResult by remember { mutableStateOf<net.meshcore.mineralog.data.repository.ImportResult?>(null) }
    var decryptAttempts by remember { mutableStateOf(3) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var operationStatusMessage by remember { mutableStateOf("") }

    // File picker for ZIP export
    val zipExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            pendingExportUri = it
            showEncryptDialog = true
        }
    }

    // File picker for ZIP import
    val zipImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingImportUri = it
            // Try import without password first
            viewModel.importBackup(it, password = null)
        }
    }

    // File picker for CSV import
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Import CSV with auto-detected column mapping
            viewModel.importCsv(it)
        }
    }

    // Handle CSV import state changes
    LaunchedEffect(csvImportState) {
        when (csvImportState) {
            is CsvImportState.Importing -> {
                operationStatusMessage = "Importing CSV data... Please wait"
            }
            is CsvImportState.Success -> {
                val result = (csvImportState as CsvImportState.Success).result
                lastImportResult = result
                operationStatusMessage = "CSV import completed. ${result.imported} minerals imported"

                // Show dialog with detailed results
                showImportResultDialog = true

                viewModel.resetCsvImportState()
            }
            is CsvImportState.Error -> {
                val errorMessage = (csvImportState as CsvImportState.Error).message
                operationStatusMessage = "CSV import failed: $errorMessage"
                snackbarHostState.showSnackbar(
                    message = "CSV import failed: $errorMessage",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetCsvImportState()
            }
            else -> {}
        }
    }

    // Handle export state changes
    LaunchedEffect(exportState) {
        when (exportState) {
            is BackupExportState.Exporting -> {
                operationStatusMessage = "Exporting backup... Please wait"
            }
            is BackupExportState.Success -> {
                operationStatusMessage = "Backup exported successfully"
                snackbarHostState.showSnackbar(
                    message = "Backup exported successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetExportState()
                pendingExportUri = null
            }
            is BackupExportState.Error -> {
                val errorMessage = (exportState as BackupExportState.Error).message
                val hasPermissionError = errorMessage.contains("permission", ignoreCase = true)
                val actionableMessage = when {
                    hasPermissionError ->
                        "Permission denied. Grant storage access to export backup."
                    errorMessage.contains("space", ignoreCase = true) ->
                        "Not enough storage space. Free up space and retry."
                    errorMessage.contains("no minerals", ignoreCase = true) ->
                        "No minerals to export. Add minerals first."
                    else -> "Export failed: $errorMessage"
                }
                operationStatusMessage = actionableMessage
                val result = snackbarHostState.showSnackbar(
                    message = actionableMessage,
                    actionLabel = if (hasPermissionError) "Open Settings" else null,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed && hasPermissionError) {
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
                viewModel.resetExportState()
                pendingExportUri = null
            }
            else -> {}
        }
    }

    // Handle import state changes
    LaunchedEffect(importState) {
        when (importState) {
            is BackupImportState.Importing -> {
                operationStatusMessage = "Importing backup... Please wait"
            }
            is BackupImportState.Success -> {
                val imported = (importState as BackupImportState.Success).imported
                operationStatusMessage = "Backup imported successfully. $imported minerals restored"
                snackbarHostState.showSnackbar(
                    message = "Backup imported successfully. $imported minerals restored.",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetImportState()
                pendingImportUri = null
                decryptAttempts = 3
            }
            is BackupImportState.PasswordRequired -> {
                operationStatusMessage = "Password required for encrypted backup"
                // Show password dialog
                showDecryptDialog = true
            }
            is BackupImportState.Error -> {
                val errorMessage = (importState as BackupImportState.Error).message
                val hasPermissionError = errorMessage.contains("permission", ignoreCase = true)
                val actionableMessage = when {
                    errorMessage.contains("decrypt", ignoreCase = true) ||
                    errorMessage.contains("password", ignoreCase = true) ->
                        "Incorrect password. ${decryptAttempts - 1} attempt${if (decryptAttempts - 1 != 1) "s" else ""} remaining."
                    errorMessage.contains("corrupt", ignoreCase = true) ->
                        "File is corrupted or invalid. Try a different backup."
                    errorMessage.contains("version", ignoreCase = true) ->
                        "Backup created with newer app version. Update app first."
                    errorMessage.contains("format", ignoreCase = true) ->
                        "Invalid file format. Select a valid MineraLog backup."
                    hasPermissionError ->
                        "Cannot read file. Grant storage access to import."
                    else -> "Import failed: $errorMessage"
                }
                operationStatusMessage = actionableMessage
                val result = snackbarHostState.showSnackbar(
                    message = actionableMessage,
                    actionLabel = if (hasPermissionError) "Open Settings" else null,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed && hasPermissionError) {
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }

                // Check if it's a wrong password error
                if (errorMessage.contains("decrypt", ignoreCase = true) ||
                    errorMessage.contains("password", ignoreCase = true)) {
                    decryptAttempts--
                    if (decryptAttempts > 0) {
                        // Show dialog again for retry
                        showDecryptDialog = true
                    } else {
                        // Max attempts reached
                        pendingImportUri = null
                        decryptAttempts = 3
                    }
                } else {
                    pendingImportUri = null
                }

                viewModel.resetImportState()
            }
            else -> {}
        }
    }

    // Handle sample data loading state
    LaunchedEffect(sampleDataState) {
        when (sampleDataState) {
            is SampleDataState.Loading -> {
                operationStatusMessage = "Loading sample data..."
            }
            is SampleDataState.Success -> {
                val count = (sampleDataState as SampleDataState.Success).count
                val message = context.getString(R.string.settings_sample_data_success, count)
                operationStatusMessage = message
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.resetSampleDataState()
            }
            is SampleDataState.Error -> {
                val errorMessage = (sampleDataState as SampleDataState.Error).message
                val message = context.getString(R.string.settings_sample_data_error, errorMessage)
                operationStatusMessage = message
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetSampleDataState()
            }
            else -> {}
        }
    }

    // Handle color reanalysis state
    LaunchedEffect(reanalysisState) {
        when (reanalysisState) {
            is ReanalysisState.Processing -> {
                val state = reanalysisState as ReanalysisState.Processing
                operationStatusMessage = "Analyzing colors... ${state.progress}/${state.total}"
            }
            is ReanalysisState.Success -> {
                val count = (reanalysisState as ReanalysisState.Success).count
                val message = "Color analysis complete. $count minerals updated."
                operationStatusMessage = message
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetReanalysisState()
            }
            is ReanalysisState.Error -> {
                val errorMessage = (reanalysisState as ReanalysisState.Error).message
                val message = "Color analysis failed: $errorMessage"
                operationStatusMessage = message
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long
                )
                viewModel.resetReanalysisState()
            }
            else -> {}
        }
    }

    // Live region for operation status announcements (invisible)
    if (operationStatusMessage.isNotEmpty()) {
        Box(
            modifier = Modifier
                .size(0.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = operationStatusMessage
                }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
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
                .verticalScroll(rememberScrollState())
        ) {
            // Backup & Restore Section
            SectionHeader(title = stringResource(R.string.settings_section_backup))

            // Export backup
            SettingsActionItem(
                icon = Icons.Default.CloudUpload,
                title = stringResource(R.string.action_backup),
                subtitle = stringResource(R.string.settings_export_subtitle),
                onClick = {
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                        .format(java.util.Date())
                    zipExportLauncher.launch("mineralog_backup_$timestamp.zip")
                }
            )

            // Import backup (ZIP)
            SettingsActionItem(
                icon = Icons.Default.Download,
                title = stringResource(R.string.action_restore),
                subtitle = stringResource(R.string.settings_import_subtitle),
                onClick = {
                    zipImportLauncher.launch("application/zip")
                }
            )

            // Import CSV
            SettingsActionItem(
                icon = Icons.Default.UploadFile,
                title = stringResource(R.string.settings_import_csv_title),
                subtitle = stringResource(R.string.settings_import_csv_subtitle),
                onClick = {
                    csvImportLauncher.launch("text/*")
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Language Selector
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageDropdown = true }
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when (language) {
                            "system" -> "System / Système"
                            "en" -> "English"
                            "fr" -> "Français"
                            else -> language.uppercase()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showLanguageDropdown,
                    onDismissRequest = { showLanguageDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("System / Système") },
                        onClick = {
                            viewModel.setLanguage("system")
                            showLanguageDropdown = false
                        },
                        leadingIcon = if (language == "system") {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                    DropdownMenuItem(
                        text = { Text("English") },
                        onClick = {
                            viewModel.setLanguage("en")
                            showLanguageDropdown = false
                        },
                        leadingIcon = if (language == "en") {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                    DropdownMenuItem(
                        text = { Text("Français") },
                        onClick = {
                            viewModel.setLanguage("fr")
                            showLanguageDropdown = false
                        },
                        leadingIcon = if (language == "fr") {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }

            HorizontalDivider()

            // Copy photos setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setCopyPhotos(!copyPhotos) }
                    .semantics(mergeDescendants = true) {}
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_copy_photos_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_copy_photos_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = copyPhotos,
                    onCheckedChange = { viewModel.setCopyPhotos(it) }
                )
            }

            HorizontalDivider()

            // Encrypt by default setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!encryptByDefault) {
                            // Show warning dialog before enabling
                            showEncryptWarningDialog = true
                        } else {
                            viewModel.setEncryptByDefault(false)
                        }
                    }
                    .semantics(mergeDescendants = true) {}
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_encrypt_default_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_encrypt_default_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = encryptByDefault,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // Show warning dialog before enabling
                            showEncryptWarningDialog = true
                        } else {
                            viewModel.setEncryptByDefault(false)
                        }
                    }
                )
            }

            HorizontalDivider()

            // Maintenance Section (v3.2.0)
            SectionHeader(title = "Maintenance")

            // Color reanalysis action
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsActionItem(
                        icon = Icons.Default.Palette,
                        title = "Reanalyze Colors",
                        subtitle = "Detect dominant colors for existing minerals with photos",
                        onClick = { viewModel.reanalyzeMineralColors() }
                    )

                    // Show progress indicator when processing
                    when (val state = reanalysisState) {
                        is ReanalysisState.Processing -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (state.total > 0) state.progress.toFloat() / state.total.toFloat() else 0f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Analyzing... ${state.progress}/${state.total}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        is ReanalysisState.Success -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✓ ${state.count} minerals updated",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        else -> {}
                    }
                }
            }

            HorizontalDivider()

            // Developer Section
            SectionHeader(title = stringResource(R.string.settings_section_developer))

            SettingsActionItem(
                icon = Icons.Default.Dataset,
                title = stringResource(R.string.settings_load_sample_data_title),
                subtitle = stringResource(R.string.settings_load_sample_data_subtitle),
                onClick = { viewModel.loadSampleData() }
            )

            HorizontalDivider()

            // About - Quick Win #5
            SettingsItem(
                title = stringResource(R.string.settings_about_title),
                subtitle = "MineraLog v${net.meshcore.mineralog.BuildConfig.VERSION_NAME}"
            ) {
                showAboutDialog = true
            }
        }
    }

    // Quick Win #5: About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "About MineraLog",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "MineraLog",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_about_version_number),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = stringResource(R.string.settings_about_description),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(stringResource(R.string.settings_about_feature_wcag), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.settings_about_feature_backup), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.settings_about_feature_csv), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.settings_about_feature_qr), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.settings_about_feature_filter), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.settings_about_feature_sort), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.settings_about_feature_stats), style = MaterialTheme.typography.bodySmall)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = stringResource(R.string.settings_about_author),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = stringResource(R.string.settings_about_copyright),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(R.string.settings_about_license),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(R.string.settings_about_built_with),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    // Encrypt password dialog for export
    if (showEncryptDialog) {
        EncryptPasswordDialog(
            onDismiss = {
                showEncryptDialog = false
                pendingExportUri = null
            },
            onConfirm = { password ->
                showEncryptDialog = false
                pendingExportUri?.let { uri ->
                    // Pass CharArray directly; null if empty
                    viewModel.exportBackup(uri, if (password.isEmpty()) null else password)
                }
            }
        )
    }

    // Decrypt password dialog for import
    if (showDecryptDialog) {
        DecryptPasswordDialog(
            attemptsRemaining = decryptAttempts,
            onDismiss = {
                showDecryptDialog = false
                pendingImportUri = null
                decryptAttempts = 3
                viewModel.resetImportState()
            },
            onConfirm = { password ->
                showDecryptDialog = false
                pendingImportUri?.let { uri ->
                    viewModel.importBackup(uri, password)
                }
            }
        )
    }

    // Import result dialog (for CSV import with detailed results)
    if (showImportResultDialog && lastImportResult != null) {
        ImportResultDialog(
            result = lastImportResult!!,
            onDismiss = {
                showImportResultDialog = false
                lastImportResult = null
            }
        )
    }

    // Encrypt by default warning dialog
    if (showEncryptWarningDialog) {
        AlertDialog(
            onDismissRequest = { showEncryptWarningDialog = false },
            icon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            },
            title = {
                Text(stringResource(R.string.settings_encrypt_warning_title))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_encrypt_warning_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_encrypt_warning_important),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setEncryptByDefault(true)
                        showEncryptWarningDialog = false
                    }
                ) {
                    Text(stringResource(R.string.settings_encrypt_warning_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEncryptWarningDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Loading indicator
    if (exportState is BackupExportState.Exporting ||
        importState is BackupImportState.Importing ||
        csvImportState is CsvImportState.Importing ||
        sampleDataState is SampleDataState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconDescription: String = title
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
