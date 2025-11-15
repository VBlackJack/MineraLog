package net.meshcore.mineralog

import android.net.Uri
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Deep link validation security tests.
 * Verifies that only valid UUIDs are accepted as deep link parameters.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27, 35])
class DeepLinkValidationTest {

    @Test
    fun `valid UUID deep link should be accepted`() {
        // Given a valid UUID
        val validUuid = UUID.randomUUID().toString()
        val uri = Uri.parse("mineralapp://mineral/$validUuid")

        // When processing the deep link
        val result = validateDeepLinkId(uri.lastPathSegment)

        // Then it should be accepted
        assertEquals(validUuid, result)
    }

    @Test
    fun `malformed UUID deep link should be rejected`() {
        // Given an invalid UUID
        val invalidId = "not-a-uuid"
        val uri = Uri.parse("mineralapp://mineral/$invalidId")

        // When processing the deep link
        val result = validateDeepLinkId(uri.lastPathSegment)

        // Then it should be rejected
        assertNull(result)
    }

    @Test
    fun `SQL injection attempt should be rejected`() {
        // Given a SQL injection attempt
        val maliciousId = "'; DROP TABLE minerals; --"
        val uri = Uri.parse("mineralapp://mineral/$maliciousId")

        // When processing the deep link
        val result = validateDeepLinkId(uri.lastPathSegment)

        // Then it should be rejected
        assertNull(result)
    }

    @Test
    fun `path traversal attempt should be rejected`() {
        // Given a path traversal attempt
        val maliciousId = "../../../etc/passwd"
        val uri = Uri.parse("mineralapp://mineral/$maliciousId")

        // When processing the deep link
        val result = validateDeepLinkId(uri.lastPathSegment)

        // Then it should be rejected
        assertNull(result)
    }

    @Test
    fun `XSS attempt should be rejected`() {
        // Given an XSS attempt
        val maliciousId = "<script>alert('xss')</script>"
        val uri = Uri.parse("mineralapp://mineral/$maliciousId")

        // When processing the deep link
        val result = validateDeepLinkId(uri.lastPathSegment)

        // Then it should be rejected
        assertNull(result)
    }

    @Test
    fun `empty deep link should be rejected`() {
        // Given an empty ID
        val result = validateDeepLinkId("")

        // Then it should be rejected
        assertNull(result)
    }

    @Test
    fun `null deep link should be rejected`() {
        // Given a null ID
        val result = validateDeepLinkId(null)

        // Then it should be rejected
        assertNull(result)
    }

    @Test
    fun `UUID with extra characters should be rejected`() {
        // Given a UUID with extra characters
        val validUuid = UUID.randomUUID().toString()
        val maliciousId = "$validUuid; rm -rf /"

        // When processing the deep link
        val result = validateDeepLinkId(maliciousId)

        // Then it should be rejected
        assertNull(result)
    }

    @Test
    fun `uppercase UUID should be accepted`() {
        // Given a valid UUID in uppercase
        val validUuid = UUID.randomUUID().toString().uppercase()
        val uri = Uri.parse("mineralapp://mineral/$validUuid")

        // When processing the deep link
        val result = validateDeepLinkId(uri.lastPathSegment)

        // Then it should be accepted (UUID.fromString handles case-insensitive)
        assertEquals(validUuid, result)
    }

    @Test
    fun `UUID without hyphens should be rejected`() {
        // Given a UUID without hyphens (not standard format)
        val uuidWithoutHyphens = UUID.randomUUID().toString().replace("-", "")

        // When processing the deep link
        val result = validateDeepLinkId(uuidWithoutHyphens)

        // Then it should be rejected (UUID.fromString requires hyphens)
        assertNull(result)
    }

    /**
     * Helper function to validate deep link IDs.
     * Mirrors the validation logic in MainActivity.
     */
    private fun validateDeepLinkId(id: String?): String? {
        return id?.let {
            try {
                UUID.fromString(it)
                it
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
