package net.meshcore.mineralog.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.data.migration.MigrationReport

/**
 * Dialog displaying the results of automatic reference mineral migration.
 *
 * Shows:
 * - Number of reference minerals created
 * - Number of simple specimens linked
 * - Number of aggregate components linked
 * - List of divergent minerals (if any)
 * - Migration duration
 *
 * @param report The migration report data
 * @param onDismiss Callback when user dismisses the dialog
 * @param onViewLibrary Callback when user wants to view the library
 */
@Composable
fun MigrationReportDialog(
    report: MigrationReport,
    onDismiss: () -> Unit,
    onViewLibrary: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Migration automatique terminée",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary text
                Text(
                    text = "Vos spécimens existants ont été analysés pour créer automatiquement des minéraux de référence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Statistics
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // References created
                    if (report.referencesCreated > 0) {
                        StatisticRow(
                            icon = Icons.Default.Check,
                            iconTint = MaterialTheme.colorScheme.primary,
                            label = "Minéraux ajoutés à la bibliothèque",
                            value = report.referencesCreated.toString()
                        )
                    }

                    // Specimens linked
                    if (report.simpleSpecimensLinked > 0) {
                        StatisticRow(
                            icon = Icons.Default.Link,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            label = "Spécimens simples liés",
                            value = report.simpleSpecimensLinked.toString()
                        )
                    }

                    // Components linked
                    if (report.componentsLinked > 0) {
                        StatisticRow(
                            icon = Icons.Default.Link,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            label = "Composants d'agrégats liés",
                            value = report.componentsLinked.toString()
                        )
                    }

                    // Divergent minerals warning
                    if (report.divergentMinerals.isNotEmpty()) {
                        StatisticRow(
                            icon = Icons.Default.Warning,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            label = "Minéraux avec propriétés divergentes",
                            value = report.divergentMinerals.size.toString()
                        )

                        // Show list of divergent minerals
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Les minéraux suivants n'ont pas été ajoutés car leurs propriétés varient trop :",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                report.divergentMinerals.take(5).forEach { name ->
                                    Text(
                                        text = "• $name",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                if (report.divergentMinerals.size > 5) {
                                    Text(
                                        text = "... et ${report.divergentMinerals.size - 5} autre(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 8.dp),
                                        fontWeight = FontWeight.Light
                                    )
                                }
                            }
                        }
                    }
                }

                // Duration
                if (report.duration > 0) {
                    Text(
                        text = "Temps de migration : ${report.duration}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Empty state
                if (report.referencesCreated == 0 && report.simpleSpecimensLinked == 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "Aucun minéral récurrent détecté. La bibliothèque n'a pas été modifiée.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (report.referencesCreated > 0) {
                TextButton(onClick = onViewLibrary) {
                    Text("Voir la bibliothèque")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

/**
 * Row displaying a migration statistic with an icon.
 */
@Composable
private fun StatisticRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
