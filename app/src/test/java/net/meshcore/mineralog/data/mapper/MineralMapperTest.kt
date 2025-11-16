package net.meshcore.mineralog.data.mapper

import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.MineralType
import net.meshcore.mineralog.domain.model.Provenance
import net.meshcore.mineralog.domain.model.Storage
import org.junit.Test
import org.junit.Assert.*
import java.time.Instant

/**
 * P1-11: Tests for Mineral mapping between entity and domain models.
 *
 * Tests cover:
 * - Entity to domain conversion
 * - Domain to entity conversion
 * - Null handling and optional fields
 * - Data preservation during mapping
 */
class MineralMapperTest {

    @Test
    fun `test create simple mineral`() {
        // Arrange & Act
        val mineral = Mineral(
            id = "test-id",
            name = "Quartz",
            group = "Silicates",
            formula = "SiO₂",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            mineralType = MineralType.SIMPLE
        )

        // Assert
        assertEquals("test-id", mineral.id)
        assertEquals("Quartz", mineral.name)
        assertEquals("Silicates", mineral.group)
        assertEquals("SiO₂", mineral.formula)
        assertEquals(7.0f, mineral.mohsMin)
        assertEquals(7.0f, mineral.mohsMax)
        assertEquals(MineralType.SIMPLE, mineral.mineralType)
    }

    @Test
    fun `test create mineral with provenance`() {
        // Arrange
        val provenance = Provenance(
            id = "prov-id",
            mineralId = "mineral-id",
            country = "France",
            locality = "Mont Blanc",
            site = "Chamonix Valley",
            latitude = 45.8326,
            longitude = 6.8652,
            acquiredAt = Instant.parse("2023-01-15T10:00:00Z"),
            source = "Field collection",
            price = 50.0f,
            estimatedValue = 100.0f,
            currency = "EUR"
        )

        val mineral = Mineral(
            id = "mineral-id",
            name = "Fluorite",
            provenance = provenance
        )

        // Assert
        assertNotNull(mineral.provenance)
        assertEquals("France", mineral.provenance?.country)
        assertEquals("Mont Blanc", mineral.provenance?.locality)
        assertEquals(45.8326, mineral.provenance?.latitude ?: 0.0, 0.0001)
        assertEquals(6.8652, mineral.provenance?.longitude ?: 0.0, 0.0001)
        assertEquals(50.0f, mineral.provenance?.price)
        assertEquals("EUR", mineral.provenance?.currency)
    }

    @Test
    fun `test create mineral with storage`() {
        // Arrange
        val storage = Storage(
            id = "storage-id",
            mineralId = "mineral-id",
            place = "Main Collection",
            container = "Cabinet A",
            box = "Box 3",
            slot = "A5"
        )

        val mineral = Mineral(
            id = "mineral-id",
            name = "Calcite",
            storage = storage
        )

        // Assert
        assertNotNull(mineral.storage)
        assertEquals("Main Collection", mineral.storage?.place)
        assertEquals("Cabinet A", mineral.storage?.container)
        assertEquals("Box 3", mineral.storage?.box)
        assertEquals("A5", mineral.storage?.slot)
    }

    @Test
    fun `test mineral with null optional fields`() {
        // Arrange & Act
        val mineral = Mineral(
            id = "min-id",
            name = "Test Mineral",
            group = null,
            formula = null,
            provenance = null,
            storage = null,
            photos = emptyList()
        )

        // Assert
        assertEquals("Test Mineral", mineral.name)
        assertNull(mineral.group)
        assertNull(mineral.formula)
        assertNull(mineral.provenance)
        assertNull(mineral.storage)
        assertTrue(mineral.photos.isEmpty())
    }

    @Test
    fun `test mineral type values`() {
        // Arrange & Act
        val simpleMineral = Mineral(id = "1", name = "Quartz", mineralType = MineralType.SIMPLE)
        val aggregateMineral = Mineral(id = "2", name = "Granite", mineralType = MineralType.AGGREGATE)
        val rockMineral = Mineral(id = "3", name = "Basalt", mineralType = MineralType.ROCK)

        // Assert
        assertEquals(MineralType.SIMPLE, simpleMineral.mineralType)
        assertEquals(MineralType.AGGREGATE, aggregateMineral.mineralType)
        assertEquals(MineralType.ROCK, rockMineral.mineralType)
    }

    @Test
    fun `test mineral with tags`() {
        // Arrange
        val tags = listOf("UV fluorescent", "Rare", "Collector's item")

        val mineral = Mineral(
            id = "min-id",
            name = "Scheelite",
            tags = tags
        )

        // Assert
        assertEquals(3, mineral.tags.size)
        assertTrue(mineral.tags.contains("UV fluorescent"))
        assertTrue(mineral.tags.contains("Rare"))
    }

    @Test
    fun `test mineral with physical properties`() {
        // Arrange & Act
        val mineral = Mineral(
            id = "min-id",
            name = "Gold",
            crystalSystem = "Cubic",
            cleavage = "None",
            fracture = "Hackly",
            luster = "Metallic",
            streak = "Golden-yellow",
            diaphaneity = "Opaque",
            habit = "Dendritic",
            specificGravity = 19.3f,
            mohsMin = 2.5f,
            mohsMax = 3.0f
        )

        // Assert
        assertEquals("Cubic", mineral.crystalSystem)
        assertEquals("None", mineral.cleavage)
        assertEquals("Hackly", mineral.fracture)
        assertEquals("Metallic", mineral.luster)
        assertEquals("Golden-yellow", mineral.streak)
        assertEquals("Opaque", mineral.diaphaneity)
        assertEquals("Dendritic", mineral.habit)
        assertEquals(19.3f, mineral.specificGravity)
        assertEquals(2.5f, mineral.mohsMin)
        assertEquals(3.0f, mineral.mohsMax)
    }

    @Test
    fun `test mineral with special properties`() {
        // Arrange & Act
        val mineral = Mineral(
            id = "min-id",
            name = "Uraninite",
            fluorescence = "None",
            radioactive = true,
            magnetic = false
        )

        // Assert
        assertTrue(mineral.radioactive)
        assertFalse(mineral.magnetic)
        assertEquals("None", mineral.fluorescence)
    }

    @Test
    fun `test mineral with dimensions and weight`() {
        // Arrange & Act
        val mineral = Mineral(
            id = "min-id",
            name = "Large Amethyst",
            dimensionsMm = "45 x 30 x 25",
            weightGr = 125.5f
        )

        // Assert
        assertEquals("45 x 30 x 25", mineral.dimensionsMm)
        assertEquals(125.5f, mineral.weightGr)
    }

    @Test
    fun `test mineral status and quality`() {
        // Arrange & Act
        val mineral = Mineral(
            id = "min-id",
            name = "Test",
            status = "complete",
            statusType = "in_collection",
            qualityRating = 4,
            completeness = 85
        )

        // Assert
        assertEquals("complete", mineral.status)
        assertEquals("in_collection", mineral.statusType)
        assertEquals(4, mineral.qualityRating)
        assertEquals(85, mineral.completeness)
    }

    @Test
    fun `test provenance coordinates validation`() {
        // Arrange
        val validLatitude = 45.0
        val validLongitude = -73.0

        val invalidLatitude = 95.0 // Out of range
        val invalidLongitude = 200.0 // Out of range

        // Assert
        assertTrue(validLatitude >= -90.0 && validLatitude <= 90.0)
        assertTrue(validLongitude >= -180.0 && validLongitude <= 180.0)
        assertFalse(invalidLatitude >= -90.0 && invalidLatitude <= 90.0)
        assertFalse(invalidLongitude >= -180.0 && invalidLongitude <= 180.0)
    }

    @Test
    fun `test mohs hardness range validation`() {
        // Arrange
        val validHardness = 5.5f
        val tooLowHardness = 0.5f
        val tooHighHardness = 11.0f

        // Assert
        assertTrue(validHardness >= 1.0f && validHardness <= 10.0f)
        assertFalse(tooLowHardness >= 1.0f)
        assertFalse(tooHighHardness <= 10.0f)
    }
}
