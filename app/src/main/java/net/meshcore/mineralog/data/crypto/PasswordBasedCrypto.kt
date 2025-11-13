package net.meshcore.mineralog.data.crypto

import android.util.Base64
import java.security.SecureRandom

/**
 * Password-based encryption using Argon2id + AES-256-GCM.
 *
 * This provides a complete solution for encrypting data with a user password:
 * 1. Argon2id derives a strong 256-bit key from the password
 * 2. AES-256-GCM encrypts the data with the derived key
 * 3. All parameters (salt, IV) are included in the result
 *
 * Security properties:
 * - Password-based key derivation with Argon2id (resistant to GPU attacks)
 * - Authenticated encryption with AES-GCM (prevents tampering)
 * - Unique salt per encryption (prevents rainbow tables)
 * - Unique IV per encryption (prevents pattern analysis)
 *
 * Usage:
 * ```kotlin
 * // Encrypt
 * val result = PasswordBasedCrypto.encrypt("sensitive data", "user password")
 * // Store: result.ciphertext, result.salt, result.iv
 *
 * // Decrypt
 * val plaintext = PasswordBasedCrypto.decrypt(
 *     ciphertext = result.ciphertext,
 *     password = "user password",
 *     salt = result.salt,
 *     iv = result.iv
 * )
 * ```
 */
object PasswordBasedCrypto {

    private const val SALT_LENGTH_BYTES = 16 // 128 bits

    /**
     * Result of password-based encryption.
     */
    data class PasswordEncryptionResult(
        val ciphertext: ByteArray,
        val salt: ByteArray,
        val iv: ByteArray,
        // Base64-encoded versions for easy storage/transmission
        val encodedCiphertext: String = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
        val encodedSalt: String = Base64.encodeToString(salt, Base64.NO_WRAP),
        val encodedIv: String = Base64.encodeToString(iv, Base64.NO_WRAP)
    )

    /**
     * Encrypts data using a password.
     *
     * Process:
     * 1. Generate random salt
     * 2. Derive encryption key from password using Argon2id
     * 3. Encrypt plaintext with AES-256-GCM
     * 4. Return ciphertext + salt + IV (all needed for decryption)
     *
     * @param plaintext Data to encrypt
     * @param password User password
     * @return [PasswordEncryptionResult] containing all necessary decryption parameters
     * @throws Exception if encryption fails
     */
    fun encrypt(plaintext: ByteArray, password: String): PasswordEncryptionResult {
        // Generate random salt for Argon2
        val salt = ByteArray(SALT_LENGTH_BYTES)
        SecureRandom().nextBytes(salt)

        // Derive encryption key from password using Argon2id
        val argon2Helper = Argon2Helper()
        val keyResult = argon2Helper.deriveKey(password.toCharArray(), salt)
        val key = keyResult.key

        // Encrypt with AES-GCM
        val cryptoHelper = CryptoHelper()
        val encryptionResult = cryptoHelper.encrypt(plaintext, key)

        return PasswordEncryptionResult(
            ciphertext = encryptionResult.ciphertext,
            salt = salt,
            iv = encryptionResult.iv
        )
    }

    /**
     * Encrypts a string using a password.
     *
     * @param plaintext String to encrypt
     * @param password User password
     * @return [PasswordEncryptionResult]
     */
    fun encryptString(plaintext: String, password: String): PasswordEncryptionResult {
        return encrypt(plaintext.toByteArray(Charsets.UTF_8), password)
    }

    /**
     * Decrypts data encrypted with [encrypt].
     *
     * Process:
     * 1. Derive encryption key from password using Argon2id (same salt as encryption)
     * 2. Decrypt ciphertext with AES-256-GCM
     *
     * @param ciphertext Encrypted data (from [PasswordEncryptionResult.ciphertext])
     * @param password User password (must match encryption password)
     * @param salt Salt used during encryption (from [PasswordEncryptionResult.salt])
     * @param iv IV used during encryption (from [PasswordEncryptionResult.iv])
     * @return Decrypted plaintext
     * @throws DecryptionException if password is wrong or data is tampered
     */
    fun decrypt(
        ciphertext: ByteArray,
        password: String,
        salt: ByteArray,
        iv: ByteArray
    ): ByteArray {
        try {
            // Derive same key from password using same salt
            val argon2Helper = Argon2Helper()
            val keyResult = argon2Helper.deriveKey(password.toCharArray(), salt)
            val key = keyResult.key

            // Decrypt with AES-GCM
            val cryptoHelper = CryptoHelper()
            return cryptoHelper.decrypt(ciphertext, key, iv)
        } catch (e: Exception) {
            throw DecryptionException("Decryption failed. Wrong password or corrupted data.", e)
        }
    }

    /**
     * Decrypts data and returns a string.
     *
     * @return Decrypted string
     * @throws DecryptionException if decryption fails
     */
    fun decryptString(
        ciphertext: ByteArray,
        password: String,
        salt: ByteArray,
        iv: ByteArray
    ): String {
        val plaintext = decrypt(ciphertext, password, salt, iv)
        return String(plaintext, Charsets.UTF_8)
    }

    /**
     * Decrypt using Base64-encoded parameters.
     *
     * Convenience method when ciphertext/salt/IV are stored as Base64 strings.
     */
    fun decryptFromBase64(
        encodedCiphertext: String,
        password: String,
        encodedSalt: String,
        encodedIv: String
    ): ByteArray {
        val ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP)
        val salt = Base64.decode(encodedSalt, Base64.NO_WRAP)
        val iv = Base64.decode(encodedIv, Base64.NO_WRAP)

        return decrypt(ciphertext, password, salt, iv)
    }
}

/**
 * Exception thrown when decryption fails.
 */
class DecryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
