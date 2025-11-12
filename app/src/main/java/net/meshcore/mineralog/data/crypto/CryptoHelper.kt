package net.meshcore.mineralog.data.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Helper for AES-256-GCM encryption and decryption.
 *
 * AES-GCM (Galois/Counter Mode) provides:
 * - Confidentiality: Data is encrypted with AES-256
 * - Authenticity: Built-in authentication tag prevents tampering
 * - Performance: Hardware-accelerated on modern devices
 *
 * Security notes:
 * - Uses 256-bit keys (derived from passwords via Argon2)
 * - 12-byte IV (recommended for GCM)
 * - 128-bit authentication tag
 * - IVs are randomly generated per encryption and stored with ciphertext
 */
class CryptoHelper {
    companion object {
        // AES algorithm and mode
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        // GCM parameters
        private const val IV_LENGTH_BYTES = 12 // 96 bits, recommended for GCM
        private const val TAG_LENGTH_BITS = 128 // Authentication tag length

        // Key size
        private const val KEY_LENGTH_BYTES = 32 // 256 bits
    }

    /**
     * Encrypts data using AES-256-GCM.
     *
     * @param plaintext Data to encrypt
     * @param key 32-byte encryption key (from Argon2)
     * @return [EncryptionResult] containing ciphertext and IV
     * @throws IllegalArgumentException if key is not 32 bytes
     * @throws javax.crypto.AEADBadTagException if encryption fails
     */
    fun encrypt(plaintext: ByteArray, key: ByteArray): EncryptionResult {
        require(key.size == KEY_LENGTH_BYTES) {
            "Key must be exactly $KEY_LENGTH_BYTES bytes (256 bits)"
        }

        // Generate random IV (must be unique for each encryption)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        // Initialize cipher
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        // Encrypt
        val ciphertext = cipher.doFinal(plaintext)

        return EncryptionResult(
            ciphertext = ciphertext,
            iv = iv,
            encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    /**
     * Encrypts a string using AES-256-GCM.
     *
     * @param plaintext String to encrypt
     * @param key 32-byte encryption key
     * @return [EncryptionResult] containing ciphertext and IV
     */
    fun encryptString(plaintext: String, key: ByteArray): EncryptionResult {
        return encrypt(plaintext.toByteArray(Charsets.UTF_8), key)
    }

    /**
     * Decrypts data using AES-256-GCM.
     *
     * @param ciphertext Encrypted data
     * @param key 32-byte decryption key (must match encryption key)
     * @param iv 12-byte initialization vector (from encryption)
     * @return Decrypted plaintext
     * @throws IllegalArgumentException if key or IV size is incorrect
     * @throws javax.crypto.AEADBadTagException if authentication fails (tampering detected or wrong key)
     */
    fun decrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(key.size == KEY_LENGTH_BYTES) {
            "Key must be exactly $KEY_LENGTH_BYTES bytes (256 bits)"
        }
        require(iv.size == IV_LENGTH_BYTES) {
            "IV must be exactly $IV_LENGTH_BYTES bytes (96 bits)"
        }

        // Initialize cipher
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        // Decrypt (will throw AEADBadTagException if tampered or wrong key)
        return cipher.doFinal(ciphertext)
    }

    /**
     * Decrypts data to a string using AES-256-GCM.
     *
     * @param ciphertext Encrypted data
     * @param key 32-byte decryption key
     * @param iv 12-byte initialization vector
     * @return Decrypted string
     */
    fun decryptToString(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): String {
        val plaintext = decrypt(ciphertext, key, iv)
        return plaintext.toString(Charsets.UTF_8)
    }

    /**
     * Encrypts a file in memory.
     * For large files, consider streaming encryption instead.
     *
     * @param fileData File content as bytes
     * @param key Encryption key
     * @return [EncryptionResult]
     */
    fun encryptFile(fileData: ByteArray, key: ByteArray): EncryptionResult {
        return encrypt(fileData, key)
    }

    /**
     * Decrypts a file in memory.
     * For large files, consider streaming decryption instead.
     *
     * @param encryptedData Encrypted file content
     * @param key Decryption key
     * @param iv Initialization vector
     * @return Decrypted file data
     */
    fun decryptFile(encryptedData: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        return decrypt(encryptedData, key, iv)
    }

    /**
     * Encodes ciphertext to Base64 for storage/transmission.
     *
     * @param ciphertext Raw encrypted bytes
     * @return Base64-encoded string
     */
    fun encodeCiphertext(ciphertext: ByteArray): String {
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    /**
     * Decodes Base64-encoded ciphertext.
     *
     * @param encoded Base64 string
     * @return Raw encrypted bytes
     */
    fun decodeCiphertext(encoded: String): ByteArray {
        return Base64.decode(encoded, Base64.NO_WRAP)
    }

    /**
     * Encodes IV to Base64 for storage.
     *
     * @param iv Raw IV bytes
     * @return Base64-encoded IV
     */
    fun encodeIv(iv: ByteArray): String {
        return Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    /**
     * Decodes Base64-encoded IV.
     *
     * @param encoded Base64 string
     * @return Raw IV bytes
     */
    fun decodeIv(encoded: String): ByteArray {
        return Base64.decode(encoded, Base64.NO_WRAP)
    }

    /**
     * Creates an encrypted package combining ciphertext and IV.
     * Format: [IV (12 bytes)][Ciphertext (variable)]
     *
     * This is convenient for storage as a single blob.
     *
     * @param encryptionResult Result from encrypt()
     * @return Combined byte array
     */
    fun packageEncrypted(encryptionResult: EncryptionResult): ByteArray {
        return encryptionResult.iv + encryptionResult.ciphertext
    }

    /**
     * Unpacks an encrypted package into IV and ciphertext.
     *
     * @param packagedData Data from packageEncrypted()
     * @return Pair of (ciphertext, IV)
     * @throws IllegalArgumentException if data is too short
     */
    fun unpackageEncrypted(packagedData: ByteArray): Pair<ByteArray, ByteArray> {
        require(packagedData.size > IV_LENGTH_BYTES) {
            "Packaged data too short (expected at least ${IV_LENGTH_BYTES + 1} bytes)"
        }

        val iv = packagedData.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = packagedData.copyOfRange(IV_LENGTH_BYTES, packagedData.size)

        return Pair(ciphertext, iv)
    }

    /**
     * One-step encryption: encrypt and package.
     *
     * @param plaintext Data to encrypt
     * @param key Encryption key
     * @return Packaged encrypted data (IV + ciphertext)
     */
    fun encryptAndPackage(plaintext: ByteArray, key: ByteArray): ByteArray {
        val result = encrypt(plaintext, key)
        return packageEncrypted(result)
    }

    /**
     * One-step decryption: unpackage and decrypt.
     *
     * @param packagedData Encrypted package from encryptAndPackage()
     * @param key Decryption key
     * @return Decrypted plaintext
     */
    fun unpackageAndDecrypt(packagedData: ByteArray, key: ByteArray): ByteArray {
        val (ciphertext, iv) = unpackageEncrypted(packagedData)
        return decrypt(ciphertext, key, iv)
    }
}

/**
 * Result of AES-GCM encryption.
 *
 * @property ciphertext Encrypted data (includes authentication tag)
 * @property iv Initialization vector (must be stored with ciphertext)
 * @property encodedIv Base64-encoded IV for convenient storage
 */
data class EncryptionResult(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val encodedIv: String
) {
    /**
     * Returns Base64-encoded ciphertext for storage.
     */
    val encodedCiphertext: String
        get() = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

    /**
     * Returns ciphertext size in bytes.
     */
    val size: Int
        get() = ciphertext.size + iv.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptionResult

        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (encodedIv != other.encodedIv) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + encodedIv.hashCode()
        return result
    }
}

/**
 * Exception thrown when decryption fails due to authentication.
 * This indicates either tampering or wrong password/key.
 */
class DecryptionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Utility for password-based encryption/decryption.
 * Combines Argon2 KDF with AES-GCM encryption.
 */
class PasswordBasedCrypto(
    private val argon2Helper: Argon2Helper = Argon2Helper(),
    private val cryptoHelper: CryptoHelper = CryptoHelper()
) {
    /**
     * Encrypts data with a password.
     *
     * Process:
     * 1. Derive key from password using Argon2
     * 2. Encrypt data with AES-GCM
     * 3. Return encrypted data + salt + IV
     *
     * @param plaintext Data to encrypt
     * @param password User password
     * @return [PasswordEncryptionResult] with all necessary data
     */
    fun encrypt(plaintext: ByteArray, password: CharArray): PasswordEncryptionResult {
        // Derive key from password
        val kdfResult = argon2Helper.deriveKey(password)

        try {
            // Encrypt with derived key
            val encResult = cryptoHelper.encrypt(plaintext, kdfResult.key)

            return PasswordEncryptionResult(
                ciphertext = encResult.ciphertext,
                salt = kdfResult.salt,
                iv = encResult.iv
            )
        } finally {
            // Clear sensitive data
            kdfResult.clear()
        }
    }

    /**
     * Decrypts data with a password.
     *
     * @param ciphertext Encrypted data
     * @param password User password (must match encryption password)
     * @param salt Salt from encryption
     * @param iv IV from encryption
     * @return Decrypted plaintext
     * @throws DecryptionException if password is wrong or data tampered
     */
    fun decrypt(
        ciphertext: ByteArray,
        password: CharArray,
        salt: ByteArray,
        iv: ByteArray
    ): ByteArray {
        // Derive key from password with same salt
        val kdfResult = argon2Helper.deriveKey(password, salt)

        return try {
            // Decrypt with derived key
            cryptoHelper.decrypt(ciphertext, kdfResult.key, iv)
        } catch (e: Exception) {
            throw DecryptionException("Decryption failed: wrong password or corrupted data", e)
        } finally {
            // Clear sensitive data
            kdfResult.clear()
        }
    }
}

/**
 * Result of password-based encryption.
 * Contains all data needed for decryption.
 */
data class PasswordEncryptionResult(
    val ciphertext: ByteArray,
    val salt: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PasswordEncryptionResult

        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!salt.contentEquals(other.salt)) return false
        if (!iv.contentEquals(other.iv)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}
