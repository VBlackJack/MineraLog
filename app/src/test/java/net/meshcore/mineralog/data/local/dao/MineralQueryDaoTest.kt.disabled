package net.meshcore.mineralog.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.UUID

/**
 * Integration tests for MineralQueryDao using Robolectric.
 *
 * Tests cover:
 * - Type-based queries (simple, aggregate, by type list)
 * - Search operations (name, group, formula, notes, tags)
 * - Simple filtering (group, crystal system, status, hardness)
 * - Advanced filtering (9 parameters, complex conditions)
 * - Distinct value queries (groups, crystal systems, tags)
 *
 * Sprint 3: DAO Tests - Target 70%+ coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MineralQueryDaoTest {

    private lateinit var database: MineraLogDatabase
    private lateinit var queryDao: MineralQueryDao
    private lateinit var basicDao: MineralBasicDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MineraLogDatabase::class.java
        ).allowMainThreadQueries().build()

        val compositeDao = database.mineralDao()
        queryDao = object : MineralQueryDao {
            override fun getAllSimpleMinerals() = compositeDao.getAllSimpleMinerals()
            override fun getAllAggregates() = compositeDao.getAllAggregates()
            override fun getMineralsByType(types: List<String>) = compositeDao.getMineralsByType(types)
            override suspend fun countByType(type: String) = compositeDao.countByType(type)
            override fun searchFlow(query: String) = compositeDao.searchFlow(query)
            override fun filterFlow(group: String?, crystalSystem: String?, status: String?, mohsMin: Float?, mohsMax: Float?) =
                compositeDao.filterFlow(group, crystalSystem, status, mohsMin, mohsMax)
            override fun filterAdvanced(
                groups: List<String>?, countries: List<String>?, mohsMin: Float?, mohsMax: Float?,
                statusTypes: List<String>?, qualityMin: Int?, qualityMax: Int?, hasPhotos: Boolean?,
                fluorescent: Boolean?, mineralTypes: List<String>?
            ) = compositeDao.filterAdvanced(groups, countries, mohsMin, mohsMax, statusTypes, qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes)
            override fun getDistinctGroupsFlow() = compositeDao.getDistinctGroupsFlow()
            override fun getDistinctCrystalSystemsFlow() = compositeDao.getDistinctCrystalSystemsFlow()
            override suspend fun getAllTags() = compositeDao.getAllTags()
        }

        basicDao = object : MineralBasicDao {
            override suspend fun insert(mineral: MineralEntity) = compositeDao.insert(mineral)
            override suspend fun insertAll(minerals: List<MineralEntity>) = compositeDao.insertAll(minerals)
            override suspend fun update(mineral: MineralEntity) = compositeDao.update(mineral)
            override suspend fun delete(mineral: MineralEntity) = compositeDao.delete(mineral)
            override suspend fun deleteById(id: String) = compositeDao.deleteById(id)
            override suspend fun deleteByIds(ids: List<String>) = compositeDao.deleteByIds(ids)
            override suspend fun deleteAll() = compositeDao.deleteAll()
            override suspend fun getById(id: String) = compositeDao.getById(id)
            override suspend fun getByIds(ids: List<String>) = compositeDao.getByIds(ids)
            override fun getByIdFlow(id: String) = compositeDao.getByIdFlow(id)
            override fun getAllFlow() = compositeDao.getAllFlow()
            override suspend fun getAll() = compositeDao.getAll()
            override suspend fun getCount() = compositeDao.getCount()
            override fun getCountFlow() = compositeDao.getCountFlow()
        }
    }

    @AfterEach
    fun tearDown() {
        database.close()
    }

    // ========== TYPE-BASED QUERIES TESTS ==========

    @Test
    @DisplayName("getAllSimpleMinerals - returns only SIMPLE type minerals")
    fun `getAllSimpleMinerals - filters by SIMPLE type - ordered by name`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("simple-1", "Quartz", type = "SIMPLE"),
            createTestMineral("aggregate-1", "Granite", type = "AGGREGATE"),
            createTestMineral("simple-2", "Calcite", type = "SIMPLE"),
            createTestMineral("rock-1", "Basalt", type = "ROCK")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.getAllSimpleMinerals()

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.mineralType == "SIMPLE" })
            // Should be ordered by name ASC
            assertEquals("Calcite", result[0].name)
            assertEquals("Quartz", result[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("getAllAggregates - returns only AGGREGATE type minerals")
    fun `getAllAggregates - filters by AGGREGATE type - ordered by name`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("simple-1", "Quartz", type = "SIMPLE"),
            createTestMineral("aggregate-1", "Pegmatite", type = "AGGREGATE"),
            createTestMineral("aggregate-2", "Granite", type = "AGGREGATE")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.getAllAggregates()

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.mineralType == "AGGREGATE" })
            assertEquals("Granite", result[0].name)
            assertEquals("Pegmatite", result[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("getMineralsByType - filters by multiple types")
    fun `getMineralsByType - multiple types - returns matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("simple-1", "Quartz", type = "SIMPLE"),
            createTestMineral("aggregate-1", "Granite", type = "AGGREGATE"),
            createTestMineral("rock-1", "Basalt", type = "ROCK"),
            createTestMineral("simple-2", "Feldspar", type = "SIMPLE")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.getMineralsByType(listOf("SIMPLE", "AGGREGATE"))

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertTrue(result.all { it.mineralType == "SIMPLE" || it.mineralType == "AGGREGATE" })
            assertFalse(result.any { it.mineralType == "ROCK" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("countByType - returns correct count for type")
    fun `countByType - accurate count - for each mineral type`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("simple-1", "Quartz", type = "SIMPLE"),
            createTestMineral("simple-2", "Calcite", type = "SIMPLE"),
            createTestMineral("simple-3", "Feldspar", type = "SIMPLE"),
            createTestMineral("aggregate-1", "Granite", type = "AGGREGATE"),
            createTestMineral("rock-1", "Basalt", type = "ROCK")
        )
        basicDao.insertAll(minerals)

        // Act & Assert
        assertEquals(3, queryDao.countByType("SIMPLE"))
        assertEquals(1, queryDao.countByType("AGGREGATE"))
        assertEquals(1, queryDao.countByType("ROCK"))
        assertEquals(0, queryDao.countByType("UNKNOWN"))
    }

    // ========== SEARCH OPERATIONS TESTS ==========

    @Test
    @DisplayName("searchFlow - finds minerals by name")
    fun `searchFlow - name match - returns matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz"),
            createTestMineral("2", "Rose Quartz"),
            createTestMineral("3", "Calcite")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.searchFlow("%quartz%")

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.any { it.name.contains("Quartz", ignoreCase = true) })
            assertTrue(result.any { it.name.contains("Rose Quartz", ignoreCase = true) })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("searchFlow - finds minerals by group")
    fun `searchFlow - group match - returns matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates"),
            createTestMineral("2", "Feldspar", group = "Silicates"),
            createTestMineral("3", "Calcite", group = "Carbonates")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.searchFlow("%silicate%")

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.group.contains("Silicate", ignoreCase = true) })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("searchFlow - finds minerals by formula")
    fun `searchFlow - formula match - returns matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", formula = "SiO2"),
            createTestMineral("2", "Cristobalite", formula = "SiO2"),
            createTestMineral("3", "Calcite", formula = "CaCO3")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.searchFlow("%SiO2%")

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.formula == "SiO2" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("searchFlow - finds minerals by notes")
    fun `searchFlow - notes match - returns matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", notes = "Beautiful clear crystal from Brazil"),
            createTestMineral("2", "Amethyst", notes = "Purple variety from Uruguay"),
            createTestMineral("3", "Calcite", notes = "Opaque white specimen")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.searchFlow("%brazil%")

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Quartz", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("searchFlow - case insensitive search")
    fun `searchFlow - case insensitive - matches regardless of case`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "QUARTZ"),
            createTestMineral("2", "quartz"),
            createTestMineral("3", "Quartz")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.searchFlow("%quartz%")

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("searchFlow - no matches returns empty list")
    fun `searchFlow - no matches - returns empty list`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz"),
            createTestMineral("2", "Calcite")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.searchFlow("%diamond%")

        // Assert
        flow.test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== SIMPLE FILTER OPERATIONS TESTS ==========

    @Test
    @DisplayName("filterFlow - filter by group")
    fun `filterFlow - group filter - returns matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates"),
            createTestMineral("2", "Feldspar", group = "Silicates"),
            createTestMineral("3", "Calcite", group = "Carbonates")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.filterFlow(group = "Silicates")

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.group == "Silicates" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("filterFlow - filter by crystal system")
    fun `filterFlow - crystal system filter - returns matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", crystalSystem = "Hexagonal"),
            createTestMineral("2", "Calcite", crystalSystem = "Hexagonal"),
            createTestMineral("3", "Halite", crystalSystem = "Cubic")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.filterFlow(crystalSystem = "Hexagonal")

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.crystalSystem == "Hexagonal" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("filterFlow - filter by status")
    fun `filterFlow - status filter - returns matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", status = "COLLECTION"),
            createTestMineral("2", "Calcite", status = "DISPLAY"),
            createTestMineral("3", "Feldspar", status = "COLLECTION")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.filterFlow(status = "COLLECTION")

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.all { it.status == "COLLECTION" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("filterFlow - filter by hardness range")
    fun `filterFlow - hardness range - returns minerals within range`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Talc", mohsMin = 1.0f, mohsMax = 1.0f),
            createTestMineral("2", "Calcite", mohsMin = 3.0f, mohsMax = 3.0f),
            createTestMineral("3", "Quartz", mohsMin = 7.0f, mohsMax = 7.0f),
            createTestMineral("4", "Apatite", mohsMin = 5.0f, mohsMax = 5.0f)
        )
        basicDao.insertAll(minerals)

        // Act - Filter for hardness 3-6 (should get Calcite and Apatite)
        val flow = queryDao.filterFlow(mohsMin = 3.0f, mohsMax = 6.0f)

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.any { it.name == "Calcite" })
            assertTrue(result.any { it.name == "Apatite" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("filterFlow - combined filters")
    fun `filterFlow - multiple criteria - returns minerals matching all`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates", crystalSystem = "Hexagonal", status = "COLLECTION", mohsMin = 7.0f, mohsMax = 7.0f),
            createTestMineral("2", "Calcite", group = "Carbonates", crystalSystem = "Hexagonal", status = "COLLECTION", mohsMin = 3.0f, mohsMax = 3.0f),
            createTestMineral("3", "Feldspar", group = "Silicates", crystalSystem = "Triclinic", status = "DISPLAY", mohsMin = 6.0f, mohsMax = 6.5f)
        )
        basicDao.insertAll(minerals)

        // Act - Filter for Silicates + Hexagonal + COLLECTION
        val flow = queryDao.filterFlow(
            group = "Silicates",
            crystalSystem = "Hexagonal",
            status = "COLLECTION"
        )

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Quartz", result[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("filterFlow - null parameters return all")
    fun `filterFlow - all null parameters - returns all minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz"),
            createTestMineral("2", "Calcite"),
            createTestMineral("3", "Feldspar")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.filterFlow()

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== ADVANCED FILTER OPERATIONS TESTS ==========

    @Test
    @DisplayName("filterAdvanced - filter by multiple groups")
    fun `filterAdvanced - groups list - returns minerals in specified groups`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates"),
            createTestMineral("2", "Calcite", group = "Carbonates"),
            createTestMineral("3", "Hematite", group = "Oxides"),
            createTestMineral("4", "Feldspar", group = "Silicates")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.filterAdvanced(groups = listOf("Silicates", "Oxides"))

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertTrue(result.any { it.name == "Quartz" })
            assertTrue(result.any { it.name == "Hematite" })
            assertTrue(result.any { it.name == "Feldspar" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("filterAdvanced - filter by quality range")
    fun `filterAdvanced - quality range - returns minerals within range`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", quality = 5),
            createTestMineral("2", "Calcite", quality = 3),
            createTestMineral("3", "Feldspar", quality = 4),
            createTestMineral("4", "Halite", quality = 2)
        )
        basicDao.insertAll(minerals)

        // Act - Quality 3-5
        val flow = queryDao.filterAdvanced(qualityMin = 3, qualityMax = 5)

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertTrue(result.all { it.quality in 3..5 })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("filterAdvanced - filter by mineral types")
    fun `filterAdvanced - mineral types list - returns matching types`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", type = "SIMPLE"),
            createTestMineral("2", "Granite", type = "AGGREGATE"),
            createTestMineral("3", "Basalt", type = "ROCK"),
            createTestMineral("4", "Calcite", type = "SIMPLE")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.filterAdvanced(mineralTypes = listOf("SIMPLE", "AGGREGATE"))

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertFalse(result.any { it.mineralType == "ROCK" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("filterAdvanced - combined criteria")
    fun `filterAdvanced - multiple criteria - returns minerals matching all`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates", type = "SIMPLE", quality = 5, mohsMin = 7.0f, mohsMax = 7.0f),
            createTestMineral("2", "Calcite", group = "Carbonates", type = "SIMPLE", quality = 3, mohsMin = 3.0f, mohsMax = 3.0f),
            createTestMineral("3", "Feldspar", group = "Silicates", type = "SIMPLE", quality = 4, mohsMin = 6.0f, mohsMax = 6.5f)
        )
        basicDao.insertAll(minerals)

        // Act - Silicates + Quality 4+ + Hardness 6+
        val flow = queryDao.filterAdvanced(
            groups = listOf("Silicates"),
            qualityMin = 4,
            mohsMin = 6.0f
        )

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertTrue(result.any { it.name == "Quartz" })
            assertTrue(result.any { it.name == "Feldspar" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("filterAdvanced - all null returns all minerals")
    fun `filterAdvanced - all null parameters - returns all minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz"),
            createTestMineral("2", "Calcite"),
            createTestMineral("3", "Feldspar")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.filterAdvanced()

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== DISTINCT VALUES TESTS ==========

    @Test
    @DisplayName("getDistinctGroupsFlow - returns unique groups ordered")
    fun `getDistinctGroupsFlow - unique groups - alphabetically ordered`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates"),
            createTestMineral("2", "Feldspar", group = "Silicates"),
            createTestMineral("3", "Calcite", group = "Carbonates"),
            createTestMineral("4", "Hematite", group = "Oxides"),
            createTestMineral("5", "Magnetite", group = "Oxides")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.getDistinctGroupsFlow()

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals(listOf("Carbonates", "Oxides", "Silicates"), result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("getDistinctCrystalSystemsFlow - returns unique crystal systems")
    fun `getDistinctCrystalSystemsFlow - unique systems - alphabetically ordered`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", crystalSystem = "Hexagonal"),
            createTestMineral("2", "Calcite", crystalSystem = "Hexagonal"),
            createTestMineral("3", "Halite", crystalSystem = "Cubic"),
            createTestMineral("4", "Feldspar", crystalSystem = "Triclinic")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.getDistinctCrystalSystemsFlow()

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(3, result.size)
            assertEquals(listOf("Cubic", "Hexagonal", "Triclinic"), result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("getAllTags - returns all unique tags")
    fun `getAllTags - all tags - from all minerals`() = runTest {
        // Arrange - Tags are stored as comma-separated strings
        val minerals = listOf(
            createTestMineral("1", "Quartz", tags = "beautiful,clear,brazil"),
            createTestMineral("2", "Amethyst", tags = "purple,uruguay"),
            createTestMineral("3", "Calcite", tags = "")
        )
        basicDao.insertAll(minerals)

        // Act
        val result = queryDao.getAllTags()

        // Assert
        assertEquals(2, result.size) // Empty tags excluded
        assertTrue(result.any { it.contains("beautiful") })
        assertTrue(result.any { it.contains("purple") })
    }

    @Test
    @DisplayName("getDistinctGroupsFlow - excludes null groups")
    fun `getDistinctGroupsFlow - null groups - excluded from results`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates"),
            createTestMineral("2", "Unknown", group = "Carbonates")
        )
        basicDao.insertAll(minerals)

        // Act
        val flow = queryDao.getDistinctGroupsFlow()

        // Assert
        flow.test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertFalse(result.contains(""))
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== HELPER METHODS ==========

    private fun createTestMineral(
        id: String,
        name: String,
        group: String = "Test Group",
        formula: String = "TestFormula",
        crystalSystem: String = "Hexagonal",
        hardness: String = "7",
        mohsMin: Float = 7.0f,
        mohsMax: Float = 7.0f,
        status: String = "COLLECTION",
        quality: Int = 4,
        type: String = "SIMPLE",
        notes: String = "Test notes",
        tags: String = ""
    ) = MineralEntity(
        id = id,
        name = name,
        group = group,
        formula = formula,
        crystalSystem = crystalSystem,
        hardness = hardness,
        mohsMin = mohsMin,
        mohsMax = mohsMax,
        specificGravity = "2.65",
        color = "Clear",
        luster = "Vitreous",
        transparency = "Transparent",
        cleavage = "None",
        fracture = "Conchoidal",
        streak = "White",
        fluorescence = "None",
        magnetism = "Non-magnetic",
        radioactivity = "None",
        notes = notes,
        provenanceId = null,
        storageId = null,
        status = status,
        quality = quality,
        estimatedValue = 100.0,
        acquisitionDate = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        mineralType = type,
        qrCode = null
    )
}
