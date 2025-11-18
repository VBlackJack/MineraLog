package net.meshcore.mineralog.ui.screens.detail

import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import net.meshcore.mineralog.data.repository.MineralRepository
import net.meshcore.mineralog.data.repository.MineralRepositoryImpl
import net.meshcore.mineralog.data.util.QrLabelPdfGenerator
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.MineralComponent
import net.meshcore.mineralog.domain.model.MineralType
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.Instant

/**
 * Unit tests for MineralDetailViewModel.
 *
 * Tests cover:
 * - Mineral loading from repository
 * - Mineral type detection (SIMPLE vs AGGREGATE)
 * - Aggregate components loading
 * - Delete mineral operation (success, error, state transitions)
 * - QR label generation (success, error, mineral not loaded)
 * - State reset operations
 *
 * Sprint 3: ViewModel Tests - Target 70%+ coverage
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MineralDetailViewModelTest {

    private lateinit var context: Context
    private lateinit var mineralRepository: MineralRepositoryImpl
    private lateinit var qrGenerator: QrLabelPdfGenerator
    private lateinit var viewModel: MineralDetailViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val mineralId = "test-mineral-id"

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        mineralRepository = mockk(relaxed = true)
        qrGenerator = mockk(relaxed = true)

        // Mock QrLabelPdfGenerator constructor
        mockkConstructor(QrLabelPdfGenerator::class)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ========== INITIALIZATION TESTS ==========

    @Test
    @DisplayName("init - loads mineral from repository")
    fun `init - mineral flow - loaded from repository`() = runTest {
        // Arrange
        val testMineral = createTestMineral(mineralId, "Quartz")
        val mineralFlow = MutableStateFlow<Mineral?>(testMineral)
        every { mineralRepository.getByIdFlow(mineralId) } returns mineralFlow
        coEvery { mineralRepository.getMineralType(mineralId) } returns MineralType.SIMPLE
        every { mineralRepository.getAggregateComponentsFlow(mineralId) } returns flowOf(emptyList())

        // Act
        viewModel = MineralDetailViewModel(context, mineralId, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.mineral.test {
            val mineral = awaitItem()
            assertNotNull(mineral)
            assertEquals("Quartz", mineral?.name)
            assertEquals(mineralId, mineral?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - loads mineral type SIMPLE")
    fun `init - mineral type - SIMPLE for simple minerals`() = runTest {
        // Arrange
        val testMineral = createTestMineral(mineralId, "Quartz")
        every { mineralRepository.getByIdFlow(mineralId) } returns flowOf(testMineral)
        coEvery { mineralRepository.getMineralType(mineralId) } returns MineralType.SIMPLE
        every { mineralRepository.getAggregateComponentsFlow(mineralId) } returns flowOf(emptyList())

        // Act
        viewModel = MineralDetailViewModel(context, mineralId, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.mineralType.test {
            val type = awaitItem()
            assertEquals(MineralType.SIMPLE, type)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - loads mineral type AGGREGATE")
    fun `init - mineral type - AGGREGATE for aggregate minerals`() = runTest {
        // Arrange
        val testMineral = createTestMineral(mineralId, "Granite", type = "AGGREGATE")
        every { mineralRepository.getByIdFlow(mineralId) } returns flowOf(testMineral)
        coEvery { mineralRepository.getMineralType(mineralId) } returns MineralType.AGGREGATE
        every { mineralRepository.getAggregateComponentsFlow(mineralId) } returns flowOf(emptyList())

        // Act
        viewModel = MineralDetailViewModel(context, mineralId, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.mineralType.test {
            val type = awaitItem()
            assertEquals(MineralType.AGGREGATE, type)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("init - loads aggregate components")
    fun `init - components - loaded for aggregate minerals`() = runTest {
        // Arrange
        val testMineral = createTestMineral(mineralId, "Granite", type = "AGGREGATE")
        val components = listOf(
            MineralComponent(mineralId, "Quartz", 40),
            MineralComponent(mineralId, "Feldspar", 35),
            MineralComponent(mineralId, "Mica", 25)
        )
        every { mineralRepository.getByIdFlow(mineralId) } returns flowOf(testMineral)
        coEvery { mineralRepository.getMineralType(mineralId) } returns MineralType.AGGREGATE
        every { mineralRepository.getAggregateComponentsFlow(mineralId) } returns flowOf(components)

        // Act
        viewModel = MineralDetailViewModel(context, mineralId, mineralRepository)
        advanceUntilIdle()

        // Assert
        viewModel.components.test {
            val loadedComponents = awaitItem()
            assertEquals(3, loadedComponents.size)
            assertTrue(loadedComponents.any { it.mineralName == "Quartz" })
            assertTrue(loadedComponents.any { it.mineralName == "Feldspar" })
            assertTrue(loadedComponents.any { it.mineralName == "Mica" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== DELETE MINERAL TESTS ==========

    @Test
    @DisplayName("deleteMineral - success - updates state to Success")
    fun `deleteMineral - success - state transitions to Success`() = runTest {
        // Arrange
        setupViewModel()
        coEvery { mineralRepository.delete(mineralId) } just Runs

        // Act
        viewModel.deleteMineral()
        advanceUntilIdle()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is DeleteState.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { mineralRepository.delete(mineralId) }
    }

    @Test
    @DisplayName("deleteMineral - sets Deleting state during operation")
    fun `deleteMineral - state transitions - Idle to Deleting to Success`() = runTest {
        // Arrange
        setupViewModel()
        coEvery { mineralRepository.delete(mineralId) } just Runs

        // Act & Assert
        viewModel.deleteState.test {
            // Initial state should be Idle
            val initial = awaitItem()
            assertTrue(initial is DeleteState.Idle)

            // Trigger delete
            viewModel.deleteMineral()

            // Should transition to Deleting
            val deleting = awaitItem()
            assertTrue(deleting is DeleteState.Deleting)

            advanceUntilIdle()

            // Should transition to Success
            val success = awaitItem()
            assertTrue(success is DeleteState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("deleteMineral - error - updates state to Error")
    fun `deleteMineral - error - state transitions to Error with message`() = runTest {
        // Arrange
        setupViewModel()
        val errorMessage = "Database connection failed"
        coEvery { mineralRepository.delete(mineralId) } throws Exception(errorMessage)

        // Act
        viewModel.deleteMineral()
        advanceUntilIdle()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is DeleteState.Error)
            assertEquals(errorMessage, (state as DeleteState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("deleteMineral - unknown error - provides fallback message")
    fun `deleteMineral - unknown error - fallback error message`() = runTest {
        // Arrange
        setupViewModel()
        coEvery { mineralRepository.delete(mineralId) } throws Exception()

        // Act
        viewModel.deleteMineral()
        advanceUntilIdle()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is DeleteState.Error)
            assertEquals("Failed to delete mineral", (state as DeleteState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("resetDeleteState - resets state to Idle")
    fun `resetDeleteState - state - resets to Idle`() = runTest {
        // Arrange
        setupViewModel()
        coEvery { mineralRepository.delete(mineralId) } just Runs
        viewModel.deleteMineral()
        advanceUntilIdle()

        // Act
        viewModel.resetDeleteState()

        // Assert
        viewModel.deleteState.test {
            val state = awaitItem()
            assertTrue(state is DeleteState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== QR LABEL GENERATION TESTS ==========

    @Test
    @DisplayName("generateQrLabel - success - updates state to Success")
    fun `generateQrLabel - success - state transitions to Success with URI`() = runTest {
        // Arrange
        val testMineral = createTestMineral(mineralId, "Quartz")
        setupViewModelWithMineral(testMineral)

        val outputUri = mockk<Uri>(relaxed = true)
        every { anyConstructed<QrLabelPdfGenerator>().generate(any(), any()) } returns Result.success(outputUri)

        // Act
        viewModel.generateQrLabel(outputUri)
        advanceUntilIdle()

        // Assert
        viewModel.qrGenerationState.test {
            val state = awaitItem()
            assertTrue(state is QrGenerationState.Success)
            assertEquals(outputUri, (state as QrGenerationState.Success).uri)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("generateQrLabel - sets Generating state during operation")
    fun `generateQrLabel - state transitions - Idle to Generating to Success`() = runTest {
        // Arrange
        val testMineral = createTestMineral(mineralId, "Quartz")
        setupViewModelWithMineral(testMineral)

        val outputUri = mockk<Uri>(relaxed = true)
        every { anyConstructed<QrLabelPdfGenerator>().generate(any(), any()) } returns Result.success(outputUri)

        // Act & Assert
        viewModel.qrGenerationState.test {
            // Initial state should be Idle
            val initial = awaitItem()
            assertTrue(initial is QrGenerationState.Idle)

            // Trigger generation
            viewModel.generateQrLabel(outputUri)

            // Should transition to Generating
            val generating = awaitItem()
            assertTrue(generating is QrGenerationState.Generating)

            advanceUntilIdle()

            // Should transition to Success
            val success = awaitItem()
            assertTrue(success is QrGenerationState.Success)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("generateQrLabel - mineral not loaded - returns Error")
    fun `generateQrLabel - mineral not loaded - Error state with message`() = runTest {
        // Arrange
        setupViewModelWithMineral(null) // No mineral loaded

        val outputUri = mockk<Uri>(relaxed = true)

        // Act
        viewModel.generateQrLabel(outputUri)
        advanceUntilIdle()

        // Assert
        viewModel.qrGenerationState.test {
            val state = awaitItem()
            assertTrue(state is QrGenerationState.Error)
            assertEquals("Mineral not loaded", (state as QrGenerationState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }

        // Should NOT call generator if mineral not loaded
        verify(exactly = 0) { anyConstructed<QrLabelPdfGenerator>().generate(any(), any()) }
    }

    @Test
    @DisplayName("generateQrLabel - generation fails - returns Error")
    fun `generateQrLabel - generation error - Error state with message`() = runTest {
        // Arrange
        val testMineral = createTestMineral(mineralId, "Quartz")
        setupViewModelWithMineral(testMineral)

        val outputUri = mockk<Uri>(relaxed = true)
        val errorMessage = "PDF generation failed"
        every { anyConstructed<QrLabelPdfGenerator>().generate(any(), any()) } returns
            Result.failure(Exception(errorMessage))

        // Act
        viewModel.generateQrLabel(outputUri)
        advanceUntilIdle()

        // Assert
        viewModel.qrGenerationState.test {
            val state = awaitItem()
            assertTrue(state is QrGenerationState.Error)
            assertEquals(errorMessage, (state as QrGenerationState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("generateQrLabel - exception thrown - returns Error")
    fun `generateQrLabel - exception - Error state with message`() = runTest {
        // Arrange
        val testMineral = createTestMineral(mineralId, "Quartz")
        setupViewModelWithMineral(testMineral)

        val outputUri = mockk<Uri>(relaxed = true)
        val errorMessage = "Unexpected error"
        every { anyConstructed<QrLabelPdfGenerator>().generate(any(), any()) } throws Exception(errorMessage)

        // Act
        viewModel.generateQrLabel(outputUri)
        advanceUntilIdle()

        // Assert
        viewModel.qrGenerationState.test {
            val state = awaitItem()
            assertTrue(state is QrGenerationState.Error)
            assertEquals(errorMessage, (state as QrGenerationState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("resetQrGenerationState - resets state to Idle")
    fun `resetQrGenerationState - state - resets to Idle`() = runTest {
        // Arrange
        val testMineral = createTestMineral(mineralId, "Quartz")
        setupViewModelWithMineral(testMineral)

        val outputUri = mockk<Uri>(relaxed = true)
        every { anyConstructed<QrLabelPdfGenerator>().generate(any(), any()) } returns Result.success(outputUri)

        viewModel.generateQrLabel(outputUri)
        advanceUntilIdle()

        // Act
        viewModel.resetQrGenerationState()

        // Assert
        viewModel.qrGenerationState.test {
            val state = awaitItem()
            assertTrue(state is QrGenerationState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ========== HELPER METHODS ==========

    private fun setupViewModel() {
        val testMineral = createTestMineral(mineralId, "Quartz")
        every { mineralRepository.getByIdFlow(mineralId) } returns flowOf(testMineral)
        coEvery { mineralRepository.getMineralType(mineralId) } returns MineralType.SIMPLE
        every { mineralRepository.getAggregateComponentsFlow(mineralId) } returns flowOf(emptyList())

        viewModel = MineralDetailViewModel(context, mineralId, mineralRepository)
        advanceUntilIdle()
    }

    private fun setupViewModelWithMineral(mineral: Mineral?) {
        every { mineralRepository.getByIdFlow(mineralId) } returns MutableStateFlow(mineral)
        coEvery { mineralRepository.getMineralType(mineralId) } returns MineralType.SIMPLE
        every { mineralRepository.getAggregateComponentsFlow(mineralId) } returns flowOf(emptyList())

        viewModel = MineralDetailViewModel(context, mineralId, mineralRepository)
        advanceUntilIdle()
    }

    private fun createTestMineral(
        id: String,
        name: String,
        type: String = "SIMPLE"
    ) = Mineral(
        id = id,
        name = name,
        group = "Silicates",
        formula = "SiO2",
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
        notes = "Test mineral",
        provenance = null,
        storage = null,
        status = "COLLECTION",
        quality = 4,
        estimatedValue = 100.0,
        acquisitionDate = Instant.now(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        mineralType = type,
        qrCode = null,
        photos = emptyList()
    )
}
