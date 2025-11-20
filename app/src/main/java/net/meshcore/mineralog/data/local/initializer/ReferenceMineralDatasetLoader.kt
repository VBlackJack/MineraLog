package net.meshcore.mineralog.data.local.initializer

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity
import java.time.Instant
import java.util.UUID

/**
 * Loads and parses the initial reference minerals dataset from JSON.
 *
 * This class is responsible for reading the reference_minerals_initial.json file
 * from the app's assets and converting it into ReferenceMineralEntity objects
 * that can be inserted into the database.
 *
 * Usage:
 * ```kotlin
 * val loader = ReferenceMineralDatasetLoader(context)
 * val minerals = loader.loadInitialDataset()
 * referenceMineralDao.insertAll(minerals)
 * ```
 */
class ReferenceMineralDatasetLoader(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Loads the initial dataset from assets/reference_minerals_initial.json
     *
     * @return List of ReferenceMineralEntity ready to be inserted into the database.
     * @throws Exception if the file cannot be read or parsed.
     */
    fun loadInitialDataset(): List<ReferenceMineralEntity> {
        android.util.Log.i("DatasetLoader", "🔍 Opening reference_minerals_initial.json from assets...")
        val jsonString = context.assets.open("reference_minerals_v6.json")
            .bufferedReader()
            .use { it.readText() }

        android.util.Log.i("DatasetLoader", "📝 Read ${jsonString.length} bytes of JSON")
        android.util.Log.i("DatasetLoader", "🔄 Parsing JSON...")
        val dataset = json.decodeFromString<MineralDataset>(jsonString)
        android.util.Log.i("DatasetLoader", "✅ Parsed dataset with ${dataset.minerals.size} minerals")
        val now = Instant.now()

        android.util.Log.i("DatasetLoader", "🔧 Transforming ${dataset.minerals.size} DTOs to entities...")
        val entities = dataset.minerals.map { dto ->
            // Combiner toxicity dans hazards si présent
            val combinedHazards = listOfNotNull(
                dto.hazards,
                dto.toxicity?.let { "Toxicité: $it" }
            ).joinToString(". ").takeIf { it.isNotEmpty() }

            ReferenceMineralEntity(
                id = dto.id ?: UUID.randomUUID().toString(),
                nameFr = dto.nameFr,
                nameEn = dto.nameEn,
                synonyms = dto.synonyms,
                mineralGroup = dto.mineralGroup,
                formula = dto.formula,
                mohsMin = dto.mohsMin,
                mohsMax = dto.mohsMax,
                density = dto.density,
                crystalSystem = dto.crystalSystem,
                cleavage = dto.cleavage,
                fracture = dto.fracture,
                habit = dto.habit,
                luster = dto.luster,
                streak = dto.streak,
                diaphaneity = dto.transparency ?: dto.diaphaneity,
                fluorescence = dto.fluorescence,
                magnetism = dto.magnetism,
                radioactivity = dto.radioactivity,
                careInstructions = dto.careInstructions,
                sensitivity = dto.sensitivity,
                hazards = combinedHazards,
                storageRecommendations = dto.storageRecommendations,
                identificationTips = dto.identificationTips,
                diagnosticProperties = dto.diagnosticProperties,
                colors = dto.color ?: dto.colors,
                varieties = dto.varietiesAndForms ?: dto.varieties,
                confusionWith = dto.commonConfusions ?: dto.confusionWith,
                geologicalEnvironment = dto.formationEnvironment ?: dto.geologicalEnvironment,
                typicalLocations = dto.typicalLocations,
                associatedMinerals = dto.associatedMinerals,
                uses = dto.uses,
                rarity = dto.rarity,
                collectingDifficulty = dto.collectingDifficulty,
                historicalInfo = dto.historicalNotes ?: dto.historicalInfo,
                etymology = dto.etymology,
                notes = dto.notes,
                isUserDefined = dto.isUserDefined ?: false,
                source = dto.source ?: dataset.source ?: "Standard library",
                createdAt = dto.createdAt?.let { Instant.parse(it) } ?: now,
                updatedAt = dto.updatedAt?.let { Instant.parse(it) } ?: now
            )
        }

        android.util.Log.i("DatasetLoader", "✅ Transformed ${entities.size} entities successfully")
        return entities
    }

    /**
     * Check if the dataset file exists in assets.
     */
    fun datasetExists(): Boolean {
        return try {
            context.assets.open("reference_minerals_v6.json").close()
            true
        } catch (e: Exception) {
            false
        }
    }

    // DTO classes for JSON deserialization
    @Serializable
    private data class MineralDataset(
        val version: String? = null,
        val source: String? = null,
        val minerals: List<MineralDto>
    )

    @Serializable
    private data class MineralDto(
        val id: String? = null,
        val nameFr: String,
        val nameEn: String,
        val synonyms: String? = null,
        val mineralGroup: String? = null,
        val formula: String? = null,
        val mohsMin: Float? = null,
        val mohsMax: Float? = null,
        val density: Float? = null,
        val crystalSystem: String? = null,
        val cleavage: String? = null,
        val fracture: String? = null,
        val habit: String? = null,
        val luster: String? = null,
        val streak: String? = null,
        val diaphaneity: String? = null,
        val fluorescence: String? = null,
        val magnetism: String? = null,
        val radioactivity: String? = null,
        val careInstructions: String? = null,
        val sensitivity: String? = null,
        val hazards: String? = null,
        val storageRecommendations: String? = null,
        val identificationTips: String? = null,
        val diagnosticProperties: String? = null,
        val colors: String? = null,
        val varieties: String? = null,
        val confusionWith: String? = null,
        val geologicalEnvironment: String? = null,
        val typicalLocations: String? = null,
        val associatedMinerals: String? = null,
        val uses: String? = null,
        val rarity: String? = null,
        val collectingDifficulty: String? = null,
        val historicalInfo: String? = null,
        val etymology: String? = null,
        val notes: String? = null,
        val isUserDefined: Boolean? = null,
        val source: String? = null,
        val createdAt: String? = null,
        val updatedAt: String? = null,
        // Legacy/alternative field names for backward compatibility
        val color: String? = null,
        val transparency: String? = null,
        val toxicity: String? = null,
        val commonConfusions: String? = null,
        val formationEnvironment: String? = null,
        val varietiesAndForms: String? = null,
        val historicalNotes: String? = null
    )
}
