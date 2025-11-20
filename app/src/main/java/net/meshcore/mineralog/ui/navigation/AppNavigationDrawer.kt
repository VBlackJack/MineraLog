package net.meshcore.mineralog.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R

/**
 * Navigation Drawer for MineraLog app.
 * Contains main navigation items: Collection, Library, Identification, Statistics, Settings.
 */
@Composable
fun AppNavigationDrawer(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToIdentification: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet {
        // Header with app name
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        // My Collection
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_drawer_my_collection)) },
            selected = currentRoute == Screen.Home.route,
            onClick = {
                onNavigateToHome()
                onCloseDrawer()
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Reference Library
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_drawer_reference_library)) },
            selected = currentRoute == Screen.ReferenceLibrary.route,
            onClick = {
                onNavigateToLibrary()
                onCloseDrawer()
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Identification
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_drawer_identification)) },
            selected = currentRoute == Screen.Identification.route,
            onClick = {
                onNavigateToIdentification()
                onCloseDrawer()
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Statistics
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_drawer_statistics)) },
            selected = currentRoute == Screen.Statistics.route,
            onClick = {
                onNavigateToStatistics()
                onCloseDrawer()
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Settings
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_drawer_settings)) },
            selected = currentRoute == Screen.Settings.route,
            onClick = {
                onNavigateToSettings()
                onCloseDrawer()
            },
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
