package net.meshcore.mineralog.ui.screens.reference

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import net.meshcore.mineralog.MineraLogApplication
import net.meshcore.mineralog.ui.components.SkeletonMineralCard

/**
 * Reference Mineral Library screen - displays a paginated list of reference minerals.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceMineralListScreen(
    onNavigateBack: () -> Unit,
    onMineralClick: (String) -> Unit,
    onAddClick: () -> Unit = {},
    viewModel: ReferenceMineralListViewModel = viewModel(
        factory = ReferenceMineralListViewModelFactory(
            referenceMineralRepository = (LocalContext.current.applicationContext as MineraLogApplication).referenceMineralRepository
        )
    )
) {
    val mineralsPaged = viewModel.mineralsPaged.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val userDefinedCount by viewModel.userDefinedCount.collectAsState()
    val showOnlyUserDefined by viewModel.showOnlyUserDefined.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bibliothèque de Minéraux",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // P1-6: Migrated to AutoMirrored icon
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter un minéral de référence"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar (placeholder for future enhancement)
            /*
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Rechercher un minéral...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                },
                singleLine = true
            )
            */

            // Count info
            if (totalCount > 0) {
                Text(
                    text = if (showOnlyUserDefined) {
                        "$userDefinedCount minéral${if (userDefinedCount > 1) "ux" else ""} personnalisé${if (userDefinedCount > 1) "s" else ""}"
                    } else {
                        "$totalCount minéraux dans la bibliothèque"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Filter chips
            if (userDefinedCount > 0) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = showOnlyUserDefined,
                            onClick = { viewModel.toggleUserDefinedFilter() },
                            label = { Text("Mes minéraux ($userDefinedCount)") },
                            leadingIcon = if (showOnlyUserDefined) {
                                {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Minerals list
            when (mineralsPaged.loadState.refresh) {
                is LoadState.Loading -> {
                    // Show skeleton loading cards
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(8) {
                            SkeletonMineralCard()
                        }
                    }
                }
                is LoadState.Error -> {
                    val error = (mineralsPaged.loadState.refresh as LoadState.Error).error
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Erreur de chargement",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error.localizedMessage ?: "Erreur inconnue",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { mineralsPaged.refresh() },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Réessayer")
                            }
                        }
                    }
                }
                else -> {
                    if (mineralsPaged.itemCount == 0) {
                        // Empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Bibliothèque vide",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Aucun minéral de référence disponible",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                count = mineralsPaged.itemCount,
                                key = mineralsPaged.itemKey { it.id }
                            ) { index ->
                                val mineral = mineralsPaged[index]
                                mineral?.let {
                                    ReferenceMineralCard(
                                        mineral = it,
                                        onClick = { onMineralClick(it.id) }
                                    )
                                }
                            }

                            // Loading indicator for pagination
                            if (mineralsPaged.loadState.append is LoadState.Loading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
