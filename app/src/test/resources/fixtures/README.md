# CSV Test Fixtures

This directory contains CSV test files for validating the MineraLog import functionality.

## Files Overview

### 1. `test_basic.csv`
**Purpose:** Basic happy path testing
**Rows:** 10 minerals
**Encoding:** UTF-8 (no BOM)
**Delimiter:** Comma (`,`)
**Complexity:** Low

**Characteristics:**
- Standard column headers (Name, Group, Formula, etc.)
- All required fields present
- No special characters or edge cases
- Simple provenance and storage data
- All values within valid ranges

**Expected Import Result:**
- ✅ 10 minerals imported successfully
- ✅ 0 validation errors
- ✅ All columns auto-mapped correctly

**Use Cases:**
- Smoke testing CSV import flow
- Testing auto-column mapping with standard headers
- Verifying basic import success feedback

---

### 2. `test_complex.csv`
**Purpose:** Real-world complexity testing
**Rows:** 20 minerals (simulates larger dataset)
**Encoding:** UTF-8 with BOM
**Delimiter:** Comma (`,`)
**Complexity:** High

**Characteristics:**
- **All 37 columns** from CSV spec (full schema coverage)
- Quoted fields with embedded commas (e.g., `"Quartz, Rose"`, `"Notes with, commas"`)
- Special characters: accents (é, ñ, ø), Unicode formulas (₂, ₃, ⁴), symbols (°, ×)
- Mixed data types: floats, integers, booleans, dates (ISO-8601)
- Geographic coordinates (latitude/longitude with proper ranges)
- Tags as semicolon-separated lists
- Full provenance, storage, and physical properties
- Scientific notation values for specific gravity

**Expected Import Result:**
- ✅ 20 minerals imported successfully
- ✅ 0 validation errors
- ✅ UTF-8 BOM correctly handled by parser
- ✅ Quoted fields with commas parsed correctly
- ✅ All domain fields mapped (name, group, mohs, provenance, storage, etc.)

**Use Cases:**
- Testing UTF-8 encoding detection (with BOM)
- Validating RFC 4180 CSV parsing (quoted fields, embedded commas)
- Testing auto-mapping with all possible column variations
- Verifying complex data type conversions (float, date, boolean)
- Stress testing preview UI with many columns

---

### 3. `test_invalid.csv`
**Purpose:** Validation error testing
**Rows:** 20 minerals (10 valid, 10 invalid)
**Encoding:** UTF-8 (no BOM)
**Delimiter:** Comma (`,`)
**Complexity:** Medium

**Validation Errors (Expected):**
1. **Row 2:** Missing required field `name` (blank)
2. **Row 4:** Mohs hardness 15.0 > max allowed (10.0)
3. **Row 7:** Latitude 95.0 > max allowed (90.0)
4. **Row 9:** Longitude 200.0 > max allowed (180.0)
5. **Row 11:** Mohs Min (8.0) > Mohs Max (3.5)
6. **Row 17:** Negative Mohs hardness (-2.0)

**Expected Import Result:**
- ✅ ~14 minerals imported successfully
- ⚠️ ~6 minerals skipped with validation errors
- ⚠️ Error messages displayed line-by-line:
  - `"Row 2: Name is required"`
  - `"Row 4: Mohs Min must be between 1.0 and 10.0 (got: 15.0)"`
  - `"Row 7: Latitude must be between -90.0 and 90.0 (got: 95.0)"`
  - `"Row 9: Longitude must be between -180.0 and 180.0 (got: 200.0)"`
  - `"Row 11: Mohs Min cannot exceed Mohs Max (8.0 > 3.5)"`
  - `"Row 17: Mohs Min must be between 1.0 and 10.0 (got: -2.0)"`

**Use Cases:**
- Testing validation logic in `BackupRepository.importCsv()`
- Verifying error messages are user-friendly and actionable
- Testing `ImportResultDialog` UI with mixed success/error results
- Ensuring partial imports succeed (fail-fast disabled)

---

### 4. `test_encrypted_backup.zip` (To be generated)
**Purpose:** Encryption round-trip testing
**Contents:** 50 minerals + manifest.json (encrypted)
**Password:** `Test1234!`
**Encryption:** Argon2id + AES-256-GCM

**Note:** This fixture should be generated programmatically using `BackupRepository.exportZip()`
with encryption enabled. It will be created during instrumentation tests or via a test utility script.

**Expected Import Result:**
- ✅ Password dialog appears
- ✅ Correct password decrypts successfully
- ✅ 50 minerals imported
- ❌ Wrong password triggers error with retry prompt

**Use Cases:**
- Testing decryption flow with `DecryptPasswordDialog`
- Verifying password retry logic (3 attempts)
- Testing encryption round-trip (export encrypted → import decrypted)

---

## Column Mapping Reference

### Standard Header Variations Supported

The `CsvColumnMapper` recognizes these header variations (case-insensitive, ignores spaces/underscores):

| Domain Field | Recognized Headers |
|--------------|-------------------|
| `name` | name, mineral_name, specimen_name, mineral |
| `group` | group, mineral_group, classification |
| `formula` | formula, chemical_formula, composition |
| `mohs` | mohs, hardness, mohs_min (if only one column) |
| `mohsMin` | mohs_min, mohs min, hardness_min |
| `mohsMax` | mohs_max, mohs max, hardness_max |
| `crystalSystem` | crystal_system, crystal system, system |
| `luster` | luster, lustre |
| `prov_country` | country, provenance_country, pays |
| `prov_locality` | locality, provenance_locality, localité |
| `storage_place` | place, storage_place, location, lieu |

**See:** `CsvParser.kt` → `CsvColumnMapper` object for full mapping table.

---

## Testing Workflow

### Manual Testing
```bash
# 1. Import test_basic.csv via UI
# Expected: ✅ Success snackbar "10 minerals imported"

# 2. Import test_complex.csv via UI
# Expected: ✅ Success snackbar "20 minerals imported"
# Verify: UTF-8 characters display correctly, quoted fields parsed

# 3. Import test_invalid.csv via UI
# Expected: ⚠️ ImportResultDialog shows "14 imported, 6 skipped"
# Verify: Error list displays 6 validation errors with line numbers

# 4. Import test_encrypted_backup.zip via UI
# Expected: 🔒 Password dialog appears
# Enter: "Test1234!" → ✅ Success "50 minerals imported"
# Enter: "WrongPassword" → ❌ Error "Wrong password. 2 attempts remaining"
```

### Unit Testing
```kotlin
// CsvParserTest.kt
@Test
fun `parse test_basic CSV succeeds`() {
    val fixture = javaClass.getResourceAsStream("/fixtures/test_basic.csv")
    val result = parser.parse(fixture!!)

    assertEquals(10, result.rows.size)
    assertEquals(0, result.errors.size)
    assertTrue(result.headers.contains("Name"))
}

// BackupRepositoryTest.kt
@Test
fun `importCsv with test_invalid shows validation errors`() = runTest {
    val uri = Uri.parse("android.resource://fixtures/test_invalid.csv")
    val result = repository.importCsv(uri).getOrThrow()

    assertTrue(result.imported >= 14)
    assertTrue(result.skipped >= 6)
    assertTrue(result.errors.any { it.contains("Mohs") })
    assertTrue(result.errors.any { it.contains("Latitude") })
}
```

---

## Maintenance

When updating fixtures:
1. ✅ Ensure UTF-8 encoding (use `file -I <filename>` to verify)
2. ✅ Validate CSV syntax with online validators (RFC 4180)
3. ✅ Update this README with new characteristics
4. ✅ Re-run all tests after fixture changes

---

**Last updated:** 2025-11-14
**Maintainer:** MineraLog Team
