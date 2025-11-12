# MineraLog - Implementation Assumptions

This document records all assumptions made during the autonomous implementation of MineraLog v1.0.

## Date: 2025-11-12

### General Architecture

1. **Database Migration Strategy (v1.0)**
   - Assumption: For v1.0 MVP, we use `fallbackToDestructiveMigration()` to simplify development
   - Rationale: Production apps should implement proper migrations, but for initial release this is acceptable
   - Future: v1.1+ should implement proper Room migration paths

2. **Photo Storage**
   - Assumption: Default behavior is to COPY photos to internal storage (`filesDir/media/{mineralId}/`)
   - Rationale: Ensures data safety even if original files are deleted by user
   - Alternative: Users can disable this in Settings to use URI references only
   - Storage path format: `media/{mineralId}/{timestamp}_{uuid}.jpg`

3. **Encryption Implementation**
   - Assumption: Argon2id is primary KDF, PBKDF2 is fallback for devices without native support
   - Parameters: Argon2id(t=3, m=64MB, p=2) for mobile devices
   - Cipher: AES-256-GCM for authenticated encryption
   - Key derivation: User password → Argon2id → 256-bit key

4. **Export/Import UUID Handling**
   - Assumption: UUIDs are preserved during import by default (MERGE mode)
   - REPLACE mode: Drops all existing data before import
   - MAP_IDS mode: Regenerates conflicting UUIDs and maintains remapping table
   - Rationale: Allows users to maintain consistent IDs across devices

5. **CSV Import Behavior**
   - Assumption: CSV import uses header row for column mapping
   - Missing fields: Left as NULL/default values
   - Invalid data: Skipped with error reported, doesn't abort entire import
   - ID handling: If ID exists → update, if missing → create new

6. **QR Code Deep Links**
   - Scheme: `mineralapp://mineral/{uuid}`
   - Format: QR Code ECC Level M (15% error correction)
   - Size: 200x200px for printing at 50×30mm labels
   - Behavior: Scanned QR navigates directly to mineral detail screen

7. **PDF Label Templates**
   - Template 1: 50×30mm, 4 columns × 9 rows (36 labels per A4 sheet)
   - Template 2: 70×35mm, 3 columns × 8 rows (24 labels per A4 sheet)
   - Configurable fields: Name, ID (short), Group, Formula, QR Code
   - Print margins: 5mm on all sides

8. **Search & Filters**
   - Search debounce: 300ms to reduce database queries
   - Full-text search fields: name, group, formula, notes, tags
   - Filter persistence: Not saved between app restarts (v1.0)
   - Future: v1.1 can add saved filter presets

9. **Google Maps Integration**
   - Clustering: Enabled for >10 provenance markers
   - Cluster radius: 100px default
   - Map type: Default to "Normal" (can switch to Satellite/Terrain)
   - Permissions: Gracefully degrades if location permission denied

10. **Offline-First Behavior**
    - All data stored locally in Room database
    - No cloud sync in v1.0 (future feature)
    - Export/Import is the only data transfer mechanism
    - Maps: Requires internet for tile loading, gracefully shows error otherwise

11. **Photo Types**
    - NORMAL: Standard visible light photography
    - UV_SW: Ultraviolet shortwave (typically 254nm)
    - UV_LW: Ultraviolet longwave (typically 365nm)
    - MACRO: Close-up/magnified photography

12. **Validation Rules**
    - Mohs hardness: 1.0 - 10.0 (fractional values allowed, e.g., 5.5)
    - Latitude: -90.0 to 90.0
    - Longitude: -180.0 to 180.0
    - Specific gravity: > 0
    - Weight: >= 0
    - Price/Value: >= 0

13. **Internationalization (i18n)**
    - Supported languages: English (en), French (fr)
    - Default: English
    - Switching language: Requires app restart (Android limitation for Compose)
    - Dates: Always stored as ISO-8601 UTC, displayed in local timezone

14. **WorkManager Backup Scheduling**
    - Minimum interval: 15 minutes (Android WorkManager constraint)
    - Battery optimization: Runs only when device charging OR battery >15%
    - Network: No network required (local backup)
    - Retry policy: Exponential backoff, max 3 retries

15. **Demo Dataset**
    - 15 minerals from 5 different countries
    - Variety: Different crystal systems, Mohs ranges, special properties
    - 2-3 placeholder photos per mineral (colored rectangles with labels)
    - 2 storage boxes: "Display Box A", "Storage Box 1"
    - Load via Settings → "Load Demo Data" (warns about data replacement)

16. **Accessibility**
    - Contrast ratio: WCAG AA compliant (4.5:1 for normal text)
    - TalkBack: All interactive elements have contentDescription
    - Dynamic type: Supports system font scaling up to 200%
    - Touch targets: Minimum 48×48dp for all clickable elements

17. **Performance Targets**
    - App cold start: <2s on mid-range device (Snapdragon 6 series)
    - Search query: <300ms for 1000 minerals
    - Photo thumbnail generation: <100ms using Coil caching
    - Export 1000 minerals: <30s (includes ZIP compression)
    - Import 1000 minerals: <30s (includes database inserts)

18. **Error Handling**
    - Database errors: Logged + user-friendly message shown
    - File I/O errors: Retry once, then fail gracefully with message
    - Crypto errors: Clear error message, never expose keys/passwords
    - Import errors: Continue processing, collect all errors, show summary

19. **Testing Strategy**
    - Unit tests: DAOs, ViewModels, Mappers, Crypto, Validation
    - Instrumented tests: Room integration, Navigation flows, Camera integration
    - Test coverage target: >70% for core business logic
    - CI: Runs on API 27 (minSdk) and API 35 (latest)

20. **API Key Management**
    - Google Maps API key: Stored in `local.properties` (gitignored)
    - CI: Uses dummy key (`dummy_key_for_ci`) - maps features won't work but build succeeds
    - Documentation: `local.properties.example` shows required format

## Future Considerations (v1.1+)

- Cloud sync with end-to-end encryption (PKCE/OAuth2)
- NFC tag reading/writing for storage locations
- Advanced statistics dashboard
- Comparison view for similar minerals
- Exhibition/showcase mode (display-ready view)
- Multi-user collections with role-based access
- Integration with online mineral databases (mindat.org API)

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025-11-12 | Use Compose over XML Views | Modern, declarative, better developer experience |
| 2025-11-12 | Room over raw SQLite | Type-safe, coroutines support, migration tooling |
| 2025-11-12 | Tink over manual crypto | Google-vetted, misuse-resistant API |
| 2025-11-12 | Argon2 over PBKDF2 | Superior resistance to GPU/ASIC attacks |
| 2025-11-12 | JUnit 5 over JUnit 4 | Better parameterized tests, modern API |
| 2025-11-12 | Conventional Commits | Clear history, potential for auto-changelog |
