package net.meshcore.mineralog.ui.screens.reference

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Help screen explaining the Reference Mineral Library feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceLibraryHelpScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aide - Bibliothèque de Référence") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Introduction
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Qu'est-ce que la bibliothèque de minéraux ?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "La bibliothèque de référence contient les propriétés techniques standard de centaines de minéraux. Elle vous permet de gagner du temps en auto-remplissant automatiquement les propriétés de vos spécimens.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // How it works
            SectionCard(
                title = "Comment ça fonctionne ?",
                icon = Icons.Default.Info
            ) {
                StepItem(
                    number = "1",
                    title = "Lors de l'ajout d'un spécimen",
                    description = "Sélectionnez le type 'Simple' et recherchez le nom du minéral dans la bibliothèque (ex: Quartz)"
                )
                StepItem(
                    number = "2",
                    title = "Auto-remplissage",
                    description = "Les propriétés techniques (formule chimique, système cristallin, dureté, etc.) sont automatiquement remplies"
                )
                StepItem(
                    number = "3",
                    title = "Propriétés du spécimen",
                    description = "Ajoutez ensuite les caractéristiques spécifiques à votre spécimen : variété de couleur, notes de qualité, etc."
                )
            }

            // Example
            SectionCard(
                title = "Exemple concret",
                icon = Icons.Default.Check
            ) {
                Text(
                    text = "Imaginons que vous collectez une améthyste :",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                PropertyExample(
                    label = "Minéral de référence",
                    value = "Quartz (SiO₂)",
                    isReference = true
                )
                PropertyExample(
                    label = "Formule",
                    value = "SiO₂",
                    isReference = true
                )
                PropertyExample(
                    label = "Système cristallin",
                    value = "Trigonal",
                    isReference = true
                )
                PropertyExample(
                    label = "Dureté Mohs",
                    value = "7",
                    isReference = true
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PropertyExample(
                    label = "Variété de couleur",
                    value = "Améthyste (violet)",
                    isReference = false
                )
                PropertyExample(
                    label = "Notes de qualité",
                    value = "Cristaux bien formés, translucide",
                    isReference = false
                )
            }

            // Benefits
            SectionCard(
                title = "Avantages",
                icon = Icons.Default.Check
            ) {
                BenefitItem("⚡ Gain de temps : 75% plus rapide qu'une saisie manuelle")
                BenefitItem("🎯 Données cohérentes : pas de fautes de frappe")
                BenefitItem("📚 Apprentissage : découvrez les propriétés de chaque minéral")
                BenefitItem("🔄 Agrégats simplifiés : ajoutez des composants en quelques clics")
            }

            // Custom minerals
            SectionCard(
                title = "Minéraux personnalisés",
                icon = Icons.Default.Info
            ) {
                Text(
                    text = "Vous pouvez également ajouter vos propres minéraux de référence à la bibliothèque. Utile pour :",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                BenefitItem("• Minéraux rares non présents dans la bibliothèque standard")
                BenefitItem("• Variétés spécifiques que vous collectionnez souvent")
                BenefitItem("• Roches ou minéraux locaux de votre région")
            }

            // Access
            SectionCard(
                title = "Accéder à la bibliothèque",
                icon = Icons.Default.Info
            ) {
                Text(
                    text = "• Depuis l'écran d'accueil : bouton 'Bibliothèque' dans la barre du haut",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Depuis l'ajout de spécimen : recherchez directement en tapant le nom",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Consultez les fiches détaillées pour en savoir plus sur chaque minéral",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
private fun StepItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(32.dp)
        ) {
            Box(
                contentAlignment = androidx.compose.ui.Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PropertyExample(
    label: String,
    value: String,
    isReference: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isReference) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isReference) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun BenefitItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
