package net.meshcore.mineralog.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.screens.home.FilterCriteria
import net.meshcore.mineralog.ui.screens.home.SortOption

/**
 * Search and filter bar component for HomeScreen.
 *
 * Responsibilities:
 * - Search text field with clear button
 * - Sort and filter action buttons
 * - Active filter chip display
 *
 * Extracted from HomeScreen.kt to follow Single Responsibility Principle.
 */
@Composable
fun SearchFilterBar(
    searchQuery: String,
    sortOption: SortOption,
    filterCriteria: FilterCriteria,
    isFilterActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onShowSortSheet: () -> Unit,
    onShowFilterSheet: () -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text(stringResource(R.string.home_search_placeholder_text)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                Row {
                    // Clear search button
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                    // Sort button
                    IconButton(onClick = onShowSortSheet) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort: ${sortOption.displayName}",
                            tint = if (sortOption != SortOption.DATE_NEWEST) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    // Filter button with badge
                    BadgedBox(
                        badge = {
                            if (isFilterActive) {
                                Badge { Text("${filterCriteria.activeCount()}") }
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (isFilterActive) {
                                "${filterCriteria.activeCount()} active filters"
                            } else {
                                "No active filters"
                            }
                        }
                    ) {
                        IconButton(onClick = onShowFilterSheet) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (isFilterActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            },
            singleLine = true
        )

        // Active filter chip
        if (isFilterActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onShowFilterSheet,
                    label = { Text(filterCriteria.toSummary()) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter icon",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                IconButton(
                    onClick = onClearFilter
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear filter",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
