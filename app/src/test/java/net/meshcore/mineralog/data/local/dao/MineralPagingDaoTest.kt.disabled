package net.meshcore.mineralog.data.local.dao

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.MineralEntity
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Integration tests for MineralPagingDao using Robolectric.
 *
 * Tests cover:
 * - Basic paged queries (8 sorting options)
 * - Type-based paged queries
 * - Search paged queries (8 sorting options)
 * - Advanced filter paged queries (8 sorting options)
 *
 * Note: PagingSource testing verifies that sources are created and can load data.
 * Full pagination behavior is tested through integration/UI tests.
 *
 * Sprint 3: DAO Tests - Target 70%+ coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MineralPagingDaoTest {

    private lateinit var database: MineraLogDatabase
    private lateinit var pagingDao: MineralPagingDao
    private lateinit var basicDao: MineralBasicDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MineraLogDatabase::class.java
        ).allowMainThreadQueries().build()

        val compositeDao = database.mineralDao()
        pagingDao = object : MineralPagingDao {
            override fun getAllPaged() = compositeDao.getAllPaged()
            override fun getAllPagedSortedByNameAsc() = compositeDao.getAllPagedSortedByNameAsc()
            override fun getAllPagedSortedByNameDesc() = compositeDao.getAllPagedSortedByNameDesc()
            override fun getAllPagedSortedByDateDesc() = compositeDao.getAllPagedSortedByDateDesc()
            override fun getAllPagedSortedByDateAsc() = compositeDao.getAllPagedSortedByDateAsc()
            override fun getAllPagedSortedByGroup() = compositeDao.getAllPagedSortedByGroup()
            override fun getAllPagedSortedByHardnessAsc() = compositeDao.getAllPagedSortedByHardnessAsc()
            override fun getAllPagedSortedByHardnessDesc() = compositeDao.getAllPagedSortedByHardnessDesc()
            override fun getMineralsByTypePaged(types: List<String>) = compositeDao.getMineralsByTypePaged(types)
            override fun searchPaged(query: String) = compositeDao.searchPaged(query)
            override fun searchPagedSortedByNameAsc(query: String) = compositeDao.searchPagedSortedByNameAsc(query)
            override fun searchPagedSortedByNameDesc(query: String) = compositeDao.searchPagedSortedByNameDesc(query)
            override fun searchPagedSortedByDateDesc(query: String) = compositeDao.searchPagedSortedByDateDesc(query)
            override fun searchPagedSortedByDateAsc(query: String) = compositeDao.searchPagedSortedByDateAsc(query)
            override fun searchPagedSortedByGroup(query: String) = compositeDao.searchPagedSortedByGroup(query)
            override fun searchPagedSortedByHardnessAsc(query: String) = compositeDao.searchPagedSortedByHardnessAsc(query)
            override fun searchPagedSortedByHardnessDesc(query: String) = compositeDao.searchPagedSortedByHardnessDesc(query)
            override fun filterAdvancedPaged(groups: List<String>?, countries: List<String>?, crystalSystems: List<String>?,
                mohsMin: Float?, mohsMax: Float?, statusTypes: List<String>?, qualityMin: Int?, qualityMax: Int?,
                hasPhotos: Boolean?, fluorescent: Boolean?, mineralTypes: List<String>?) =
                compositeDao.filterAdvancedPaged(groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes, qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes)
            override fun filterAdvancedPagedSortedByNameAsc(groups: List<String>?, countries: List<String>?, crystalSystems: List<String>?,
                mohsMin: Float?, mohsMax: Float?, statusTypes: List<String>?, qualityMin: Int?, qualityMax: Int?,
                hasPhotos: Boolean?, fluorescent: Boolean?, mineralTypes: List<String>?) =
                compositeDao.filterAdvancedPagedSortedByNameAsc(groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes, qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes)
            override fun filterAdvancedPagedSortedByNameDesc(groups: List<String>?, countries: List<String>?, crystalSystems: List<String>?,
                mohsMin: Float?, mohsMax: Float?, statusTypes: List<String>?, qualityMin: Int?, qualityMax: Int?,
                hasPhotos: Boolean?, fluorescent: Boolean?, mineralTypes: List<String>?) =
                compositeDao.filterAdvancedPagedSortedByNameDesc(groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes, qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes)
            override fun filterAdvancedPagedSortedByDateDesc(groups: List<String>?, countries: List<String>?, crystalSystems: List<String>?,
                mohsMin: Float?, mohsMax: Float?, statusTypes: List<String>?, qualityMin: Int?, qualityMax: Int?,
                hasPhotos: Boolean?, fluorescent: Boolean?, mineralTypes: List<String>?) =
                compositeDao.filterAdvancedPagedSortedByDateDesc(groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes, qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes)
            override fun filterAdvancedPagedSortedByDateAsc(groups: List<String>?, countries: List<String>?, crystalSystems: List<String>?,
                mohsMin: Float?, mohsMax: Float?, statusTypes: List<String>?, qualityMin: Int?, qualityMax: Int?,
                hasPhotos: Boolean?, fluorescent: Boolean?, mineralTypes: List<String>?) =
                compositeDao.filterAdvancedPagedSortedByDateAsc(groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes, qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes)
            override fun filterAdvancedPagedSortedByGroup(groups: List<String>?, countries: List<String>?, crystalSystems: List<String>?,
                mohsMin: Float?, mohsMax: Float?, statusTypes: List<String>?, qualityMin: Int?, qualityMax: Int?,
                hasPhotos: Boolean?, fluorescent: Boolean?, mineralTypes: List<String>?) =
                compositeDao.filterAdvancedPagedSortedByGroup(groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes, qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes)
            override fun filterAdvancedPagedSortedByHardnessAsc(groups: List<String>?, countries: List<String>?, crystalSystems: List<String>?,
                mohsMin: Float?, mohsMax: Float?, statusTypes: List<String>?, qualityMin: Int?, qualityMax: Int?,
                hasPhotos: Boolean?, fluorescent: Boolean?, mineralTypes: List<String>?) =
                compositeDao.filterAdvancedPagedSortedByHardnessAsc(groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes, qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes)
            override fun filterAdvancedPagedSortedByHardnessDesc(groups: List<String>?, countries: List<String>?, crystalSystems: List<String>?,
                mohsMin: Float?, mohsMax: Float?, statusTypes: List<String>?, qualityMin: Int?, qualityMax: Int?,
                hasPhotos: Boolean?, fluorescent: Boolean?, mineralTypes: List<String>?) =
                compositeDao.filterAdvancedPagedSortedByHardnessDesc(groups, countries, crystalSystems, mohsMin, mohsMax, statusTypes, qualityMin, qualityMax, hasPhotos, fluorescent, mineralTypes)
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

    // ========== BASIC PAGED QUERIES TESTS ==========

    @Test
    @DisplayName("getAllPaged - creates valid PagingSource")
    fun `getAllPaged - PagingSource creation - succeeds`() = runTest {
        // Arrange
        val minerals = createTestMinerals(5)
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.getAllPaged()

        // Assert
        assertNotNull(pagingSource)
        assertTrue(pagingSource is PagingSource<Int, MineralEntity>)
    }

    @Test
    @DisplayName("getAllPaged - loads data correctly")
    fun `getAllPaged - data loading - returns minerals`() = runTest {
        // Arrange
        val now = Instant.now()
        val minerals = listOf(
            createTestMineral("1", "Quartz", updatedAt = now.minus(2, ChronoUnit.DAYS)),
            createTestMineral("2", "Calcite", updatedAt = now.minus(1, ChronoUnit.DAYS)),
            createTestMineral("3", "Feldspar", updatedAt = now)
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.getAllPaged()
        val loadParams = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = 10,
            placeholdersEnabled = false
        )
        val result = pagingSource.load(loadParams)

        // Assert
        assertTrue(result is PagingSource.LoadResult.Page)
        val pageResult = result as PagingSource.LoadResult.Page
        assertEquals(3, pageResult.data.size)
        // Should be ordered by updatedAt DESC (most recent first)
        assertEquals("Feldspar", pageResult.data[0].name)
        assertEquals("Calcite", pageResult.data[1].name)
        assertEquals("Quartz", pageResult.data[2].name)
    }

    @Test
    @DisplayName("getAllPagedSortedByNameAsc - sorts by name ascending")
    fun `getAllPagedSortedByNameAsc - sorting - alphabetical order`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz"),
            createTestMineral("2", "Calcite"),
            createTestMineral("3", "Feldspar")
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.getAllPagedSortedByNameAsc()
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertEquals("Calcite", result[0].name)
        assertEquals("Feldspar", result[1].name)
        assertEquals("Quartz", result[2].name)
    }

    @Test
    @DisplayName("getAllPagedSortedByNameDesc - sorts by name descending")
    fun `getAllPagedSortedByNameDesc - sorting - reverse alphabetical`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Calcite"),
            createTestMineral("2", "Feldspar"),
            createTestMineral("3", "Quartz")
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.getAllPagedSortedByNameDesc()
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertEquals("Quartz", result[0].name)
        assertEquals("Feldspar", result[1].name)
        assertEquals("Calcite", result[2].name)
    }

    @Test
    @DisplayName("getAllPagedSortedByGroup - sorts by group then name")
    fun `getAllPagedSortedByGroup - sorting - by group then name`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates"),
            createTestMineral("2", "Hematite", group = "Oxides"),
            createTestMineral("3", "Feldspar", group = "Silicates"),
            createTestMineral("4", "Calcite", group = "Carbonates")
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.getAllPagedSortedByGroup()
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(4, result.size)
        assertEquals("Calcite", result[0].name) // Carbonates
        assertEquals("Hematite", result[1].name) // Oxides
        assertEquals("Feldspar", result[2].name) // Silicates (F before Q)
        assertEquals("Quartz", result[3].name) // Silicates
    }

    @Test
    @DisplayName("getAllPagedSortedByHardnessAsc - sorts by hardness ascending")
    fun `getAllPagedSortedByHardnessAsc - sorting - softest to hardest`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", mohsMin = 7.0f),
            createTestMineral("2", "Calcite", mohsMin = 3.0f),
            createTestMineral("3", "Feldspar", mohsMin = 6.0f)
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.getAllPagedSortedByHardnessAsc()
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertEquals("Calcite", result[0].name) // 3.0
        assertEquals("Feldspar", result[1].name) // 6.0
        assertEquals("Quartz", result[2].name) // 7.0
    }

    @Test
    @DisplayName("getAllPagedSortedByHardnessDesc - sorts by hardness descending")
    fun `getAllPagedSortedByHardnessDesc - sorting - hardest to softest`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Calcite", mohsMax = 3.0f),
            createTestMineral("2", "Feldspar", mohsMax = 6.5f),
            createTestMineral("3", "Quartz", mohsMax = 7.0f)
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.getAllPagedSortedByHardnessDesc()
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertEquals("Quartz", result[0].name) // 7.0
        assertEquals("Feldspar", result[1].name) // 6.5
        assertEquals("Calcite", result[2].name) // 3.0
    }

    // ========== TYPE-BASED PAGED QUERIES TESTS ==========

    @Test
    @DisplayName("getMineralsByTypePaged - filters by types")
    fun `getMineralsByTypePaged - type filter - returns matching types`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", type = "SIMPLE"),
            createTestMineral("2", "Granite", type = "AGGREGATE"),
            createTestMineral("3", "Basalt", type = "ROCK"),
            createTestMineral("4", "Calcite", type = "SIMPLE")
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.getMineralsByTypePaged(listOf("SIMPLE", "AGGREGATE"))
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertTrue(result.any { it.name == "Quartz" })
        assertTrue(result.any { it.name == "Granite" })
        assertTrue(result.any { it.name == "Calcite" })
        assertFalse(result.any { it.name == "Basalt" })
    }

    // ========== SEARCH PAGED QUERIES TESTS ==========

    @Test
    @DisplayName("searchPaged - searches by name")
    fun `searchPaged - name search - returns matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz"),
            createTestMineral("2", "Rose Quartz"),
            createTestMineral("3", "Calcite")
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.searchPaged("%quartz%")
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.any { it.name.contains("Quartz", ignoreCase = true) })
    }

    @Test
    @DisplayName("searchPagedSortedByNameAsc - searches and sorts")
    fun `searchPagedSortedByNameAsc - search and sort - by name ascending`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Rose Quartz"),
            createTestMineral("2", "Quartz"),
            createTestMineral("3", "Amethyst Quartz"),
            createTestMineral("4", "Calcite")
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.searchPagedSortedByNameAsc("%quartz%")
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertEquals("Amethyst Quartz", result[0].name)
        assertEquals("Quartz", result[1].name)
        assertEquals("Rose Quartz", result[2].name)
    }

    @Test
    @DisplayName("searchPagedSortedByGroup - searches and sorts by group")
    fun `searchPagedSortedByGroup - search and sort - by group then name`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz SiO2", group = "Silicates", formula = "SiO2"),
            createTestMineral("2", "Silicon Oxide", group = "Oxides", formula = "SiO2"),
            createTestMineral("3", "Cristobalite", group = "Silicates", formula = "SiO2")
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.searchPagedSortedByGroup("%SiO2%")
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertEquals("Silicon Oxide", result[0].name) // Oxides
        assertEquals("Cristobalite", result[1].name) // Silicates
        assertEquals("Quartz SiO2", result[2].name) // Silicates
    }

    @Test
    @DisplayName("searchPagedSortedByHardnessDesc - searches and sorts by hardness")
    fun `searchPagedSortedByHardnessDesc - search and sort - by hardness descending`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Soft Silicate", group = "Silicates", mohsMax = 3.0f),
            createTestMineral("2", "Medium Silicate", group = "Silicates", mohsMax = 6.0f),
            createTestMineral("3", "Hard Silicate", group = "Silicates", mohsMax = 8.0f),
            createTestMineral("4", "Carbonate", group = "Carbonates", mohsMax = 3.0f)
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.searchPagedSortedByHardnessDesc("%silicate%")
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertEquals("Hard Silicate", result[0].name) // 8.0
        assertEquals("Medium Silicate", result[1].name) // 6.0
        assertEquals("Soft Silicate", result[2].name) // 3.0
    }

    // ========== ADVANCED FILTER PAGED QUERIES TESTS ==========

    @Test
    @DisplayName("filterAdvancedPaged - filters by multiple criteria")
    fun `filterAdvancedPaged - multiple criteria - returns matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates", type = "SIMPLE", quality = 5),
            createTestMineral("2", "Calcite", group = "Carbonates", type = "SIMPLE", quality = 3),
            createTestMineral("3", "Feldspar", group = "Silicates", type = "SIMPLE", quality = 4),
            createTestMineral("4", "Granite", group = "Silicates", type = "AGGREGATE", quality = 5)
        )
        basicDao.insertAll(minerals)

        // Act - Filter for Silicates + Quality 4+
        val pagingSource = pagingDao.filterAdvancedPaged(
            groups = listOf("Silicates"),
            qualityMin = 4
        )
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertTrue(result.all { it.group == "Silicates" && it.quality >= 4 })
    }

    @Test
    @DisplayName("filterAdvancedPagedSortedByNameAsc - filters and sorts")
    fun `filterAdvancedPagedSortedByNameAsc - filter and sort - by name ascending`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates"),
            createTestMineral("2", "Feldspar", group = "Silicates"),
            createTestMineral("3", "Calcite", group = "Carbonates"),
            createTestMineral("4", "Albite", group = "Silicates")
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.filterAdvancedPagedSortedByNameAsc(
            groups = listOf("Silicates")
        )
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertEquals("Albite", result[0].name)
        assertEquals("Feldspar", result[1].name)
        assertEquals("Quartz", result[2].name)
    }

    @Test
    @DisplayName("filterAdvancedPagedSortedByGroup - filters and sorts by group")
    fun `filterAdvancedPagedSortedByGroup - filter and sort - by group`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates", type = "SIMPLE"),
            createTestMineral("2", "Hematite", group = "Oxides", type = "SIMPLE"),
            createTestMineral("3", "Calcite", group = "Carbonates", type = "SIMPLE"),
            createTestMineral("4", "Feldspar", group = "Silicates", type = "SIMPLE")
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.filterAdvancedPagedSortedByGroup(
            mineralTypes = listOf("SIMPLE")
        )
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(4, result.size)
        assertEquals("Calcite", result[0].name) // Carbonates
        assertEquals("Hematite", result[1].name) // Oxides
        assertTrue(result[2].name in listOf("Feldspar", "Quartz")) // Silicates
        assertTrue(result[3].name in listOf("Feldspar", "Quartz")) // Silicates
    }

    @Test
    @DisplayName("filterAdvancedPagedSortedByHardnessAsc - filters and sorts by hardness")
    fun `filterAdvancedPagedSortedByHardnessAsc - filter and sort - by hardness ascending`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz", group = "Silicates", mohsMin = 7.0f),
            createTestMineral("2", "Feldspar", group = "Silicates", mohsMin = 6.0f),
            createTestMineral("3", "Calcite", group = "Carbonates", mohsMin = 3.0f),
            createTestMineral("4", "Apatite", group = "Silicates", mohsMin = 5.0f)
        )
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.filterAdvancedPagedSortedByHardnessAsc(
            groups = listOf("Silicates")
        )
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(3, result.size)
        assertEquals("Apatite", result[0].name) // 5.0
        assertEquals("Feldspar", result[1].name) // 6.0
        assertEquals("Quartz", result[2].name) // 7.0
    }

    @Test
    @DisplayName("filterAdvancedPaged - null parameters return all")
    fun `filterAdvancedPaged - all null - returns all minerals`() = runTest {
        // Arrange
        val minerals = createTestMinerals(5)
        basicDao.insertAll(minerals)

        // Act
        val pagingSource = pagingDao.filterAdvancedPaged()
        val result = loadFirstPage(pagingSource)

        // Assert
        assertEquals(5, result.size)
    }

    // ========== HELPER METHODS ==========

    /**
     * Helper to load the first page from a PagingSource.
     */
    private suspend fun loadFirstPage(
        pagingSource: PagingSource<Int, MineralEntity>,
        pageSize: Int = 50
    ): List<MineralEntity> {
        val loadParams = PagingSource.LoadParams.Refresh<Int>(
            key = null,
            loadSize = pageSize,
            placeholdersEnabled = false
        )
        val result = pagingSource.load(loadParams)
        return if (result is PagingSource.LoadResult.Page) {
            result.data
        } else {
            emptyList()
        }
    }

    private fun createTestMinerals(count: Int): List<MineralEntity> {
        return (1..count).map { i ->
            createTestMineral(
                id = "mineral-$i",
                name = "Mineral $i"
            )
        }
    }

    private fun createTestMineral(
        id: String,
        name: String = "Test Mineral $id",
        group: String = "Silicates",
        formula: String = "TestFormula",
        crystalSystem: String = "Hexagonal",
        mohsMin: Float = 7.0f,
        mohsMax: Float = 7.0f,
        type: String = "SIMPLE",
        quality: Int = 4,
        updatedAt: Instant = Instant.now()
    ) = MineralEntity(
        id = id,
        name = name,
        group = group,
        formula = formula,
        crystalSystem = crystalSystem,
        hardness = "7",
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
        notes = "Test notes",
        provenanceId = null,
        storageId = null,
        status = "COLLECTION",
        quality = quality,
        estimatedValue = 100.0,
        acquisitionDate = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = updatedAt,
        mineralType = type,
        qrCode = null
    )
}
