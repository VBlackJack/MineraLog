package net.meshcore.mineralog.data.mapper

import net.meshcore.mineralog.data.local.entity.MineralComponentEntity
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.PhotoEntity
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity
import net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity
import net.meshcore.mineralog.data.local.entity.StorageEntity
import net.meshcore.mineralog.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

/**
 * Unit tests for EntityMappers - Refactored Architecture (v3.2.0+)
 *
 * Tests the refactored mapping pattern where:
 * - SIMPLE minerals map physical/chemical properties from SimplePropertiesEntity
 * - AGGREGATE minerals map components from MineralComponentEntity
 * - Type safety is enforced at compile time
 */
class EntityMappersTest {

    // ==================== SIMPLE Mineral Mapping Tests ====================

    @Test
    fun `toDomain maps SIMPLE mineral with SimplePropertiesEntity correctly`() {
        // Given: SIMPLE mineral entity + properties entity
        val entity = MineralEntity(
            id = "quartz-1",
            name = "Quartz",
            type = "SIMPLE",
            magnetic = false,
            radioactive = false,
            dimensionsMm = "50x30x20",
            weightGr = 150.0f,
            dominantColor = "White",
            notes = "Beautiful specimen",
            tags = "display,collection",
            status = "complete",
            statusType = "in_collection",
            completeness = 100,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-02T00:00:00Z")
        )
        val simpleProperties = SimplePropertiesEntity(
            id = "quartz-1_props",
            mineralId = "quartz-1",
            group = "Silicates",
            formula = "SiO₂",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            density = 2.65f,
            crystalSystem = "Hexagonal",
            luster = "Vitreous",
            diaphaneity = "Transparent",
            cleavage = "None",
            fracture = "Conchoidal",
            habit = "Prismatic",
            streak = "White",
            fluorescence = "None"
        )

        // When
        val domain = entity.toDomain(simpleProperties = simpleProperties)

        // Then: Basic fields from MineralEntity
        assertEquals("quartz-1", domain.id)
        assertEquals("Quartz", domain.name)
        assertEquals(MineralType.SIMPLE, domain.mineralType)
        assertEquals(false, domain.magnetic)
        assertEquals(false, domain.radioactive)
        assertEquals("50x30x20", domain.dimensionsMm)
        assertEquals(150.0f, domain.weightGr)
        assertEquals("White", domain.dominantColor)
        assertEquals("Beautiful specimen", domain.notes)
        assertEquals(listOf("display", "collection"), domain.tags)

        // Then: Physical/chemical properties from SimplePropertiesEntity
        assertEquals("Silicates", domain.group)
        assertEquals("SiO₂", domain.formula)
        assertEquals(7.0f, domain.mohsMin)
        assertEquals(7.0f, domain.mohsMax)
        assertEquals(2.65f, domain.specificGravity)
        assertEquals("Hexagonal", domain.crystalSystem)
        assertEquals("Vitreous", domain.luster)
        assertEquals("Transparent", domain.diaphaneity)
        assertEquals("None", domain.cleavage)
        assertEquals("Conchoidal", domain.fracture)
        assertEquals("Prismatic", domain.habit)
        assertEquals("White", domain.streak)
        assertEquals("None", domain.fluorescence)

        // Then: Components should be empty for SIMPLE minerals
        assertTrue(domain.components.isEmpty())

        // Then: Aggregate-specific fields should be null
        assertNull(domain.rockType)
        assertNull(domain.texture)
    }

    @Test
    fun `toDomain maps SIMPLE mineral without SimplePropertiesEntity (null properties)`() {
        // Given: SIMPLE mineral entity WITHOUT properties (data inconsistency scenario)
        val entity = MineralEntity(
            id = "quartz-1",
            name = "Quartz",
            type = "SIMPLE",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // When: No simpleProperties parameter provided
        val domain = entity.toDomain(simpleProperties = null)

        // Then: Basic fields mapped
        assertEquals("quartz-1", domain.id)
        assertEquals("Quartz", domain.name)
        assertEquals(MineralType.SIMPLE, domain.mineralType)

        // Then: Physical properties should be NULL (missing data)
        assertNull(domain.group)
        assertNull(domain.formula)
        assertNull(domain.mohsMin)
        assertNull(domain.mohsMax)
        assertNull(domain.specificGravity)
        assertNull(domain.crystalSystem)
    }

    @Test
    fun `toSimplePropertiesEntity converts Mineral to SimplePropertiesEntity correctly`() {
        // Given: SIMPLE mineral domain model
        val mineral = Mineral(
            id = "quartz-1",
            name = "Quartz",
            mineralType = MineralType.SIMPLE,
            group = "Silicates",
            formula = "SiO₂",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            specificGravity = 2.65f,
            crystalSystem = "Hexagonal",
            luster = "Vitreous",
            diaphaneity = "Transparent",
            cleavage = "None",
            fracture = "Conchoidal",
            habit = "Prismatic",
            streak = "White",
            fluorescence = "None",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // When
        val simpleProperties = mineral.toSimplePropertiesEntity()

        // Then
        assertEquals("quartz-1_props", simpleProperties.id)
        assertEquals("quartz-1", simpleProperties.mineralId)
        assertEquals("Silicates", simpleProperties.group)
        assertEquals("SiO₂", simpleProperties.formula)
        assertEquals(7.0f, simpleProperties.mohsMin)
        assertEquals(7.0f, simpleProperties.mohsMax)
        assertEquals(2.65f, simpleProperties.density)
        assertEquals("Hexagonal", simpleProperties.crystalSystem)
        assertEquals("Vitreous", simpleProperties.luster)
        assertEquals("Transparent", simpleProperties.diaphaneity)
        assertEquals("None", simpleProperties.cleavage)
        assertEquals("Conchoidal", simpleProperties.fracture)
        assertEquals("Prismatic", simpleProperties.habit)
        assertEquals("White", simpleProperties.streak)
        assertEquals("None", simpleProperties.fluorescence)
    }

    @Test
    fun `toSimplePropertiesEntity throws exception for AGGREGATE mineral`() {
        // Given: AGGREGATE mineral (invalid for SimpleProperties conversion)
        val mineral = Mineral(
            id = "granite-1",
            name = "Granite",
            mineralType = MineralType.AGGREGATE,
            rockType = "Granite",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // When & Then: Should throw IllegalArgumentException
        val exception = assertThrows<IllegalArgumentException> {
            mineral.toSimplePropertiesEntity()
        }
        assertTrue(exception.message?.contains("SIMPLE") == true)
    }

    // ==================== AGGREGATE Mineral Mapping Tests ====================

    @Test
    fun `toDomain maps AGGREGATE mineral with components correctly`() {
        // Given: AGGREGATE mineral entity + components
        val entity = MineralEntity(
            id = "granite-1",
            name = "Granite Rose",
            type = "AGGREGATE",
            rockType = "Granite",
            texture = "Grenu",
            dominantMinerals = "Quartz, Feldspath, Mica",
            interestingFeatures = "Cristaux de feldspath rose",
            magnetic = false,
            radioactive = false,
            notes = "Beautiful pink granite",
            tags = "collection,display",
            status = "complete",
            statusType = "in_collection",
            completeness = 90,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-02T00:00:00Z")
        )
        val component1 = MineralComponentEntity(
            id = "comp-1",
            aggregateId = "granite-1",
            displayOrder = 0,
            mineralName = "Quartz",
            mineralGroup = "Silicates",
            percentage = 30.0f,
            role = "MAJOR",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            density = 2.65f,
            formula = "SiO₂",
            crystalSystem = "Hexagonal",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val component2 = MineralComponentEntity(
            id = "comp-2",
            aggregateId = "granite-1",
            displayOrder = 1,
            mineralName = "Feldspath",
            mineralGroup = "Silicates",
            percentage = 60.0f,
            role = "MAJOR",
            mohsMin = 6.0f,
            mohsMax = 6.5f,
            density = 2.56f,
            formula = "KAlSi₃O₈",
            crystalSystem = "Monoclinic",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // When
        val domain = entity.toDomain(components = listOf(component1, component2))

        // Then: Basic fields from MineralEntity
        assertEquals("granite-1", domain.id)
        assertEquals("Granite Rose", domain.name)
        assertEquals(MineralType.AGGREGATE, domain.mineralType)

        // Then: Aggregate-specific fields populated
        assertEquals("Granite", domain.rockType)
        assertEquals("Grenu", domain.texture)
        assertEquals("Quartz, Feldspath, Mica", domain.dominantMinerals)
        assertEquals("Cristaux de feldspath rose", domain.interestingFeatures)

        // Then: Physical/chemical properties should be NULL for AGGREGATE
        assertNull(domain.group)
        assertNull(domain.formula)
        assertNull(domain.mohsMin)
        assertNull(domain.mohsMax)
        assertNull(domain.specificGravity)

        // Then: Components should be populated (sorted by displayOrder)
        assertEquals(2, domain.components.size)
        assertEquals("Quartz", domain.components[0].mineralName)
        assertEquals(30.0f, domain.components[0].percentage)
        assertEquals(7.0f, domain.components[0].mohsMin)
        assertEquals(ComponentRole.MAJOR, domain.components[0].role)
        assertEquals("Feldspath", domain.components[1].mineralName)
        assertEquals(60.0f, domain.components[1].percentage)
        assertEquals(6.0f, domain.components[1].mohsMin)
    }

    @Test
    fun `toDomain maps AGGREGATE mineral without components (empty list)`() {
        // Given: AGGREGATE mineral entity WITHOUT components (incomplete data)
        val entity = MineralEntity(
            id = "granite-1",
            name = "Granite",
            type = "AGGREGATE",
            rockType = "Granite",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // When: No components parameter provided
        val domain = entity.toDomain(components = emptyList())

        // Then
        assertEquals("granite-1", domain.id)
        assertEquals("Granite", domain.name)
        assertEquals(MineralType.AGGREGATE, domain.mineralType)
        assertEquals("Granite", domain.rockType)

        // Then: Components should be empty (incomplete data scenario)
        assertTrue(domain.components.isEmpty())

        // Then: Physical properties should be NULL
        assertNull(domain.group)
        assertNull(domain.mohsMin)
    }

    @Test
    fun `toEntity maps AGGREGATE Mineral to MineralEntity correctly`() {
        // Given: AGGREGATE mineral domain model
        val mineral = Mineral(
            id = "granite-1",
            name = "Granite Rose",
            mineralType = MineralType.AGGREGATE,
            rockType = "Granite",
            texture = "Grenu",
            dominantMinerals = "Quartz, Feldspath",
            interestingFeatures = "Pink feldspar crystals",
            magnetic = false,
            radioactive = false,
            notes = "Collector's piece",
            tags = listOf("display", "rare"),
            status = "complete",
            statusType = "in_collection",
            completeness = 95,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-02T00:00:00Z")
        )

        // When
        val entity = mineral.toEntity()

        // Then
        assertEquals("granite-1", entity.id)
        assertEquals("Granite Rose", entity.name)
        assertEquals("AGGREGATE", entity.type)
        assertEquals("Granite", entity.rockType)
        assertEquals("Grenu", entity.texture)
        assertEquals("Quartz, Feldspath", entity.dominantMinerals)
        assertEquals("Pink feldspar crystals", entity.interestingFeatures)
        assertEquals(false, entity.magnetic)
        assertEquals(false, entity.radioactive)
        assertEquals("Collector's piece", entity.notes)
        assertEquals("display,rare", entity.tags)
        assertEquals("complete", entity.status)
        assertEquals("in_collection", entity.statusType)
        assertEquals(95, entity.completeness)
    }

    @Test
    fun `MineralComponent toEntity maps correctly with displayOrder`() {
        // Given: MineralComponent domain model
        val component = MineralComponent(
            id = "comp-1",
            mineralName = "Quartz",
            mineralGroup = "Silicates",
            percentage = 30.0f,
            role = ComponentRole.MAJOR,
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            density = 2.65f,
            formula = "SiO₂",
            crystalSystem = "Hexagonal",
            luster = "Vitreous",
            notes = "Visible crystals",
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-02T00:00:00Z")
        )

        // When
        val entity = component.toEntity(aggregateId = "granite-1", displayOrder = 2)

        // Then
        assertEquals("comp-1", entity.id)
        assertEquals("granite-1", entity.aggregateId)
        assertEquals(2, entity.displayOrder)
        assertEquals("Quartz", entity.mineralName)
        assertEquals("Silicates", entity.mineralGroup)
        assertEquals(30.0f, entity.percentage)
        assertEquals("MAJOR", entity.role)
        assertEquals(7.0f, entity.mohsMin)
        assertEquals(7.0f, entity.mohsMax)
        assertEquals(2.65f, entity.density)
        assertEquals("SiO₂", entity.formula)
        assertEquals("Hexagonal", entity.crystalSystem)
        assertEquals("Vitreous", entity.luster)
        assertEquals("Visible crystals", entity.notes)
    }

    // ==================== Related Entity Mapping Tests ====================

    @Test
    fun `toDomain maps MineralEntity with Provenance, Storage, and Photos`() {
        // Given
        val mineralEntity = MineralEntity(
            id = "mineral-1",
            name = "Quartz",
            type = "SIMPLE",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val provenanceEntity = ProvenanceEntity(
            id = "prov-1",
            mineralId = "mineral-1",
            site = "Mont Blanc",
            locality = "Chamonix",
            country = "France",
            latitude = 45.8326,
            longitude = 6.8652
        )
        val storageEntity = StorageEntity(
            id = "storage-1",
            mineralId = "mineral-1",
            place = "Cabinet A",
            container = "Drawer 3",
            box = "Box 12"
        )
        val photoEntity = PhotoEntity(
            id = "photo-1",
            mineralId = "mineral-1",
            type = net.meshcore.mineralog.data.local.entity.PhotoType.NORMAL,
            caption = "Main view",
            fileName = "mineral-1_main.jpg",
            takenAt = Instant.now()
        )

        // When
        val domain = mineralEntity.toDomain(
            provenance = provenanceEntity,
            storage = storageEntity,
            photos = listOf(photoEntity)
        )

        // Then: Provenance mapped correctly
        assertNotNull(domain.provenance)
        assertEquals("prov-1", domain.provenance?.id)
        assertEquals("Mont Blanc", domain.provenance?.site)
        assertEquals("Chamonix", domain.provenance?.locality)
        assertEquals("France", domain.provenance?.country)
        assertEquals(45.8326, domain.provenance?.latitude)
        assertEquals(6.8652, domain.provenance?.longitude)

        // Then: Storage mapped correctly
        assertNotNull(domain.storage)
        assertEquals("storage-1", domain.storage?.id)
        assertEquals("Cabinet A", domain.storage?.place)
        assertEquals("Drawer 3", domain.storage?.container)
        assertEquals("Box 12", domain.storage?.box)

        // Then: Photos mapped correctly
        assertEquals(1, domain.photos.size)
        assertEquals("photo-1", domain.photos[0].id)
        assertEquals("NORMAL", domain.photos[0].type)
        assertEquals("Main view", domain.photos[0].caption)
        assertEquals("mineral-1_main.jpg", domain.photos[0].fileName)
    }

    @Test
    fun `SimpleProperties toDomain maps all fields correctly`() {
        // Given
        val entity = SimplePropertiesEntity(
            id = "props-1",
            mineralId = "mineral-1",
            group = "Silicates",
            mohsMin = 7.0f,
            mohsMax = 7.5f,
            density = 2.65f,
            formula = "SiO₂",
            crystalSystem = "Hexagonal",
            luster = "Vitreous",
            diaphaneity = "Transparent",
            cleavage = "None",
            fracture = "Conchoidal",
            habit = "Prismatic",
            streak = "White",
            fluorescence = "None",
            colorVariety = "Rose Quartz",
            actualDiaphaneity = "Translucent",
            qualityNotes = "High quality specimen"
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertEquals("Silicates", domain.group)
        assertEquals(7.0f, domain.mohsMin)
        assertEquals(7.5f, domain.mohsMax)
        assertEquals(2.65f, domain.density)
        assertEquals("SiO₂", domain.formula)
        assertEquals("Hexagonal", domain.crystalSystem)
        assertEquals("Vitreous", domain.luster)
        assertEquals("Transparent", domain.diaphaneity)
        assertEquals("None", domain.cleavage)
        assertEquals("Conchoidal", domain.fracture)
        assertEquals("Prismatic", domain.habit)
        assertEquals("White", domain.streak)
        assertEquals("None", domain.fluorescence)
        assertEquals("Rose Quartz", domain.colorVariety)
        assertEquals("Translucent", domain.actualDiaphaneity)
        assertEquals("High quality specimen", domain.qualityNotes)
    }

    @Test
    fun `SimpleProperties toEntity maps domain to entity correctly`() {
        // Given
        val domain = SimpleProperties(
            group = "Silicates",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            density = 2.65f,
            formula = "SiO₂",
            crystalSystem = "Hexagonal",
            luster = "Vitreous",
            diaphaneity = "Transparent",
            cleavage = "None",
            fracture = "Conchoidal",
            habit = "Prismatic",
            streak = "White",
            fluorescence = "None"
        )

        // When
        val entity = domain.toEntity(mineralId = "quartz-1")

        // Then
        assertEquals("quartz-1_props", entity.id)
        assertEquals("quartz-1", entity.mineralId)
        assertEquals("Silicates", entity.group)
        assertEquals(7.0f, entity.mohsMin)
        assertEquals(7.0f, entity.mohsMax)
        assertEquals(2.65f, entity.density)
        assertEquals("SiO₂", entity.formula)
        assertEquals("Hexagonal", entity.crystalSystem)
        assertEquals("Vitreous", entity.luster)
        assertEquals("Transparent", entity.diaphaneity)
        assertEquals("None", entity.cleavage)
        assertEquals("Conchoidal", entity.fracture)
        assertEquals("Prismatic", entity.habit)
        assertEquals("White", entity.streak)
        assertEquals("None", entity.fluorescence)
    }

    // ==================== Edge Cases and Special Scenarios ====================

    @Test
    fun `toDomain handles tags parsing correctly`() {
        // Given
        val entity1 = MineralEntity(id = "1", name = "Test1", tags = "tag1,tag2,tag3", createdAt = Instant.now(), updatedAt = Instant.now())
        val entity2 = MineralEntity(id = "2", name = "Test2", tags = "", createdAt = Instant.now(), updatedAt = Instant.now())
        val entity3 = MineralEntity(id = "3", name = "Test3", tags = null, createdAt = Instant.now(), updatedAt = Instant.now())

        // When
        val domain1 = entity1.toDomain()
        val domain2 = entity2.toDomain()
        val domain3 = entity3.toDomain()

        // Then
        assertEquals(listOf("tag1", "tag2", "tag3"), domain1.tags)
        assertTrue(domain2.tags.isEmpty())
        assertTrue(domain3.tags.isEmpty())
    }

    @Test
    fun `toEntity handles empty tags list correctly`() {
        // Given
        val mineral = Mineral(
            id = "test-id",
            name = "Test",
            tags = emptyList(),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // When
        val entity = mineral.toEntity()

        // Then
        assertEquals("", entity.tags)
    }

    @Test
    fun `toDomain maps MineralType correctly`() {
        // Given
        val simpleEntity = MineralEntity(id = "1", name = "Simple", type = "SIMPLE", createdAt = Instant.now(), updatedAt = Instant.now())
        val aggregateEntity = MineralEntity(id = "2", name = "Aggregate", type = "AGGREGATE", createdAt = Instant.now(), updatedAt = Instant.now())
        val rockEntity = MineralEntity(id = "3", name = "Rock", type = "ROCK", createdAt = Instant.now(), updatedAt = Instant.now())

        // When
        val simpleDomain = simpleEntity.toDomain()
        val aggregateDomain = aggregateEntity.toDomain()
        val rockDomain = rockEntity.toDomain()

        // Then
        assertEquals(MineralType.SIMPLE, simpleDomain.mineralType)
        assertEquals(MineralType.AGGREGATE, aggregateDomain.mineralType)
        assertEquals(MineralType.SIMPLE, rockDomain.mineralType) // Default to SIMPLE for unknown types
    }

    @Test
    fun `toEntity maps MineralType to string correctly`() {
        // Given
        val simple = Mineral(id = "1", name = "Simple", mineralType = MineralType.SIMPLE, createdAt = Instant.now(), updatedAt = Instant.now())
        val aggregate = Mineral(id = "2", name = "Aggregate", mineralType = MineralType.AGGREGATE, createdAt = Instant.now(), updatedAt = Instant.now())
        val rock = Mineral(id = "3", name = "Rock", mineralType = MineralType.ROCK, createdAt = Instant.now(), updatedAt = Instant.now())

        // When
        val simpleEntity = simple.toEntity()
        val aggregateEntity = aggregate.toEntity()
        val rockEntity = rock.toEntity()

        // Then
        assertEquals("SIMPLE", simpleEntity.type)
        assertEquals("AGGREGATE", aggregateEntity.type)
        assertEquals("ROCK", rockEntity.type)
    }

    @Test
    fun `MineralComponent toDomain maps ComponentRole correctly`() {
        // Given
        val majorEntity = MineralComponentEntity(id = "1", aggregateId = "agg-1", displayOrder = 0, mineralName = "Quartz", mineralGroup = "Silicates", percentage = 50f, role = "MAJOR", createdAt = Instant.now(), updatedAt = Instant.now())
        val minorEntity = MineralComponentEntity(id = "2", aggregateId = "agg-1", displayOrder = 1, mineralName = "Mica", mineralGroup = "Silicates", percentage = 10f, role = "MINOR", createdAt = Instant.now(), updatedAt = Instant.now())
        val traceEntity = MineralComponentEntity(id = "3", aggregateId = "agg-1", displayOrder = 2, mineralName = "Zircon", mineralGroup = "Silicates", percentage = 1f, role = "TRACE", createdAt = Instant.now(), updatedAt = Instant.now())

        // When
        val major = majorEntity.toDomain()
        val minor = minorEntity.toDomain()
        val trace = traceEntity.toDomain()

        // Then
        assertEquals(ComponentRole.MAJOR, major.role)
        assertEquals(ComponentRole.MINOR, minor.role)
        assertEquals(ComponentRole.TRACE, trace.role)
    }

    @Test
    fun `components are sorted by displayOrder in toDomain`() {
        // Given: Components in random order
        val entity = MineralEntity(id = "granite-1", name = "Granite", type = "AGGREGATE", createdAt = Instant.now(), updatedAt = Instant.now())
        val component2 = MineralComponentEntity(id = "c2", aggregateId = "granite-1", displayOrder = 2, mineralName = "Mica", mineralGroup = "Silicates", percentage = 10f, role = "MINOR", createdAt = Instant.now(), updatedAt = Instant.now())
        val component0 = MineralComponentEntity(id = "c0", aggregateId = "granite-1", displayOrder = 0, mineralName = "Quartz", mineralGroup = "Silicates", percentage = 30f, role = "MAJOR", createdAt = Instant.now(), updatedAt = Instant.now())
        val component1 = MineralComponentEntity(id = "c1", aggregateId = "granite-1", displayOrder = 1, mineralName = "Feldspath", mineralGroup = "Silicates", percentage = 60f, role = "MAJOR", createdAt = Instant.now(), updatedAt = Instant.now())

        // When: Pass components in random order
        val domain = entity.toDomain(components = listOf(component2, component0, component1))

        // Then: Components should be sorted by displayOrder
        assertEquals(3, domain.components.size)
        assertEquals("Quartz", domain.components[0].mineralName)
        assertEquals("Feldspath", domain.components[1].mineralName)
        assertEquals("Mica", domain.components[2].mineralName)
    }
}
