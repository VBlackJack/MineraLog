# MineraLog v3.0.0 - Manual Test Plan

**Version**: 3.0.0-rc
**Date**: 2025-11-16
**Tester**: [Name]
**Test Environment**: [Device/Emulator Details]

---

## Test Matrix - Devices

Test on a variety of devices to ensure compatibility across Android versions and screen configurations.

| Device | Android | Screen | Density | Status | Tester | Notes |
|--------|---------|--------|---------|--------|--------|-------|
| Pixel 6 | 14 | 6.4" | xxhdpi | ⬜ | | |
| Samsung S21 | 13 | 6.2" | xxxhdpi | ⬜ | | |
| OnePlus 9 | 12 | 6.55" | xxhdpi | ⬜ | | |
| Xiaomi Redmi | 11 | 6.67" | xhdpi | ⬜ | | |
| Generic Tablet | 10 | 10" | mdpi | ⬜ | | |
| Emulator (Min SDK) | 8.1 (API 27) | 5.5" | hdpi | ⬜ | | Critical: Test minimum SDK |

**Legend**: ✅ Pass | ❌ Fail | ⚠️ Warning | ⬜ Not Tested

---

## Flow 1: First Launch & Onboarding

**Objective**: Verify app initializes correctly and provides good first impression.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 1.1 | Install APK and launch app | App launches without crash | ⬜ | |
| 1.2 | Check database creation | Database created with SQLCipher encryption | ⬜ | Check `/data/data/net.meshcore.mineralog/databases/` |
| 1.3 | Verify no permission prompts | No permissions requested on first launch (privacy-first) | ⬜ | |
| 1.4 | Check home screen empty state | Empty state shown with "Add Mineral" CTA | ⬜ | Should have clear message |
| 1.5 | Verify language detection | App shows FR if device FR, else EN | ⬜ | Test with device locale changes |
| 1.6 | Check app version | About screen shows v3.0.0 | ⬜ | Settings → About |

---

## Flow 2: Add First Mineral (Simple)

**Objective**: Test the core mineral creation workflow.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 2.1 | Tap "Add Mineral" FAB | Add Mineral screen opens | ⬜ | |
| 2.2 | Submit empty form | Name field shows error (required) | ⬜ | Validation should work |
| 2.3 | Enter name < 2 chars | Shows "Minimum 2 characters" error | ⬜ | |
| 2.4 | Enter valid name "Quartz" | Name accepted, no error | ⬜ | |
| 2.5 | Select mineral group "Silicates" | Group dropdown works | ⬜ | |
| 2.6 | Enter formula "SiO₂" | Formula field accepts special characters | ⬜ | |
| 2.7 | Select crystal system "Hexagonal" | Crystal system dropdown works | ⬜ | |
| 2.8 | Enter Mohs hardness 7.0 | Numeric input accepted | ⬜ | |
| 2.9 | Add tags "favorites, display" | Tags parsed correctly | ⬜ | Check comma separation |
| 2.10 | Tap "Save" | Success message, returns to home | ⬜ | |
| 2.11 | Verify mineral appears in list | "Quartz" visible in home screen | ⬜ | |
| 2.12 | Tap mineral card | Detail view opens with all data | ⬜ | |

---

## Flow 3: Reference Mineral Library

**Objective**: Test the new v3.0 reference library feature.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 3.1 | Navigate to Reference Library | Menu → Reference Library opens | ⬜ | |
| 3.2 | Verify 300+ minerals loaded | List shows minerals with pagination | ⬜ | Should have skeleton loading |
| 3.3 | Search by name "Fluorite" | Search returns Fluorite entries | ⬜ | Test FTS5 search |
| 3.4 | Search by name "Fluorine" (FR) | Search returns Fluorite (bilingual) | ⬜ | Test French names |
| 3.5 | Filter by group "Halides" | Only halide minerals shown | ⬜ | |
| 3.6 | Tap reference mineral | Detail view shows all 17 collector fields | ⬜ | Verify careInstructions, hazards, etc. |
| 3.7 | Check careInstructions field | Non-empty for sensitive minerals | ⬜ | Example: Selenite care |
| 3.8 | Check hazards field | Shows warnings for toxic minerals | ⬜ | Example: Cinnabar (mercury) |
| 3.9 | Check identificationTips | Helpful tips displayed | ⬜ | |
| 3.10 | Check geologicalEnvironment | Geological context shown | ⬜ | |
| 3.11 | Tap "Link to Specimen" button | Opens mineral creation with auto-fill | ⬜ | Test reference linking |
| 3.12 | Verify properties auto-filled | Formula, hardness, system pre-filled | ⬜ | From reference data |
| 3.13 | Create custom reference mineral | "Add Reference Mineral" works | ⬜ | User-defined mineral |
| 3.14 | Filter user-defined minerals | Filter chip shows only custom refs | ⬜ | |

---

## Flow 4: Advanced Features (Aggregates)

**Objective**: Test mineral aggregate support (v3.0 major feature).

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 4.1 | Create aggregate mineral "Granite" | Type selector shows SIMPLE/AGGREGATE/ROCK | ⬜ | |
| 4.2 | Select type AGGREGATE | Component editor appears | ⬜ | |
| 4.3 | Add component "Quartz" | Component added to list | ⬜ | |
| 4.4 | Set component percentage 40% | Percentage accepted | ⬜ | |
| 4.5 | Add component "Feldspar" 50% | Second component added | ⬜ | |
| 4.6 | Add component "Mica" 10% | Third component added | ⬜ | |
| 4.7 | Verify percentage sum | Total shows 100% | ⬜ | |
| 4.8 | Try to add > 100% | Validation error shown | ⬜ | |
| 4.9 | Set component role "Matrix" | Role dropdown works | ⬜ | |
| 4.10 | Link component to reference | Component auto-fills from reference | ⬜ | |
| 4.11 | Save aggregate | Aggregate saved successfully | ⬜ | |
| 4.12 | View aggregate detail | Shows all components with % | ⬜ | |

---

## Flow 5: Photo Management

**Objective**: Test photo capture, gallery, and viewing workflows.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 5.1 | Tap camera icon in mineral detail | Camera permission prompt appears | ⬜ | First time only |
| 5.2 | Grant camera permission | Camera preview opens | ⬜ | |
| 5.3 | Select photo type "Normal" | Type selector shows active | ⬜ | |
| 5.4 | Capture photo | Photo captured < 2s | ⬜ | Performance requirement |
| 5.5 | Verify photo saved | Returns to detail, photo visible | ⬜ | |
| 5.6 | Capture UV Shortwave photo | Type badge shows "UV-SW" | ⬜ | |
| 5.7 | Capture Macro photo | Type badge shows "Macro" | ⬜ | |
| 5.8 | Tap photo gallery | Gallery opens with 3-column grid | ⬜ | |
| 5.9 | Tap photo | Fullscreen viewer opens | ⬜ | |
| 5.10 | Pinch to zoom | Zoom works 1x-5x | ⬜ | |
| 5.11 | Swipe to next photo | HorizontalPager navigation works | ⬜ | |
| 5.12 | Toggle torch in camera | Flashlight turns on/off | ⬜ | |
| 5.13 | Delete photo | Confirmation dialog, photo deleted | ⬜ | |
| 5.14 | Select photo from gallery | Gallery picker works | ⬜ | Alternative to camera |

---

## Flow 6: QR Code & Scanning

**Objective**: Test QR code generation and scanning workflows.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 6.1 | Tap QR icon in mineral detail | QR code generated instantly | ⬜ | Should be < 500ms |
| 6.2 | Verify QR format | Contains `mineralapp://mineral/{uuid}` | ⬜ | |
| 6.3 | Generate QR for long UUID | QR code renders correctly | ⬜ | Test with complex IDs |
| 6.4 | Print QR labels (PDF) | PDF generated with 8 labels/page | ⬜ | Menu → Labels |
| 6.5 | Customize label template | 50×30mm and 70×35mm options work | ⬜ | |
| 6.6 | Select fields for label | Name, group, formula, QR shown | ⬜ | |
| 6.7 | Open QR scanner | Scanner screen opens | ⬜ | Menu → Scan QR |
| 6.8 | Scan valid QR code | Navigates to mineral detail | ⬜ | |
| 6.9 | Scan invalid QR code | Error message shown | ⬜ | "Invalid QR code format" |
| 6.10 | Scan non-mineral QR | Error message shown | ⬜ | |

---

## Flow 7: Import/Export

**Objective**: Test data import/export workflows with encryption.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 7.1 | Export to ZIP (encrypted) | Password dialog appears | ⬜ | Settings → Export |
| 7.2 | Enter weak password "123" | Shows "Weak" strength indicator | ⬜ | |
| 7.3 | Enter strong password | Shows "Strong" strength indicator | ⬜ | Min 12 chars, mixed |
| 7.4 | Confirm password mismatch | Error shown | ⬜ | |
| 7.5 | Confirm password correct | ZIP export starts | ⬜ | |
| 7.6 | Verify ZIP created | File saved to selected location | ⬜ | |
| 7.7 | Import ZIP (encrypted) | Password prompt appears | ⬜ | |
| 7.8 | Enter wrong password | Error, attempt counter increments | ⬜ | Max 3 attempts |
| 7.9 | Enter correct password | Import succeeds, data restored | ⬜ | |
| 7.10 | Verify data integrity | All minerals, photos, metadata intact | ⬜ | |
| 7.11 | Import CSV (50 rows) | CSV import dialog opens | ⬜ | |
| 7.12 | Auto-detect encoding | UTF-8 detected correctly | ⬜ | Test with UTF-8 file |
| 7.13 | Auto-detect delimiter | Comma detected correctly | ⬜ | |
| 7.14 | Preview first 5 rows | Preview table shows data | ⬜ | |
| 7.15 | Map columns (auto) | Fuzzy matching auto-maps columns | ⬜ | |
| 7.16 | Import with validation error | Error report shown (line-specific) | ⬜ | Test with invalid data |
| 7.17 | Export to CSV | CSV file created with all fields | ⬜ | |
| 7.18 | Verify CSV injection protection | Leading = escaped | ⬜ | Test with formula "=SUM(A1:A10)" |

---

## Flow 8: Search & Filtering

**Objective**: Test search and advanced filtering features.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 8.1 | Type in search bar "Quartz" | Results update < 300ms (debounced) | ⬜ | |
| 8.2 | Search by formula "SiO₂" | Finds Quartz | ⬜ | FTS5 search |
| 8.3 | Search by notes content | Finds minerals with matching notes | ⬜ | |
| 8.4 | Search by tag "favorites" | Finds tagged minerals | ⬜ | |
| 8.5 | Open advanced filters | Filter bottom sheet opens | ⬜ | |
| 8.6 | Filter by group "Halides" | Only halides shown | ⬜ | |
| 8.7 | Filter by crystal system "Cubic" | Only cubic minerals shown | ⬜ | |
| 8.8 | Filter by hardness range 5-7 | Only minerals in range shown | ⬜ | Range slider |
| 8.9 | Filter by country "France" | Only French minerals shown | ⬜ | |
| 8.10 | Filter by status "Complete" | Only complete records shown | ⬜ | |
| 8.11 | Combine multiple filters | AND logic applied correctly | ⬜ | Group + hardness + country |
| 8.12 | Save filter preset "My Halides" | Preset saved to database | ⬜ | |
| 8.13 | Load saved preset | Filters restored correctly | ⬜ | |
| 8.14 | Sort by name A-Z | List sorted alphabetically | ⬜ | |
| 8.15 | Sort by date (newest first) | List sorted by creation date | ⬜ | |
| 8.16 | Sort by hardness (low-high) | List sorted by mohsMin | ⬜ | |

---

## Flow 9: Bulk Operations

**Objective**: Test bulk selection and operations.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 9.1 | Long-press on mineral card | Selection mode activates | ⬜ | |
| 9.2 | Select 10 minerals | Selection counter shows "10 selected" | ⬜ | |
| 9.3 | Tap "Select All" | All minerals selected | ⬜ | |
| 9.4 | Tap "Deselect All" | Selection cleared | ⬜ | |
| 9.5 | Bulk delete | Confirmation dialog appears | ⬜ | |
| 9.6 | Confirm bulk delete | All selected minerals deleted | ⬜ | |
| 9.7 | Verify cascade deletion | Photos, provenance, storage also deleted | ⬜ | No orphaned entities |
| 9.8 | Bulk export to CSV | CSV contains only selected minerals | ⬜ | |
| 9.9 | Bulk compare (3 minerals) | Comparator screen opens | ⬜ | |
| 9.10 | Compare properties | Diff highlighting works | ⬜ | Different values highlighted |

---

## Flow 10: Error Handling

**Objective**: Test error scenarios and recovery.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 10.1 | Deny camera permission | Friendly error + settings link shown | ⬜ | |
| 10.2 | Fill device storage | Error message + cleanup suggestion | ⬜ | Simulate full disk |
| 10.3 | Import invalid CSV | Line-specific error report shown | ⬜ | Test with malformed CSV |
| 10.4 | Import with SQL injection | Input sanitized, no injection | ⬜ | Test with `'; DROP TABLE--` |
| 10.5 | Import with formula injection | Leading = escaped | ⬜ | Test with `=1+1` |
| 10.6 | Kill app during import | State restored on relaunch | ⬜ | Test transaction rollback |
| 10.7 | Rotate screen during photo capture | Camera state preserved | ⬜ | |
| 10.8 | Network unavailable (maps) | Offline-first works, no crash | ⬜ | Maps optional |
| 10.9 | Invalid deep link URL | Error message, no crash | ⬜ | Test with malformed URL |

---

## Flow 11: Accessibility (TalkBack)

**Objective**: Test screen reader compatibility.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 11.1 | Enable TalkBack | All screens navigable | ⬜ | Android Settings → Accessibility |
| 11.2 | Navigate home screen | Mineral cards announced | ⬜ | Name, group, date |
| 11.3 | Activate "Add Mineral" FAB | Button announced and activated | ⬜ | |
| 11.4 | Navigate form fields | Labels announced correctly | ⬜ | |
| 11.5 | Activate dropdowns | Options announced | ⬜ | |
| 11.6 | Submit form | Success message announced | ⬜ | |
| 11.7 | Navigate photo gallery | Photo types announced | ⬜ | "Normal photo of Quartz" |
| 11.8 | Check all icons | contentDescription present | ⬜ | No "unlabeled button" |
| 11.9 | Verify touch targets | All targets ≥ 48×48dp | ⬜ | |
| 11.10 | Test focus order | Logical top-to-bottom, left-to-right | ⬜ | |

---

## Flow 12: Internationalization

**Objective**: Test bilingual support (EN/FR).

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 12.1 | Set device to French | App UI switches to French | ⬜ | Settings → Langue |
| 12.2 | Verify all strings translated | No English text visible (606/606) | ⬜ | Check all screens |
| 12.3 | Check French typography | Espaces insécables before : ; ? ! | ⬜ | "Minéral :" not "Minéral:" |
| 12.4 | Check date formatting | French format (DD/MM/YYYY) | ⬜ | |
| 12.5 | Check number formatting | French format (1 234,56) | ⬜ | Spaces + comma decimal |
| 12.6 | Search with French accents | "Fluorine" finds "Fluorite" | ⬜ | Accent-insensitive |
| 12.7 | Switch to English | All strings switch to English | ⬜ | |
| 12.8 | Verify reference minerals bilingual | French names shown in FR locale | ⬜ | "Quartz" → "Quartz" (same), but "Fluorite" → "Fluorine" |

---

## Flow 13: Performance Benchmarks

**Objective**: Verify performance targets are met.

| # | Test Case | Expected Result | Target | Status | Actual | Notes |
|---|-----------|-----------------|--------|--------|--------|-------|
| 13.1 | Cold start time | App launches | < 2s | ⬜ | | Pixel 6 or equivalent |
| 13.2 | Home screen with 1000 minerals | Smooth scroll | 60fps | ⬜ | | Check frame times |
| 13.3 | Photo capture latency | Capture to save | < 500ms | ⬜ | | Camera button to success |
| 13.4 | Database query (95th %ile) | Query execution | < 100ms | ⬜ | | Check logs |
| 13.5 | QR code generation | Generate QR | < 500ms | ⬜ | | Detail screen to QR visible |
| 13.6 | Search debounce | Search query | 300ms | ⬜ | | Time from last keystroke |
| 13.7 | Export 1000 minerals (ZIP) | Export completes | < 30s | ⬜ | | Mid-range device |
| 13.8 | Import 1000 minerals (ZIP) | Import completes | < 30s | ⬜ | | Mid-range device |
| 13.9 | APK size | Release APK | < 20MB | ⬜ | | Check build output |

---

## Flow 14: Security Validation

**Objective**: Verify security features are active.

| # | Test Case | Expected Result | Status | Notes |
|---|-----------|-----------------|--------|-------|
| 14.1 | Check database file | Encrypted (SQLCipher) | ⬜ | Use `file` command on .db file |
| 14.2 | Verify no plaintext .db | Only encrypted .db exists | ⬜ | `/data/data/.../databases/` |
| 14.3 | Check backup encryption | ZIP contains encrypted data | ⬜ | Cannot open without password |
| 14.4 | Verify Argon2id KDF | Backup uses strong KDF | ⬜ | Check crypto logs |
| 14.5 | Check keystore integration | Master key in Android Keystore | ⬜ | EncryptedSharedPreferences |
| 14.6 | Verify no debug logs in release | LogCat shows no sensitive data | ⬜ | ProGuard removes logs |
| 14.7 | Check clipboard auto-clear | Clipboard cleared after 30s | ⬜ | Copy error message, wait |
| 14.8 | Verify CSV injection protection | Leading = escaped | ⬜ | Export mineral with formula "=SUM()" |
| 14.9 | Check minimum permissions | Only camera + storage requested | ⬜ | No unnecessary perms |
| 14.10 | Verify network security config | Cleartext HTTP blocked | ⬜ | Try HTTP request (should fail) |

---

## Test Completion Summary

**Total Test Cases**: 200+
**Passed**: ___
**Failed**: ___
**Warnings**: ___
**Not Tested**: ___

**Pass Rate**: ___% (Target: ≥ 95%)

---

## Critical Issues Found

| ID | Severity | Description | Steps to Reproduce | Status |
|----|----------|-------------|-------------------|--------|
| | P0/P1/P2 | | | Open/Fixed |

---

## Performance Results

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Cold start | < 2s | ___ | ⬜ |
| Scroll (1000 items) | 60fps | ___ | ⬜ |
| Photo capture | < 500ms | ___ | ⬜ |
| DB query (p95) | < 100ms | ___ | ⬜ |
| APK size | < 20MB | ___ | ⬜ |

---

## Device Coverage

| Android Version | Devices Tested | Status |
|-----------------|----------------|--------|
| 8.1 (API 27) | ___ | ⬜ |
| 9-11 | ___ | ⬜ |
| 12-13 | ___ | ⬜ |
| 14-15 | ___ | ⬜ |

---

## Acceptance Criteria

- [ ] All P0 test cases pass (0 failures)
- [ ] P1 test cases ≥ 95% pass rate
- [ ] Performance benchmarks met
- [ ] Tested on Android 8.1 (minSdk 27)
- [ ] Tested on Android 14+ (latest)
- [ ] Accessibility: TalkBack fully functional
- [ ] Internationalization: FR/EN parity
- [ ] Security: All security tests pass
- [ ] APK size < 20MB
- [ ] No crashes on 10K monkey test events

---

## Sign-Off

**QA Lead**: ___________________  Date: __________

**Product Owner**: ___________________  Date: __________

**Release Manager**: ___________________  Date: __________

---

**Status**: ⬜ Ready for Production | ⬜ Requires Fixes
