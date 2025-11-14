# MineraLog

> A comprehensive Android application for cataloging and managing mineral collections.

[![Android CI](https://github.com/VBlackJack/MineraLog/workflows/Android%20CI/badge.svg)](https://github.com/VBlackJack/MineraLog/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Version-1.5.0-brightgreen.svg)](CHANGELOG.md)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-27-green.svg)](https://developer.android.com/about/versions/oreo)
[![WCAG 2.1 AA](https://img.shields.io/badge/WCAG%202.1-AA%20Compliant-brightgreen.svg)](ACCESSIBILITY.md)

## Features

### 🔬 Comprehensive Cataloging
- **Mineralogical Properties**: Crystal system, Mohs hardness, cleavage, fracture, luster, streak, diaphaneity, habit, specific gravity
- **Special Properties**: Fluorescence (LW/SW), magnetic, radioactive
- **Physical Measurements**: Dimensions (mm), weight (g)
- **Metadata**: Notes, tags, status tracking (complete/incomplete)

### 📸 Photo Management
- **4 Photo Types**: Normal, UV Shortwave, UV Longwave, Macro
- **CameraX Integration**: High-quality in-app camera with live preview
- **Photo Gallery**: Grid view with fullscreen swipe viewer
- **Pinch-to-Zoom**: 1x-5x zoom in fullscreen mode
- **Photo Type Badges**: Color-coded type indicators
- **Smart Storage**: App-specific directory (no permissions needed on API 29+)

### 🗺️ Provenance Tracking
- **Geographic Origin**: Site, locality, country with coordinates
- **Acquisition Details**: Date, source (purchase/exchange/collected/gift/inheritance), price, estimated value
- **Multi-Currency Support**: Track prices in different currencies
- **Map View**: Planned for v1.6 (Google Maps integration)

### 📦 Storage Organization
- **Hierarchical System**: Place → Container → Box → Slot
- **Reverse Search**: "Where is this mineral?" queries
- **QR Codes**: Generate and scan labels for physical organization

### 🏷️ QR Scanning & Labels
- **QR Code Scanner**: ML Kit barcode scanning with torch support
- **Deep Links**: `mineralapp://mineral/{uuid}` opens mineral detail instantly
- **Direct UUID Support**: Also recognizes raw UUIDs
- **QR Label Generation**: Planned for v1.6
  - PDF templates (50×30mm, 70×35mm)
  - Customizable fields

### 🔍 Search & Filtering
- **Full-Text Search**: Name, group, formula, notes, tags
- **Advanced Filters**: Mohs range, crystal system, country, fluorescence, status, tags
- **Real-Time**: Debounced search for smooth UX (<300ms latency)

### 💾 Import/Export
- **ZIP Format**: Complete backup with photos
  - **3 Import Modes**: Merge (upsert), Replace (fresh start), Map IDs (conflict resolution)
  - **Encryption Backend**: Ready (Argon2id + AES-256-GCM), UI planned for v1.6
- **CSV Format**: Export with selective column selection
  - CSV import: Planned for v1.6
  - Spreadsheet-compatible bulk editing workflow
- **Validation & Error Reporting**: Detailed logs of import issues (ZIP only)

### 🔒 Security & Privacy
- **Offline-First**: All data stored locally, no cloud dependency
- **Encrypted Backups**: Crypto backend ready (Argon2id KDF + AES-256-GCM cipher), UI planned for v1.6
- **No Telemetry**: Zero analytics without explicit consent
- **Storage Access Framework**: Secure file access via Android SAF

### 🌍 Internationalization
- **Bilingual**: English, Français
- **Material 3 Theming**: Light/Dark modes

### ♿ Accessibility (WCAG 2.1 AA Certified)
- **TalkBack Support**: Complete screen reader compatibility with live regions
- **Focus Management**: Auto-focus in dialogs, keyboard navigation (Tab/Enter/Escape)
- **Technical Field Tooltips**: Inline help for complex mineral properties
- **Auto-Save Drafts**: Never lose work with 500ms debounced auto-save
- **Reduced Motion**: Respects prefers-reduced-motion for animations
- **Haptic Feedback**: Tactile confirmation for critical actions
- **Color Contrast**: Verified 4.5:1 minimum ratios throughout
- **Dynamic Type**: Supports up to 200% font scaling without truncation
- **Touch Targets**: Minimum 48×48dp on all interactive elements
- **Automated Testing**: CI-integrated accessibility test suite

See [ACCESSIBILITY.md](ACCESSIBILITY.md) for implementation guide.

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
- **[ACCESSIBILITY](ACCESSIBILITY.md)**: WCAG 2.1 AA compliance guide for contributors
- **[User Guide](DOCS/user_guide.md)**: Complete app usage documentation
- **[Import/Export Spec](DOCS/import_export_spec.md)**: File format specifications
- **[Assumptions Log](DOCS/assumptions.md)**: Implementation decisions and rationale

## What's New in 1.5.0

🎉 **Major feature release - Photo Workflows & QR Scanning!**

### New Features
- **📸 Photo Capture**: CameraX integration with 4 photo types (Normal, UV-SW, UV-LW, Macro)
- **🖼️ Photo Gallery**: Grid view + fullscreen swipe viewer with pinch-to-zoom (1x-5x)
- **📷 QR Scanner**: ML Kit barcode scanning with deep links (`mineralapp://mineral/{uuid}`)
- **🧪 Test Coverage**: Expanded to ~35-40% with comprehensive unit + instrumentation tests
- **♿ Accessibility**: WCAG 2.1 AA compliant (88/100 score) with full TalkBack support
- **📊 CI Health**: Comprehensive monitoring with analysis scripts and health reports

### Previous Update (1.4.1)
- **10x faster** mineral list loading (optimized batch queries)
- **70% faster** statistics screen (parallel query execution)
- **Multi-currency support** for provenance tracking
- **87 bug fixes**: 8 critical, 31 major, 48 minor improvements

See [CHANGELOG.md](CHANGELOG.md) for complete version history.

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