package net.meshcore.mineralog.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.screens.home.DecryptPasswordDialog
import net.meshcore.mineralog.ui.screens.home.EncryptPasswordDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            settingsRepository = (LocalContext.current.applicationContext as MineraLogApplication).settingsRepository,
            backupRepository = (LocalContext.current.applicationContext as MineraLogApplication).backupRepository
        )
    )
) {
    val copyPhotos by viewModel.copyPhotosToInternal.collectAsState()
    val language by viewModel.language.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val importState by viewModel.importState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showEncryptDialog by remember { mutableStateOf(false) }
    var showDecryptDialog by remember { mutableStateOf(false) }
    var decryptAttempts by remember { mutableStateOf(3) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

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

    // Handle export state changes
    LaunchedEffect(exportState) {
        when (exportState) {
            is BackupExportState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Backup exported successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetExportState()
                pendingExportUri = null
            }
            is BackupExportState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "Export failed: ${(exportState as BackupExportState.Error).message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetExportState()
                pendingExportUri = null
            }
            else -> {}
        }
    }

    // Handle import state changes
    LaunchedEffect(importState) {
        when (importState) {
            is BackupImportState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Backup imported successfully. ${(importState as BackupImportState.Success).imported} minerals restored.",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetImportState()
                pendingImportUri = null
                decryptAttempts = 3
            }
            is BackupImportState.PasswordRequired -> {
                // Show password dialog
                showDecryptDialog = true
            }
            is BackupImportState.Error -> {
                val errorMessage = (importState as BackupImportState.Error).message
                snackbarHostState.showSnackbar(
                    message = "Import failed: $errorMessage",
                    duration = SnackbarDuration.Long
                )

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            // Backup & Restore Section
            SectionHeader(title = stringResource(R.string.settings_section_backup))

            // Export backup
            SettingsActionItem(
                icon = Icons.Default.Upload,
                title = stringResource(R.string.action_backup),
                subtitle = "Export all data with encryption",
                onClick = {
                    val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                        .format(java.util.Date())
                    zipExportLauncher.launch("mineralog_backup_$timestamp.zip")
                }
            )

            // Import backup
            SettingsActionItem(
                icon = Icons.Default.Download,
                title = stringResource(R.string.action_restore),
                subtitle = "Restore from encrypted backup",
                onClick = {
                    zipImportLauncher.launch("application/zip")
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Language
            SettingsItem(
                title = "Language",
                subtitle = language.uppercase()
            ) {
                viewModel.setLanguage(if (language == "en") "fr" else "en")
            }

            HorizontalDivider()

            // Copy photos setting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setCopyPhotos(!copyPhotos) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Copy photos to internal storage",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Recommended for data safety",
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

            // About
            SettingsItem(
                title = "About",
                subtitle = "MineraLog v1.5.0"
            ) {
                // Show about dialog
            }
        }
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
                    viewModel.exportBackup(uri, password.ifEmpty { null })
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

    // Loading indicator
    if (exportState is BackupExportState.Exporting || importState is BackupImportState.Importing) {
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
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
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
