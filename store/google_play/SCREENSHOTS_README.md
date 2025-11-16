# Google Play Store Screenshots

**Required**: 8 screenshots (16:9 aspect ratio recommended)
**Dimensions**: 1920×1080 or 2560×1440
**Format**: PNG or JPEG

## Screenshot List (Manually Capture These)

### 1. Home Screen with Mineral List (`01_home_list.png`)
**Show**:
- List of 5-10 minerals with photos
- Search bar at top
- FAB "Add Mineral" button visible
- Material 3 design

**Caption** (optional):
> Organize your collection with photos and detailed properties

---

### 2. Mineral Detail View (`02_detail_view.png`)
**Show**:
- Mineral detail screen for a specimen (e.g., Fluorite)
- Photo visible
- Properties: Name, Group, Formula, Crystal System, Hardness, etc.
- QR code icon in app bar

**Caption**:
> View all properties, photos, and provenance for each specimen

---

### 3. Reference Library Browser (`03_reference_library.png`)
**Show**:
- Reference Mineral Library screen
- List of reference minerals (Quartz, Fluorite, Calcite, etc.)
- Search bar
- Filter chips visible

**Caption**:
> Browse 300+ minerals with scientific and collector information

---

### 4. Add Mineral Form (`04_add_mineral.png`)
**Show**:
- Add Mineral screen
- Form fields filled: Name, Group, Formula, Crystal System
- Type selector showing "SIMPLE" selected
- "Link to Reference" button visible

**Caption**:
> Add minerals with auto-fill from reference library

---

### 5. QR Code Generation (`05_qr_code.png`)
**Show**:
- QR code displayed for a mineral
- QR code centered and large enough to scan
- Mineral name below QR code
- "Share" and "Print Label" options

**Caption**:
> Generate QR codes for easy identification and labeling

---

### 6. Advanced Filters (`06_filters.png`)
**Show**:
- Advanced filter bottom sheet open
- Filters shown: Group, Crystal System, Hardness range slider, Country
- Some filters applied (chips visible)

**Caption**:
> Filter by group, hardness, location, quality, and more

---

### 7. CSV Import Dialog (`07_import_csv.png`)
**Show**:
- CSV import screen
- Column mapping interface showing source → destination
- Preview of first 5 rows
- "Import" button

**Caption**:
> Import your existing collection from CSV with smart column mapping

---

### 8. Encrypted Backup Screen (`08_backup.png`)
**Show**:
- Settings screen with Export section
- "Export to ZIP" option highlighted
- Password protection dialog (optional)
- Success message: "Backup created successfully"

**Caption**:
> Secure encrypted backups protect your collection data

---

## Screenshot Guidelines

### Framing
- Use a clean device with good screen resolution (Pixel 6, Samsung S21, etc.)
- Portrait orientation
- Show full screen (no black bars)
- Remove status bar notifications (Do Not Disturb mode)
- Ensure good battery level (>50%) in status bar

### Content
- Use realistic but diverse mineral data:
  - Mix of common and rare minerals
  - Different colors and photos
  - Various groups (Silicates, Halides, Oxides, etc.)
- Show complete, professional-looking data (not "Test123")
- Use high-quality photos (well-lit, in-focus)

### Lighting & Colors
- Capture in light theme (better for store listings)
- Ensure good contrast
- Avoid dark/washed-out screenshots

### Text Overlay (Optional)
Google Play allows text overlay on screenshots. Consider adding:
- Feature callout: "300+ Reference Minerals"
- Security badge: "AES-256 Encrypted"
- Privacy badge: "100% Offline"

**Tool**: Use Canva, Figma, or Android Studio's Layout Inspector

---

## Capture Instructions

### Using Android Studio Emulator
1. Launch emulator (Pixel 6 API 35 recommended)
2. Install debug APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Load demo data: Settings → Load Demo Data
4. Navigate to each screen
5. Take screenshots: Emulator toolbar → Camera icon

### Using Real Device
1. Enable Developer Options
2. Enable USB Debugging
3. Connect device via USB
4. Load demo data
5. Take screenshots: `adb shell screencap -p /sdcard/screenshot.png`
6. Pull screenshots: `adb pull /sdcard/screenshot.png screenshots/`

### Using Device Directly
1. Load demo data
2. Take screenshots: Power + Volume Down (most devices)
3. Transfer to computer via USB or cloud

---

## Naming Convention

Use consistent naming:
- `01_home_list.png` (not `Screenshot_2025_01_15.png`)
- Sequential numbering matches order in Google Play Console
- Descriptive names for easier management

---

## Upload to Google Play Console

1. **Google Play Console** → Your app → Store presence → Main store listing
2. **Scroll to "Screenshots"** section
3. **Phone screenshots** (required):
   - Upload all 8 screenshots
   - Drag to reorder if needed
4. **7-inch tablet screenshots** (optional):
   - Use same screenshots resized or capture from tablet emulator
5. **10-inch tablet screenshots** (optional):
   - Capture from 10" tablet for optimized layout

---

## Alternative: Automated Screenshot Generation

**Espresso UI Testing** can automate screenshot capture:

```kotlin
// In androidTest/
@Test
fun captureScreenshots() {
    // Navigate to home screen
    onView(withId(R.id.home_screen)).check(matches(isDisplayed()))
    takeScreenshot("01_home_list")

    // Navigate to detail
    onView(withText("Fluorite")).perform(click())
    takeScreenshot("02_detail_view")

    // ... etc
}

fun takeScreenshot(name: String) {
    val screenshot = Screenshot.capture()
    screenshot.name = name
    screenshot.process(/* save to file */)
}
```

**Tool**: Use `androidx.test.runner.screenshot.Screenshot` or Fastlane Screengrab.

---

## Quality Checklist

Before uploading:
- [ ] All 8 screenshots captured
- [ ] Aspect ratio 16:9 or close (1920×1080, 2560×1440)
- [ ] File size < 8 MB each (Google Play limit)
- [ ] PNG or JPEG format
- [ ] No personal data visible (if using real collection)
- [ ] Status bar clean (no notifications)
- [ ] Text readable (no blurry text)
- [ ] Consistent theme (all light or all dark)
- [ ] Realistic data (not "Test123")
- [ ] Showcases key features (reference library, QR codes, filters, backup)

---

## Storage

Store screenshots in:
- `store/google_play/screenshots/phone/` (this directory)
- Commit to Git (if < 1 MB each) or store in Git LFS
- Also keep backup in project management tool (Notion, Google Drive)

---

**Need help?** See [Google Play Screenshot Specifications](https://support.google.com/googleplay/android-developer/answer/9866151)
