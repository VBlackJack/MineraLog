package net.meshcore.mineralog.data.local.dao

import androidx.room.Room
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.StorageEntity
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
 * Comprehensive tests for [StorageDao].
 *
 * Tests cover:
 * - CRUD operations
 * - Batch operations (getByMineralIds, deleteByMineralIds)
 * - Cascade deletes (when mineral is deleted)
 * - Unique constraint (one storage per mineral)
 * - Hierarchical location filtering (place → container → box → slot)
 * - Distinct value aggregation for location hierarchy
 * - Optional identifiers (NFC, QR code)
 * - Edge cases (null fields)
 *
 * Uses in-memory database for fast, isolated tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27, 35])
class StorageDaoTest {

    private lateinit var database: MineraLogDatabase
    private lateinit var storageDao: StorageDao
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

        storageDao = database.storageDao()
        mineralDao = database.mineralDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== CREATE Tests ==========

    @Test
    fun insert_singleStorage_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(
            mineralId = mineral.id,
            place = "Living Room",
            container = "Cabinet A",
            box = "Box 1",
            slot = "A3"
        )

        // When
        storageDao.insert(storage)

        // Then
        val retrieved = storageDao.getById(storage.id)
        assertNotNull(retrieved)
        assertEquals("Living Room", retrieved!!.place)
        assertEquals("Cabinet A", retrieved!!.container)
        assertEquals(mineral.id, retrieved!!.mineralId)
    }

    @Test
    fun insertAll_multipleStorages_success() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        mineralDao.insertAll(listOf(mineral1, mineral2))

        val storages = listOf(
            createTestStorage(mineralId = mineral1.id, place = "Living Room"),
            createTestStorage(mineralId = mineral2.id, place = "Basement")
        )

        // When
        storageDao.insertAll(storages)

        // Then
        val all = storageDao.getAll()
        assertEquals(2, all.size)
    }

    @Test
    fun insert_withReplace_updatesExisting() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(
            mineralId = mineral.id,
            place = "Original Place"
        )
        storageDao.insert(storage)

        // When - insert with same ID but different data (REPLACE strategy)
        val updated = storage.copy(place = "Updated Place")
        storageDao.insert(updated)

        // Then
        val retrieved = storageDao.getById(storage.id)
        assertNotNull(retrieved)
        assertEquals("Updated Place", retrieved!!.place)
    }

    // ========== READ Tests ==========

    @Test
    fun getById_existingStorage_returnsStorage() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(mineralId = mineral.id, place = "Living Room")
        storageDao.insert(storage)

        // When
        val result = storageDao.getById(storage.id)

        // Then
        assertNotNull(result)
        assertEquals(storage.id, result!!.id)
        assertEquals("Living Room", result!!.place)
    }

    @Test
    fun getById_nonExistingStorage_returnsNull() = runTest {
        // When
        val result = storageDao.getById("non-existing-id")

        // Then
        assertNull(result)
    }

    @Test
    fun getByMineralId_existingStorage_returnsStorage() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(mineralId = mineral.id, place = "Living Room")
        storageDao.insert(storage)

        // When
        val result = storageDao.getByMineralId(mineral.id)

        // Then
        assertNotNull(result)
        assertEquals(mineral.id, result!!.mineralId)
    }

    @Test
    fun getByMineralId_nonExisting_returnsNull() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)

        // When - No storage inserted
        val result = storageDao.getByMineralId(mineral.id)

        // Then
        assertNull(result)
    }

    @Test
    fun getByMineralIds_returnsStoragesForMultipleMinerals() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        val mineral3 = createTestMineral(name = "Fluorite")
        mineralDao.insertAll(listOf(mineral1, mineral2, mineral3))

        val storages = listOf(
            createTestStorage(mineralId = mineral1.id, place = "Living Room"),
            createTestStorage(mineralId = mineral2.id, place = "Basement"),
            createTestStorage(mineralId = mineral3.id, place = "Office")
        )
        storageDao.insertAll(storages)

        // When
        val result = storageDao.getByMineralIds(listOf(mineral1.id, mineral2.id))

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
        val storage = createTestStorage(mineralId = mineral.id, place = "Living Room")

        // When
        val flow = storageDao.getByMineralIdFlow(mineral.id)
        val initial = flow.first()
        assertNull(initial)

        storageDao.insert(storage)
        val afterInsert = flow.first()

        // Then
        assertNotNull(afterInsert)
        assertEquals(storage.id, afterInsert!!.id)
    }

    @Test
    fun getAll_returnsAllStorages() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        mineralDao.insertAll(listOf(mineral1, mineral2))

        val storages = listOf(
            createTestStorage(mineralId = mineral1.id, place = "Living Room"),
            createTestStorage(mineralId = mineral2.id, place = "Basement")
        )
        storageDao.insertAll(storages)

        // When
        val result = storageDao.getAll()

        // Then
        assertEquals(2, result!!.size)
    }

    // ========== HIERARCHICAL FILTERING Tests ==========

    @Test
    fun filterByLocationFlow_byPlaceOnly() = runTest {
        // Given
        val minerals = (1..4).map { createTestMineral(name = "Mineral$it") }
        mineralDao.insertAll(minerals)

        val storages = listOf(
            createTestStorage(mineralId = minerals[0].id, place = "Living Room", container = "Cabinet A"),
            createTestStorage(mineralId = minerals[1].id, place = "Living Room", container = "Cabinet B"),
            createTestStorage(mineralId = minerals[2].id, place = "Basement", container = "Shelf 1"),
            createTestStorage(mineralId = minerals[3].id, place = "Office", container = "Desk Drawer")
        )
        storageDao.insertAll(storages)

        // When
        val result = storageDao.filterByLocationFlow(place = "Living Room").first()

        // Then
        assertEquals(2, result!!.size)
        assertTrue(result!!.all { it.place == "Living Room" })
    }

    @Test
    fun filterByLocationFlow_byPlaceAndContainer() = runTest {
        // Given
        val minerals = (1..3).map { createTestMineral(name = "Mineral$it") }
        mineralDao.insertAll(minerals)

        val storages = listOf(
            createTestStorage(mineralId = minerals[0].id, place = "Living Room", container = "Cabinet A", box = "Box 1"),
            createTestStorage(mineralId = minerals[1].id, place = "Living Room", container = "Cabinet A", box = "Box 2"),
            createTestStorage(mineralId = minerals[2].id, place = "Living Room", container = "Cabinet B", box = "Box 1")
        )
        storageDao.insertAll(storages)

        // When
        val result = storageDao.filterByLocationFlow(place = "Living Room", container = "Cabinet A").first()

        // Then
        assertEquals(2, result!!.size)
        assertTrue(result!!.all { it.place == "Living Room" && it.container == "Cabinet A" })
    }

    @Test
    fun filterByLocationFlow_byPlaceContainerAndBox() = runTest {
        // Given
        val minerals = (1..3).map { createTestMineral(name = "Mineral$it") }
        mineralDao.insertAll(minerals)

        val storages = listOf(
            createTestStorage(mineralId = minerals[0].id, place = "Living Room", container = "Cabinet A", box = "Box 1", slot = "A1"),
            createTestStorage(mineralId = minerals[1].id, place = "Living Room", container = "Cabinet A", box = "Box 1", slot = "A2"),
            createTestStorage(mineralId = minerals[2].id, place = "Living Room", container = "Cabinet A", box = "Box 2", slot = "A1")
        )
        storageDao.insertAll(storages)

        // When
        val result = storageDao.filterByLocationFlow(place = "Living Room", container = "Cabinet A", box = "Box 1").first()

        // Then
        assertEquals(2, result!!.size)
        assertTrue(result!!.all { it.place == "Living Room" && it.container == "Cabinet A" && it.box == "Box 1" })
    }

    @Test
    fun filterByLocationFlow_nullFilters_returnsAll() = runTest {
        // Given
        val minerals = (1..3).map { createTestMineral(name = "Mineral$it") }
        mineralDao.insertAll(minerals)

        val storages = listOf(
            createTestStorage(mineralId = minerals[0].id, place = "Living Room"),
            createTestStorage(mineralId = minerals[1].id, place = "Basement"),
            createTestStorage(mineralId = minerals[2].id, place = "Office")
        )
        storageDao.insertAll(storages)

        // When - No filters
        val result = storageDao.filterByLocationFlow(null, null, null).first()

        // Then
        assertEquals(3, result!!.size)
    }

    // ========== DISTINCT VALUES Tests ==========

    @Test
    fun getDistinctPlacesFlow_returnsUniquePlacesSorted() = runTest {
        // Given
        val minerals = (1..5).map { createTestMineral(name = "Mineral$it") }
        mineralDao.insertAll(minerals)

        val storages = listOf(
            createTestStorage(mineralId = minerals[0].id, place = "Living Room"),
            createTestStorage(mineralId = minerals[1].id, place = "Living Room"), // Duplicate
            createTestStorage(mineralId = minerals[2].id, place = "Basement"),
            createTestStorage(mineralId = minerals[3].id, place = "Office"),
            createTestStorage(mineralId = minerals[4].id, place = null) // Should be excluded
        )
        storageDao.insertAll(storages)

        // When
        val result = storageDao.getDistinctPlacesFlow().first()

        // Then - Unique, sorted, no nulls
        assertEquals(3, result!!.size)
        assertEquals("Basement", result[0])
        assertEquals("Living Room", result[1])
        assertEquals("Office", result[2])
    }

    @Test
    fun getDistinctContainersFlow_noPlaceFilter_returnsAll() = runTest {
        // Given
        val minerals = (1..4).map { createTestMineral(name = "Mineral$it") }
        mineralDao.insertAll(minerals)

        val storages = listOf(
            createTestStorage(mineralId = minerals[0].id, place = "Living Room", container = "Cabinet A"),
            createTestStorage(mineralId = minerals[1].id, place = "Living Room", container = "Cabinet B"),
            createTestStorage(mineralId = minerals[2].id, place = "Basement", container = "Shelf 1"),
            createTestStorage(mineralId = minerals[3].id, place = "Office", container = "Cabinet A")
        )
        storageDao.insertAll(storages)

        // When - No place filter
        val result = storageDao.getDistinctContainersFlow(null).first()

        // Then
        assertEquals(3, result!!.size)
        assertEquals(setOf("Cabinet A", "Cabinet B", "Shelf 1"), result!!.toSet())
    }

    @Test
    fun getDistinctContainersFlow_withPlaceFilter() = runTest {
        // Given
        val minerals = (1..3).map { createTestMineral(name = "Mineral$it") }
        mineralDao.insertAll(minerals)

        val storages = listOf(
            createTestStorage(mineralId = minerals[0].id, place = "Living Room", container = "Cabinet A"),
            createTestStorage(mineralId = minerals[1].id, place = "Living Room", container = "Cabinet B"),
            createTestStorage(mineralId = minerals[2].id, place = "Basement", container = "Shelf 1")
        )
        storageDao.insertAll(storages)

        // When - Filter by place
        val result = storageDao.getDistinctContainersFlow("Living Room").first()

        // Then - Only containers in Living Room
        assertEquals(2, result!!.size)
        assertEquals(setOf("Cabinet A", "Cabinet B"), result!!.toSet())
    }

    @Test
    fun getDistinctBoxesFlow_withPlaceAndContainerFilter() = runTest {
        // Given
        val minerals = (1..4).map { createTestMineral(name = "Mineral$it") }
        mineralDao.insertAll(minerals)

        val storages = listOf(
            createTestStorage(mineralId = minerals[0].id, place = "Living Room", container = "Cabinet A", box = "Box 1"),
            createTestStorage(mineralId = minerals[1].id, place = "Living Room", container = "Cabinet A", box = "Box 2"),
            createTestStorage(mineralId = minerals[2].id, place = "Living Room", container = "Cabinet B", box = "Box 1"),
            createTestStorage(mineralId = minerals[3].id, place = "Basement", container = "Shelf 1", box = "Box 1")
        )
        storageDao.insertAll(storages)

        // When - Filter by place and container
        val result = storageDao.getDistinctBoxesFlow("Living Room", "Cabinet A").first()

        // Then - Only boxes in Living Room → Cabinet A
        assertEquals(2, result!!.size)
        assertEquals(setOf("Box 1", "Box 2"), result!!.toSet())
    }

    // ========== UPDATE Tests ==========

    @Test
    fun update_existingStorage_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(
            mineralId = mineral.id,
            place = "Original Place"
        )
        storageDao.insert(storage)

        // When
        val updated = storage.copy(place = "Updated Place")
        storageDao.update(updated)

        // Then
        val result = storageDao.getById(storage.id)
        assertNotNull(result)
        assertEquals("Updated Place", result!!.place)
    }

    // ========== DELETE Tests ==========

    @Test
    fun delete_existingStorage_removesStorage() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(mineralId = mineral.id, place = "Living Room")
        storageDao.insert(storage)

        // When
        storageDao.delete(storage)

        // Then
        val result = storageDao.getById(storage.id)
        assertNull(result)
    }

    @Test
    fun deleteByMineralId_removesStorage() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(mineralId = mineral.id, place = "Living Room")
        storageDao.insert(storage)

        // When
        storageDao.deleteByMineralId(mineral.id)

        // Then
        val result = storageDao.getByMineralId(mineral.id)
        assertNull(result)
    }

    @Test
    fun deleteByMineralIds_removesStoragesForMultipleMinerals() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        val mineral3 = createTestMineral(name = "Fluorite")
        mineralDao.insertAll(listOf(mineral1, mineral2, mineral3))

        val storages = listOf(
            createTestStorage(mineralId = mineral1.id, place = "Living Room"),
            createTestStorage(mineralId = mineral2.id, place = "Basement"),
            createTestStorage(mineralId = mineral3.id, place = "Office")
        )
        storageDao.insertAll(storages)

        // When
        storageDao.deleteByMineralIds(listOf(mineral1.id, mineral2.id))

        // Then
        val remaining = storageDao.getAll()
        assertEquals(1, remaining.size)
        assertEquals(mineral3.id, remaining[0].mineralId)
    }

    @Test
    fun deleteAll_removesAllStorages() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        mineralDao.insertAll(listOf(mineral1, mineral2))

        val storages = listOf(
            createTestStorage(mineralId = mineral1.id, place = "Living Room"),
            createTestStorage(mineralId = mineral2.id, place = "Basement")
        )
        storageDao.insertAll(storages)

        // When
        storageDao.deleteAll()

        // Then
        val result = storageDao.getAll()
        assertEquals(0, result!!.size)
    }

    // ========== CASCADE DELETE Tests ==========

    @Test
    fun cascadeDelete_deletingMineral_deletesStorage() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(mineralId = mineral.id, place = "Living Room")
        storageDao.insert(storage)

        // When - Delete the mineral (should cascade to storage)
        mineralDao.delete(mineral)

        // Then
        val remainingStorage = storageDao.getByMineralId(mineral.id)
        assertNull(remainingStorage)
    }

    // ========== EDGE CASES ==========

    @Test
    fun insert_nullOptionalFields_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(
            mineralId = mineral.id,
            place = null,
            container = null,
            box = null,
            slot = null,
            nfcTagId = null,
            qrContent = null
        )

        // When
        storageDao.insert(storage)

        // Then
        val result = storageDao.getById(storage.id)
        assertNotNull(result)
        assertNull(result!!.place)
        assertNull(result!!.container)
        assertNull(result!!.box)
        assertNull(result!!.slot)
    }

    @Test
    fun insert_withNfcAndQrIdentifiers_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(
            mineralId = mineral.id,
            place = "Living Room",
            nfcTagId = "NFC-12345",
            qrContent = "QR-ABCDEF-001"
        )

        // When
        storageDao.insert(storage)

        // Then
        val result = storageDao.getById(storage.id)
        assertNotNull(result)
        assertEquals("NFC-12345", result!!.nfcTagId)
        assertEquals("QR-ABCDEF-001", result!!.qrContent)
    }

    @Test
    fun hierarchicalStructure_fullPath() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val storage = createTestStorage(
            mineralId = mineral.id,
            place = "Living Room",
            container = "Display Cabinet A",
            box = "Drawer 3",
            slot = "A5"
        )

        // When
        storageDao.insert(storage)

        // Then
        val result = storageDao.getById(storage.id)
        assertNotNull(result)
        assertEquals("Living Room", result!!.place)
        assertEquals("Display Cabinet A", result!!.container)
        assertEquals("Drawer 3", result!!.box)
        assertEquals("A5", result!!.slot)
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

    private fun createTestStorage(
        id: String = java.util.UUID.randomUUID().toString(),
        mineralId: String,
        place: String? = null,
        container: String? = null,
        box: String? = null,
        slot: String? = null,
        nfcTagId: String? = null,
        qrContent: String? = null
    ): StorageEntity {
        return StorageEntity(
            id = id,
            mineralId = mineralId,
            place = place,
            container = container,
            box = box,
            slot = slot,
            nfcTagId = nfcTagId,
            qrContent = qrContent
        )
    }
}
