package net.meshcore.mineralog.ui.accessibility

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Utility for respecting user's reduced motion accessibility preferences.
 *
 * WCAG 2.1 Success Criterion 2.3.3 (Level AAA):
 * Motion animation triggered by interaction can be disabled,
 * unless the animation is essential to the functionality or
 * the information being conveyed.
 */
object ReducedMotionUtil {

    /**
     * Check if user has enabled reduced motion in system settings.
     *
     * @param context Android context
     * @return true if reduced motion is enabled
     */
    fun isReducedMotionEnabled(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get appropriate animation duration based on reduced motion preference.
     *
     * @param context Android context
     * @param normalDurationMs Normal animation duration in milliseconds
     * @return Reduced duration (50ms) if reduced motion enabled, otherwise normal duration
     */
    fun getAnimationDuration(context: Context, normalDurationMs: Int): Int {
        return if (isReducedMotionEnabled(context)) {
            50 // Minimal animation for state feedback
        } else {
            normalDurationMs
        }
    }

    /**
     * Get appropriate spring stiffness based on reduced motion preference.
     *
     * @param context Android context
     * @return Very high stiffness (quick) if reduced motion, otherwise normal stiffness
     */
    fun getSpringStiffness(context: Context): Float {
        return if (isReducedMotionEnabled(context)) {
            Spring.StiffnessHigh // Quick, minimal bounce
        } else {
            Spring.StiffnessMedium // Normal spring animation
        }
    }
}

/**
 * Composable hook to check reduced motion preference.
 *
 * Usage:
 * ```kotlin
 * val reducedMotion = rememberReducedMotion()
 * AnimatedVisibility(
 *     visible = isVisible,
 *     enter = if (reducedMotion) fadeIn(tween(50)) else slideInHorizontally()
 * ) { ... }
 * ```
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        ReducedMotionUtil.isReducedMotionEnabled(context)
    }
}

/**
 * Get accessible enter transition based on reduced motion preference.
 *
 * @param reducedMotion Whether reduced motion is enabled
 * @param normalTransition Normal enter transition (default: slide + fade)
 * @return Reduced or normal transition
 */
fun accessibleEnterTransition(
    reducedMotion: Boolean,
    normalTransition: EnterTransition = slideInHorizontally() + fadeIn()
): EnterTransition {
    return if (reducedMotion) {
        fadeIn(animationSpec = tween(50))
    } else {
        normalTransition
    }
}

/**
 * Get accessible exit transition based on reduced motion preference.
 *
 * @param reducedMotion Whether reduced motion is enabled
 * @param normalTransition Normal exit transition (default: slide + fade)
 * @return Reduced or normal transition
 */
fun accessibleExitTransition(
    reducedMotion: Boolean,
    normalTransition: ExitTransition = slideOutHorizontally() + fadeOut()
): ExitTransition {
    return if (reducedMotion) {
        fadeOut(animationSpec = tween(50))
    } else {
        normalTransition
    }
}

/**
 * Get accessible vertical expand transition.
 *
 * @param reducedMotion Whether reduced motion is enabled
 * @return Reduced or normal expand transition
 */
fun accessibleExpandVertically(reducedMotion: Boolean): EnterTransition {
    return if (reducedMotion) {
        fadeIn(animationSpec = tween(50))
    } else {
        expandVertically() + fadeIn()
    }
}

/**
 * Get accessible vertical shrink transition.
 *
 * @param reducedMotion Whether reduced motion is enabled
 * @return Reduced or normal shrink transition
 */
fun accessibleShrinkVertically(reducedMotion: Boolean): ExitTransition {
    return if (reducedMotion) {
        fadeOut(animationSpec = tween(50))
    } else {
        shrinkVertically() + fadeOut()
    }
}

/**
 * Example usage in a composable:
 *
 * ```kotlin
 * @Composable
 * fun MyAnimatedScreen() {
 *     val reducedMotion = rememberReducedMotion()
 *     var isVisible by remember { mutableStateOf(false) }
 *
 *     AnimatedVisibility(
 *         visible = isVisible,
 *         enter = accessibleEnterTransition(reducedMotion),
 *         exit = accessibleExitTransition(reducedMotion)
 *     ) {
 *         Text("Animated content")
 *     }
 * }
 * ```
 */
