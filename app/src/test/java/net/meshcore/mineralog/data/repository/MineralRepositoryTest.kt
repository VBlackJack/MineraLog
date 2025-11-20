package net.meshcore.mineralog.data.repository

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.dao.MineralComponentDao
import net.meshcore.mineralog.data.local.dao.MineralDaoComposite
import net.meshcore.mineralog.data.local.dao.PhotoDao
import net.meshcore.mineralog.data.local.dao.ProvenanceDao
import net.meshcore.mineralog.data.local.dao.SimplePropertiesDao
import net.meshcore.mineralog.data.local.dao.StorageDao
import net.meshcore.mineralog.data.local.entity.MineralComponentEntity
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.PhotoEntity
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity
import net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity
import net.meshcore.mineralog.data.local.entity.StorageEntity
import net.meshcore.mineralog.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests for MineralRepositoryImpl - Refactored Architecture (v3.2.0+)
 *
 * Tests the refactored data loading pattern where:
 * - SIMPLE minerals load physical/chemical properties from SimplePropertiesEntity
 * - AGGREGATE minerals load components from MineralComponentEntity
 * - Repository uses batch loading to prevent N+1 query problem
 *
 * Critical Test Scenarios:
 * 1. SIMPLE mineral → mohsMin loaded from SimplePropertiesEntity, components empty
 * 2. AGGREGATE mineral → mohsMin null, components populated
 * 3. Type switch (SIMPLE↔AGGREGATE) → SimpleProperties deleted when switching to AGGREGATE
 */
class MineralRepositoryTest {

    private lateinit var database: MineraLogDatabase
    private lateinit var mineralDao: MineralDaoComposite
    private lateinit var simplePropertiesDao: SimplePropertiesDao
    private lateinit var provenanceDao: ProvenanceDao
    private lateinit var storageDao: StorageDao
    private lateinit var photoDao: PhotoDao
    private lateinit var mineralComponentDao: MineralComponentDao
    private lateinit var repository: MineralRepository

    @BeforeEach
    fun setup() {
        database = mockk(relaxed = true)
        mineralDao = mockk(relaxed = true)
        simplePropertiesDao = mockk(relaxed = true)
        provenanceDao = mockk(relaxed = true)
        storageDao = mockk(relaxed = true)
        photoDao = mockk(relaxed = true)
        mineralComponentDao = mockk(relaxed = true)

        // Mock withTransaction to execute the block immediately
        val transactionLambda = slot<suspend () -> Any?>()
        coEvery { database.withTransaction(capture(transactionLambda)) } coAnswers {
            transactionLambda.captured.invoke()
        }

        repository = MineralRepositoryImpl(
            database,
            mineralDao,
            simplePropertiesDao,
            provenanceDao,
            storageDao,
            photoDao,
            mineralComponentDao,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    // ==================== CRITICAL SCENARIO 1: SIMPLE Mineral ====================
    // Given: MineralEntity with type="SIMPLE" + SimplePropertiesEntity with mohsMin=7.0
    // When: repository.getById() is called
    // Then: Result should have mohsMin=7.0 (from properties), components empty

    @Test
    fun `getById returns SIMPLE mineral with properties from SimplePropertiesEntity`() = runTest {
        // Given: SIMPLE mineral with properties
        val mineralId = "quartz-1"
        val mineralEntity = MineralEntity(
            id = mineralId,
            name = "Quartz",
            type = "SIMPLE",
            magnetic = false,
            radioactive = false,
            dimensionsMm = "50x30x20",
            weightGr = 150.0f,
            dominantColor = "White",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val simpleProperties = SimplePropertiesEntity(
            id = "${mineralId}_props",
            mineralId = mineralId,
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

        // Mock DAO calls
        coEvery { mineralDao.getById(mineralId) } returns mineralEntity
        coEvery { simplePropertiesDao.getByMineralIds(listOf(mineralId)) } returns listOf(simpleProperties)
        coEvery { provenanceDao.getByMineralIds(listOf(mineralId)) } returns emptyList()
        coEvery { storageDao.getByMineralIds(listOf(mineralId)) } returns emptyList()
        coEvery { photoDao.getByMineralIds(listOf(mineralId)) } returns emptyList()
        coEvery { mineralComponentDao.getByAggregateIds(emptyList()) } returns emptyList()

        // When
        val result = repository.getById(mineralId)

        // Then: Verify SIMPLE mineral reconstruction
        assertNotNull(result)
        assertEquals("Quartz", result?.name)
        assertEquals(MineralType.SIMPLE, result?.mineralType)

        // ✅ CRITICAL: Physical properties loaded from SimplePropertiesEntity
        assertEquals("Silicates", result?.group)
        assertEquals("SiO₂", result?.formula)
        assertEquals(7.0f, result?.mohsMin)
        assertEquals(7.0f, result?.mohsMax)
        assertEquals(2.65f, result?.specificGravity)
        assertEquals("Hexagonal", result?.crystalSystem)
        assertEquals("Vitreous", result?.luster)

        // ✅ CRITICAL: Components should be empty for SIMPLE minerals
        assertTrue(result?.components?.isEmpty() == true)

        // ✅ CRITICAL: Verify batch loading (no N+1 queries)
        coVerify(exactly = 1) { simplePropertiesDao.getByMineralIds(listOf(mineralId)) }
        coVerify(exactly = 0) { mineralComponentDao.getByAggregateIds(any()) }
    }

    // ==================== CRITICAL SCENARIO 2: AGGREGATE Mineral ====================
    // Given: MineralEntity with type="AGGREGATE" + 2 MineralComponentEntity
    // When: repository.getById() is called
    // Then: Result should have mohsMin null, components populated

    @Test
    fun `getById returns AGGREGATE mineral with components, no SimpleProperties`() = runTest {
        // Given: AGGREGATE mineral with components
        val mineralId = "granite-1"
        val mineralEntity = MineralEntity(
            id = mineralId,
            name = "Granite Rose",
            type = "AGGREGATE",
            rockType = "Granite",
            texture = "Grenu",
            dominantMinerals = "Quartz, Feldspath, Mica",
            interestingFeatures = "Cristaux de feldspath rose",
            magnetic = false,
            radioactive = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val component1 = MineralComponentEntity(
            id = "comp-1",
            aggregateId = mineralId,
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
            aggregateId = mineralId,
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

        // Mock DAO calls
        coEvery { mineralDao.getById(mineralId) } returns mineralEntity
        coEvery { simplePropertiesDao.getByMineralIds(emptyList()) } returns emptyList()
        coEvery { provenanceDao.getByMineralIds(listOf(mineralId)) } returns emptyList()
        coEvery { storageDao.getByMineralIds(listOf(mineralId)) } returns emptyList()
        coEvery { photoDao.getByMineralIds(listOf(mineralId)) } returns emptyList()
        coEvery { mineralComponentDao.getByAggregateIds(listOf(mineralId)) } returns listOf(component1, component2)

        // When
        val result = repository.getById(mineralId)

        // Then: Verify AGGREGATE mineral reconstruction
        assertNotNull(result)
        assertEquals("Granite Rose", result?.name)
        assertEquals(MineralType.AGGREGATE, result?.mineralType)

        // ✅ CRITICAL: Physical properties should be NULL for AGGREGATE minerals
        assertNull(result?.group)
        assertNull(result?.formula)
        assertNull(result?.mohsMin)
        assertNull(result?.mohsMax)
        assertNull(result?.specificGravity)

        // ✅ CRITICAL: Aggregate-specific fields populated
        assertEquals("Granite", result?.rockType)
        assertEquals("Grenu", result?.texture)
        assertEquals("Quartz, Feldspath, Mica", result?.dominantMinerals)

        // ✅ CRITICAL: Components should be populated for AGGREGATE minerals
        assertEquals(2, result?.components?.size)
        assertEquals("Quartz", result?.components?.get(0)?.mineralName)
        assertEquals(30.0f, result?.components?.get(0)?.percentage)
        assertEquals(7.0f, result?.components?.get(0)?.mohsMin)
        assertEquals("Feldspath", result?.components?.get(1)?.mineralName)
        assertEquals(60.0f, result?.components?.get(1)?.percentage)

        // ✅ CRITICAL: Verify batch loading (no N+1 queries)
        coVerify(exactly = 0) { simplePropertiesDao.getByMineralIds(any()) }
        coVerify(exactly = 1) { mineralComponentDao.getByAggregateIds(listOf(mineralId)) }
    }

    // ==================== CRITICAL SCENARIO 3: Batch Loading Optimization ====================
    // Given: 3 minerals (2 SIMPLE, 1 AGGREGATE)
    // When: repository.getByIds() is called
    // Then: Should batch-load properties and components (6 queries total, not 81)

    @Test
    fun `getByIds uses batch loading to avoid N+1 problem`() = runTest {
        // Given: 3 minerals (2 SIMPLE, 1 AGGREGATE)
        val entities = listOf(
            MineralEntity(id = "mineral-1", name = "Quartz", type = "SIMPLE", createdAt = Instant.now(), updatedAt = Instant.now()),
            MineralEntity(id = "mineral-2", name = "Calcite", type = "SIMPLE", createdAt = Instant.now(), updatedAt = Instant.now()),
            MineralEntity(id = "mineral-3", name = "Granite", type = "AGGREGATE", createdAt = Instant.now(), updatedAt = Instant.now())
        )
        val simpleProperties = listOf(
            SimplePropertiesEntity(id = "mineral-1_props", mineralId = "mineral-1", group = "Silicates", mohsMin = 7.0f, mohsMax = 7.0f),
            SimplePropertiesEntity(id = "mineral-2_props", mineralId = "mineral-2", group = "Carbonates", mohsMin = 3.0f, mohsMax = 3.0f)
        )
        val components = listOf(
            MineralComponentEntity(id = "comp-1", aggregateId = "mineral-3", displayOrder = 0, mineralName = "Quartz", mineralGroup = "Silicates", percentage = 30.0f, role = "MAJOR", createdAt = Instant.now(), updatedAt = Instant.now())
        )

        // Mock batch DAO calls
        coEvery { mineralDao.getByIds(listOf("mineral-1", "mineral-2", "mineral-3")) } returns entities
        coEvery { simplePropertiesDao.getByMineralIds(listOf("mineral-1", "mineral-2")) } returns simpleProperties
        coEvery { provenanceDao.getByMineralIds(listOf("mineral-1", "mineral-2", "mineral-3")) } returns emptyList()
        coEvery { storageDao.getByMineralIds(listOf("mineral-1", "mineral-2", "mineral-3")) } returns emptyList()
        coEvery { photoDao.getByMineralIds(listOf("mineral-1", "mineral-2", "mineral-3")) } returns emptyList()
        coEvery { mineralComponentDao.getByAggregateIds(listOf("mineral-3")) } returns components

        // When
        val result = repository.getByIds(listOf("mineral-1", "mineral-2", "mineral-3"))

        // Then
        assertEquals(3, result.size)

        // ✅ CRITICAL: Verify batch loading - exactly 1 call per DAO (total 6 queries)
        coVerify(exactly = 1) { mineralDao.getByIds(any()) }
        coVerify(exactly = 1) { simplePropertiesDao.getByMineralIds(any()) }
        coVerify(exactly = 1) { provenanceDao.getByMineralIds(any()) }
        coVerify(exactly = 1) { storageDao.getByMineralIds(any()) }
        coVerify(exactly = 1) { photoDao.getByMineralIds(any()) }
        coVerify(exactly = 1) { mineralComponentDao.getByAggregateIds(any()) }

        // ✅ CRITICAL: Verify SIMPLE minerals have properties
        val quartz = result.find { it.name == "Quartz" }
        assertNotNull(quartz)
        assertEquals(7.0f, quartz?.mohsMin)
        assertEquals("Silicates", quartz?.group)
        assertTrue(quartz?.components?.isEmpty() == true)

        // ✅ CRITICAL: Verify AGGREGATE mineral has components
        val granite = result.find { it.name == "Granite" }
        assertNotNull(granite)
        assertNull(granite?.mohsMin)
        assertNull(granite?.group)
        assertEquals(1, granite?.components?.size)
    }

    // ==================== INSERT & UPDATE Tests ====================

    @Test
    fun `insert saves SIMPLE mineral with SimpleProperties`() = runTest {
        // Given: SIMPLE mineral with properties
        val mineral = Mineral(
            id = "quartz-1",
            name = "Quartz",
            mineralType = MineralType.SIMPLE,
            group = "Silicates",
            formula = "SiO₂",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            specificGravity = 2.65f,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // When
        val result = repository.insert(mineral)

        // Then
        assertEquals("quartz-1", result)
        coVerify { mineralDao.insert(any()) }

        // ✅ CRITICAL: Verify SimpleProperties saved for SIMPLE mineral
        coVerify { simplePropertiesDao.insert(match {
            it.mineralId == "quartz-1" && it.mohsMin == 7.0f && it.group == "Silicates"
        }) }

        // ✅ CRITICAL: No components saved for SIMPLE mineral
        coVerify(exactly = 1) { mineralComponentDao.deleteByAggregateId("quartz-1") }
        coVerify(exactly = 0) { mineralComponentDao.insertAll(any()) }
    }

    @Test
    fun `insert saves AGGREGATE mineral with components, no SimpleProperties`() = runTest {
        // Given: AGGREGATE mineral with components
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
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val mineral = Mineral(
            id = "granite-1",
            name = "Granite",
            mineralType = MineralType.AGGREGATE,
            rockType = "Granite",
            components = listOf(component),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // When
        val result = repository.insert(mineral)

        // Then
        assertEquals("granite-1", result)
        coVerify { mineralDao.insert(any()) }

        // ✅ CRITICAL: No SimpleProperties saved for AGGREGATE mineral
        coVerify(exactly = 0) { simplePropertiesDao.insert(any()) }

        // ✅ CRITICAL: Components saved for AGGREGATE mineral
        coVerify { mineralComponentDao.deleteByAggregateId("granite-1") }
        coVerify { mineralComponentDao.insertAll(match { it.size == 1 && it[0].mineralName == "Quartz" }) }
    }

    @Test
    fun `update handles type switch from SIMPLE to AGGREGATE`() = runTest {
        // Given: Mineral switched from SIMPLE to AGGREGATE
        val component = MineralComponent(
            id = "comp-1",
            mineralName = "Quartz",
            mineralGroup = "Silicates",
            percentage = 30.0f,
            role = ComponentRole.MAJOR,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val mineral = Mineral(
            id = "mineral-1",
            name = "Granite",
            mineralType = MineralType.AGGREGATE,
            rockType = "Granite",
            components = listOf(component),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // When
        repository.update(mineral)

        // Then
        coVerify { mineralDao.update(any()) }

        // ✅ CRITICAL: SimpleProperties deleted when switching to AGGREGATE
        coVerify { simplePropertiesDao.deleteByMineralId("mineral-1") }

        // ✅ CRITICAL: Components inserted for new AGGREGATE type
        coVerify { mineralComponentDao.deleteByAggregateId("mineral-1") }
        coVerify { mineralComponentDao.insertAll(match { it.size == 1 }) }
    }

    @Test
    fun `update handles type switch from AGGREGATE to SIMPLE`() = runTest {
        // Given: Mineral switched from AGGREGATE to SIMPLE
        val mineral = Mineral(
            id = "mineral-1",
            name = "Quartz",
            mineralType = MineralType.SIMPLE,
            group = "Silicates",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // When
        repository.update(mineral)

        // Then
        coVerify { mineralDao.update(any()) }

        // ✅ CRITICAL: SimpleProperties inserted when switching to SIMPLE
        coVerify { simplePropertiesDao.insert(match {
            it.mineralId == "mineral-1" && it.mohsMin == 7.0f
        }) }

        // ✅ CRITICAL: Components deleted when switching to SIMPLE
        coVerify { mineralComponentDao.deleteByAggregateId("mineral-1") }
        coVerify(exactly = 0) { mineralComponentDao.insertAll(any()) }
    }

    // ==================== DELETE Tests ====================

    @Test
    fun `delete removes mineral with cascade deletion including SimpleProperties`() = runTest {
        // When
        repository.delete("mineral-1")

        // Then - verify cascade deletion in correct order
        coVerify { provenanceDao.deleteByMineralId("mineral-1") }
        coVerify { storageDao.deleteByMineralId("mineral-1") }
        coVerify { photoDao.deleteByMineralId("mineral-1") }
        coVerify { mineralComponentDao.deleteByAggregateId("mineral-1") }

        // ✅ CRITICAL: SimpleProperties also deleted (defense-in-depth)
        coVerify { simplePropertiesDao.deleteByMineralId("mineral-1") }

        coVerify { mineralDao.deleteById("mineral-1") }
    }

    @Test
    fun `deleteByIds performs batch deletion with cascade`() = runTest {
        // Given
        val ids = listOf("mineral-1", "mineral-2", "mineral-3")

        // When
        repository.deleteByIds(ids)

        // Then
        coVerify { provenanceDao.deleteByMineralIds(ids) }
        coVerify { storageDao.deleteByMineralIds(ids) }
        coVerify { photoDao.deleteByMineralIds(ids) }
        coVerify { mineralComponentDao.deleteByAggregateIds(ids) }

        // ✅ CRITICAL: SimpleProperties batch deletion
        coVerify { simplePropertiesDao.deleteByMineralIds(ids) }

        coVerify { mineralDao.deleteByIds(ids) }
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `getById returns null when mineral not found`() = runTest {
        // Given
        coEvery { mineralDao.getById("invalid-id") } returns null

        // When
        val result = repository.getById("invalid-id")

        // Then
        assertNull(result)
    }

    @Test
    fun `getByIds returns empty list for empty input`() = runTest {
        // When
        val result = repository.getByIds(emptyList())

        // Then
        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { mineralDao.getByIds(any()) }
    }

    @Test
    fun `SIMPLE mineral without SimpleProperties returns with null properties`() = runTest {
        // Given: SIMPLE mineral but no corresponding SimplePropertiesEntity (data inconsistency)
        val mineralId = "quartz-1"
        val mineralEntity = MineralEntity(
            id = mineralId,
            name = "Quartz",
            type = "SIMPLE",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { mineralDao.getById(mineralId) } returns mineralEntity
        coEvery { simplePropertiesDao.getByMineralIds(listOf(mineralId)) } returns emptyList()
        coEvery { provenanceDao.getByMineralIds(listOf(mineralId)) } returns emptyList()
        coEvery { storageDao.getByMineralIds(listOf(mineralId)) } returns emptyList()
        coEvery { photoDao.getByMineralIds(listOf(mineralId)) } returns emptyList()
        coEvery { mineralComponentDao.getByAggregateIds(emptyList()) } returns emptyList()

        // When
        val result = repository.getById(mineralId)

        // Then: Should still return mineral, but with null properties
        assertNotNull(result)
        assertEquals("Quartz", result?.name)
        assertEquals(MineralType.SIMPLE, result?.mineralType)
        assertNull(result?.mohsMin)
        assertNull(result?.group)
        assertTrue(result?.components?.isEmpty() == true)
    }

    // ==================== ADDITIONAL CRUD Tests ====================

    @Test
    fun `deleteAll removes all minerals and related entities including SimpleProperties`() = runTest {
        // When
        repository.deleteAll()

        // Then
        coVerify { mineralDao.deleteAll() }
        coVerify { provenanceDao.deleteAll() }
        coVerify { storageDao.deleteAll() }
        coVerify { photoDao.deleteAll() }
        coVerify { mineralComponentDao.deleteAll() }

        // ✅ CRITICAL: SimpleProperties also deleted
        coVerify { simplePropertiesDao.deleteAll() }
    }

    @Test
    fun `getCount returns mineral count`() = runTest {
        // Given
        coEvery { mineralDao.getCount() } returns 42

        // When
        val result = repository.getCount()

        // Then
        assertEquals(42, result)
    }

    @Test
    fun `getCountFlow emits mineral count`() = runTest {
        // Given
        every { mineralDao.getCountFlow() } returns flowOf(42, 43, 44)

        // When & Then
        repository.getCountFlow().test {
            assertEquals(42, awaitItem())
            assertEquals(43, awaitItem())
            assertEquals(44, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getAllUniqueTags parses and returns sorted unique tags`() = runTest {
        // Given
        val tagStrings = listOf(
            "fluorescent, blue, rare",
            "fluorescent, green",
            "collector, blue"
        )
        coEvery { mineralDao.getAllTags() } returns tagStrings

        // When
        val result = repository.getAllUniqueTags()

        // Then
        assertEquals(listOf("blue", "collector", "fluorescent", "green", "rare"), result)
    }

    @Test
    fun `insertPhoto saves photo entity`() = runTest {
        // Given
        val photo = Photo(
            id = "photo-1",
            mineralId = "mineral-1",
            fileName = "test.jpg",
            type = "NORMAL"
        )

        // When
        repository.insertPhoto(photo)

        // Then
        coVerify { photoDao.insert(any()) }
    }

    @Test
    fun `deletePhoto removes photo by id`() = runTest {
        // When
        repository.deletePhoto("photo-1")

        // Then
        coVerify { photoDao.deleteById("photo-1") }
    }
}
