package net.meshcore.mineralog.data.service

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * P1-7 & P1-11: Tests for CSV Injection Protection.
 *
 * Tests cover:
 * - Formula injection prevention (=, +, -, @, tab, CR)
 * - Standard CSV escaping (quotes, commas, newlines)
 * - Edge cases and attack vectors
 */
class CsvInjectionProtectionTest {

    private lateinit var csvMapper: MineralCsvMapper

    @Before
    fun setup() {
        csvMapper = MineralCsvMapper()
    }

    // P1-7: CSV Injection Protection Tests

    @Test
    fun `test CSV injection - formula with equals sign`() {
        // Arrange - Attack: =1+1 could execute as formula
        val maliciousValue = "=1+1"

        // Act
        val sanitized = csvMapper.escapeCSV(maliciousValue)

        // Assert - OWASP recommendation: prefix with single quote to preserve data
        assertEquals("'=1+1", sanitized) // Prefixed with ' instead of removing =
        assertTrue(sanitized.startsWith("'"))
        assertFalse(sanitized.startsWith("="))
    }

    @Test
    fun `test CSV injection - formula with plus sign`() {
        // Arrange
        val maliciousValue = "+1234567890"

        // Act
        val sanitized = csvMapper.escapeCSV(maliciousValue)

        // Assert - Data preserved with prefix
        assertEquals("'+1234567890", sanitized)
        assertTrue(sanitized.startsWith("'"))
        assertFalse(sanitized.startsWith("+"))
    }

    @Test
    fun `test CSV injection - formula with minus sign`() {
        // Arrange
        val maliciousValue = "-cmd|'/c calc'!A1"

        // Act
        val sanitized = csvMapper.escapeCSV(maliciousValue)

        // Assert - Data preserved with prefix
        assertEquals("'-cmd|'/c calc'!A1", sanitized)
        assertTrue(sanitized.startsWith("'"))
        assertFalse(sanitized.startsWith("-"))
    }

    @Test
    fun `test CSV injection - formula with at sign`() {
        // Arrange
        val maliciousValue = "@SUM(A1:A10)"

        // Act
        val sanitized = csvMapper.escapeCSV(maliciousValue)

        // Assert - Data preserved with prefix
        assertEquals("'@SUM(A1:A10)", sanitized)
        assertTrue(sanitized.startsWith("'"))
        assertFalse(sanitized.startsWith("@"))
    }

    @Test
    fun `test CSV injection - multiple leading dangerous characters`() {
        // Arrange
        val maliciousValue = "=+@-cmd"

        // Act
        val sanitized = csvMapper.escapeCSV(maliciousValue)

        // Assert - Only the first dangerous character triggers prefix
        assertEquals("'=+@-cmd", sanitized)
        assertTrue(sanitized.startsWith("'"))
        assertFalse(sanitized.startsWith("="))
    }

    @Test
    fun `test CSV injection - tab character`() {
        // Arrange
        val maliciousValue = "\t=1+1"

        // Act
        val sanitized = csvMapper.escapeCSV(maliciousValue)

        // Assert - Tab triggers prefix
        assertEquals("'\t=1+1", sanitized)
        assertTrue(sanitized.startsWith("'"))
        assertFalse(sanitized.startsWith("\t"))
    }

    @Test
    fun `test CSV injection - carriage return`() {
        // Arrange
        val maliciousValue = "\r=HYPERLINK"

        // Act
        val sanitized = csvMapper.escapeCSV(maliciousValue)

        // Assert - CR triggers prefix
        assertEquals("'\r=HYPERLINK", sanitized)
        assertTrue(sanitized.startsWith("'"))
        assertFalse(sanitized.startsWith("\r"))
    }

    // Standard CSV Escaping Tests

    @Test
    fun `test standard CSV escaping - comma in value`() {
        // Arrange
        val valueWithComma = "Locality: Paris, France"

        // Act
        val escaped = csvMapper.escapeCSV(valueWithComma)

        // Assert
        assertTrue(escaped.startsWith("\""))
        assertTrue(escaped.endsWith("\""))
        assertTrue(escaped.contains("Paris, France"))
    }

    @Test
    fun `test standard CSV escaping - quotes in value`() {
        // Arrange
        val valueWithQuotes = "Called \"Fool's Gold\""

        // Act
        val escaped = csvMapper.escapeCSV(valueWithQuotes)

        // Assert
        assertTrue(escaped.contains("\"\"")) // Quotes should be doubled
        assertTrue(escaped.startsWith("\""))
        assertTrue(escaped.endsWith("\""))
    }

    @Test
    fun `test standard CSV escaping - newline in value`() {
        // Arrange
        val valueWithNewline = "Line 1\nLine 2"

        // Act
        val escaped = csvMapper.escapeCSV(valueWithNewline)

        // Assert
        assertTrue(escaped.startsWith("\""))
        assertTrue(escaped.endsWith("\""))
        assertTrue(escaped.contains("\n"))
    }

    @Test
    fun `test CSV escaping - safe value unchanged`() {
        // Arrange
        val safeValue = "Quartz"

        // Act
        val result = csvMapper.escapeCSV(safeValue)

        // Assert
        assertEquals("Quartz", result)
        assertFalse(result.startsWith("\""))
    }

    @Test
    fun `test CSV escaping - empty string`() {
        // Arrange
        val empty = ""

        // Act
        val result = csvMapper.escapeCSV(empty)

        // Assert
        assertEquals("", result)
    }

    // Combined Injection + Escaping Tests

    @Test
    fun `test combined - formula with comma`() {
        // Arrange
        val malicious = "=1+1, attack"

        // Act
        val sanitized = csvMapper.escapeCSV(malicious)

        // Assert - Prefix applied first, then CSV escaping for comma
        assertTrue(sanitized.startsWith("\"")) // Quoted due to comma
        assertTrue(sanitized.contains("'=1+1, attack")) // Prefix inside quotes
        assertFalse(sanitized.startsWith("="))
    }

    @Test
    fun `test combined - formula with quotes`() {
        // Arrange
        val malicious = "=HYPERLINK(\"evil.com\")"

        // Act
        val sanitized = csvMapper.escapeCSV(malicious)

        // Assert - Prefix applied first, then CSV escaping for quotes
        assertTrue(sanitized.startsWith("\"")) // Wrapped in quotes
        assertTrue(sanitized.contains("'=HYPERLINK(")) // Prefix preserved
        assertTrue(sanitized.contains("\"\"")) // Quotes doubled
        assertFalse(sanitized.startsWith("="))
    }

    // Real-world Mineral Data Tests

    @Test
    fun `test real data - normal mineral name`() {
        // Arrange
        val mineralName = "Fluorite"

        // Act
        val result = csvMapper.escapeCSV(mineralName)

        // Assert
        assertEquals("Fluorite", result)
    }

    @Test
    fun `test real data - chemical formula`() {
        // Arrange
        val formula = "CaCO₃"

        // Act
        val result = csvMapper.escapeCSV(formula)

        // Assert
        assertEquals("CaCO₃", result)
    }

    @Test
    fun `test real data - locality with comma`() {
        // Arrange
        val locality = "Mont Blanc, Chamonix, France"

        // Act
        val result = csvMapper.escapeCSV(locality)

        // Assert
        assertTrue(result.startsWith("\""))
        assertTrue(result.contains("Mont Blanc, Chamonix, France"))
    }

    @Test
    fun `test real data - notes with newline`() {
        // Arrange
        val notes = "Found in 2023\nExcellent UV fluorescence"

        // Act
        val result = csvMapper.escapeCSV(notes)

        // Assert
        assertTrue(result.startsWith("\""))
        assertTrue(result.contains("\n"))
    }

    @Test
    fun `test real data - special mineral name with hyphen`() {
        // Arrange - Hyphen in middle is safe, only leading hyphen is dangerous
        val mineralName = "Micro-crystalline Quartz"

        // Act
        val result = csvMapper.escapeCSV(mineralName)

        // Assert
        assertEquals("Micro-crystalline Quartz", result)
        assertFalse(result.startsWith("\""))
    }

    @Test
    fun `test attack vector - DDE injection`() {
        // Arrange - Dynamic Data Exchange attack
        val ddeAttack = "=cmd|'/c calc'!A1"

        // Act
        val sanitized = csvMapper.escapeCSV(ddeAttack)

        // Assert - DDE attack neutralized with prefix, data preserved
        assertEquals("'=cmd|'/c calc'!A1", sanitized)
        assertTrue(sanitized.startsWith("'"))
        assertFalse(sanitized.startsWith("="))
    }

    @Test
    fun `test attack vector - HYPERLINK injection`() {
        // Arrange
        val hyperlinkAttack = "=HYPERLINK(\"http://evil.com\",\"Click\")"

        // Act
        val sanitized = csvMapper.escapeCSV(hyperlinkAttack)

        // Assert - HYPERLINK attack neutralized with prefix
        assertTrue(sanitized.startsWith("\"")) // Quoted due to internal quotes
        assertTrue(sanitized.contains("'=HYPERLINK")) // Prefix preserved inside quotes
        assertFalse(sanitized.startsWith("="))
    }
}
