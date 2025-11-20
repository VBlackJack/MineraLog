package net.meshcore.mineralog.data.sample

import android.content.Context
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.Provenance
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Sample data generator for testing and demonstration purposes.
 * Creates a collection of 12 common minerals with realistic properties.
 */
class SampleDataGenerator(
    private val context: Context,
    private val mineralRepository: MineralRepository
) {

    suspend fun generateSampleMinerals(): List<Mineral> {
        val samples = listOf(
            createQuartz(),
            createCalcite(),
            createPyrite(),
            createGalena(),
            createFluorite(),
            createMalachite(),
            createAmethyst(),
            createRoseQuartz(),
            createHematite(),
            createMagnetite(),
            createGypsum(),
            createAzurite()
        )

        // Insert using repository
        samples.forEach { mineral ->
            mineralRepository.insert(mineral)
        }

        return samples
    }

    private fun createQuartz(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Quartz",
            formula = "SiO₂",
            group = "Silicates",
            streak = "White",
            luster = "Vitreous",
            diaphaneity = "Transparent to translucent",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            cleavage = "None",
            fracture = "Conchoidal",
            specificGravity = 2.65f,
            crystalSystem = "Trigonal",
            habit = "Prismatic crystals",
            notes = "Very common mineral. Transparent to white color. Forms beautiful hexagonal crystals.",
            weightGr = 125.5f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "Brazil",
                locality = "Minas Gerais",
                latitude = -18.5122,
                longitude = -44.5550,
                acquiredAt = Instant.now().minus(180, ChronoUnit.DAYS),
                price = 25.0f,
                currency = "EUR"
            ),
            createdAt = Instant.now().minus(180, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(180, ChronoUnit.DAYS)
        )
    }

    private fun createCalcite(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Calcite",
            formula = "CaCO₃",
            group = "Carbonates",
            streak = "White",
            luster = "Vitreous to pearly",
            diaphaneity = "Transparent to translucent",
            mohsMin = 3.0f,
            mohsMax = 3.0f,
            cleavage = "Perfect rhombohedral",
            fracture = "Conchoidal",
            specificGravity = 2.71f,
            crystalSystem = "Trigonal",
            habit = "Rhombohedral, scalenohedral",
            notes = "Colorless to white, yellow, orange, pink. Shows double refraction. Reacts with dilute HCl.",
            weightGr = 89.3f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "Mexico",
                locality = "Chihuahua",
                mineName = "Naica Mine",
                latitude = 27.8507,
                longitude = -105.4947,
                acquiredAt = Instant.now().minus(90, ChronoUnit.DAYS),
                price = 15.0f,
                currency = "EUR"
            ),
            createdAt = Instant.now().minus(90, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(90, ChronoUnit.DAYS)
        )
    }

    private fun createPyrite(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Pyrite",
            formula = "FeS₂",
            group = "Sulfides",
            streak = "Greenish-black to brownish-black",
            luster = "Metallic",
            diaphaneity = "Opaque",
            mohsMin = 6.0f,
            mohsMax = 6.5f,
            cleavage = "Indistinct",
            fracture = "Conchoidal to uneven",
            specificGravity = 5.02f,
            crystalSystem = "Cubic",
            habit = "Cubic, pyritohedral crystals",
            notes = "Brass yellow, golden color. Fool's gold - often mistaken for gold. Forms perfect cubes.",
            weightGr = 245.8f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "Spain",
                locality = "Navajún, La Rioja",
                latitude = 42.1833,
                longitude = -2.0333,
                acquiredAt = Instant.now().minus(45, ChronoUnit.DAYS),
                price = 35.0f,
                currency = "EUR"
            ),
            createdAt = Instant.now().minus(45, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(45, ChronoUnit.DAYS)
        )
    }

    private fun createGalena(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Galena",
            formula = "PbS",
            group = "Sulfides",
            streak = "Lead gray",
            luster = "Metallic",
            diaphaneity = "Opaque",
            mohsMin = 2.5f,
            mohsMax = 2.5f,
            cleavage = "Perfect cubic",
            fracture = "Subconchoidal",
            specificGravity = 7.58f,
            crystalSystem = "Cubic",
            habit = "Cubic crystals",
            notes = "Lead gray color. Primary ore of lead. Very heavy for its size.",
            weightGr = 432.1f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "USA",
                locality = "Missouri",
                mineName = "Sweetwater Mine",
                latitude = 37.7500,
                longitude = -90.5000,
                acquiredAt = Instant.now().minus(120, ChronoUnit.DAYS),
                price = 20.0f,
                currency = "USD"
            ),
            createdAt = Instant.now().minus(120, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(120, ChronoUnit.DAYS)
        )
    }

    private fun createFluorite(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Fluorite",
            formula = "CaF₂",
            group = "Halides",
            streak = "White",
            luster = "Vitreous",
            diaphaneity = "Transparent to translucent",
            mohsMin = 4.0f,
            mohsMax = 4.0f,
            cleavage = "Perfect octahedral",
            fracture = "Subconchoidal to uneven",
            specificGravity = 3.18f,
            crystalSystem = "Cubic",
            habit = "Cubic crystals",
            fluorescence = "Blue-violet under UV",
            notes = "Purple, green, yellow, or colorless. Fluorescent under UV light. Beautiful color zoning.",
            weightGr = 156.7f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "China",
                locality = "Hunan Province",
                latitude = 27.7100,
                longitude = 111.6500,
                acquiredAt = Instant.now().minus(60, ChronoUnit.DAYS),
                price = 28.0f,
                currency = "EUR"
            ),
            createdAt = Instant.now().minus(60, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(60, ChronoUnit.DAYS)
        )
    }

    private fun createMalachite(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Malachite",
            formula = "Cu₂CO₃(OH)₂",
            group = "Carbonates",
            streak = "Light green",
            luster = "Adamantine to vitreous",
            diaphaneity = "Translucent to opaque",
            mohsMin = 3.5f,
            mohsMax = 4.0f,
            cleavage = "Perfect",
            fracture = "Subconchoidal to uneven",
            specificGravity = 4.0f,
            crystalSystem = "Monoclinic",
            habit = "Botryoidal, fibrous, massive",
            notes = "Bright green color. Copper ore. Beautiful banding patterns. Used as gemstone.",
            weightGr = 198.4f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "Congo (DRC)",
                locality = "Katanga Province",
                latitude = -10.8500,
                longitude = 26.9000,
                acquiredAt = Instant.now().minus(30, ChronoUnit.DAYS),
                price = 45.0f,
                currency = "EUR"
            ),
            createdAt = Instant.now().minus(30, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(30, ChronoUnit.DAYS)
        )
    }

    private fun createAmethyst(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Amethyst",
            formula = "SiO₂",
            group = "Silicates",
            streak = "White",
            luster = "Vitreous",
            diaphaneity = "Transparent to translucent",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            cleavage = "None",
            fracture = "Conchoidal",
            specificGravity = 2.65f,
            crystalSystem = "Trigonal",
            habit = "Prismatic crystals",
            notes = "Purple to violet variety of quartz. Color from iron impurities. Popular gemstone.",
            weightGr = 87.2f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "Uruguay",
                locality = "Artigas",
                latitude = -30.4000,
                longitude = -56.4667,
                acquiredAt = Instant.now().minus(150, ChronoUnit.DAYS),
                price = 32.0f,
                currency = "EUR"
            ),
            createdAt = Instant.now().minus(150, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(150, ChronoUnit.DAYS)
        )
    }

    private fun createRoseQuartz(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Rose Quartz",
            formula = "SiO₂",
            group = "Silicates",
            streak = "White",
            luster = "Vitreous",
            diaphaneity = "Translucent",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            cleavage = "None",
            fracture = "Conchoidal",
            specificGravity = 2.65f,
            crystalSystem = "Trigonal",
            habit = "Massive, rarely as crystals",
            notes = "Pink to rose red. Pink variety of quartz. Color from titanium, iron, or manganese.",
            weightGr = 142.9f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "Madagascar",
                locality = "Fianarantsoa Province",
                latitude = -21.4500,
                longitude = 47.0860,
                acquiredAt = Instant.now().minus(75, ChronoUnit.DAYS),
                price = 18.0f,
                currency = "EUR"
            ),
            createdAt = Instant.now().minus(75, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(75, ChronoUnit.DAYS)
        )
    }

    private fun createHematite(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Hematite",
            formula = "Fe₂O₃",
            group = "Oxides",
            streak = "Red to reddish-brown",
            luster = "Metallic to earthy",
            diaphaneity = "Opaque",
            mohsMin = 5.5f,
            mohsMax = 6.5f,
            cleavage = "None",
            fracture = "Uneven to subconchoidal",
            specificGravity = 5.26f,
            crystalSystem = "Trigonal",
            habit = "Tabular, botryoidal, massive",
            magnetic = false,
            notes = "Black, gray, red-brown color. Important iron ore. Distinctive red streak. Can be magnetic when heated.",
            weightGr = 312.5f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "England",
                locality = "Cumberland",
                mineName = "Florence Mine",
                latitude = 54.5667,
                longitude = -3.3667,
                acquiredAt = Instant.now().minus(200, ChronoUnit.DAYS),
                price = 22.0f,
                currency = "GBP"
            ),
            createdAt = Instant.now().minus(200, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(200, ChronoUnit.DAYS)
        )
    }

    private fun createMagnetite(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Magnetite",
            formula = "Fe₃O₄",
            group = "Oxides",
            streak = "Black",
            luster = "Metallic",
            diaphaneity = "Opaque",
            mohsMin = 5.5f,
            mohsMax = 6.5f,
            cleavage = "None",
            fracture = "Subconchoidal to uneven",
            specificGravity = 5.17f,
            crystalSystem = "Cubic",
            habit = "Octahedral crystals",
            magnetic = true,
            notes = "Black, gray color. Naturally magnetic (lodestone). Important iron ore. Strongly attracted to magnets.",
            weightGr = 267.8f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "Sweden",
                locality = "Kiruna",
                mineName = "LKAB Mine",
                latitude = 67.8558,
                longitude = 20.2253,
                acquiredAt = Instant.now().minus(105, ChronoUnit.DAYS),
                price = 15.0f,
                currency = "EUR"
            ),
            createdAt = Instant.now().minus(105, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(105, ChronoUnit.DAYS)
        )
    }

    private fun createGypsum(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Gypsum",
            formula = "CaSO₄·2H₂O",
            group = "Sulfates",
            streak = "White",
            luster = "Vitreous to pearly",
            diaphaneity = "Transparent to translucent",
            mohsMin = 2.0f,
            mohsMax = 2.0f,
            cleavage = "Perfect",
            fracture = "Conchoidal",
            specificGravity = 2.32f,
            crystalSystem = "Monoclinic",
            habit = "Tabular, fibrous, massive",
            notes = "Colorless, white, gray. Very soft - can be scratched by fingernail. Selenite variety is transparent.",
            weightGr = 54.6f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "Mexico",
                locality = "Chihuahua",
                mineName = "Naica Mine",
                latitude = 27.8507,
                longitude = -105.4947,
                acquiredAt = Instant.now().minus(135, ChronoUnit.DAYS),
                price = 12.0f,
                currency = "EUR"
            ),
            createdAt = Instant.now().minus(135, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(135, ChronoUnit.DAYS)
        )
    }

    private fun createAzurite(): Mineral {
        val id = UUID.randomUUID().toString()
        return Mineral(
            id = id,
            name = "Azurite",
            formula = "Cu₃(CO₃)₂(OH)₂",
            group = "Carbonates",
            streak = "Light blue",
            luster = "Vitreous to dull",
            diaphaneity = "Translucent to opaque",
            mohsMin = 3.5f,
            mohsMax = 4.0f,
            cleavage = "Perfect",
            fracture = "Conchoidal",
            specificGravity = 3.77f,
            crystalSystem = "Monoclinic",
            habit = "Prismatic crystals, nodular",
            notes = "Azure blue, deep blue color. Copper ore. Intense blue. Often found with malachite. Historical pigment.",
            weightGr = 176.3f,
            provenance = Provenance(
                id = UUID.randomUUID().toString(),
                mineralId = id,
                country = "Morocco",
                locality = "Kerrouchen",
                latitude = 32.8333,
                longitude = -5.3833,
                acquiredAt = Instant.now().minus(20, ChronoUnit.DAYS),
                price = 38.0f,
                currency = "EUR"
            ),
            createdAt = Instant.now().minus(20, ChronoUnit.DAYS),
            updatedAt = Instant.now().minus(20, ChronoUnit.DAYS)
        )
    }
}
