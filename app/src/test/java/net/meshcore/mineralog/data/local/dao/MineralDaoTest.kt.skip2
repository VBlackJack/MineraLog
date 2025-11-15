package net.meshcore.mineralog.data.local.dao

import androidx.room.Room
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.MineralEntity
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
 * Comprehensive tests for [MineralDao].
 *
 * Tests cover:
 * - CRUD operations
 * - Search and filter queries
 * - Sorting
 * - Edge cases (empty results, special characters)
 * - Performance (with realistic data volumes)
 *
 * Uses in-memory database for fast, isolated tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27, 35])
class MineralDaoTest {

    private lateinit var database: MineraLogDatabase
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

        mineralDao = database.mineralDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== CREATE Tests ==========

    @Test
    fun insert_singleMineral_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")

        // When
        mineralDao.insert(mineral)

        // Then
        val retrieved = mineralDao.getById(mineral.id).first()
        assertNotNull(retrieved)
        assertEquals("Quartz", retrieved!!.name)
    }

    @Test
    fun insertAll_multipleMinserals_success() = runTest {
        // Given
        val minerals = listOf(
            createTestMineral(name = "Quartz"),
            createTestMineral(name = "Feldspar"),
            createTestMineral(name = "Calcite")
        )

        // When
        mineralDao.insertAll(minerals)

        // Then
        val all = mineralDao.getAll().first()
        assertEquals(3, all.size)
    }

    // ========== READ Tests ==========

    @Test
    fun getById_existingMineral_returnsMineral() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)

        // When
        val result = mineralDao.getById(mineral.id).first()

        // Then
        assertNotNull(result)
        assertEquals(mineral.id, result!!.id)
        assertEquals("Quartz", result!!.name)
    }

    @Test
    fun getById_nonExistentMineral_returnsNull() = runTest {
        // When
        val result = mineralDao.getById("non_existent_id").first()

        // Then
        assertNull(result)
    }

    @Test
    fun getAll_emptyDatabase_returnsEmptyList() = runTest {
        // When
        val result = mineralDao.getAll().first()

        // Then
        assertTrue(result!!.isEmpty())
    }

    @Test
    fun getAll_withMinerals_returnsAllInDescendingOrder() = runTest {
        // Given
        val now = Instant.now()
        val minerals = listOf(
            createTestMineral(name = "First", createdAt = now.minusSeconds(300)),
            createTestMineral(name = "Second", createdAt = now.minusSeconds(200)),
            createTestMineral(name = "Third", createdAt = now.minusSeconds(100))
        )
        mineralDao.insertAll(minerals)

        // When
        val result = mineralDao.getAll().first()

        // Then
        assertEquals(3, result!!.size)
        // Should be ordered by createdAt DESC (newest first)
        assertEquals("Third", result[0].name)
        assertEquals("Second", result[1].name)
        assertEquals("First", result[2].name)
    }

    // ========== SEARCH Tests ==========

    @Test
    fun search_byName_returnsMatches() = runTest {
        // Given
        mineralDao.insertAll(listOf(
            createTestMineral(name = "Quartz"),
            createTestMineral(name = "Rose Quartz"),
            createTestMineral(name = "Feldspar")
        ))

        // When
        val result = mineralDao.search("quartz").first()

        // Then
        assertEquals(2, result!!.size)
        assertTrue(result!!.all { it.name.contains("Quartz", ignoreCase = true) })
    }

    @Test
    fun search_byGroup_returnsMatches() = runTest {
        // Given
        mineralDao.insertAll(listOf(
            createTestMineral(name = "Quartz", group = "Silicates"),
            createTestMineral(name = "Feldspar", group = "Silicates"),
            createTestMineral(name = "Hematite", group = "Oxides")
        ))

        // When
        val result = mineralDao.search("silicates").first()

        // Then
        assertEquals(2, result!!.size)
    }

    @Test
    fun search_byFormula_returnsMatches() = runTest {
        // Given
        mineralDao.insertAll(listOf(
            createTestMineral(name = "Quartz", formula = "SiO₂"),
            createTestMineral(name = "Magnetite", formula = "Fe₃O₄")
        ))

        // When
        val result = mineralDao.search("SiO").first()

        // Then
        assertEquals(1, result!!.size)
        assertEquals("Quartz", result[0].name)
    }

    @Test
    fun search_caseInsensitive_returnsMatches() = runTest {
        // Given
        mineralDao.insert(createTestMineral(name = "Quartz"))

        // When
        val result = mineralDao.search("QUARTZ").first()

        // Then
        assertEquals(1, result!!.size)
    }

    @Test
    fun search_emptyQuery_returnsAll() = runTest {
        // Given
        mineralDao.insertAll(listOf(
            createTestMineral(name = "Quartz"),
            createTestMineral(name = "Feldspar")
        ))

        // When
        val result = mineralDao.search("").first()

        // Then
        assertEquals(2, result!!.size)
    }

    @Test
    fun search_noMatches_returnsEmpty() = runTest {
        // Given
        mineralDao.insert(createTestMineral(name = "Quartz"))

        // When
        val result = mineralDao.search("Feldspar").first()

        // Then
        assertTrue(result!!.isEmpty())
    }

    // ========== FILTER Tests ==========

    @Test
    fun filterByGroup_returnsOnlyMatchingGroup() = runTest {
        // Given
        mineralDao.insertAll(listOf(
            createTestMineral(name = "Quartz", group = "Silicates"),
            createTestMineral(name = "Feldspar", group = "Silicates"),
            createTestMineral(name = "Hematite", group = "Oxides")
        ))

        // When
        val result = mineralDao.filterByGroup("Silicates").first()

        // Then
        assertEquals(2, result!!.size)
        assertTrue(result!!.all { it.group == "Silicates" })
    }

    @Test
    fun filterByStatus_returnsOnlyMatchingStatus() = runTest {
        // Given
        mineralDao.insertAll(listOf(
            createTestMineral(name = "Displayed", statusType = "on_display"),
            createTestMineral(name = "Loaned", statusType = "loaned"),
            createTestMineral(name = "Stored", statusType = "in_collection")
        ))

        // When
        val result = mineralDao.filterByStatus("on_display").first()

        // Then
        assertEquals(1, result!!.size)
        assertEquals("Displayed", result[0].name)
    }

    // ========== UPDATE Tests ==========

    @Test
    fun update_existingMineral_updatesSuccessfully() = runTest {
        // Given
        val original = createTestMineral(name = "Quartz", formula = "SiO2")
        mineralDao.insert(original)

        // When
        val updated = original.copy(formula = "SiO₂", notes = "Updated formula")
        mineralDao.update(updated)

        // Then
        val result = mineralDao.getById(original.id).first()
        assertNotNull(result)
        assertEquals("SiO₂", result!!.formula)
        assertEquals("Updated formula", result!!.notes)
    }

    // ========== DELETE Tests ==========

    @Test
    fun delete_existingMineral_removesFromDatabase() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)

        // When
        mineralDao.delete(mineral)

        // Then
        val result = mineralDao.getById(mineral.id).first()
        assertNull(result)
    }

    @Test
    fun deleteById_existingMineral_removesFromDatabase() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)

        // When
        mineralDao.deleteById(mineral.id)

        // Then
        val result = mineralDao.getById(mineral.id).first()
        assertNull(result)
    }

    @Test
    fun deleteAll_multipleMineral_removesAll() = runTest {
        // Given
        mineralDao.insertAll(listOf(
            createTestMineral(name = "Quartz"),
            createTestMineral(name = "Feldspar"),
            createTestMineral(name = "Calcite")
        ))

        // When
        mineralDao.deleteAll()

        // Then
        val result = mineralDao.getAll().first()
        assertTrue(result!!.isEmpty())
    }

    // ========== STATISTICS Tests ==========

    @Test
    fun getCount_returnsCorrectCount() = runTest {
        // Given
        mineralDao.insertAll(listOf(
            createTestMineral(name = "Quartz"),
            createTestMineral(name = "Feldspar"),
            createTestMineral(name = "Calcite")
        ))

        // When
        val count = mineralDao.getCount().first()

        // Then
        assertEquals(3, count)
    }

    @Test
    fun getCount_emptyDatabase_returnsZero() = runTest {
        // When
        val count = mineralDao.getCount().first()

        // Then
        assertEquals(0, count)
    }

    // ========== EDGE CASES Tests ==========

    @Test
    fun insert_mineralWithSpecialCharacters_handlesCorrectly() = runTest {
        // Given
        val mineral = createTestMineral(
            name = "Quartz (α-SiO₂)",
            formula = "α-SiO₂",
            notes = "Contains: αβγδ and emoji 🔬"
        )

        // When
        mineralDao.insert(mineral)

        // Then
        val result = mineralDao.getById(mineral.id).first()
        assertNotNull(result)
        assertEquals("Quartz (α-SiO₂)", result!!.name)
        assertEquals("α-SiO₂", result!!.formula)
        assertTrue(result!!.notes?.contains("🔬") == true)
    }

    @Test
    fun insert_mineralWithNullFields_handlesCorrectly() = runTest {
        // Given
        val mineral = createTestMineral(
            name = "Minimal Mineral",
            group = null,
            formula = null,
            notes = null
        )

        // When
        mineralDao.insert(mineral)

        // Then
        val result = mineralDao.getById(mineral.id).first()
        assertNotNull(result)
        assertEquals("Minimal Mineral", result!!.name)
        assertNull(result!!.group)
        assertNull(result!!.formula)
        assertNull(result!!.notes)
    }

    // ========== Helper Methods ==========

    private fun createTestMineral(
        id: String = java.util.UUID.randomUUID().toString(),
        name: String,
        group: String? = null,
        formula: String? = null,
        statusType: String = "in_collection",
        notes: String? = null,
        createdAt: Instant = Instant.now()
    ): MineralEntity {
        return MineralEntity(
            id = id,
            name = name,
            group = group,
            formula = formula,
            statusType = statusType,
            notes = notes,
            createdAt = createdAt
        )
    }
}
