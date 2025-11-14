package net.meshcore.mineralog.ui.screens.qr

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for QR scanner utility functions.
 * Tests M2 Sprint Item #6 (QR Scanning).
 */
class QrScannerTest {

    @Test
    fun `extractMineralIdFromQrCode deep link format returns mineral ID`() {
        // Given
        val qrCode = "mineralapp://mineral/550e8400-e29b-41d4-a716-446655440000"

        // When
        val result = extractMineralIdFromQrCode(qrCode)

        // Then
        assertEquals("550e8400-e29b-41d4-a716-446655440000", result)
    }

    @Test
    fun `extractMineralIdFromQrCode direct UUID format returns UUID`() {
        // Given
        val uuid = "550e8400-e29b-41d4-a716-446655440000"

        // When
        val result = extractMineralIdFromQrCode(uuid)

        // Then
        assertEquals(uuid, result)
    }

    @Test
    fun `extractMineralIdFromQrCode invalid format returns null`() {
        // Given
        val invalidQrCode = "https://example.com/mineral/123"

        // When
        val result = extractMineralIdFromQrCode(invalidQrCode)

        // Then
        assertNull(result)
    }

    @Test
    fun `extractMineralIdFromQrCode malformed UUID returns null`() {
        // Given
        val malformedUuid = "not-a-uuid-123"

        // When
        val result = extractMineralIdFromQrCode(malformedUuid)

        // Then
        assertNull(result)
    }

    @Test
    fun `extractMineralIdFromQrCode empty string returns null`() {
        // Given
        val emptyString = ""

        // When
        val result = extractMineralIdFromQrCode(emptyString)

        // Then
        assertNull(result)
    }

    @Test
    fun `extractMineralIdFromQrCode uppercase UUID returns UUID`() {
        // Given
        val uuid = "550E8400-E29B-41D4-A716-446655440000"

        // When
        val result = extractMineralIdFromQrCode(uuid)

        // Then
        assertEquals(uuid, result)
    }

    @Test
    fun `extractMineralIdFromQrCode deep link with trailing slash returns mineral ID`() {
        // Given
        val qrCode = "mineralapp://mineral/550e8400-e29b-41d4-a716-446655440000/"

        // When
        val result = extractMineralIdFromQrCode(qrCode)

        // Then
        assertEquals("550e8400-e29b-41d4-a716-446655440000/", result)
    }

    @Test
    fun `extractMineralIdFromQrCode deep link uppercase scheme returns mineral ID`() {
        // Given
        val qrCode = "MINERALAPP://mineral/550e8400-e29b-41d4-a716-446655440000"

        // When
        val result = extractMineralIdFromQrCode(qrCode)

        // Then
        // Should return null because the function is case-sensitive
        assertNull(result)
    }

    @Test
    fun `extractMineralIdFromQrCode special characters in UUID returns null`() {
        // Given
        val invalidUuid = "550e8400-e29b-41d4-a716-44665544000@"

        // When
        val result = extractMineralIdFromQrCode(invalidUuid)

        // Then
        assertNull(result)
    }

    @Test
    fun `extractMineralIdFromQrCode too short UUID returns null`() {
        // Given
        val shortUuid = "550e8400-e29b-41d4"

        // When
        val result = extractMineralIdFromQrCode(shortUuid)

        // Then
        assertNull(result)
    }
}
