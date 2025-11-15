package net.meshcore.mineralog.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import net.meshcore.mineralog.R

/**
 * Simple bar chart component using Compose Canvas.
 * Displays distribution data as horizontal bars.
 *
 * @param data Map of label to value (e.g., "USA" to 25)
 * @param modifier Modifier for the chart container
 * @param barColor Color of the bars
 * @param maxBars Maximum number of bars to display (shows top N)
 * @param showValues Whether to show value labels on bars
 */
@Composable
fun BarChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    maxBars: Int = 15,
    showValues: Boolean = true
) {
    if (data.isEmpty()) {
        Box(modifier = modifier) {
            Text(
                text = stringResource(R.string.chart_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Sort by value descending and take top N
    val sortedData = data.entries
        .sortedByDescending { it.value }
        .take(maxBars)

    val maxValue = sortedData.maxOfOrNull { it.value } ?: 1
    val textMeasurer = rememberTextMeasurer()

    // Generate accessible description of chart data
    val chartDescription = buildString {
        append("Bar chart showing top ${sortedData.size} items: ")
        sortedData.forEachIndexed { index, (label, value) ->
            append("$label $value")
            if (index < sortedData.size - 1) append(", ")
        }
        if (data.size > maxBars) {
            append(", and ${data.size - maxBars} more")
        }
    }

    Column(modifier = modifier
        .padding(16.dp)
        .semantics { contentDescription = chartDescription }
    ) {
        sortedData.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label (fixed width)
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(100.dp)
                )

                // Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                ) {
                    val barWidthFraction = value.toFloat() / maxValue.toFloat()

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = size.width * barWidthFraction
                        val barHeight = size.height

                        // Draw bar
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset.Zero,
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }

                // Value label
                if (showValues) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(40.dp)
                    )
                }
            }
        }

        // Show "and N more" if data was truncated
        if (data.size > maxBars) {
            Text(
                text = stringResource(R.string.chart_and_more, data.size - maxBars),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
