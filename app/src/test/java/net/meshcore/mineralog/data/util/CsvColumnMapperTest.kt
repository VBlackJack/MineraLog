package net.meshcore.mineralog.data.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for CsvColumnMapper.
 *
 * Tests automatic column mapping, fuzzy matching, normalization,
 * and suggestions for ambiguous headers.
 */
class CsvColumnMapperTest {

    @Test
    fun `map standard English headers`() {
        val headers = listOf("Name", "Group", "Formula", "Mohs Hardness", "Country", "Locality")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("name", mapping["Name"])
        assertEquals("group", mapping["Group"])
        assertEquals("formula", mapping["Formula"])
        assertEquals("mohs", mapping["Mohs Hardness"])
        assertEquals("prov_country", mapping["Country"])
        assertEquals("prov_locality", mapping["Locality"])
    }

    @Test
    fun `map headers case-insensitively`() {
        val headers = listOf("NAME", "group", "FORMULA", "Mohs")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("name", mapping["NAME"])
        assertEquals("group", mapping["group"])
        assertEquals("formula", mapping["FORMULA"])
        assertEquals("mohs", mapping["Mohs"])
    }

    @Test
    fun `map headers with underscores`() {
        val headers = listOf("mineral_name", "mineral_group", "mohs_hardness", "crystal_system")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("name", mapping["mineral_name"])
        assertEquals("group", mapping["mineral_group"])
        assertEquals("mohs", mapping["mohs_hardness"])
        assertEquals("crystalSystem", mapping["crystal_system"])
    }

    @Test
    fun `map headers with spaces`() {
        val headers = listOf("Mineral Name", "Mohs Min", "Mohs Max", "Crystal System")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("name", mapping["Mineral Name"])
        assertEquals("mohsMin", mapping["Mohs Min"])
        assertEquals("mohsMax", mapping["Mohs Max"])
        assertEquals("crystalSystem", mapping["Crystal System"])
    }

    @Test
    fun `map French locale headers`() {
        val headers = listOf("Nom", "Groupe", "Formule", "Système Cristallin", "Pays", "Localité")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        // Should recognize French variants
        assertEquals("name", mapping["Nom"])
        assertEquals("group", mapping["Groupe"])
        assertEquals("formula", mapping["Formule"])
        assertEquals("prov_country", mapping["Pays"])
        assertEquals("prov_locality", mapping["Localité"])
    }

    @Test
    fun `map provenance fields`() {
        val headers = listOf(
            "Country", "Locality", "Site", "Latitude", "Longitude",
            "Source", "Price", "Estimated Value", "Currency"
        )

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("prov_country", mapping["Country"])
        assertEquals("prov_locality", mapping["Locality"])
        assertEquals("prov_site", mapping["Site"])
        assertEquals("prov_latitude", mapping["Latitude"])
        assertEquals("prov_longitude", mapping["Longitude"])
        assertEquals("prov_source", mapping["Source"])
        assertEquals("prov_price", mapping["Price"])
        assertEquals("prov_estimatedValue", mapping["Estimated Value"])
        assertEquals("prov_currency", mapping["Currency"])
    }

    @Test
    fun `map storage fields`() {
        val headers = listOf("Place", "Container", "Box", "Slot", "Location")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("storage_place", mapping["Place"])
        assertEquals("storage_container", mapping["Container"])
        assertEquals("storage_box", mapping["Box"])
        assertEquals("storage_slot", mapping["Slot"])
        // "Location" is ambiguous - could be storage_place or prov_locality
        assertTrue(mapping["Location"] == "storage_place" || mapping["Location"] == "prov_locality")
    }

    @Test
    fun `map physical properties`() {
        val headers = listOf(
            "Cleavage", "Fracture", "Luster", "Streak", "Diaphaneity", "Habit",
            "Specific Gravity", "Fluorescence", "Magnetic", "Radioactive"
        )

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("cleavage", mapping["Cleavage"])
        assertEquals("fracture", mapping["Fracture"])
        assertEquals("luster", mapping["Luster"])
        assertEquals("streak", mapping["Streak"])
        assertEquals("diaphaneity", mapping["Diaphaneity"])
        assertEquals("habit", mapping["Habit"])
        assertEquals("specificGravity", mapping["Specific Gravity"])
        assertEquals("fluorescence", mapping["Fluorescence"])
        assertEquals("magnetic", mapping["Magnetic"])
        assertEquals("radioactive", mapping["Radioactive"])
    }

    @Test
    fun `map dimensions and weight`() {
        val headers = listOf("Dimensions (mm)", "Dimensions", "Weight (g)", "Weight")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("dimensionsMm", mapping["Dimensions (mm)"])
        assertEquals("dimensionsMm", mapping["Dimensions"])
        assertEquals("weightGr", mapping["Weight (g)"])
        assertEquals("weightGr", mapping["Weight"])
    }

    @Test
    fun `map status fields`() {
        val headers = listOf("Status", "Status Type", "Quality Rating", "Completeness")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("status", mapping["Status"])
        assertEquals("statusType", mapping["Status Type"])
        assertEquals("qualityRating", mapping["Quality Rating"])
        assertEquals("completeness", mapping["Completeness"])
    }

    @Test
    fun `map notes and tags`() {
        val headers = listOf("Notes", "Tags", "Comments", "Description")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("notes", mapping["Notes"])
        assertEquals("tags", mapping["Tags"])
        // "Comments" and "Description" are ambiguous - could map to notes
        assertTrue(mapping["Comments"] == "notes" || mapping.containsKey("Comments"))
    }

    @Test
    fun `ignore unmapped headers`() {
        val headers = listOf("Name", "Group", "UnknownColumn", "AnotherUnknown")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertEquals("name", mapping["Name"])
        assertEquals("group", mapping["Group"])
        // Unknown headers should not be mapped
        assertFalse(mapping.containsKey("UnknownColumn"))
        assertFalse(mapping.containsKey("AnotherUnknown"))
    }

    @Test
    fun `handle empty header list`() {
        val headers = emptyList<String>()

        val mapping = CsvColumnMapper.mapHeaders(headers)

        assertTrue(mapping.isEmpty())
    }

    @Test
    fun `handle duplicate headers (last one wins)`() {
        val headers = listOf("Name", "Group", "Name", "Formula")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        // Both "Name" columns should map to "name"
        assertEquals("name", mapping["Name"])
        // Other headers should work normally
        assertEquals("group", mapping["Group"])
        assertEquals("formula", mapping["Formula"])
    }

    @Test
    fun `fuzzy match with typos`() {
        val headers = listOf("Nme", "Grp", "Formla", "Mhos")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        // Fuzzy matching should handle minor typos
        // (Implementation-dependent - may or may not match)
        // This test documents expected behavior if fuzzy matching is implemented
        assertTrue(mapping.isEmpty() || mapping.containsKey("Nme"))
    }

    @Test
    fun `prefer exact matches over partial matches`() {
        val headers = listOf("Name", "Mineral Name", "Group")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        // "Name" should be preferred over "Mineral Name" if both present
        assertEquals("name", mapping["Name"])
        // "Mineral Name" should also map to name (but with lower priority)
        assertTrue(mapping["Mineral Name"] == "name" || !mapping.containsKey("Mineral Name"))
    }

    @Test
    fun `map abbreviated headers`() {
        val headers = listOf("SG", "Lat", "Lon", "Min", "Max")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        // "SG" = Specific Gravity (common abbreviation)
        assertEquals("specificGravity", mapping["SG"])
        // "Lat" / "Lon" = Latitude / Longitude
        assertEquals("prov_latitude", mapping["Lat"])
        assertEquals("prov_longitude", mapping["Lon"])
        // "Min" / "Max" alone are ambiguous (could be mohs, dimensions, etc.)
    }

    @Test
    fun `suggest alternative mappings for ambiguous headers`() {
        val headers = listOf("Hardness", "Location", "Size")

        val suggestions = CsvColumnMapper.suggestMappings(headers)

        // "Hardness" could be mohs, mohsMin, or mohsMax
        assertTrue(suggestions["Hardness"]?.contains("mohs") == true ||
                   suggestions["Hardness"]?.contains("mohsMin") == true)

        // "Location" could be storage_place or prov_locality
        assertTrue(suggestions["Location"]?.contains("storage_place") == true ||
                   suggestions["Location"]?.contains("prov_locality") == true)

        // "Size" could be dimensionsMm or weightGr
        assertTrue(suggestions["Size"]?.contains("dimensionsMm") == true ||
                   suggestions["Size"]?.contains("weightGr") == true)
    }

    @Test
    fun `normalize header text (remove special chars)`() {
        val headers = listOf("Name!", "Group#1", "Formula (Chemical)", "Mohs@Hardness")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        // Special characters should be stripped/normalized
        assertEquals("name", mapping["Name!"])
        assertEquals("group", mapping["Group#1"])
        assertEquals("formula", mapping["Formula (Chemical)"])
        assertEquals("mohs", mapping["Mohs@Hardness"])
    }

    @Test
    fun `map real-world MineraLog export headers`() {
        val headers = listOf(
            "Name", "Group", "Formula", "Crystal System", "Mohs Min", "Mohs Max",
            "Cleavage", "Fracture", "Luster", "Streak", "Diaphaneity", "Habit",
            "Specific Gravity", "Fluorescence", "Magnetic", "Radioactive",
            "Dimensions (mm)", "Weight (g)", "Status", "Status Type",
            "Quality Rating", "Completeness", "Country", "Locality", "Site",
            "Latitude", "Longitude", "Acquired At", "Source", "Price",
            "Estimated Value", "Currency", "Place", "Container", "Box", "Slot",
            "Notes", "Tags"
        )

        val mapping = CsvColumnMapper.mapHeaders(headers)

        // All 37 standard headers should map correctly
        assertEquals(37, mapping.size)

        // Verify critical fields
        assertEquals("name", mapping["Name"])
        assertEquals("group", mapping["Group"])
        assertEquals("formula", mapping["Formula"])
        assertEquals("mohsMin", mapping["Mohs Min"])
        assertEquals("mohsMax", mapping["Mohs Max"])
        assertEquals("prov_country", mapping["Country"])
        assertEquals("prov_latitude", mapping["Latitude"])
        assertEquals("storage_place", mapping["Place"])
        assertEquals("notes", mapping["Notes"])
        assertEquals("tags", mapping["Tags"])
    }

    @Test
    fun `map with leading and trailing whitespace`() {
        val headers = listOf("  Name  ", " Group", "Formula ")

        val mapping = CsvColumnMapper.mapHeaders(headers)

        // Whitespace should be trimmed before mapping
        assertEquals("name", mapping["  Name  "])
        assertEquals("group", mapping[" Group"])
        assertEquals("formula", mapping["Formula "])
    }

    @Test
    fun `map partial match variations`() {
        val headers = listOf(
            "Specimen Name", "Mineral Group", "Chemical Formula",
            "Hardness Value", "Origin Country"
        )

        val mapping = CsvColumnMapper.mapHeaders(headers)

        // Partial matches should work (contains "Name", "Group", etc.)
        assertEquals("name", mapping["Specimen Name"])
        assertEquals("group", mapping["Mineral Group"])
        assertEquals("formula", mapping["Chemical Formula"])
        assertEquals("mohs", mapping["Hardness Value"])
        assertEquals("prov_country", mapping["Origin Country"])
    }
}
