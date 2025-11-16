package net.meshcore.mineralog.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.MineralComponent
import net.meshcore.mineralog.domain.model.Provenance
import net.meshcore.mineralog.domain.model.getSynthesizedCrystalSystems
import net.meshcore.mineralog.domain.model.getSynthesizedFormulas
import net.meshcore.mineralog.domain.model.getSynthesizedHardnessRange
import java.text.SimpleDateFormat
import java.util.*

/**
 * Composable pour afficher la section Provenance de manière proéminente.
 * v3.1: Mise en avant des informations de provenance pour les collectionneurs.
 */
@Composable
fun ProvenanceSection(
    provenance: Provenance?,
    modifier: Modifier = Modifier
) {
    if (provenance == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // En-tête
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Provenance & Acquisition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Nom de la mine (si présent)
            provenance.mineName?.let { mine ->
                ProvenanceField(
                    icon = Icons.Default.Diamond,
                    label = "Mine",
                    value = mine,
                    prominent = true
                )
            }

            // Dealer (si présent)
            provenance.dealer?.let { dealer ->
                ProvenanceField(
                    icon = Icons.Default.Store,
                    label = "Fournisseur",
                    value = dealer,
                    prominent = true
                )
            }

            // Numéro de catalogue (si présent)
            provenance.catalogNumber?.let { catalog ->
                ProvenanceField(
                    icon = Icons.Default.Tag,
                    label = "N° Catalogue",
                    value = catalog,
                    prominent = true
                )
            }

            // Collectionneur (si présent)
            provenance.collectorName?.let { collector ->
                ProvenanceField(
                    icon = Icons.Default.Person,
                    label = "Collectionneur",
                    value = collector
                )
            }

            // Localisation géographique
            val location = buildString {
                provenance.site?.let { append(it) }
                provenance.locality?.let {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
                provenance.country?.let {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
            }
            if (location.isNotBlank()) {
                ProvenanceField(
                    icon = Icons.Default.Place,
                    label = "Localisation",
                    value = location
                )
            }

            // Prix et source
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                provenance.price?.let { price ->
                    Column(modifier = Modifier.weight(1f)) {
                        ProvenanceField(
                            icon = Icons.Default.AttachMoney,
                            label = "Prix",
                            value = "${String.format("%.2f", price)} ${provenance.currency ?: "USD"}"
                        )
                    }
                }

                provenance.source?.let { source ->
                    Column(modifier = Modifier.weight(1f)) {
                        ProvenanceField(
                            icon = Icons.Default.Source,
                            label = "Source",
                            value = source.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        )
                    }
                }
            }

            // Date d'acquisition
            provenance.acquiredAt?.let { date ->
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                ProvenanceField(
                    icon = Icons.Default.CalendarToday,
                    label = "Acquis le",
                    value = formatter.format(Date.from(date))
                )
            }

            // Notes d'acquisition
            provenance.acquisitionNotes?.let { notes ->
                if (notes.isNotBlank()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ProvenanceField(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    prominent: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = if (prominent) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (prominent) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Composable pour afficher les propriétés spécifiques aux agrégats.
 * v3.1: Champs agrégat (rockType, texture, dominantMinerals, interestingFeatures).
 */
@Composable
fun AggregatePropertiesSection(
    mineral: Mineral,
    modifier: Modifier = Modifier
) {
    // N'afficher que pour les agrégats
    if (mineral.mineralType.toString() != "AGGREGATE") return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // En-tête
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Propriétés de l'Agrégat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Type de roche
            mineral.rockType?.let { rockType ->
                PropertyItem(
                    label = "Type de roche",
                    value = rockType
                )
            }

            // Texture
            mineral.texture?.let { texture ->
                PropertyItem(
                    label = "Texture",
                    value = texture
                )
            }

            // Minéraux dominants visibles
            mineral.dominantMinerals?.let { minerals ->
                PropertyItem(
                    label = "Minéraux dominants visibles",
                    value = minerals
                )
            }

            // Caractéristiques intéressantes
            mineral.interestingFeatures?.let { features ->
                if (features.isNotBlank()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Caractéristiques intéressantes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = features,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Composable pour afficher la synthèse des propriétés des composants d'un agrégat.
 * v3.1: Affiche les propriétés calculées à partir des composants.
 */
@Composable
fun ComponentsSynthesisSection(
    mineral: Mineral,
    components: List<MineralComponent>,
    modifier: Modifier = Modifier
) {
    // N'afficher que pour les agrégats avec composants
    if (mineral.mineralType.toString() != "AGGREGATE" || components.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // En-tête
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoGraph,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Synthèse des Composants (${components.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            // Dureté globale
            mineral.getSynthesizedHardnessRange()?.let { hardness ->
                SynthesisItem(
                    label = "Dureté globale (Mohs)",
                    value = hardness
                )
            }

            // Liste des composants
            Text(
                text = "Composition minérale :",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )

            components.forEachIndexed { index, component ->
                ComponentItem(
                    component = component,
                    index = index
                )
            }
        }
    }
}

@Composable
private fun SynthesisItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun ComponentItem(
    component: MineralComponent,
    index: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Nom + pourcentage + rôle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}. ${component.mineralName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                component.percentage?.let { pct ->
                    Text(
                        text = "${pct.toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Text(
                text = component.role.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
            )

            // Propriétés techniques
            val properties = buildList {
                component.formula?.let { add("Formule: $it") }
                if (component.mohsMin != null && component.mohsMax != null) {
                    add("Dureté: ${component.mohsMin}-${component.mohsMax}")
                } else if (component.mohsMin != null) {
                    add("Dureté: ${component.mohsMin}")
                }
                component.crystalSystem?.let { add("Système: $it") }
            }

            if (properties.isNotEmpty()) {
                Text(
                    text = properties.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }

            // Notes si présentes
            component.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}
