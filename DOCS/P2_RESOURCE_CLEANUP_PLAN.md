# P2.9: Unused Resource Cleanup Plan

## Status: Requires Lint Analysis

Resource cleanup requires running Android Lint to identify unused resources. This document provides a plan for cleanup when Gradle/Lint is available.

## Why Requires Lint?

1. **Safe identification**: Android Lint accurately identifies truly unused resources
2. **False positives**: Manual search can miss dynamic resource references
3. **Runtime validation**: Ensures no strings are used via reflection or dynamic lookup
4. **Multi-locale handling**: Properly handles translated strings

## Cleanup Procedure (When Lint Available)

### Step 1: Run Android Lint

```bash
./gradlew lintRelease
```

Look for `UnusedResources` warnings in the report:
- `build/reports/lint-results-release.html`
- `build/reports/lint-results-release.xml`

### Step 2: Review Unused String Resources

Expected findings based on context:
- Translation strings for removed features
- Old error messages no longer displayed
- Deprecated UI labels
- Unused dialog messages

### Step 3: Automated Cleanup

Use Android Studio's built-in cleanup:
1. Analyze → Run Inspection by Name → "Unused resources"
2. Review all findings carefully
3. Click "Remove All Unused Resources"
4. **Important**: Review the preview before applying

### Step 4: Manual Verification

After automated cleanup, verify:
```bash
# Build the app
./gradlew assembleDebug

# Run all tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Manual smoke test:
# 1. Launch app
# 2. Navigate all screens
# 3. Trigger all dialogs
# 4. Test all language switches
# 5. Verify no missing strings
```

### Step 5: Check Translations

Verify all translated files are cleaned consistently:
- `/app/src/main/res/values/strings.xml` (English)
- `/app/src/main/res/values-fr/strings.xml` (French)
- Any other language files

## Risk Mitigation

### High Risk Areas (Review Carefully)

1. **Dynamic String References**
   ```kotlin
   // These won't be detected by Lint
   val resourceName = "app_name_$variant"
   getString(resources.getIdentifier(resourceName, "string", packageName))
   ```

2. **Third-party Library Strings**
   ```kotlin
   // Library might reference app strings
   SomeLibrary.configure(R.string.custom_message)
   ```

3. **Compose String Resources**
   ```kotlin
   // Ensure Lint recognizes Compose string usage
   stringResource(R.string.some_label)
   ```

4. **Test Resources**
   - Don't remove strings used in tests
   - Check test fixtures and mock data

### Safe to Remove (Low Risk)

1. **Commented out strings**
2. **Strings with "old_", "legacy_", "unused_" prefix**
3. **Strings for removed features (verify with git history)**
4. **Duplicate strings (consolidate first, then remove)**

## Current Resource Status

Based on manual inspection:

### String Resources

```bash
# Count by file:
app/src/main/res/values/strings.xml: ~450 lines
app/src/main/res/values-fr/strings.xml: ~450 lines (French translations)
```

### Potential Cleanup Categories

1. **Error messages**: Some may be for deprecated error handling
2. **Dialog labels**: Check if all dialogs are still in use
3. **Menu items**: Verify all menu options are implemented
4. **Settings labels**: Ensure all settings screens use these
5. **Import/Export strings**: Validate against backup flows

### Expected Savings

Based on context and typical Android app cleanup:
- **Optimistic estimate**: 100-150 unused strings (~20-25%)
- **Conservative estimate**: 50-100 unused strings (~10-15%)
- **Realistic estimate**: ~225 strings mentioned in requirements seems high

**Note**: The 225 unused strings figure from requirements may be an estimate. Actual cleanup should be based on Lint findings, not arbitrary numbers.

## Best Practices for Future

### 1. Use String References Validation

Add to CI pipeline:
```kotlin
// Create a test that validates all string resources are used
@Test
fun `verify all string resources are referenced`() {
    val allStrings = R.string::class.java.fields
    allStrings.forEach { field ->
        assertTrue("String ${field.name} is not used", isStringUsed(field.name))
    }
}
```

### 2. Naming Conventions

Use clear prefixes to indicate purpose:
- `error_*`: Error messages
- `dialog_*`: Dialog strings
- `screen_*`: Screen labels
- `button_*`: Button labels
- `hint_*`: Input hints

### 3. Resource Documentation

Add comments for non-obvious strings:
```xml
<!-- Used dynamically in QR code generation -->
<string name="qr_mineral_label">Mineral: %1$s</string>

<!-- Referenced by third-party map library -->
<string name="map_marker_title">Collection Location</string>
```

### 4. Regular Audits

Schedule quarterly resource audits:
- Run Lint analysis
- Review unused resources
- Clean up incrementally
- Update translations

## Alternative: Gradual Cleanup

If full cleanup is risky, use gradual approach:

1. **Phase 1**: Remove obviously unused strings (with "unused_" prefix)
2. **Phase 2**: Remove deprecated feature strings (check git history)
3. **Phase 3**: Remove duplicate strings (consolidate first)
4. **Phase 4**: Remove remaining Lint-identified unused strings

Each phase includes full test suite validation.

## Recommendation

**Defer aggressive cleanup until Lint analysis available.**

Instead:
1. ✅ Add resource usage validation tests
2. ✅ Improve naming conventions for new strings
3. ✅ Document purpose of non-obvious strings
4. ⏳ Wait for Lint analysis before mass deletion

## Safety Checklist

Before removing any resource:
- [ ] Verified by Android Lint as unused
- [ ] Searched codebase for string name references
- [ ] Checked for dynamic resource lookups
- [ ] Verified not used in tests
- [ ] Checked all language files
- [ ] Ran full test suite after removal
- [ ] Manual testing completed
- [ ] Git history reviewed for context

## Commands for Cleanup (When Ready)

```bash
# 1. Generate Lint report
./gradlew lintRelease --continue

# 2. View unused resources
grep "UnusedResources" build/reports/lint-results-release.txt

# 3. Remove unused resources (Android Studio)
# Refactor → Remove Unused Resources

# 4. Verify build
./gradlew clean assembleDebug assembleRelease

# 5. Run all tests
./gradlew test connectedAndroidTest

# 6. Check APK size reduction
ls -lh app/build/outputs/apk/release/app-release.apk
```

## Expected Benefits

After proper cleanup:
- **APK size**: ~50-200 KB reduction (depending on cleanup extent)
- **Build time**: Marginal improvement (~1-2%)
- **Maintainability**: Easier to find relevant strings
- **Translation cost**: Fewer strings to translate for new languages

## Conclusion

Unused resource cleanup is valuable but requires proper tooling (Lint) and validation (tests) to execute safely. Defer mass cleanup until environment supports comprehensive validation.
