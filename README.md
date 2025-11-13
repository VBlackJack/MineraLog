# MineraLog

> A comprehensive Android application for cataloging and managing mineral collections.

[![Android CI](https://github.com/VBlackJack/MineraLog/workflows/Android%20CI/badge.svg)](https://github.com/VBlackJack/MineraLog/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Version-1.4.1-brightgreen.svg)](CHANGELOG.md)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-27-green.svg)](https://developer.android.com/about/versions/oreo)

## Features

### 🔬 Comprehensive Cataloging
- **Mineralogical Properties**: Crystal system, Mohs hardness, cleavage, fracture, luster, streak, diaphaneity, habit, specific gravity
- **Special Properties**: Fluorescence (LW/SW), magnetic, radioactive
- **Physical Measurements**: Dimensions (mm), weight (g)
- **Metadata**: Notes, tags, status tracking (complete/incomplete)

### 📸 Photo Management
- **4 Photo Types**: Normal, UV Shortwave, UV Longwave, Macro
- **CameraX Integration**: High-quality in-app photography
- **Gallery Import**: Select from existing photos
- **EXIF Support**: Preserves metadata
- **Smart Storage**: Copy to internal storage (configurable) for data safety

### 🗺️ Provenance Tracking
- **Geographic Origin**: Site, locality, country with coordinates
- **Acquisition Details**: Date, source (purchase/exchange/collected/gift/inheritance), price, estimated value
- **Map View**: Interactive map with clustering for specimens from same regions

### 📦 Storage Organization
- **Hierarchical System**: Place → Container → Box → Slot
- **Reverse Search**: "Where is this mineral?" queries
- **QR Codes**: Generate and scan labels for physical organization

### 🏷️ QR Label Generation
- **2 Templates**:
  - 50×30mm: 36 labels per A4 sheet (4×9 grid)
  - 70×35mm: 24 labels per A4 sheet (3×8 grid)
- **Customizable Fields**: Name, ID, group, formula, QR code
- **Deep Links**: Scan to instantly open mineral detail (`mineralapp://mineral/{uuid}`)
- **PDF Export**: Print or save for later

### 🔍 Search & Filtering
- **Full-Text Search**: Name, group, formula, notes, tags
- **Advanced Filters**: Mohs range, crystal system, country, fluorescence, status, tags
- **Real-Time**: Debounced search for smooth UX (<300ms latency)

### 💾 Import/Export
- **ZIP Format**: Complete backup with photos, encryption support ready (Argon2id + AES-256-GCM)
  - **3 Import Modes**: Merge (upsert), Replace (fresh start), Map IDs (conflict resolution)
  - **Encryption UI**: Coming in v1.5
- **CSV Format**: Export only (import coming in v1.5)
  - Spreadsheet-compatible with selective column export
  - Bulk editing friendly for reimport via ZIP workflow
- **Validation & Error Reporting**: Detailed logs of import issues (ZIP only)

### 🔒 Security & Privacy
- **Offline-First**: All data stored locally, no cloud dependency
- **Encrypted Backups**: Crypto implementation ready (Argon2id KDF + AES-256-GCM cipher, UI integration in v1.5)
- **No Telemetry**: Zero analytics without explicit consent
- **Storage Access Framework**: Secure file access via Android SAF

### 🌍 Internationalization
- **Bilingual**: English, Français
- **Material 3 Theming**: Light/Dark modes

### ♿ Accessibility
- **TalkBack Support**: Complete screen reader compatibility
- **WCAG AA Compliant**: 4.5:1 contrast ratios
- **Dynamic Type**: Supports up to 200% font scaling
- **Touch Targets**: Minimum 48×48dp

## Quick Start

1. **Install** the APK from releases
2. **Grant permissions**: Camera, Storage
3. **Add your first mineral**: Tap the + button
4. **Take a photo**: Use the camera icon in detail view
5. **Organize**: Add provenance, storage location, tags
6. **Export**: Backup your collection (Settings → Export)

## Building from Source

### Prerequisites
- Android Studio Ladybug or later
- JDK 17
- Android SDK 35
- Gradle 8.7+

### Setup

1. Clone the repository:
```bash
git clone https://github.com/VBlackJack/MineraLog.git
cd MineraLog
```

2. Create `local.properties`:
```properties
MAPS_API_KEY=your_google_maps_api_key_here
```

Get your Maps API key: [Google Cloud Console](https://console.cloud.google.com/google/maps-apis)

3. Build:
```bash
./gradlew assembleDebug
```

4. Run tests:
```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

## Architecture

- **Language**: Kotlin 2.0.0
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room 2.6.1 with optimized batch queries
- **Async**: Kotlin Coroutines + Flow with parallel execution
- **Camera**: CameraX 1.4.1
- **Image Loading**: Coil 2.7.0
- **Maps**: Google Maps SDK + Compose wrappers
- **Crypto**: Tink + Argon2kt (AES-256-GCM + Argon2id)
- **QR**: ML Kit Barcode + ZXing
- **Testing**: JUnit 5, Robolectric, Espresso
- **Security**: ProGuard/R8 obfuscation with comprehensive rules

## Documentation

- **[CHANGELOG](CHANGELOG.md)**: Version history and release notes
- **[User Guide](DOCS/user_guide.md)**: Complete app usage documentation
- **[Import/Export Spec](DOCS/import_export_spec.md)**: File format specifications
- **[Assumptions Log](DOCS/assumptions.md)**: Implementation decisions and rationale

## What's New in 1.4.1

🚀 **Major performance and quality update!**

- **10x faster** mineral list loading (optimized batch queries)
- **70% faster** statistics screen (parallel query execution)
- **Critical bug fixes**: Fixed syntax errors, N+1 query patterns, data integrity issues
- **Enhanced security**: Comprehensive ProGuard rules, protected user data
- **Multi-currency support**: Track acquisition prices in different currencies
- **87 total fixes**: 8 critical, 31 major, 48 minor improvements

See [CHANGELOG.md](CHANGELOG.md) for complete details.

## PC Tools

### CSV to ZIP Converter

```bash
cd tools/csv_to_zip
python csv_to_zip.py -i minerals.csv -o export.zip --encrypt
```

See [tools/csv_to_zip/README.md](tools/csv_to_zip/README.md) for details.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Made with ⛏️ by mineral enthusiasts, for mineral enthusiasts.**