package net.meshcore.mineralog.data.service

import android.util.Base64
import net.meshcore.mineralog.data.crypto.DecryptionException
import net.meshcore.mineralog.data.crypto.PasswordBasedCrypto
import java.time.Instant

/**
 * Service responsible for encryption and decryption of backup data.
 * Extracted from BackupRepository for better separation of concerns.
 */
class BackupEncryptionService {

    data class EncryptionResult(
        val ciphertext: ByteArray,
        val encodedSalt: String,
        val encodedIv: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptionResult

            if (!ciphertext.contentEquals(other.ciphertext)) return false
            if (encodedSalt != other.encodedSalt) return false
            if (encodedIv != other.encodedIv) return false

            return true
        }

        override fun hashCode(): Int {
            var result = ciphertext.contentHashCode()
            result = 31 * result + encodedSalt.hashCode()
            result = 31 * result + encodedIv.hashCode()
            return result
        }
    }

    /**
     * Encrypt data with a password.
     *
     * @param data The data to encrypt
     * @param password The password to use for encryption
     * @return EncryptionResult containing ciphertext, salt, and IV
     */
    fun encrypt(data: ByteArray, password: CharArray): EncryptionResult {
        val result = PasswordBasedCrypto.encrypt(data, password)
        return EncryptionResult(
            ciphertext = result.ciphertext,
            encodedSalt = result.encodedSalt,
            encodedIv = result.encodedIv
        )
    }

    /**
     * Decrypt data with a password.
     *
     * @param ciphertext The encrypted data
     * @param password The password to use for decryption
     * @param encodedSalt The Base64-encoded salt used during encryption
     * @param encodedIv The Base64-encoded IV used during encryption
     * @return The decrypted data
     * @throws DecryptionException if decryption fails
     */
    fun decrypt(
        ciphertext: ByteArray,
        password: CharArray,
        encodedSalt: String,
        encodedIv: String
    ): ByteArray {
        val encodedCiphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return PasswordBasedCrypto.decryptFromBase64(
            encodedCiphertext = encodedCiphertext,
            password = password,
            encodedSalt = encodedSalt,
            encodedIv = encodedIv
        )
    }

    /**
     * Create manifest encryption metadata.
     *
     * @param encryptionResult The result from encrypting the data
     * @return Map containing encryption algorithm, salt, and IV
     */
    fun createEncryptionMetadata(encryptionResult: EncryptionResult): Map<String, String> {
        return mapOf(
            "algorithm" to "Argon2id+AES-256-GCM",
            "salt" to encryptionResult.encodedSalt,
            "iv" to encryptionResult.encodedIv
        )
    }

    /**
     * Create backup manifest.
     *
     * @param mineralCount Number of minerals in the backup
     * @param photoCount Number of photos in the backup
     * @param encrypted Whether the backup is encrypted
     * @param encryptionMetadata Optional encryption metadata if encrypted
     * @return Map representing the manifest
     */
    fun createManifest(
        mineralCount: Int,
        photoCount: Int,
        encrypted: Boolean,
        encryptionMetadata: Map<String, String>? = null
    ): Map<String, Any> {
        val manifest = mutableMapOf<String, Any>(
            "app" to "MineraLog",
            "schemaVersion" to "1.0.0",
            "exportedAt" to Instant.now().toString(),
            "counts" to mapOf(
                "minerals" to mineralCount,
                "photos" to photoCount
            ),
            "encrypted" to encrypted
        )

        if (encrypted && encryptionMetadata != null) {
            manifest["encryption"] = encryptionMetadata
        }

        return manifest
    }

    /**
     * Validate schema version compatibility.
     *
     * @param version The schema version to validate
     * @return true if the version is supported, false otherwise
     */
    fun validateSchemaVersion(version: String?): Boolean {
        // Currently only support v1.0.0
        return version == "1.0.0"
    }
}
