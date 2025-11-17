package net.meshcore.mineralog.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.R
import net.meshcore.mineralog.ui.screens.home.BulkOperationProgress

/**
 * Bulk operation progress indicator card for HomeScreen.
 *
 * Responsibilities:
 * - Display progress bar for bulk operations (export, delete, etc.)
 * - Show operation name and current/total count
 * - Accessibility: Live region for screen reader announcements
 *
 * Extracted from HomeScreen.kt to follow Single Responsibility Principle.
 * Quick Win #6: Bulk operation progress indicator
 */
@Composable
fun BulkOperationProgressCard(
    progress: BulkOperationProgress.InProgress,
    modifier: Modifier = Modifier
) {
    val percentage = (progress.current.toFloat() / progress.total.toFloat())
    val operationName = progress.operation.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "${progress.operation} in progress: ${progress.current} of ${progress.total} items"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_operation_in_progress, operationName),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "${progress.current}/${progress.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
