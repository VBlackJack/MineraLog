package net.meshcore.mineralog.ui.accessibility

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * Check A11y #3: Color Contrast Validator
 *
 * Validates color contrast ratios according to WCAG 2.1 Level AA standards:
 * - Normal text: minimum 4.5:1 contrast ratio
 * - Large text (18pt+ or 14pt+ bold): minimum 3.0:1 contrast ratio
 * - Non-text content: minimum 3.0:1 contrast ratio
 */
object ColorContrastValidator {

    /**
     * Calculate contrast ratio between two colors using WCAG formula
     * https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html
     *
     * Formula: (L1 + 0.05) / (L2 + 0.05)
     * where L1 is the relative luminance of the lighter color
     * and L2 is the relative luminance of the darker color
     */
    fun calculateContrastRatio(foreground: Color, background: Color): Double {
        val luminance1 = foreground.luminance().toDouble() + 0.05
        val luminance2 = background.luminance().toDouble() + 0.05

        val lighter = max(luminance1, luminance2)
        val darker = min(luminance1, luminance2)

        return lighter / darker
    }

    /**
     * Check if contrast ratio meets WCAG AA standard for normal text (4.5:1)
     */
    fun meetsNormalTextStandard(foreground: Color, background: Color): Boolean {
        return calculateContrastRatio(foreground, background) >= 4.5
    }

    /**
     * Check if contrast ratio meets WCAG AA standard for large text (3.0:1)
     */
    fun meetsLargeTextStandard(foreground: Color, background: Color): Boolean {
        return calculateContrastRatio(foreground, background) >= 3.0
    }

    /**
     * Check if contrast ratio meets WCAG AAA standard for normal text (7.0:1)
     */
    fun meetsNormalTextStandardAAA(foreground: Color, background: Color): Boolean {
        return calculateContrastRatio(foreground, background) >= 7.0
    }

    /**
     * Get accessibility level for a color pair
     */
    fun getAccessibilityLevel(foreground: Color, background: Color, isLargeText: Boolean = false): AccessibilityLevel {
        val ratio = calculateContrastRatio(foreground, background)

        return when {
            ratio >= 7.0 -> AccessibilityLevel.AAA
            ratio >= 4.5 && !isLargeText -> AccessibilityLevel.AA
            ratio >= 3.0 && isLargeText -> AccessibilityLevel.AA
            ratio >= 3.0 && !isLargeText -> AccessibilityLevel.AA_LARGE_TEXT_ONLY
            else -> AccessibilityLevel.FAIL
        }
    }

    enum class AccessibilityLevel(val displayName: String) {
        AAA("WCAG AAA"),
        AA("WCAG AA"),
        AA_LARGE_TEXT_ONLY("WCAG AA (Large Text Only)"),
        FAIL("Does not meet WCAG standards")
    }

    /**
     * Validation result with detailed information
     */
    data class ContrastValidationResult(
        val foreground: Color,
        val background: Color,
        val contrastRatio: Double,
        val accessibilityLevel: AccessibilityLevel,
        val meetsAANormalText: Boolean,
        val meetsAALargeText: Boolean,
        val meetsAAA: Boolean
    ) {
        override fun toString(): String {
            return """
                Contrast Ratio: ${"%.2f".format(contrastRatio)}:1
                Accessibility Level: ${accessibilityLevel.displayName}
                WCAG AA (Normal Text ≥4.5:1): ${if (meetsAANormalText) "✓ PASS" else "✗ FAIL"}
                WCAG AA (Large Text ≥3.0:1): ${if (meetsAALargeText) "✓ PASS" else "✗ FAIL"}
                WCAG AAA (≥7.0:1): ${if (meetsAAA) "✓ PASS" else "✗ FAIL"}
            """.trimIndent()
        }
    }

    /**
     * Validate contrast with detailed results
     */
    fun validate(foreground: Color, background: Color, isLargeText: Boolean = false): ContrastValidationResult {
        val ratio = calculateContrastRatio(foreground, background)
        return ContrastValidationResult(
            foreground = foreground,
            background = background,
            contrastRatio = ratio,
            accessibilityLevel = getAccessibilityLevel(foreground, background, isLargeText),
            meetsAANormalText = ratio >= 4.5,
            meetsAALargeText = ratio >= 3.0,
            meetsAAA = ratio >= 7.0
        )
    }
}
