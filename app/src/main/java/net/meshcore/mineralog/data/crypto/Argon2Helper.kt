package net.meshcore.mineralog.data.crypto

import android.util.Base64
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.SecureRandom

/**
 * Helper for Argon2id key derivation.
 *
 * Argon2id is the recommended password hashing algorithm (RFC 9106).
 * It combines Argon2i (data-independent) and Argon2d (data-dependent) for
 * resistance against both side-channel and GPU attacks.
 *
 * Parameters chosen for mobile devices:
 * - Memory: 128 MB (suitable for modern phones, balances security vs performance)
 * - Iterations: 4 (t-cost, adequate for mobile with high m-cost)
 * - Parallelism: 2 (matches typical dual-core minimum)
 *
 * Security note: These parameters should take ~500ms on mid-range device (2020+).
 * Adjust if targeting older devices.
 */
class Argon2Helper(
    private val argon2: Argon2Kt = Argon2Kt()
) {
    companion object {
        // Memory cost in KB (128 MB = 131072 KB)
        private const val MEMORY_COST_KB = 131072

        // Number of iterations (time cost)
        private const val ITERATIONS = 4

        // Parallelism factor (number of threads)
        private const val PARALLELISM = 2

        // Salt length in bytes (16 bytes = 128 bits, recommended minimum)
        private const val SALT_LENGTH_BYTES = 16

        // Output key length in bytes (32 bytes = 256 bits for AES-256)
        private const val KEY_LENGTH_BYTES = 32

        // Argon2 mode: Argon2id (hybrid, most secure)
        private val MODE = Argon2Mode.ARGON2_ID
    }

    /**
     * Derives a cryptographic key from a password using Argon2id.
     *
     * @param password User's password (will be cleared from memory after use)
     * @param salt Random salt (16 bytes). If null, generates a new salt.
     * @return [KeyDerivationResult] containing the derived key and salt used
     * @throws IllegalArgumentException if password is empty
     */
    fun deriveKey(password: CharArray, salt: ByteArray? = null): KeyDerivationResult {
        require(password.isNotEmpty()) { "Password cannot be empty" }

        val actualSalt = salt ?: generateSalt()

        try {
            // Convert password to bytes (UTF-8)
            val passwordBytes = password.concatToString().toByteArray(Charsets.UTF_8)

            // Derive key using Argon2id
            val derivedKey = argon2.hash(
                mode = MODE,
                password = passwordBytes,
                salt = actualSalt,
                tCostInIterations = ITERATIONS,
                mCostInKibibytes = MEMORY_COST_KB,
                parallelism = PARALLELISM,
                hashLengthInBytes = KEY_LENGTH_BYTES
            )

            // Clear sensitive data from memory
            passwordBytes.fill(0)

            return KeyDerivationResult(
                key = derivedKey.rawHashAsByteArray(),
                salt = actualSalt,
                encodedSalt = Base64.encodeToString(actualSalt, Base64.NO_WRAP)
            )
        } finally {
            // Clear password from memory for security (use null character, not '0')
            password.fill('\u0000')
        }
    }

    /**
     * Verifies a password against a previously derived key.
     *
     * @param password Password to verify
     * @param expectedKey Expected derived key
     * @param salt Salt used in original derivation
     * @return true if password matches, false otherwise
     */
    fun verifyPassword(
        password: CharArray,
        expectedKey: ByteArray,
        salt: ByteArray
    ): Boolean {
        return try {
            val result = deriveKey(password, salt)
            result.key.contentEquals(expectedKey)
        } catch (e: Exception) {
            false
        } finally {
            password.fill('\u0000')
        }
    }

    /**
     * Generates a cryptographically secure random salt.
     *
     * @return 16-byte random salt
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    /**
     * Encodes a salt to Base64 for storage/transmission.
     *
     * @param salt Raw salt bytes
     * @return Base64-encoded salt string
     */
    fun encodeSalt(salt: ByteArray): String {
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /**
     * Decodes a Base64-encoded salt.
     *
     * @param encodedSalt Base64 string
     * @return Raw salt bytes
     */
    fun decodeSalt(encodedSalt: String): ByteArray {
        return Base64.decode(encodedSalt, Base64.NO_WRAP)
    }

    /**
     * Estimates the time required for key derivation on the current device.
     * Useful for showing progress or warning users.
     *
     * @return Estimated time in milliseconds
     */
    fun estimateDerivationTime(): Long {
        val startTime = System.currentTimeMillis()
        val testPassword = "benchmark_password_123".toCharArray()

        try {
            deriveKey(testPassword)
        } catch (e: Exception) {
            return -1 // Error occurred
        } finally {
            testPassword.fill('\u0000')
        }

        return System.currentTimeMillis() - startTime
    }
}

/**
 * Result of Argon2 key derivation.
 *
 * @property key Derived cryptographic key (32 bytes for AES-256)
 * @property salt Salt used in derivation (16 bytes)
 * @property encodedSalt Base64-encoded salt for storage
 */
data class KeyDerivationResult(
    val key: ByteArray,
    val salt: ByteArray,
    val encodedSalt: String
) {
    /**
     * Clears sensitive data from memory.
     * Call this when key is no longer needed.
     */
    fun clear() {
        key.fill(0)
        salt.fill(0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyDerivationResult

        if (!key.contentEquals(other.key)) return false
        if (!salt.contentEquals(other.salt)) return false
        if (encodedSalt != other.encodedSalt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + encodedSalt.hashCode()
        return result
    }
}

/**
 * Password strength assessment utility.
 */
object PasswordStrength {
    /**
     * Assesses password strength.
     *
     * Criteria:
     * - WEAK: < 8 characters
     * - FAIR: 8+ chars, single type
     * - GOOD: 8+ chars, 2 types (e.g., letters + numbers)
     * - STRONG: 12+ chars, 3+ types (upper, lower, numbers, symbols)
     *
     * @param password Password to assess
     * @return [Strength] enum value
     */
    fun assess(password: CharArray): Strength {
        if (password.size < 8) return Strength.WEAK

        val hasLower = password.any { it.isLowerCase() }
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSymbol = password.any { !it.isLetterOrDigit() }

        val typeCount = listOf(hasLower, hasUpper, hasDigit, hasSymbol).count { it }

        return when {
            password.size >= 12 && typeCount >= 3 -> Strength.STRONG
            password.size >= 10 && typeCount >= 2 -> Strength.GOOD
            password.size >= 8 && typeCount >= 2 -> Strength.FAIR
            else -> Strength.WEAK
        }
    }

    enum class Strength {
        WEAK, FAIR, GOOD, STRONG
    }
}
