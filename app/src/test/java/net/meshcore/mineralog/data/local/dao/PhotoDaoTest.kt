package net.meshcore.mineralog.data.local.dao

import androidx.room.Room
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.data.local.MineraLogDatabase
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.PhotoEntity
import net.meshcore.mineralog.data.local.entity.PhotoType
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
 * Comprehensive tests for [PhotoDao].
 *
 * Tests cover:
 * - CRUD operations
 * - Batch operations (getByMineralIds, deleteByMineralIds)
 * - Cascade deletes (when mineral is deleted)
 * - Photo type filtering
 * - Sorting by takenAt
 * - Edge cases (empty results, null fields)
 *
 * Uses in-memory database for fast, isolated tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27, 35])
class PhotoDaoTest {

    private lateinit var database: MineraLogDatabase
    private lateinit var photoDao: PhotoDao
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

        photoDao = database.photoDao()
        mineralDao = database.mineralDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== CREATE Tests ==========

    @Test
    fun insert_singlePhoto_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photo = createTestPhoto(mineralId = mineral.id, fileName = "media/${mineral.id}/photo1.jpg")

        // When
        photoDao.insert(photo)

        // Then
        val retrieved = photoDao.getById(photo.id)
        assertNotNull(retrieved)
        assertEquals(photo.fileName, retrieved!!.fileName)
        assertEquals(mineral.id, retrieved!!.mineralId)
    }

    @Test
    fun insertAll_multiplePhotos_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photos = listOf(
            createTestPhoto(mineralId = mineral.id, fileName = "media/${mineral.id}/photo1.jpg"),
            createTestPhoto(mineralId = mineral.id, fileName = "media/${mineral.id}/photo2.jpg"),
            createTestPhoto(mineralId = mineral.id, fileName = "media/${mineral.id}/photo3.jpg")
        )

        // When
        photoDao.insertAll(photos)

        // Then
        val all = photoDao.getByMineralId(mineral.id)
        assertEquals(3, all.size)
    }

    @Test
    fun insert_withReplace_updatesExisting() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photo = createTestPhoto(
            mineralId = mineral.id,
            fileName = "media/${mineral.id}/photo1.jpg",
            caption = "Original"
        )
        photoDao.insert(photo)

        // When - insert with same ID but different caption (REPLACE strategy)
        val updatedPhoto = photo.copy(caption = "Updated")
        photoDao.insert(updatedPhoto)

        // Then
        val retrieved = photoDao.getById(photo.id)
        assertNotNull(retrieved)
        assertEquals("Updated", retrieved!!.caption)
    }

    // ========== READ Tests ==========

    @Test
    fun getById_existingPhoto_returnsPhoto() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photo = createTestPhoto(mineralId = mineral.id, fileName = "media/${mineral.id}/photo1.jpg")
        photoDao.insert(photo)

        // When
        val result = photoDao.getById(photo.id)

        // Then
        assertNotNull(result)
        assertEquals(photo.id, result!!.id)
        assertEquals(photo.fileName, result!!.fileName)
    }

    @Test
    fun getById_nonExistingPhoto_returnsNull() = runTest {
        // When
        val result = photoDao.getById("non-existing-id")

        // Then
        assertNull(result)
    }

    @Test
    fun getByMineralId_returnsPhotosInDescendingOrder() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val now = Instant.now()
        val photos = listOf(
            createTestPhoto(mineralId = mineral.id, fileName = "photo1.jpg", takenAt = now.minusSeconds(300)),
            createTestPhoto(mineralId = mineral.id, fileName = "photo2.jpg", takenAt = now.minusSeconds(200)),
            createTestPhoto(mineralId = mineral.id, fileName = "photo3.jpg", takenAt = now.minusSeconds(100))
        )
        photoDao.insertAll(photos)

        // When
        val result = photoDao.getByMineralId(mineral.id)

        // Then
        assertEquals(3, result!!.size)
        // Should be sorted DESC by takenAt (newest first)
        assertEquals("photo3.jpg", result[0].fileName)
        assertEquals("photo2.jpg", result[1].fileName)
        assertEquals("photo1.jpg", result[2].fileName)
    }

    @Test
    fun getByMineralIds_returnsPhotosForMultipleMinerals() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        mineralDao.insertAll(listOf(mineral1, mineral2))

        val photos = listOf(
            createTestPhoto(mineralId = mineral1.id, fileName = "photo1.jpg"),
            createTestPhoto(mineralId = mineral1.id, fileName = "photo2.jpg"),
            createTestPhoto(mineralId = mineral2.id, fileName = "photo3.jpg")
        )
        photoDao.insertAll(photos)

        // When
        val result = photoDao.getByMineralIds(listOf(mineral1.id, mineral2.id))

        // Then
        assertEquals(3, result!!.size)
        assertTrue(result!!.any { it.mineralId == mineral1.id })
        assertTrue(result!!.any { it.mineralId == mineral2.id })
    }

    @Test
    fun getByMineralIdFlow_emitsUpdates() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photo = createTestPhoto(mineralId = mineral.id, fileName = "photo1.jpg")

        // When
        val flow = photoDao.getByMineralIdFlow(mineral.id)
        val initial = flow.first()
        assertEquals(0, initial.size)

        photoDao.insert(photo)
        val afterInsert = flow.first()

        // Then
        assertEquals(1, afterInsert.size)
        assertEquals(photo.id, afterInsert[0].id)
    }

    @Test
    fun getByMineralIdAndTypeFlow_filtersByType() = runTest {
        // Given
        val mineral = createTestMineral(name = "Fluorite")
        mineralDao.insert(mineral)
        val photos = listOf(
            createTestPhoto(mineralId = mineral.id, fileName = "normal.jpg", type = PhotoType.NORMAL),
            createTestPhoto(mineralId = mineral.id, fileName = "uv_sw.jpg", type = PhotoType.UV_SW),
            createTestPhoto(mineralId = mineral.id, fileName = "uv_lw.jpg", type = PhotoType.UV_LW),
            createTestPhoto(mineralId = mineral.id, fileName = "macro.jpg", type = PhotoType.MACRO)
        )
        photoDao.insertAll(photos)

        // When
        val uvSwPhotos = photoDao.getByMineralIdAndTypeFlow(mineral.id, PhotoType.UV_SW).first()

        // Then
        assertEquals(1, uvSwPhotos.size)
        assertEquals("uv_sw.jpg", uvSwPhotos[0].fileName)
        assertEquals(PhotoType.UV_SW, uvSwPhotos[0].type)
    }

    @Test
    fun getAll_returnsAllPhotos() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        mineralDao.insertAll(listOf(mineral1, mineral2))

        val photos = listOf(
            createTestPhoto(mineralId = mineral1.id, fileName = "photo1.jpg"),
            createTestPhoto(mineralId = mineral2.id, fileName = "photo2.jpg")
        )
        photoDao.insertAll(photos)

        // When
        val result = photoDao.getAll()

        // Then
        assertEquals(2, result!!.size)
    }

    @Test
    fun getCountByMineralId_returnsCorrectCount() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photos = listOf(
            createTestPhoto(mineralId = mineral.id, fileName = "photo1.jpg"),
            createTestPhoto(mineralId = mineral.id, fileName = "photo2.jpg"),
            createTestPhoto(mineralId = mineral.id, fileName = "photo3.jpg")
        )
        photoDao.insertAll(photos)

        // When
        val count = photoDao.getCountByMineralId(mineral.id)

        // Then
        assertEquals(3, count)
    }

    @Test
    fun getCountByMineralIdFlow_emitsUpdates() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val flow = photoDao.getCountByMineralIdFlow(mineral.id)

        // When
        val initialCount = flow.first()
        assertEquals(0, initialCount)

        photoDao.insert(createTestPhoto(mineralId = mineral.id, fileName = "photo1.jpg"))
        val afterInsert = flow.first()

        // Then
        assertEquals(1, afterInsert)
    }

    @Test
    fun getFirstPhotoByMineralId_returnsOldestPhoto() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val now = Instant.now()
        val photos = listOf(
            createTestPhoto(mineralId = mineral.id, fileName = "oldest.jpg", takenAt = now.minusSeconds(300)),
            createTestPhoto(mineralId = mineral.id, fileName = "middle.jpg", takenAt = now.minusSeconds(200)),
            createTestPhoto(mineralId = mineral.id, fileName = "newest.jpg", takenAt = now.minusSeconds(100))
        )
        photoDao.insertAll(photos)

        // When
        val result = photoDao.getFirstPhotoByMineralId(mineral.id)

        // Then
        assertNotNull(result)
        assertEquals("oldest.jpg", result!!.fileName)
    }

    // ========== UPDATE Tests ==========

    @Test
    fun update_existingPhoto_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photo = createTestPhoto(
            mineralId = mineral.id,
            fileName = "photo1.jpg",
            caption = "Original caption"
        )
        photoDao.insert(photo)

        // When
        val updated = photo.copy(caption = "Updated caption")
        photoDao.update(updated)

        // Then
        val result = photoDao.getById(photo.id)
        assertNotNull(result)
        assertEquals("Updated caption", result!!.caption)
    }

    // ========== DELETE Tests ==========

    @Test
    fun delete_existingPhoto_removesPhoto() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photo = createTestPhoto(mineralId = mineral.id, fileName = "photo1.jpg")
        photoDao.insert(photo)

        // When
        photoDao.delete(photo)

        // Then
        val result = photoDao.getById(photo.id)
        assertNull(result)
    }

    @Test
    fun deleteById_existingPhoto_removesPhoto() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photo = createTestPhoto(mineralId = mineral.id, fileName = "photo1.jpg")
        photoDao.insert(photo)

        // When
        photoDao.deleteById(photo.id)

        // Then
        val result = photoDao.getById(photo.id)
        assertNull(result)
    }

    @Test
    fun deleteByMineralId_removesAllPhotosForMineral() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photos = listOf(
            createTestPhoto(mineralId = mineral.id, fileName = "photo1.jpg"),
            createTestPhoto(mineralId = mineral.id, fileName = "photo2.jpg"),
            createTestPhoto(mineralId = mineral.id, fileName = "photo3.jpg")
        )
        photoDao.insertAll(photos)

        // When
        photoDao.deleteByMineralId(mineral.id)

        // Then
        val result = photoDao.getByMineralId(mineral.id)
        assertEquals(0, result!!.size)
    }

    @Test
    fun deleteByMineralIds_removesPhotosForMultipleMinerals() = runTest {
        // Given
        val mineral1 = createTestMineral(name = "Quartz")
        val mineral2 = createTestMineral(name = "Calcite")
        val mineral3 = createTestMineral(name = "Fluorite")
        mineralDao.insertAll(listOf(mineral1, mineral2, mineral3))

        val photos = listOf(
            createTestPhoto(mineralId = mineral1.id, fileName = "photo1.jpg"),
            createTestPhoto(mineralId = mineral2.id, fileName = "photo2.jpg"),
            createTestPhoto(mineralId = mineral3.id, fileName = "photo3.jpg")
        )
        photoDao.insertAll(photos)

        // When
        photoDao.deleteByMineralIds(listOf(mineral1.id, mineral2.id))

        // Then
        val remaining = photoDao.getAll()
        assertEquals(1, remaining.size)
        assertEquals(mineral3.id, remaining[0].mineralId)
    }

    @Test
    fun deleteAll_removesAllPhotos() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photos = listOf(
            createTestPhoto(mineralId = mineral.id, fileName = "photo1.jpg"),
            createTestPhoto(mineralId = mineral.id, fileName = "photo2.jpg")
        )
        photoDao.insertAll(photos)

        // When
        photoDao.deleteAll()

        // Then
        val result = photoDao.getAll()
        assertEquals(0, result!!.size)
    }

    // ========== CASCADE DELETE Tests ==========

    @Test
    fun cascadeDelete_deletingMineral_deletesPhotos() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photos = listOf(
            createTestPhoto(mineralId = mineral.id, fileName = "photo1.jpg"),
            createTestPhoto(mineralId = mineral.id, fileName = "photo2.jpg")
        )
        photoDao.insertAll(photos)

        // When - Delete the mineral (should cascade to photos)
        mineralDao.delete(mineral)

        // Then
        val remainingPhotos = photoDao.getByMineralId(mineral.id)
        assertEquals(0, remainingPhotos.size)
    }

    // ========== EDGE CASES ==========

    @Test
    fun getByMineralId_emptyResult_returnsEmptyList() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)

        // When - No photos inserted
        val result = photoDao.getByMineralId(mineral.id)

        // Then
        assertEquals(0, result!!.size)
    }

    @Test
    fun insert_nullCaption_success() = runTest {
        // Given
        val mineral = createTestMineral(name = "Quartz")
        mineralDao.insert(mineral)
        val photo = createTestPhoto(
            mineralId = mineral.id,
            fileName = "photo1.jpg",
            caption = null
        )

        // When
        photoDao.insert(photo)

        // Then
        val result = photoDao.getById(photo.id)
        assertNotNull(result)
        assertNull(result!!.caption)
    }

    @Test
    fun photoTypes_allTypesSupported() = runTest {
        // Given
        val mineral = createTestMineral(name = "Fluorite")
        mineralDao.insert(mineral)
        val photos = listOf(
            createTestPhoto(mineralId = mineral.id, fileName = "normal.jpg", type = PhotoType.NORMAL),
            createTestPhoto(mineralId = mineral.id, fileName = "uv_sw.jpg", type = PhotoType.UV_SW),
            createTestPhoto(mineralId = mineral.id, fileName = "uv_lw.jpg", type = PhotoType.UV_LW),
            createTestPhoto(mineralId = mineral.id, fileName = "macro.jpg", type = PhotoType.MACRO)
        )

        // When
        photoDao.insertAll(photos)

        // Then
        val result = photoDao.getByMineralId(mineral.id)
        assertEquals(4, result!!.size)
        assertEquals(setOf(PhotoType.NORMAL, PhotoType.UV_SW, PhotoType.UV_LW, PhotoType.MACRO),
                     result!!.map { it.type }.toSet())
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

    private fun createTestPhoto(
        id: String = java.util.UUID.randomUUID().toString(),
        mineralId: String,
        fileName: String,
        type: PhotoType = PhotoType.NORMAL,
        caption: String? = null,
        takenAt: Instant = Instant.now()
    ): PhotoEntity {
        return PhotoEntity(
            id = id,
            mineralId = mineralId,
            type = type,
            caption = caption,
            takenAt = takenAt,
            fileName = fileName
        )
    }
}
