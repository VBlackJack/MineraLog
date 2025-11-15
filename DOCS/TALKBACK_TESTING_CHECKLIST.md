# TalkBack Testing Checklist - MineraLog v1.5.0

**Purpose:** Manual accessibility verification with Android TalkBack screen reader
**Estimated Time:** 45-60 minutes
**Device:** Android phone/tablet with API 27+ or emulator

---

## Setup Instructions

### Enable TalkBack

1. Open **Settings** on your Android device
2. Navigate to **Accessibility**
3. Find **TalkBack** and enable it
4. Complete the TalkBack tutorial (recommended for first-time users)

### TalkBack Gestures Reference

| Gesture | Action |
|---------|--------|
| **Swipe right** | Move to next element |
| **Swipe left** | Move to previous element |
| **Double tap** | Activate selected element |
| **Two-finger swipe down** | Read from top of screen |
| **Two-finger swipe up** | Read from current position |
| **Swipe up then right** | Open global context menu |
| **Swipe down then right** | Open local context menu |
| **Two-finger double tap** | Pause/resume TalkBack |
| **Three-finger swipe up/down** | Scroll |

---

## Testing Procedure

For each screen below:
1. ✅ Navigate to the screen
2. ✅ Use swipe gestures to move through all elements
3. ✅ Verify each item announces correctly
4. ✅ Test all interactive elements (buttons, fields, switches)
5. ✅ Document any issues in the "Issues Found" column

---

## Screen 1: Home / Mineral List

### Navigation Elements

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| QR Scanner button | "Scan QR code, button" | ⬜ | ⬜ | |
| Import CSV button | "Import CSV, button" | ⬜ | ⬜ | |
| Bulk edit button | "Bulk edit, button" | ⬜ | ⬜ | |
| Statistics button | "Statistics, button" | ⬜ | ⬜ | |
| Settings button | "Settings, button" | ⬜ | ⬜ | |
| Add mineral FAB | "Add mineral, button" | ⬜ | ⬜ | |

### Search & Filter

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Search field | "Search minerals, edit box" | ⬜ | ⬜ | |
| Search icon | "Search" | ⬜ | ⬜ | |
| Clear search button | "Clear search, button" (when text present) | ⬜ | ⬜ | |
| Filter button (no filters) | "Filter, button, No active filters" | ⬜ | ⬜ | |
| Filter button (with filters) | "Filter, button, X active filters" | ⬜ | ⬜ | |
| Active filter chip | Announces filter summary | ⬜ | ⬜ | |
| Clear filter button | "Clear filter, button" | ⬜ | ⬜ | |

### Mineral List

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Mineral list item | Announces mineral name and details | ⬜ | ⬜ | |
| Empty collection message | "Your collection is empty. Start building..." | ⬜ | ⬜ | |
| No search results message | "No search results found for..." | ⬜ | ⬜ | |
| Loading indicator | "Loading minerals" | ⬜ | ⬜ | |

### Selection Mode

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Enter selection (bulk edit button) | Activates selection mode | ⬜ | ⬜ | |
| Selection count | "X selected" in top bar | ⬜ | ⬜ | |
| Exit selection | "Exit selection, button" | ⬜ | ⬜ | |
| Select all button | "Select all, button" | ⬜ | ⬜ | |
| Actions menu | "Actions, button" | ⬜ | ⬜ | |

### Bulk Operations

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Bulk operation progress card | "Operation in progress: X of Y items" | ⬜ | ⬜ | |
| Progress percentage | Announces percentage value | ⬜ | ⬜ | |
| Operation completion | "Operation completed: X items" | ⬜ | ⬜ | |

**Screen 1 Score:** ___/20 Pass
**Critical Issues:** ____________________________________

---

## Screen 2: Add Mineral

### Navigation

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Back button | "Back, button" | ⬜ | ⬜ | |
| Save button | "Save, button" | ⬜ | ⬜ | |
| Draft indicator | Announces if draft exists | ⬜ | ⬜ | |

### Required Fields

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Required field legend | Announces "* indicates required field" | ⬜ | ⬜ | |
| Name field (empty) | "Name, required, edit box" | ⬜ | ⬜ | |
| Name field (error) | "Name is required. This field cannot be empty" | ⬜ | ⬜ | |
| Error message appears | Announces immediately when field loses focus | ⬜ | ⬜ | |
| Supporting error text | Visible error text is read | ⬜ | ⬜ | |

### Form Fields

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Group field | "Group, edit box" | ⬜ | ⬜ | |
| Formula field | "Chemical Formula, edit box" | ⬜ | ⬜ | |
| Notes field | "Notes, edit box" | ⬜ | ⬜ | |

### Tooltip Dropdown Fields

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Diaphaneity field | "Diaphaneity dropdown. Current value: none. Tap to select from X options" | ⬜ | ⬜ | |
| Tooltip toggle | "Show tooltip, button" / "Hide tooltip, button" | ⬜ | ⬜ | |
| Tooltip content | Announces full tooltip text | ⬜ | ⬜ | |
| Dropdown menu open | Lists all available options | ⬜ | ⬜ | |
| Dropdown selection | "Select [option], button" | ⬜ | ⬜ | |

**Test all TooltipDropdownField components:**
- ⬜ Cleavage
- ⬜ Fracture
- ⬜ Luster
- ⬜ Streak
- ⬜ Habit
- ⬜ Crystal System

### Tag Autocomplete

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Tags field | "Tags field. Enter comma-separated tags. Autocomplete suggestions available" | ⬜ | ⬜ | |
| Type character | Announces character typed | ⬜ | ⬜ | |
| Suggestions appear | "X tag suggestions available" | ⬜ | ⬜ | |
| Navigate suggestions | Each suggestion announces correctly | ⬜ | ⬜ | |
| Select suggestion | "Select tag: [tag name]" | ⬜ | ⬜ | |
| Tag added | Announces tag was added | ⬜ | ⬜ | |

### Photo Management

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| "No photos" state | Announces empty state | ⬜ | ⬜ | |
| "Add from gallery" button | **Expected: "Open gallery, button"** | ⬜ | ⬜ | **Known issue: no description** |
| "Take photo" button | **Expected: "Take photo, button"** | ⬜ | ⬜ | **Known issue: no description** |
| Photo thumbnail | "Photo" (should announce type) | ⬜ | ⬜ | |
| Edit caption button | "Edit caption, button" | ⬜ | ⬜ | |
| Change type button | "Change type, button" | ⬜ | ⬜ | |
| Remove photo button | "Remove photo, button" | ⬜ | ⬜ | |

**Screen 2 Score:** ___/30 Pass
**Critical Issues:** ____________________________________

---

## Screen 3: Edit Mineral

### Same as Add Mineral, plus:

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Loading state | Announces "Loading" while fetching mineral | ⬜ | ⬜ | |
| Existing data populated | Fields announce current values | ⬜ | ⬜ | |

**Screen 3 Score:** ___/30 Pass
**Critical Issues:** ____________________________________

---

## Screen 4: Camera Capture

### Navigation

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Back button | "Back, button" | ⬜ | ⬜ | |
| Photo type selector | "Select photo type, button" | ⬜ | ⬜ | |
| Flash toggle (off) | "Enable flash, button" | ⬜ | ⬜ | |
| Flash toggle (on) | "Disable flash, button" | ⬜ | ⬜ | |

### Permission State

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Permission required message | Should announce requirement | ⬜ | ⬜ | **Known issue: icon has no description** |
| "Grant Permission" button | "Grant Permission, button" | ⬜ | ⬜ | |
| Permission rationale text | Announces explanation text | ⬜ | ⬜ | |

### Camera Active

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Camera preview | Should announce camera state | ⬜ | ⬜ | **Known issue: no announcement** |
| Photo type indicator | Should announce current type | ⬜ | ⬜ | **Known issue: no announcement** |
| Capture button | "Capture photo, button" | ⬜ | ⬜ | |
| Capture in progress | Should announce "Capturing..." | ⬜ | ⬜ | **Known issue: no announcement** |
| Capture success | Should announce "Photo captured" | ⬜ | ⬜ | **Known issue: no announcement** |

### Photo Type Dropdown

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Dropdown opens | Lists all photo types | ⬜ | ⬜ | |
| Selected type | "Selected" checkmark announced | ⬜ | ⬜ | |
| Select new type | Should announce type change | ⬜ | ⬜ | **Known issue: no live region** |

**Screen 4 Score:** ___/15 Pass
**Critical Issues:** ____________________________________

**Expected Score:** ~9/15 due to known issues

---

## Screen 5: Settings

### Navigation

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Back button | "Back, button" | ⬜ | ⬜ | |

### Action Items

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Export backup icon | **Expected: icon description** | ⬜ | ⬜ | **Known issue: no description** |
| Export backup text | "Export backup" | ⬜ | ⬜ | |
| Import backup icon | **Expected: icon description** | ⬜ | ⬜ | **Known issue: no description** |
| Import backup text | "Import backup" | ⬜ | ⬜ | |
| Import CSV icon | **Expected: icon description** | ⬜ | ⬜ | **Known issue: no description** |
| Import CSV text | "Import CSV data" | ⬜ | ⬜ | |

### Switch Controls

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| "Copy photos" label | Announces label text | ⬜ | ⬜ | |
| "Copy photos" switch | Should announce "Enabled/Disabled" | ⬜ | ⬜ | **Known issue: not linked to label** |
| Toggle switch | Should announce state change | ⬜ | ⬜ | **Known issue: no live region** |
| "Encrypt by default" label | Announces label text | ⬜ | ⬜ | |
| "Encrypt by default" switch | Should announce "Enabled/Disabled" | ⬜ | ⬜ | **Known issue: not linked to label** |

### Export/Import Flow

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| Tap export | Opens file picker | ⬜ | ⬜ | |
| Export progress | Should announce progress | ⬜ | ⬜ | **Known issue: no live region** |
| Export success | Announces via snackbar | ⬜ | ⬜ | |
| Export error | Announces error message | ⬜ | ⬜ | |
| Permission error | "Open Settings" button announced | ⬜ | ⬜ | |

### Dialogs

| Element | Expected Announcement | ✓ Pass | ✗ Fail | Issues Found |
|---------|----------------------|--------|--------|--------------|
| About dialog appears | Announces dialog title | ⬜ | ⬜ | |
| Navigate dialog content | Can navigate through sections | ⬜ | ⬜ | **Structure unclear** |
| Close dialog | "Close, button" | ⬜ | ⬜ | |
| Warning dialog | Icon correctly decorative | ⬜ | ⬜ | |

**Screen 5 Score:** ___/20 Pass
**Critical Issues:** ____________________________________

**Expected Score:** ~11/20 due to known issues

---

## Overall Results Summary

### Test Completion

| Screen | Score | % Pass | Grade | Critical Issues |
|--------|-------|--------|-------|-----------------|
| Home/Mineral List | ___/20 | ___% | ___ | |
| Add Mineral | ___/30 | ___% | ___ | |
| Edit Mineral | ___/30 | ___% | ___ | |
| Camera Capture | ___/15 | ___% | ___ | |
| Settings | ___/20 | ___% | ___ | |
| **TOTAL** | ___/115 | ___% | ___ | |

### Grading Scale

- **A+ (95-100%)**: Excellent - Full WCAG 2.1 AA compliance
- **A (90-94%)**: Very Good - Minor issues only
- **A- (85-89%)**: Good - Few moderate issues
- **B+ (80-84%)**: Above Average - Some improvements needed
- **B (70-79%)**: Average - Multiple improvements needed
- **B- (60-69%)**: Below Average - Significant gaps
- **C or lower (<60%)**: Poor - Major accessibility barriers

### Expected Results (Based on Code Audit)

| Screen | Expected Score | Expected Grade |
|--------|---------------|----------------|
| Home/Mineral List | 19/20 (95%) | A+ |
| Add Mineral | 27/30 (90%) | A |
| Edit Mineral | 27/30 (90%) | A |
| Camera Capture | 9/15 (60%) | B- |
| Settings | 11/20 (55%) | C+ |
| **Overall** | 93/115 (81%) | **B+** |

---

## Known Issues Reference

### High Priority (Blockers)

1. **Camera Capture: No capture state announcements**
   - Capturing, success, error states not announced
   - Fix: Add live regions for camera states

2. **Settings: Switch controls not linked to labels**
   - Switches announce separately from their descriptions
   - Fix: Use `semantics(mergeDescendants = true)`

3. **Settings: Missing icon descriptions**
   - Export/Import icons have `contentDescription = null`
   - Fix: Add descriptions to all action icons

4. **PhotoManager: Missing button descriptions**
   - Gallery/Camera buttons have no descriptions
   - Fix: Add "Open gallery" / "Take photo" descriptions

### Medium Priority

5. **Camera: No photo type change announcements**
   - Type selection not announced via live region
   - Fix: Add live region for type changes

6. **Settings: No export/import progress announcements**
   - Operations progress not announced
   - Fix: Add live region for state changes

---

## Report Submission

**Tester Name:** ______________________________
**Date Tested:** ______________________________
**Device/Emulator:** ______________________________
**Android Version:** ______________________________
**TalkBack Version:** ______________________________

**Additional Notes:**
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________

**Recommendations:**
_________________________________________________________________
_________________________________________________________________
_________________________________________________________________

---

## Next Steps After Testing

1. **Document all failures** in the "Issues Found" column
2. **Calculate final scores** for each screen
3. **Compare with expected results** from code audit
4. **Prioritize fixes** based on impact
5. **Create GitHub issues** for each critical problem
6. **Re-test after fixes** to verify improvements

**Good luck with testing!** 🎯
