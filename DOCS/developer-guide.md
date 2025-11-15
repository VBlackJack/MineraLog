# MineraLog Developer Guide

Complete guide for setting up your development environment and building MineraLog from source.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Initial Setup](#initial-setup)
- [Building the App](#building-the-app)
- [Running Tests](#running-tests)
- [IDE Configuration](#ide-configuration)
- [Project Structure](#project-structure)
- [Debugging Tips](#debugging-tips)
- [Common Issues](#common-issues)

---

## Prerequisites

### Required Software

| Tool | Minimum Version | Download |
|------|----------------|----------|
| **Android Studio** | Ladybug (2024.2.1) or later | [developer.android.com/studio](https://developer.android.com/studio) |
| **JDK** | 17 (OpenJDK or Oracle) | Bundled with Android Studio |
| **Android SDK** | API 35 (Android 15) | Via SDK Manager |
| **Git** | 2.30+ | [git-scm.com](https://git-scm.com/) |

### Recommended Versions

- **Gradle**: 8.7+ (wrapper included, no manual install)
- **Kotlin**: 2.0.0 (configured in project)
- **Minimum SDK**: API 27 (Android 8.0 Oreo)
- **Target SDK**: API 35 (Android 15)

### Platform Requirements

- **Windows**: Windows 10/11 (64-bit)
- **macOS**: macOS 12.0+ (Intel or Apple Silicon)
- **Linux**: Ubuntu 20.04+ or equivalent

**Hardware:**
- 8 GB RAM minimum (16 GB recommended)
- 10 GB free disk space for Android Studio + SDK
- SSD strongly recommended for build performance

---

## Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/VBlackJack/MineraLog.git
cd MineraLog
```

### 2. Configure Google Maps API Key

MineraLog uses Google Maps for provenance tracking. You need an API key:

**Get API Key:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (or select existing)
3. Enable **Maps SDK for Android**
4. Navigate to **Credentials** → **Create Credentials** → **API Key**
5. Restrict key to Android apps (add your package name + SHA-1)

**Add to Project:**

Create `local.properties` in the project root:

```properties
# local.properties (DO NOT COMMIT THIS FILE)
MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

**Note**: `local.properties` is in `.gitignore` to prevent committing secrets.

**No API Key?** The app will build and run without Maps functionality (graceful degradation).

### 3. Sync Gradle

Open the project in Android Studio:

```bash
# Option 1: Command line
studio .

# Option 2: Open Android Studio → Open → Select MineraLog folder
```

Android Studio will automatically:
- Download Gradle wrapper
- Sync dependencies
- Build project indexes

**First sync takes 5-10 minutes** (downloads ~500MB dependencies).

---

## Building the App

### Command Line (Gradle)

```bash
# Debug build (fast, includes debugging info)
./gradlew assembleDebug

# Release build (optimized, ProGuard enabled)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Build + Install + Launch
./gradlew installDebug && adb shell am start -n net.meshcore.mineralog/.MainActivity
```

**Output:**
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

### Android Studio (GUI)

1. **Build Menu** → **Make Project** (⌘F9 / Ctrl+F9)
2. **Run** → **Run 'app'** (⌃R / Shift+F10)
   - Builds + installs + launches on selected device/emulator

**Build Variants:**
- **Debug**: Fast builds, logs enabled, no obfuscation
- **Release**: Optimized, ProGuard/R8 enabled, signed APK

Switch variants: **Build** → **Select Build Variant**

---

## Running Tests

### Unit Tests (JUnit 5)

Fast tests that don't require Android device:

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "*.MineralRepositoryTest"

# Run with coverage report
./gradlew testDebugUnitTest jacocoTestReport
# Report: app/build/reports/jacoco/testDebugUnitTest/html/index.html
```

**Android Studio:**
- Right-click test file → **Run 'TestClassName'**
- Green = pass, Red = fail

### Instrumented Tests (Espresso + Compose)

Require Android device or emulator:

```bash
# Run all instrumented tests
./gradlew connectedDebugAndroidTest

# Run specific test
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=net.meshcore.mineralog.ui.screens.home.HomeScreenTest
```

**Setup:**
1. Start emulator or connect physical device
2. Ensure USB debugging enabled
3. Verify connection: `adb devices`

### Lint & Code Quality

```bash
# Run Android Lint
./gradlew lint
# Report: app/build/reports/lint-results-debug.html

# Check code style (Detekt - if configured)
./gradlew detekt
```

---

## IDE Configuration

### Android Studio Settings

**Recommended Plugins:**
- **Kotlin** (pre-installed)
- **Compose Multipreview** (preview multiple screen states)
- **Rainbow Brackets** (code readability)
- **GitToolBox** (git blame inline)

**Code Style:**

File → Settings → Editor → Code Style → Kotlin

- **Indent**: 4 spaces
- **Continuation indent**: 4 spaces
- **Tab size**: 4
- **Use tab character**: ❌ Unchecked
- **Right margin**: 120 columns

Import code style: Use `.editorconfig` in project root (auto-detected).

### Useful Shortcuts

| Action | Windows/Linux | macOS |
|--------|---------------|-------|
| Build project | Ctrl+F9 | ⌘F9 |
| Run app | Shift+F10 | ⌃R |
| Debug app | Shift+F9 | ⌃D |
| Run tests | Ctrl+Shift+F10 | ⌃⇧R |
| Find file | Ctrl+Shift+N | ⌘⇧O |
| Search everywhere | Double Shift | Double Shift |
| Refactor → Rename | Shift+F6 | ⇧F6 |
| Optimize imports | Ctrl+Alt+O | ⌃⌥O |

---

## Project Structure

```
MineraLog/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/net/meshcore/mineralog/
│   │   │   │   ├── data/           # Data layer (Room, repositories, services)
│   │   │   │   │   ├── local/      # Room database, DAOs, entities
│   │   │   │   │   ├── model/      # Domain models
│   │   │   │   │   ├── repository/ # Repository pattern
│   │   │   │   │   ├── service/    # Business logic (backup, CSV, crypto)
│   │   │   │   │   └── util/       # Data utilities (CSV parser, migrations)
│   │   │   │   ├── ui/             # Presentation layer
│   │   │   │   │   ├── screens/    # Full screens (Home, Add, Edit, etc.)
│   │   │   │   │   ├── components/ # Reusable UI components
│   │   │   │   │   ├── navigation/ # Navigation graph
│   │   │   │   │   └── theme/      # Material 3 theme
│   │   │   │   ├── util/           # App utilities, extensions
│   │   │   │   └── MainActivity.kt # App entry point
│   │   │   ├── res/
│   │   │   │   ├── values/         # Strings, colors, themes (English)
│   │   │   │   ├── values-fr/      # French translations
│   │   │   │   ├── mipmap-*/       # App icons
│   │   │   │   └── drawable/       # Vector icons, images
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                   # Unit tests (JUnit 5)
│   │   └── androidTest/            # Instrumented tests (Espresso)
│   ├── build.gradle.kts            # App module build config
│   └── proguard-rules.pro          # ProGuard/R8 rules
├── build.gradle.kts                # Root build config
├── gradle/libs.versions.toml       # Version catalog (dependencies)
├── settings.gradle.kts             # Project settings
├── DOCS/                           # Documentation
├── tools/                          # Scripts and utilities
└── README.md
```

### Key Files

- **`app/build.gradle.kts`**: App configuration (dependencies, SDK versions, build types)
- **`gradle/libs.versions.toml`**: Centralized dependency versions
- **`app/src/main/java/.../data/local/MineraLogDatabase.kt`**: Room database definition
- **`app/src/main/java/.../ui/navigation/MineraLogNavHost.kt`**: Compose navigation
- **`app/src/main/res/values/strings.xml`**: String resources (i18n)

---

## Debugging Tips

### Logcat Filtering

**Android Studio Logcat:**
- Filter by package: `package:net.meshcore.mineralog`
- Filter by tag: `tag:MineralRepo`
- Filter by level: Select **Warn** or **Error** dropdown

**Command Line:**
```bash
# Real-time logs
adb logcat -s MineralRepo:D AndroidRuntime:E

# Clear and monitor
adb logcat -c && adb logcat | grep "mineralog"

# Save to file
adb logcat > logcat.txt
```

### Breakpoints

- **Line breakpoint**: Click gutter next to line number
- **Conditional breakpoint**: Right-click breakpoint → Add condition
  - Example: `mineral.name == "Quartz"`
- **Logpoint**: Right-click breakpoint → **More** → Check "Log evaluated expression"

### Compose Preview

Use `@Preview` annotation to preview composables without running app:

```kotlin
@Preview(showBackground = true)
@Composable
fun MineralCardPreview() {
    MineraLogTheme {
        MineralCard(
            mineral = TestFixtures.createMineral(name = "Quartz"),
            onClick = {}
        )
    }
}
```

**Refresh preview**: Build → Refresh Previews

### Database Inspection

**Android Studio Database Inspector:**
1. Run app on emulator/device (API 26+)
2. **View** → **Tool Windows** → **App Inspection**
3. Select **Database Inspector** tab
4. Query Room database live

**Manual inspection:**
```bash
# Pull database from device
adb pull /data/data/net.meshcore.mineralog/databases/mineralog_database.db

# Open with SQLite browser
sqlite3 mineralog_database.db
.tables
SELECT * FROM minerals LIMIT 10;
```

---

## Common Issues

### Issue: Gradle Sync Failed

**Error**: `Could not resolve com.google.android.material:material:X.X.X`

**Solution:**
1. Check internet connection
2. Invalidate caches: **File** → **Invalidate Caches** → **Invalidate and Restart**
3. Delete `.gradle` folder and re-sync

### Issue: MAPS_API_KEY Not Found

**Error**: `Missing MAPS_API_KEY in local.properties`

**Solution:**
- Create `local.properties` with API key (see [Initial Setup](#2-configure-google-maps-api-key))
- Or comment out Maps dependency in `build.gradle.kts` (app will skip Maps)

### Issue: Tests Fail with "Cannot find Room schema"

**Error**: `java.io.FileNotFoundException: Missing schema file`

**Solution:**
```bash
# Generate schema files
./gradlew assembleDebug

# Or update Room annotation processor config in build.gradle.kts
kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}
```

### Issue: App Crashes on Startup (Release Build)

**Error**: `ClassNotFoundException` or `MethodNotFoundException`

**Solution**: ProGuard/R8 is removing needed classes. Update `proguard-rules.pro`:

```proguard
# Keep Room entities
-keep class net.meshcore.mineralog.data.model.** { *; }

# Keep kotlinx.serialization
-keep @kotlinx.serialization.Serializable class * { *; }
```

### Issue: Emulator Slow/Laggy

**Solution:**
1. Use **x86_64** system image (not ARM)
2. Enable hardware acceleration:
   - **Windows**: Intel HAXM or Windows Hypervisor Platform (WHPX)
   - **macOS**: Hypervisor.framework (automatic)
   - **Linux**: KVM
3. Allocate more RAM to emulator (4GB+)
4. Use **Cold Boot** instead of saved snapshots

---

## Next Steps

- **Read Architecture Docs**: [ARCHITECTURE.md](../ARCHITECTURE.md)
- **Review Code Style**: [CONTRIBUTING.md](../CONTRIBUTING.md#code-style)
- **Run Manual Tests**: [DOCS/qa/manual-testing-guide.md](qa/manual-testing-guide.md)
- **Explore Codebase**: Start with `MainActivity.kt` → `MineraLogNavHost.kt` → Screens

---

## Need Help?

- **Build errors**: Check [Common Issues](#common-issues) above
- **Questions**: [GitHub Discussions](https://github.com/VBlackJack/MineraLog/discussions)
- **Bugs**: [GitHub Issues](https://github.com/VBlackJack/MineraLog/issues)

**Happy coding! 🪨⛏️**
