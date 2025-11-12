# MineraLog Roadmap

## Current Version: v1.2.0 (2025-11-12)

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

## 🔜 v1.2.1 - Filter UI & Export Enhancements (Next Patch)

**Priority:** High
**Effort:** Small (1 week)

### Features
- **Statistics Dashboard**
  - Total minerals, total value, average value
  - Distribution charts: by group (pie), by country (bar), by hardness (bar)
  - Most common group, most valuable specimen
  - Completeness metrics: % fully documented
  - Time-based stats: added this month/year

- **Advanced Filtering**
  - Multi-criteria filters (group + country + hardness range)
  - Saved filter presets
  - Filter export/sharing

- **Export Enhancements**
  - Statistics export to PDF
  - Custom CSV column selection
  - Batch photo export (ZIP of just photos)

### Technical
- Compose Canvas charts (no heavy dependencies)
- Lazy computation with ViewModel caching
- Indexed queries for performance (<300ms)

---

## 🎯 v1.3.0 - Comparator & Bulk Operations (Q2 2026)

**Priority:** High
**Effort:** Medium (3-4 weeks)

### Features
- **Mineral Comparator**
  - Select 2-3 minerals for side-by-side comparison
  - Diff highlighting for different values
  - Sticky headers for scrolling long comparisons
  - Export comparison as PDF

- **Bulk Editor**
  - Multi-select mode in home screen
  - Bulk actions: move to storage, add tags, delete, export
  - Confirmation dialogs with counts
  - Progress indicators for long operations
  - Undo support (where applicable)

- **Batch Import Improvements**
  - CSV import with column mapping UI
  - Preview before import (first 10 rows)
  - Conflict resolution options (skip, overwrite, create new)

### Technical
- Selection state management in ViewModel
- Optimistic UI updates with rollback
- Background WorkManager for bulk operations

---

## 📷 v1.4.0 - Photo Gallery & Camera (Q3 2026)

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

## 🗺️ v1.5.0 - Map View & Geolocation (Q4 2026)

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

## 📋 v1.6.0 - QR Labels & Barcode Scanning (Q1 2027)

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

## 🔐 v1.7.0 - Enhanced Security & Backup (Q2 2027)

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

## 🏥 v1.8.0 - Diagnostics & Health Panel (Q3 2027)

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
| Statistics Dashboard | High | Medium | High | v1.2 |
| Comparator & Bulk Ops | High | Medium | High | v1.3 |
| Photo Gallery | High | Large | High | v1.4 |
| Map View | Medium | Medium | Medium | v1.5 |
| QR Labels | Medium | Medium | High | v1.6 |
| Enhanced Security | Medium | Medium | Medium | v1.7 |
| Diagnostics | Low | Small | Low | v1.8 |
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

*Last Updated: 2025-11-12*
