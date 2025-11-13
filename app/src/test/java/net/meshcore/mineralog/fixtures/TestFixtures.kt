package net.meshcore.mineralog.fixtures

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.meshcore.mineralog.domain.model.Mineral
import net.meshcore.mineralog.domain.model.Photo
import net.meshcore.mineralog.domain.model.Provenance
import net.meshcore.mineralog.domain.model.Storage
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Test fixtures for BackupRepository and other tests.
 * Provides reusable test data for minerals, ZIPs, and databases.
 */
object TestFixtures {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Create a minimal valid mineral.
     */
    fun createMineral(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Mineral",
        formula: String? = "XYZ",
        group: String? = "Test Group",
        mohsMin: Float? = 5.0f,
        mohsMax: Float? = 6.0f,
        provenance: Provenance? = null,
        storage: Storage? = null,
        photos: List<Photo> = emptyList()
    ): Mineral {
        return Mineral(
            id = id,
            name = name,
            formula = formula,
            group = group,
            mohsMin = mohsMin,
            mohsMax = mohsMax,
            statusType = "in_collection",
            completeness = 50,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            provenance = provenance,
            storage = storage,
            photos = photos
        )
    }

    /**
     * Create a provenance.
     */
    fun createProvenance(
        mineralId: String,
        country: String? = "France",
        locality: String? = "Paris",
        latitude: Double? = 48.8566,
        longitude: Double? = 2.3522,
        price: Float? = 100f,
        currency: String = "USD"
    ): Provenance {
        return Provenance(
            id = UUID.randomUUID().toString(),
            mineralId = mineralId,
            country = country,
            locality = locality,
            latitude = latitude,
            longitude = longitude,
            price = price,
            estimatedValue = price?.times(1.5f),
            currency = currency,
            acquiredAt = Instant.now()
        )
    }

    /**
     * Create a storage.
     */
    fun createStorage(
        mineralId: String,
        place: String? = "Cabinet A",
        container: String? = "Drawer 1"
    ): Storage {
        return Storage(
            id = UUID.randomUUID().toString(),
            mineralId = mineralId,
            place = place,
            container = container
        )
    }

    /**
     * Create a photo.
     */
    fun createPhoto(
        mineralId: String,
        fileName: String = "photo_${UUID.randomUUID()}.jpg"
    ): Photo {
        return Photo(
            id = UUID.randomUUID().toString(),
            mineralId = mineralId,
            fileName = fileName,
            type = "NORMAL",
            takenAt = Instant.now()
        )
    }

    /**
     * Create a valid unencrypted ZIP backup.
     */
    fun createValidZipBackup(minerals: List<Mineral>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            // manifest.json
            val manifest = mapOf(
                "app" to "MineraLog",
                "schemaVersion" to "1.0.0",
                "exportedAt" to Instant.now().toString(),
                "counts" to mapOf(
                    "minerals" to minerals.size,
                    "photos" to minerals.sumOf { it.photos.size }
                ),
                "encrypted" to false
            )
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(json.encodeToString(manifest).toByteArray())
            zip.closeEntry()

            // minerals.json
            zip.putNextEntry(ZipEntry("minerals.json"))
            zip.write(json.encodeToString(minerals).toByteArray())
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    /**
     * Create a corrupted ZIP (truncated).
     */
    fun createCorruptedZip(): ByteArray {
        val validZip = createValidZipBackup(listOf(createMineral(name = "Quartz")))
        // Truncate to 50% to simulate interrupted download
        return validZip.copyOfRange(0, validZip.size / 2)
    }

    /**
     * Create a ZIP bomb (high decompression ratio).
     */
    fun createZipBomb(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            // Create a highly compressible file (10MB of zeros)
            val largeData = ByteArray(10 * 1024 * 1024) { 0 }

            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"app":"MineraLog","schemaVersion":"1.0.0"}""".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("minerals.json"))
            zip.write(largeData) // This will compress very well (high ratio)
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    /**
     * Create a ZIP with path injection attempt.
     */
    fun createPathInjectionZip(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            // Try to escape to parent directory
            zip.putNextEntry(ZipEntry("../../../etc/passwd"))
            zip.write("malicious content".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("""{"app":"MineraLog","schemaVersion":"1.0.0"}""".toByteArray())
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    /**
     * Create a ZIP with invalid schema version.
     */
    fun createInvalidSchemaVersionZip(): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            val manifest = mapOf(
                "app" to "MineraLog",
                "schemaVersion" to "2.0.0", // Future incompatible version
                "exportedAt" to Instant.now().toString()
            )
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(json.encodeToString(manifest).toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("minerals.json"))
            zip.write(json.encodeToString(listOf(createMineral())).toByteArray())
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    /**
     * Write bytes to a temporary file.
     */
    fun writeTempFile(bytes: ByteArray, dir: File, name: String): File {
        val file = File(dir, name)
        file.writeBytes(bytes)
        return file
    }

    // ===== PDF Label Generation Fixtures =====

    /**
     * Mineral with a very long name for testing text wrapping.
     */
    val longNameMineral = Mineral(
        id = "test-long-name",
        name = "Potassium Aluminum Silicate Hydroxide Fluoride Complex",
        formula = "KAl₂(AlSi₃O₁₀)(F,OH)₂",
        group = "Phyllosilicates - Mica Group Minerals with Extended Classification",
        statusType = "in_collection",
        completeness = 100,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    /**
     * Mineral with unicode characters in name and formula.
     */
    val unicodeMineral = Mineral(
        id = "test-unicode",
        name = "Azurite α-crystal",
        formula = "Cu₃(CO₃)₂(OH)₂",
        group = "Carbonates",
        statusType = "in_collection",
        completeness = 100,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    /**
     * Minimal mineral with only required fields.
     */
    val minimalMineral = Mineral(
        id = "test-minimal",
        name = "Unknown",
        formula = null,
        group = null,
        statusType = "in_collection",
        completeness = 0,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    /**
     * Generate a batch of 100 minerals for performance testing.
     */
    fun batch100Minerals(): List<Mineral> {
        return (1..100).map { i ->
            Mineral(
                id = "batch-$i",
                name = "Mineral #$i",
                formula = "XYZ$i",
                group = "Test Group",
                statusType = "in_collection",
                completeness = 50,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }
    }

    /**
     * Generate a batch of 1000 minerals for stress testing.
     */
    fun batch1000Minerals(): List<Mineral> {
        return (1..1000).map { i ->
            Mineral(
                id = "large-$i",
                name = "Specimen $i",
                formula = "ABC$i",
                group = "Test Group",
                statusType = "in_collection",
                completeness = 50,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        }
    }
}
