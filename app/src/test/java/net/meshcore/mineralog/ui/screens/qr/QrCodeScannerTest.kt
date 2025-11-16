package net.meshcore.mineralog.ui.screens.qr

import net.meshcore.mineralog.data.util.QrCodeGenerator
import org.junit.Test
import org.junit.Assert.*

/**
 * P1-1: Tests for QR Code generation and scanning functionality.
 *
 * Tests cover:
 * - Happy path: Valid QR code generation and parsing
 * - Edge cases: Invalid formats, malformed URIs
 * - Error handling: Null values, empty strings
 */
class QrCodeScannerTest {

    @Test
    fun `test QR code generation with valid mineral ID - happy path`() {
        // Arrange
        val mineralId = "550e8400-e29b-41d4-a716-446655440000"

        // Act
        val qrBitmap = QrCodeGenerator.generate(
            data = QrCodeGenerator.encodeMineralUri(mineralId),
            size = 256
        )

        // Assert
        assertNotNull(qrBitmap)
        assertEquals(256, qrBitmap.width)
        assertEquals(256, qrBitmap.height)
    }

    @Test
    fun `test encode mineral URI format`() {
        // Arrange
        val mineralId = "test-mineral-123"

        // Act
        val encoded = QrCodeGenerator.encodeMineralUri(mineralId)

        // Assert
        assertEquals("mineralapp://mineral/test-mineral-123", encoded)
        assertTrue(encoded.startsWith("mineralapp://mineral/"))
        assertTrue(encoded.endsWith(mineralId))
    }

    @Test
    fun `test decode valid mineral URI - happy path`() {
        // Arrange
        val expectedId = "550e8400-e29b-41d4-a716-446655440000"
        val uri = "mineralapp://mineral/$expectedId"

        // Act
        val decodedId = QrCodeGenerator.decodeMineralUri(uri)

        // Assert
        assertEquals(expectedId, decodedId)
    }

    @Test
    fun `test decode legacy mineralog URI`() {
        // Arrange - Legacy scheme for backwards compatibility
        val expectedId = "legacy-mineral-id"
        val legacyUri = "mineralog://mineral/$expectedId"

        // Act
        val decodedId = QrCodeGenerator.decodeMineralUri(legacyUri)

        // Assert
        assertEquals(expectedId, decodedId)
    }

    @Test
    fun `test decode plain mineral ID`() {
        // Arrange - Support plain IDs without scheme
        val mineralId = "plain-id-12345"

        // Act
        val decodedId = QrCodeGenerator.decodeMineralUri(mineralId)

        // Assert
        assertEquals(mineralId, decodedId)
    }

    @Test
    fun `test extract mineral ID from deep link - happy path`() {
        // Arrange
        val deepLink = "mineralapp://mineral/abc-123-def"

        // Act
        val mineralId = extractMineralIdFromQrCode(deepLink)

        // Assert
        assertEquals("abc-123-def", mineralId)
    }

    @Test
    fun `test extract mineral ID from valid UUID format`() {
        // Arrange
        val uuid = "550e8400-e29b-41d4-a716-446655440000"

        // Act
        val mineralId = extractMineralIdFromQrCode(uuid)

        // Assert
        assertEquals(uuid, mineralId)
    }

    @Test
    fun `test extract mineral ID from invalid format - error case`() {
        // Arrange
        val invalidQr = "https://example.com/not-a-mineral"

        // Act
        val mineralId = extractMineralIdFromQrCode(invalidQr)

        // Assert
        assertNull(mineralId)  // Invalid format should return null
    }

    @Test
    fun `test extract mineral ID from empty string - error case`() {
        // Arrange
        val emptyQr = ""

        // Act
        val mineralId = extractMineralIdFromQrCode(emptyQr)

        // Assert
        assertNull(mineralId)  // Empty string should return null
    }

    @Test
    fun `test extract mineral ID from random text - error case`() {
        // Arrange
        val randomText = "This is just random text, not a QR code"

        // Act
        val mineralId = extractMineralIdFromQrCode(randomText)

        // Assert
        assertNull(mineralId)  // Random text should return null
    }

    @Test
    fun `test QR code batch generation`() {
        // Arrange
        val mineralIds = listOf(
            "mineral-1",
            "mineral-2",
            "mineral-3"
        )

        // Act
        val qrCodes = QrCodeGenerator.generateBatch(
            mineralIds = mineralIds,
            size = 512
        )

        // Assert
        assertEquals(3, qrCodes.size)
        assertTrue(qrCodes.containsKey("mineral-1"))
        assertTrue(qrCodes.containsKey("mineral-2"))
        assertTrue(qrCodes.containsKey("mineral-3"))

        // Verify all bitmaps are correct size
        qrCodes.values.forEach { bitmap ->
            assertEquals(512, bitmap.width)
            assertEquals(512, bitmap.height)
        }
    }

    @Test
    fun `test QR code generation with different sizes`() {
        // Arrange
        val mineralId = "test-id"
        val sizes = listOf(128, 256, 512, 1024)

        // Act & Assert
        sizes.forEach { size ->
            val qrCode = QrCodeGenerator.generate(
                data = QrCodeGenerator.encodeMineralUri(mineralId),
                size = size
            )
            assertEquals(size, qrCode.width)
            assertEquals(size, qrCode.height)
        }
    }

    @Test
    fun `test QR code generation with custom margin`() {
        // Arrange
        val mineralId = "test-margin"

        // Act
        val qrCode = QrCodeGenerator.generate(
            data = QrCodeGenerator.encodeMineralUri(mineralId),
            size = 256,
            margin = 0  // No margin
        )

        // Assert
        assertNotNull(qrCode)
        assertEquals(256, qrCode.width)
    }

    @Test
    fun `test UUID format validation in extraction`() {
        // Valid UUID formats
        val validUuid = "550e8400-e29b-41d4-a716-446655440000"
        assertNotNull(extractMineralIdFromQrCode(validUuid))

        // Invalid UUID formats
        val invalidUuids = listOf(
            "not-a-uuid",
            "550e8400",  // Too short
            "550e8400-e29b-41d4-a716",  // Missing segments
            "ggge8400-e29b-41d4-a716-446655440000",  // Invalid hex
        )

        invalidUuids.forEach { invalidUuid ->
            assertNull(extractMineralIdFromQrCode(invalidUuid))
        }
    }

    @Test
    fun `test QR scanner state transitions`() {
        // Test the QrScannerState sealed class

        val idleState = QrScannerState.Idle
        val scanningState = QrScannerState.Scanning
        val successState = QrScannerState.Success("test-data")
        val errorState = QrScannerState.Error("Error message")

        // Assert states are distinct
        assertNotEquals(idleState, scanningState)
        assertNotEquals(successState, errorState)

        // Assert data classes contain correct data
        assertEquals("test-data", (successState as QrScannerState.Success).qrData)
        assertEquals("Error message", (errorState as QrScannerState.Error).message)
    }
}
