package net.meshcore.mineralog.domain.provider

import android.content.Context
import androidx.annotation.StringRes

/**
 * Abstraction layer for accessing Android resources.
 *
 * Decouples ViewModels from Android Context to enable unit testing without instrumentation.
 * Follows Dependency Inversion Principle (DIP) - ViewModels depend on abstraction, not concrete Context.
 *
 * Sprint 2: Architecture Refactoring - Dependency Inversion Principle (DIP)
 * Target: Make ViewModels testable without Android framework
 *
 * @see net.meshcore.mineralog.ui.screens.home.HomeViewModel
 */
interface ResourceProvider {

    /**
     * Get a string resource by its resource ID.
     *
     * @param resId The string resource ID
     * @return The localized string
     */
    fun getString(@StringRes resId: Int): String

    /**
     * Get a formatted string resource with arguments.
     *
     * @param resId The string resource ID
     * @param formatArgs Format arguments to substitute into the string
     * @return The formatted localized string
     */
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String

    /**
     * Get a quantity string (plurals) resource.
     *
     * @param resId The plural resource ID
     * @param quantity The number used to select the appropriate plural form
     * @return The localized plural string
     */
    fun getQuantityString(@StringRes resId: Int, quantity: Int): String

    /**
     * Get a quantity string with format arguments.
     *
     * @param resId The plural resource ID
     * @param quantity The number used to select the appropriate plural form
     * @param formatArgs Format arguments to substitute into the string
     * @return The formatted localized plural string
     */
    fun getQuantityString(@StringRes resId: Int, quantity: Int, vararg formatArgs: Any): String
}

/**
 * Android implementation of ResourceProvider.
 *
 * Uses Android Context to access application resources.
 */
class AndroidResourceProvider(
    private val context: Context
) : ResourceProvider {

    override fun getString(resId: Int): String {
        return context.getString(resId)
    }

    override fun getString(resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }

    override fun getQuantityString(resId: Int, quantity: Int): String {
        return context.resources.getQuantityString(resId, quantity)
    }

    override fun getQuantityString(resId: Int, quantity: Int, vararg formatArgs: Any): String {
        return context.resources.getQuantityString(resId, quantity, *formatArgs)
    }
}
