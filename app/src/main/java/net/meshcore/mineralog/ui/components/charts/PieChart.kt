package net.meshcore.mineralog.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Simple pie chart component using Compose Canvas.
 * Displays distribution data as colored pie slices.
 *
 * @param data Map of label to value (e.g., "Silicates" to 15)
 * @param modifier Modifier for the chart container
 * @param colors List of colors to use for slices (cycles if fewer than data size)
 * @param showLabels Whether to show labels on slices
 */
@Composable
fun PieChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier,
    colors: List<Color> = defaultPieColors(),
    showLabels: Boolean = true
) {
    if (data.isEmpty()) {
        Box(modifier = modifier) {
            Text(
                text = "No data to display",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val total = data.values.sum().toFloat()
    val textMeasurer = rememberTextMeasurer()

    // Generate accessible description of chart data
    val chartDescription = buildString {
        append("Pie chart showing distribution: ")
        data.entries.forEachIndexed { index, (label, value) ->
            val percentage = ((value / total) * 100).toInt()
            append("$label $value ($percentage%)")
            if (index < data.size - 1) append(", ")
        }
    }

    Canvas(modifier = modifier
        .padding(16.dp)
        .semantics { contentDescription = chartDescription }
    ) {
        val canvasSize = size.minDimension
        val radius = canvasSize / 2f * 0.8f // 80% of canvas for pie, 20% for labels
        val center = Offset(size.width / 2f, size.height / 2f)

        var startAngle = -90f // Start from top

        data.entries.forEachIndexed { index, (label, value) ->
            val sweepAngle = (value / total) * 360f
            val color = colors[index % colors.size]

            // Draw pie slice
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Draw border
            drawArc(
                color = Color.White.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 2f)
            )

            startAngle += sweepAngle
        }
    }

    // Legend below chart
    if (showLabels) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            data.entries.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Color box
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 4.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(color = colors[index % colors.size])
                        }
                    }
                    // Label and value
                    Text(
                        text = "$label: $value (${((value / total) * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Default color palette for pie chart slices.
 * Uses Material3 color scheme variants.
 */
@Composable
fun defaultPieColors(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.primaryContainer,
    MaterialTheme.colorScheme.secondaryContainer,
    MaterialTheme.colorScheme.tertiaryContainer,
    MaterialTheme.colorScheme.surfaceVariant,
    MaterialTheme.colorScheme.inversePrimary,
    MaterialTheme.colorScheme.error,
    MaterialTheme.colorScheme.errorContainer
)
