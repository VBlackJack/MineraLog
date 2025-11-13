# Accessibility Guide - MineraLog

## 🎯 WCAG 2.1 AA Compliance

MineraLog is committed to **WCAG 2.1 Level AA** accessibility compliance, ensuring the app is usable by everyone, including users with disabilities.

**Current Status:** ✅ **100% Compliant** (as of v1.6.1)

**Recent Improvements (v1.6.1 - 2025-11-13):**
- ✅ PDF label generation now shows progress indicator with live region
- ✅ Detail screen loading state announces to screen readers
- ✅ Filter sections (Photos/Fluorescence) now expandable for consistency
- ✅ Reduced redundant slider announcements (decorative text marked)
- ✅ Comprehensive automated test suite with 5 core WCAG checks

See [DOCS/UX_ACCESSIBILITY_IMPROVEMENTS_SPEC.md](DOCS/UX_ACCESSIBILITY_IMPROVEMENTS_SPEC.md) for full details.

---

## 📋 Table of Contents

1. [Core Principles](#core-principles)
2. [Implementation Checklist](#implementation-checklist)
3. [Code Examples](#code-examples)
4. [Testing](#testing)
5. [Tools & Resources](#tools--resources)

---

## Core Principles

### 1. Perceivable
Users must be able to perceive the information being presented.

- ✅ All interactive elements have descriptive labels
- ✅ Color is not the only means of conveying information
- ✅ Text contrast meets 4.5:1 minimum ratio
- ✅ Text is resizable up to 200% without loss of content

### 2. Operable
Users must be able to operate the interface.

- ✅ All functionality is keyboard accessible
- ✅ Touch targets are minimum 48dp × 48dp
- ✅ No timing-sensitive operations without user control
- ✅ Focus management in dialogs and forms

### 3. Understandable
Users must be able to understand the interface and information.

- ✅ Error messages are clear and actionable
- ✅ Input labels and placeholders are descriptive
- ✅ Technical fields have inline help tooltips
- ✅ Consistent navigation patterns throughout

### 4. Robust
Content must work with current and future assistive technologies.

- ✅ Semantic HTML/Compose structures
- ✅ TalkBack/screen reader compatibility tested
- ✅ Automated accessibility tests in CI

---

## Implementation Checklist

Use this checklist when creating new UI components:

### ✅ Text Fields

```kotlin
OutlinedTextField(
    value = value,
    onValueChange = { /* ... */ },
    label = { Text("Field Label *") },  // ✅ Clear label
    isError = hasError,
    modifier = Modifier
        .fillMaxWidth()
        .semantics {
            if (hasError) {
                error("Descriptive error message")  // ✅ Error semantics
                liveRegion = LiveRegionMode.Polite  // ✅ Announce errors
            }
        },
    supportingText = if (hasError) {
        { Text("Error hint") }  // ✅ Visual error message
    } else null,
    keyboardOptions = KeyboardOptions(
        imeAction = ImeAction.Next  // ✅ Keyboard navigation
    )
)
```

### ✅ Buttons & Icons

```kotlin
// ✅ Good: Icon button with contentDescription
IconButton(onClick = { /* ... */ }) {
    Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "Add new mineral"  // ✅ Descriptive
    )
}

// ❌ Bad: No contentDescription
IconButton(onClick = { /* ... */ }) {
    Icon(Icons.Default.Add, contentDescription = null)  // ❌ Not accessible
}

// ✅ Ensure minimum touch target
Button(
    onClick = { /* ... */ },
    modifier = Modifier.heightIn(min = 48.dp)  // ✅ Min height
) {
    Text("Save")
}
```

### ✅ Dialogs & Bottom Sheets

```kotlin
@Composable
fun MyDialog(onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }

    // ✅ Auto-focus first input field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dialog Title") },
        text = {
            OutlinedTextField(
                /* ... */
                modifier = Modifier.focusRequester(focusRequester)  // ✅ Focus
            )
        },
        confirmButton = {
            Button(onClick = { /* ... */ }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### ✅ Lists & Cards

```kotlin
// ✅ Merge semantics for complex cards
Card(
    modifier = Modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) {  // ✅ Merge for screen readers
            contentDescription = "Quartz specimen from Brazil, 5 out of 5 rating"
        }
        .clickable { /* ... */ }
) {
    // Card content
}
```

### ✅ Loading States

```kotlin
// ✅ Announce loading state to screen readers
if (isLoading) {
    Box(
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite  // ✅ Announce
        }
    ) {
        CircularProgressIndicator()
        Text("Loading minerals...")
    }
}
```

### ✅ Empty States

```kotlin
// ✅ Provide meaningful empty state descriptions
if (minerals.isEmpty()) {
    Column(
        modifier = Modifier.semantics {
            contentDescription = "No minerals in your collection yet. " +
                    "Tap the add button to create your first entry."
        }
    ) {
        Icon(Icons.Default.Collections, contentDescription = null)
        Text("No minerals yet")
        Button(onClick = { /* ... */ }) {
            Text("Add First Mineral")
        }
    }
}
```

### ✅ Technical Fields with Tooltips

```kotlin
// ✅ Use TooltipTextField for complex fields
TooltipTextField(
    value = diaphaneity,
    onValueChange = { /* ... */ },
    label = "Diaphaneity",
    tooltipText = "Transparency level: transparent (light passes through), " +
            "translucent (light passes through but not clearly), " +
            "or opaque (no light passes through)",
    placeholder = "e.g., transparent, translucent, opaque",
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
)
```

---

## Code Examples

### Example 1: Accessible Form Screen

```kotlin
@Composable
fun AccessibleFormScreen() {
    val focusManager = LocalFocusManager.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Required field with error handling
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                hasError = it.isBlank()
            },
            label = { Text("Name *") },
            isError = hasError,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    if (hasError) {
                        error("Name is required")
                        liveRegion = LiveRegionMode.Polite
                    }
                },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            supportingText = if (hasError) {
                { Text("Name is required") }
            } else null
        )

        // Optional field
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    // Submit form
                }
            )
        )

        // Accessible button
        Button(
            onClick = { /* ... */ },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text("Submit")
        }
    }
}
```

### Example 2: Accessible List Item

```kotlin
@Composable
fun MineralListItem(
    mineral: Mineral,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(mineral.name)
                    mineral.group?.let { append(", group: $it") }
                    mineral.qualityRating?.let { append(", rating: $it out of 5") }
                }
            }
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = mineral.name,
                    style = MaterialTheme.typography.titleMedium
                )
                mineral.group?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            mineral.qualityRating?.let { rating ->
                RatingStars(
                    rating = rating,
                    // Icon-only decorative, text is merged in card semantics
                    modifier = Modifier.semantics { contentDescription = "" }
                )
            }
        }
    }
}
```

---

## Testing

### Manual Testing with TalkBack

1. **Enable TalkBack:**
   - Settings → Accessibility → TalkBack → Enable

2. **Key Gestures:**
   - Swipe right: Next element
   - Swipe left: Previous element
   - Double tap: Activate
   - Swipe up then right: Global context menu

3. **Test Checklist:**
   - [ ] Can navigate through all elements
   - [ ] All interactive elements are announced
   - [ ] Form fields have clear labels
   - [ ] Errors are announced immediately
   - [ ] Loading states are announced
   - [ ] Modal dialogs trap focus correctly

### Automated Testing

Run accessibility tests:

```bash
# Run all accessibility tests
./gradlew connectedAndroidTest

# Run specific test class (AddMineralScreen tests)
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.meshcore.mineralog.ui.accessibility.ComposeAccessibilityTest

# Run automated accessibility checks (WCAG 2.1 AA validation)
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.meshcore.mineralog.ui.accessibility.AutomatedAccessibilityTests
```

**New: Automated Accessibility Test Suite (v1.6.1)**

Five core automated checks validate WCAG 2.1 AA compliance:

1. ✅ **Touch Target Size Validator** - Verifies all interactive elements ≥ 48dp
2. ✅ **ContentDescription Coverage** - Ensures all icons have proper descriptions
3. ✅ **Semantic Properties** - Validates custom components have semantic attributes
4. ✅ **Live Region Announcements** - Checks loading/error states announce properly
5. ✅ **Text Scaling Support** - Tests UI at 200% font scale

See `AutomatedAccessibilityTests.kt` for implementation details.

### CI/CD Integration

Add to your CI pipeline (`.github/workflows/android.yml`):

```yaml
- name: Run Accessibility Tests
  run: ./gradlew connectedAndroidTest

- name: Check Accessibility Test Results
  if: failure()
  run: |
    echo "Accessibility tests failed. Please review the errors."
    exit 1
```

---

## Tools & Resources

### Testing Tools

- **Google Accessibility Scanner** (Android)
  - Install from Play Store
  - Scan any screen for accessibility issues

- **Android Studio Layout Inspector**
  - View → Tool Windows → Layout Inspector
  - Check touch target sizes and content descriptions

### Contrast Checkers

- **WebAIM Contrast Checker**: https://webaim.org/resources/contrastchecker/
- **APCA Contrast Calculator**: https://www.myndex.com/APCA/

### Official Guidelines

- **WCAG 2.1**: https://www.w3.org/WAI/WCAG21/quickref/
- **Android Accessibility**: https://developer.android.com/guide/topics/ui/accessibility
- **Material Design Accessibility**: https://m3.material.io/foundations/accessible-design

### MineraLog-Specific

- **Accessibility Tests**: `app/src/androidTest/java/.../ui/accessibility/`
  - `ComposeAccessibilityTest.kt` - Screen-specific tests
  - `AutomatedAccessibilityTests.kt` - WCAG 2.1 AA automated validation suite (NEW)
- **Accessibility Spec**: `DOCS/UX_ACCESSIBILITY_IMPROVEMENTS_SPEC.md` - Full improvement roadmap (NEW)
- **Tooltip Components**: `app/src/main/java/.../ui/components/TooltipTextField.kt`
- **Example Screens**: See `AddMineralScreen.kt` for reference implementation

---

## Common Pitfalls to Avoid

### ❌ Don't Do This

```kotlin
// ❌ No content description
Icon(Icons.Default.Delete, contentDescription = null)

// ❌ Touch target too small
IconButton(
    onClick = { /* ... */ },
    modifier = Modifier.size(24.dp)  // Too small!
) { /* ... */ }

// ❌ No keyboard support
LazyColumn {
    items(minerals) { mineral ->
        Box(
            modifier = Modifier.clickable { /* ... */ }
            // ❌ No focus indication
        )
    }
}

// ❌ Color-only error indication
TextField(
    /* ... */
    colors = if (hasError) errorColors else normalColors  // ❌ Color only
)
```

### ✅ Do This Instead

```kotlin
// ✅ Descriptive content description
Icon(
    Icons.Default.Delete,
    contentDescription = "Delete ${mineral.name}"
)

// ✅ Minimum 48dp touch target
IconButton(
    onClick = { /* ... */ },
    modifier = Modifier.size(48.dp)  // ✅ Accessible size
) { /* ... */ }

// ✅ Keyboard and focus support
LazyColumn {
    items(minerals) { mineral ->
        Card(
            modifier = Modifier
                .focusable()  // ✅ Keyboard focusable
                .clickable { /* ... */ }
        ) { /* ... */ }
    }
}

// ✅ Multi-sensory error feedback
TextField(
    /* ... */
    isError = hasError,  // ✅ Visual
    supportingText = if (hasError) {
        { Text("Error message") }  // ✅ Text
    } else null,
    modifier = Modifier.semantics {
        if (hasError) {
            error("Error message")  // ✅ Semantic
            liveRegion = LiveRegionMode.Polite  // ✅ Announced
        }
    }
)
```

---

## Badge

MineraLog is **WCAG 2.1 AA Compliant**. Add this badge to README.md:

```markdown
![WCAG 2.1 AA Compliant](https://img.shields.io/badge/WCAG%202.1-AA%20Compliant-brightgreen)
```

---

## Questions or Issues?

- **GitHub Issues**: Report accessibility bugs with the `accessibility` label
- **Discussions**: Ask questions in GitHub Discussions
- **Testing**: Use TalkBack and report any navigation issues

**Remember:** Accessibility is not optional—it's essential. Every user deserves a great experience with MineraLog.

---

*Last updated: 2025-11-13*
*Contributors: Claude, MineraLog Team*
*Version: v1.6.1*
