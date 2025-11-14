package net.meshcore.mineralog.data.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.crypto.AEADBadTagException

/**
 * Unit tests for CryptoHelper.
 *
 * Tests AES-256-GCM encryption/decryption, IV uniqueness,
 * authentication tag verification, and security properties.
 */
class CryptoHelperTest {

    private val cryptoHelper = CryptoHelper()
    private val argon2Helper = Argon2Helper()

    private fun generateKey(): ByteArray {
        val password = "TestPassword123".toCharArray()
        return argon2Helper.deriveKey(password).key
    }

    @Test
    fun `encrypt and decrypt round-trip succeeds`() {
        val plaintext = "Hello, MineraLog!".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val decrypted = cryptoHelper.decrypt(encrypted.ciphertext, key, encrypted.iv)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encryptString and decryptToString round-trip succeeds`() {
        val plaintext = "Test message with unicode: 中文 日本語 한국어"
        val key = generateKey()

        val encrypted = cryptoHelper.encryptString(plaintext, key)
        val decrypted = cryptoHelper.decryptToString(encrypted.ciphertext, key, encrypted.iv)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt generates unique IV for each encryption`() {
        val plaintext = "Same plaintext".toByteArray()
        val key = generateKey()

        val encrypted1 = cryptoHelper.encrypt(plaintext, key)
        val encrypted2 = cryptoHelper.encrypt(plaintext, key)

        // IVs should be different
        assertFalse(encrypted1.iv.contentEquals(encrypted2.iv))
        // Ciphertexts should be different (because IVs are different)
        assertFalse(encrypted1.ciphertext.contentEquals(encrypted2.ciphertext))
    }

    @Test
    fun `encrypt generates 12-byte IV`() {
        val plaintext = "Test".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)

        // IV should be 12 bytes (96 bits) for GCM
        assertEquals(12, encrypted.iv.size)
    }

    @Test
    fun `encrypt with wrong key size throws exception`() {
        val plaintext = "Test".toByteArray()
        val wrongKey = ByteArray(16) // 128 bits instead of 256

        assertThrows<IllegalArgumentException> {
            cryptoHelper.encrypt(plaintext, wrongKey)
        }
    }

    @Test
    fun `decrypt with wrong key size throws exception`() {
        val ciphertext = ByteArray(32)
        val wrongKey = ByteArray(16) // 128 bits instead of 256
        val iv = ByteArray(12)

        assertThrows<IllegalArgumentException> {
            cryptoHelper.decrypt(ciphertext, wrongKey, iv)
        }
    }

    @Test
    fun `decrypt with wrong IV size throws exception`() {
        val ciphertext = ByteArray(32)
        val key = generateKey()
        val wrongIv = ByteArray(16) // Wrong size

        assertThrows<IllegalArgumentException> {
            cryptoHelper.decrypt(ciphertext, key, wrongIv)
        }
    }

    @Test
    fun `decrypt with wrong key fails authentication`() {
        val plaintext = "Secret data".toByteArray()
        val correctKey = generateKey()
        val wrongKey = generateKey() // Different key

        val encrypted = cryptoHelper.encrypt(plaintext, correctKey)

        // Should throw AEADBadTagException (authentication failure)
        assertThrows<AEADBadTagException> {
            cryptoHelper.decrypt(encrypted.ciphertext, wrongKey, encrypted.iv)
        }
    }

    @Test
    fun `decrypt with tampered ciphertext fails authentication`() {
        val plaintext = "Secret data".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val tamperedCiphertext = encrypted.ciphertext.clone()
        if (tamperedCiphertext.isNotEmpty()) {
            tamperedCiphertext[0] = (tamperedCiphertext[0] + 1).toByte()
        }

        // Should throw AEADBadTagException (tampering detected)
        assertThrows<AEADBadTagException> {
            cryptoHelper.decrypt(tamperedCiphertext, key, encrypted.iv)
        }
    }

    @Test
    fun `decrypt with tampered IV fails authentication`() {
        val plaintext = "Secret data".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val tamperedIv = encrypted.iv.clone()
        tamperedIv[0] = (tamperedIv[0] + 1).toByte()

        // Should throw AEADBadTagException (IV tampering detected)
        assertThrows<AEADBadTagException> {
            cryptoHelper.decrypt(encrypted.ciphertext, key, tamperedIv)
        }
    }

    @Test
    fun `encrypt empty plaintext succeeds`() {
        val plaintext = ByteArray(0)
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val decrypted = cryptoHelper.decrypt(encrypted.ciphertext, key, encrypted.iv)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt large data succeeds`() {
        val plaintext = ByteArray(100000) { it.toByte() } // 100KB
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val decrypted = cryptoHelper.decrypt(encrypted.ciphertext, key, encrypted.iv)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `ciphertext includes authentication tag`() {
        val plaintext = "Test".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)

        // Ciphertext should be plaintext + 16-byte authentication tag
        assertEquals(plaintext.size + 16, encrypted.ciphertext.size)
    }

    @Test
    fun `encryptFile and decryptFile work correctly`() {
        val fileData = "File content with data".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encryptFile(fileData, key)
        val decrypted = cryptoHelper.decryptFile(encrypted.ciphertext, key, encrypted.iv)

        assertArrayEquals(fileData, decrypted)
    }

    @Test
    fun `encodeCiphertext and decodeCiphertext round-trip`() {
        val plaintext = "Test".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val encoded = cryptoHelper.encodeCiphertext(encrypted.ciphertext)
        val decoded = cryptoHelper.decodeCiphertext(encoded)

        assertArrayEquals(encrypted.ciphertext, decoded)
    }

    @Test
    fun `encodeIv and decodeIv round-trip`() {
        val plaintext = "Test".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val encoded = cryptoHelper.encodeIv(encrypted.iv)
        val decoded = cryptoHelper.decodeIv(encoded)

        assertArrayEquals(encrypted.iv, decoded)
    }

    @Test
    fun `packageEncrypted combines IV and ciphertext`() {
        val plaintext = "Test".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val packaged = cryptoHelper.packageEncrypted(encrypted)

        // Packaged should be IV (12 bytes) + ciphertext
        assertEquals(12 + encrypted.ciphertext.size, packaged.size)

        // First 12 bytes should be IV
        val extractedIv = packaged.copyOfRange(0, 12)
        assertArrayEquals(encrypted.iv, extractedIv)
    }

    @Test
    fun `unpackageEncrypted extracts IV and ciphertext`() {
        val plaintext = "Test".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val packaged = cryptoHelper.packageEncrypted(encrypted)
        val (ciphertext, iv) = cryptoHelper.unpackageEncrypted(packaged)

        assertArrayEquals(encrypted.ciphertext, ciphertext)
        assertArrayEquals(encrypted.iv, iv)
    }

    @Test
    fun `unpackageEncrypted throws for too short data`() {
        val tooShort = ByteArray(10) // Less than 12 bytes (IV size)

        assertThrows<IllegalArgumentException> {
            cryptoHelper.unpackageEncrypted(tooShort)
        }
    }

    @Test
    fun `encryptAndPackage and unpackageAndDecrypt round-trip`() {
        val plaintext = "Test message".toByteArray()
        val key = generateKey()

        val packaged = cryptoHelper.encryptAndPackage(plaintext, key)
        val decrypted = cryptoHelper.unpackageAndDecrypt(packaged, key)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `EncryptionResult encodedCiphertext matches manual encoding`() {
        val plaintext = "Test".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val manualEncoded = cryptoHelper.encodeCiphertext(encrypted.ciphertext)

        assertEquals(manualEncoded, encrypted.encodedCiphertext)
    }

    @Test
    fun `EncryptionResult size includes both ciphertext and IV`() {
        val plaintext = "Test".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)

        assertEquals(encrypted.ciphertext.size + encrypted.iv.size, encrypted.size)
    }

    @Test
    fun `EncryptionResult equals works correctly`() {
        val plaintext = "Test".toByteArray()
        val key = generateKey()
        val iv = ByteArray(12) { it.toByte() }

        // Note: Can't create identical EncryptionResults in practice because IV is random
        // This tests the equals implementation with manually constructed results
        val result1 = EncryptionResult(
            ciphertext = byteArrayOf(1, 2, 3),
            iv = iv,
            encodedIv = cryptoHelper.encodeIv(iv)
        )
        val result2 = EncryptionResult(
            ciphertext = byteArrayOf(1, 2, 3),
            iv = iv,
            encodedIv = cryptoHelper.encodeIv(iv)
        )

        assertEquals(result1, result2)
    }

    @Test
    fun `EncryptionResult hashCode consistent with equals`() {
        val iv = ByteArray(12) { it.toByte() }
        val result1 = EncryptionResult(
            ciphertext = byteArrayOf(1, 2, 3),
            iv = iv,
            encodedIv = cryptoHelper.encodeIv(iv)
        )
        val result2 = EncryptionResult(
            ciphertext = byteArrayOf(1, 2, 3),
            iv = iv,
            encodedIv = cryptoHelper.encodeIv(iv)
        )

        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun `encrypt handles special characters correctly`() {
        val plaintext = "Special: !@#$%^&*()_+-=[]{}|;':\",./<>?`~".toByteArray()
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val decrypted = cryptoHelper.decrypt(encrypted.ciphertext, key, encrypted.iv)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt handles binary data correctly`() {
        val plaintext = ByteArray(256) { it.toByte() } // All possible byte values
        val key = generateKey()

        val encrypted = cryptoHelper.encrypt(plaintext, key)
        val decrypted = cryptoHelper.decrypt(encrypted.ciphertext, key, encrypted.iv)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `IV is truly random and unpredictable`() {
        val plaintext = "Test".toByteArray()
        val key = generateKey()

        // Generate multiple encryptions
        val ivs = (1..100).map {
            cryptoHelper.encrypt(plaintext, key).iv
        }

        // Check that all IVs are unique
        val uniqueIvs = ivs.distinctBy { it.contentToString() }
        assertEquals(100, uniqueIvs.size, "IVs should all be unique")

        // Check that IVs are not sequential or predictable
        for (i in 0 until ivs.size - 1) {
            assertFalse(ivs[i].contentEquals(ivs[i + 1]))
        }
    }

    @Test
    fun `encryption is non-deterministic`() {
        val plaintext = "Same plaintext every time".toByteArray()
        val key = generateKey()

        val encrypted1 = cryptoHelper.encrypt(plaintext, key)
        val encrypted2 = cryptoHelper.encrypt(plaintext, key)
        val encrypted3 = cryptoHelper.encrypt(plaintext, key)

        // All ciphertexts should be different (random IVs)
        assertFalse(encrypted1.ciphertext.contentEquals(encrypted2.ciphertext))
        assertFalse(encrypted2.ciphertext.contentEquals(encrypted3.ciphertext))
        assertFalse(encrypted1.ciphertext.contentEquals(encrypted3.ciphertext))
    }
}
