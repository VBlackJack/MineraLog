package net.meshcore.mineralog.util

import android.util.Log
import net.meshcore.mineralog.BuildConfig

/**
 * Application logger wrapper that prevents logging in production builds.
 *
 * This wrapper ensures that all logging statements are stripped from release APKs,
 * improving performance and preventing potential information leakage.
 *
 * Usage:
 * ```kotlin
 * AppLogger.d("MyTag", "Debug message")
 * AppLogger.e("MyTag", "Error message", exception)
 * ```
 *
 * In DEBUG builds: Logs are written to Logcat
 * In RELEASE builds: Logs are silently discarded (no-op)
 *
 * ProGuard Configuration:
 * The app's ProGuard rules should include:
 * ```
 * -assumenosideeffects class net.meshcore.mineralog.util.AppLogger {
 *     public static *** v(...);
 *     public static *** d(...);
 *     public static *** i(...);
 *     public static *** w(...);
 *     public static *** e(...);
 * }
 * ```
 * This ensures complete removal of logging bytecode in release builds.
 */
object AppLogger {

    /**
     * Verbose log - for detailed debugging information.
     * Only logs in DEBUG builds.
     *
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     */
    @JvmStatic
    fun v(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message)
        }
    }

    /**
     * Debug log - for debugging information.
     * Only logs in DEBUG builds.
     *
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     */
    @JvmStatic
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    /**
     * Info log - for informational messages.
     * Only logs in DEBUG builds.
     *
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     */
    @JvmStatic
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message)
        }
    }

    /**
     * Warning log - for warning messages.
     * Only logs in DEBUG builds.
     *
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     */
    @JvmStatic
    fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message)
        }
    }

    /**
     * Warning log with throwable - for warning messages with exceptions.
     * Only logs in DEBUG builds.
     *
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     * @param throwable An exception to log
     */
    @JvmStatic
    fun w(tag: String, message: String, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message, throwable)
        }
    }

    /**
     * Error log - for error messages.
     * Only logs in DEBUG builds.
     *
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     */
    @JvmStatic
    fun e(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message)
        }
    }

    /**
     * Error log with throwable - for error messages with exceptions.
     * Only logs in DEBUG builds.
     *
     * @param tag Used to identify the source of a log message
     * @param message The message to log
     * @param throwable An exception to log
     */
    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        }
    }
}
