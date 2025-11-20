package net.meshcore.mineralog.ui.screens.home.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.screens.home.CatalogGenerationState
import net.meshcore.mineralog.ui.screens.home.ExportState
import net.meshcore.mineralog.ui.screens.home.LabelGenerationState

/**
 * Container for loading indicators used in HomeScreen operations.
 *
 * Responsibilities:
 * - CSV export loading overlay
 * - PDF label generation loading overlay
 * - PDF catalog generation loading overlay
 * - Accessibility support with live regions
 *
 * Part of Sprint 3 refactoring to reduce prop drilling and improve modularity.
 */
@Composable
fun HomeLoadingIndicators(
    exportState: ExportState,
    labelGenerationState: LabelGenerationState,
    catalogGenerationState: CatalogGenerationState,
    modifier: Modifier = Modifier
) {
    // Loading indicator for export
    if (exportState is ExportState.Exporting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Exporting minerals"
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Loading indicator for label generation (Quick Win #1)
    if (labelGenerationState is LabelGenerationState.Generating) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Generating PDF labels"
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.home_generating_labels),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Loading indicator for catalog generation
    if (catalogGenerationState is CatalogGenerationState.Generating) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Generating PDF catalog"
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.catalog_export_generating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
