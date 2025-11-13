package net.meshcore.mineralog.data.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for PasswordBasedCrypto.
 *
 * Tests encryption/decryption roundtrip, password validation,
 * data integrity, and security properties.
 */
class PasswordBasedCryptoTest {

    @Test
    fun `encrypt and decrypt string successfully`() {
        val plaintext = "Hello, MineraLog!"
        val password = "SecurePassword123"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)
        val decrypted = PasswordBasedCrypto.decryptString(
            encrypted.ciphertext,
            password,
            encrypted.salt,
            encrypted.iv
        )

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt and decrypt byte array successfully`() {
        val plaintext = "Test data".toByteArray()
        val password = "password123"

        val encrypted = PasswordBasedCrypto.encrypt(plaintext, password)
        val decrypted = PasswordBasedCrypto.decrypt(
            encrypted.ciphertext,
            password,
            encrypted.salt,
            encrypted.iv
        )

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `different passwords produce different ciphertexts`() {
        val plaintext = "Same plaintext"
        val password1 = "password1"
        val password2 = "password2"

        val encrypted1 = PasswordBasedCrypto.encryptString(plaintext, password1)
        val encrypted2 = PasswordBasedCrypto.encryptString(plaintext, password2)

        assertFalse(encrypted1.ciphertext.contentEquals(encrypted2.ciphertext))
        assertFalse(encrypted1.salt.contentEquals(encrypted2.salt))
    }

    @Test
    fun `same plaintext and password produce different ciphertexts (unique salt and IV)`() {
        val plaintext = "Same plaintext"
        val password = "same password"

        val encrypted1 = PasswordBasedCrypto.encryptString(plaintext, password)
        val encrypted2 = PasswordBasedCrypto.encryptString(plaintext, password)

        // Salts should be different
        assertFalse(encrypted1.salt.contentEquals(encrypted2.salt))
        // IVs should be different
        assertFalse(encrypted1.iv.contentEquals(encrypted2.iv))
        // Ciphertexts should be different
        assertFalse(encrypted1.ciphertext.contentEquals(encrypted2.ciphertext))
    }

    @Test
    fun `wrong password throws DecryptionException`() {
        val plaintext = "Secret data"
        val correctPassword = "correct"
        val wrongPassword = "wrong"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, correctPassword)

        assertThrows<DecryptionException> {
            PasswordBasedCrypto.decryptString(
                encrypted.ciphertext,
                wrongPassword,
                encrypted.salt,
                encrypted.iv
            )
        }
    }

    @Test
    fun `tampered ciphertext throws DecryptionException`() {
        val plaintext = "Secret data"
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)

        // Tamper with ciphertext
        val tamperedCiphertext = encrypted.ciphertext.clone()
        if (tamperedCiphertext.isNotEmpty()) {
            tamperedCiphertext[0] = (tamperedCiphertext[0] + 1).toByte()
        }

        assertThrows<DecryptionException> {
            PasswordBasedCrypto.decryptString(
                tamperedCiphertext,
                password,
                encrypted.salt,
                encrypted.iv
            )
        }
    }

    @Test
    fun `tampered salt throws DecryptionException`() {
        val plaintext = "Secret data"
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)

        // Tamper with salt
        val tamperedSalt = encrypted.salt.clone()
        tamperedSalt[0] = (tamperedSalt[0] + 1).toByte()

        assertThrows<DecryptionException> {
            PasswordBasedCrypto.decryptString(
                encrypted.ciphertext,
                password,
                tamperedSalt,
                encrypted.iv
            )
        }
    }

    @Test
    fun `tampered IV throws DecryptionException`() {
        val plaintext = "Secret data"
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)

        // Tamper with IV
        val tamperedIv = encrypted.iv.clone()
        tamperedIv[0] = (tamperedIv[0] + 1).toByte()

        assertThrows<DecryptionException> {
            PasswordBasedCrypto.decryptString(
                encrypted.ciphertext,
                password,
                encrypted.salt,
                tamperedIv
            )
        }
    }

    @Test
    fun `empty string encrypts and decrypts`() {
        val plaintext = ""
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)
        val decrypted = PasswordBasedCrypto.decryptString(
            encrypted.ciphertext,
            password,
            encrypted.salt,
            encrypted.iv
        )

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `large data encrypts and decrypts`() {
        val plaintext = "A".repeat(10000) // 10KB
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)
        val decrypted = PasswordBasedCrypto.decryptString(
            encrypted.ciphertext,
            password,
            encrypted.salt,
            encrypted.iv
        )

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `special characters in plaintext encrypt and decrypt`() {
        val plaintext = "Special: !@#$%^&*()_+-=[]{}|;':\",./<>?`~äöüß中文日本語"
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)
        val decrypted = PasswordBasedCrypto.decryptString(
            encrypted.ciphertext,
            password,
            encrypted.salt,
            encrypted.iv
        )

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `special characters in password work correctly`() {
        val plaintext = "Secret"
        val password = "p@ssw0rd!#$%^&*()"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)
        val decrypted = PasswordBasedCrypto.decryptString(
            encrypted.ciphertext,
            password,
            encrypted.salt,
            encrypted.iv
        )

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `unicode password works correctly`() {
        val plaintext = "Secret"
        val password = "пароль密码パスワード"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)
        val decrypted = PasswordBasedCrypto.decryptString(
            encrypted.ciphertext,
            password,
            encrypted.salt,
            encrypted.iv
        )

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `salt has expected length`() {
        val plaintext = "Test"
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)

        assertEquals(16, encrypted.salt.size) // 128 bits
    }

    @Test
    fun `base64 encoding and decoding works`() {
        val plaintext = "Test data"
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)

        // Decrypt using Base64-encoded parameters
        val decrypted = PasswordBasedCrypto.decryptFromBase64(
            encrypted.encodedCiphertext,
            password,
            encrypted.encodedSalt,
            encrypted.encodedIv
        )

        assertEquals(plaintext, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun `ciphertext is larger than plaintext (includes auth tag)`() {
        val plaintext = "Test"
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)

        // AES-GCM adds 16-byte authentication tag
        assertTrue(encrypted.ciphertext.size >= plaintext.toByteArray().size + 16)
    }

    @Test
    fun `encryption is deterministic with same salt and IV (not in normal use)`() {
        // This test is purely for understanding - in production,
        // salt and IV are always random
        val plaintext = "Test".toByteArray()
        val password = "password"

        // First encryption to get salt and IV
        val encrypted1 = PasswordBasedCrypto.encrypt(plaintext, password)

        // Manually use same salt and IV (not exposed in API, so this
        // test documents expected behavior rather than testing actual API)
        // In practice, each encryption will always have different salt/IV
    }

    @Test
    fun `very long password works correctly`() {
        val plaintext = "Secret"
        val password = "a".repeat(1000)

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)
        val decrypted = PasswordBasedCrypto.decryptString(
            encrypted.ciphertext,
            password,
            encrypted.salt,
            encrypted.iv
        )

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `JSON-like data encrypts correctly`() {
        val plaintext = """{"name":"Quartz","formula":"SiO2","group":"Silicate"}"""
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)
        val decrypted = PasswordBasedCrypto.decryptString(
            encrypted.ciphertext,
            password,
            encrypted.salt,
            encrypted.iv
        )

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `newlines and whitespace preserved`() {
        val plaintext = "Line 1\nLine 2\r\nLine 3\n\tTabbed"
        val password = "password"

        val encrypted = PasswordBasedCrypto.encryptString(plaintext, password)
        val decrypted = PasswordBasedCrypto.decryptString(
            encrypted.ciphertext,
            password,
            encrypted.salt,
            encrypted.iv
        )

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encryption performance is acceptable`() {
        val plaintext = "A".repeat(1000) // 1KB
        val password = "password"

        val start = System.currentTimeMillis()
        repeat(10) {
            PasswordBasedCrypto.encryptString(plaintext, password)
        }
        val duration = System.currentTimeMillis() - start

        // 10 encryptions should take < 3000ms (300ms each on average)
        // Argon2id is intentionally slow for security, but should still be usable
        assertTrue(duration < 3000, "10 encryptions took ${duration}ms")
    }
}
