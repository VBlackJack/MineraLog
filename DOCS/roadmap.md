# MineraLog Roadmap

## Current Version: v1.3.1 (2025-11-13)

---

## ✅ Completed in v1.1.0

### Core Improvements
- ✅ Room database migration v1→v2 with data preservation
- ✅ Comprehensive string resources (English + French, 330+ strings each)
- ✅ MineralStatus lifecycle tracking (5 states: Collection, Display, Loaned, Restoration, For Sale)
- ✅ Quality rating system (1-5 scale)
- ✅ Data completeness tracking (0-100%)
- ✅ Proper indexing on new database fields

### Security & Encryption
- ✅ Argon2id key derivation (128MB memory, 4 iterations, parallelism=2)
- ✅ AES-256-GCM authenticated encryption
- ✅ Password strength assessment utility
- ✅ Secure key management (never stores passwords)
- ✅ PasswordBasedCrypto helper for easy encryption/decryption

### Testing & Quality
- ✅ Comprehensive MineralDao tests (25+ test cases)
- ✅ CRUD, search, filter, and edge case coverage
- ✅ In-memory database testing setup

### Documentation
- ✅ JSON Schema v1.1.0 with backward compatibility
- ✅ Comprehensive assumptions document
- ✅ Assessment of codebase state
- ✅ Migration strategy documented

---

---

## ✅ Completed in v1.2.0 (2025-11-12)

### Statistics Dashboard ⭐
- ✅ Comprehensive CollectionStatistics data model
- ✅ Statistics aggregation queries in MineralDao (13 new queries)
- ✅ StatisticsRepository with caching support
- ✅ Statistics screen with multiple sections:
  - Overview metrics (total, value, completeness)
  - Recent activity (added this month/year)
  - Highlights (most common, most valuable)
  - Distribution charts (group, country, hardness, status)
- ✅ Compose Canvas chart components (PieChart, BarChart)
- ✅ Empty state handling
- ✅ Dark/light theme support
- ✅ i18n strings (English + French)

### Advanced Filtering Infrastructure
- ✅ FilterCriteria data model with 9 filter types
- ✅ FilterPreset entity + DAO for saved presets
- ✅ Database migration v2→v3 (filter_presets table)
- ✅ FilterPresetRepository with JSON serialization
- ✅ Advanced MineralDao.filterAdvanced() query
- 🔄 UI implementation deferred to v1.2.1 (backend ready)

### Technical Improvements
- ✅ Database version upgraded to v3
- ✅ Room migration 2→3 tested and documented
- ✅ Statistics computation performance optimized
- ✅ Comprehensive unit tests (StatisticsRepositoryTest)
- ✅ Version updated to 1.2.0 (versionCode 2)

### Documentation
- ✅ Sprint plan created (DOCS/sprint_plan.md)
- ✅ Roadmap updated
- ✅ i18n strings added
- ✅ Test coverage increased

### Known Limitations (v1.2.0)
- ⚠️ Filter preset UI not yet implemented (deferred to v1.2.1)
- ⚠️ CSV column selection UI not implemented (deferred to v1.2.1)
- ⚠️ Statistics export to PDF not implemented (future)
- ⚠️ Dependency injection TODO placeholder in navigation (functional but needs cleanup)

---

## ✅ Completed in v1.2.1 (2025-11-12)

### Critical Fixes ⚡
- ✅ **DI Container Implementation**
  - Application-scoped repositories (statisticsRepository, filterPresetRepository)
  - Manual DI pattern in MineraLogApplication
  - Removed TODO placeholder in NavHost
  - Statistics feature now fully functional

- ✅ **HomeScreen Statistics Integration**
  - Added BarChart icon button in TopAppBar
  - onStatisticsClick parameter in HomeScreen signature
  - Direct navigation to Statistics screen
  - Material 3 compliant UI/UX

- ✅ **French i18n Completion**
  - 10 new statistics strings translated (FR)
  - statistics_title, statistics_overview, statistics_time_based, etc.
  - Complete bilingual support (EN/FR)

### Technical Improvements
- ✅ Version bumped to 1.2.1 (versionCode 3)
- ✅ No breaking changes
- ✅ Full backward compatibility

### Known Limitations (Deferred to v1.3.0)
- ⏸️ Filter preset UI not implemented (backend ready)
- ⏸️ CSV column selection UI not implemented (backend ready)
- ⏸️ Statistics export to PDF (future enhancement)

---

## ✅ Completed in v1.3.0 (2025-11-13)

### Advanced Filtering UI ⭐
- ✅ **FilterBottomSheet Component**
  - Multi-criteria filter UI (groups, countries, hardness, status, quality, photos, fluorescence)
  - Collapsible sections for better UX
  - Range sliders for Mohs hardness and quality ratings
  - Multi-select chips for groups, countries, and status types
  - Boolean toggles for photos and fluorescence
  - Real-time filter preview with active count badge

- ✅ **Filter Preset Management**
  - Save custom filter combinations as presets
  - Load saved presets with one tap
  - Delete unwanted presets
  - Preset name customization
  - Persistent storage in Room database

- ✅ **HomeScreen Filter Integration**
  - Filter icon with badge showing active filter count
  - Filter summary chip when filters are active
  - Quick clear filter button
  - Seamless integration with search functionality
  - Filter takes precedence over default "show all" mode

### Bulk Operations ⭐
- ✅ **Multi-Select Mode**
  - Enter selection mode from HomeScreen TopAppBar
  - Visual selection with checkboxes and highlighted cards
  - Selection counter in TopAppBar
  - Select All / Deselect All actions
  - Exit selection mode with close button

- ✅ **Bulk Actions Bottom Sheet**
  - Delete selected minerals (with confirmation dialog)
  - Export to CSV (backend ready, full UI integration in v1.3.1)
  - Mineral count display
  - Warning indicators for destructive actions

- ✅ **Selection State Management**
  - ViewModel-based selection tracking
  - Persistent selection across configuration changes
  - Safe deletion with confirmation
  - Automatic exit after bulk operations

### Export Infrastructure
- ✅ **CSV Export Backend**
  - BackupRepository.exportCsv() method
  - Comprehensive 35-column CSV format
  - Proper CSV escaping for special characters
  - Includes all mineral properties (basic, physical, provenance, storage)
  - Ready for UI integration in future releases

### Technical Improvements
- ✅ MineralRepository.filterAdvancedFlow() implementation
- ✅ HomeViewModel extended with filter and selection state
- ✅ Compose BadgedBox for filter count indicators
- ✅ Material 3 selection styling (primaryContainer background)
- ✅ Version updated to 1.3.0 (versionCode 4)

### Internationalization
- ✅ 22 new English strings for filters
- ✅ 22 new French strings for filters
- ✅ 8 new English strings for bulk operations
- ✅ 8 new French strings for bulk operations
- ✅ Complete bilingual support maintained

### Known Limitations (Deferred to v1.3.1)
- ⏸️ CSV export UI (file picker integration) - backend ready
- ⏸️ CSV column selection dialog - backend supports all columns
- ⏸️ Batch CSV import with column mapping - future enhancement
- ⏸️ Bulk tag addition/removal - future enhancement
- ⏸️ Bulk storage location assignment - future enhancement

---

## ✅ Completed in v1.3.1 (2025-11-13)

### Mineral Comparator ⭐
- ✅ **ComparatorScreen Component**
  - Side-by-side comparison UI for 2-3 minerals
  - Scrollable sections (Basic, Physical, Special Properties, Status, Provenance)
  - Diff highlighting with secondaryContainer background (30% alpha)
  - Sticky header row with mineral names
  - ComparisonRow component with bold text for differences

- ✅ **ComparatorViewModel**
  - Loads 2-3 minerals by ID from MineralRepository
  - Input validation (min 2, max 3 minerals)
  - Sealed class UI states (Loading/Success/Error)
  - Factory pattern for mineralIds parameter injection

- ✅ **Navigation Integration**
  - Screen.Compare route with comma-separated mineralIds
  - Navigation from BulkActionsBottomSheet
  - HomeScreen integration with onCompareClick callback
  - MineraLogNavHost composable route with argument parsing

- ✅ **BulkActions Integration**
  - Compare action in BulkActionsBottomSheet (conditional: 2-3 minerals)
  - Icons.Default.Compare icon
  - Description text for user guidance
  - Automatic sheet dismiss after action

### Internationalization
- ✅ 8 new English strings for Comparator
- ✅ 8 new French strings for Comparator
- ✅ Complete bilingual support maintained (EN/FR)

### Technical Improvements
- ✅ Manual DI via Application-scoped repositories
- ✅ Material 3 compliant comparison UI
- ✅ Comma-separated ID routing for multi-parameter navigation
- ✅ Conditional UI rendering based on selection count
- ✅ Version updated to 1.3.1 (versionCode 5)

### Documentation
- ✅ Roadmap updated with v1.3.1 completion details
- ✅ i18n strings added for all Comparator features

### Known Limitations (Deferred to Future Releases)
- ⏸️ CSV export UI (file picker integration) - backend ready
- ⏸️ CSV column selection dialog - backend supports all columns
- ⏸️ Export comparison as PDF - future enhancement
- ⏸️ Batch CSV import with column mapping - future enhancement
- ⏸️ Bulk tag addition/removal - future enhancement
- ⏸️ Bulk storage location assignment - future enhancement

---

## 🎯 v1.4.0 - Export UI & Photo Gallery (Q1 2026)

**Priority:** High
**Effort:** Small (1-2 weeks)

### Features
- **CSV Export UI**
  - File picker integration for export destination
  - Column selection dialog with checkboxes
  - Preview CSV header before export
  - Save preferences for future exports

- **Batch Import Improvements**
  - CSV import with column mapping UI
  - Preview before import (first 10 rows)
  - Conflict resolution options (skip, overwrite, create new)

- **Bulk Operations Enhancements**
  - Bulk tag addition/removal
  - Bulk storage location assignment
  - Progress indicators for long operations

### Technical
- File picker integration (SAF - Storage Access Framework)
- Background WorkManager for bulk operations
- Optimistic UI updates with rollback

---

## 📷 v1.5.0 - Photo Gallery & Camera (Q2 2026)

**Priority:** High
**Effort:** Large (5-6 weeks)

### Features
- **Photo Gallery**
  - Grid view with photo types (Normal, UV, Macro, Context)
  - Swipe-to-view fullscreen gallery
  - UV photo highlighting (visual indicator)
  - Photo editing: rotate, crop, brightness/contrast
  - Batch photo operations (delete, export)

- **Camera Integration**
  - CameraX-based capture
  - UV filter mode (if device supports)
  - Macro mode with focus assist
  - Auto-save to mineral record
  - Geo-tagging option

- **Image Optimization**
  - Thumbnail generation (256x256, 512x512)
  - JPEG compression levels (quality vs size trade-off)
  - EXIF data extraction and display
  - Photo limit: 100 per mineral (performance)

### Technical
- Coil image loading with memory caching
- Paging for photo lists (50 at a time)
- Background thumbnail generation (WorkManager)
- Storage usage tracking

---

## 🗺️ v1.6.0 - Map View & Geolocation (Q3 2026)

**Priority:** Medium
**Effort:** Medium (3-4 weeks)

### Features
- **Provenance Map**
  - Google Maps integration with clustering
  - Marker icons by mineral group
  - Info windows with mineral preview
  - Heatmap mode (density visualization)
  - Filter sync with home screen

- **Geolocation Features**
  - GPS coordinates from device location
  - Reverse geocoding (coordinates → address)
  - Distance calculations
  - "Minerals nearby" feature

### Technical
- Google Maps Compose library
- Clustering for >10 markers
- Offline tile caching (if Maps SDK supports)
- Graceful degradation without internet

---

## 📋 v1.7.0 - QR Labels & Barcode Scanning (Q4 2026)

**Priority:** Medium
**Effort:** Medium (3-4 weeks)

### Features
- **QR Label Generator**
  - PDF export with multiple templates:
    - Basic: Name + QR (50×30mm, 36/sheet)
    - Detailed: Name + Group + Formula + QR (70×35mm, 24/sheet)
    - Compact: Small labels (30×20mm, 80/sheet)
  - Variable fields: {name}, {group}, {formula}, {uuid}
  - Batch generation for selected minerals
  - Print-ready A4 PDF

- **Barcode Scanner**
  - ML Kit barcode detection
  - Scan QR to open mineral detail
  - Scan storage location QR
  - History of scanned items

### Technical
- ZXing Core for QR generation
- PDF library (iText or PDFBox)
- Deep link handling (`mineralapp://mineral/{uuid}`)
- ML Kit Barcode Scanner

---

## 🔐 v1.8.0 - Enhanced Security & Backup (Q1 2027)

**Priority:** Medium
**Effort:** Medium (3-4 weeks)

### Features
- **Enhanced Encryption**
  - Option to encrypt media files (in addition to database)
  - Configurable KDF parameters (for power users)
  - Encryption benchmark (shows derivation time)
  - Password hints (stored unencrypted)

- **Automatic Backups**
  - Scheduled backups (daily, weekly, monthly)
  - Backup to local storage or cloud (Google Drive, Dropbox)
  - Backup rotation (keep last N backups)
  - Restore wizard with preview

- **Import/Export UI**
  - File picker integration
  - Format selection (ZIP/CSV/JSON)
  - Progress bar with cancel support
  - Detailed error messages with retry
  - Success summary with counts

### Technical
- WorkManager periodic backup jobs
- Cloud storage SDK integration
- Incremental backups (delta exports)

---

## 🏥 v1.9.0 - Diagnostics & Health Panel (Q2 2027)

**Priority:** Low
**Effort:** Small (1-2 weeks)

### Features
- **Diagnostics Screen**
  - App version, database version
  - Storage used/available
  - Mineral count, photo count
  - Last backup timestamp
  - Encryption key status
  - API key validation (Maps, ML Kit)
  - Health status: OK / Warning / Error

- **Database Maintenance**
  - Vacuum/optimize database
  - Rebuild indices
  - Clear cache
  - Export logs for debugging

### Technical
- Android StatFs for storage info
- Room database size queries
- API key validation pings

---

## 🌐 v2.0.0 - Cloud Sync & Multi-User (2027)

**Priority:** Low
**Effort:** Very Large (12+ weeks)

### Features
- **Cloud Sync**
  - End-to-end encryption (E2EE)
  - OAuth2/PKCE authentication
  - Conflict resolution (last-write-wins or manual)
  - Selective sync (choose what to sync)
  - Offline-first with sync queue

- **Multi-User Support**
  - User accounts with roles (Owner, Editor, Viewer)
  - Shared collections
  - Activity log (who changed what when)
  - Permissions management

- **Web Companion**
  - View-only web interface
  - Export/import via web
  - Statistics dashboard

### Technical
- Backend API (Firebase, AWS Amplify, or custom)
- Sync engine with conflict resolution
- PKCE flow for OAuth2
- WebSocket for real-time updates

---

## 🔮 Future Considerations (No ETA)

### Advanced Features
- **NFC Integration**
  - NFC tag reading/writing for storage locations
  - Tap phone to storage box to see contents
  - NFC-enabled labels

- **AI/ML Features**
  - Mineral identification from photos (TensorFlow Lite)
  - Auto-fill properties based on name
  - Duplicate detection
  - Photo quality assessment

- **Social Features**
  - Share collection publicly (opt-in)
  - Mineral trading marketplace
  - Connect with other collectors
  - Collection showcases

- **Advanced Analytics**
  - Value trends over time
  - Collection growth charts
  - Acquisition patterns
  - Spending analytics

- **Integration with Online Databases**
  - Mindat.org API integration
  - Auto-import mineral properties
  - Cross-reference with known localities
  - Link to mineral photos database

### Platform Expansion
- **iOS App** (Swift/SwiftUI)
- **Desktop App** (Kotlin Multiplatform or Electron)
- **Wear OS** (quick specimen lookup)

---

## Priority Matrix

| Feature | Priority | Effort | Impact | Target |
|---------|----------|--------|--------|--------|
| Statistics Dashboard | High | Medium | High | ✅ v1.2 |
| Advanced Filtering & Bulk Ops | High | Medium | High | ✅ v1.3 |
| Mineral Comparator | High | Small | High | ✅ v1.3.1 |
| Export UI & Bulk Enhancements | High | Small | Medium | v1.4 |
| Photo Gallery | High | Large | High | v1.5 |
| Map View | Medium | Medium | Medium | v1.6 |
| QR Labels | Medium | Medium | High | v1.7 |
| Enhanced Security | Medium | Medium | Medium | v1.8 |
| Diagnostics | Low | Small | Low | v1.9 |
| Cloud Sync | Low | Very Large | High | v2.0 |

---

## Release Cadence

- **v1.x releases:** Quarterly (every 3 months)
- **Patch releases:** As needed for critical bugs
- **Beta channel:** 2 weeks before stable release

---

## Feedback & Contributions

Users can request features or report bugs at:
- GitHub Issues: https://github.com/mineralog/mineralog/issues
- Email: feedback@mineralog.net

---

*Last Updated: 2025-11-13*
