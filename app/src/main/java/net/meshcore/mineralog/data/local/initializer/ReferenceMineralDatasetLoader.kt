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
        val jsonString = context.assets.open("reference_minerals_initial.json")
            .bufferedReader()
            .use { it.readText() }

        val dataset = json.decodeFromString<MineralDataset>(jsonString)
        val now = Instant.now()

        return dataset.minerals.map { dto ->
            ReferenceMineralEntity(
                id = UUID.randomUUID().toString(),
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
                diaphaneity = dto.diaphaneity,
                fluorescence = dto.fluorescence,
                magnetism = dto.magnetism,
                radioactivity = dto.radioactivity,
                notes = dto.notes,
                isUserDefined = false, // Initial dataset minerals are not user-defined
                source = dataset.source ?: "Standard library",
                createdAt = now,
                updatedAt = now
            )
        }
    }

    /**
     * Check if the dataset file exists in assets.
     */
    fun datasetExists(): Boolean {
        return try {
            context.assets.open("reference_minerals_initial.json").close()
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
        val notes: String? = null
    )
}
