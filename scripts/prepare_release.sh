#!/bin/bash

# MineraLog Release Asset Preparation Script
# Builds release APK, generates checksums, and creates release archive
# Usage: ./scripts/prepare_release.sh [version]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Version from argument or build.gradle.kts
if [ -n "$1" ]; then
    VERSION="$1"
else
    # Extract version from build.gradle.kts
    VERSION=$(grep 'versionName =' "$PROJECT_ROOT/app/build.gradle.kts" | sed 's/.*versionName = "\(.*\)".*/\1/')
fi

VERSION_CODE=$(grep 'versionCode =' "$PROJECT_ROOT/app/build.gradle.kts" | sed 's/.*versionCode = \(.*\)/\1/')
OUTPUT_DIR="$PROJECT_ROOT/release_assets_v${VERSION}"
BUILD_DIR="$PROJECT_ROOT/app/build/outputs"

echo "========================================"
echo "MineraLog Release Asset Preparation"
echo "========================================"
echo "Version: $VERSION (code: $VERSION_CODE)"
echo "Output: $OUTPUT_DIR"
echo "========================================"
echo ""

# Check prerequisites
echo "📋 Checking prerequisites..."

# Check if Android SDK is available
if ! command -v ./gradlew &> /dev/null; then
    echo "❌ ERROR: gradlew not found. Are you in the project root?"
    exit 1
fi

# Check if keystore is configured
if [ -z "$RELEASE_KEYSTORE_PATH" ]; then
    echo "⚠️  WARNING: RELEASE_KEYSTORE_PATH not set"
    echo "Release APK will be signed with debug keystore (not suitable for production)"
    echo "Set environment variables for production signing:"
    echo "  export RELEASE_KEYSTORE_PATH=/path/to/keystore.jks"
    echo "  export RELEASE_KEYSTORE_PASSWORD=your_password"
    echo "  export RELEASE_KEY_ALIAS=your_alias"
    echo "  export RELEASE_KEY_PASSWORD=your_key_password"
    echo ""
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "✅ Prerequisites checked"
echo ""

# Create output directory
echo "📁 Creating output directory..."
mkdir -p "$OUTPUT_DIR"
echo "✅ Output directory created: $OUTPUT_DIR"
echo ""

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean
echo "✅ Clean complete"
echo ""

# Build release APK
echo "🏗️  Building release APK..."
echo "This may take several minutes..."
./gradlew assembleRelease --console=plain

if [ $? -ne 0 ]; then
    echo "❌ ERROR: Release build failed"
    echo "Check build logs above for details"
    exit 1
fi

APK_PATH="$BUILD_DIR/apk/release/app-release.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ ERROR: Release APK not found at $APK_PATH"
    exit 1
fi

echo "✅ Release APK built successfully"
echo ""

# Copy APK to output directory
echo "📦 Copying APK to release directory..."
cp "$APK_PATH" "$OUTPUT_DIR/mineralog-v${VERSION}.apk"

APK_SIZE=$(du -h "$OUTPUT_DIR/mineralog-v${VERSION}.apk" | cut -f1)
echo "✅ APK copied: mineralog-v${VERSION}.apk ($APK_SIZE)"
echo ""

# Generate checksums
echo "🔒 Generating checksums..."
cd "$OUTPUT_DIR"

sha256sum "mineralog-v${VERSION}.apk" > SHA256SUMS.txt
md5sum "mineralog-v${VERSION}.apk" > MD5SUMS.txt

SHA256_HASH=$(cat SHA256SUMS.txt | awk '{print $1}')
MD5_HASH=$(cat MD5SUMS.txt | awk '{print $1}')

echo "✅ Checksums generated:"
echo "   SHA-256: $SHA256_HASH"
echo "   MD5:     $MD5_HASH"
echo ""

# Verify APK signature (if signed)
echo "🔍 Verifying APK signature..."
if command -v apksigner &> /dev/null; then
    apksigner verify --verbose "mineralog-v${VERSION}.apk" > signature_verification.txt 2>&1
    if [ $? -eq 0 ]; then
        echo "✅ APK signature valid"

        # Extract certificate info
        CERT_INFO=$(apksigner verify --print-certs "mineralog-v${VERSION}.apk" | head -20)
        echo "$CERT_INFO" > certificate_info.txt
        echo "Certificate info saved to certificate_info.txt"
    else
        echo "⚠️  WARNING: APK signature verification failed"
        echo "See signature_verification.txt for details"
    fi
else
    echo "⚠️  apksigner not found in PATH, skipping signature verification"
    echo "Install Android SDK Build Tools to verify signatures"
fi
echo ""

# Generate release notes
echo "📝 Generating release notes..."
cat > RELEASE_NOTES.txt <<EOF
MineraLog v${VERSION}
Release Date: $(date +%Y-%m-%d)
Version Code: ${VERSION_CODE}

======================================
Installation
======================================

1. Download mineralog-v${VERSION}.apk
2. Enable "Install from unknown sources" in Android settings:
   Settings → Security → Unknown sources (Android < 8)
   Settings → Apps → Special access → Install unknown apps (Android 8+)
3. Open the downloaded APK file and install
4. Launch MineraLog from your app drawer

======================================
Verification
======================================

Before installing, verify the APK integrity:

SHA-256: ${SHA256_HASH}
MD5:     ${MD5_HASH}

On Linux/Mac:
  sha256sum mineralog-v${VERSION}.apk

On Windows (PowerShell):
  Get-FileHash mineralog-v${VERSION}.apk -Algorithm SHA256

The output should match the SHA-256 hash above.

======================================
System Requirements
======================================

- Android 8.1+ (API 27+)
- ~15 MB storage for app
- ~100 MB storage for photos and data (recommended)
- No internet connection required

======================================
What's New in v${VERSION}
======================================

See RELEASE_NOTES_v${VERSION}.md for detailed release notes.

Key highlights:
- 300+ mineral reference library
- Support for mineral aggregates (Granite, Gneiss, etc.)
- Enhanced security (AES-256 database encryption)
- 60%+ test coverage with 1,800+ test lines
- Bilingual support (EN/FR) with 606 strings each
- Performance optimizations (93% query reduction)

======================================
Upgrading from Previous Versions
======================================

Database migration is automatic:
- v1.x, v2.x databases will be migrated to v7
- Backup your data first (recommended):
  Settings → Export → ZIP (with encryption)
- Migration preserves all data (minerals, photos, notes)

======================================
Support
======================================

- GitHub Issues: https://github.com/VBlackJack/MineraLog/issues
- Documentation: docs/SUPPORT.md
- User Guide: DOCS/user_guide.md

======================================
License
======================================

Apache License 2.0
Open source: https://github.com/VBlackJack/MineraLog

======================================
Privacy
======================================

- 100% offline (no internet permission)
- No data collection or tracking
- AES-256 encrypted database (SQLCipher)
- Your data stays on YOUR device

Happy collecting! 🔮💎⛏️
EOF

echo "✅ Release notes saved to RELEASE_NOTES.txt"
echo ""

# Generate build info
echo "🔧 Generating build info..."
cat > BUILD_INFO.txt <<EOF
MineraLog v${VERSION} Build Information
========================================

Build Date: $(date +"%Y-%m-%d %H:%M:%S %Z")
Build Host: $(hostname)
Builder: $(whoami)

Version Information:
- Version Name: ${VERSION}
- Version Code: ${VERSION_CODE}
- Min SDK: 27 (Android 8.1)
- Target SDK: 35 (Android 15)
- Compile SDK: 35

APK Information:
- File: mineralog-v${VERSION}.apk
- Size: ${APK_SIZE}
- SHA-256: ${SHA256_HASH}
- MD5: ${MD5_HASH}

Build Environment:
- Java Version: $(java -version 2>&1 | head -n 1)
- Gradle Version: $(./gradlew --version | grep Gradle | head -n 1)
- Kotlin Version: $(grep kotlin_version "$PROJECT_ROOT/gradle/libs.versions.toml" | head -n 1 || echo "N/A")

Signing:
$(if [ -n "$RELEASE_KEYSTORE_PATH" ]; then
    echo "- Signed with: $RELEASE_KEY_ALIAS"
    echo "- Keystore: $RELEASE_KEYSTORE_PATH"
else
    echo "- Signed with: debug keystore (WARNING: NOT FOR PRODUCTION)"
fi)

Build Command:
./gradlew assembleRelease

Build Success: Yes
EOF

echo "✅ Build info saved to BUILD_INFO.txt"
echo ""

# Create archive
echo "📦 Creating release archive..."
cd "$PROJECT_ROOT"
tar -czf "mineralog-v${VERSION}-release.tar.gz" "release_assets_v${VERSION}"

ARCHIVE_SIZE=$(du -h "mineralog-v${VERSION}-release.tar.gz" | cut -f1)
echo "✅ Archive created: mineralog-v${VERSION}-release.tar.gz ($ARCHIVE_SIZE)"
echo ""

# Generate final checklist
echo "📋 Release Checklist"
echo "========================================"
cat > "$OUTPUT_DIR/RELEASE_CHECKLIST.txt" <<EOF
MineraLog v${VERSION} - Release Checklist
==========================================

Pre-Release:
 [ ] All tests passing (unit + instrumentation)
 [ ] Code coverage ≥ 60%
 [ ] Security audit completed
 [ ] Manual QA test plan completed (200+ tests)
 [ ] Monkey test completed (10K events, 0 crashes)
 [ ] Documentation updated (user guide, changelog, release notes)
 [ ] Version bumped in build.gradle.kts
 [ ] CHANGELOG.md updated
 [ ] Git tag created (v${VERSION})

Build:
 [✓] Release APK built
 [✓] APK signed with $(if [ -n "$RELEASE_KEYSTORE_PATH" ]; then echo "production key"; else echo "debug key (⚠️  NOT FOR PRODUCTION)"; fi)
 [✓] Checksums generated (SHA-256, MD5)
 [ ] APK signature verified
 [ ] APK tested on real device

Release Assets:
 [✓] mineralog-v${VERSION}.apk ($APK_SIZE)
 [✓] SHA256SUMS.txt
 [✓] MD5SUMS.txt
 [✓] RELEASE_NOTES.txt
 [✓] BUILD_INFO.txt
 [ ] signature_verification.txt (if apksigner available)
 [ ] Screenshots (8 images for store listings)
 [ ] Store listings (Google Play, F-Droid)

Deployment:
 [ ] GitHub Release created with assets
 [ ] Tag pushed to GitHub (git push --tags)
 [ ] Google Play Store submission (staged rollout: 5% → 100%)
 [ ] F-Droid metadata submitted
 [ ] Release announcement on GitHub Discussions
 [ ] Social media announcements (Reddit, Twitter)
 [ ] Email beta testers

Post-Release:
 [ ] Monitor crash rate (< 1%)
 [ ] Monitor user reviews (respond within 24h)
 [ ] Monitor GitHub Issues for bugs
 [ ] Prepare hotfix if critical issues found
 [ ] Celebrate! 🎉

Notes:
- See docs/ROLLOUT_PLAN.md for staged rollout strategy
- See docs/SUPPORT.md for user support channels
- If critical issues found, rollback plan: Google Play Console → Halt rollout
EOF

cat "$OUTPUT_DIR/RELEASE_CHECKLIST.txt"
echo ""

# Summary
echo "========================================"
echo "✅ Release Assets Ready!"
echo "========================================"
echo ""
echo "Files created in: $OUTPUT_DIR/"
echo ""
echo "  - mineralog-v${VERSION}.apk ($APK_SIZE)"
echo "  - SHA256SUMS.txt"
echo "  - MD5SUMS.txt"
echo "  - RELEASE_NOTES.txt"
echo "  - BUILD_INFO.txt"
echo "  - RELEASE_CHECKLIST.txt"
if [ -f "$OUTPUT_DIR/signature_verification.txt" ]; then
    echo "  - signature_verification.txt"
fi
if [ -f "$OUTPUT_DIR/certificate_info.txt" ]; then
    echo "  - certificate_info.txt"
fi
echo ""
echo "Archive: mineralog-v${VERSION}-release.tar.gz ($ARCHIVE_SIZE)"
echo ""
echo "========================================"
echo "Next Steps:"
echo "========================================"
echo ""
echo "1. Test APK on real device:"
echo "   adb install -r $OUTPUT_DIR/mineralog-v${VERSION}.apk"
echo ""
echo "2. Create GitHub Release:"
echo "   git tag v${VERSION}"
echo "   git push origin v${VERSION}"
echo "   Upload APK and checksums to GitHub Releases"
echo ""
echo "3. Submit to stores:"
echo "   - Google Play Console: Upload APK, set staged rollout (5%)"
echo "   - F-Droid: Submit metadata to fdroiddata repo"
echo ""
echo "4. Announce release:"
echo "   - GitHub Discussions"
echo "   - Reddit (r/mineralogy, r/rockhounds)"
echo "   - Twitter, Mastodon"
echo ""
echo "See docs/ROLLOUT_PLAN.md for detailed deployment strategy."
echo ""
echo "Good luck with the release! 🚀"
