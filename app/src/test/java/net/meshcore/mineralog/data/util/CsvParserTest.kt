package net.meshcore.mineralog.data.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Unit tests for CsvParser.
 *
 * Tests encoding detection, delimiter detection, RFC 4180 compliance,
 * error handling, and various CSV formats.
 */
class CsvParserTest {

    private val parser = CsvParser()

    @Test
    fun `parse simple CSV with comma delimiter`() {
        val csv = """
            Name,Group,Formula
            Quartz,Silicate,SiO2
            Calcite,Carbonate,CaCO3
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(listOf("Name", "Group", "Formula"), result.headers)
        assertEquals(2, result.rows.size)
        assertEquals("Quartz", result.rows[0]["Name"])
        assertEquals("Silicate", result.rows[0]["Group"])
        assertEquals("SiO2", result.rows[0]["Formula"])
        assertEquals(',', result.delimiter)
    }

    @Test
    fun `parse CSV with semicolon delimiter`() {
        val csv = """
            Name;Group;Formula
            Quartz;Silicate;SiO2
            Calcite;Carbonate;CaCO3
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(listOf("Name", "Group", "Formula"), result.headers)
        assertEquals(';', result.delimiter)
        assertEquals("Quartz", result.rows[0]["Name"])
    }

    @Test
    fun `parse CSV with tab delimiter`() {
        val csv = "Name\tGroup\tFormula\nQuartz\tSilicate\tSiO2"

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(listOf("Name", "Group", "Formula"), result.headers)
        assertEquals('\t', result.delimiter)
    }

    @Test
    fun `parse CSV with quoted fields containing commas`() {
        val csv = """
            Name,Group,Notes
            "Quartz, Clear",Silicate,"Beautiful, transparent crystal"
            Calcite,Carbonate,Normal note
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals("Quartz, Clear", result.rows[0]["Name"])
        assertEquals("Beautiful, transparent crystal", result.rows[0]["Notes"])
    }

    @Test
    fun `parse CSV with quoted fields containing newlines`() {
        val csv = """
            Name,Notes
            Quartz,"Multi-line
            note with
            newlines"
            Calcite,Simple
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertTrue(result.rows[0]["Notes"]!!.contains("\n"))
        assertEquals("Simple", result.rows[1]["Notes"])
    }

    @Test
    fun `parse CSV with escaped quotes (double quotes)`() {
        val csv = """
            Name,Notes
            Quartz,"This is a ""quoted"" word"
            Calcite,Normal
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals("This is a \"quoted\" word", result.rows[0]["Notes"])
    }

    @Test
    fun `detect UTF-8 encoding`() {
        val csv = "Name,Group\nQuartz,Silicate"
        val bytes = csv.toByteArray(StandardCharsets.UTF_8)

        val result = parser.parse(ByteArrayInputStream(bytes))

        assertEquals("UTF-8", result.encoding.name())
    }

    @Test
    fun `detect UTF-8 with BOM`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val csv = "Name,Group\nQuartz,Silicate"
        val bytes = bom + csv.toByteArray(StandardCharsets.UTF_8)

        val result = parser.parse(ByteArrayInputStream(bytes))

        assertEquals("UTF-8", result.encoding.name())
        assertEquals("Name", result.headers[0]) // BOM should be stripped
    }

    @Test
    fun `handle empty CSV`() {
        val csv = ""

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertTrue(result.headers.isEmpty())
        assertTrue(result.rows.isEmpty())
    }

    @Test
    fun `handle CSV with only headers`() {
        val csv = "Name,Group,Formula"

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(3, result.headers.size)
        assertTrue(result.rows.isEmpty())
    }

    @Test
    fun `handle empty fields`() {
        val csv = """
            Name,Group,Formula
            Quartz,,SiO2
            ,Carbonate,
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals("", result.rows[0]["Group"])
        assertEquals("", result.rows[1]["Name"])
        assertEquals("", result.rows[1]["Formula"])
    }

    @Test
    fun `handle rows with different number of fields`() {
        val csv = """
            Name,Group,Formula
            Quartz,Silicate,SiO2
            Calcite,Carbonate
            Pyrite,Sulfide,FeS2,Extra
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(3, result.rows.size)
        // Row 1: Missing Formula
        assertEquals("", result.rows[1]["Formula"])
        // Row 2: Extra field should be ignored (not in headers)
        assertEquals(3, result.rows[2].size)
    }

    @Test
    fun `parse with maxRows limit`() {
        val csv = """
            Name,Group
            Quartz,Silicate
            Calcite,Carbonate
            Pyrite,Sulfide
            Galena,Sulfide
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()), maxRows = 2)

        assertEquals(2, result.rows.size)
        assertEquals("Quartz", result.rows[0]["Name"])
        assertEquals("Calcite", result.rows[1]["Name"])
    }

    @Test
    fun `handle whitespace around fields`() {
        val csv = """
            Name,Group,Formula
            " Quartz ",  Silicate  , SiO2
            Calcite,Carbonate,CaCO3
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        // Quoted field: whitespace inside quotes is preserved
        assertEquals(" Quartz ", result.rows[0]["Name"])
        // Unquoted fields: whitespace is trimmed by default
        assertEquals("Silicate", result.rows[0]["Group"]?.trim())
    }

    @Test
    fun `detect most common delimiter when ambiguous`() {
        val csv = """
            Name,Group;Formula
            Quartz,Silicate;SiO2
            Calcite,Carbonate;CaCO3
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        // Comma appears more frequently than semicolon
        assertEquals(',', result.delimiter)
    }

    @Test
    fun `collect errors for malformed rows`() {
        val csv = """
            Name,Group,Formula
            Quartz,Silicate,SiO2
            "Unclosed quote,Carbonate,CaCO3
            Pyrite,Sulfide,FeS2
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        // Should have error for unclosed quote
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.message.contains("Unclosed", ignoreCase = true) ||
                                        it.message.contains("quote", ignoreCase = true) })
    }

    @Test
    fun `parse real-world mineralog export`() {
        val csv = """
            Name,Group,Formula,Mohs Min,Mohs Max,Status,Status Type
            "Quartz, Clear",Silicate,SiO2,7.0,7.0,complete,in_collection
            Calcite,Carbonate,CaCO3,3.0,3.0,incomplete,in_collection
            "Pyrite ""Fool's Gold""",Sulfide,FeS2,6.0,6.5,complete,on_loan
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(7, result.headers.size)
        assertEquals(3, result.rows.size)
        assertEquals("Quartz, Clear", result.rows[0]["Name"])
        assertEquals("Pyrite \"Fool's Gold\"", result.rows[2]["Name"])
        assertEquals("7.0", result.rows[0]["Mohs Min"])
        assertEquals("6.5", result.rows[2]["Mohs Max"])
    }

    @Test
    fun `parse with French locale (semicolon delimiter)`() {
        val csv = """
            Nom;Groupe;Formule
            Quartz;Silicate;SiO2
            Calcite;Carbonate;CaCO3
        """.trimIndent()

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(';', result.delimiter)
        assertEquals("Nom", result.headers[0])
        assertEquals("Quartz", result.rows[0]["Nom"])
    }

    @Test
    fun `handle CRLF line endings (Windows)`() {
        val csv = "Name,Group\r\nQuartz,Silicate\r\nCalcite,Carbonate\r\n"

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(2, result.rows.size)
        assertEquals("Quartz", result.rows[0]["Name"])
        assertEquals("Calcite", result.rows[1]["Name"])
    }

    @Test
    fun `handle LF line endings (Unix)`() {
        val csv = "Name,Group\nQuartz,Silicate\nCalcite,Carbonate\n"

        val result = parser.parse(ByteArrayInputStream(csv.toByteArray()))

        assertEquals(2, result.rows.size)
    }

    @Test
    fun `parse large CSV efficiently`() {
        // Generate 1000-row CSV
        val sb = StringBuilder("Name,Group,Formula\n")
        repeat(1000) { i ->
            sb.append("Mineral$i,Group$i,Formula$i\n")
        }

        val start = System.currentTimeMillis()
        val result = parser.parse(ByteArrayInputStream(sb.toString().toByteArray()))
        val duration = System.currentTimeMillis() - start

        assertEquals(1000, result.rows.size)
        // Should parse 1000 rows in < 500ms (performance target)
        assertTrue(duration < 500, "Parsing took ${duration}ms, expected < 500ms")
    }
}
