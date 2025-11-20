package net.meshcore.mineralog.ui.screens.identification.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log

/**
 * Utility object for analyzing images and detecting dominant colors using HSV histogram voting.
 * Used for photo-assisted mineral identification.
 *
 * Algorithm: HSV Histogram Voting
 * - Converts each pixel to HSV color space
 * - Classifies pixel into one of 12 color categories
 * - Returns the color with the most votes (dominant color)
 *
 * This approach is much more accurate than RGB averaging for textured minerals.
 */
object ImageAnalyzer {

    /**
     * Reference RGB values for common mineral colors.
     * These values are used for display purposes and the getColorRgb() helper function.
     */
    private val COLOR_REFERENCES = mapOf(
        "White" to Triple(255, 255, 255),
        "Black" to Triple(0, 0, 0),
        "Gray" to Triple(128, 128, 128),
        "Red" to Triple(220, 20, 60),        // Crimson red
        "Pink" to Triple(255, 192, 203),
        "Orange" to Triple(255, 140, 0),      // Dark orange
        "Yellow" to Triple(255, 215, 0),      // Gold
        "Green" to Triple(34, 139, 34),       // Forest green
        "Blue" to Triple(30, 144, 255),       // Dodger blue
        "Purple" to Triple(138, 43, 226),     // Blue violet
        "Brown" to Triple(139, 69, 19),       // Saddle brown
        "Colorless" to Triple(240, 240, 240)  // Very light gray
    )

    /**
     * Detects the dominant color from a bitmap using HSV histogram voting.
     *
     * Algorithm:
     * 1. Resize bitmap to 100x100 for performance (10,000 pixels to analyze)
     * 2. For each pixel:
     *    - Convert RGB to HSV
     *    - Classify into one of 12 color categories
     *    - Increment vote counter for that color
     * 3. Return the color with the most votes
     *
     * @param bitmap The image bitmap to analyze
     * @return The name of the dominant color (e.g., "Red", "Blue"), NEVER null
     */
    fun detectDominantColorName(inputBitmap: Bitmap): String {
        return try {
            // Convert Hardware Bitmap to Software Bitmap if needed (Android P+)
            // Hardware Bitmaps are stored in GPU memory and cannot be read with getPixel()
            val softwareBitmap = if (inputBitmap.config == Bitmap.Config.HARDWARE) {
                inputBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: inputBitmap
            } else {
                inputBitmap
            }

            // Resize to 100x100 for performance (10,000 pixels)
            val resizedBitmap = Bitmap.createScaledBitmap(softwareBitmap, 100, 100, false)

            // HSV histogram voting
            val colorVotes = mutableMapOf<String, Int>().withDefault { 0 }
            val hsv = FloatArray(3)
            val width = resizedBitmap.width
            val height = resizedBitmap.height
            val pixels = IntArray(width * height)
            resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            for (pixel in pixels) {
                if (pixel == 0) continue // Skip transparent pixels

                Color.colorToHSV(pixel, hsv)
                val s = hsv[1]
                val v = hsv[2]

                // Skip white backgrounds (very bright and desaturated)
                if (s < 0.10f && v > 0.90f) {
                    continue
                }
                // Skip colored studio backgrounds (very saturated and bright)
                if (s > 0.60f && v > 0.85f) {
                    continue
                }

                val category = classifyPixelColor(hsv[0], hsv[1], hsv[2])
                colorVotes[category] = colorVotes.getValue(category) + 1
            }

            // Determine winner (most votes)
            val winner = colorVotes.maxByOrNull { it.value }?.key ?: "Black"

            // Cleanup bitmaps
            if (softwareBitmap != inputBitmap) {
                softwareBitmap.recycle()
            }
            resizedBitmap.recycle()

            winner

        } catch (e: Exception) {
            Log.e("ImageAnalyzer", "Color detection failed", e)
            "Black"
        }
    }

    /**
     * Classifies a pixel into one of the 12 color categories based on HSV values.
     *
     * HSV Color Space:
     * - Hue (H): 0-360° - Color type (Red, Green, Blue, etc.)
     * - Saturation (S): 0-1 - Color purity (0 = gray, 1 = pure color)
     * - Value (V): 0-1 - Brightness (0 = black, 1 = bright)
     *
     * Classification Priority (v3.2.0 - Fixed for Amethyst/Malachite):
     * 1. Achromatic colors (White, Black, Gray) - with reduced thresholds
     * 2. Chromatic colors (Red, Orange, Yellow, Green, Blue, Purple) - prioritized
     * 3. Special cases (Brown, Pink) - only for specific edge cases
     *
     * HSV Coverage: Continuous 0-360° with no gaps
     *
     * @param hue Hue value (0-360°)
     * @param saturation Saturation value (0-1)
     * @param value Brightness value (0-1)
     * @return Color category name (NEVER null)
     */
    private fun classifyPixelColor(hue: Float, saturation: Float, value: Float): String {
        // Priority 1: Achromatic colors (Gemini-optimized thresholds for minerals)

        // White: Very high brightness, very low saturation
        if (value > 0.85f && saturation < 0.10f) {
            return "White"
        }

        // Black: Low brightness AND low saturation (only true black, NOT dark colors)
        // KEY: Dark saturated minerals (Amethyst: value=0.25, sat=0.80) MUST pass through!
        // A pixel is black ONLY if it's dark AND desaturated
        if (value < 0.20f && saturation < 0.30f) {
            return "Black"
        }

        // Gray: Low saturation (desaturated colors at any brightness)
        if (saturation < 0.15f) {
            return "Gray"
        }

        // Colorless (very light gray, almost white)
        if (saturation < 0.08f && value > 0.85f) {
            return "Colorless"
        }

        // Priority 2: Chromatic colors (based on hue ranges - continuous 0-360° coverage)
        // These are checked BEFORE Brown/Pink to ensure minerals like Amethyst/Malachite
        // are classified correctly

        // Red: 0-20° or 340-360°
        if (hue < 20f || hue >= 340f) {
            // Special case: Light, desaturated red may be Pink
            if (value > 0.70f && saturation > 0.15f && saturation < 0.70f) {
                return "Pink"
            }
            return "Red"
        }

        // Orange: 20-45°
        if (hue >= 20f && hue < 45f) {
            // Special case: Dark orange with low brightness may be Brown
            if (value < 0.6f && saturation > 0.2f) {
                return "Brown"
            }
            return "Orange"
        }

        // Yellow: 45-70°
        if (hue >= 45f && hue < 70f) {
            // Special case: Dark yellow with low brightness may be Brown
            if (value < 0.6f && saturation > 0.2f && hue <= 50f) {
                return "Brown"
            }
            return "Yellow"
        }

        // Green: 70-190° (includes cyan for minerals like Malachite, Amazonite)
        if (hue >= 70f && hue < 190f) {
            return "Green"
        }

        // Blue: 190-260° (pure blue for minerals like Azurite, Lapis Lazuli)
        if (hue >= 190f && hue < 260f) {
            return "Blue"
        }

        // Purple: 260-340° (Amethyst should detect here)
        if (hue >= 260f && hue < 340f) {
            // Special case: Light purple with moderate saturation may be Pink
            if (hue >= 300f && value > 0.65f && saturation > 0.20f && saturation < 0.75f) {
                return "Pink"
            }
            return "Purple"
        }

        // Fallback (should never happen with continuous 0-360° coverage)
        return "Gray"
    }

    /**
     * Gets the RGB values for a given color name.
     * Useful for debugging or displaying the reference color.
     *
     * @param colorName The name of the color (e.g., "Red", "Blue")
     * @return Triple of RGB values, or null if color name not found
     */
    fun getColorRgb(colorName: String): Triple<Int, Int, Int>? {
        return COLOR_REFERENCES[colorName]
    }

    /**
     * Returns all available color names that can be detected.
     *
     * @return List of color names
     */
    fun getAvailableColors(): List<String> {
        return COLOR_REFERENCES.keys.toList()
    }

    /**
     * Debug helper: Analyzes a bitmap and returns vote counts for each color.
     * Useful for understanding the color distribution in an image.
     *
     * @param bitmap The image bitmap to analyze
     * @return Map of color names to vote counts, or null if analysis fails
     */
    fun getColorDistribution(bitmap: Bitmap): Map<String, Int>? {
        return try {
            // CRITICAL FIX - Convert Hardware Bitmap to software-readable format
            val softwareBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                // Use Canvas to convert Hardware Bitmap to Software Bitmap (more reliable)
                val width = bitmap.width
                val height = bitmap.height
                val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(newBitmap)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                newBitmap
            } else {
                bitmap
            }

            val resizedBitmap = Bitmap.createScaledBitmap(softwareBitmap, 100, 100, true)
            val colorVotes = mutableMapOf<String, Int>().apply {
                COLOR_REFERENCES.keys.forEach { put(it, 0) }
            }

            val width = resizedBitmap.width
            val height = resizedBitmap.height
            val hsv = FloatArray(3)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixel = resizedBitmap.getPixel(x, y)
                    Color.colorToHSV(pixel, hsv)

                    // Skip background pixels (same as detectDominantColorName)
                    if (hsv[1] < 0.10f && hsv[2] > 0.90f) {
                        continue
                    }
                    if (hsv[1] > 0.60f && hsv[2] > 0.85f) {
                        continue
                    }

                    val colorCategory = classifyPixelColor(hsv[0], hsv[1], hsv[2])
                    colorVotes[colorCategory] = colorVotes[colorCategory]!! + 1
                }
            }

            // Cleanup bitmaps
            if (resizedBitmap != softwareBitmap) {
                resizedBitmap.recycle()
            }
            if (softwareBitmap != bitmap) {
                softwareBitmap.recycle()
            }

            colorVotes
        } catch (e: Exception) {
            null
        }
    }
}
