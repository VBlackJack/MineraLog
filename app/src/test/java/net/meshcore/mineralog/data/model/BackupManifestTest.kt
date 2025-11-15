package net.meshcore.mineralog.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for BackupManifest serialization.
 * Verifies that the manifest can be serialized/deserialized without Map<String, Any> issues.
 */
class BackupManifestTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun `serialize and deserialize unencrypted manifest`() {
        // Given - An unencrypted backup manifest
        val originalManifest = BackupManifest(
            app = "MineraLog",
            schemaVersion = "1.0.0",
            exportedAt = "2025-01-15T10:30:00Z",
            counts = BackupCounts(
                minerals = 5,
                photos = 10
            ),
            encrypted = false,
            encryption = null
        )

        // When - Serialize to JSON
        val jsonString = json.encodeToString(originalManifest)

        // Then - Should not throw serialization error
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("\"app\":\"MineraLog\""))
        assertTrue(jsonString.contains("\"schemaVersion\":\"1.0.0\""))
        assertTrue(jsonString.contains("\"encrypted\":false"))

        // And - Deserialize back
        val deserializedManifest = json.decodeFromString<BackupManifest>(jsonString)

        // Then - Should match original
        assertEquals(originalManifest.app, deserializedManifest.app)
        assertEquals(originalManifest.schemaVersion, deserializedManifest.schemaVersion)
        assertEquals(originalManifest.exportedAt, deserializedManifest.exportedAt)
        assertEquals(originalManifest.counts.minerals, deserializedManifest.counts.minerals)
        assertEquals(originalManifest.counts.photos, deserializedManifest.counts.photos)
        assertEquals(originalManifest.encrypted, deserializedManifest.encrypted)
        assertNull(deserializedManifest.encryption)
    }

    @Test
    fun `serialize and deserialize encrypted manifest`() {
        // Given - An encrypted backup manifest with metadata
        val encryptionMetadata = EncryptionMetadata(
            algorithm = "Argon2id+AES-256-GCM",
            salt = "base64EncodedSalt==",
            iv = "base64EncodedIV=="
        )

        val originalManifest = BackupManifest(
            app = "MineraLog",
            schemaVersion = "1.0.0",
            exportedAt = "2025-01-15T10:30:00Z",
            counts = BackupCounts(
                minerals = 3,
                photos = 7
            ),
            encrypted = true,
            encryption = encryptionMetadata
        )

        // When - Serialize to JSON
        val jsonString = json.encodeToString(originalManifest)

        // Then - Should not throw serialization error
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("\"encrypted\":true"))
        assertTrue(jsonString.contains("\"algorithm\":\"Argon2id+AES-256-GCM\""))
        assertTrue(jsonString.contains("\"salt\":\"base64EncodedSalt==\""))
        assertTrue(jsonString.contains("\"iv\":\"base64EncodedIV==\""))

        // And - Deserialize back
        val deserializedManifest = json.decodeFromString<BackupManifest>(jsonString)

        // Then - Should match original
        assertEquals(originalManifest.app, deserializedManifest.app)
        assertEquals(originalManifest.encrypted, deserializedManifest.encrypted)
        assertNotNull(deserializedManifest.encryption)
        assertEquals(encryptionMetadata.algorithm, deserializedManifest.encryption?.algorithm)
        assertEquals(encryptionMetadata.salt, deserializedManifest.encryption?.salt)
        assertEquals(encryptionMetadata.iv, deserializedManifest.encryption?.iv)
    }

    @Test
    fun `manifest schema version validation`() {
        // Given - Manifest with specific schema version
        val manifest = BackupManifest(
            app = "MineraLog",
            schemaVersion = "1.0.0",
            exportedAt = "2025-01-15T10:30:00Z",
            counts = BackupCounts(minerals = 1, photos = 0),
            encrypted = false
        )

        // When - Serialize and deserialize
        val jsonString = json.encodeToString(manifest)
        val deserialized = json.decodeFromString<BackupManifest>(jsonString)

        // Then - Schema version should be preserved
        assertEquals("1.0.0", deserialized.schemaVersion)
    }

    @Test
    fun `manifest counts are preserved`() {
        // Given - Manifest with specific counts
        val manifest = BackupManifest(
            app = "MineraLog",
            schemaVersion = "1.0.0",
            exportedAt = "2025-01-15T10:30:00Z",
            counts = BackupCounts(
                minerals = 42,
                photos = 128
            ),
            encrypted = false
        )

        // When - Serialize and deserialize
        val jsonString = json.encodeToString(manifest)
        val deserialized = json.decodeFromString<BackupManifest>(jsonString)

        // Then - Counts should be exact
        assertEquals(42, deserialized.counts.minerals)
        assertEquals(128, deserialized.counts.photos)
    }
}
