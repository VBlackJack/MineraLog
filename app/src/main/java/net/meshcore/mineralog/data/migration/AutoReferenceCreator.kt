package net.meshcore.mineralog.data.migration

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import java.time.Instant
import java.util.UUID

/**
 * Data class representing the result of an automatic reference migration.
 */
data class MigrationReport(
    val referencesCreated: Int,
    val simpleSpecimensLinked: Int,
    val componentsLinked: Int,
    val divergentMinerals: List<String>, // Names of minerals with too much variance
    val duration: Long // milliseconds
)

/**
 * Service responsible for automatically creating reference minerals from existing specimens.
 *
 * This one-shot migration:
 * 1. Analyzes all existing SimplePropertiesEntity and MineralComponentEntity
 * 2. Groups them by name (normalized)
 * 3. Detects common properties (>70% agreement threshold)
 * 4. Creates ReferenceMineralEntity with isUserDefined=true
 * 5. Links specimens/components to the new references
 * 6. Generates a migration report
 *
 * Executed once after DB migration v5→v6 (controlled by SharedPreferences flag).
 */
class AutoReferenceCreator(
    private val context: Context,
    private val database: MineraLogDatabase
) {

    companion object {
        private const val PREFS_NAME = "reference_library_migration"
        private const val KEY_MIGRATION_DONE = "auto_reference_migration_done"
        private const val PROPERTY_AGREEMENT_THRESHOLD = 0.7 // 70% agreement required
        private const val MIN_OCCURRENCES = 2 // Minimum occurrences to create a reference
    }

    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if automatic migration has already been performed.
     */
    fun isMigrationDone(): Boolean {
        return sharedPrefs.getBoolean(KEY_MIGRATION_DONE, false)
    }

    /**
     * Mark migration as completed.
     */
    private fun markMigrationDone() {
        sharedPrefs.edit().putBoolean(KEY_MIGRATION_DONE, true).apply()
    }

    /**
     * Run the automatic reference migration.
     *
     * @return MigrationReport with statistics
     */
    suspend fun run(): MigrationReport = withContext(Dispatchers.IO) {
        if (isMigrationDone()) {
            return@withContext MigrationReport(0, 0, 0, emptyList(), 0)
        }

        val startTime = System.currentTimeMillis()

        try {
            val report = database.withTransaction {
                // Step 1: Analyze simple specimens
                val simpleGroups = analyzeSimpleSpecimens()

                // Step 2: Analyze aggregate components
                val componentGroups = analyzeComponents()

                // Merge groups by name (combine simple and component data)
                val allGroups = mergeGroups(simpleGroups, componentGroups)

                // Step 3: Create references and link
                val (created, linkedSimple, linkedComponents, divergent) = createReferencesAndLink(allGroups)

                MigrationReport(
                    referencesCreated = created,
                    simpleSpecimensLinked = linkedSimple,
                    componentsLinked = linkedComponents,
                    divergentMinerals = divergent,
                    duration = System.currentTimeMillis() - startTime
                )
            }

            markMigrationDone()
            report
        } catch (e: Exception) {
            android.util.Log.e("AutoReferenceCreator", "Migration failed", e)
            MigrationReport(0, 0, 0, emptyList(), System.currentTimeMillis() - startTime)
        }
    }

    /**
     * Analyze all simple specimens and group by normalized name.
     * TODO: Fix - SimplePropertiesEntity doesn't have mineralName, needs JOIN with MineralEntity
     */
    private suspend fun analyzeSimpleSpecimens(): Map<String, List<SimpleSpecimenData>> {
        // Temporarily disabled - needs proper implementation with JOIN
        return emptyMap()
    }

    /**
     * Analyze all aggregate components and group by normalized name.
     * TODO: Fix - needs proper implementation
     */
    private suspend fun analyzeComponents(): Map<String, List<ComponentData>> {
        // Temporarily disabled - needs proper implementation
        return emptyMap()
    }

    /**
     * Merge simple and component groups.
     */
    private fun mergeGroups(
        simpleGroups: Map<String, List<SimpleSpecimenData>>,
        componentGroups: Map<String, List<ComponentData>>
    ): Map<String, MineralGroup> {
        val allKeys = (simpleGroups.keys + componentGroups.keys).toSet()

        return allKeys.associateWith { key ->
            MineralGroup(
                normalizedName = key,
                simpleSpecimens = simpleGroups[key] ?: emptyList(),
                components = componentGroups[key] ?: emptyList()
            )
        }.filter { it.value.totalOccurrences() >= MIN_OCCURRENCES }
    }

    /**
     * Create references for recurring minerals and link specimens.
     *
     * @return Tuple of (referencesCreated, simpleLinked, componentsLinked, divergentNames)
     */
    private suspend fun createReferencesAndLink(
        groups: Map<String, MineralGroup>
    ): Tuple4<Int, Int, Int, List<String>> {
        var referencesCreated = 0
        var simpleLinked = 0
        var componentsLinked = 0
        val divergentNames = mutableListOf<String>()

        for ((normalizedName, group) in groups) {
            // Detect common properties
            val referenceMineral = detectCommonProperties(group)

            if (referenceMineral != null) {
                // Create reference
                database.referenceMineralDao().insert(referenceMineral)
                referencesCreated++

                // Link simple specimens
                group.simpleSpecimens.forEach { simple ->
                    database.simplePropertiesDao().updateReferenceMineralId(
                        mineralId = simple.id,
                        referenceMineralId = referenceMineral.id
                    )
                    simpleLinked++
                }

                // Link components
                group.components.forEach { component ->
                    database.mineralComponentDao().updateReferenceMineralId(
                        componentId = component.id,
                        referenceMineralId = referenceMineral.id
                    )
                    componentsLinked++
                }
            } else {
                // Too much divergence
                divergentNames.add(group.displayName())
            }
        }

        return Tuple4(referencesCreated, simpleLinked, componentsLinked, divergentNames)
    }

    /**
     * Detect common properties from a group of minerals.
     *
     * Returns a ReferenceMineralEntity if >70% agreement, null otherwise.
     */
    private fun detectCommonProperties(group: MineralGroup): ReferenceMineralEntity? {
        // Collect all property data
        val allData = group.simpleSpecimens.map { it.entity } + group.components.map { it.entity }
        if (allData.isEmpty()) return null

        // Extract most common values for each property
        val nameFr = group.normalizedName
        val nameEn = nameFr // For migration, we use the same name

        // Chemistry
        val formula = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.formula })

        // Physical properties - use mohsMin as representative value
        val mohsMin = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.mohsMin })
        val mohsMax = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.mohsMax })
        val density = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.density })

        // Crystallographic
        val crystalSystem = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.crystalSystem })
        val cleavage = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.cleavage })
        val fracture = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.fracture })
        val habit = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.habit })

        // Optical
        val luster = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.luster })
        val streak = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.streak })
        val diaphaneity = mostCommonValue(allData.mapNotNull { (it as? net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity)?.diaphaneity })

        // Calculate agreement score (% of properties with majority consensus)
        val agreementScore = calculateAgreementScore(group)

        if (agreementScore < PROPERTY_AGREEMENT_THRESHOLD) {
            return null // Too much divergence
        }

        // Create reference mineral
        return ReferenceMineralEntity(
            id = UUID.randomUUID().toString(),
            nameFr = nameFr,
            nameEn = nameEn,
            formula = formula,
            mohsMin = mohsMin,
            mohsMax = mohsMax,
            density = density,
            crystalSystem = crystalSystem,
            cleavage = cleavage,
            fracture = fracture,
            habit = habit,
            luster = luster,
            streak = streak,
            diaphaneity = diaphaneity,
            isUserDefined = true,
            source = "Créé automatiquement à partir de ${group.totalOccurrences()} spécimens",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    /**
     * Calculate agreement score for a group (0.0 to 1.0).
     */
    private fun calculateAgreementScore(group: MineralGroup): Double {
        val allData = group.simpleSpecimens.map { it.entity }
        if (allData.isEmpty()) return 0.0

        val properties = listOf(
            allData.mapNotNull { it.formula },
            allData.mapNotNull { it.mohsMin },
            allData.mapNotNull { it.crystalSystem },
            allData.mapNotNull { it.luster }
        )

        val scores = properties.map { values ->
            if (values.isEmpty()) return@map 0.0
            val mostCommon = values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.value ?: 0
            mostCommon.toDouble() / values.size
        }

        return scores.average()
    }

    /**
     * Find the most common value in a list.
     */
    private fun <T> mostCommonValue(values: List<T>): T {
        return values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: values.first()
    }

    /**
     * Normalize a mineral name for grouping (lowercase, trim).
     */
    private fun normalizeName(name: String): String {
        return name.trim().lowercase()
    }

    // Data classes

    private data class SimpleSpecimenData(
        val id: String,
        val name: String,
        val entity: net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity
    )

    private data class ComponentData(
        val id: String,
        val name: String,
        val entity: net.meshcore.mineralog.data.local.entity.MineralComponentEntity
    )

    private data class MineralGroup(
        val normalizedName: String,
        val simpleSpecimens: List<SimpleSpecimenData>,
        val components: List<ComponentData>
    ) {
        fun totalOccurrences() = simpleSpecimens.size + components.size

        fun displayName(): String {
            return (simpleSpecimens.firstOrNull()?.name ?: components.firstOrNull()?.name)
                ?.replaceFirstChar { it.uppercase() } ?: normalizedName
        }
    }

    private data class Tuple4<A, B, C, D>(val v1: A, val v2: B, val v3: C, val v4: D)
}
