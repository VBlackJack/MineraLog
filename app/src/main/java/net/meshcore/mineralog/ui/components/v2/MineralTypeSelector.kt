package net.meshcore.mineralog.ui.components.v2

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.domain.model.MineralType

/**
 * A composable for selecting the mineral type (SIMPLE or AGGREGATE).
 *
 * This is a v2.0 component used in the add/edit mineral screens to allow users
 * to choose between creating a simple mineral or an aggregate.
 *
 * @param selectedType The currently selected mineral type.
 * @param onTypeSelected Callback when a type is selected.
 * @param modifier Modifier for the container.
 * @param enabled Whether the selector is enabled.
 */
@Composable
fun MineralTypeSelector(
    selectedType: MineralType,
    onTypeSelected: (MineralType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .selectableGroup()
    ) {
        Text(
            text = "Type de minéral",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MineralTypeOption(
                type = MineralType.SIMPLE,
                icon = Icons.Default.Diamond,
                label = "Minéral Simple",
                description = "Un seul minéral\n(Quartz, Pyrite...)",
                selected = selectedType == MineralType.SIMPLE,
                onClick = { if (enabled) onTypeSelected(MineralType.SIMPLE) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )

            MineralTypeOption(
                type = MineralType.AGGREGATE,
                icon = Icons.Default.Landscape,
                label = "Agrégat",
                description = "Roche composée\n(Granite, Gneiss...)",
                selected = selectedType == MineralType.AGGREGATE,
                onClick = { if (enabled) onTypeSelected(MineralType.AGGREGATE) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MineralTypeOption(
    type: MineralType,
    icon: ImageVector,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = if (selected) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        CardDefaults.cardColors()
    }

    Card(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton
            ),
        colors = colors,
        border = if (selected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else {
            CardDefaults.outlinedCardBorder()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selected) {
                Spacer(modifier = Modifier.height(8.dp))
                RadioButton(
                    selected = true,
                    onClick = null,
                    enabled = enabled
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MineralTypeSelectorPreview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                MineralTypeSelector(
                    selectedType = MineralType.SIMPLE,
                    onTypeSelected = {}
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MineralTypeSelectorAggregatePreview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                MineralTypeSelector(
                    selectedType = MineralType.AGGREGATE,
                    onTypeSelected = {}
                )
            }
        }
    }
}
