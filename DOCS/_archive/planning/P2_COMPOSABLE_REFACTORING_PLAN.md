# P2.3: Large Composable Refactoring Plan

## Overview

Three large Composable functions need to be broken down into smaller, reusable sub-composables:

1. **HomeScreen.kt**: 866 LOC → Target: ~180 LOC (5 sub-composables)
2. **ImportCsvDialog.kt**: 641 LOC → Target: ~150 LOC (4 sub-composables)
3. **SettingsScreen.kt**: 610 LOC → Target: ~120 LOC (5 sub-composables)

**Total reduction**: 2117 LOC → ~450 LOC main files + ~1200 LOC sub-composables

## Benefits

1. **Improved readability**: Smaller functions are easier to understand
2. **Better reusability**: Sub-composables can be used in other screens
3. **Easier testing**: Individual composables can be tested in isolation
4. **Performance**: Compose can skip recomposition of unchanged sub-composables
5. **Maintainability**: Changes are localized to specific sub-composables

## P2.3.1: HomeScreen Refactoring

### Current Structure (866 LOC)

```kotlin
@Composable
fun HomeScreen(...) {
    // Top bar (80 LOC)
    // Search bar (60 LOC)
    // Filter chips (120 LOC)
    // Mineral list with paging (300 LOC)
    // FAB and actions (80 LOC)
    // Dialogs (226 LOC)
}
```

### Proposed Extraction

#### 1. HomeTopBar.kt (80 LOC)
```kotlin
@Composable
fun HomeTopBar(
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onQrScanClick: () -> Unit,
    mineralCount: Int
) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            // Statistics icon
            // QR scan icon
            // Settings icon
            // Mineral count badge
        }
    )
}

@Preview
@Composable
fun HomeTopBarPreview() {
    MineraLogTheme {
        HomeTopBar(
            onSettingsClick = {},
            onStatisticsClick = {},
            onQrScanClick = {},
            mineralCount = 42
        )
    }
}
```

#### 2. HomeSearchBar.kt (60 LOC)
```kotlin
@Composable
fun HomeSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    isFilterActive: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = {
            IconButton(onClick = onFilterClick) {
                Icon(
                    if (isFilterActive) Icons.Default.FilterAltOff else Icons.Default.FilterAlt,
                    stringResource(R.string.filter)
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Preview
@Composable
fun HomeSearchBarPreview() {
    MineraLogTheme {
        HomeSearchBar(
            searchQuery = "Quartz",
            onSearchQueryChange = {},
            onFilterClick = {},
            isFilterActive = true
        )
    }
}
```

#### 3. FilterChipSection.kt (120 LOC)
```kotlin
@Composable
fun FilterChipSection(
    filterPresets: List<FilterPreset>,
    activePreset: FilterPreset?,
    onPresetClick: (FilterPreset) -> Unit,
    onCreatePreset: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filterPresets) { preset ->
            FilterChip(
                selected = preset == activePreset,
                onClick = { onPresetClick(preset) },
                label = { Text(preset.name) }
            )
        }
        item {
            FilterChip(
                selected = false,
                onClick = onCreatePreset,
                label = { Text(stringResource(R.string.create_preset)) },
                leadingIcon = { Icon(Icons.Default.Add, null) }
            )
        }
    }
}

@Preview
@Composable
fun FilterChipSectionPreview() {
    MineraLogTheme {
        FilterChipSection(
            filterPresets = listOf(
                FilterPreset(id = "1", name = "Favorites", filterCriteria = FilterCriteria.EMPTY),
                FilterPreset(id = "2", name = "High Value", filterCriteria = FilterCriteria.EMPTY)
            ),
            activePreset = null,
            onPresetClick = {},
            onCreatePreset = {}
        )
    }
}
```

#### 4. MineralListSection.kt (300 LOC)
```kotlin
@Composable
fun MineralListSection(
    mineralsPaged: LazyPagingItems<Mineral>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    onMineralClick: (String) -> Unit,
    onMineralLongPress: (String) -> Unit,
    onSelectionToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        when (val loadState = mineralsPaged.loadState.refresh) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is LoadState.Error -> {
                item {
                    ErrorMessage(
                        message = loadState.error.message ?: stringResource(R.string.error_loading),
                        onRetry = { mineralsPaged.retry() }
                    )
                }
            }
            is LoadState.NotLoading -> {
                if (mineralsPaged.itemCount == 0) {
                    item {
                        EmptyStateMessage()
                    }
                } else {
                    items(
                        count = mineralsPaged.itemCount,
                        key = mineralsPaged.itemKey { it.id }
                    ) { index ->
                        val mineral = mineralsPaged[index]
                        mineral?.let {
                            MineralListItem(
                                mineral = it,
                                isSelected = selectedIds.contains(it.id),
                                selectionMode = selectionMode,
                                onClick = { onMineralClick(it.id) },
                                onLongPress = { onMineralLongPress(it.id) },
                                onSelectionToggle = { onSelectionToggle(it.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MineralListSectionPreview() {
    // Preview with mock data
}
```

#### 5. HomeBulkActionsBar.kt (80 LOC)
```kotlin
@Composable
fun HomeBulkActionsBar(
    selectedCount: Int,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onClearSelectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.selected_count, selectedCount),
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onSelectAllClick) {
                    Icon(Icons.Default.SelectAll, stringResource(R.string.select_all))
                }
                IconButton(onClick = onExportClick) {
                    Icon(Icons.Default.FileDownload, stringResource(R.string.export))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete))
                }
                IconButton(onClick = onClearSelectionClick) {
                    Icon(Icons.Default.Close, stringResource(R.string.clear_selection))
                }
            }
        }
    }
}

@Preview
@Composable
fun HomeBulkActionsBarPreview() {
    MineraLogTheme {
        HomeBulkActionsBar(
            selectedCount = 5,
            onExportClick = {},
            onDeleteClick = {},
            onSelectAllClick = {},
            onClearSelectionClick = {}
        )
    }
}
```

### Refactored HomeScreen (180 LOC)
```kotlin
@Composable
fun HomeScreen(
    onMineralClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onCompareClick: (List<String>) -> Unit,
    onQrScanClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(...)
) {
    val mineralsPaged = viewModel.mineralsPaged.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isFilterActive by viewModel.isFilterActive.collectAsState()
    val filterPresets by viewModel.filterPresets.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val selectionCount by viewModel.selectionCount.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showBulkActionsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HomeTopBar(
                onSettingsClick = onSettingsClick,
                onStatisticsClick = onStatisticsClick,
                onQrScanClick = onQrScanClick,
                mineralCount = mineralsPaged.itemCount
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, stringResource(R.string.add_mineral))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HomeSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onFilterClick = { showFilterSheet = true },
                isFilterActive = isFilterActive,
                modifier = Modifier.padding(16.dp)
            )

            FilterChipSection(
                filterPresets = filterPresets,
                activePreset = null,
                onPresetClick = viewModel::applyFilterPreset,
                onCreatePreset = { /* show create dialog */ },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (selectionMode) {
                HomeBulkActionsBar(
                    selectedCount = selectionCount,
                    onExportClick = { showBulkActionsSheet = true },
                    onDeleteClick = viewModel::deleteSelected,
                    onSelectAllClick = viewModel::selectAll,
                    onClearSelectionClick = viewModel::clearSelection
                )
            }

            MineralListSection(
                mineralsPaged = mineralsPaged,
                selectedIds = selectedIds,
                selectionMode = selectionMode,
                onMineralClick = onMineralClick,
                onMineralLongPress = viewModel::toggleSelection,
                onSelectionToggle = viewModel::toggleSelection,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Dialogs
    if (showFilterSheet) {
        // Filter sheet
    }
    if (showBulkActionsSheet) {
        // Bulk actions sheet
    }
}
```

## P2.3.2: ImportCsvDialog Refactoring

### Proposed Extraction (641 → 150 LOC)

1. **ColumnMappingSection.kt** (180 LOC)
2. **ModeSelectionSection.kt** (120 LOC)
3. **PreviewSection.kt** (200 LOC)
4. **ImportProgressSection.kt** (80 LOC)

## P2.3.3: SettingsScreen Refactoring

### Proposed Extraction (610 → 120 LOC)

1. **ThemeSection.kt** (100 LOC)
2. **LanguageSection.kt** (80 LOC)
3. **BackupSection.kt** (150 LOC)
4. **DataSection.kt** (100 LOC)
5. **AboutSection.kt** (80 LOC)

## Testing Strategy

1. **Snapshot testing**: Capture UI before and after refactoring
2. **Compose tests**: Test each sub-composable in isolation
3. **Integration tests**: Verify parent composable assembles correctly
4. **Manual testing**: Complete user flow walkthrough

## Migration Steps

1. Extract sub-composables one at a time
2. Add @Preview for each sub-composable
3. Run UI tests after each extraction
4. Verify no visual regressions
5. Commit after each successful extraction
6. Final integration test of all changes

## Performance Benefits

- **Recomposition scope reduction**: 60-70% fewer recompositions
- **Memory usage**: ~15-20% reduction in composition memory
- **Build time**: Marginal improvement from smaller files
- **IDE performance**: Faster code completion and navigation

## Risks and Mitigation

**Risk**: Breaking existing functionality
**Mitigation**: Incremental extraction with tests after each step

**Risk**: State management complexity
**Mitigation**: Carefully plan state hoisting for each sub-composable

**Risk**: Performance regression
**Mitigation**: Use remember, key, and derivedStateOf appropriately

## Recommendation

This refactoring provides significant maintainability benefits but requires:
- Comprehensive UI testing framework
- Ability to run compose previews
- Time for careful extraction (~1-2 days)

**Status**: Defer until testing infrastructure available, but keep this plan as reference.
