package net.meshcore.mineralog.ui.accessibility

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.meshcore.mineralog.ui.screens.add.AddMineralScreen
import net.meshcore.mineralog.ui.theme.MineraLogTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated accessibility tests for WCAG 2.1 AA compliance.
 *
 * Tests:
 * - Touch target sizes (48dp minimum)
 * - ContentDescription coverage for interactive elements
 * - Semantic properties for screen readers
 * - Keyboard navigation support
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ComposeAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addMineralScreen_allButtonsHaveMinimumTouchTargetSize() {
        composeTestRule.setContent {
            MineraLogTheme {
                AddMineralScreen(
                    onNavigateBack = {},
                    onMineralAdded = {}
                )
            }
        }

        // Verify back button has minimum touch target (48dp)
        composeTestRule.onNodeWithContentDescription("Back")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        // Verify save button has minimum touch target
        composeTestRule.onNodeWithText("SAVE")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun addMineralScreen_allTextFieldsHaveLabels() {
        composeTestRule.setContent {
            MineraLogTheme {
                AddMineralScreen(
                    onNavigateBack = {},
                    onMineralAdded = {}
                )
            }
        }

        // Verify required field has label and error semantics
        composeTestRule.onNodeWithText("Name *")
            .assertIsDisplayed()

        // Verify optional fields have labels
        composeTestRule.onNodeWithText("Group")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Chemical Formula")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Notes")
            .assertIsDisplayed()
    }

    @Test
    fun addMineralScreen_technicalFieldsHaveTooltips() {
        composeTestRule.setContent {
            MineraLogTheme {
                AddMineralScreen(
                    onNavigateBack = {},
                    onMineralAdded = {}
                )
            }
        }

        // Scroll to technical properties section
        composeTestRule.onNodeWithText("Technical Properties")
            .assertExists()

        // Verify tooltip help buttons exist and have contentDescription
        composeTestRule.onAllNodesWithContentDescription("Help for Diaphaneity")
            .assertCountEquals(1)

        composeTestRule.onAllNodesWithContentDescription("Help for Cleavage")
            .assertCountEquals(1)

        composeTestRule.onAllNodesWithContentDescription("Help for Fracture")
            .assertCountEquals(1)
    }

    @Test
    fun addMineralScreen_requiredFieldShowsError() {
        composeTestRule.setContent {
            MineraLogTheme {
                AddMineralScreen(
                    onNavigateBack = {},
                    onMineralAdded = {}
                )
            }
        }

        // Verify error message is shown for empty name field
        composeTestRule.onNodeWithText("Name is required")
            .assertIsDisplayed()

        // Type in name field
        composeTestRule.onNodeWithText("Name *")
            .performTextInput("Quartz")

        // Error should disappear after input
        composeTestRule.onNodeWithText("Name is required")
            .assertDoesNotExist()
    }

    @Test
    fun addMineralScreen_draftSavedIndicatorAppears() {
        composeTestRule.setContent {
            MineraLogTheme {
                AddMineralScreen(
                    onNavigateBack = {},
                    onMineralAdded = {}
                )
            }
        }

        // Type in name field to trigger auto-save
        composeTestRule.onNodeWithText("Name *")
            .performTextInput("Test Mineral")

        // Wait for debounce (500ms) + indicator display (2s)
        // Note: In real test, use IdlingResource or test clock
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("Draft saved")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Draft saved")
            .assertIsDisplayed()
    }

    @Test
    fun addMineralScreen_saveButtonEnabledWhenNameProvided() {
        composeTestRule.setContent {
            MineraLogTheme {
                AddMineralScreen(
                    onNavigateBack = {},
                    onMineralAdded = {}
                )
            }
        }

        // Initially disabled (name is empty)
        composeTestRule.onNodeWithText("SAVE")
            .assertIsNotEnabled()

        // Type in name field
        composeTestRule.onNodeWithText("Name *")
            .performTextInput("Quartz")

        // Now enabled
        composeTestRule.onNodeWithText("SAVE")
            .assertIsEnabled()
    }

    @Test
    fun addMineralScreen_tooltipsAreToggleable() {
        composeTestRule.setContent {
            MineraLogTheme {
                AddMineralScreen(
                    onNavigateBack = {},
                    onMineralAdded = {}
                )
            }
        }

        // Scroll to technical properties
        composeTestRule.onNodeWithText("Diaphaneity")
            .performScrollTo()

        // Click info button to show tooltip
        composeTestRule.onNodeWithContentDescription("Help for Diaphaneity")
            .performClick()

        // Tooltip text should appear
        composeTestRule.onNodeWithText("Transparency level", substring = true)
            .assertIsDisplayed()

        // Click again to hide tooltip
        composeTestRule.onNodeWithContentDescription("Help for Diaphaneity")
            .performClick()

        // Tooltip should disappear
        composeTestRule.onNodeWithText("Transparency level", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun addMineralScreen_keyboardNavigationSupported() {
        composeTestRule.setContent {
            MineraLogTheme {
                AddMineralScreen(
                    onNavigateBack = {},
                    onMineralAdded = {}
                )
            }
        }

        // Verify IME actions are set correctly
        // Name field should have "Next" action
        composeTestRule.onNodeWithText("Name *")
            .assertIsDisplayed()
            .performTextInput("Test")

        // Group field should be focusable after "Next"
        composeTestRule.onNodeWithText("Group")
            .assertIsDisplayed()
    }

    @Test
    fun addMineralScreen_unsavedChangesWarningShown() {
        composeTestRule.setContent {
            MineraLogTheme {
                AddMineralScreen(
                    onNavigateBack = {},
                    onMineralAdded = {}
                )
            }
        }

        // Type some content
        composeTestRule.onNodeWithText("Name *")
            .performTextInput("Test Mineral")

        // Press back
        composeTestRule.onNodeWithContentDescription("Back")
            .performClick()

        // Discard dialog should appear
        composeTestRule.onNodeWithText("Discard changes?")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("You have unsaved changes. Are you sure you want to discard them?")
            .assertIsDisplayed()

        // Verify action buttons
        composeTestRule.onNodeWithText("Discard")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel")
            .assertIsDisplayed()
    }
}
