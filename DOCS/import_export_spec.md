# MineraLog Import/Export Specification v1.0

## Overview

MineraLog supports two export/import formats:
1. **ZIP Archive** - Complete backup with media files
2. **CSV** - Spreadsheet-compatible, metadata only

## ZIP Export Format

### Archive Structure

```
export_2025-11-12T143022Z.zip
├── manifest.json
├── minerals.json
├── checksums.sha256
└── media/
    ├── {mineral-id-1}/
    │   ├── photo1.jpg
    │   └── photo2.jpg
    └── {mineral-id-2}/
        └── photo1.jpg
```

### manifest.json

```json
{
  "app": "MineraLog",
  "schemaVersion": "1.0.0",
  "exportedAt": "2025-11-12T14:30:22.123Z",
  "counts": {
    "minerals": 150,
    "photos": 450
  },
  "encrypted": false,
  "kdf": null,
  "cipher": null
}
```

### minerals.json

Array of mineral objects with nested provenance, storage, and photos:

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Quartz",
    "group": "Silicates",
    "formula": "SiO₂",
    "crystalSystem": "Hexagonal",
    "mohsMin": 7.0,
    "mohsMax": 7.0,
    "cleavage": "None",
    "fracture": "Conchoidal",
    "luster": "Vitreous",
    "streak": "White",
    "diaphaneity": "Transparent",
    "habit": "Prismatic",
    "specificGravity": 2.65,
    "fluorescence": "none",
    "magnetic": false,
    "radioactive": false,
    "dimensionsMm": "45 x 30 x 25",
    "weightGr": 85.5,
    "notes": "Clear crystal with minor inclusions",
    "tags": ["display", "favorites", "brazil"],
    "status": "complete",
    "createdAt": "2025-01-15T10:30:00.000Z",
    "updatedAt": "2025-11-12T14:25:00.000Z",
    "provenance": {
      "id": "650e8400-e29b-41d4-a716-446655440001",
      "mineralId": "550e8400-e29b-41d4-a716-446655440000",
      "site": "Minas Gerais Mine",
      "locality": "Minas Gerais",
      "country": "Brazil",
      "latitude": -19.9227,
      "longitude": -43.9450,
      "acquiredAt": "2025-01-15T00:00:00.000Z",
      "source": "purchase",
      "price": 45.50,
      "estimatedValue": 75.00
    },
    "storage": {
      "id": "750e8400-e29b-41d4-a716-446655440002",
      "mineralId": "550e8400-e29b-41d4-a716-446655440000",
      "place": "Living Room",
      "container": "Display Cabinet A",
      "box": "Box 1",
      "slot": "A3",
      "nfcTagId": null,
      "qrContent": "mineralapp://mineral/550e8400-e29b-41d4-a716-446655440000"
    },
    "photos": [
      {
        "id": "850e8400-e29b-41d4-a716-446655440003",
        "mineralId": "550e8400-e29b-41d4-a716-446655440000",
        "type": "NORMAL",
        "caption": "Main view",
        "takenAt": "2025-01-15T10:35:00.000Z",
        "fileName": "media/550e8400-e29b-41d4-a716-446655440000/photo1.jpg"
      },
      {
        "id": "850e8400-e29b-41d4-a716-446655440004",
        "mineralId": "550e8400-e29b-41d4-a716-446655440000",
        "type": "UV_LW",
        "caption": "Under longwave UV",
        "takenAt": "2025-01-15T10:40:00.000Z",
        "fileName": "media/550e8400-e29b-41d4-a716-446655440000/photo2.jpg"
      }
    ]
  }
]
```

### checksums.sha256

Format: `relative_path;sha256_hex`

```
minerals.json;a1b2c3d4e5f6...
media/550e8400-e29b-41d4-a716-446655440000/photo1.jpg;1234567890ab...
media/550e8400-e29b-41d4-a716-446655440000/photo2.jpg;fedcba098765...
```

### Encrypted ZIP

When encrypted with password:

```json
{
  "app": "MineraLog",
  "schemaVersion": "1.0.0",
  "exportedAt": "2025-11-12T14:30:22.123Z",
  "counts": {
    "minerals": 150,
    "photos": 450
  },
  "encrypted": true,
  "kdf": "argon2id",
  "kdfParams": {
    "iterations": 3,
    "memoryCost": 65536,
    "parallelism": 2,
    "saltHex": "hex-encoded-salt"
  },
  "cipher": "AES-256-GCM",
  "ivHex": "hex-encoded-iv"
}
```

Files `minerals.json` and media files are encrypted with AES-256-GCM.

## CSV Export Format

### minerals.csv

UTF-8 encoding, comma-separated, header row required.

**Columns:**
```
id,name,group,formula,crystalSystem,mohsMin,mohsMax,cleavage,fracture,luster,streak,diaphaneity,habit,specificGravity,fluorescence,magnetic,radioactive,dimensionsMm,weightGr,notes,status,tags,site,locality,country,lat,lon,acquiredAt,source,price,estimatedValue,place,container,box,slot
```

**Example:**
```csv
id,name,group,formula,crystalSystem,mohsMin,mohsMax,cleavage,luster,status
550e8400-e29b-41d4-a716-446655440000,Quartz,Silicates,SiO₂,Hexagonal,7.0,7.0,None,Vitreous,complete
```

**Notes:**
- Empty fields: Leave blank (consecutive commas)
- Quotes: Use double-quotes for fields containing commas or newlines
- Booleans: `true`/`false` or `1`/`0`
- Dates: ISO-8601 format (`YYYY-MM-DDTHH:MM:SSZ`)
- Tags: Comma-separated within quotes (`"tag1,tag2,tag3"`)

### photos.csv (Optional)

```csv
mineralId,type,caption,takenAt,fileName
550e8400-e29b-41d4-a716-446655440000,NORMAL,Main view,2025-01-15T10:35:00Z,photo1.jpg
550e8400-e29b-41d4-a716-446655440000,UV_LW,Under UV,2025-01-15T10:40:00Z,photo2.jpg
```

**Note:** Photo files must be provided separately if importing from CSV.

## Import Modes

### 1. MERGE (Default)
- Upsert by ID: existing IDs are updated, new IDs are inserted
- Preserves data not in import file
- Safe for incremental updates

### 2. REPLACE
- Deletes ALL existing data
- Imports everything from file
- Use for complete restore

### 3. MAP_IDS
- Regenerates conflicting UUIDs
- Maintains internal UUID remapping table
- Use when merging collections from different sources

## Validation Rules

### Import Validation

- **Required fields:** `id`, `name`
- **Mohs hardness:** 1.0 - 10.0, mohsMax >= mohsMin
- **Coordinates:** lat ∈ [-90, 90], lon ∈ [-180, 180]
- **Positive values:** specificGravity, weightGr, price, estimatedValue
- **Enums:** Validate against known values (crystal systems, photo types, sources)
- **UUIDs:** Valid UUID v4 format

### Error Handling

Import continues on errors, collecting all issues:

```json
{
  "imported": 145,
  "skipped": 5,
  "errors": [
    "Row 12: Invalid Mohs hardness 15.0 (must be 1-10)",
    "Row 34: Missing required field 'name'",
    "Row 56: Invalid UUID format",
    "Row 78: Invalid date format in acquiredAt",
    "Row 90: Invalid latitude 95.0 (must be -90 to 90)"
  ]
}
```

## PC Tools

### csv_to_zip.py

Converts CSV to ZIP format:

```bash
python tools/csv_to_zip.py \
  --input minerals.csv \
  --output export.zip \
  --photos photos.csv \
  --media-dir ./media \
  --encrypt \
  --password "your-secure-password"
```

### validate_export.py

Validates export ZIP against schema:

```bash
python tools/validate_export.py export.zip
```

## JSON Schema

See `/docs/json_schema/mineral_export_v1.json` for complete JSON Schema definition.

## Changelog

### v1.0.0 (2025-11-12)
- Initial schema version
- Support for minerals, provenance, storage, photos
- Encryption with Argon2id + AES-256-GCM
- CSV import/export
