package net.meshcore.mineralog.ui.screens.home.components.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.meshcore.mineralog.data.model.FilterCriteria
import net.meshcore.mineralog.domain.model.FilterPreset
import net.meshcore.mineralog.ui.screens.home.FilterBottomSheet
import net.meshcore.mineralog.ui.screens.home.SortBottomSheet
import net.meshcore.mineralog.ui.screens.home.SortOption

/**
 * Container for filter and sort related dialogs used in HomeScreen.
 *
 * Responsibilities:
 * - Filter bottom sheet with preset management
 * - Sort bottom sheet with sorting options
 *
 * Part of Sprint 3 refactoring to reduce prop drilling and improve modularity.
 */
@Composable
fun HomeFilterDialogs(
    // Bottom sheet states
    showFilterSheet: Boolean,
    showSortSheet: Boolean,

    // Filter data
    filterCriteria: FilterCriteria,
    filterPresets: List<FilterPreset>,
    sortOption: SortOption,

    // Filter actions
    onFilterCriteriaChange: (FilterCriteria) -> Unit,
    onClearFilter: () -> Unit,
    onSavePreset: (FilterPreset) -> Unit,
    onLoadPreset: (FilterPreset) -> Unit,
    onDeletePreset: (String) -> Unit,

    // Sort actions
    onSortSelected: (SortOption) -> Unit,

    // Dismiss callbacks
    onDismissFilterSheet: () -> Unit,
    onDismissSortSheet: () -> Unit,

    modifier: Modifier = Modifier
) {
    // Filter bottom sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            criteria = filterCriteria,
            presets = filterPresets,
            onCriteriaChange = onFilterCriteriaChange,
            onApply = { },
            onClear = onClearFilter,
            onSavePreset = onSavePreset,
            onLoadPreset = onLoadPreset,
            onDeletePreset = onDeletePreset,
            onDismiss = onDismissFilterSheet
        )
    }

    // Sort bottom sheet
    if (showSortSheet) {
        SortBottomSheet(
            currentSort = sortOption,
            onSortSelected = onSortSelected,
            onDismiss = onDismissSortSheet
        )
    }
}
