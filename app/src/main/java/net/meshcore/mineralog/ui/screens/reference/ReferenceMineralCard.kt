package net.meshcore.mineralog.ui.screens.reference

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity

/**
 * Card component for displaying a reference mineral in the list.
 *
 * Shows the mineral's name, group, crystal system, and hardness.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceMineralCard(
    mineral: ReferenceMineralEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Name (French/English)
            Text(
                text = mineral.nameFr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (mineral.nameEn != mineral.nameFr) {
                Text(
                    text = mineral.nameEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Properties row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Group
                mineral.mineralGroup?.let { group ->
                    PropertyChip(
                        label = "Groupe",
                        value = group
                    )
                }

                // Crystal System
                mineral.crystalSystem?.let { system ->
                    PropertyChip(
                        label = "Système",
                        value = system
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Hardness & Formula row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mohs hardness
                if (mineral.mohsMin != null && mineral.mohsMax != null) {
                    val hardnessText = if (mineral.mohsMin == mineral.mohsMax) {
                        "${mineral.mohsMin}"
                    } else {
                        "${mineral.mohsMin} - ${mineral.mohsMax}"
                    }
                    PropertyChip(
                        label = "Dureté",
                        value = hardnessText
                    )
                }

                // Formula
                mineral.formula?.let { formula ->
                    PropertyChip(
                        label = "Formule",
                        value = formula
                    )
                }
            }

            // User-defined badge
            if (mineral.isUserDefined) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("Personnalisé", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.height(24.dp)
                )
            }
        }
    }
}

/**
 * Helper composable for displaying a property label-value pair.
 */
@Composable
private fun PropertyChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
