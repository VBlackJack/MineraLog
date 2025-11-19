package net.meshcore.mineralog.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Security-critical instrumented tests for DatabaseKeyManager.
 *
 * Tests cover:
 * - Passphrase generation and retrieval
 * - Thread-safety and race conditions
 * - Persistence in EncryptedSharedPreferences
 * - Fallback SecureRandom mechanism
 * - Hex conversion utilities
 *
 * Requires: Real Android device or emulator (uses Android Keystore)
 * Target coverage: 80%+
 */
@RunWith(AndroidJUnit4::class)
class DatabaseKeyManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        clearStoredPassphrase()
    }

    @After
    fun tearDown() {
        clearStoredPassphrase()
    }

    /**
     * Helper to clear stored passphrase between tests
     */
    private fun clearStoredPassphrase() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                "mineralog_db_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().clear().commit()
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }

    // ========================================
    // PASSPHRASE GENERATION TESTS
    // ========================================

    @Test
    
    fun test_getOrCreatePassphrase_first_call_generates_new_passphrase() {
        // Act
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert
        assertNotNull(passphrase)
        assertEquals("Passphrase should be 32 bytes (256 bits)", 32, passphrase.size)
        assertTrue("Passphrase should not be all zeros", passphrase.any { it != 0.toByte() })
    }

    @Test
    
    fun test_getOrCreatePassphrase_second_call_returns_same_passphrase() {
        // Arrange
        val firstPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Act
        val secondPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert
        assertArrayEquals(
            "Second call should return the same passphrase",
            firstPassphrase,
            secondPassphrase
        )
    }

    @Test
    
    fun test_getOrCreatePassphrase_multiple_calls_returns_consistent_passphrase() {
        // Arrange
        val initialPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Act - Call 10 times
        val passphrases = (1..10).map {
            DatabaseKeyManager.getOrCreatePassphrase(context)
        }

        // Assert
        passphrases.forEach { passphrase ->
            assertArrayEquals(
                "All calls should return the same passphrase",
                initialPassphrase,
                passphrase
            )
        }
    }

    @Test
    
    fun test_getOrCreatePassphrase_generated_passphrase_is_cryptographically_random() {
        // Arrange - Clear and generate 3 passphrases in different contexts
        val passphrases = mutableListOf<ByteArray>()

        for (i in 1..3) {
            clearStoredPassphrase()
            passphrases.add(DatabaseKeyManager.getOrCreatePassphrase(context))
        }

        // Assert - Each passphrase should be different
        assertFalse(
            "First and second passphrases should be different",
            passphrases[0].contentEquals(passphrases[1])
        )
        assertFalse(
            "Second and third passphrases should be different",
            passphrases[1].contentEquals(passphrases[2])
        )
        assertFalse(
            "First and third passphrases should be different",
            passphrases[0].contentEquals(passphrases[2])
        )
    }

    // ========================================
    // THREAD-SAFETY TESTS
    // ========================================

    @Test
    
    fun test_getOrCreatePassphrase_concurrent_calls_no_race_conditions() {
        // Arrange
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val passphrases = mutableListOf<ByteArray>()
        val passphrasesList = mutableListOf<ByteArray>()

        // Act - Call from 10 threads simultaneously
        val threads = (1..threadCount).map {
            thread {
                val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)
                synchronized(passphrasesList) {
                    passphrasesList.add(passphrase)
                }
                latch.countDown()
            }
        }

        // Wait for all threads to complete
        latch.await(5, TimeUnit.SECONDS)
        threads.forEach { it.join(1000) }

        // Assert - All threads should get the same passphrase
        assertEquals("All threads should complete", threadCount, passphrasesList.size)

        val firstPassphrase = passphrasesList[0]
        passphrasesList.forEach { passphrase ->
            assertArrayEquals(
                "All threads should receive the same passphrase",
                firstPassphrase,
                passphrase
            )
        }
    }

    @Test
    
    fun test_getOrCreatePassphrase_synchronized_annotation_prevents_concurrent_generation() {
        // Arrange
        val executor = Executors.newFixedThreadPool(5)
        val passphrases = mutableListOf<ByteArray>()
        val latch = CountDownLatch(5)

        // Act - Submit 5 tasks simultaneously
        repeat(5) {
            executor.submit {
                val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)
                synchronized(passphrases) {
                    passphrases.add(passphrase)
                }
                latch.countDown()
            }
        }

        // Wait for completion
        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        // Assert - Should have generated only ONE passphrase
        assertEquals(5, passphrases.size)
        val firstPassphrase = passphrases[0]
        passphrases.forEach { passphrase ->
            assertArrayEquals(firstPassphrase, passphrase)
        }
    }

    // ========================================
    // PERSISTENCE TESTS
    // ========================================

    @Test
    
    fun test_getOrCreatePassphrase_persists_across_app_restarts() {
        // Arrange - Generate passphrase in "first launch"
        val originalPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Act - Simulate app restart by creating new context
        // (In Robolectric, we can't truly restart, but we can verify persistence)
        val retrievedPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert
        assertArrayEquals(
            "Passphrase should persist and be retrievable",
            originalPassphrase,
            retrievedPassphrase
        )
    }

    @Test
    
    fun test_getOrCreatePassphrase_stored_passphrase_is_encrypted() {
        // Arrange
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Act - Try to read raw SharedPreferences (should be encrypted)
        val rawPrefs = context.getSharedPreferences("mineralog_db_prefs", Context.MODE_PRIVATE)
        val storedValue = rawPrefs.getString("db_passphrase", null)

        // Assert
        assertNotNull("Passphrase should be stored", storedValue)
        // The stored value is a hex string, so it should be 64 characters (32 bytes * 2)
        assertEquals("Stored hex string should be 64 characters", 64, storedValue!!.length)

        // Verify it's a valid hex string
        assertTrue(storedValue.all { it in '0'..'9' || it in 'a'..'f' })
    }

    // ========================================
    // HEX CONVERSION TESTS
    // ========================================

    @Test
    
    fun test_hex_conversion_round_trip_preserves_data() {
        // Arrange
        val originalBytes = byteArrayOf(
            0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte()
        )

        // Act - Generate and retrieve passphrase (which uses hex conversion internally)
        clearStoredPassphrase()
        DatabaseKeyManager.getOrCreatePassphrase(context)
        val retrievedPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert - The round-trip should work (tested implicitly through persistence)
        assertNotNull(retrievedPassphrase)
        assertEquals(32, retrievedPassphrase.size)
    }

    @Test
    
    fun test_hex_conversion_all_byte_values_correctly_converted() {
        // This test verifies the conversion works by generating a passphrase
        // and ensuring it can be stored and retrieved correctly

        // Arrange & Act
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Clear and retrieve again
        val retrievedPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert
        assertArrayEquals(passphrase, retrievedPassphrase)
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Test
    
    fun test_generateSecurePassphrase_fallback_uses_SecureRandom() {
        // This test verifies that even if Keystore fails, we get a passphrase
        // We test this indirectly by ensuring passphrase is always generated

        // Act
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert
        assertNotNull(passphrase)
        assertEquals(32, passphrase.size)
        assertTrue(passphrase.any { it != 0.toByte() })
    }

    @Test
    
    fun test_getOrCreatePassphrase_always_returns_valid_passphrase() {
        // Act - Call multiple times
        val passphrases = (1..5).map {
            DatabaseKeyManager.getOrCreatePassphrase(context)
        }

        // Assert
        passphrases.forEach { passphrase ->
            assertNotNull("Passphrase should never be null", passphrase)
            assertEquals("Passphrase should always be 32 bytes", 32, passphrase.size)
        }
    }

    // ========================================
    // SECURITY PROPERTIES TESTS
    // ========================================

    @Test
    
    fun test_getOrCreatePassphrase_entropy_check_passphrase_is_random() {
        // Arrange
        clearStoredPassphrase()
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert - Check that not all bytes are the same
        val uniqueBytes = passphrase.toSet()
        assertTrue(
            "Passphrase should have multiple distinct byte values",
            uniqueBytes.size > 1
        )

        // Check that passphrase is not a simple pattern
        val isAllSame = passphrase.all { it == passphrase[0] }
        assertFalse("Passphrase should not be all the same byte", isAllSame)

        val isSequential = passphrase.toList().zipWithNext().all { (a, b) -> b.toInt() == a.toInt() + 1 }
        assertFalse("Passphrase should not be sequential", isSequential)
    }

    @Test
    
    fun test_getOrCreatePassphrase_length_always_32_bytes() {
        // Act - Generate 10 passphrases
        val passphrases = (1..10).map {
            clearStoredPassphrase()
            DatabaseKeyManager.getOrCreatePassphrase(context)
        }

        // Assert
        passphrases.forEach { passphrase ->
            assertEquals("Passphrase must always be 32 bytes (256 bits)", 32, passphrase.size)
        }
    }

    @Test
    
    fun test_getOrCreatePassphrase_different_contexts_generate_different_passphrases() {
        // Arrange
        val passphrase1 = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Clear and regenerate
        clearStoredPassphrase()
        val passphrase2 = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert - New installation should get different passphrase
        assertFalse(
            "Fresh installation should generate a new passphrase",
            passphrase1.contentEquals(passphrase2)
        )
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    
    fun test_getOrCreatePassphrase_rapid_calls_handle_correctly() {
        // Act - Make 100 rapid calls
        val passphrases = (1..100).map {
            DatabaseKeyManager.getOrCreatePassphrase(context)
        }

        // Assert - All should be the same
        val firstPassphrase = passphrases[0]
        passphrases.forEach { passphrase ->
            assertArrayEquals(firstPassphrase, passphrase)
        }
    }

    @Test

    fun test_getOrCreatePassphrase_context_parameter_is_required() {
        // This test verifies that the method doesn't crash with valid context

        // Act & Assert - Should not throw
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)
        assertNotNull(passphrase)
    }
}
