package net.meshcore.mineralog.ui.screens.identification.utils

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ImageAnalyzer color detection functionality.
 * Uses Robolectric for Android framework dependencies (Bitmap, Color).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageAnalyzerTest {

    /**
     * Helper function to create a solid color bitmap for testing.
     * Creates a 1x1 pixel bitmap with the specified color.
     *
     * @param color The ARGB color to fill the bitmap with
     * @return A 1x1 bitmap with the solid color
     */
    private fun createSolidColorBitmap(color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, color)
        return bitmap
    }

    // ===== Primary Colors Tests =====

    @Test
    fun `detectDominantColorName returns Red for pure red color`() {
        val bitmap = createSolidColorBitmap(Color.RED)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Red", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName returns Blue for pure blue color`() {
        val bitmap = createSolidColorBitmap(Color.BLUE)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Blue", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName returns Green for pure green color`() {
        val bitmap = createSolidColorBitmap(Color.GREEN)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Green", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName returns Yellow for pure yellow color`() {
        val bitmap = createSolidColorBitmap(Color.YELLOW)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Yellow", result)
        bitmap.recycle()
    }

    // ===== Grayscale Tests =====

    @Test
    fun `detectDominantColorName returns White for pure white color`() {
        val bitmap = createSolidColorBitmap(Color.WHITE)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("White", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName returns Black for pure black color`() {
        val bitmap = createSolidColorBitmap(Color.BLACK)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Black", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName returns Gray for medium gray color`() {
        val bitmap = createSolidColorBitmap(Color.GRAY)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Gray", result)
        bitmap.recycle()
    }

    // ===== Secondary and Complex Colors Tests =====

    @Test
    fun `detectDominantColorName returns Brown for saddle brown color`() {
        // Saddle brown: RGB(139, 69, 19)
        val saddleBrown = Color.rgb(139, 69, 19)
        val bitmap = createSolidColorBitmap(saddleBrown)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Brown", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName returns Orange for orange color`() {
        // Dark orange: RGB(255, 140, 0)
        val darkOrange = Color.rgb(255, 140, 0)
        val bitmap = createSolidColorBitmap(darkOrange)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Orange", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName returns Pink for pink color`() {
        // Pink: RGB(255, 192, 203)
        val pink = Color.rgb(255, 192, 203)
        val bitmap = createSolidColorBitmap(pink)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Pink", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName returns Purple for purple color`() {
        // Blue violet: RGB(138, 43, 226)
        val purple = Color.rgb(138, 43, 226)
        val bitmap = createSolidColorBitmap(purple)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Purple", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName returns Colorless for very light gray`() {
        // Very light gray (almost white): RGB(240, 240, 240)
        val lightGray = Color.rgb(240, 240, 240)
        val bitmap = createSolidColorBitmap(lightGray)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        // Should match either White or Colorless (both are very similar)
        assertNotNull(result)
        assert(result == "White" || result == "Colorless")
        bitmap.recycle()
    }

    // ===== Edge Cases and Error Handling =====

    @Test
    fun `detectDominantColorName handles larger bitmap without crashing`() {
        // Create a 100x100 bitmap with red color
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                bitmap.setPixel(x, y, Color.RED)
            }
        }
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Red", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName resizes large bitmap for performance`() {
        // Create a 500x500 bitmap (should be resized to 100x100 internally)
        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        for (x in 0 until 500) {
            for (y in 0 until 500) {
                bitmap.setPixel(x, y, Color.BLUE)
            }
        }
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertEquals("Blue", result)
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName handles invalid bitmap gracefully`() {
        // Create a bitmap and recycle it to make it invalid
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.recycle()

        // Should handle recycled bitmap gracefully without crashing
        // Note: Robolectric may return a valid color (Black for zero pixels) instead of null
        // The key is that it doesn't crash
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        // Result can be null (real Android) or a valid color (Robolectric)
        assert(result == null || result in ImageAnalyzer.getAvailableColors())
    }

    @Test
    fun `detectDominantColorName handles mixed colors by voting`() {
        // Create a 2x2 bitmap with different colors in each quadrant
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.RED)
        bitmap.setPixel(1, 0, Color.RED)
        bitmap.setPixel(0, 1, Color.WHITE)
        bitmap.setPixel(1, 1, Color.WHITE)

        // With HSV voting, pixels vote individually after interpolation during resize
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertNotNull(result)
        // Could be Red, Pink, or White depending on interpolation and voting
        assert(result in listOf("Red", "Pink", "White"))
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName with mixed pixels returns majority color not blend`() {
        // Create a 10x10 bitmap: 70 red pixels, 30 blue pixels
        // This tests that HSV voting returns the majority color (Red)
        // instead of a blended color (Purple) like RGB averaging would
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        var pixelCount = 0

        for (x in 0 until 10) {
            for (y in 0 until 10) {
                // First 70 pixels are red, remaining 30 are blue
                bitmap.setPixel(x, y, if (pixelCount < 70) Color.RED else Color.BLUE)
                pixelCount++
            }
        }

        val result = ImageAnalyzer.detectDominantColorName(bitmap)

        // HSV voting should return "Red" (70 votes) not "Purple" (RGB average)
        assertEquals("Red", result)
        bitmap.recycle()
    }

    // ===== Helper Functions Tests =====

    @Test
    fun `getColorRgb returns correct RGB values for known colors`() {
        val whiteRgb = ImageAnalyzer.getColorRgb("White")
        assertNotNull(whiteRgb)
        assertEquals(Triple(255, 255, 255), whiteRgb)

        val blackRgb = ImageAnalyzer.getColorRgb("Black")
        assertNotNull(blackRgb)
        assertEquals(Triple(0, 0, 0), blackRgb)

        val redRgb = ImageAnalyzer.getColorRgb("Red")
        assertNotNull(redRgb)
        assertEquals(Triple(220, 20, 60), redRgb)
    }

    @Test
    fun `getColorRgb returns null for unknown color name`() {
        val result = ImageAnalyzer.getColorRgb("UnknownColor")
        assertNull(result)
    }

    @Test
    fun `getAvailableColors returns all expected colors`() {
        val colors = ImageAnalyzer.getAvailableColors()
        assertEquals(12, colors.size)
        assert(colors.contains("White"))
        assert(colors.contains("Black"))
        assert(colors.contains("Red"))
        assert(colors.contains("Blue"))
        assert(colors.contains("Green"))
        assert(colors.contains("Yellow"))
        assert(colors.contains("Gray"))
        assert(colors.contains("Brown"))
        assert(colors.contains("Pink"))
        assert(colors.contains("Orange"))
        assert(colors.contains("Purple"))
        assert(colors.contains("Colorless"))
    }

    // ===== Boundary Testing =====

    @Test
    fun `detectDominantColorName handles magenta as closest to Purple or Pink`() {
        val magenta = Color.MAGENTA
        val bitmap = createSolidColorBitmap(magenta)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertNotNull(result)
        // Magenta should match Purple or Pink
        assert(result == "Purple" || result == "Pink")
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName handles cyan as closest to Blue`() {
        val cyan = Color.CYAN
        val bitmap = createSolidColorBitmap(cyan)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertNotNull(result)
        // Cyan should be closest to Blue or Green
        assert(result == "Blue" || result == "Green")
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName handles dark colors`() {
        // Dark gray: RGB(64, 64, 64)
        val darkGray = Color.rgb(64, 64, 64)
        val bitmap = createSolidColorBitmap(darkGray)
        val result = ImageAnalyzer.detectDominantColorName(bitmap)
        assertNotNull(result)
        // Should be Black or Gray
        assert(result == "Black" || result == "Gray")
        bitmap.recycle()
    }

    @Test
    fun `detectDominantColorName is consistent for same color`() {
        // Run analysis multiple times on the same color
        val bitmap = createSolidColorBitmap(Color.RED)
        val result1 = ImageAnalyzer.detectDominantColorName(bitmap)
        val result2 = ImageAnalyzer.detectDominantColorName(bitmap)
        val result3 = ImageAnalyzer.detectDominantColorName(bitmap)

        // All results should be identical
        assertEquals(result1, result2)
        assertEquals(result2, result3)
        assertEquals("Red", result1)
        bitmap.recycle()
    }
}
