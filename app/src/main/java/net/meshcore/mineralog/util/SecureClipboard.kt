package net.meshcore.mineralog.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.meshcore.mineralog.util.logging.AppLogger

/**
 * Secure clipboard manager that automatically clears clipboard content after a specified delay.
 *
 * This prevents sensitive data (like mineral IDs, import errors, etc.) from persisting
 * in the clipboard longer than necessary, reducing security risks.
 *
 * Usage:
 * ```kotlin
 * SecureClipboard.copyWithAutoCleanup(
 *     context = context,
 *     label = "Mineral ID",
 *     text = mineralId,
 *     delayMs = 30_000L // 30 seconds
 * )
 * ```
 */
object SecureClipboard {
    private var clearJob: Job? = null

    /**
     * Copy text to clipboard with automatic cleanup after specified delay.
     *
     * @param context Android context for accessing clipboard service
     * @param label Label for the clipboard data (shown in clipboard UI)
     * @param text Text to copy to clipboard
     * @param delayMs Delay in milliseconds before auto-clearing (default: 30 seconds)
     */
    fun copyWithAutoCleanup(
        context: Context,
        label: String,
        text: String,
        delayMs: Long = 30_000L // 30 seconds default
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

        if (clipboard == null) {
            AppLogger.e("SecureClipboard", "ClipboardManager not available")
            return
        }

        // Copy to clipboard
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)

        AppLogger.d("SecureClipboard", "Copied to clipboard: '$label' (will clear in ${delayMs}ms)")

        // Cancel any existing clear job
        clearJob?.cancel()

        // Schedule auto-clear
        clearJob = CoroutineScope(Dispatchers.Main).launch {
            delay(delayMs)
            try {
                // Clear clipboard by setting empty content
                val emptyClip = ClipData.newPlainText("", "")
                clipboard.setPrimaryClip(emptyClip)
                AppLogger.d("SecureClipboard", "Clipboard cleared after ${delayMs}ms")
            } catch (e: Exception) {
                AppLogger.e("SecureClipboard", "Failed to clear clipboard", e)
            }
        }
    }

    /**
     * Manually clear any pending auto-clear job.
     * Useful when the user manually clears or the activity is destroyed.
     */
    fun cancelAutoClear() {
        clearJob?.cancel()
        clearJob = null
        AppLogger.d("SecureClipboard", "Auto-clear cancelled")
    }
}
