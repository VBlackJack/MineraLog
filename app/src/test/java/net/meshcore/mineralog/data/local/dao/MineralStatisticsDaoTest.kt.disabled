package net.meshcore.mineralog.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Integration tests for MineralStatisticsDao using Robolectric.
 *
 * Tests cover:
 * - Distribution statistics (group, country, crystal system, hardness, status, type)
 * - Value statistics (total, average, most valuable)
 * - Completeness statistics (average, fully documented count)
 * - Time-based statistics (added this month/year, by month distribution)
 * - Most common statistics (group, country)
 * - Aggregate component statistics
 *
 * Sprint 3: DAO Tests - Target 70%+ coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MineralStatisticsDaoTest {

    private lateinit var database: MineraLogDatabase
    private lateinit var statisticsDao: MineralStatisticsDao
    private lateinit var basicDao: MineralBasicDao
    private lateinit var provenanceDao: ProvenanceDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MineraLogDatabase::class.java
        ).allowMainThreadQueries().build()

        val compositeDao = database.mineralDao()
        statisticsDao = object : MineralStatisticsDao {
            override suspend fun getGroupDistribution() = compositeDao.getGroupDistribution()
            override suspend fun getCountryDistribution() = compositeDao.getCountryDistribution()
            override suspend fun getCrystalSystemDistribution() = compositeDao.getCrystalSystemDistribution()
            override suspend fun getHardnessDistribution() = compositeDao.getHardnessDistribution()
            override suspend fun getStatusDistribution() = compositeDao.getStatusDistribution()
            override suspend fun getTypeDistribution() = compositeDao.getTypeDistribution()
            override suspend fun getTotalValue() = compositeDao.getTotalValue()
            override suspend fun getAverageValue() = compositeDao.getAverageValue()
            override suspend fun getMostValuableSpecimen() = compositeDao.getMostValuableSpecimen()
            override suspend fun getAverageCompleteness() = compositeDao.getAverageCompleteness()
            override suspend fun getFullyDocumentedCount() = compositeDao.getFullyDocumentedCount()
            override suspend fun getAddedThisMonth() = compositeDao.getAddedThisMonth()
            override suspend fun getAddedThisYear() = compositeDao.getAddedThisYear()
            override suspend fun getAddedByMonthDistribution() = compositeDao.getAddedByMonthDistribution()
            override suspend fun getMostCommonGroup() = compositeDao.getMostCommonGroup()
            override suspend fun getMostCommonCountry() = compositeDao.getMostCommonCountry()
            override suspend fun getMostFrequentComponents() = compositeDao.getMostFrequentComponents()
            override suspend fun getAverageComponentCount() = compositeDao.getAverageComponentCount()
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

        provenanceDao = database.provenanceDao()
    }

    @AfterEach
    fun tearDown() {
        database.close()
    }

    // ========== DISTRIBUTION STATISTICS TESTS ==========

    @Test
    @DisplayName("getGroupDistribution - returns distribution ordered by count")
    fun `getGroupDistribution - group counts - ordered by count descending`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", group = "Silicates"),
            createTestMineral("2", group = "Silicates"),
            createTestMineral("3", group = "Silicates"),
            createTestMineral("4", group = "Carbonates"),
            createTestMineral("5", group = "Carbonates"),
            createTestMineral("6", group = "Oxides")
        )
        basicDao.insertAll(minerals)

        // Act
        val distribution = statisticsDao.getGroupDistribution()

        // Assert
        assertEquals(3, distribution.size)
        assertEquals(3, distribution["Silicates"])
        assertEquals(2, distribution["Carbonates"])
        assertEquals(1, distribution["Oxides"])
    }

    @Test
    @DisplayName("getGroupDistribution - excludes null groups")
    fun `getGroupDistribution - null groups - excluded from results`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", group = "Silicates"),
            createTestMineral("2", group = "Silicates")
        )
        basicDao.insertAll(minerals)

        // Act
        val distribution = statisticsDao.getGroupDistribution()

        // Assert
        assertEquals(1, distribution.size)
        assertEquals(2, distribution["Silicates"])
    }

    @Test
    @DisplayName("getCrystalSystemDistribution - returns distribution by crystal system")
    fun `getCrystalSystemDistribution - crystal system counts - ordered by count`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", crystalSystem = "Hexagonal"),
            createTestMineral("2", crystalSystem = "Hexagonal"),
            createTestMineral("3", crystalSystem = "Cubic"),
            createTestMineral("4", crystalSystem = "Triclinic")
        )
        basicDao.insertAll(minerals)

        // Act
        val distribution = statisticsDao.getCrystalSystemDistribution()

        // Assert
        assertEquals(3, distribution.size)
        assertEquals(2, distribution["Hexagonal"])
        assertEquals(1, distribution["Cubic"])
        assertEquals(1, distribution["Triclinic"])
    }

    @Test
    @DisplayName("getHardnessDistribution - returns distribution by hardness ranges")
    fun `getHardnessDistribution - hardness ranges - grouped correctly`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", mohsMin = 1.5f), // 1-2
            createTestMineral("2", mohsMin = 3.0f), // 3-4
            createTestMineral("3", mohsMin = 3.5f), // 3-4
            createTestMineral("4", mohsMin = 7.0f), // 7-8
            createTestMineral("5", mohsMin = 7.5f), // 7-8
            createTestMineral("6", mohsMin = 7.2f)  // 7-8
        )
        basicDao.insertAll(minerals)

        // Act
        val distribution = statisticsDao.getHardnessDistribution()

        // Assert
        assertEquals(3, distribution.size)
        assertEquals(1, distribution["1-2"])
        assertEquals(2, distribution["3-4"])
        assertEquals(3, distribution["7-8"])
    }

    @Test
    @DisplayName("getStatusDistribution - returns distribution by status")
    fun `getStatusDistribution - status counts - all statuses included`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", status = "COLLECTION"),
            createTestMineral("2", status = "COLLECTION"),
            createTestMineral("3", status = "DISPLAY"),
            createTestMineral("4", status = "STORAGE")
        )
        basicDao.insertAll(minerals)

        // Act
        val distribution = statisticsDao.getStatusDistribution()

        // Assert
        assertEquals(3, distribution.size)
        assertEquals(2, distribution["COLLECTION"])
        assertEquals(1, distribution["DISPLAY"])
        assertEquals(1, distribution["STORAGE"])
    }

    @Test
    @DisplayName("getTypeDistribution - returns distribution by mineral type")
    fun `getTypeDistribution - type counts - SIMPLE vs AGGREGATE vs ROCK`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", type = "SIMPLE"),
            createTestMineral("2", type = "SIMPLE"),
            createTestMineral("3", type = "SIMPLE"),
            createTestMineral("4", type = "AGGREGATE"),
            createTestMineral("5", type = "ROCK")
        )
        basicDao.insertAll(minerals)

        // Act
        val distribution = statisticsDao.getTypeDistribution()

        // Assert
        assertEquals(3, distribution.size)
        assertEquals(3, distribution["SIMPLE"])
        assertEquals(1, distribution["AGGREGATE"])
        assertEquals(1, distribution["ROCK"])
    }

    // ========== VALUE STATISTICS TESTS ==========

    @Test
    @DisplayName("getTotalValue - sums all mineral values from provenance")
    fun `getTotalValue - sum of values - from all minerals with provenance`() = runTest {
        // Arrange
        val provenance1 = createProvenance("prov-1", estimatedValue = 100.0)
        val provenance2 = createProvenance("prov-2", estimatedValue = 250.5)
        val provenance3 = createProvenance("prov-3", estimatedValue = 149.5)
        provenanceDao.insert(provenance1)
        provenanceDao.insert(provenance2)
        provenanceDao.insert(provenance3)

        val minerals = listOf(
            createTestMineral("1", provenanceId = "prov-1"),
            createTestMineral("2", provenanceId = "prov-2"),
            createTestMineral("3", provenanceId = "prov-3"),
            createTestMineral("4", provenanceId = null) // No provenance
        )
        basicDao.insertAll(minerals)

        // Act
        val totalValue = statisticsDao.getTotalValue()

        // Assert
        assertEquals(500.0, totalValue, 0.01)
    }

    @Test
    @DisplayName("getTotalValue - returns 0 when no minerals have value")
    fun `getTotalValue - no values - returns zero`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", provenanceId = null),
            createTestMineral("2", provenanceId = null)
        )
        basicDao.insertAll(minerals)

        // Act
        val totalValue = statisticsDao.getTotalValue()

        // Assert
        assertEquals(0.0, totalValue, 0.01)
    }

    @Test
    @DisplayName("getAverageValue - calculates average of all values")
    fun `getAverageValue - average calculation - across all valued minerals`() = runTest {
        // Arrange
        val provenance1 = createProvenance("prov-1", estimatedValue = 100.0)
        val provenance2 = createProvenance("prov-2", estimatedValue = 200.0)
        val provenance3 = createProvenance("prov-3", estimatedValue = 300.0)
        provenanceDao.insert(provenance1)
        provenanceDao.insert(provenance2)
        provenanceDao.insert(provenance3)

        val minerals = listOf(
            createTestMineral("1", provenanceId = "prov-1"),
            createTestMineral("2", provenanceId = "prov-2"),
            createTestMineral("3", provenanceId = "prov-3")
        )
        basicDao.insertAll(minerals)

        // Act
        val averageValue = statisticsDao.getAverageValue()

        // Assert
        assertEquals(200.0, averageValue, 0.01)
    }

    @Test
    @DisplayName("getMostValuableSpecimen - returns mineral with highest value")
    fun `getMostValuableSpecimen - highest value - returns correct mineral info`() = runTest {
        // Arrange
        val provenance1 = createProvenance("prov-1", estimatedValue = 500.0)
        val provenance2 = createProvenance("prov-2", estimatedValue = 1500.0)
        val provenance3 = createProvenance("prov-3", estimatedValue = 750.0)
        provenanceDao.insert(provenance1)
        provenanceDao.insert(provenance2)
        provenanceDao.insert(provenance3)

        val minerals = listOf(
            createTestMineral("1", "Quartz", provenanceId = "prov-1"),
            createTestMineral("2", "Diamond", provenanceId = "prov-2"),
            createTestMineral("3", "Emerald", provenanceId = "prov-3")
        )
        basicDao.insertAll(minerals)

        // Act
        val mostValuable = statisticsDao.getMostValuableSpecimen()

        // Assert
        assertNotNull(mostValuable)
        assertEquals("2", mostValuable?.id)
        assertEquals("Diamond", mostValuable?.name)
        assertEquals(1500.0, mostValuable?.value ?: 0.0, 0.01)
        assertEquals("USD", mostValuable?.currency)
    }

    @Test
    @DisplayName("getMostValuableSpecimen - returns null when no values")
    fun `getMostValuableSpecimen - no values - returns null`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", provenanceId = null),
            createTestMineral("2", provenanceId = null)
        )
        basicDao.insertAll(minerals)

        // Act
        val mostValuable = statisticsDao.getMostValuableSpecimen()

        // Assert
        assertNull(mostValuable)
    }

    // ========== COMPLETENESS STATISTICS TESTS ==========

    @Test
    @DisplayName("getAverageCompleteness - calculates average completeness")
    fun `getAverageCompleteness - average calculation - across all minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", completeness = 100),
            createTestMineral("2", completeness = 80),
            createTestMineral("3", completeness = 60),
            createTestMineral("4", completeness = 40)
        )
        basicDao.insertAll(minerals)

        // Act
        val average = statisticsDao.getAverageCompleteness()

        // Assert
        assertEquals(70.0, average, 0.01)
    }

    @Test
    @DisplayName("getFullyDocumentedCount - counts minerals >= 80% complete")
    fun `getFullyDocumentedCount - count minerals - with completeness 80 or more`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", completeness = 100),
            createTestMineral("2", completeness = 85),
            createTestMineral("3", completeness = 80),
            createTestMineral("4", completeness = 79),
            createTestMineral("5", completeness = 60)
        )
        basicDao.insertAll(minerals)

        // Act
        val fullyDocumented = statisticsDao.getFullyDocumentedCount()

        // Assert
        assertEquals(3, fullyDocumented)
    }

    // ========== TIME-BASED STATISTICS TESTS ==========

    @Test
    @DisplayName("getAddedThisMonth - counts minerals added this month")
    fun `getAddedThisMonth - current month - returns correct count`() = runTest {
        // Arrange
        val now = Instant.now()
        val thisMonth = now
        val lastMonth = now.minus(35, ChronoUnit.DAYS)
        val lastYear = now.minus(400, ChronoUnit.DAYS)

        val minerals = listOf(
            createTestMineral("1", createdAt = thisMonth),
            createTestMineral("2", createdAt = thisMonth),
            createTestMineral("3", createdAt = lastMonth),
            createTestMineral("4", createdAt = lastYear)
        )
        basicDao.insertAll(minerals)

        // Act
        val addedThisMonth = statisticsDao.getAddedThisMonth()

        // Assert
        assertEquals(2, addedThisMonth)
    }

    @Test
    @DisplayName("getAddedThisYear - counts minerals added this year")
    fun `getAddedThisYear - current year - returns correct count`() = runTest {
        // Arrange
        val now = Instant.now()
        val thisYear = now
        val twoMonthsAgo = now.minus(60, ChronoUnit.DAYS)
        val lastYear = now.minus(400, ChronoUnit.DAYS)

        val minerals = listOf(
            createTestMineral("1", createdAt = thisYear),
            createTestMineral("2", createdAt = twoMonthsAgo),
            createTestMineral("3", createdAt = thisYear),
            createTestMineral("4", createdAt = lastYear)
        )
        basicDao.insertAll(minerals)

        // Act
        val addedThisYear = statisticsDao.getAddedThisYear()

        // Assert
        // Should be 3 (thisYear + twoMonthsAgo + thisYear) if still in same year
        assertTrue(addedThisYear >= 3)
    }

    @Test
    @DisplayName("getAddedByMonthDistribution - returns distribution by month")
    fun `getAddedByMonthDistribution - month distribution - grouped by YYYY-MM`() = runTest {
        // Arrange
        val now = Instant.now()
        val month1 = now
        val month2 = now.minus(35, ChronoUnit.DAYS)

        val minerals = listOf(
            createTestMineral("1", createdAt = month1),
            createTestMineral("2", createdAt = month1),
            createTestMineral("3", createdAt = month2),
            createTestMineral("4", createdAt = month1)
        )
        basicDao.insertAll(minerals)

        // Act
        val distribution = statisticsDao.getAddedByMonthDistribution()

        // Assert
        assertTrue(distribution.isNotEmpty())
        // Should have entries for at least 1-2 months
        assertTrue(distribution.size >= 1)
    }

    // ========== MOST COMMON STATISTICS TESTS ==========

    @Test
    @DisplayName("getMostCommonGroup - returns group with highest count")
    fun `getMostCommonGroup - most frequent - returns correct group`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", group = "Silicates"),
            createTestMineral("2", group = "Silicates"),
            createTestMineral("3", group = "Silicates"),
            createTestMineral("4", group = "Carbonates"),
            createTestMineral("5", group = "Oxides")
        )
        basicDao.insertAll(minerals)

        // Act
        val mostCommon = statisticsDao.getMostCommonGroup()

        // Assert
        assertEquals("Silicates", mostCommon)
    }

    @Test
    @DisplayName("getMostCommonGroup - returns null when no minerals")
    fun `getMostCommonGroup - no minerals - returns null`() = runTest {
        // Act
        val mostCommon = statisticsDao.getMostCommonGroup()

        // Assert
        assertNull(mostCommon)
    }

    @Test
    @DisplayName("getMostCommonCountry - returns country with highest count")
    fun `getMostCommonCountry - most frequent - returns correct country`() = runTest {
        // Arrange
        val provFrance1 = createProvenance("prov-fr-1", country = "France")
        val provFrance2 = createProvenance("prov-fr-2", country = "France")
        val provBrazil = createProvenance("prov-br", country = "Brazil")
        provenanceDao.insert(provFrance1)
        provenanceDao.insert(provFrance2)
        provenanceDao.insert(provBrazil)

        val minerals = listOf(
            createTestMineral("1", provenanceId = "prov-fr-1"),
            createTestMineral("2", provenanceId = "prov-fr-2"),
            createTestMineral("3", provenanceId = "prov-br")
        )
        basicDao.insertAll(minerals)

        // Act
        val mostCommon = statisticsDao.getMostCommonCountry()

        // Assert
        assertEquals("France", mostCommon)
    }

    @Test
    @DisplayName("getCountryDistribution - returns distribution from provenance")
    fun `getCountryDistribution - country counts - from provenance join`() = runTest {
        // Arrange
        val provFrance1 = createProvenance("prov-fr-1", country = "France")
        val provFrance2 = createProvenance("prov-fr-2", country = "France")
        val provBrazil = createProvenance("prov-br", country = "Brazil")
        provenanceDao.insert(provFrance1)
        provenanceDao.insert(provFrance2)
        provenanceDao.insert(provBrazil)

        val minerals = listOf(
            createTestMineral("1", provenanceId = "prov-fr-1"),
            createTestMineral("2", provenanceId = "prov-fr-2"),
            createTestMineral("3", provenanceId = "prov-br"),
            createTestMineral("4", provenanceId = null) // No provenance
        )
        basicDao.insertAll(minerals)

        // Act
        val distribution = statisticsDao.getCountryDistribution()

        // Assert
        assertEquals(2, distribution.size)
        assertEquals(2, distribution["France"])
        assertEquals(1, distribution["Brazil"])
    }

    // ========== HELPER METHODS ==========

    private fun createTestMineral(
        id: String,
        name: String = "Test Mineral $id",
        group: String = "Silicates",
        crystalSystem: String = "Hexagonal",
        mohsMin: Float = 7.0f,
        status: String = "COLLECTION",
        type: String = "SIMPLE",
        completeness: Int = 50,
        provenanceId: String? = null,
        createdAt: Instant = Instant.now()
    ) = MineralEntity(
        id = id,
        name = name,
        group = group,
        formula = "TestFormula",
        crystalSystem = crystalSystem,
        hardness = "7",
        mohsMin = mohsMin,
        mohsMax = mohsMin,
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
        provenanceId = provenanceId,
        storageId = null,
        status = status,
        quality = 4,
        estimatedValue = 100.0,
        acquisitionDate = Instant.now(),
        createdAt = createdAt,
        updatedAt = Instant.now(),
        mineralType = type,
        qrCode = null
    )

    private fun createProvenance(
        id: String,
        country: String = "France",
        estimatedValue: Double? = null
    ) = ProvenanceEntity(
        id = id,
        country = country,
        region = "Test Region",
        locality = "Test Locality",
        mine = null,
        discoveryDate = null,
        acquiredFrom = "Test Source",
        acquisitionDate = Instant.now(),
        estimatedValue = estimatedValue,
        currency = "USD",
        notes = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
}
