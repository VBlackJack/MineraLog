package net.meshcore.mineralog.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.FilterPresetEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for [FilterPresetDao].
 *
 * Tests cover:
 * - CRUD operations
 * - Search by name (case-insensitive)
 * - Sorting by updatedAt DESC
 * - Count operations
 * - JSON criteria storage
 * - Edge cases (special characters, empty results)
 *
 * Uses in-memory database for fast, isolated tests.
 */
@RunWith(AndroidJUnit4::class)
class FilterPresetDaoTest {

    private lateinit var database: MineraLogDatabase
    private lateinit var filterPresetDao: FilterPresetDao

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MineraLogDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        filterPresetDao = database.filterPresetDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== CREATE Tests ==========

    @Test
    fun insert_singlePreset_success() = runTest {
        // Given
        val preset = createTestPreset(
            name = "High-Value Quartz",
            criteriaJson = """{"group":"Quartz","minValue":100}"""
        )

        // When
        filterPresetDao.insert(preset)

        // Then
        val retrieved = filterPresetDao.getById(preset.id)
        assertNotNull(retrieved)
        assertEquals("High-Value Quartz", retrieved.name)
        assertEquals("""{"group":"Quartz","minValue":100}""", retrieved.criteriaJson)
    }

    @Test
    fun insertAll_multiplePresets_success() = runTest {
        // Given
        val presets = listOf(
            createTestPreset(name = "Preset 1", criteriaJson = "{}"),
            createTestPreset(name = "Preset 2", criteriaJson = "{}"),
            createTestPreset(name = "Preset 3", criteriaJson = "{}")
        )

        // When
        filterPresetDao.insertAll(presets)

        // Then
        val all = filterPresetDao.getAll()
        assertEquals(3, all.size)
    }

    @Test
    fun insert_withReplace_updatesExisting() = runTest {
        // Given
        val preset = createTestPreset(
            name = "Original Name",
            criteriaJson = """{"group":"Quartz"}"""
        )
        filterPresetDao.insert(preset)

        // When - insert with same ID but different data (REPLACE strategy)
        val updated = preset.copy(
            name = "Updated Name",
            criteriaJson = """{"group":"Calcite"}""",
            updatedAt = Instant.now()
        )
        filterPresetDao.insert(updated)

        // Then
        val retrieved = filterPresetDao.getById(preset.id)
        assertNotNull(retrieved)
        assertEquals("Updated Name", retrieved.name)
        assertEquals("""{"group":"Calcite"}""", retrieved.criteriaJson)
    }

    @Test
    fun insert_withCustomIcon_success() = runTest {
        // Given
        val preset = createTestPreset(
            name = "Favorites",
            icon = "star",
            criteriaJson = "{}"
        )

        // When
        filterPresetDao.insert(preset)

        // Then
        val retrieved = filterPresetDao.getById(preset.id)
        assertNotNull(retrieved)
        assertEquals("star", retrieved.icon)
    }

    // ========== READ Tests ==========

    @Test
    fun getById_existingPreset_returnsPreset() = runTest {
        // Given
        val preset = createTestPreset(name = "Test Preset", criteriaJson = "{}")
        filterPresetDao.insert(preset)

        // When
        val result = filterPresetDao.getById(preset.id)

        // Then
        assertNotNull(result)
        assertEquals(preset.id, result.id)
        assertEquals("Test Preset", result.name)
    }

    @Test
    fun getById_nonExistingPreset_returnsNull() = runTest {
        // When
        val result = filterPresetDao.getById("non-existing-id")

        // Then
        assertNull(result)
    }

    @Test
    fun getByIdFlow_emitsUpdates() = runTest {
        // Given
        val preset = createTestPreset(name = "Test Preset", criteriaJson = "{}")

        // When
        val flow = filterPresetDao.getByIdFlow(preset.id)
        val initial = flow.first()
        assertNull(initial)

        filterPresetDao.insert(preset)
        val afterInsert = flow.first()

        // Then
        assertNotNull(afterInsert)
        assertEquals(preset.id, afterInsert.id)
    }

    @Test
    fun getAll_returnsAllPresetsSortedByUpdatedAt() = runTest {
        // Given
        val now = Instant.now()
        val presets = listOf(
            createTestPreset(name = "Oldest", criteriaJson = "{}", updatedAt = now.minusSeconds(300)),
            createTestPreset(name = "Middle", criteriaJson = "{}", updatedAt = now.minusSeconds(200)),
            createTestPreset(name = "Newest", criteriaJson = "{}", updatedAt = now.minusSeconds(100))
        )
        filterPresetDao.insertAll(presets)

        // When
        val result = filterPresetDao.getAll()

        // Then - Should be sorted DESC by updatedAt (newest first)
        assertEquals(3, result.size)
        assertEquals("Newest", result[0].name)
        assertEquals("Middle", result[1].name)
        assertEquals("Oldest", result[2].name)
    }

    @Test
    fun getAllFlow_emitsAllPresets() = runTest {
        // Given
        val presets = listOf(
            createTestPreset(name = "Preset 1", criteriaJson = "{}"),
            createTestPreset(name = "Preset 2", criteriaJson = "{}")
        )

        // When
        val flow = filterPresetDao.getAllFlow()
        val initial = flow.first()
        assertEquals(0, initial.size)

        filterPresetDao.insertAll(presets)
        val afterInsert = flow.first()

        // Then
        assertEquals(2, afterInsert.size)
    }

    @Test
    fun getAllFlow_maintainsDescendingOrder() = runTest {
        // Given
        val now = Instant.now()
        val presets = listOf(
            createTestPreset(name = "First", criteriaJson = "{}", updatedAt = now.minusSeconds(100)),
            createTestPreset(name = "Second", criteriaJson = "{}", updatedAt = now.minusSeconds(50))
        )
        filterPresetDao.insertAll(presets)

        // When
        val result = filterPresetDao.getAllFlow().first()

        // Then - Newest first
        assertEquals("Second", result[0].name)
        assertEquals("First", result[1].name)
    }

    // ========== SEARCH Tests ==========

    @Test
    fun searchFlow_exactMatch_returnsPreset() = runTest {
        // Given
        val preset = createTestPreset(name = "High-Value Quartz", criteriaJson = "{}")
        filterPresetDao.insert(preset)

        // When
        val result = filterPresetDao.searchFlow("High-Value Quartz").first()

        // Then
        assertEquals(1, result.size)
        assertEquals("High-Value Quartz", result[0].name)
    }

    @Test
    fun searchFlow_partialMatch_returnsPreset() = runTest {
        // Given
        val presets = listOf(
            createTestPreset(name = "High-Value Quartz", criteriaJson = "{}"),
            createTestPreset(name = "Low-Value Quartz", criteriaJson = "{}"),
            createTestPreset(name = "Fluorescent Calcite", criteriaJson = "{}")
        )
        filterPresetDao.insertAll(presets)

        // When
        val result = filterPresetDao.searchFlow("Quartz").first()

        // Then - Should match both presets with "Quartz"
        assertEquals(2, result.size)
        assertTrue(result.all { "Quartz" in it.name })
    }

    @Test
    fun searchFlow_caseInsensitive() = runTest {
        // Given
        val preset = createTestPreset(name = "High-Value Quartz", criteriaJson = "{}")
        filterPresetDao.insert(preset)

        // When - Search with different case
        val resultLower = filterPresetDao.searchFlow("quartz").first()
        val resultUpper = filterPresetDao.searchFlow("QUARTZ").first()

        // Then - Both should find the preset (SQLite LIKE is case-insensitive by default)
        assertEquals(1, resultLower.size)
        assertEquals(1, resultUpper.size)
    }

    @Test
    fun searchFlow_noMatch_returnsEmpty() = runTest {
        // Given
        val preset = createTestPreset(name = "High-Value Quartz", criteriaJson = "{}")
        filterPresetDao.insert(preset)

        // When
        val result = filterPresetDao.searchFlow("Calcite").first()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun searchFlow_emptyQuery_returnsAll() = runTest {
        // Given
        val presets = listOf(
            createTestPreset(name = "Preset 1", criteriaJson = "{}"),
            createTestPreset(name = "Preset 2", criteriaJson = "{}")
        )
        filterPresetDao.insertAll(presets)

        // When - Empty string matches everything
        val result = filterPresetDao.searchFlow("").first()

        // Then
        assertEquals(2, result.size)
    }

    // ========== COUNT Tests ==========

    @Test
    fun getCount_returnsCorrectCount() = runTest {
        // Given
        val presets = listOf(
            createTestPreset(name = "Preset 1", criteriaJson = "{}"),
            createTestPreset(name = "Preset 2", criteriaJson = "{}"),
            createTestPreset(name = "Preset 3", criteriaJson = "{}")
        )
        filterPresetDao.insertAll(presets)

        // When
        val count = filterPresetDao.getCount()

        // Then
        assertEquals(3, count)
    }

    @Test
    fun getCount_emptyTable_returnsZero() = runTest {
        // When
        val count = filterPresetDao.getCount()

        // Then
        assertEquals(0, count)
    }

    @Test
    fun getCountFlow_emitsUpdates() = runTest {
        // Given
        val flow = filterPresetDao.getCountFlow()

        // When
        val initialCount = flow.first()
        assertEquals(0, initialCount)

        filterPresetDao.insert(createTestPreset(name = "Preset 1", criteriaJson = "{}"))
        val afterInsert = flow.first()

        // Then
        assertEquals(1, afterInsert)
    }

    // ========== UPDATE Tests ==========

    @Test
    fun update_existingPreset_success() = runTest {
        // Given
        val preset = createTestPreset(
            name = "Original Name",
            criteriaJson = """{"group":"Quartz"}"""
        )
        filterPresetDao.insert(preset)

        // When
        val updated = preset.copy(
            name = "Updated Name",
            updatedAt = Instant.now().plusSeconds(10)
        )
        filterPresetDao.update(updated)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNotNull(result)
        assertEquals("Updated Name", result.name)
    }

    @Test
    fun update_criteriaJson_success() = runTest {
        // Given
        val preset = createTestPreset(
            name = "Filter",
            criteriaJson = """{"group":"Quartz"}"""
        )
        filterPresetDao.insert(preset)

        // When - Update criteria
        val updated = preset.copy(criteriaJson = """{"group":"Calcite","minHardness":3}""")
        filterPresetDao.update(updated)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNotNull(result)
        assertEquals("""{"group":"Calcite","minHardness":3}""", result.criteriaJson)
    }

    // ========== DELETE Tests ==========

    @Test
    fun delete_existingPreset_removesPreset() = runTest {
        // Given
        val preset = createTestPreset(name = "Test Preset", criteriaJson = "{}")
        filterPresetDao.insert(preset)

        // When
        filterPresetDao.delete(preset)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNull(result)
    }

    @Test
    fun deleteById_existingPreset_removesPreset() = runTest {
        // Given
        val preset = createTestPreset(name = "Test Preset", criteriaJson = "{}")
        filterPresetDao.insert(preset)

        // When
        filterPresetDao.deleteById(preset.id)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNull(result)
    }

    @Test
    fun deleteAll_removesAllPresets() = runTest {
        // Given
        val presets = listOf(
            createTestPreset(name = "Preset 1", criteriaJson = "{}"),
            createTestPreset(name = "Preset 2", criteriaJson = "{}")
        )
        filterPresetDao.insertAll(presets)

        // When
        filterPresetDao.deleteAll()

        // Then
        val result = filterPresetDao.getAll()
        assertEquals(0, result.size)
    }

    // ========== JSON CRITERIA Tests ==========

    @Test
    fun criteriaJson_complexJson_storedCorrectly() = runTest {
        // Given
        val complexJson = """
            {
                "group": "Quartz",
                "minHardness": 7.0,
                "maxHardness": 7.0,
                "colors": ["Clear", "Smoky", "Rose"],
                "tags": ["favorites", "collection"],
                "valueRange": {
                    "min": 100,
                    "max": 1000
                }
            }
        """.trimIndent()
        val preset = createTestPreset(
            name = "Complex Filter",
            criteriaJson = complexJson
        )

        // When
        filterPresetDao.insert(preset)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNotNull(result)
        assertEquals(complexJson, result.criteriaJson)
    }

    @Test
    fun criteriaJson_emptyJson_success() = runTest {
        // Given
        val preset = createTestPreset(name = "Empty Filter", criteriaJson = "{}")

        // When
        filterPresetDao.insert(preset)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNotNull(result)
        assertEquals("{}", result.criteriaJson)
    }

    @Test
    fun criteriaJson_withSpecialCharacters_success() = runTest {
        // Given
        val jsonWithSpecialChars = """{"name":"Quartz \"Premium\"","notes":"Line1\nLine2"}"""
        val preset = createTestPreset(
            name = "Special Chars",
            criteriaJson = jsonWithSpecialChars
        )

        // When
        filterPresetDao.insert(preset)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNotNull(result)
        assertEquals(jsonWithSpecialChars, result.criteriaJson)
    }

    // ========== EDGE CASES ==========

    @Test
    fun name_withSpecialCharacters_success() = runTest {
        // Given
        val preset = createTestPreset(
            name = "Filter: Quartz (High-Value) - 2024",
            criteriaJson = "{}"
        )

        // When
        filterPresetDao.insert(preset)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNotNull(result)
        assertEquals("Filter: Quartz (High-Value) - 2024", result.name)
    }

    @Test
    fun name_withEmojis_success() = runTest {
        // Given
        val preset = createTestPreset(
            name = "⭐ Favorites ✨",
            criteriaJson = "{}"
        )

        // When
        filterPresetDao.insert(preset)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNotNull(result)
        assertEquals("⭐ Favorites ✨", result.name)
    }

    @Test
    fun timestamps_preservedCorrectly() = runTest {
        // Given
        val createdAt = Instant.parse("2024-01-01T12:00:00Z")
        val updatedAt = Instant.parse("2024-01-02T12:00:00Z")
        val preset = createTestPreset(
            name = "Test Preset",
            criteriaJson = "{}",
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // When
        filterPresetDao.insert(preset)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNotNull(result)
        assertEquals(createdAt, result.createdAt)
        assertEquals(updatedAt, result.updatedAt)
    }

    @Test
    fun defaultIcon_usedWhenNotSpecified() = runTest {
        // Given
        val preset = createTestPreset(name = "Test", criteriaJson = "{}")
        // Icon defaults to "filter_list" in entity

        // When
        filterPresetDao.insert(preset)

        // Then
        val result = filterPresetDao.getById(preset.id)
        assertNotNull(result)
        assertEquals("filter_list", result.icon)
    }

    // ========== Helper Methods ==========

    private fun createTestPreset(
        id: String = java.util.UUID.randomUUID().toString(),
        name: String,
        icon: String = "filter_list",
        criteriaJson: String,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now()
    ): FilterPresetEntity {
        return FilterPresetEntity(
            id = id,
            name = name,
            icon = icon,
            criteriaJson = criteriaJson,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
