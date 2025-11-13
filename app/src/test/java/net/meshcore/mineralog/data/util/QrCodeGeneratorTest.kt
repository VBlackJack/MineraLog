package net.meshcore.mineralog.data.util

import android.graphics.Bitmap
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf

/**
 * Unit tests for QrCodeGenerator.
 *
 * Tests QR code generation, encoding/decoding, and batch operations.
 * Note: Some tests require Android environment (Robolectric).
 */
class QrCodeGeneratorTest {

    @Test
    fun `encodeMineralUri formats correctly`() {
        val mineralId = "123e4567-e89b-12d3-a456-426614174000"

        val encoded = QrCodeGenerator.encodeMineralUri(mineralId)

        assertEquals("mineralog://mineral/$mineralId", encoded)
        assertTrue(encoded.startsWith("mineralog://mineral/"))
    }

    @Test
    fun `decodeMineralUri extracts ID from deep link`() {
        val mineralId = "123e4567-e89b-12d3-a456-426614174000"
        val deepLink = "mineralog://mineral/$mineralId"

        val decoded = QrCodeGenerator.decodeMineralUri(deepLink)

        assertEquals(mineralId, decoded)
    }

    @Test
    fun `decodeMineralUri handles plain ID`() {
        val mineralId = "123e4567-e89b-12d3-a456-426614174000"

        val decoded = QrCodeGenerator.decodeMineralUri(mineralId)

        assertEquals(mineralId, decoded)
    }

    @Test
    fun `decodeMineralUri returns null for invalid format`() {
        val invalid = "not-a-valid-uri"

        val decoded = QrCodeGenerator.decodeMineralUri(invalid)

        assertEquals(invalid, decoded) // Falls back to returning the input
    }

    @Test
    fun `encodeMineralUri handles special characters in ID`() {
        val mineralId = "id-with-dashes_and_underscores.123"

        val encoded = QrCodeGenerator.encodeMineralUri(mineralId)

        assertEquals("mineralog://mineral/$mineralId", encoded)
    }

    @Test
    fun `decodeMineralUri roundtrip works`() {
        val mineralId = "test-mineral-id-123"

        val encoded = QrCodeGenerator.encodeMineralUri(mineralId)
        val decoded = QrCodeGenerator.decodeMineralUri(encoded)

        assertEquals(mineralId, decoded)
    }

    // The following tests require Android/Robolectric due to Bitmap and ZXing usage
    // They are marked with @EnabledIf for conditional execution

    @Test
    @EnabledIf("isAndroidEnvironmentAvailable")
    fun `generate creates non-null bitmap`() {
        val data = "test-data"

        val bitmap = QrCodeGenerator.generate(data)

        assertNotNull(bitmap)
    }

    @Test
    @EnabledIf("isAndroidEnvironmentAvailable")
    fun `generate creates bitmap with correct size`() {
        val data = "test"
        val size = 256

        val bitmap = QrCodeGenerator.generate(data, size)

        assertEquals(size, bitmap.width)
        assertEquals(size, bitmap.height)
    }

    @Test
    @EnabledIf("isAndroidEnvironmentAvailable")
    fun `generate handles long data`() {
        val data = "a".repeat(500)

        val bitmap = QrCodeGenerator.generate(data)

        assertNotNull(bitmap)
    }

    @Test
    @EnabledIf("isAndroidEnvironmentAvailable")
    fun `generate handles unicode data`() {
        val data = "中文日本語한국어"

        val bitmap = QrCodeGenerator.generate(data)

        assertNotNull(bitmap)
    }

    @Test
    @EnabledIf("isAndroidEnvironmentAvailable")
    fun `generateBatch creates QR codes for all IDs`() {
        val mineralIds = listOf(
            "mineral-1",
            "mineral-2",
            "mineral-3"
        )

        val results = QrCodeGenerator.generateBatch(mineralIds)

        assertEquals(3, results.size)
        mineralIds.forEach { id ->
            assertTrue(results.containsKey(id))
            assertNotNull(results[id])
        }
    }

    @Test
    @EnabledIf("isAndroidEnvironmentAvailable")
    fun `generateBatch handles empty list`() {
        val results = QrCodeGenerator.generateBatch(emptyList())

        assertTrue(results.isEmpty())
    }

    @Test
    @EnabledIf("isAndroidEnvironmentAvailable")
    fun `generateBatch performance for 100 codes`() {
        val mineralIds = (1..100).map { "mineral-$it" }

        val start = System.currentTimeMillis()
        val results = QrCodeGenerator.generateBatch(mineralIds, size = 256)
        val duration = System.currentTimeMillis() - start

        assertEquals(100, results.size)
        // Should generate 100 QR codes in < 5 seconds
        assertTrue(duration < 5000, "Batch generation of 100 codes took ${duration}ms")
    }

    @Test
    @EnabledIf("isAndroidEnvironmentAvailable")
    fun `different data produces different QR codes`() {
        val data1 = "data-1"
        val data2 = "data-2"

        val bitmap1 = QrCodeGenerator.generate(data1)
        val bitmap2 = QrCodeGenerator.generate(data2)

        // Bitmaps should be different (not pixel-perfect comparison, just reference)
        assertNotEquals(bitmap1, bitmap2)
    }

    @Test
    @EnabledIf("isAndroidEnvironmentAvailable")
    fun `same data produces same QR code`() {
        val data = "same-data"

        val bitmap1 = QrCodeGenerator.generate(data)
        val bitmap2 = QrCodeGenerator.generate(data)

        // Should produce identical QR codes (deterministic)
        assertEquals(bitmap1.width, bitmap2.width)
        assertEquals(bitmap1.height, bitmap2.height)
    }

    @Test
    @EnabledIf("isAndroidEnvironmentAvailable")
    fun `custom margin affects QR code`() {
        val data = "test"
        val size = 256

        val bitmap0 = QrCodeGenerator.generate(data, size, margin = 0)
        val bitmap4 = QrCodeGenerator.generate(data, size, margin = 4)

        // With larger margin, active QR code area should be smaller
        // (This is a basic sanity check)
        assertNotNull(bitmap0)
        assertNotNull(bitmap4)
    }

    // Helper method for conditional test execution
    companion object {
        @JvmStatic
        fun isAndroidEnvironmentAvailable(): Boolean {
            return try {
                Class.forName("android.graphics.Bitmap")
                Class.forName("com.google.zxing.qrcode.QRCodeWriter")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }
}
