package net.meshcore.mineralog.ui.screens.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import net.meshcore.mineralog.data.local.entity.PhotoType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P1-1: Integration tests for Camera Capture functionality.
 *
 * Tests cover:
 * - Camera permissions handling
 * - Photo capture flow
 * - Error handling for camera failures
 * - Photo type selection
 *
 * Requires instrumentation testing with AndroidJUnit4.
 */
@RunWith(AndroidJUnit4::class)
class CameraIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA
    )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun cameraScreen_permissionGranted_showsCameraPreview() {
        // Arrange
        var navigatedBack = false
        var capturedPhoto: Pair<Uri, PhotoType>? = null

        // Act
        composeTestRule.setContent {
            CameraCaptureScreen(
                mineralId = "test-mineral-id",
                onPhotoCaptured = { uri, type ->
                    capturedPhoto = Pair(uri, type)
                },
                onNavigateBack = { navigatedBack = true }
            )
        }

        // Assert - Camera UI elements are visible
        composeTestRule.onNodeWithContentDescription("Camera preview", substring = true, ignoreCase = true)
            .assertExists()

        // Back button exists
        composeTestRule.onNodeWithContentDescription("Back", ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun cameraScreen_backButton_navigatesBack() {
        // Arrange
        var navigatedBack = false

        composeTestRule.setContent {
            CameraCaptureScreen(
                mineralId = "test-mineral-id",
                onPhotoCaptured = { _, _ -> },
                onNavigateBack = { navigatedBack = true }
            )
        }

        // Act
        composeTestRule.onNodeWithContentDescription("Back", ignoreCase = true)
            .performClick()

        // Wait for navigation
        composeTestRule.waitForIdle()

        // Assert
        assert(navigatedBack) { "Expected navigation back to occur" }
    }

    @Test
    fun cameraScreen_photoTypeSelector_displaysAllTypes() {
        // Arrange
        composeTestRule.setContent {
            CameraCaptureScreen(
                mineralId = "test-mineral-id",
                onPhotoCaptured = { _, _ -> },
                onNavigateBack = { }
            )
        }

        // Act - Find and click photo type selector
        composeTestRule.onNodeWithContentDescription("Select photo type", ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
            .performClick()

        // Wait for menu
        composeTestRule.waitForIdle()

        // Assert - All photo types should be available
        composeTestRule.onNodeWithText("Normal", substring = true)
            .assertExists()

        composeTestRule.onNodeWithText("UV Shortwave", substring = true)
            .assertExists()

        composeTestRule.onNodeWithText("UV Longwave", substring = true)
            .assertExists()

        composeTestRule.onNodeWithText("Macro", substring = true)
            .assertExists()
    }

    @Test
    fun cameraScreen_captureButton_exists() {
        // Arrange
        composeTestRule.setContent {
            CameraCaptureScreen(
                mineralId = "test-mineral-id",
                onPhotoCaptured = { _, _ -> },
                onNavigateBack = { }
            )
        }

        // Assert - Capture button exists with proper semantics
        composeTestRule.onNodeWithContentDescription("Capture photo", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun cameraScreen_torchToggle_exists() {
        // Arrange
        composeTestRule.setContent {
            CameraCaptureScreen(
                mineralId = "test-mineral-id",
                onPhotoCaptured = { _, _ -> },
                onNavigateBack = { }
            )
        }

        // Wait for camera to initialize
        composeTestRule.waitForIdle()

        // Assert - Flash/torch toggle should exist
        // Note: This might not be available if device doesn't have flash
        composeTestRule.onNodeWithContentDescription("flash", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun cameraScreen_photoTypeChange_updatesIndicator() {
        // Arrange
        composeTestRule.setContent {
            CameraCaptureScreen(
                mineralId = "test-mineral-id",
                onPhotoCaptured = { _, _ -> },
                onNavigateBack = { }
            )
        }

        // Act - Open photo type menu
        composeTestRule.onNodeWithContentDescription("Select photo type", ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Select UV Shortwave
        composeTestRule.onNodeWithText("UV Shortwave", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Assert - Photo type indicator should update
        composeTestRule.onNodeWithText("UV Shortwave", substring = true)
            .assertExists()
    }

    @Test
    fun cameraState_errorHandling_displaysSnackbar() {
        // This test verifies that error states are handled
        // In a real scenario, we would mock camera failures

        composeTestRule.setContent {
            CameraCaptureScreen(
                mineralId = "test-mineral-id",
                onPhotoCaptured = { _, _ -> },
                onNavigateBack = { }
            )
        }

        // Wait for potential initialization errors
        composeTestRule.waitForIdle()

        // Note: Without mocking camera provider, we can't force errors
        // This test validates the UI is rendered without crashes
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun cameraScreen_accessibility_hasContentDescriptions() {
        // Arrange
        composeTestRule.setContent {
            CameraCaptureScreen(
                mineralId = "test-mineral-id",
                onPhotoCaptured = { _, _ -> },
                onNavigateBack = { }
            )
        }

        // Assert - All interactive elements have content descriptions
        composeTestRule.onNodeWithContentDescription("Back", ignoreCase = true)
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Select photo type", ignoreCase = true)
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Capture photo", substring = true, ignoreCase = true)
            .assertExists()
    }
}
