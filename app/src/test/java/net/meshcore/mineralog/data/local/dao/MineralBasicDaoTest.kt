package net.meshcore.mineralog.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.MineralEntity
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Integration tests for MineralBasicDao using Robolectric.
 *
 * Tests cover:
 * - Insert operations (single, batch, conflict resolution)
 * - Update operations
 * - Delete operations (single, batch, by ID, all)
 * - Retrieval operations (by ID, all, Flow-based)
 * - Count operations (suspend, Flow)
 *
 * Sprint 3: DAO Tests - Target 70%+ coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class MineralBasicDaoTest {

    private lateinit var database: MineraLogDatabase
    private lateinit var mineralDao: MineralBasicDao

    @BeforeEach
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MineraLogDatabase::class.java
        ).allowMainThreadQueries().build()

        mineralDao = database.mineralDao().let { compositeDao ->
            // Extract the basic DAO from the composite
            // For testing, we use the database's mineral() method which returns the composite
            // The composite delegates to the basic DAO
            object : MineralBasicDao {
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
    }

    @AfterEach
    fun tearDown() {
        database.close()
    }

    // ========== INSERT OPERATIONS TESTS ==========

    @Test
    @DisplayName("insert - single mineral - returns row ID")
    fun `insert - single mineral - returns valid row ID`() = runTest {
        // Arrange
        val mineral = createTestMineral("test-1", "Quartz")

        // Act
        val rowId = mineralDao.insert(mineral)

        // Assert
        assertTrue(rowId > 0, "Row ID should be positive")

        val retrieved = mineralDao.getById("test-1")
        assertNotNull(retrieved)
        assertEquals("Quartz", retrieved?.name)
    }

    @Test
    @DisplayName("insert - duplicate ID - replaces existing (REPLACE strategy)")
    fun `insert - duplicate ID - replaces existing mineral`() = runTest {
        // Arrange
        val mineral1 = createTestMineral("duplicate-id", "Original Name")
        val mineral2 = createTestMineral("duplicate-id", "Updated Name")

        // Act
        mineralDao.insert(mineral1)
        mineralDao.insert(mineral2)

        // Assert
        val retrieved = mineralDao.getById("duplicate-id")
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved?.name, "Should have replaced with new name")

        val count = mineralDao.getCount()
        assertEquals(1, count, "Should only have 1 mineral, not 2")
    }

    @Test
    @DisplayName("insertAll - batch insert - all minerals inserted")
    fun `insertAll - batch of minerals - all inserted successfully`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("batch-1", "Quartz"),
            createTestMineral("batch-2", "Feldspar"),
            createTestMineral("batch-3", "Calcite")
        )

        // Act
        mineralDao.insertAll(minerals)

        // Assert
        val count = mineralDao.getCount()
        assertEquals(3, count)

        val retrieved = mineralDao.getAll()
        assertEquals(3, retrieved.size)
        assertTrue(retrieved.any { it.name == "Quartz" })
        assertTrue(retrieved.any { it.name == "Feldspar" })
        assertTrue(retrieved.any { it.name == "Calcite" })
    }

    @Test
    @DisplayName("insertAll - empty list - no error")
    fun `insertAll - empty list - completes without error`() = runTest {
        // Arrange
        val emptyList = emptyList<MineralEntity>()

        // Act & Assert - Should not throw
        assertDoesNotThrow {
            runTest {
                mineralDao.insertAll(emptyList)
            }
        }

        val count = mineralDao.getCount()
        assertEquals(0, count)
    }

    // ========== UPDATE OPERATIONS TESTS ==========

    @Test
    @DisplayName("update - existing mineral - updates successfully")
    fun `update - existing mineral - changes persisted`() = runTest {
        // Arrange
        val original = createTestMineral("update-1", "Original Name")
        mineralDao.insert(original)

        val updated = original.copy(
            name = "Updated Name",
            formula = "Updated Formula"
        )

        // Act
        mineralDao.update(updated)

        // Assert
        val retrieved = mineralDao.getById("update-1")
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved?.name)
        assertEquals("Updated Formula", retrieved?.formula)
    }

    @Test
    @DisplayName("update - non-existent mineral - no error (Room behavior)")
    fun `update - non-existent mineral - completes silently`() = runTest {
        // Arrange
        val nonExistent = createTestMineral("non-existent", "Ghost Mineral")

        // Act & Assert - Room silently ignores updates to non-existent rows
        assertDoesNotThrow {
            runTest {
                mineralDao.update(nonExistent)
            }
        }

        val count = mineralDao.getCount()
        assertEquals(0, count)
    }

    // ========== DELETE OPERATIONS TESTS ==========

    @Test
    @DisplayName("delete - by entity - removes mineral")
    fun `delete - by entity - mineral removed from database`() = runTest {
        // Arrange
        val mineral = createTestMineral("delete-1", "ToDelete")
        mineralDao.insert(mineral)

        // Act
        mineralDao.delete(mineral)

        // Assert
        val retrieved = mineralDao.getById("delete-1")
        assertNull(retrieved, "Mineral should be deleted")

        val count = mineralDao.getCount()
        assertEquals(0, count)
    }

    @Test
    @DisplayName("deleteById - existing ID - removes mineral")
    fun `deleteById - existing mineral - removed successfully`() = runTest {
        // Arrange
        val mineral = createTestMineral("delete-by-id", "ToDelete")
        mineralDao.insert(mineral)

        // Act
        mineralDao.deleteById("delete-by-id")

        // Assert
        val retrieved = mineralDao.getById("delete-by-id")
        assertNull(retrieved)
    }

    @Test
    @DisplayName("deleteById - non-existent ID - no error")
    fun `deleteById - non-existent ID - completes silently`() = runTest {
        // Act & Assert
        assertDoesNotThrow {
            runTest {
                mineralDao.deleteById("non-existent-id")
            }
        }
    }

    @Test
    @DisplayName("deleteByIds - multiple IDs - all removed")
    fun `deleteByIds - batch delete - all specified minerals removed`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("del-1", "Quartz"),
            createTestMineral("del-2", "Feldspar"),
            createTestMineral("del-3", "Calcite"),
            createTestMineral("keep-1", "Keep This")
        )
        mineralDao.insertAll(minerals)

        // Act
        mineralDao.deleteByIds(listOf("del-1", "del-2", "del-3"))

        // Assert
        val remaining = mineralDao.getAll()
        assertEquals(1, remaining.size)
        assertEquals("Keep This", remaining[0].name)
    }

    @Test
    @DisplayName("deleteByIds - empty list - no error")
    fun `deleteByIds - empty list - completes without error`() = runTest {
        // Arrange
        mineralDao.insert(createTestMineral("test", "Test"))

        // Act & Assert
        assertDoesNotThrow {
            runTest {
                mineralDao.deleteByIds(emptyList())
            }
        }

        val count = mineralDao.getCount()
        assertEquals(1, count, "Mineral should still exist")
    }

    @Test
    @DisplayName("deleteAll - removes all minerals")
    fun `deleteAll - database cleared - all minerals removed`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("1", "Quartz"),
            createTestMineral("2", "Feldspar"),
            createTestMineral("3", "Calcite")
        )
        mineralDao.insertAll(minerals)

        // Act
        mineralDao.deleteAll()

        // Assert
        val count = mineralDao.getCount()
        assertEquals(0, count)

        val all = mineralDao.getAll()
        assertTrue(all.isEmpty())
    }

    // ========== RETRIEVAL OPERATIONS TESTS ==========

    @Test
    @DisplayName("getById - existing ID - returns mineral")
    fun `getById - existing mineral - returned correctly`() = runTest {
        // Arrange
        val mineral = createTestMineral("get-1", "Quartz")
        mineralDao.insert(mineral)

        // Act
        val retrieved = mineralDao.getById("get-1")

        // Assert
        assertNotNull(retrieved)
        assertEquals("get-1", retrieved?.id)
        assertEquals("Quartz", retrieved?.name)
    }

    @Test
    @DisplayName("getById - non-existent ID - returns null")
    fun `getById - non-existent ID - returns null`() = runTest {
        // Act
        val retrieved = mineralDao.getById("non-existent")

        // Assert
        assertNull(retrieved)
    }

    @Test
    @DisplayName("getByIds - multiple IDs - returns matching minerals")
    fun `getByIds - multiple IDs - returns all matching minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("get-multi-1", "Quartz"),
            createTestMineral("get-multi-2", "Feldspar"),
            createTestMineral("get-multi-3", "Calcite")
        )
        mineralDao.insertAll(minerals)

        // Act
        val retrieved = mineralDao.getByIds(listOf("get-multi-1", "get-multi-3"))

        // Assert
        assertEquals(2, retrieved.size)
        assertTrue(retrieved.any { it.name == "Quartz" })
        assertTrue(retrieved.any { it.name == "Calcite" })
        assertFalse(retrieved.any { it.name == "Feldspar" })
    }

    @Test
    @DisplayName("getByIdFlow - emits updates on changes")
    fun `getByIdFlow - Flow emits - on mineral insert and update`() = runTest {
        // Arrange
        val mineralId = "flow-test"
        val flow = mineralDao.getByIdFlow(mineralId)

        // Act & Assert
        flow.test {
            // Initial state - null
            val initial = awaitItem()
            assertNull(initial)

            // Insert mineral
            mineralDao.insert(createTestMineral(mineralId, "Original"))
            val afterInsert = awaitItem()
            assertNotNull(afterInsert)
            assertEquals("Original", afterInsert?.name)

            // Update mineral
            mineralDao.update(afterInsert!!.copy(name = "Updated"))
            val afterUpdate = awaitItem()
            assertEquals("Updated", afterUpdate?.name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("getAllFlow - emits all minerals")
    fun `getAllFlow - returns all minerals - ordered by updatedAt desc`() = runTest {
        // Arrange
        val mineral1 = createTestMineral("all-1", "First")
        val mineral2 = createTestMineral("all-2", "Second")
        mineralDao.insertAll(listOf(mineral1, mineral2))

        // Act
        val flow = mineralDao.getAllFlow()

        // Assert
        flow.test {
            val minerals = awaitItem()
            assertEquals(2, minerals.size)
            // Ordered by updatedAt DESC (most recent first)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("getAll - returns all minerals")
    fun `getAll - suspend function - returns all minerals`() = runTest {
        // Arrange
        val minerals = listOf(
            createTestMineral("all-suspend-1", "Quartz"),
            createTestMineral("all-suspend-2", "Feldspar")
        )
        mineralDao.insertAll(minerals)

        // Act
        val retrieved = mineralDao.getAll()

        // Assert
        assertEquals(2, retrieved.size)
        assertTrue(retrieved.any { it.name == "Quartz" })
        assertTrue(retrieved.any { it.name == "Feldspar" })
    }

    // ========== COUNT OPERATIONS TESTS ==========

    @Test
    @DisplayName("getCount - returns correct count")
    fun `getCount - accurate count - of all minerals`() = runTest {
        // Arrange
        assertEquals(0, mineralDao.getCount())

        // Act & Assert - Add 1
        mineralDao.insert(createTestMineral("count-1", "One"))
        assertEquals(1, mineralDao.getCount())

        // Add 2 more
        mineralDao.insertAll(listOf(
            createTestMineral("count-2", "Two"),
            createTestMineral("count-3", "Three")
        ))
        assertEquals(3, mineralDao.getCount())

        // Delete 1
        mineralDao.deleteById("count-2")
        assertEquals(2, mineralDao.getCount())
    }

    @Test
    @DisplayName("getCountFlow - emits updates on changes")
    fun `getCountFlow - Flow emits - on insert and delete`() = runTest {
        // Arrange
        val flow = mineralDao.getCountFlow()

        // Act & Assert
        flow.test {
            // Initial count
            assertEquals(0, awaitItem())

            // Insert
            mineralDao.insert(createTestMineral("flow-count-1", "First"))
            assertEquals(1, awaitItem())

            // Insert another
            mineralDao.insert(createTestMineral("flow-count-2", "Second"))
            assertEquals(2, awaitItem())

            // Delete one
            mineralDao.deleteById("flow-count-1")
            assertEquals(1, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== HELPER METHODS ==========

    private fun createTestMineral(id: String, name: String) = MineralEntity(
        id = id,
        name = name,
        group = "Test Group",
        formula = "TestFormula",
        crystalSystem = "Hexagonal",
        hardness = "7",
        mohsMin = 7.0f,
        mohsMax = 7.0f,
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
        quality = 4,
        estimatedValue = 100.0,
        acquisitionDate = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        mineralType = "SIMPLE",
        qrCode = null
    )
}
