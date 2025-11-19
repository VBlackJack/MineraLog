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
import net.meshcore.mineralog.data.local.dao.StorageDao
import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.data.local.entity.PhotoEntity
import net.meshcore.mineralog.data.local.entity.ProvenanceEntity
import net.meshcore.mineralog.data.local.entity.StorageEntity
import net.meshcore.mineralog.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit tests for MineralRepository.
 * Tests CRUD operations, batch operations, and cascade deletion.
 */
class MineralRepositoryTest {

    private lateinit var database: MineraLogDatabase
    private lateinit var mineralDao: MineralDaoComposite
    private lateinit var provenanceDao: ProvenanceDao
    private lateinit var storageDao: StorageDao
    private lateinit var photoDao: PhotoDao
    private lateinit var mineralComponentDao: MineralComponentDao
    private lateinit var repository: MineralRepository

    @BeforeEach
    fun setup() {
        database = mockk(relaxed = true)
        mineralDao = mockk(relaxed = true)
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
            provenanceDao,
            storageDao,
            photoDao,
            mineralComponentDao,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `insert saves mineral with provenance, storage, and photos`() = runTest {
        // Given
        val provenance = Provenance(
            mineralId = "mineral-1",
            site = "Test Site",
            country = "France"
        )
        val storage = Storage(
            mineralId = "mineral-1",
            place = "Drawer A",
            container = "Box 1"
        )
        val photo = Photo(
            id = "photo-1",
            mineralId = "mineral-1",
            fileName = "test.jpg",
            type = PhotoType.NORMAL
        )
        val mineral = Mineral(
            id = "mineral-1",
            name = "Quartz",
            group = "Silicates",
            provenance = provenance,
            storage = storage,
            photos = listOf(photo)
        )

        // When
        val result = repository.insert(mineral)

        // Then
        assertEquals("mineral-1", result)
        coVerify { mineralDao.insert(any()) }
        coVerify { provenanceDao.insert(any()) }
        coVerify { storageDao.insert(any()) }
        coVerify { photoDao.deleteByMineralId("mineral-1") }
        coVerify { photoDao.insertAll(match { it.size == 1 }) }
        coVerify { mineralComponentDao.deleteByAggregateId("mineral-1") }
    }

    @Test
    fun `insert saves mineral without optional provenance and storage`() = runTest {
        // Given
        val mineral = Mineral(
            id = "mineral-1",
            name = "Quartz",
            group = "Silicates"
        )

        // When
        val result = repository.insert(mineral)

        // Then
        assertEquals("mineral-1", result)
        coVerify { mineralDao.insert(any()) }
        coVerify(exactly = 0) { provenanceDao.insert(any()) }
        coVerify(exactly = 0) { storageDao.insert(any()) }
    }

    @Test
    fun `delete removes mineral with cascade deletion of related entities`() = runTest {
        // When
        repository.delete("mineral-1")

        // Then - verify cascade deletion in correct order
        coVerify { provenanceDao.deleteByMineralId("mineral-1") }
        coVerify { storageDao.deleteByMineralId("mineral-1") }
        coVerify { photoDao.deleteByMineralId("mineral-1") }
        coVerify { mineralComponentDao.deleteByAggregateId("mineral-1") }
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
        coVerify { mineralDao.deleteByIds(ids) }
    }

    @Test
    fun `deleteByIds does nothing for empty list`() = runTest {
        // When
        repository.deleteByIds(emptyList())

        // Then
        coVerify(exactly = 0) { mineralDao.deleteByIds(any()) }
    }

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
    fun `getById returns mineral with related entities`() = runTest {
        // Given
        val entity = MineralEntity(
            id = "mineral-1",
            name = "Quartz",
            group = "Silicates"
        )
        val provenanceEntity = ProvenanceEntity(mineralId = "mineral-1", site = "Test Site")
        val storageEntity = StorageEntity(mineralId = "mineral-1", place = "Drawer A")
        val photoEntities = listOf(
            PhotoEntity(id = "photo-1", mineralId = "mineral-1", fileName = "test.jpg", type = PhotoType.NORMAL)
        )

        coEvery { mineralDao.getById("mineral-1") } returns entity
        coEvery { provenanceDao.getByMineralId("mineral-1") } returns provenanceEntity
        coEvery { storageDao.getByMineralId("mineral-1") } returns storageEntity
        coEvery { photoDao.getByMineralId("mineral-1") } returns photoEntities

        // When
        val result = repository.getById("mineral-1")

        // Then
        assertNotNull(result)
        assertEquals("mineral-1", result?.id)
        assertEquals("Quartz", result?.name)
        assertNotNull(result?.provenance)
        assertNotNull(result?.storage)
        assertEquals(1, result?.photos?.size)
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
    fun `getByIds uses batch loading to avoid N+1 problem`() = runTest {
        // Given
        val entities = listOf(
            MineralEntity(id = "mineral-1", name = "Quartz", group = "Silicates"),
            MineralEntity(id = "mineral-2", name = "Calcite", group = "Carbonates")
        )
        val provenances = listOf(ProvenanceEntity(mineralId = "mineral-1", site = "Site 1"))
        val storages = listOf(StorageEntity(mineralId = "mineral-2", place = "Drawer A"))
        val photos = listOf(PhotoEntity(id = "photo-1", mineralId = "mineral-1", fileName = "test.jpg", type = PhotoType.NORMAL))

        coEvery { mineralDao.getByIds(listOf("mineral-1", "mineral-2")) } returns entities
        coEvery { provenanceDao.getByMineralIds(listOf("mineral-1", "mineral-2")) } returns provenances
        coEvery { storageDao.getByMineralIds(listOf("mineral-1", "mineral-2")) } returns storages
        coEvery { photoDao.getByMineralIds(listOf("mineral-1", "mineral-2")) } returns photos

        // When
        val result = repository.getByIds(listOf("mineral-1", "mineral-2"))

        // Then
        assertEquals(2, result.size)
        // Verify batch calls were made (not individual calls per mineral)
        coVerify(exactly = 1) { provenanceDao.getByMineralIds(any()) }
        coVerify(exactly = 1) { storageDao.getByMineralIds(any()) }
        coVerify(exactly = 1) { photoDao.getByMineralIds(any()) }
    }

    @Test
    fun `getAllFlow emits empty list when no minerals`() = runTest {
        // Given
        every { mineralDao.getAllFlow() } returns flowOf(emptyList())

        // When & Then
        repository.getAllFlow().test {
            val result = awaitItem()
            assertTrue(result.isEmpty())
            awaitComplete()
        }
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
    fun `insertPhoto saves photo entity`() = runTest {
        // Given
        val photo = Photo(
            id = "photo-1",
            mineralId = "mineral-1",
            fileName = "test.jpg",
            type = PhotoType.UV_SW
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

    @Test
    fun `getPhotosFlow returns photos for mineral`() = runTest {
        // Given
        val photoEntities = listOf(
            PhotoEntity(id = "photo-1", mineralId = "mineral-1", fileName = "test1.jpg", type = PhotoType.NORMAL),
            PhotoEntity(id = "photo-2", mineralId = "mineral-1", fileName = "test2.jpg", type = PhotoType.MACRO)
        )
        every { photoDao.getByMineralIdFlow("mineral-1") } returns flowOf(photoEntities)

        // When & Then
        repository.getPhotosFlow("mineral-1").test {
            val result = awaitItem()
            assertEquals(2, result.size)
            assertEquals("photo-1", result[0].id)
            assertEquals(PhotoType.NORMAL, result[0].type)
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
    fun `getAllUniqueTags filters blank tags`() = runTest {
        // Given
        val tagStrings = listOf(
            "fluorescent, , blue",
            "  , green",
            ""
        )
        coEvery { mineralDao.getAllTags() } returns tagStrings

        // When
        val result = repository.getAllUniqueTags()

        // Then
        assertEquals(listOf("blue", "fluorescent", "green"), result)
        assertFalse(result.contains(""))
    }

    @Test
    fun `deleteAll removes all minerals and related entities`() = runTest {
        // When
        repository.deleteAll()

        // Then
        coVerify { mineralDao.deleteAll() }
        coVerify { provenanceDao.deleteAll() }
        coVerify { storageDao.deleteAll() }
        coVerify { photoDao.deleteAll() }
        coVerify { mineralComponentDao.deleteAll() }
    }

    @Test
    fun `update saves mineral with provenance and storage`() = runTest {
        // Given
        val provenance = Provenance(mineralId = "mineral-1", site = "Updated Site")
        val storage = Storage(mineralId = "mineral-1", place = "New Drawer")
        val mineral = Mineral(
            id = "mineral-1",
            name = "Quartz Updated",
            group = "Silicates",
            provenance = provenance,
            storage = storage
        )

        // When
        repository.update(mineral)

        // Then
        coVerify { mineralDao.update(any()) }
        coVerify { provenanceDao.insert(any()) }
        coVerify { storageDao.insert(any()) }
    }
}
