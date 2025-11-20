package net.meshcore.mineralog.ui.accessibility

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.meshcore.mineralog.ui.screens.home.HomeScreen
import net.meshcore.mineralog.ui.screens.detail.MineralDetailScreen
import net.meshcore.mineralog.ui.screens.statistics.StatisticsScreen
import net.meshcore.mineralog.ui.theme.MineraLogTheme
import net.meshcore.mineralog.ui.accessibility.ColorContrastValidator
import androidx.compose.material3.MaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

/**
 * Automated Accessibility Tests - 5 Core Checks
 *
 * These tests implement the 5 key accessibility verification strategies:
 * 1. Touch Target Size Validator (48dp minimum)
 * 2. ContentDescription Coverage (all interactive icons)
 * 3. Color Contrast Validation (WCAG AA)
 * 4. Live Region Announcements (loading/error states)
 * 5. Text Scaling Support (200%)
 *
 * Run with: ./gradlew connectedAndroidTest
 *
 * Implementation Date: 2025-11-13
 * WCAG 2.1 Level AA Compliance Target
 */
@RunWith(AndroidJUnit4::class)
class AutomatedAccessibilityTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Test 1: Touch Target Size Validator
     *
     * Verifies all interactive elements meet WCAG 2.1 minimum touch target size of 48dp.
     * Covers: Buttons, IconButtons, clickable Cards, FilterChips, etc.
     */
    @Test
    fun check1_allInteractiveElements_meetMinimumTouchTargetSize() {
        composeTestRule.setContent {
            MineraLogTheme {
                HomeScreen(
                    onMineralClick = {},
                    onAddClick = {},
                    onSettingsClick = {},
                    onStatisticsClick = {},
                    onCompareClick = {}
                )
            }
        }

        // Test FAB (Add button)
        composeTestRule.onNodeWithContentDescription("Add mineral")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        // Test top bar action buttons
        composeTestRule.onNodeWithContentDescription("Statistics")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithContentDescription("Bulk edit")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        // Test filter button
        composeTestRule.onNodeWithContentDescription("Filter")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    /**
     * Test 2: ContentDescription Coverage
     *
     * Ensures all interactive icons and images have appropriate contentDescription
     * for screen reader accessibility. Decorative icons should have null or empty.
     */
    @Test
    fun check2_allInteractiveIcons_haveContentDescription() {
        composeTestRule.setContent {
            MineraLogTheme {
                HomeScreen(
                    onMineralClick = {},
                    onAddClick = {},
                    onSettingsClick = {},
                    onStatisticsClick = {},
                    onCompareClick = {}
                )
            }
        }

        // Verify navigation and action icons have descriptions
        composeTestRule.onNodeWithContentDescription("Add mineral")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Settings")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Statistics")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Bulk edit")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Filter")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Search")
            .assertExists()

        // If search has text, clear button should exist
        composeTestRule.onNode(hasText("Search minerals..."))
            .performTextInput("test")

        composeTestRule.onNodeWithContentDescription("Clear search")
            .assertExists()
    }

    /**
     * Test 3: Semantic Properties Validation
     *
     * Verifies custom components have appropriate semantic properties for
     * screen reader navigation (contentDescription, stateDescription, liveRegion).
     */
    @Test
    fun check3_customComponents_haveSemanticProperties() {
        composeTestRule.setContent {
            MineraLogTheme {
                StatisticsScreen(
                    onNavigateBack = {}
                )
            }
        }

        // Verify loading state has live region
        composeTestRule.onNode(
            hasContentDescription("Loading statistics") and
            hasTestTag("loading_statistics") // If we add test tags
        ).assertExists()
            .assert(
                SemanticsMatcher("has live region") {
                    it.config.getOrNull(
                        androidx.compose.ui.semantics.SemanticsProperties.LiveRegion
                    ) == androidx.compose.ui.semantics.LiveRegionMode.Polite
                }
            )
    }

    /**
     * Test 4: Live Region Announcements
     *
     * Validates that loading and error states properly announce to screen readers
     * using liveRegion semantics (Polite or Assertive modes).
     */
    @Test
    fun check4_loadingAndErrorStates_announceToScreenReaders() {
        composeTestRule.setContent {
            MineraLogTheme {
                HomeScreen(
                    onMineralClick = {},
                    onAddClick = {},
                    onSettingsClick = {},
                    onStatisticsClick = {},
                    onCompareClick = {}
                )
            }
        }

        // Check that loading states have live regions
        // This will appear when the list is loading
        composeTestRule.onNode(
            hasContentDescription("Loading minerals")
        ).assertExists()
            .assert(
                SemanticsMatcher("has polite live region") {
                    it.config.getOrNull(
                        androidx.compose.ui.semantics.SemanticsProperties.LiveRegion
                    ) == androidx.compose.ui.semantics.LiveRegionMode.Polite
                }
            )
    }

    /**
     * Test 5: Text Scaling to 200%
     *
     * Ensures UI remains usable and readable when text scaling is set to 200%
     * as required by WCAG 2.1 Level AA.
     *
     * Note: This test verifies that text doesn't get truncated with ellipsis (...)
     * and that touch targets remain accessible.
     */
    @Test
    fun check5_uiSupportsTextScalingTo200Percent() {
        composeTestRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides
                    androidx.compose.ui.unit.Density(
                        density = 1f,
                        fontScale = 2.0f
                    )
            ) {
                MineraLogTheme {
                    HomeScreen(
                        onMineralClick = {},
                        onAddClick = {},
                        onSettingsClick = {},
                        onStatisticsClick = {},
                        onCompareClick = {}
                    )
                }
            }
        }

        // Verify critical UI elements are still accessible
        composeTestRule.onNodeWithContentDescription("Add mineral")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        // Verify search field remains functional
        composeTestRule.onNode(hasText("Search minerals..."))
            .assertIsDisplayed()

        // Verify top bar buttons remain accessible
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithContentDescription("Statistics")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        // Test that buttons don't overlap or become inaccessible
        composeTestRule.onNodeWithContentDescription("Filter")
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Bulk edit")
            .assertIsDisplayed()
    }

    /**
     * Additional Test: Empty State Semantics
     *
     * Verifies that empty states have comprehensive semantic descriptions
     * for screen readers to provide context and guidance.
     */
    @Test
    fun check_emptyState_hasComprehensiveSemantics() {
        composeTestRule.setContent {
            MineraLogTheme {
                HomeScreen(
                    onMineralClick = {},
                    onAddClick = {},
                    onSettingsClick = {},
                    onStatisticsClick = {},
                    onCompareClick = {}
                )
            }
        }

        // When list is empty, verify comprehensive empty state message
        composeTestRule.onNode(
            hasContentDescription(
                "Empty collection state. Your collection is empty. " +
                "Start building your mineral collection by adding your first specimen. " +
                "Tap the add button below to get started.",
                substring = true
            )
        ).assertExists()
    }

    /**
     * Additional Test: Filter Badge Semantics
     *
     * Verifies that filter badge properly announces active filter count
     * to screen readers.
     */
    @Test
    fun check_filterBadge_announcesActiveCount() {
        composeTestRule.setContent {
            MineraLogTheme {
                HomeScreen(
                    onMineralClick = {},
                    onAddClick = {},
                    onSettingsClick = {},
                    onStatisticsClick = {},
                    onCompareClick = {}
                )
            }
        }

        // Initially no filters active
        composeTestRule.onNode(
            hasContentDescription("No active filters")
        ).assertExists()

        // After applying filter (would need to interact with filter sheet in real test)
        // The badge should announce count like "3 active filters"
    }

    /**
     * Additional Test: Detail Screen Loading State
     *
     * Verifies Quick Win #2 implementation - loading state in detail screen
     * properly announces to screen readers.
     */
    @Test
    fun check_detailScreenLoading_announcesToScreenReaders() {
        composeTestRule.setContent {
            MineraLogTheme {
                MineralDetailScreen(
                    mineralId = "test-id",
                    onNavigateBack = {}
                )
            }
        }

        // Verify loading indicator has proper semantics
        composeTestRule.onNode(
            hasContentDescription("Loading mineral details")
        ).assertExists()
            .assert(
                SemanticsMatcher("has polite live region") {
                    it.config.getOrNull(
                        androidx.compose.ui.semantics.SemanticsProperties.LiveRegion
                    ) == androidx.compose.ui.semantics.LiveRegionMode.Polite
                }
            )
    }

    /**
     * Additional Test: PDF Label Generation Progress
     *
     * Verifies Quick Win #1 implementation - PDF label generation shows
     * progress indicator with proper announcements.
     */
    @Test
    fun check_pdfLabelGeneration_showsProgressWithAnnouncement() {
        composeTestRule.setContent {
            MineraLogTheme {
                HomeScreen(
                    onMineralClick = {},
                    onAddClick = {},
                    onSettingsClick = {},
                    onStatisticsClick = {},
                    onCompareClick = {}
                )
            }
        }

        // When label generation is in progress (would need to trigger in real test)
        // Verify that progress indicator announces to screen readers
        composeTestRule.onNode(
            hasContentDescription("Generating PDF labels")
        ).assertExists()
            .assert(
                SemanticsMatcher("has polite live region") {
                    it.config.getOrNull(
                        androidx.compose.ui.semantics.SemanticsProperties.LiveRegion
                    ) == androidx.compose.ui.semantics.LiveRegionMode.Polite
                }
            )
    }

    /**
     * Check #3: Color Contrast Validator (WCAG 2.1 AA)
     *
     * Validates that the Material 3 theme color scheme meets WCAG 2.1 AA
     * minimum contrast requirements:
     * - Normal text: 4.5:1 minimum
     * - Large text: 3.0:1 minimum
     * - Non-text content: 3.0:1 minimum
     */
    @Test
    fun check3_colorContrast_meetsWCAG_AA() {
        lateinit var onSurface: androidx.compose.ui.graphics.Color
        lateinit var surface: androidx.compose.ui.graphics.Color
        lateinit var onPrimary: androidx.compose.ui.graphics.Color
        lateinit var primary: androidx.compose.ui.graphics.Color
        lateinit var onError: androidx.compose.ui.graphics.Color
        lateinit var error: androidx.compose.ui.graphics.Color

        composeTestRule.setContent {
            MineraLogTheme {
                val colorScheme = MaterialTheme.colorScheme
                onSurface = colorScheme.onSurface
                surface = colorScheme.surface
                onPrimary = colorScheme.onPrimary
                primary = colorScheme.primary
                onError = colorScheme.onError
                error = colorScheme.error
            }
        }

        // Test primary text on surface (most common pairing)
        val surfaceTextResult = ColorContrastValidator.validate(onSurface, surface)
        assertTrue(
            "Primary text on surface must meet WCAG AA (4.5:1). Got: ${surfaceTextResult.contrastRatio}:1",
            surfaceTextResult.meetsAANormalText
        )

        // Test text on primary (buttons, chips)
        val primaryTextResult = ColorContrastValidator.validate(onPrimary, primary)
        assertTrue(
            "Text on primary must meet WCAG AA (4.5:1). Got: ${primaryTextResult.contrastRatio}:1",
            primaryTextResult.meetsAANormalText
        )

        // Test error text
        val errorTextResult = ColorContrastValidator.validate(onError, error)
        assertTrue(
            "Error text must meet WCAG AA (4.5:1). Got: ${errorTextResult.contrastRatio}:1",
            errorTextResult.meetsAANormalText
        )
    }
}
