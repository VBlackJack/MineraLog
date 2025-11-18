package net.meshcore.mineralog.ui.screens.gallery

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.domain.model.Photo
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

/**
 * Unit tests for PhotoGalleryViewModel.
 *
 * Tests cover:
 * - Photo list loading from repository
 * - Photo flow reactivity (updates on changes)
 * - Delete photo operation
 * - Empty photo list handling
 *
 * Sprint 3: ViewModel Tests - Target 70%+ coverage
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PhotoGalleryViewModelTest {

    private lateinit var mineralRepository: MineralRepository
    private lateinit var viewModel: PhotoGalleryViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val mineralId = "test-mineral-id"

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mineralRepository = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== PHOTO LOADING TESTS ==========

    @Test
    @DisplayName("init - loads photos from repository")
    fun `init - photos flow - loaded from repository`() = runTest {
        // Arrange
        val photos = listOf(
            createTestPhoto("photo-1", "photo1.jpg"),
            createTestPhoto("photo-2", "photo2.jpg"),
            createTestPhoto("photo-3", "photo3.jpg")
        )
        every { mineralRepository.getPhotosFlow(mineralId) } returns flowOf(photos)

        // Act
        viewModel = PhotoGalleryViewModel(mineralId, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.photos.test {
            val loadedPhotos = awaitItem()
            assertEquals(3, loadedPhotos.size)
            assertEquals("photo1.jpg", loadedPhotos[0].filename)
            assertEquals("photo2.jpg", loadedPhotos[1].filename)
            assertEquals("photo3.jpg", loadedPhotos[2].filename)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - empty photo list - returns empty list")
    fun `init - no photos - empty list`() = runTest {
        // Arrange
        every { mineralRepository.getPhotosFlow(mineralId) } returns flowOf(emptyList())

        // Act
        viewModel = PhotoGalleryViewModel(mineralId, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.photos.test {
            val loadedPhotos = awaitItem()
            assertTrue(loadedPhotos.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - single photo - loads correctly")
    fun `init - single photo - loaded successfully`() = runTest {
        // Arrange
        val photo = createTestPhoto("photo-1", "photo1.jpg")
        every { mineralRepository.getPhotosFlow(mineralId) } returns flowOf(listOf(photo))

        // Act
        viewModel = PhotoGalleryViewModel(mineralId, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.photos.test {
            val loadedPhotos = awaitItem()
            assertEquals(1, loadedPhotos.size)
            assertEquals("photo1.jpg", loadedPhotos[0].filename)
            assertEquals("photo-1", loadedPhotos[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("photos - flow updates - emits new values")
    fun `photos - flow updates - when photos change`() = runTest {
        // Arrange
        val photosFlow = MutableStateFlow(
            listOf(createTestPhoto("photo-1", "photo1.jpg"))
        )
        every { mineralRepository.getPhotosFlow(mineralId) } returns photosFlow

        viewModel = PhotoGalleryViewModel(mineralId, mineralRepository)
        advanceUntilIdle()

        // Act & Assert
        viewModel.photos.test {
            // Initial photo
            val initial = awaitItem()
            assertEquals(1, initial.size)
            assertEquals("photo1.jpg", initial[0].filename)

            // Add new photo
            photosFlow.value = listOf(
                createTestPhoto("photo-1", "photo1.jpg"),
                createTestPhoto("photo-2", "photo2.jpg")
            )

            val updated = awaitItem()
            assertEquals(2, updated.size)
            assertEquals("photo2.jpg", updated[1].filename)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("photos - primary photo - correctly identified")
    fun `photos - primary photo flag - set correctly`() = runTest {
        // Arrange
        val photos = listOf(
            createTestPhoto("photo-1", "photo1.jpg", isPrimary = false),
            createTestPhoto("photo-2", "photo2.jpg", isPrimary = true),
            createTestPhoto("photo-3", "photo3.jpg", isPrimary = false)
        )
        every { mineralRepository.getPhotosFlow(mineralId) } returns flowOf(photos)

        // Act
        viewModel = PhotoGalleryViewModel(mineralId, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.photos.test {
            val loadedPhotos = awaitItem()
            assertEquals(3, loadedPhotos.size)
            assertFalse(loadedPhotos[0].isPrimary)
            assertTrue(loadedPhotos[1].isPrimary)
            assertFalse(loadedPhotos[2].isPrimary)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== DELETE PHOTO TESTS ==========

    @Test
    @DisplayName("deletePhoto - calls repository with correct ID")
    fun `deletePhoto - repository call - with photo ID`() = runTest {
        // Arrange
        every { mineralRepository.getPhotosFlow(mineralId) } returns flowOf(emptyList())
        coEvery { mineralRepository.deletePhoto(any()) } just Runs

        viewModel = PhotoGalleryViewModel(mineralId, mineralRepository)
        advanceUntilIdle()

        val photoId = "photo-to-delete"

        // Act
        viewModel.deletePhoto(photoId)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mineralRepository.deletePhoto(photoId) }
    }

    @Test
    @DisplayName("deletePhoto - multiple calls - each processed correctly")
    fun `deletePhoto - multiple deletions - all calls made`() = runTest {
        // Arrange
        every { mineralRepository.getPhotosFlow(mineralId) } returns flowOf(emptyList())
        coEvery { mineralRepository.deletePhoto(any()) } just Runs

        viewModel = PhotoGalleryViewModel(mineralId, mineralRepository)
        advanceUntilIdle()

        // Act
        viewModel.deletePhoto("photo-1")
        viewModel.deletePhoto("photo-2")
        viewModel.deletePhoto("photo-3")
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { mineralRepository.deletePhoto("photo-1") }
        coVerify(exactly = 1) { mineralRepository.deletePhoto("photo-2") }
        coVerify(exactly = 1) { mineralRepository.deletePhoto("photo-3") }
    }

    @Test
    @DisplayName("deletePhoto - repository exception - does not crash")
    fun `deletePhoto - repository error - handled gracefully`() = runTest {
        // Arrange
        every { mineralRepository.getPhotosFlow(mineralId) } returns flowOf(emptyList())
        coEvery { mineralRepository.deletePhoto(any()) } throws Exception("Delete failed")

        viewModel = PhotoGalleryViewModel(mineralId, mineralRepository)
        advanceUntilIdle()

        // Act & Assert - Should not throw
        assertDoesNotThrow {
            viewModel.deletePhoto("photo-1")
            advanceUntilIdle()
        }

        coVerify(exactly = 1) { mineralRepository.deletePhoto("photo-1") }
    }

    @Test
    @DisplayName("deletePhoto - flow updates after deletion")
    fun `deletePhoto - flow updates - reflects deletion`() = runTest {
        // Arrange
        val photosFlow = MutableStateFlow(
            listOf(
                createTestPhoto("photo-1", "photo1.jpg"),
                createTestPhoto("photo-2", "photo2.jpg")
            )
        )
        every { mineralRepository.getPhotosFlow(mineralId) } returns photosFlow
        coEvery { mineralRepository.deletePhoto(any()) } answers {
            // Simulate deletion by updating the flow
            photosFlow.value = photosFlow.value.filter { it.id != firstArg<String>() }
        }

        viewModel = PhotoGalleryViewModel(mineralId, mineralRepository)
        advanceUntilIdle()

        // Act & Assert
        viewModel.photos.test {
            // Initial 2 photos
            val initial = awaitItem()
            assertEquals(2, initial.size)

            // Delete first photo
            viewModel.deletePhoto("photo-1")
            advanceUntilIdle()

            // Should have 1 photo remaining
            val afterDelete = awaitItem()
            assertEquals(1, afterDelete.size)
            assertEquals("photo-2", afterDelete[0].id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== HELPER METHODS ==========

    private fun createTestPhoto(
        id: String,
        filename: String,
        isPrimary: Boolean = false
    ) = Photo(
        id = id,
        mineralId = mineralId,
        filename = filename,
        filepath = "/path/to/$filename",
        isPrimary = isPrimary,
        caption = null,
        takenAt = Instant.now(),
        createdAt = Instant.now()
    )
}
