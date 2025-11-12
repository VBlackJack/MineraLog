package net.meshcore.mineralog.data.mapper

import net.meshcore.mineralog.data.local.entity.MineralEntity
import net.meshcore.mineralog.domain.model.Mineral
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class EntityMappersTest {

    @Test
    fun `test mineral entity to domain mapping`() {
        // Given
        val entity = MineralEntity(
            id = "test-id",
            name = "Quartz",
            group = "Silicates",
            formula = "SiO₂",
            mohsMin = 7.0f,
            mohsMax = 7.0f,
            tags = "tag1,tag2,tag3",
            status = "complete",
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-02T00:00:00Z")
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertEquals("test-id", domain.id)
        assertEquals("Quartz", domain.name)
        assertEquals("Silicates", domain.group)
        assertEquals("SiO₂", domain.formula)
        assertEquals(7.0f, domain.mohsMin)
        assertEquals(7.0f, domain.mohsMax)
        assertEquals(listOf("tag1", "tag2", "tag3"), domain.tags)
        assertEquals("complete", domain.status)
    }

    @Test
    fun `test domain to mineral entity mapping`() {
        // Given
        val domain = Mineral(
            id = "test-id",
            name = "Fluorite",
            group = "Halides",
            formula = "CaF₂",
            mohsMin = 4.0f,
            mohsMax = 4.0f,
            tags = listOf("display", "favorites"),
            status = "complete",
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-02T00:00:00Z")
        )

        // When
        val entity = domain.toEntity()

        // Then
        assertEquals("test-id", entity.id)
        assertEquals("Fluorite", entity.name)
        assertEquals("Halides", entity.group)
        assertEquals("CaF₂", entity.formula)
        assertEquals(4.0f, entity.mohsMin)
        assertEquals(4.0f, entity.mohsMax)
        assertEquals("display,favorites", entity.tags)
        assertEquals("complete", entity.status)
    }

    @Test
    fun `test tags parsing with empty string`() {
        // Given
        val entity = MineralEntity(
            id = "test-id",
            name = "Test",
            tags = ""
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertTrue(domain.tags.isEmpty())
    }

    @Test
    fun `test tags parsing with null`() {
        // Given
        val entity = MineralEntity(
            id = "test-id",
            name = "Test",
            tags = null
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertTrue(domain.tags.isEmpty())
    }
}
