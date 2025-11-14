package net.meshcore.mineralog.ui.screens.edit

import android.net.Uri
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.Photo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("EditMineralViewModel Tests")
class EditMineralViewModelTest {

    private lateinit var mineralRepository: MineralRepository
    private lateinit var viewModel: EditMineralViewModel
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testMineralId = UUID.randomUUID().toString()
    private lateinit var testMineral: Mineral

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mineralRepository = mockk(relaxed = true)

        testMineral = Mineral(
            id = testMineralId,
            name = "Quartz",
            group = "Silicates",
            formula = "SiO2",
            notes = "Test notes",
            diaphaneity = "Transparent",
            cleavage = "None",
            fracture = "Conchoidal",
            luster = "Vitreous",
            streak = "White",
            habit = "Prismatic",
            crystalSystem = "Hexagonal",
            tags = listOf("common", "mineral"),
            status = "complete",
            statusType = "in_collection",
            statusDetails = null,
            qualityRating = 4,
            completeness = 100,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            provenance = null,
            storage = null
        )

        // Setup default repository responses
        every { mineralRepository.getByIdFlow(testMineralId) } returns flowOf(testMineral)
        coEvery { mineralRepository.getAllUniqueTags() } returns listOf("common", "rare", "mineral", "crystal")
        coEvery { mineralRepository.update(any()) } returns Unit
        coEvery { mineralRepository.insertPhoto(any()) } returns Unit
        coEvery { mineralRepository.deletePhoto(any()) } returns Unit

        viewModel = EditMineralViewModel(testMineralId, mineralRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Initial state tests
    @Test
    fun `viewModel should load mineral data on init`() = runTest {
        // Then
        viewModel.name.test {
            assertEquals("Quartz", awaitItem())
        }
        viewModel.group.test {
            assertEquals("Silicates", awaitItem())
        }
        viewModel.formula.test {
            assertEquals("SiO2", awaitItem())
        }
    }

    @Test
    fun `viewModel should load available tags on init`() = runTest {
        // Then
        viewModel.availableTags.test {
            val tags = awaitItem()
            assertTrue(tags.contains("common"))
            assertTrue(tags.contains("mineral"))
            assertEquals(4, tags.size)
        }
    }

    @Test
    fun `viewModel with nonexistent mineral should emit error`() = runTest {
        // Given
        val invalidId = UUID.randomUUID().toString()
        every { mineralRepository.getByIdFlow(invalidId) } returns flowOf(null)

        // When
        val errorViewModel = EditMineralViewModel(invalidId, mineralRepository)

        // Then
        errorViewModel.updateState.test {
            val state = awaitItem()
            assertTrue(state is UpdateMineralState.Error)
            assertEquals("Mineral not found", (state as UpdateMineralState.Error).message)
        }
    }

    // Field update tests
    @Test
    fun `onNameChange should update name state`() = runTest {
        // When
        viewModel.onNameChange("Amethyst")

        // Then
        viewModel.name.test {
            assertEquals("Amethyst", awaitItem())
        }
    }

    @Test
    fun `onGroupChange should update group state`() = runTest {
        // When
        viewModel.onGroupChange("Oxides")

        // Then
        viewModel.group.test {
            assertEquals("Oxides", awaitItem())
        }
    }

    @Test
    fun `onFormulaChange should update formula state`() = runTest {
        // When
        viewModel.onFormulaChange("Al2O3")

        // Then
        viewModel.formula.test {
            assertEquals("Al2O3", awaitItem())
        }
    }

    @Test
    fun `onNotesChange should update notes state`() = runTest {
        // When
        viewModel.onNotesChange("Updated notes")

        // Then
        viewModel.notes.test {
            assertEquals("Updated notes", awaitItem())
        }
    }

    @Test
    fun `onTagsChange should update tags state`() = runTest {
        // When
        viewModel.onTagsChange("rare, valuable, crystal")

        // Then
        viewModel.tags.test {
            assertEquals("rare, valuable, crystal", awaitItem())
        }
    }

    // Photo management tests
    @Test
    fun `addPhoto should add photo to list`() = runTest {
        // Given
        val uri = mockk<Uri>()

        // When
        viewModel.addPhoto(uri, "SPECIMEN", "Test caption")

        // Then
        viewModel.photos.test {
            val photos = awaitItem()
            assertEquals(1, photos.size)
            assertEquals("SPECIMEN", photos[0].type)
            assertEquals("Test caption", photos[0].caption)
            assertEquals(uri, photos[0].uri)
            assertFalse(photos[0].isExisting)
        }
    }

    @Test
    fun `removePhoto should remove photo from list`() = runTest {
        // Given
        val uri = mockk<Uri>()
        viewModel.addPhoto(uri, "NORMAL", null)

        viewModel.photos.test {
            val photosWithAdded = awaitItem()
            assertEquals(1, photosWithAdded.size)
            val photoId = photosWithAdded[0].id

            // When
            viewModel.removePhoto(photoId)

            // Then
            val photosAfterRemove = awaitItem()
            assertEquals(0, photosAfterRemove.size)
        }
    }

    @Test
    fun `updatePhotoCaption should update caption`() = runTest {
        // Given
        val uri = mockk<Uri>()
        viewModel.addPhoto(uri, "NORMAL", "Original caption")

        viewModel.photos.test {
            val photos = awaitItem()
            val photoId = photos[0].id

            // When
            viewModel.updatePhotoCaption(photoId, "Updated caption")

            // Then
            val updatedPhotos = awaitItem()
            assertEquals("Updated caption", updatedPhotos[0].caption)
        }
    }

    @Test
    fun `updatePhotoType should update type`() = runTest {
        // Given
        val uri = mockk<Uri>()
        viewModel.addPhoto(uri, "NORMAL", null)

        viewModel.photos.test {
            val photos = awaitItem()
            val photoId = photos[0].id

            // When
            viewModel.updatePhotoType(photoId, "UV_LIGHT")

            // Then
            val updatedPhotos = awaitItem()
            assertEquals("UV_LIGHT", updatedPhotos[0].type)
        }
    }

    // Validation tests
    @Test
    fun `updateMineral with empty name should emit error`() = runTest {
        // Given
        viewModel.onNameChange("")
        val photosDir = mockk<File>()

        // When
        viewModel.updateMineral({}, photosDir)

        // Then
        viewModel.updateState.test {
            val state = awaitItem()
            assertTrue(state is UpdateMineralState.Error)
            assertEquals("Mineral name is required", (state as UpdateMineralState.Error).message)
        }
    }

    @Test
    fun `updateMineral with blank name should emit error`() = runTest {
        // Given
        viewModel.onNameChange("   ")
        val photosDir = mockk<File>()

        // When
        viewModel.updateMineral({}, photosDir)

        // Then
        viewModel.updateState.test {
            val state = awaitItem()
            assertTrue(state is UpdateMineralState.Error)
            assertEquals("Mineral name is required", (state as UpdateMineralState.Error).message)
        }
    }

    @Test
    fun `updateMineral with name less than 2 characters should emit error`() = runTest {
        // Given
        viewModel.onNameChange("Q")
        val photosDir = mockk<File>()

        // When
        viewModel.updateMineral({}, photosDir)

        // Then
        viewModel.updateState.test {
            val state = awaitItem()
            assertTrue(state is UpdateMineralState.Error)
            assertEquals("Mineral name must be at least 2 characters", (state as UpdateMineralState.Error).message)
        }
    }

    @Test
    fun `updateMineral with valid data should succeed`() = runTest {
        // Given
        viewModel.onNameChange("Valid Mineral Name")
        val photosDir = mockk<File>()
        var callbackCalled = false
        val onSuccess: (String) -> Unit = { callbackCalled = true }

        // When
        viewModel.updateMineral(onSuccess, photosDir)

        // Then
        assertTrue(callbackCalled)
        viewModel.updateState.test {
            val state = awaitItem()
            assertTrue(state is UpdateMineralState.Success)
        }
        coVerify { mineralRepository.update(any()) }
    }

    @Test
    fun `updateMineral should trim whitespace from name`() = runTest {
        // Given
        viewModel.onNameChange("  Quartz  ")
        val photosDir = mockk<File>()
        val mineralSlot = slot<Mineral>()
        coEvery { mineralRepository.update(capture(mineralSlot)) } returns Unit

        // When
        viewModel.updateMineral({}, photosDir)

        // Then
        assertEquals("Quartz", mineralSlot.captured.name)
    }

    @Test
    fun `updateMineral should parse tags correctly`() = runTest {
        // Given
        viewModel.onTagsChange("  rare  , valuable,  crystal  ")
        val photosDir = mockk<File>()
        val mineralSlot = slot<Mineral>()
        coEvery { mineralRepository.update(capture(mineralSlot)) } returns Unit

        // When
        viewModel.updateMineral({}, photosDir)

        // Then
        val tags = mineralSlot.captured.tags
        assertEquals(3, tags.size)
        assertTrue(tags.contains("rare"))
        assertTrue(tags.contains("valuable"))
        assertTrue(tags.contains("crystal"))
    }

    @Test
    fun `updateMineral should filter empty tags`() = runTest {
        // Given
        viewModel.onTagsChange("rare,  ,valuable, , crystal,")
        val photosDir = mockk<File>()
        val mineralSlot = slot<Mineral>()
        coEvery { mineralRepository.update(capture(mineralSlot)) } returns Unit

        // When
        viewModel.updateMineral({}, photosDir)

        // Then
        val tags = mineralSlot.captured.tags
        assertEquals(3, tags.size)
        assertFalse(tags.any { it.isBlank() })
    }

    @Test
    fun `updateMineral should insert new photos`() = runTest {
        // Given
        val uri = mockk<Uri>()
        every { uri.lastPathSegment } returns "test.jpg"
        viewModel.addPhoto(uri, "SPECIMEN", "Test photo")
        val photosDir = mockk<File>()

        // When
        viewModel.updateMineral({}, photosDir)

        // Then
        coVerify { mineralRepository.insertPhoto(match { it.mineralId == testMineralId && it.type == "SPECIMEN" }) }
    }

    @Test
    fun `updateMineral should delete removed photos`() = runTest {
        // Given - mineral with existing photo
        val existingPhoto = Photo(
            id = UUID.randomUUID().toString(),
            mineralId = testMineralId,
            type = "NORMAL",
            caption = null,
            takenAt = Instant.now(),
            fileName = "existing.jpg"
        )
        testMineral = testMineral.copy(photos = listOf(existingPhoto))
        every { mineralRepository.getByIdFlow(testMineralId) } returns flowOf(testMineral)

        // Create new viewModel with updated mineral
        viewModel = EditMineralViewModel(testMineralId, mineralRepository)

        // Remove the existing photo
        viewModel.photos.test {
            val photos = awaitItem()
            assertEquals(1, photos.size)
        }

        viewModel.removePhoto(existingPhoto.id)
        val photosDir = mockk<File>()

        // When
        viewModel.updateMineral({}, photosDir)

        // Then
        coVerify { mineralRepository.deletePhoto(existingPhoto.id) }
    }

    @Test
    fun `updateMineral failure should emit error state`() = runTest {
        // Given
        viewModel.onNameChange("Valid Name")
        val photosDir = mockk<File>()
        val errorMessage = "Database error"
        coEvery { mineralRepository.update(any()) } throws Exception(errorMessage)

        // When
        viewModel.updateMineral({}, photosDir)

        // Then
        viewModel.updateState.test {
            val state = awaitItem()
            assertTrue(state is UpdateMineralState.Error)
            assertEquals(errorMessage, (state as UpdateMineralState.Error).message)
        }
    }

    @Test
    fun `resetUpdateState should set state to idle`() = runTest {
        // Given
        viewModel.onNameChange("")
        viewModel.updateMineral({}, mockk())

        viewModel.updateState.test {
            val errorState = awaitItem()
            assertTrue(errorState is UpdateMineralState.Error)

            // When
            viewModel.resetUpdateState()

            // Then
            val idleState = awaitItem()
            assertEquals(UpdateMineralState.Idle, idleState)
        }
    }

    // Tag suggestions tests
    @Test
    fun `tag suggestions should filter available tags`() = runTest {
        // When
        viewModel.onTagsChange("min")

        // Then
        viewModel.tagSuggestions.test {
            val suggestions = awaitItem()
            assertTrue(suggestions.contains("mineral"))
            assertEquals(1, suggestions.size)
        }
    }

    @Test
    fun `tag suggestions should work with multiple tags`() = runTest {
        // When
        viewModel.onTagsChange("common, ra")

        // Then
        viewModel.tagSuggestions.test {
            val suggestions = awaitItem()
            assertTrue(suggestions.contains("rare"))
        }
    }

    @Test
    fun `tag suggestions should be empty for short input`() = runTest {
        // When
        viewModel.onTagsChange("m")

        // Then
        viewModel.tagSuggestions.test {
            val suggestions = awaitItem()
            assertTrue(suggestions.isEmpty())
        }
    }

    @Test
    fun `tag suggestions should limit to 5 results`() = runTest {
        // Given - more than 5 matching tags
        coEvery { mineralRepository.getAllUniqueTags() } returns
            listOf("mineral1", "mineral2", "mineral3", "mineral4", "mineral5", "mineral6", "mineral7")
        viewModel = EditMineralViewModel(testMineralId, mineralRepository)

        // When
        viewModel.onTagsChange("mineral")

        // Then
        viewModel.tagSuggestions.test {
            val suggestions = awaitItem()
            assertEquals(5, suggestions.size)
        }
    }

    // Draft state preservation tests
    @Test
    fun `updateMineral should preserve original mineral properties`() = runTest {
        // Given
        viewModel.onNameChange("Updated Name")
        val photosDir = mockk<File>()
        val mineralSlot = slot<Mineral>()
        coEvery { mineralRepository.update(capture(mineralSlot)) } returns Unit

        // When
        viewModel.updateMineral({}, photosDir)

        // Then - verify original properties are preserved
        assertEquals(testMineral.status, mineralSlot.captured.status)
        assertEquals(testMineral.statusType, mineralSlot.captured.statusType)
        assertEquals(testMineral.qualityRating, mineralSlot.captured.qualityRating)
        assertEquals(testMineral.createdAt, mineralSlot.captured.createdAt)
    }

    @Test
    fun `updateMineral should update updatedAt timestamp`() = runTest {
        // Given
        val originalUpdatedAt = testMineral.updatedAt
        viewModel.onNameChange("Updated Name")
        val photosDir = mockk<File>()
        val mineralSlot = slot<Mineral>()
        coEvery { mineralRepository.update(capture(mineralSlot)) } returns Unit

        // When
        viewModel.updateMineral({}, photosDir)

        // Then
        assertTrue(mineralSlot.captured.updatedAt >= originalUpdatedAt)
    }
}
