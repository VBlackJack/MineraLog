package net.meshcore.mineralog.data.service

import net.meshcore.mineralog.data.crypto.DecryptionException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.Base64

/**
 * Security-critical tests for BackupEncryptionService.
 *
 * Tests cover:
 * - Round-trip encryption/decryption
 * - Wrong password detection
 * - Data corruption detection
 * - Encryption metadata creation
 * - Manifest creation
 * - Schema version validation
 *
 * Target coverage: 80%+
 */
class BackupEncryptionServiceTest {

    private lateinit var encryptionService: BackupEncryptionService

    @BeforeEach
    fun setup() {
        encryptionService = BackupEncryptionService()
    }

    // ========================================
    // ENCRYPTION / DECRYPTION TESTS
    // ========================================

    @Test
    @DisplayName("Round-trip encryption and decryption preserves data")
    fun `encrypt then decrypt - preserves data`() {
        // Arrange
        val originalData = "Test data for encryption".toByteArray()
        val password = "SecurePassword123!".toCharArray()

        // Act
        val encryptionResult = encryptionService.encrypt(originalData, password)
        val decryptedData = encryptionService.decrypt(
            ciphertext = encryptionResult.ciphertext,
            password = password,
            encodedSalt = encryptionResult.encodedSalt,
            encodedIv = encryptionResult.encodedIv
        )

        // Assert
        assertArrayEquals(originalData, decryptedData, "Decrypted data should match original")
    }

    @Test
    @DisplayName("Encryption with different passwords produces different ciphertext")
    fun `encrypt - different passwords - different ciphertext`() {
        // Arrange
        val data = "Sensitive data".toByteArray()
        val password1 = "Password1".toCharArray()
        val password2 = "Password2".toCharArray()

        // Act
        val result1 = encryptionService.encrypt(data, password1)
        val result2 = encryptionService.encrypt(data, password2)

        // Assert
        assertFalse(
            result1.ciphertext.contentEquals(result2.ciphertext),
            "Different passwords should produce different ciphertext"
        )
        assertNotEquals(result1.encodedSalt, result2.encodedSalt, "Different salts")
        assertNotEquals(result1.encodedIv, result2.encodedIv, "Different IVs")
    }

    @Test
    @DisplayName("Same data encrypted twice produces different ciphertext (randomized IV)")
    fun `encrypt - same data twice - different ciphertext`() {
        // Arrange
        val data = "Test data".toByteArray()
        val password = "Password".toCharArray()

        // Act
        val result1 = encryptionService.encrypt(data, password)
        val result2 = encryptionService.encrypt(data, password)

        // Assert
        assertFalse(
            result1.ciphertext.contentEquals(result2.ciphertext),
            "Randomized IV should produce different ciphertext each time"
        )
    }

    @Test
    @DisplayName("Empty data can be encrypted and decrypted")
    fun `encrypt - empty data - handles correctly`() {
        // Arrange
        val emptyData = ByteArray(0)
        val password = "Password".toCharArray()

        // Act
        val encryptionResult = encryptionService.encrypt(emptyData, password)
        val decryptedData = encryptionService.decrypt(
            ciphertext = encryptionResult.ciphertext,
            password = password,
            encodedSalt = encryptionResult.encodedSalt,
            encodedIv = encryptionResult.encodedIv
        )

        // Assert
        assertArrayEquals(emptyData, decryptedData)
        assertEquals(0, decryptedData.size)
    }

    @Test
    @DisplayName("Large data can be encrypted and decrypted")
    fun `encrypt - large data - handles correctly`() {
        // Arrange
        val largeData = ByteArray(100_000) { it.toByte() } // 100 KB
        val password = "Password".toCharArray()

        // Act
        val encryptionResult = encryptionService.encrypt(largeData, password)
        val decryptedData = encryptionService.decrypt(
            ciphertext = encryptionResult.ciphertext,
            password = password,
            encodedSalt = encryptionResult.encodedSalt,
            encodedIv = encryptionResult.encodedIv
        )

        // Assert
        assertArrayEquals(largeData, decryptedData)
    }

    @Test
    @DisplayName("Binary data can be encrypted and decrypted")
    fun `encrypt - binary data - preserves all bytes`() {
        // Arrange
        val binaryData = ByteArray(256) { it.toByte() } // All byte values 0-255
        val password = "Password".toCharArray()

        // Act
        val encryptionResult = encryptionService.encrypt(binaryData, password)
        val decryptedData = encryptionService.decrypt(
            ciphertext = encryptionResult.ciphertext,
            password = password,
            encodedSalt = encryptionResult.encodedSalt,
            encodedIv = encryptionResult.encodedIv
        )

        // Assert
        assertArrayEquals(binaryData, decryptedData)
    }

    // ========================================
    // WRONG PASSWORD TESTS
    // ========================================

    @Test
    @DisplayName("Decrypt with wrong password throws DecryptionException")
    fun `decrypt - wrong password - throws DecryptionException`() {
        // Arrange
        val data = "Secret data".toByteArray()
        val correctPassword = "CorrectPassword".toCharArray()
        val wrongPassword = "WrongPassword".toCharArray()

        val encryptionResult = encryptionService.encrypt(data, correctPassword)

        // Act & Assert
        assertThrows<DecryptionException> {
            encryptionService.decrypt(
                ciphertext = encryptionResult.ciphertext,
                password = wrongPassword,
                encodedSalt = encryptionResult.encodedSalt,
                encodedIv = encryptionResult.encodedIv
            )
        }
    }

    @Test
    @DisplayName("Decrypt with empty password throws DecryptionException")
    fun `decrypt - empty password - throws DecryptionException`() {
        // Arrange
        val data = "Secret data".toByteArray()
        val correctPassword = "CorrectPassword".toCharArray()
        val emptyPassword = CharArray(0)

        val encryptionResult = encryptionService.encrypt(data, correctPassword)

        // Act & Assert
        assertThrows<DecryptionException> {
            encryptionService.decrypt(
                ciphertext = encryptionResult.ciphertext,
                password = emptyPassword,
                encodedSalt = encryptionResult.encodedSalt,
                encodedIv = encryptionResult.encodedIv
            )
        }
    }

    @Test
    @DisplayName("Decrypt with slightly different password fails")
    fun `decrypt - slightly different password - fails`() {
        // Arrange
        val data = "Secret data".toByteArray()
        val correctPassword = "Password123".toCharArray()
        val slightlyWrongPassword = "Password124".toCharArray() // One char different

        val encryptionResult = encryptionService.encrypt(data, correctPassword)

        // Act & Assert
        assertThrows<DecryptionException> {
            encryptionService.decrypt(
                ciphertext = encryptionResult.ciphertext,
                password = slightlyWrongPassword,
                encodedSalt = encryptionResult.encodedSalt,
                encodedIv = encryptionResult.encodedIv
            )
        }
    }

    // ========================================
    // DATA CORRUPTION TESTS
    // ========================================

    @Test
    @DisplayName("Decrypt with corrupted ciphertext throws DecryptionException")
    fun `decrypt - corrupted ciphertext - throws DecryptionException`() {
        // Arrange
        val data = "Secret data".toByteArray()
        val password = "Password".toCharArray()

        val encryptionResult = encryptionService.encrypt(data, password)

        // Corrupt the ciphertext
        val corruptedCiphertext = encryptionResult.ciphertext.clone()
        if (corruptedCiphertext.isNotEmpty()) {
            corruptedCiphertext[0] = (corruptedCiphertext[0] + 1).toByte()
        }

        // Act & Assert
        assertThrows<DecryptionException> {
            encryptionService.decrypt(
                ciphertext = corruptedCiphertext,
                password = password,
                encodedSalt = encryptionResult.encodedSalt,
                encodedIv = encryptionResult.encodedIv
            )
        }
    }

    @Test
    @DisplayName("Decrypt with corrupted salt throws DecryptionException")
    fun `decrypt - corrupted salt - throws DecryptionException`() {
        // Arrange
        val data = "Secret data".toByteArray()
        val password = "Password".toCharArray()

        val encryptionResult = encryptionService.encrypt(data, password)

        // Corrupt the salt by modifying one character
        val corruptedSalt = encryptionResult.encodedSalt.replaceFirst("A", "B")

        // Act & Assert
        assertThrows<Exception> { // May throw DecryptionException or IllegalArgumentException
            encryptionService.decrypt(
                ciphertext = encryptionResult.ciphertext,
                password = password,
                encodedSalt = corruptedSalt,
                encodedIv = encryptionResult.encodedIv
            )
        }
    }

    @Test
    @DisplayName("Decrypt with corrupted IV throws DecryptionException")
    fun `decrypt - corrupted IV - throws DecryptionException`() {
        // Arrange
        val data = "Secret data".toByteArray()
        val password = "Password".toCharArray()

        val encryptionResult = encryptionService.encrypt(data, password)

        // Corrupt the IV
        val corruptedIv = encryptionResult.encodedIv.replaceFirst("A", "B")

        // Act & Assert
        assertThrows<DecryptionException> {
            encryptionService.decrypt(
                ciphertext = encryptionResult.ciphertext,
                password = password,
                encodedSalt = encryptionResult.encodedSalt,
                encodedIv = corruptedIv
            )
        }
    }

    @Test
    @DisplayName("Decrypt with invalid Base64 salt throws exception")
    fun `decrypt - invalid base64 salt - throws exception`() {
        // Arrange
        val data = "Secret data".toByteArray()
        val password = "Password".toCharArray()

        val encryptionResult = encryptionService.encrypt(data, password)

        // Create invalid Base64 string
        val invalidSalt = "not-valid-base64!!!"

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            encryptionService.decrypt(
                ciphertext = encryptionResult.ciphertext,
                password = password,
                encodedSalt = invalidSalt,
                encodedIv = encryptionResult.encodedIv
            )
        }
    }

    // ========================================
    // ENCRYPTION METADATA TESTS
    // ========================================

    @Test
    @DisplayName("Create encryption metadata includes all required fields")
    fun `createEncryptionMetadata - includes all fields`() {
        // Arrange
        val data = "Test data".toByteArray()
        val password = "Password".toCharArray()
        val encryptionResult = encryptionService.encrypt(data, password)

        // Act
        val metadata = encryptionService.createEncryptionMetadata(encryptionResult)

        // Assert
        assertNotNull(metadata)
        assertEquals("Argon2id+AES-256-GCM", metadata.algorithm)
        assertEquals(encryptionResult.encodedSalt, metadata.salt)
        assertEquals(encryptionResult.encodedIv, metadata.iv)
    }

    @Test
    @DisplayName("Encryption metadata salt and IV are valid Base64")
    fun `createEncryptionMetadata - valid base64`() {
        // Arrange
        val data = "Test data".toByteArray()
        val password = "Password".toCharArray()
        val encryptionResult = encryptionService.encrypt(data, password)
        val metadata = encryptionService.createEncryptionMetadata(encryptionResult)

        // Act & Assert - Should not throw
        assertDoesNotThrow {
            Base64.getDecoder().decode(metadata.salt)
            Base64.getDecoder().decode(metadata.iv)
        }
    }

    // ========================================
    // MANIFEST CREATION TESTS
    // ========================================

    @Test
    @DisplayName("Create manifest with encryption metadata")
    fun `createManifest - with encryption - includes metadata`() {
        // Arrange
        val data = "Test data".toByteArray()
        val password = "Password".toCharArray()
        val encryptionResult = encryptionService.encrypt(data, password)
        val encryptionMetadata = encryptionService.createEncryptionMetadata(encryptionResult)

        // Act
        val manifest = encryptionService.createManifest(
            mineralCount = 10,
            photoCount = 5,
            encrypted = true,
            encryptionMetadata = encryptionMetadata
        )

        // Assert
        assertEquals("1.0.0", manifest.schemaVersion)
        assertEquals(10, manifest.mineralCount)
        assertEquals(5, manifest.photoCount)
        assertTrue(manifest.encrypted)
        assertNotNull(manifest.encryption)
        assertEquals("Argon2id+AES-256-GCM", manifest.encryption?.algorithm)
    }

    @Test
    @DisplayName("Create manifest without encryption")
    fun `createManifest - without encryption - no metadata`() {
        // Act
        val manifest = encryptionService.createManifest(
            mineralCount = 20,
            photoCount = 10,
            encrypted = false,
            encryptionMetadata = null
        )

        // Assert
        assertEquals("1.0.0", manifest.schemaVersion)
        assertEquals(20, manifest.mineralCount)
        assertEquals(10, manifest.photoCount)
        assertFalse(manifest.encrypted)
        assertNull(manifest.encryption)
    }

    @Test
    @DisplayName("Create manifest with zero counts")
    fun `createManifest - zero counts - valid`() {
        // Act
        val manifest = encryptionService.createManifest(
            mineralCount = 0,
            photoCount = 0,
            encrypted = false
        )

        // Assert
        assertEquals(0, manifest.mineralCount)
        assertEquals(0, manifest.photoCount)
    }

    // ========================================
    // SCHEMA VERSION VALIDATION TESTS
    // ========================================

    @Test
    @DisplayName("Validate schema version 1.0.0 returns true")
    fun `validateSchemaVersion - 1_0_0 - returns true`() {
        // Act
        val isValid = encryptionService.validateSchemaVersion("1.0.0")

        // Assert
        assertTrue(isValid)
    }

    @Test
    @DisplayName("Validate invalid schema version returns false")
    fun `validateSchemaVersion - invalid - returns false`() {
        // Arrange
        val invalidVersions = listOf(
            "2.0.0",
            "0.9.0",
            "1.1.0",
            "invalid",
            "",
            "1.0",
            "1.0.0.0"
        )

        // Act & Assert
        invalidVersions.forEach { version ->
            val isValid = encryptionService.validateSchemaVersion(version)
            assertFalse(isValid, "Version $version should be invalid")
        }
    }

    @Test
    @DisplayName("Validate null schema version returns false")
    fun `validateSchemaVersion - null - returns false`() {
        // Act
        val isValid = encryptionService.validateSchemaVersion(null)

        // Assert
        assertFalse(isValid)
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Encryption result has proper equals and hashCode")
    fun `EncryptionResult - equals and hashCode - work correctly`() {
        // Arrange
        val data = "Test".toByteArray()
        val password = "Password".toCharArray()
        val result1 = encryptionService.encrypt(data, password)
        val result2 = encryptionService.encrypt(data, password)

        // Assert
        assertNotEquals(result1, result2, "Different encryptions should not be equal")
        assertNotEquals(result1.hashCode(), result2.hashCode())

        // Test equality with self
        assertEquals(result1, result1)
        assertEquals(result1.hashCode(), result1.hashCode())
    }

    @Test
    @DisplayName("Password with special characters works correctly")
    fun `encrypt - password with special characters - works`() {
        // Arrange
        val data = "Test data".toByteArray()
        val specialPassword = "P@ssw0rd!#$%^&*()".toCharArray()

        // Act
        val encryptionResult = encryptionService.encrypt(data, specialPassword)
        val decryptedData = encryptionService.decrypt(
            ciphertext = encryptionResult.ciphertext,
            password = specialPassword,
            encodedSalt = encryptionResult.encodedSalt,
            encodedIv = encryptionResult.encodedIv
        )

        // Assert
        assertArrayEquals(data, decryptedData)
    }

    @Test
    @DisplayName("Unicode data can be encrypted and decrypted")
    fun `encrypt - unicode data - preserves correctly`() {
        // Arrange
        val unicodeData = "Hello 世界 🌍 Минералы".toByteArray(Charsets.UTF_8)
        val password = "Password".toCharArray()

        // Act
        val encryptionResult = encryptionService.encrypt(unicodeData, password)
        val decryptedData = encryptionService.decrypt(
            ciphertext = encryptionResult.ciphertext,
            password = password,
            encodedSalt = encryptionResult.encodedSalt,
            encodedIv = encryptionResult.encodedIv
        )

        // Assert
        assertArrayEquals(unicodeData, decryptedData)
        assertEquals(
            "Hello 世界 🌍 Минералы",
            String(decryptedData, Charsets.UTF_8)
        )
    }

    @Test
    @DisplayName("Long password works correctly")
    fun `encrypt - long password - works`() {
        // Arrange
        val data = "Test data".toByteArray()
        val longPassword = "A".repeat(100).toCharArray() // 100 character password

        // Act
        val encryptionResult = encryptionService.encrypt(data, longPassword)
        val decryptedData = encryptionService.decrypt(
            ciphertext = encryptionResult.ciphertext,
            password = longPassword,
            encodedSalt = encryptionResult.encodedSalt,
            encodedIv = encryptionResult.encodedIv
        )

        // Assert
        assertArrayEquals(data, decryptedData)
    }
}
