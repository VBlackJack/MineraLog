package net.meshcore.mineralog.data.model

import kotlinx.serialization.Serializable

/**
 * Represents the manifest metadata for a backup ZIP file.
 * Used to store backup information and encryption details.
 *
 * BUGFIX: Replaced Map<String, Any> with proper @Serializable data class
 * to fix kotlinx.serialization error with Any type.
 */
@Serializable
data class BackupManifest(
    val app: String = "MineraLog",
    val schemaVersion: String = "1.0.0",
    val exportedAt: String, // ISO-8601 timestamp
    val counts: BackupCounts,
    val encrypted: Boolean = false,
    val encryption: EncryptionMetadata? = null
)

/**
 * Statistics about the backup contents.
 */
@Serializable
data class BackupCounts(
    val minerals: Int,
    val photos: Int
)

/**
 * Encryption metadata for encrypted backups.
 */
@Serializable
data class EncryptionMetadata(
    val algorithm: String, // e.g., "Argon2id+AES-256-GCM"
    val salt: String,      // Base64-encoded salt
    val iv: String         // Base64-encoded IV
)
