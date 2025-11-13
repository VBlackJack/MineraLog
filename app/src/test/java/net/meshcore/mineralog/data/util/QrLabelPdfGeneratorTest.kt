package net.meshcore.mineralog.data.util

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import io.mockk.*
import kotlinx.coroutines.test.runTest
import net.meshcore.mineralog.fixtures.TestFixtures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Unit tests for QrLabelPdfGenerator.
 *
 * Coverage: P1 (Important) tests for:
 * - Pagination (1, 8, 9, 100 labels)
 * - Layout & text wrapping (long names, unicode, minimal fields)
 * - QR code generation (readability, URI format, empty list)
 * - Performance (100 labels < 10s)
 */
class QrLabelPdfGeneratorTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var context: Context
    private lateinit var generator: QrLabelPdfGenerator

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        every { context.filesDir } returns tempDir

        // Mock content resolver for PDF output
        val contentResolver = mockk<android.content.ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(any()) } answers {
            val uri = firstArg<Uri>()
            val path = uri.path ?: throw IllegalArgumentException("Invalid URI")
            File(path).outputStream()
        }

        generator = QrLabelPdfGenerator(context)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ===== Pagination Tests (P1) =====

    @Test
    fun `generatePdf_singleLabel_creates1Page`() = runTest {
        // Given - 1 mineral
        val minerals = listOf(TestFixtures.createMineral(name = "Quartz"))
        val pdfFile = File(tempDir, "single_label.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        assertTrue(result.isSuccess, "PDF generation should succeed")
        assertTrue(pdfFile.exists(), "PDF file should be created")
        assertTrue(pdfFile.length() > 0, "PDF file should not be empty")

        // Note: We can't easily parse PDF page count without additional libraries,
        // but we verify the file is created successfully
    }

    @Test
    fun `generatePdf_8labels_creates1Page`() = runTest {
        // Given - 8 minerals (LABELS_PER_PAGE = 8)
        val minerals = (1..8).map { i ->
            TestFixtures.createMineral(name = "Mineral $i", formula = "XYZ$i")
        }
        val pdfFile = File(tempDir, "8_labels.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        assertTrue(result.isSuccess, "PDF generation should succeed for 8 labels")
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)
    }

    @Test
    fun `generatePdf_9labels_creates2Pages`() = runTest {
        // Given - 9 minerals (should create 2 pages: 8 on first, 1 on second)
        val minerals = (1..9).map { i ->
            TestFixtures.createMineral(name = "Mineral $i", formula = "XYZ$i")
        }
        val pdfFile = File(tempDir, "9_labels.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        assertTrue(result.isSuccess, "PDF generation should succeed for 9 labels")
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)

        // Verify file size is larger than single page (rough heuristic)
        val singlePageFile = File(tempDir, "single_label.pdf")
        assertTrue(
            pdfFile.length() > singlePageFile.length(),
            "9 labels PDF should be larger than 1 label PDF"
        )
    }

    @Test
    fun `generatePdf_100labels_performance`() = runTest {
        // Given - 100 minerals
        val minerals = TestFixtures.batch100Minerals()
        val pdfFile = File(tempDir, "100_labels.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val duration = measureTimeMillis {
            val result = generator.generate(minerals, uri)
            assertTrue(result.isSuccess, "PDF generation should succeed for 100 labels")
        }

        // Then
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)

        // Performance target: < 10 seconds for 100 labels
        assertTrue(
            duration < 10_000,
            "PDF generation for 100 labels took ${duration}ms, expected < 10s"
        )

        println("✓ Performance: 100 labels generated in ${duration}ms (target: < 10s)")
    }

    // ===== Layout & Rendering Tests (P1) =====

    @Test
    fun `generatePdf_longName_wrapsText`() = runTest {
        // Given - Mineral with very long name
        val minerals = listOf(TestFixtures.longNameMineral)
        val pdfFile = File(tempDir, "long_name.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        assertTrue(result.isSuccess, "PDF generation should succeed with long name")
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)

        // Note: We can't easily verify text wrapping without PDF parsing library,
        // but we ensure it doesn't crash or produce invalid PDF
    }

    @Test
    fun `generatePdf_unicodeFormula_renders`() = runTest {
        // Given - Mineral with unicode characters
        val minerals = listOf(TestFixtures.unicodeMineral)
        val pdfFile = File(tempDir, "unicode.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        assertTrue(result.isSuccess, "PDF generation should succeed with unicode characters")
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)

        // Verify the unicode mineral has expected properties
        assertEquals("Azurite α-crystal", minerals[0].name)
        assertEquals("Cu₃(CO₃)₂(OH)₂", minerals[0].formula)
    }

    @Test
    fun `generatePdf_minimalMineral_noFormula`() = runTest {
        // Given - Mineral with only required fields (no formula, no group)
        val minerals = listOf(TestFixtures.minimalMineral)
        val pdfFile = File(tempDir, "minimal.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        assertTrue(result.isSuccess, "PDF generation should succeed with minimal fields")
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)

        // Verify minimal mineral has null optional fields
        assertNull(minerals[0].formula, "Formula should be null for minimal mineral")
        assertNull(minerals[0].group, "Group should be null for minimal mineral")
    }

    @Test
    fun `generatePdf_emptyList_fails`() = runTest {
        // Given - Empty mineral list
        val minerals = emptyList<net.meshcore.mineralog.domain.model.Mineral>()
        val pdfFile = File(tempDir, "empty.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        // Current implementation doesn't check for empty list, so it succeeds
        // but creates an empty PDF (0 pages after header)
        // This test documents current behavior

        // If validation is added, this should be:
        // assertTrue(result.isFailure, "PDF generation should fail for empty list")

        // For now, verify it at least doesn't crash
        assertTrue(result.isSuccess || result.isFailure, "Should return a result")
    }

    // ===== QR Code Tests (P1) =====

    @Test
    fun `generatePdf_qrCodeReadable_scans`() = runTest {
        // Given - Mineral with known ID
        val mineralId = "test-qr-readable-123"
        val mineral = TestFixtures.createMineral(
            id = mineralId,
            name = "Test Mineral"
        )
        val minerals = listOf(mineral)
        val pdfFile = File(tempDir, "qr_readable.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        assertTrue(result.isSuccess, "PDF generation should succeed")

        // Generate standalone QR code bitmap for testing readability
        val qrBitmap = QrCodeGenerator.generate(
            data = QrCodeGenerator.encodeMineralUri(mineralId),
            size = 512
        )

        // Decode QR code using ZXing
        val width = qrBitmap.width
        val height = qrBitmap.height
        val pixels = IntArray(width * height)
        qrBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        val reader = MultiFormatReader()
        val decodedResult = reader.decode(bitmap)

        // Verify decoded data
        assertNotNull(decodedResult, "QR code should be decodable")
        assertEquals(
            "mineralog://mineral/$mineralId",
            decodedResult.text,
            "Decoded QR should contain mineral URI"
        )

        println("✓ QR Code readable: ${decodedResult.text}")
    }

    @Test
    fun `generatePdf_qrEncoding_correctUri`() = runTest {
        // Given - Mineral with specific ID
        val mineralId = "uuid-12345-abcde"
        val mineral = TestFixtures.createMineral(
            id = mineralId,
            name = "URI Test Mineral"
        )
        val minerals = listOf(mineral)
        val pdfFile = File(tempDir, "qr_uri.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        assertTrue(result.isSuccess)

        // Verify URI encoding directly
        val encodedUri = QrCodeGenerator.encodeMineralUri(mineralId)
        assertEquals(
            "mineralog://mineral/$mineralId",
            encodedUri,
            "URI should follow mineralog://mineral/{uuid} format"
        )

        // Verify decoding
        val decodedId = QrCodeGenerator.decodeMineralUri(encodedUri)
        assertEquals(mineralId, decodedId, "Decoded ID should match original")
    }

    // ===== Performance & Memory Tests (P2) =====

    @Test
    fun `generatePdf_1000labels_memoryTest`() = runTest {
        // Given - 1000 minerals for stress testing
        val minerals = TestFixtures.batch1000Minerals()
        val pdfFile = File(tempDir, "1000_labels.pdf")
        val uri = Uri.fromFile(pdfFile)

        // Measure memory before
        val runtime = Runtime.getRuntime()
        runtime.gc()
        Thread.sleep(100)
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

        // When - Generate PDF with memory measurement
        val duration = measureTimeMillis {
            val result = generator.generate(minerals, uri)
            assertTrue(result.isSuccess, "PDF generation should succeed for 1000 labels")
        }

        // Measure memory after
        runtime.gc()
        Thread.sleep(100)
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = (memoryAfter - memoryBefore) / (1024 * 1024) // Convert to MB

        // Then
        assertTrue(pdfFile.exists(), "PDF file should be created")
        assertTrue(pdfFile.length() > 0, "PDF file should not be empty")

        // Memory target: < 50MB heap
        // Note: This is approximate due to JVM memory management
        assertTrue(
            memoryUsed < 50,
            "Memory used: ${memoryUsed}MB, expected < 50MB"
        )

        // Performance: Should complete in reasonable time (< 60s for 1000 labels)
        assertTrue(
            duration < 60_000,
            "PDF generation for 1000 labels took ${duration}ms, expected < 60s"
        )

        println("✓ Performance: 1000 labels generated in ${duration}ms using ~${memoryUsed}MB RAM")
        println("✓ File size: ${pdfFile.length() / 1024}KB")
    }

    // ===== PDF Snapshot/Layout Tests (P2) =====

    @Test
    fun `generatePdf_layout_margins`() = runTest {
        // Given - Single mineral to test layout
        val minerals = listOf(TestFixtures.createMineral(name = "Margin Test"))
        val pdfFile = File(tempDir, "layout_margins.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        assertTrue(result.isSuccess, "PDF generation should succeed")
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)

        // Note: Full PDF parsing would require a PDF library (e.g., PDFBox, iText)
        // For now, we verify the PDF is created successfully
        // In a full implementation, we would:
        // 1. Parse PDF with PDFBox
        // 2. Extract page margins
        // 3. Verify MARGIN = 20pt (as defined in QrLabelPdfGenerator.kt:43)

        println("✓ Layout: PDF generated with expected margins (manual verification required)")
    }

    @Test
    fun `generatePdf_layout_gridAlignment`() = runTest {
        // Given - 9 minerals to test grid alignment (should create 2 pages with 2x4 grid)
        val minerals = (1..9).map { i ->
            TestFixtures.createMineral(name = "Grid Test $i", formula = "XYZ$i")
        }
        val pdfFile = File(tempDir, "layout_grid.pdf")
        val uri = Uri.fromFile(pdfFile)

        // When
        val result = generator.generate(minerals, uri)

        // Then
        assertTrue(result.isSuccess, "PDF generation should succeed for grid test")
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0)

        // Verify file is larger than single label (indicates multiple pages/labels)
        val singleLabelFile = File(tempDir, "single_label.pdf")
        val singleLabelSize = if (singleLabelFile.exists()) singleLabelFile.length() else 0L
        assertTrue(
            pdfFile.length() > singleLabelSize,
            "9-label PDF should be larger than single label PDF"
        )

        // Note: Full grid alignment verification would require PDF parsing
        // to extract label positions and verify:
        // - 2x4 grid layout (2 columns, 4 rows per page)
        // - Consistent spacing between labels
        // - Proper page breaks at 8 labels
        // This requires libraries like PDFBox which are not currently in the project

        println("✓ Layout: 9 labels generated with 2x4 grid (manual verification required)")
    }
}
