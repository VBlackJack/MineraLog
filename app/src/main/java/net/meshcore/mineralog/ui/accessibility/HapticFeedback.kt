package net.meshcore.mineralog.ui.accessibility

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Haptic feedback utility for accessibility and improved UX.
 *
 * Provides tactile feedback for critical actions, helping users with
 * visual impairments confirm their actions were registered.
 *
 * WCAG 2.1 Guideline 3.2: Make Web pages appear and operate in predictable ways.
 * Haptic feedback provides consistent, predictable confirmation of actions.
 */
object HapticFeedback {

    /**
     * Light haptic feedback for standard interactions.
     * Use for: button clicks, list item selection, checkbox toggles.
     */
    fun performLight(view: View?) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
    }

    /**
     * Medium haptic feedback for important actions.
     * Use for: form submissions, confirmations, navigation.
     */
    fun performMedium(view: View?) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
    }

    /**
     * Strong haptic feedback for critical actions.
     * Use for: delete operations, error confirmations, critical warnings.
     */
    fun performStrong(view: View?) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
    }

    /**
     * Success haptic feedback.
     * Use for: successful saves, completions, confirmations.
     */
    fun performSuccess(view: View?) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.CONFIRM,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
    }

    /**
     * Reject haptic feedback.
     * Use for: validation errors, rejected actions, warnings.
     */
    fun performReject(view: View?) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.REJECT,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
    }
}

/**
 * Composable hook to get haptic feedback helper.
 *
 * Usage:
 * ```kotlin
 * val haptic = rememberHaptic()
 *
 * Button(
 *     onClick = {
 *         haptic.performStrong()
 *         deleteMineral()
 *     }
 * ) {
 *     Text("Delete")
 * }
 * ```
 */
@Composable
fun rememberHaptic(): HapticHelper {
    val view = LocalView.current
    return remember(view) {
        HapticHelper(view)
    }
}

/**
 * Helper class for haptic feedback in Composables.
 */
class HapticHelper(private val view: View?) {
    fun performLight() = HapticFeedback.performLight(view)
    fun performMedium() = HapticFeedback.performMedium(view)
    fun performStrong() = HapticFeedback.performStrong(view)
    fun performSuccess() = HapticFeedback.performSuccess(view)
    fun performReject() = HapticFeedback.performReject(view)
}

/**
 * Example usage scenarios:
 *
 * ```kotlin
 * // Delete action (critical)
 * IconButton(
 *     onClick = {
 *         haptic.performStrong()
 *         showDeleteDialog = true
 *     }
 * ) {
 *     Icon(Icons.Default.Delete, "Delete")
 * }
 *
 * // Form submission (important)
 * Button(
 *     onClick = {
 *         haptic.performMedium()
 *         submitForm()
 *     }
 * ) {
 *     Text("Submit")
 * }
 *
 * // Save success (confirmation)
 * LaunchedEffect(saveState) {
 *     if (saveState is SaveState.Success) {
 *         haptic.performSuccess()
 *     }
 * }
 *
 * // Validation error (rejection)
 * LaunchedEffect(validationError) {
 *     if (validationError != null) {
 *         haptic.performReject()
 *     }
 * }
 * ```
 */
