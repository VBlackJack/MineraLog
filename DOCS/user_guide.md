# MineraLog User Guide v1.0

## Introduction

MineraLog is a comprehensive Android application for cataloging and managing your mineral collection. Whether you're a hobbyist, collector, or researcher, MineraLog helps you track, search, and share information about your specimens.

**Key Features:**
- 📝 Detailed mineral records with scientific properties
- 📸 Multiple photos per specimen (normal, UV, macro)
- 🗺️ Map view of specimen origins with clustering
- 🔍 Powerful search and filtering
- 📦 Hierarchical storage tracking
- 🏷️ Printable QR code labels
- 💾 Import/Export (ZIP, CSV)
- 🔒 Encrypted backups
- 🌍 Bilingual (English/Français)

---

## Quick Start

### Adding Your First Mineral

1. Tap the **+ (Plus)** button on the home screen
2. Enter the mineral name (required)
3. Add optional details: group, formula, notes
4. Tap **SAVE**
5. The mineral appears in your collection

**Pro Tip:** You can add more details later by editing the record.

---

## Mineral Records

### Basic Information
- **Name:** Common or scientific name (e.g., "Quartz", "Fluorite")
- **Group:** Mineral class (e.g., "Silicates", "Halides")
- **Formula:** Chemical formula (e.g., "SiO₂", "CaF₂")
- **Crystal System:** Triclinic, Monoclinic, Orthorhombic, Tetragonal, Trigonal, Hexagonal, Cubic

### Physical Properties
- **Mohs Hardness:** Range from 1.0 to 10.0 (fractional values supported, e.g., 6.5)
- **Cleavage:** Description of cleavage planes
- **Fracture:** Type (e.g., "Conchoidal", "Uneven")
- **Luster:** Appearance (Metallic, Vitreous, Pearly, Resinous, Silky, Greasy, Dull)
- **Streak:** Powder color when scratched
- **Diaphaneity:** Transparent, Translucent, Opaque
- **Habit:** Crystal form (e.g., "Prismatic", "Tabular", "Massive")
- **Specific Gravity:** Density relative to water

### Special Properties
- **Fluorescence:** Response to UV light (LW/SW wavelengths, colors)
- **Magnetic:** Check if specimen is attracted to magnets
- **Radioactive:** Check if specimen emits radiation

### Measurements
- **Dimensions:** Length × Width × Height in millimeters
- **Weight:** Mass in grams

### Notes & Tags
- **Notes:** Free-form text for observations, history, research notes
- **Tags:** Comma-separated keywords for categorization (e.g., "favorites, display, rare")
- **Status:** "Complete" (fully documented) or "Incomplete"

---

## Provenance Tracking

Track where and when you acquired each specimen:

- **Site:** Specific mine or location name
- **Locality:** City or region
- **Country:** Country of origin
- **Coordinates:** Latitude/Longitude (tap map to set)
- **Acquired Date:** When you obtained the specimen
- **Source:** Purchase, Exchange, Collected, Gift, Inheritance
- **Price:** Purchase price in your currency
- **Estimated Value:** Current estimated worth

**Map View:** Specimens with coordinates appear on the map. Markers cluster automatically when zoomed out.

---

## Storage Location

Never lose a specimen again with hierarchical storage tracking:

- **Place:** Physical location (e.g., "Living Room", "Basement")
- **Container:** Cabinet, display case, drawer (e.g., "Cabinet A")
- **Box:** Specific box or compartment (e.g., "Box 1")
- **Slot:** Position within box (e.g., "A3", "Slot 12")

**Reverse Search:** "Where is this mineral?" shows exact location path.

---

## Photos

### Taking Photos

1. Open a mineral record
2. Tap the camera icon
3. Choose:
   - **Take Photo:** Use CameraX to capture new image
   - **From Gallery:** Select existing photo

### Photo Types

- **Normal:** Standard visible-light photography
- **UV Shortwave (UV_SW):** Under shortwave UV (~254nm)
- **UV Longwave (UV_LW):** Under longwave UV (~365nm)
- **Macro:** Close-up/magnified view

**Captions:** Add descriptive captions to each photo.

**Storage:** By default, photos are copied to app's internal storage for safety. Change this in Settings.

---

## Search & Filters

### Quick Search

Tap the search bar on the home screen and type:
- Mineral names
- Groups
- Chemical formulas
- Notes content
- Tags

Search is debounced (300ms) for smooth performance.

### Advanced Filters

Tap the filter icon to filter by:
- **Group:** Select specific mineral group
- **Crystal System:** Filter by crystal structure
- **Mohs Hardness:** Range slider (e.g., 5.0 - 7.5)
- **Country:** Filter by origin country
- **Status:** Complete vs. Incomplete records
- **Tags:** Filter by specific tags

---

## QR Code Labels

### Generating Labels

1. Go to **Menu → Labels**
2. Select specimens to print
3. Choose template:
   - **50×30mm:** 36 labels per A4 sheet (4 columns × 9 rows)
   - **70×35mm:** 24 labels per A4 sheet (3 columns × 8 rows)
4. Configure fields to show:
   - Name (always shown)
   - Group
   - Formula
   - Short ID
   - QR Code (recommended)
5. Preview layout
6. Export as PDF or print directly

### Scanning QR Codes

1. Use **Menu → Scan QR** or any QR scanner app
2. Scanning `mineralapp://mineral/{uuid}` opens the mineral detail instantly
3. Works even if MineraLog isn't running (via Android App Links)

**ECC Level:** QR codes use Error Correction Level M (15% damage tolerance).

---

## Import/Export

### ZIP Export (Recommended)

Complete backup with all data and photos:

1. **Menu → Export → ZIP**
2. Choose location (use Storage Access Framework)
3. Optional: Set encryption password
4. Tap **Export**

**Contents:**
- `manifest.json`: Metadata
- `minerals.json`: All mineral data
- `checksums.sha256`: File integrity hashes
- `media/`: All photos

**Encryption:** Uses Argon2id + AES-256-GCM. Strong password required.

### ZIP Import

1. **Menu → Import → ZIP**
2. Select ZIP file
3. Optional: Enter decryption password
4. Choose import mode:
   - **Merge:** Add new, update existing (by ID)
   - **Replace:** Delete all, then import (⚠️ DATA LOSS)
   - **Map IDs:** Remap conflicting UUIDs
5. Review import report

### CSV Export

Spreadsheet-compatible export (metadata only, no photos):

1. **Menu → Export → CSV**
2. Choose columns to include
3. Optionally export `photos.csv` separately
4. Save file

**Use Case:** Bulk editing in Excel/LibreOffice, sharing lightweight data.

### CSV Import

1. **Menu → Import → CSV**
2. Select CSV file
3. Map columns to fields (auto-detected from headers)
4. Preview first 20 rows
5. Tap **Import**
6. Review error report (if any)

**Validation:** Invalid rows are skipped, errors logged.

---

## Backup & Restore

### Manual Backup

1. **Settings → Backup → Create Backup Now**
2. Optional: Set encryption password
3. Backup saved to `Internal Storage/MineraLog/Backups/`

### Scheduled Backups

1. **Settings → Backup → Schedule Backups**
2. Choose frequency (Daily, Weekly, Monthly)
3. Set preferred time
4. Backups run automatically via WorkManager

**Battery Optimization:** Scheduled backups run when:
- Device is charging, OR
- Battery > 15%

### Restore

1. **Settings → Backup → Restore**
2. Select backup file
3. Enter password (if encrypted)
4. Confirm restoration (⚠️ replaces all current data)

---

## Settings

### Language
- English (EN)
- Français (FR)

**Note:** Changing language requires app restart.

### Theme
- Light
- Dark
- System Default (follows Android system theme)

### Photo Storage
- **Copy to Internal Storage (Recommended):** Photos copied to app's private directory
- **Reference Only:** Stores only URI, relies on original file remaining accessible

### Maps API Key
- Requires Google Maps API key
- See `local.properties.example` for setup instructions

---

## Demo Data

### Loading Demo Dataset

1. **Settings → Load Demo Data**
2. Confirm (⚠️ replaces existing collection)
3. 15 sample minerals with 2-3 photos each loaded
4. Includes variety of countries, crystal systems, and properties

**Use Case:** Explore app features, training, screenshots.

---

## Tips & Best Practices

### Data Entry
- ✅ Enter core data first (name, group, formula)
- ✅ Take photos immediately after acquisition
- ✅ Add provenance details while memory is fresh
- ✅ Use consistent tag naming (e.g., "favorites" not "fav")

### Organization
- 📦 Use hierarchical storage (Place → Container → Box → Slot)
- 🏷️ Print QR labels for physical organization
- 🗂️ Use tags for cross-cutting categories ("display", "research", "trade")

### Backups
- 💾 Export to ZIP weekly (at minimum)
- 🔒 Always encrypt backups with strong passwords
- ☁️ Store backups off-device (cloud, PC, external drive)
- 🔄 Test restore periodically

### Photos
- 📸 Capture multiple angles
- 💡 Use consistent lighting for normal photos
- 🔦 Document fluorescence with both LW and SW UV
- 📏 Include scale reference in macro shots

### Search
- 🔍 Use tags liberally for easier filtering
- 📝 Add unique keywords to notes for searchability
- ⭐ Mark important specimens with "favorites" tag

---

## Performance

### Tested Limits
- **1000+ minerals:** Search <300ms
- **3000+ photos:** Smooth scrolling with thumbnail caching
- **Export 1000 specimens:** <30s on mid-range device
- **Import 1000 specimens:** <30s

### Optimization Tips
- 🖼️ Thumbnails cached automatically (Coil)
- 💾 Database indexed on frequently-searched fields
- 🔄 Large operations show progress indicators
- ⚡ Search debounced to reduce queries

---

## Accessibility

MineraLog is designed to be accessible:

- ♿ TalkBack support with content descriptions
- 🎨 WCAG AA contrast ratios (4.5:1 minimum)
- 📱 Dynamic type scaling (supports 200% font size)
- 👆 Touch targets minimum 48×48dp
- ⌨️ Keyboard navigation supported

---

## Troubleshooting

### Photos not appearing
- Check Settings → Photo Storage mode
- Ensure camera/storage permissions granted
- Verify photos exist at stored path

### Maps not loading
- Check internet connection (maps require online access)
- Verify Google Maps API key in `local.properties`
- Ensure location permissions granted (optional)

### Import fails
- Check file format (ZIP or CSV)
- Verify encryption password (if encrypted)
- Review error log for specific validation issues

### App crashes
- Clear app cache: Settings → Apps → MineraLog → Clear Cache
- Report bug with crash log (Settings → About → Send Feedback)

---

## Privacy & Security

### Data Storage
- ✅ **All data stored locally** on your device
- ✅ **No cloud sync** (v1.0) - you control all data
- ✅ **No telemetry** without explicit consent

### Encryption
- 🔒 Backups: Argon2id + AES-256-GCM
- 🔑 Passwords never stored, always user-provided
- 🛡️ Encrypted backups resistant to brute-force attacks

### Permissions
- **Camera:** Take photos of specimens
- **Storage:** Import/export files, save photos
- **Location:** Optional, for provenance coordinates only

---

## Support & Feedback

### Documentation
- User Guide: `/docs/user_guide.md`
- Import/Export Spec: `/docs/import_export_spec.md`
- Assumptions Log: `/docs/assumptions.md`

### Reporting Issues
- GitHub: https://github.com/VBlackJack/MineraLog/issues
- Email: support@mineralog.app (if available)

### Contributing
- Source code: https://github.com/VBlackJack/MineraLog
- Pull requests welcome!
- Follow Conventional Commits format

---

## Version History

### v1.0.0 (2025-11-12)
- Initial release
- Core mineral cataloging
- Photo management (4 types)
- Provenance tracking with maps
- Storage location hierarchy
- Search & filters
- QR label generation (2 templates)
- ZIP import/export with encryption
- CSV import/export
- Scheduled backups
- Demo dataset
- English/French localization

---

**Happy Collecting! 🔮💎⛏️**
