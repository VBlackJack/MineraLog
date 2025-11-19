package net.meshcore.mineralog.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * Stateful holder for password input that stores characters in a [CharArray] instead of [String].
 *
 * This allows wiping the sensitive data from memory immediately after the dialog is dismissed.
 */
@Stable
class SecurePasswordState internal constructor() {

    private var backingChars by mutableStateOf(CharArray(0), structuralEqualityPolicy())

    val length: Int
        get() = backingChars.size

    val isNotEmpty: Boolean
        get() = backingChars.isNotEmpty()

    fun updateFrom(input: String) {
        replaceWith(input.toCharArray())
    }

    fun reveal(): String = backingChars.concatToString()

    fun contentEquals(other: SecurePasswordState): Boolean = backingChars.contentEquals(other.backingChars)

    inline fun <R> withCharArray(block: (CharArray) -> R): R {
        val copy = backingChars.copyOf()
        return try {
            block(copy)
        } finally {
            copy.fill(ZERO_CHAR)
        }
    }

    fun export(): CharArray = backingChars.copyOf()

    fun clear() {
        if (backingChars.isNotEmpty()) {
            backingChars.fill(ZERO_CHAR)
        }
        backingChars = CharArray(0)
    }

    private fun replaceWith(newValue: CharArray) {
        clear()
        backingChars = newValue
    }

    private companion object {
        private const val ZERO_CHAR: Char = '\u0000'
    }
}

@Composable
fun rememberSecurePasswordState(): SecurePasswordState = remember { SecurePasswordState() }
