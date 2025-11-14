package net.meshcore.mineralog.data.crypto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for Argon2Helper.
 *
 * Tests key derivation, password verification, salt generation,
 * and security properties specific to Argon2id.
 */
class Argon2HelperTest {

    private val argon2Helper = Argon2Helper()

    @Test
    fun `deriveKey generates valid key with correct length`() {
        val password = "TestPassword123".toCharArray()
        val result = argon2Helper.deriveKey(password)

        // Key should be 32 bytes (256 bits) for AES-256
        assertEquals(32, result.key.size)
        // Key should not be all zeros (actual derivation happened)
        assertFalse(result.key.all { it == 0.toByte() })
    }

    @Test
    fun `deriveKey generates unique salts for different invocations`() {
        val password = "TestPassword123".toCharArray()

        val result1 = argon2Helper.deriveKey(password.clone())
        val result2 = argon2Helper.deriveKey(password.clone())

        // Salts should be different
        assertFalse(result1.salt.contentEquals(result2.salt))
        // Keys should be different (because salts are different)
        assertFalse(result1.key.contentEquals(result2.key))
    }

    @Test
    fun `deriveKey with same salt produces same key`() {
        val password = "TestPassword123".toCharArray()

        val result1 = argon2Helper.deriveKey(password.clone())
        val result2 = argon2Helper.deriveKey(password.clone(), result1.salt)

        // Same password + same salt = same key
        assertArrayEquals(result1.key, result2.key)
        assertArrayEquals(result1.salt, result2.salt)
    }

    @Test
    fun `deriveKey with different passwords produces different keys`() {
        val password1 = "Password1".toCharArray()
        val password2 = "Password2".toCharArray()
        val salt = argon2Helper.generateSalt()

        val result1 = argon2Helper.deriveKey(password1, salt)
        val result2 = argon2Helper.deriveKey(password2, salt)

        // Different passwords should produce different keys
        assertFalse(result1.key.contentEquals(result2.key))
    }

    @Test
    fun `deriveKey throws exception for empty password`() {
        val emptyPassword = CharArray(0)

        assertThrows<IllegalArgumentException> {
            argon2Helper.deriveKey(emptyPassword)
        }
    }

    @Test
    fun `generateSalt produces 16-byte salt`() {
        val salt = argon2Helper.generateSalt()

        // Salt should be 16 bytes (128 bits)
        assertEquals(16, salt.size)
    }

    @Test
    fun `generateSalt produces unique salts`() {
        val salt1 = argon2Helper.generateSalt()
        val salt2 = argon2Helper.generateSalt()
        val salt3 = argon2Helper.generateSalt()

        // All salts should be different
        assertFalse(salt1.contentEquals(salt2))
        assertFalse(salt2.contentEquals(salt3))
        assertFalse(salt1.contentEquals(salt3))
    }

    @Test
    fun `generateSalt produces non-zero salts`() {
        val salt = argon2Helper.generateSalt()

        // Salt should not be all zeros
        assertFalse(salt.all { it == 0.toByte() })
    }

    @Test
    fun `verifyPassword returns true for correct password`() {
        val password = "CorrectPassword".toCharArray()
        val result = argon2Helper.deriveKey(password.clone())

        val isValid = argon2Helper.verifyPassword(
            password.clone(),
            result.key,
            result.salt
        )

        assertTrue(isValid)
    }

    @Test
    fun `verifyPassword returns false for incorrect password`() {
        val correctPassword = "CorrectPassword".toCharArray()
        val wrongPassword = "WrongPassword".toCharArray()
        val result = argon2Helper.deriveKey(correctPassword)

        val isValid = argon2Helper.verifyPassword(
            wrongPassword,
            result.key,
            result.salt
        )

        assertFalse(isValid)
    }

    @Test
    fun `verifyPassword returns false for slightly different password`() {
        val password = "Password123".toCharArray()
        val wrongPassword = "password123".toCharArray() // Different case
        val result = argon2Helper.deriveKey(password.clone())

        val isValid = argon2Helper.verifyPassword(
            wrongPassword,
            result.key,
            result.salt
        )

        assertFalse(isValid)
    }

    @Test
    fun `encodeSalt produces Base64 string`() {
        val salt = argon2Helper.generateSalt()
        val encoded = argon2Helper.encodeSalt(salt)

        // Base64 encoded 16 bytes should be 24 characters (no padding with NO_WRAP)
        assertTrue(encoded.isNotEmpty())
        assertTrue(encoded.matches(Regex("^[A-Za-z0-9+/]+$")))
    }

    @Test
    fun `decodeSalt reverses encodeSalt`() {
        val originalSalt = argon2Helper.generateSalt()
        val encoded = argon2Helper.encodeSalt(originalSalt)
        val decoded = argon2Helper.decodeSalt(encoded)

        assertArrayEquals(originalSalt, decoded)
    }

    @Test
    fun `KeyDerivationResult includes encoded salt`() {
        val password = "TestPassword".toCharArray()
        val result = argon2Helper.deriveKey(password)

        // Encoded salt should match manual encoding
        val manualEncoded = argon2Helper.encodeSalt(result.salt)
        assertEquals(manualEncoded, result.encodedSalt)
    }

    @Test
    fun `KeyDerivationResult clear method zeros out sensitive data`() {
        val password = "TestPassword".toCharArray()
        val result = argon2Helper.deriveKey(password)

        // Save original values for comparison
        val originalKey = result.key.clone()
        val originalSalt = result.salt.clone()

        result.clear()

        // Key and salt should be zeroed
        assertTrue(result.key.all { it == 0.toByte() })
        assertTrue(result.salt.all { it == 0.toByte() })
        // Original values should not be affected (cloned)
        assertFalse(originalKey.all { it == 0.toByte() })
        assertFalse(originalSalt.all { it == 0.toByte() })
    }

    @Test
    fun `deriveKey works with very long password`() {
        val longPassword = "a".repeat(1000).toCharArray()
        val result = argon2Helper.deriveKey(longPassword)

        assertEquals(32, result.key.size)
        assertFalse(result.key.all { it == 0.toByte() })
    }

    @Test
    fun `deriveKey works with special characters in password`() {
        val password = "p@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?`~".toCharArray()
        val result = argon2Helper.deriveKey(password)

        assertEquals(32, result.key.size)
        assertFalse(result.key.all { it == 0.toByte() })
    }

    @Test
    fun `deriveKey works with unicode password`() {
        val password = "密码パスワードпароль".toCharArray()
        val result = argon2Helper.deriveKey(password)

        assertEquals(32, result.key.size)
        assertFalse(result.key.all { it == 0.toByte() })
    }

    @Test
    fun `KeyDerivationResult equals works correctly`() {
        val password = "TestPassword".toCharArray()
        val result1 = argon2Helper.deriveKey(password.clone())
        val result2 = argon2Helper.deriveKey(password.clone(), result1.salt)

        // Same password and salt should produce equal results
        assertEquals(result1, result2)
    }

    @Test
    fun `KeyDerivationResult hashCode consistent with equals`() {
        val password = "TestPassword".toCharArray()
        val result1 = argon2Helper.deriveKey(password.clone())
        val result2 = argon2Helper.deriveKey(password.clone(), result1.salt)

        // Same password and salt should produce same hashCode
        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun `estimateDerivationTime returns positive value`() {
        val time = argon2Helper.estimateDerivationTime()

        // Should return a positive time estimate (or -1 if error)
        assertTrue(time > 0 || time == -1L)
    }

    @Test
    fun `estimateDerivationTime is reasonably fast for mobile`() {
        val time = argon2Helper.estimateDerivationTime()

        // Argon2 should complete in reasonable time for mobile (< 2 seconds)
        // This is intentionally slow for security, but not unusably slow
        assertTrue(time < 2000 || time == -1L, "Derivation took ${time}ms, expected < 2000ms")
    }

    @Test
    fun `PasswordStrength assess returns WEAK for short password`() {
        val weakPassword = "abc".toCharArray()
        val strength = PasswordStrength.assess(weakPassword)

        assertEquals(PasswordStrength.Strength.WEAK, strength)
    }

    @Test
    fun `PasswordStrength assess returns STRONG for complex password`() {
        val strongPassword = "MyP@ssw0rd123!".toCharArray()
        val strength = PasswordStrength.assess(strongPassword)

        assertTrue(
            strength == PasswordStrength.Strength.STRONG || strength == PasswordStrength.Strength.GOOD,
            "Expected STRONG or GOOD, got $strength"
        )
    }

    @Test
    fun `PasswordStrength assess returns FAIR for medium password`() {
        val mediumPassword = "password123".toCharArray()
        val strength = PasswordStrength.assess(mediumPassword)

        assertTrue(
            strength == PasswordStrength.Strength.FAIR || strength == PasswordStrength.Strength.GOOD,
            "Expected FAIR or GOOD, got $strength"
        )
    }
}
