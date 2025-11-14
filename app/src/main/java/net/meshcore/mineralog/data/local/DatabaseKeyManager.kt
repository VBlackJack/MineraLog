package net.meshcore.mineralog.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages the database encryption passphrase using Android Keystore.
 *
 * The passphrase is:
 * 1. Generated once using a cryptographically secure random generator
 * 2. Encrypted with Android Keystore MasterKey
 * 3. Stored in EncryptedSharedPreferences
 * 4. Retrieved on subsequent launches
 *
 * Security properties:
 * - Passphrase is hardware-backed on devices with TEE/StrongBox
 * - Passphrase is never stored in plaintext
 * - Passphrase is unique per app installation
 */
object DatabaseKeyManager {

    private const val KEYSTORE_ALIAS = "mineralog_db_key"
    private const val PREFS_NAME = "mineralog_db_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    /**
     * Gets or generates the database passphrase.
     *
     * @param context Application context
     * @return Database passphrase as ByteArray
     */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Try to retrieve existing passphrase
        val existingPassphrase = encryptedPrefs.getString(KEY_DB_PASSPHRASE, null)
        if (existingPassphrase != null) {
            return hexStringToByteArray(existingPassphrase)
        }

        // Generate new passphrase
        val newPassphrase = generateSecurePassphrase()

        // Store encrypted passphrase
        encryptedPrefs.edit()
            .putString(KEY_DB_PASSPHRASE, byteArrayToHexString(newPassphrase))
            .apply()

        return newPassphrase
    }

    /**
     * Generates a cryptographically secure random passphrase.
     *
     * Uses Android Keystore to generate a 256-bit AES key,
     * which is then used as the database passphrase.
     *
     * @return 32-byte random passphrase
     */
    private fun generateSecurePassphrase(): ByteArray {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        // Generate key in Android Keystore
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        val secretKey: SecretKey = keyGenerator.generateKey()

        // Extract key bytes (this is the passphrase)
        // Note: On some devices, extracting key bytes may not be possible
        // In that case, we use a secure random generator as fallback
        return try {
            secretKey.encoded ?: generateFallbackPassphrase()
        } catch (e: Exception) {
            generateFallbackPassphrase()
        }
    }

    /**
     * Fallback method to generate passphrase using SecureRandom.
     * Used when Android Keystore key extraction is not supported.
     *
     * @return 32-byte random passphrase
     */
    private fun generateFallbackPassphrase(): ByteArray {
        val passphrase = ByteArray(32)
        java.security.SecureRandom().nextBytes(passphrase)
        return passphrase
    }

    /**
     * Converts ByteArray to hex string for storage.
     */
    private fun byteArrayToHexString(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Converts hex string back to ByteArray.
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string must have even length" }
        return hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}
