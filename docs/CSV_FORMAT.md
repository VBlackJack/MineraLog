# Reference Minerals CSV Format

> **Version:** 1.0
> **Date:** 2025-01-16
> **Purpose:** Documentation of the `reference_minerals.csv` format used in MineraLog backups

---

## Overview

The `reference_minerals.csv` file contains the reference mineral library data. It is included in ZIP backups created by MineraLog and can be imported/exported independently.

## File Structure

### Encoding
- **Character Set:** UTF-8
- **Line Ending:** LF (`\n`) or CRLF (`\r\n`)
- **BOM:** Not required

### CSV Format
- **Delimiter:** Comma (`,`)
- **Quote Character:** Double quote (`"`)
- **Escape Sequence:** Doubled quote (`""`) for literal quotes
- **Header Row:** Required (first line)

### Quoting Rules
- Fields containing commas, quotes, or newlines **MUST** be quoted
- Quotes within quoted fields must be escaped by doubling them (`""`)
- Empty values are represented as empty strings (no quotes needed)

**Example:**
```csv
id,nameFr,nameEn,notes
abc-123,Quartz,"Quartz, alpha","This mineral has ""excellent"" clarity"
```

---

## Column Specification

The CSV **must** contain the following columns in this exact order:

| # | Column Name | Type | Required | Description |
|---|-------------|------|----------|-------------|
| 1 | `id` | UUID | ✅ Yes | Unique identifier (UUID format) |
| 2 | `nameFr` | String | ✅ Yes | French name of the mineral |
| 3 | `nameEn` | String | ✅ Yes | English name of the mineral |
| 4 | `synonyms` | String | ❌ No | Alternative names (comma-separated) |
| 5 | `mineralGroup` | String | ❌ No | Mineralogical group (e.g., "Silicates", "Carbonates") |
| 6 | `formula` | String | ❌ No | Chemical formula (e.g., "SiO₂", "CaCO₃") |
| 7 | `mohsMin` | Float | ❌ No | Minimum Mohs hardness (1.0-10.0) |
| 8 | `mohsMax` | Float | ❌ No | Maximum Mohs hardness (1.0-10.0) |
| 9 | `density` | Float | ❌ No | Density in g/cm³ |
| 10 | `crystalSystem` | String | ❌ No | Crystal system (e.g., "Hexagonal", "Cubic") |
| 11 | `cleavage` | String | ❌ No | Cleavage type |
| 12 | `fracture` | String | ❌ No | Fracture type |
| 13 | `habit` | String | ❌ No | Crystal habit |
| 14 | `luster` | String | ❌ No | Luster type (e.g., "Vitreous", "Metallic") |
| 15 | `streak` | String | ❌ No | Streak color |
| 16 | `diaphaneity` | String | ❌ No | Transparency (e.g., "Transparent", "Opaque") |
| 17 | `fluorescence` | String | ❌ No | Fluorescence properties |
| 18 | `magnetism` | String | ❌ No | Magnetic properties |
| 19 | `radioactivity` | String | ❌ No | Radioactivity level |
| 20 | `notes` | Text | ❌ No | Additional notes (can contain newlines if quoted) |
| 21 | `isUserDefined` | Boolean | ✅ Yes | `true` if user-created, `false` if from standard library |
| 22 | `source` | String | ❌ No | Data source (e.g., "mindat.org", "User-created") |
| 23 | `createdAt` | ISO 8601 | ✅ Yes | Creation timestamp (e.g., `2025-01-16T10:30:00Z`) |
| 24 | `updatedAt` | ISO 8601 | ✅ Yes | Last update timestamp (e.g., `2025-01-16T15:45:00Z`) |

---

## Data Types

### UUID
- Format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
- Example: `550e8400-e29b-41d4-a716-446655440000`

### String
- Max length: Unlimited (practical limit: 1000 characters)
- Unicode support: Full UTF-8

### Float
- Format: Decimal number (period as decimal separator)
- Examples: `2.5`, `7.0`, `2.65`

### Boolean
- Valid values: `true`, `false` (case-sensitive)

### ISO 8601 Timestamp
- Format: `YYYY-MM-DDTHH:MM:SS.sssZ` or `YYYY-MM-DDTHH:MM:SSZ`
- Examples:
  - `2025-01-16T10:30:00Z`
  - `2025-01-16T10:30:00.123Z`

---

## Example CSV

```csv
id,nameFr,nameEn,synonyms,mineralGroup,formula,mohsMin,mohsMax,density,crystalSystem,cleavage,fracture,habit,luster,streak,diaphaneity,fluorescence,magnetism,radioactivity,notes,isUserDefined,source,createdAt,updatedAt
550e8400-e29b-41d4-a716-446655440000,Quartz,Quartz,"Quartz alpha, Rock crystal",Silicates,SiO₂,7.0,7.0,2.65,Hexagonal,Aucun,Conchoïdale,Prismatique,Vitreux,Blanc,Transparent,Non fluorescent,Non magnétique,Non radioactif,"Très commun, utilisé en joaillerie",false,mindat.org,2025-01-15T08:00:00Z,2025-01-15T08:00:00Z
8b3a5c2e-1234-5678-9abc-def012345678,Calcite,Calcite,Spath d'Islande,Carbonates,CaCO₃,3.0,3.0,2.71,Trigonal,Parfait,Conchoïdale,Rhomboédrique,Vitreux,Blanc,Transparent à translucide,"UV courte, UV longue",Non magnétique,Non radioactif,Double réfraction notable,false,webmineral.com,2025-01-15T09:00:00Z,2025-01-15T09:00:00Z
7c4f9a1b-abcd-ef12-3456-789012345abc,"Fluorite personnalisée","Custom Fluorite",Fluorine,Halogénures,CaF₂,4.0,4.0,3.18,Cubique,Parfait,Conchoïdale,Cubique,Vitreux,Blanc,Transparent,"UV courte, UV longue - Bleu intense",Non magnétique,Non radioactif,"Spécimen rare trouvé dans ma région, fluorescence exceptionnelle",true,Collection personnelle,2025-01-16T14:00:00Z,2025-01-16T14:30:00Z
```

---

## Import/Export Behavior

### Export
- **Automatic:** Reference minerals CSV is automatically included in all ZIP backups if the library contains at least one mineral
- **File name:** `reference_minerals.csv`
- **Location in ZIP:** Root level (same as `minerals.json`)

### Import

#### Import Modes

| Mode | Behavior | Use Case |
|------|----------|----------|
| **MERGE** | Update existing (by ID), insert new | Sync libraries between devices |
| **REPLACE** | Delete all, then insert | Full library replacement |
| **MAP_IDS** | Remap conflicting IDs, insert all | Merge two independent libraries |

#### Conflict Resolution (MERGE mode)
- **ID match:** Update existing mineral with imported data
- **No ID match:** Insert as new mineral
- **Name conflict:** Allowed (different minerals can have same name)

#### Error Handling
- **Parse errors:** Logged, row skipped, import continues
- **Validation errors:** Logged, row skipped, import continues
- **Transaction failures:** Entire import rolled back

---

## Validation Rules

### Required Fields
- `id`: Must be valid UUID
- `nameFr`: Must not be blank
- `nameEn`: Must not be blank
- `isUserDefined`: Must be `true` or `false`
- `createdAt`: Must be valid ISO 8601 timestamp
- `updatedAt`: Must be valid ISO 8601 timestamp

### Optional Fields
- Empty strings (`""`) are converted to `null`
- Invalid numeric values default to `null`
- Invalid dates default to current timestamp

### Business Rules
- `mohsMin` should be ≤ `mohsMax` (not enforced, warning logged)
- `density` should be > 0 (not enforced)

---

## Security Considerations

### Input Sanitization
- CSV injection protection: Leading `=`, `+`, `-`, `@` characters in cells are escaped
- Path traversal prevention: File names are sanitized
- Size limits: CSV files > 50 MB are rejected

### Data Privacy
- User-defined minerals (`isUserDefined=true`) may contain personal notes
- Consider encryption when sharing backups

---

## Compatibility

### Version Support
- **Current version:** 1.0
- **Backward compatibility:** CSV format is stable and backward-compatible
- **Forward compatibility:** Unknown columns are ignored during import

### Cross-Platform
- Format is platform-independent
- Can be edited in Excel, Google Sheets, LibreOffice Calc, or any text editor

---

## Editing CSV Manually

### Recommended Tools
- **Text editors:** VS Code, Sublime Text, Notepad++
- **Spreadsheet software:** LibreOffice Calc, Google Sheets (NOT Excel - encoding issues)
- **CSV editors:** CSVed, Ron's CSV Editor

### Best Practices
1. **Always keep a backup** before editing
2. **Use UTF-8 encoding** to preserve special characters (²₃, é, ñ, etc.)
3. **Quote fields with commas** to avoid column misalignment
4. **Generate UUIDs properly** for new entries (use online UUID generator)
5. **Update `updatedAt`** timestamp when modifying rows
6. **Set `isUserDefined=true`** for manually added minerals

### Common Mistakes
❌ Using Excel (corrupts UTF-8 encoding)
❌ Forgetting to quote fields with commas
❌ Mixing line endings (CRLF vs LF)
❌ Using semicolon (`;`) instead of comma (`,`)
❌ Invalid UUID format

---

## Example Workflow

### Export Reference Library Only
1. Open MineraLog Settings
2. Go to "Backup & Restore"
3. Tap "Export Library (CSV only)"
4. Select destination folder
5. Share `reference_minerals.csv` with colleagues

### Import Shared Library
1. Receive `reference_minerals.csv` file
2. Create a ZIP containing: `reference_minerals.csv`
3. Open MineraLog Settings
4. Go to "Backup & Restore"
5. Tap "Import Backup"
6. Select ZIP file
7. Choose import mode (recommended: **MERGE**)
8. Review import report

---

## Troubleshooting

### "Failed to parse CSV row"
- **Cause:** Malformed CSV (missing quotes, wrong column count)
- **Fix:** Validate CSV structure, check for unquoted commas

### "Duplicate ID detected"
- **Cause:** Same UUID exists in library
- **Fix:** Use **MAP_IDS** import mode to auto-remap IDs

### "Invalid timestamp format"
- **Cause:** Timestamp not in ISO 8601 format
- **Fix:** Use format `YYYY-MM-DDTHH:MM:SSZ`

### "Special characters corrupted (é → Ã©)"
- **Cause:** File not saved as UTF-8
- **Fix:** Re-save file with UTF-8 encoding (no BOM)

---

## Future Enhancements

Planned features (not yet implemented):
- ✨ Standalone CSV export (without full backup)
- ✨ CSV import with preview and column mapping UI
- ✨ Fuzzy name matching for duplicate detection
- ✨ Batch edit support (multiple minerals at once)

---

## Related Documentation

- [Backup System Overview](./BACKUP_SYSTEM.md) _(if exists)_
- [Database Schema v6](./DATABASE_SCHEMA_V6.md) _(if exists)_
- [Reference Library Roadmap](./ROADMAP_REFERENCE_LIBRARY.md)
- [Implementation Status](./IMPLEMENTATION_STATUS.yaml)

---

**Last updated:** 2025-01-16
**Maintained by:** MineraLog Development Team
