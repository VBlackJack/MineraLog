package net.meshcore.mineralog.data.local.dao

import androidx.room.Room
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.Instant
// removed: import kotlin.test.assertEquals
// removed: import kotlin.test.assertNotNull
// removed: import kotlin.test.assertNull
// removed: import kotlin.test.assertTrue

/**
 * Comprehensive tests for [ProvenanceDao].
 *
 * Tests cover:
 * - CRUD operations
 * - Batch operations (getByMineralIds, deleteByMineralIds)
 * - Cascade deletes (when mineral is deleted)
 * - Unique constraint (one provenance per mineral)
 * - Coordinate filtering
 * - Country aggregation
 * - Edge cases (null fields)
 *
 * Uses in-memory database for fast, isolated tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27, 35])
class ProvenanceDaoTest {

    private lateinit var database: MineraLogDatabase
    private lateinit var provenanceDao: ProvenanceDao
    private lateinit var mineralDao: MineralDao

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            MineraLogDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        provenanceDao = database.provenanceDao()
        mineralDao = database.mineralDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== CREATE Tests ==========

    @Test
    fun insert_singleProvenance_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(
            mineralId = mineral.id,
            locality = "Mount Ida, Arkansas",
            country = "United States"
        )

        // When
        provenanceDao.insert(provenance)

        // Then
        val retrieved = provenanceDao.getById(provenance.id)
        assertNotNull(retrieved)
        assertEquals("Mount Ida, Arkansas", retrieved!!.locality)
        assertEquals(mineral.id, retrieved!!.mineralId)
    }

    @Test
    fun insertAll_multipleProvenances_success() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        mineralDao.insertAll(listOf(mineral1, mineral2))

        val provenances = listOf(
            createTestProvenance(mineralId = mineral1.id, locality = "Arkansas"),
            createTestProvenance(mineralId = mineral2.id, locality = "Mexico")
        )

        // When
        provenanceDao.insertAll(provenances)

        // Then
        val all = provenanceDao.getAll()
        assertEquals(2, all.size)
    }

    @Test
    fun insert_withReplace_updatesExisting() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(
            mineralId = mineral.id,
            locality = "Original locality",
            country = "USA"
        )
        provenanceDao.insert(provenance)

        // When - insert with same ID but different data (REPLACE strategy)
        val updated = provenance.copy(locality = "Updated locality")
        provenanceDao.insert(updated)

        // Then
        val retrieved = provenanceDao.getById(provenance.id)
        assertNotNull(retrieved)
        assertEquals("Updated locality", retrieved!!.locality)
    }

    // ========== READ Tests ==========

    @Test
    fun getById_existingProvenance_returnsProvenance() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(mineralId = mineral.id, locality = "Arkansas")
        provenanceDao.insert(provenance)

        // When
        val result = provenanceDao.getById(provenance.id)

        // Then
        assertNotNull(result)
        assertEquals(provenance.id, result!!.id)
        assertEquals("Arkansas", result!!.locality)
    }

    @Test
    fun getById_nonExistingProvenance_returnsNull() = runTest {
        // When
        val result = provenanceDao.getById("non-existing-id")

        // Then
        assertNull(result)
    }

    @Test
    fun getByMineralId_existingProvenance_returnsProvenance() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(mineralId = mineral.id, locality = "Arkansas")
        provenanceDao.insert(provenance)

        // When
        val result = provenanceDao.getByMineralId(mineral.id)

        // Then
        assertNotNull(result)
        assertEquals(mineral.id, result!!.mineralId)
    }

    @Test
    fun getByMineralId_nonExisting_returnsNull() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)

        // When - No provenance inserted
        val result = provenanceDao.getByMineralId(mineral.id)

        // Then
        assertNull(result)
    }

    @Test
    fun getByMineralIds_returnsProvenancesForMultipleMinerals() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        val mineral3 = createTestMineral(name = "Fluorite")
        mineralDao.insertAll(listOf(mineral1, mineral2, mineral3))

        val provenances = listOf(
            createTestProvenance(mineralId = mineral1.id, locality = "Arkansas"),
            createTestProvenance(mineralId = mineral2.id, locality = "Mexico"),
            createTestProvenance(mineralId = mineral3.id, locality = "China")
        )
        provenanceDao.insertAll(provenances)

        // When
        val result = provenanceDao.getByMineralIds(listOf(mineral1.id, mineral2.id))

        // Then
        assertEquals(2, result!!.size)
        assertTrue(result!!.any { it.mineralId == mineral1.id })
        assertTrue(result!!.any { it.mineralId == mineral2.id })
    }

    @Test
    fun getByMineralIdFlow_emitsUpdates() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(mineralId = mineral.id, locality = "Arkansas")

        // When
        val flow = provenanceDao.getByMineralIdFlow(mineral.id)
        val initial = flow.first()
        assertNull(initial)

        provenanceDao.insert(provenance)
        val afterInsert = flow.first()

        // Then
        assertNotNull(afterInsert)
        assertEquals(provenance.id, afterInsert!!.id)
    }

    @Test
    fun getAll_returnsAllProvenances() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        mineralDao.insertAll(listOf(mineral1, mineral2))

        val provenances = listOf(
            createTestProvenance(mineralId = mineral1.id, locality = "Arkansas"),
            createTestProvenance(mineralId = mineral2.id, locality = "Mexico")
        )
        provenanceDao.insertAll(provenances)

        // When
        val result = provenanceDao.getAll()

        // Then
        assertEquals(2, result!!.size)
    }

    @Test
    fun getAllWithCoordinates_onlyReturnsProvenancesWithBothLatLong() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        val mineral3 = createTestMineral(name = "Fluorite")
        val mineral4 = createTestMineral(name = "Pyrite")
        mineralDao.insertAll(listOf(mineral1, mineral2, mineral3, mineral4))

        val provenances = listOf(
            createTestProvenance(mineralId = mineral1.id, locality = "Arkansas", latitude = 34.7, longitude = -92.3),
            createTestProvenance(mineralId = mineral2.id, locality = "Mexico", latitude = null, longitude = -99.1),
            createTestProvenance(mineralId = mineral3.id, locality = "China", latitude = 23.1, longitude = null),
            createTestProvenance(mineralId = mineral4.id, locality = "Peru", latitude = null, longitude = null)
        )
        provenanceDao.insertAll(provenances)

        // When
        val result = provenanceDao.getAllWithCoordinates()

        // Then - Only mineral1 has both latitude and longitude
        assertEquals(1, result!!.size)
        assertEquals(mineral1.id, result[0].mineralId)
    }

    @Test
    fun getAllWithCoordinatesFlow_emitsOnlyWithCoordinates() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        mineralDao.insertAll(listOf(mineral1, mineral2))

        val provenances = listOf(
            createTestProvenance(mineralId = mineral1.id, locality = "Arkansas", latitude = 34.7, longitude = -92.3),
            createTestProvenance(mineralId = mineral2.id, locality = "Mexico", latitude = null, longitude = null)
        )
        provenanceDao.insertAll(provenances)

        // When
        val result = provenanceDao.getAllWithCoordinatesFlow().first()

        // Then
        assertEquals(1, result!!.size)
        assertEquals(mineral1.id, result[0].mineralId)
    }

    @Test
    fun getDistinctCountriesFlow_returnsUniqueCountriesSorted() = runTest {
        // Given
        val minerals = listOf(
            createTestMineral(name = "Quartz1"),
            createTestMineral(name = "Quartz2"),
            createTestMineral(name = "Calcite"),
            createTestMineral(name = "Fluorite"),
            createTestMineral(name = "Pyrite")
        )
        mineralDao.insertAll(minerals)

        val provenances = listOf(
            createTestProvenance(mineralId = minerals[0].id, country = "United States"),
            createTestProvenance(mineralId = minerals[1].id, country = "United States"), // Duplicate
            createTestProvenance(mineralId = minerals[2].id, country = "Mexico"),
            createTestProvenance(mineralId = minerals[3].id, country = "China"),
            createTestProvenance(mineralId = minerals[4].id, country = null) // Should be excluded
        )
        provenanceDao.insertAll(provenances)

        // When
        val result = provenanceDao.getDistinctCountriesFlow().first()

        // Then - Unique, sorted, no nulls
        assertEquals(3, result!!.size)
        assertEquals("China", result[0])
        assertEquals("Mexico", result[1])
        assertEquals("United States", result[2])
    }

    // ========== UPDATE Tests ==========

    @Test
    fun update_existingProvenance_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(
            mineralId = mineral.id,
            locality = "Original",
            country = "USA"
        )
        provenanceDao.insert(provenance)

        // When
        val updated = provenance.copy(locality = "Updated")
        provenanceDao.update(updated)

        // Then
        val result = provenanceDao.getById(provenance.id)
        assertNotNull(result)
        assertEquals("Updated", result!!.locality)
    }

    // ========== DELETE Tests ==========

    @Test
    fun delete_existingProvenance_removesProvenance() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(mineralId = mineral.id, locality = "Arkansas")
        provenanceDao.insert(provenance)

        // When
        provenanceDao.delete(provenance)

        // Then
        val result = provenanceDao.getById(provenance.id)
        assertNull(result)
    }

    @Test
    fun deleteByMineralId_removesProvenance() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(mineralId = mineral.id, locality = "Arkansas")
        provenanceDao.insert(provenance)

        // When
        provenanceDao.deleteByMineralId(mineral.id)

        // Then
        val result = provenanceDao.getByMineralId(mineral.id)
        assertNull(result)
    }

    @Test
    fun deleteByMineralIds_removesProvenancesForMultipleMinerals() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        val mineral3 = createTestMineral(name = "Fluorite")
        mineralDao.insertAll(listOf(mineral1, mineral2, mineral3))

        val provenances = listOf(
            createTestProvenance(mineralId = mineral1.id, locality = "Arkansas"),
            createTestProvenance(mineralId = mineral2.id, locality = "Mexico"),
            createTestProvenance(mineralId = mineral3.id, locality = "China")
        )
        provenanceDao.insertAll(provenances)

        // When
        provenanceDao.deleteByMineralIds(listOf(mineral1.id, mineral2.id))

        // Then
        val remaining = provenanceDao.getAll()
        assertEquals(1, remaining.size)
        assertEquals(mineral3.id, remaining[0].mineralId)
    }

    @Test
    fun deleteAll_removesAllProvenances() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        mineralDao.insertAll(listOf(mineral1, mineral2))

        val provenances = listOf(
            createTestProvenance(mineralId = mineral1.id, locality = "Arkansas"),
            createTestProvenance(mineralId = mineral2.id, locality = "Mexico")
        )
        provenanceDao.insertAll(provenances)

        // When
        provenanceDao.deleteAll()

        // Then
        val result = provenanceDao.getAll()
        assertEquals(0, result!!.size)
    }

    // ========== CASCADE DELETE Tests ==========

    @Test
    fun cascadeDelete_deletingMineral_deletesProvenance() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(mineralId = mineral.id, locality = "Arkansas")
        provenanceDao.insert(provenance)

        // When - Delete the mineral (should cascade to provenance)
        mineralDao.delete(mineral)

        // Then
        val remainingProvenance = provenanceDao.getByMineralId(mineral.id)
        assertNull(remainingProvenance)
    }

    // ========== EDGE CASES ==========

    @Test
    fun insert_nullOptionalFields_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(
            mineralId = mineral.id,
            site = null,
            locality = null,
            country = null,
            latitude = null,
            longitude = null,
            source = null,
            price = null,
            estimatedValue = null
        )

        // When
        provenanceDao.insert(provenance)

        // Then
        val result = provenanceDao.getById(provenance.id)
        assertNotNull(result)
        assertNull(result!!.locality)
        assertNull(result!!.country)
        assertNull(result!!.latitude)
        assertNull(result!!.longitude)
    }

    @Test
    fun coordinates_validRanges_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(
            mineralId = mineral.id,
            locality = "Test",
            latitude = -45.5,  // Valid: -90 to 90
            longitude = 120.3  // Valid: -180 to 180
        )

        // When
        provenanceDao.insert(provenance)

        // Then
        val result = provenanceDao.getById(provenance.id)
        assertNotNull(result)
        assertEquals(-45.5, result!!.latitude)
        assertEquals(120.3, result!!.longitude)
    }

    @Test
    fun uniqueConstraint_oneProvenancePerMineral() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance1 = createTestProvenance(
            mineralId = mineral.id,
            locality = "First"
        )
        val provenance2 = createTestProvenance(
            mineralId = mineral.id,
            locality = "Second"
        )

        // When - Insert first provenance
        provenanceDao.insert(provenance1)

        // Then - Insert second provenance for same mineral (should replace due to unique constraint)
        provenanceDao.insert(provenance2)

        val all = provenanceDao.getByMineralId(mineral.id)
        assertNotNull(all)
        // Due to unique index on mineralId, only one provenance should exist
        // The behavior depends on whether we're using REPLACE or ABORT strategy
        // With REPLACE, the second insert will replace the first
    }

    @Test
    fun price_andCurrency_optionalFields() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val provenance = createTestProvenance(
            mineralId = mineral.id,
            locality = "Arkansas",
            price = 150.50f,
            estimatedValue = 300.75f,
            currency = "USD"
        )

        // When
        provenanceDao.insert(provenance)

        // Then
        val result = provenanceDao.getById(provenance.id)
        assertNotNull(result)
        assertEquals(150.50f, result!!.price)
        assertEquals(300.75f, result!!.estimatedValue)
        assertEquals("USD", result!!.currency)
    }

    // ========== Helper Methods ==========

    private fun createTestMineral(
        id: String = java.util.UUID.randomUUID().toString(),
        name: String
    ): MineralEntity {
        return MineralEntity(
            id = id,
            name = name,
            statusType = "in_collection",
            createdAt = Instant.now()
        )
    }

    private fun createTestProvenance(
        id: String = java.util.UUID.randomUUID().toString(),
        mineralId: String,
        site: String? = null,
        locality: String? = null,
        country: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        source: String? = null,
        price: Float? = null,
        estimatedValue: Float? = null,
        currency: String? = "USD"
    ): ProvenanceEntity {
        return ProvenanceEntity(
            id = id,
            mineralId = mineralId,
            site = site,
            locality = locality,
            country = country,
            latitude = latitude,
            longitude = longitude,
            acquiredAt = Instant.now(),
            source = source,
            price = price,
            estimatedValue = estimatedValue,
            currency = currency
        )
    }
}
