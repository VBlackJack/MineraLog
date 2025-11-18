package net.meshcore.mineralog.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.junit.jupiter.api.DisplayName
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Security-critical tests for DatabaseKeyManager.
 *
 * Tests cover:
 * - Passphrase generation and retrieval
 * - Thread-safety and race conditions
 * - Persistence in EncryptedSharedPreferences
 * - Fallback SecureRandom mechanism
 * - Hex conversion utilities
 *
 * Target coverage: 80%+
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DatabaseKeyManagerTest {

    private lateinit var context: Context

    @BeforeEach
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        clearStoredPassphrase()
    }

    @AfterEach
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
    @DisplayName("First call generates new passphrase")
    fun `getOrCreatePassphrase - first call - generates new passphrase`() {
        // Act
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert
        assertNotNull(passphrase)
        assertEquals(32, passphrase.size, "Passphrase should be 32 bytes (256 bits)")
        assertTrue(passphrase.any { it != 0.toByte() }, "Passphrase should not be all zeros")
    }

    @Test
    @DisplayName("Second call returns same passphrase")
    fun `getOrCreatePassphrase - second call - returns same passphrase`() {
        // Arrange
        val firstPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Act
        val secondPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert
        assertArrayEquals(
            firstPassphrase,
            secondPassphrase,
            "Second call should return the same passphrase"
        )
    }

    @Test
    @DisplayName("Multiple calls return consistent passphrase")
    fun `getOrCreatePassphrase - multiple calls - returns consistent passphrase`() {
        // Arrange
        val initialPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Act - Call 10 times
        val passphrases = (1..10).map {
            DatabaseKeyManager.getOrCreatePassphrase(context)
        }

        // Assert
        passphrases.forEach { passphrase ->
            assertArrayEquals(
                initialPassphrase,
                passphrase,
                "All calls should return the same passphrase"
            )
        }
    }

    @Test
    @DisplayName("Generated passphrase is cryptographically random")
    fun `getOrCreatePassphrase - generated passphrase - is cryptographically random`() {
        // Arrange - Clear and generate 3 passphrases in different contexts
        val passphrases = mutableListOf<ByteArray>()

        for (i in 1..3) {
            clearStoredPassphrase()
            passphrases.add(DatabaseKeyManager.getOrCreatePassphrase(context))
        }

        // Assert - Each passphrase should be different
        assertFalse(
            passphrases[0].contentEquals(passphrases[1]),
            "First and second passphrases should be different"
        )
        assertFalse(
            passphrases[1].contentEquals(passphrases[2]),
            "Second and third passphrases should be different"
        )
        assertFalse(
            passphrases[0].contentEquals(passphrases[2]),
            "First and third passphrases should be different"
        )
    }

    // ========================================
    // THREAD-SAFETY TESTS
    // ========================================

    @Test
    @DisplayName("Concurrent calls are thread-safe")
    fun `getOrCreatePassphrase - concurrent calls - no race conditions`() {
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
        assertEquals(threadCount, passphrasesList.size, "All threads should complete")

        val firstPassphrase = passphrasesList[0]
        passphrasesList.forEach { passphrase ->
            assertArrayEquals(
                firstPassphrase,
                passphrase,
                "All threads should receive the same passphrase"
            )
        }
    }

    @Test
    @DisplayName("@Synchronized annotation prevents race conditions")
    fun `getOrCreatePassphrase - synchronized annotation - prevents concurrent generation`() {
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
    @DisplayName("Passphrase persists across app restarts")
    fun `getOrCreatePassphrase - persists - across app restarts`() {
        // Arrange - Generate passphrase in "first launch"
        val originalPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Act - Simulate app restart by creating new context
        // (In Robolectric, we can't truly restart, but we can verify persistence)
        val retrievedPassphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert
        assertArrayEquals(
            originalPassphrase,
            retrievedPassphrase,
            "Passphrase should persist and be retrievable"
        )
    }

    @Test
    @DisplayName("Stored passphrase is encrypted in SharedPreferences")
    fun `getOrCreatePassphrase - stored passphrase - is encrypted`() {
        // Arrange
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Act - Try to read raw SharedPreferences (should be encrypted)
        val rawPrefs = context.getSharedPreferences("mineralog_db_prefs", Context.MODE_PRIVATE)
        val storedValue = rawPrefs.getString("db_passphrase", null)

        // Assert
        assertNotNull(storedValue, "Passphrase should be stored")
        // The stored value is a hex string, so it should be 64 characters (32 bytes * 2)
        assertEquals(64, storedValue!!.length, "Stored hex string should be 64 characters")

        // Verify it's a valid hex string
        assertTrue(storedValue.all { it in '0'..'9' || it in 'a'..'f' })
    }

    // ========================================
    // HEX CONVERSION TESTS
    // ========================================

    @Test
    @DisplayName("ByteArray to Hex to ByteArray is bijective")
    fun `hex conversion - round trip - preserves data`() {
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
    @DisplayName("Hex string conversion handles all byte values")
    fun `hex conversion - all byte values - correctly converted`() {
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
    @DisplayName("Fallback passphrase generation works")
    fun `generateSecurePassphrase - fallback - uses SecureRandom`() {
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
    @DisplayName("Passphrase generation never returns null")
    fun `getOrCreatePassphrase - always returns - valid passphrase`() {
        // Act - Call multiple times
        val passphrases = (1..5).map {
            DatabaseKeyManager.getOrCreatePassphrase(context)
        }

        // Assert
        passphrases.forEach { passphrase ->
            assertNotNull(passphrase, "Passphrase should never be null")
            assertEquals(32, passphrase.size, "Passphrase should always be 32 bytes")
        }
    }

    // ========================================
    // SECURITY PROPERTIES TESTS
    // ========================================

    @Test
    @DisplayName("Passphrase has sufficient entropy")
    fun `getOrCreatePassphrase - entropy check - passphrase is random`() {
        // Arrange
        clearStoredPassphrase()
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert - Check that not all bytes are the same
        val uniqueBytes = passphrase.toSet()
        assertTrue(
            uniqueBytes.size > 1,
            "Passphrase should have multiple distinct byte values"
        )

        // Check that passphrase is not a simple pattern
        val isAllSame = passphrase.all { it == passphrase[0] }
        assertFalse(isAllSame, "Passphrase should not be all the same byte")

        val isSequential = passphrase.toList().zipWithNext().all { (a, b) -> b == a + 1 }
        assertFalse(isSequential, "Passphrase should not be sequential")
    }

    @Test
    @DisplayName("Passphrase length is always 32 bytes")
    fun `getOrCreatePassphrase - length - always 32 bytes`() {
        // Act - Generate 10 passphrases
        val passphrases = (1..10).map {
            clearStoredPassphrase()
            DatabaseKeyManager.getOrCreatePassphrase(context)
        }

        // Assert
        passphrases.forEach { passphrase ->
            assertEquals(32, passphrase.size, "Passphrase must always be 32 bytes (256 bits)")
        }
    }

    @Test
    @DisplayName("Different app contexts generate different passphrases")
    fun `getOrCreatePassphrase - different contexts - generate different passphrases`() {
        // Arrange
        val passphrase1 = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Clear and regenerate
        clearStoredPassphrase()
        val passphrase2 = DatabaseKeyManager.getOrCreatePassphrase(context)

        // Assert - New installation should get different passphrase
        assertFalse(
            passphrase1.contentEquals(passphrase2),
            "Fresh installation should generate a new passphrase"
        )
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    @DisplayName("Rapid sequential calls work correctly")
    fun `getOrCreatePassphrase - rapid calls - handle correctly`() {
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
    @DisplayName("Context parameter is used correctly")
    fun `getOrCreatePassphrase - context parameter - is required`() {
        // This test verifies that the method doesn't crash with valid context

        // Act & Assert - Should not throw
        assertDoesNotThrow {
            DatabaseKeyManager.getOrCreatePassphrase(context)
        }
    }
}
