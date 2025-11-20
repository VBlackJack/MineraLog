package net.meshcore.mineralog.ui.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.meshcore.mineralog.R

/**
 * Top app bar component for HomeScreen.
 *
 * Responsibilities:
 * - Normal mode: Show title and action buttons (library, QR scanner, bulk edit, statistics, settings)
 * - Selection mode: Show selection count and actions (close, select all, more actions)
 *
 * Extracted from HomeScreen.kt to follow Single Responsibility Principle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenTopBar(
    selectionMode: Boolean,
    selectionCount: Int,
    totalCount: Int,
    onExitSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onShowBulkActionsSheet: () -> Unit,
    onLibraryClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    if (selectionMode) {
        // Selection mode top bar
        TopAppBar(
            title = { Text(stringResource(R.string.home_selection_count, selectionCount)) },
            navigationIcon = {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_exit_selection))
                }
            },
            actions = {
                if (selectionCount < totalCount) {
                    IconButton(onClick = onSelectAll) {
                        Icon(Icons.Default.DoneAll, contentDescription = stringResource(R.string.cd_select_all))
                    }
                }
                if (selectionCount > 0) {
                    IconButton(onClick = onShowBulkActionsSheet) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_actions))
                    }
                }
            }
        )
    } else {
        // Normal top bar
        TopAppBar(
            title = { Text(stringResource(R.string.home_title)) },
            actions = {
                // Library button
                IconButton(onClick = onLibraryClick) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = stringResource(R.string.cd_library))
                }
                // QR Scanner button
                IconButton(onClick = onQrScanClick) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.cd_scan_qr))
                }
                // Bulk edit button
                IconButton(onClick = onEnterSelectionMode) {
                    Icon(Icons.Default.Ballot, contentDescription = stringResource(R.string.cd_bulk_edit))
                }
                IconButton(onClick = onStatisticsClick) {
                    Icon(Icons.Default.BarChart, contentDescription = stringResource(R.string.cd_statistics))
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                }
            }
        )
    }
}
