package net.meshcore.mineralog.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import net.meshcore.mineralog.R
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.ui.screens.home.MineralListItem

/**
 * Paged mineral list component for HomeScreen.
 *
 * Responsibilities:
 * - Display paginated list of minerals using LazyColumn
 * - Handle loading states (refresh, append, error)
 * - Display empty states (no results vs. empty collection)
 * - Accessibility: Live regions for loading announcements
 *
 * Extracted from HomeScreen.kt to follow Single Responsibility Principle.
 * v1.5.0: Uses Paging 3 for efficient loading of large datasets.
 */
@Composable
fun MineralPagingList(
    mineralsPaged: LazyPagingItems<Mineral>,
    searchQuery: String,
    isFilterActive: Boolean,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onMineralClick: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSearch: () -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show loading indicator at the top when refreshing
        when (mineralsPaged.loadState.refresh) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = "Loading minerals"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is LoadState.Error -> {
                val error = (mineralsPaged.loadState.refresh as LoadState.Error).error
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_error_loading, error.localizedMessage ?: ""),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            is LoadState.NotLoading -> {
                if (mineralsPaged.itemCount == 0) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Quick Win #2: Different states for empty collection vs. no search results
                            if (searchQuery.isNotEmpty() || isFilterActive) {
                                EmptySearchResultsState(
                                    searchQuery = searchQuery,
                                    isFilterActive = isFilterActive,
                                    onClearSearch = onClearSearch,
                                    onClearFilter = onClearFilter
                                )
                            } else {
                                EmptyCollectionState()
                            }
                        }
                    }
                }
            }
        }

        // Paged items
        items(
            count = mineralsPaged.itemCount,
            key = mineralsPaged.itemKey { it.id }
        ) { index ->
            val mineral = mineralsPaged[index]
            mineral?.let {
                MineralListItem(
                    mineral = it,
                    selectionMode = selectionMode,
                    isSelected = it.id in selectedIds,
                    onClick = {
                        if (selectionMode) {
                            onToggleSelection(it.id)
                        } else {
                            onMineralClick(it.id)
                        }
                    }
                )
            }
        }

        // Show loading indicator at the bottom when loading more
        when (mineralsPaged.loadState.append) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = "Loading more minerals"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is LoadState.Error -> {
                val error = (mineralsPaged.loadState.append as LoadState.Error).error
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_error_loading_more, error.localizedMessage ?: ""),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

/**
 * Empty state when no search results are found.
 * Quick Win #2: Distinct state for empty search vs. empty collection.
 */
@Composable
private fun EmptySearchResultsState(
    searchQuery: String,
    isFilterActive: Boolean,
    onClearSearch: () -> Unit,
    onClearFilter: () -> Unit
) {
    val noResultsDescription = "No search results found for '$searchQuery'. " +
        "Try different keywords or clear filters to see all minerals."

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(32.dp)
            .semantics {
                contentDescription = noResultsDescription
            }
    ) {
        Icon(
            Icons.Default.ManageSearch,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_no_results_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (searchQuery.isNotEmpty()) {
            Text(
                text = stringResource(R.string.home_no_results_message, searchQuery),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (isFilterActive) {
            Text(
                text = stringResource(R.string.home_no_results_with_filters),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (searchQuery.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearSearch
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.home_clear_search_button))
                }
            }
            if (isFilterActive) {
                OutlinedButton(
                    onClick = onClearFilter
                ) {
                    Icon(
                        Icons.Default.FilterAltOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.home_clear_filters_button))
                }
            }
        }
    }
}

/**
 * Empty state when the mineral collection is empty.
 * Quick Win #2: Encourages user to add their first mineral.
 */
@Composable
private fun EmptyCollectionState() {
    val emptyStateDescription = "Empty collection state. Your collection is empty. " +
        "Start building your mineral collection by adding your first specimen. " +
        "Tap the add button below to get started."

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(32.dp)
            .semantics {
                contentDescription = emptyStateDescription
            }
    ) {
        Icon(
            Icons.Default.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_empty_state_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_empty_state_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.home_empty_state_action),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
