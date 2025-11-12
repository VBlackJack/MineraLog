# MineraLog CSV to ZIP Converter

Python tool to convert CSV mineral data to MineraLog ZIP export format.

## Requirements

```bash
pip install cryptography  # Optional, for encryption support
```

## Usage

### Basic Conversion

```bash
python csv_to_zip.py -i minerals.csv -o export.zip
```

### With Encryption

```bash
python csv_to_zip.py -i minerals.csv -o export.zip --encrypt --password "your-secure-password"
```

Password will be prompted if `--encrypt` is used without `--password`.

### With Media Files

```bash
python csv_to_zip.py \
  -i minerals.csv \
  -o export.zip \
  --media-dir ./media \
  --encrypt
```

## CSV Format

Required column: `name`

Optional columns:
```
id, name, group, formula, crystalSystem, mohsMin, mohsMax, cleavage, fracture,
luster, streak, diaphaneity, habit, specificGravity, fluorescence, magnetic,
radioactive, dimensionsMm, weightGr, notes, status, tags, site, locality,
country, lat, lon, acquiredAt, source, price, estimatedValue, place,
container, box, slot
```

### Example CSV

```csv
name,group,formula,mohsMin,mohsMax,country
Quartz,Silicates,SiO₂,7.0,7.0,Brazil
Fluorite,Halides,CaF₂,4.0,4.0,China
```

## Output

Creates a ZIP file with:
- `manifest.json` - Export metadata
- `minerals.json` - Mineral data (encrypted if password provided)
- `checksums.sha256` - File integrity hashes
- `media/*` - Photo files (if --media-dir specified)

## Encryption

- Algorithm: AES-256-GCM
- KDF: PBKDF2-SHA256 (100,000 iterations)
- Salt: 16 bytes random
- IV: 12 bytes random

**Note:** Android app uses Argon2id for better security. This script uses PBKDF2 for broader compatibility.

## Error Handling

- Missing required fields: Row skipped
- Invalid data types: Parsed as NULL
- Duplicate IDs: Later entry overwrites earlier

## Validation

The script performs basic validation:
- UUIDs generated for missing IDs
- Booleans: `true/false`, `1/0`, `yes/no`
- Floats: Parsed with error handling
- Dates: Accepted as-is (should be ISO-8601)

For strict validation, use `validate_export.py` after generation.
