# MineraLog - Support & Communication

**Version**: 3.0.0
**Last Updated**: 2025-11-16

---

## For Users

### Getting Help

If you're experiencing issues or have questions about MineraLog, here are your support options:

---

### 📚 Documentation

Start here for common questions and how-to guides:

1. **User Guide** (Comprehensive)
   - Location: `DOCS/user_guide.md`
   - Topics: Adding minerals, photos, import/export, search, QR codes
   - Languages: English, Français (partial)

2. **CSV Import/Export Format**
   - Location: `docs/CSV_FORMAT.md`
   - Topics: Column mapping, data types, import troubleshooting

3. **Release Notes**
   - Location: `RELEASE_NOTES_v3.0.0.md`
   - Topics: What's new, known issues, migration notes

4. **Changelog** (Technical)
   - Location: `CHANGELOG.md`
   - Topics: Detailed version history, bug fixes, technical changes

---

### 🐛 Bug Reports

Found a problem? Report it on GitHub Issues.

**GitHub Issues**: [https://github.com/VBlackJack/MineraLog/issues](https://github.com/VBlackJack/MineraLog/issues)

**Before reporting**:
1. Search existing issues to avoid duplicates
2. Update to the latest version (Settings → About)
3. Check if issue is already fixed in release notes

**When reporting**, include:
- **App version**: Settings → About → Version
- **Android version**: e.g., Android 13
- **Device model**: e.g., Pixel 6, Samsung S21
- **Steps to reproduce**:
  1. Step one
  2. Step two
  3. Expected result vs. actual result
- **Screenshots**: If applicable
- **Logs**: If you have crash logs (optional)

**Template**: GitHub will auto-load our bug report template with these fields.

**Response Time**: < 48 hours average (usually same day for P0 bugs)

---

### 💡 Feature Requests

Have an idea for improving MineraLog? We'd love to hear it!

**GitHub Discussions**: [https://github.com/VBlackJack/MineraLog/discussions](https://github.com/VBlackJack/MineraLog/discussions)

**Categories**:
- **Ideas**: New feature suggestions
- **Show and Tell**: Share your mineral collection, workflows, tips
- **Q&A**: Questions about using the app

**Voting**:
- Upvote existing feature requests with 👍 emoji
- Most-requested features get prioritized in roadmap

**Roadmap**:
- Public roadmap on GitHub Projects (coming soon)
- See `docs/ROADMAP_REFERENCE_LIBRARY.md` for current plans

---

### 🗺️ Public Roadmap

**Planned Features** (v3.1.0+):
- Hilt dependency injection migration
- Composable refactoring (performance)
- Crash reporting (opt-in, privacy-first)
- Cloud sync (optional, encrypted end-to-end)
- Widget for home screen
- PDF export for collection reports
- Advanced collection statistics

**Long-term** (v4.0+):
- Multi-user collections (family sharing)
- Barcode scanning for cataloging books/labels
- Integration with Mindat.org API
- AR visualization (view mineral in 3D)

**Vote on features**: GitHub Discussions → Ideas category

---

### 💬 Community

Connect with other MineraLog users and mineral collectors:

#### Reddit
- **r/mineralogy**: Tag posts with `[MineraLog]`
- **r/rockhounds**: Share collection photos
- **r/android**: For technical Android discussions

**Note**: We're not official moderators, but we monitor these communities.

#### Discord (Planned)
- **Status**: Not yet created
- **Threshold**: Will create if > 500 active users request it
- **Purpose**: Real-time chat, collection sharing, beta testing coordination

#### Social Media
- **Twitter**: [@MineraLogApp](https://twitter.com/MineraLogApp) (coming soon)
- **Mastodon**: @mineralog@fosstodon.org (coming soon)

---

### 📧 Email Support

**Email**: support@mineralog.app (coming soon)

**Note**: Email support is not yet active. Please use GitHub Issues for now.

**When available**:
- Response time: 2-3 business days
- For private issues (account, billing - future paid features)
- For security vulnerability reports (see Security Policy below)

---

### 🔒 Security Vulnerabilities

**DO NOT report security vulnerabilities via public GitHub Issues.**

**Instead**:
1. **Email**: security@mineralog.app (coming soon)
2. **GitHub Security Advisories**: [Private vulnerability reporting](https://github.com/VBlackJack/MineraLog/security/advisories)

**Response Time**: < 24 hours for critical vulnerabilities

**Responsible Disclosure**:
- We follow a 90-day disclosure timeline
- We'll credit you in release notes (unless you prefer anonymity)
- See `SECURITY.md` for full policy (coming soon)

---

## For Developers

### Contributing

We welcome contributions from the community!

**Getting Started**:
1. Read `CONTRIBUTING.md` (comprehensive contribution guide)
2. Check `docs/DEVELOPMENT.md` for build instructions
3. Review `docs/ARCHITECTURE.md` to understand codebase structure
4. Pick an issue labeled `good-first-issue` or `help-wanted`

**Code of Conduct**:
- Be respectful and inclusive
- No harassment, discrimination, or trolling
- Assume good intent
- Full Code of Conduct: `CODE_OF_CONDUCT.md` (coming soon)

---

### Building from Source

**Prerequisites**:
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 35
- Git

**Steps**:
```bash
# Clone repository
git clone https://github.com/VBlackJack/MineraLog.git
cd MineraLog

# Create local.properties for Google Maps API key (optional)
echo "MAPS_API_KEY=your_api_key_here" > local.properties
# Or use dummy key:
echo "MAPS_API_KEY=dummy" > local.properties

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest

# Generate coverage report
./gradlew jacocoTestReport
```

**APK Output**: `app/build/outputs/apk/debug/app-debug.apk`

**Documentation**: See `docs/DEVELOPMENT.md` for detailed instructions

---

### Pull Requests

**Workflow**:
1. **Fork** the repository
2. **Create branch**: `git checkout -b feature/your-feature-name`
3. **Commit changes**: Use [Conventional Commits](https://www.conventionalcommits.org/)
   - `feat: add support for mineral aggregates`
   - `fix: resolve crash on Android 8.1`
   - `docs: update user guide with QR code instructions`
   - `test: add unit tests for CsvParser`
4. **Push to fork**: `git push origin feature/your-feature-name`
5. **Open Pull Request**: Against `main` branch
6. **Wait for review**: Maintainers will review within 3-5 days

**PR Requirements**:
- [ ] **CI passes**: All tests, lint, detekt checks must pass
- [ ] **Code coverage**: Overall coverage ≥ 60%, ViewModels ≥ 70%
- [ ] **Tests included**: New features require unit tests
- [ ] **Documentation updated**: Update user guide, changelog if applicable
- [ ] **Conventional commits**: Follow commit message format
- [ ] **Squash commits**: Squash before merge (maintainers will do this if needed)

**Review Process**:
- 1 maintainer approval required
- Automated checks (CI, coverage, lint) must pass
- Code review focuses on: correctness, performance, security, maintainability
- Average merge time: 3-7 days

---

### Architecture & Design

**Key Documents**:
- **Architecture**: `docs/ARCHITECTURE.md`
  - Clean Architecture (Data, Domain, Presentation layers)
  - MVVM with StateFlow
  - Room + SQLCipher for encrypted database
  - Dependency injection (manual → Hilt migration planned)

- **Development Guide**: `docs/DEVELOPMENT.md`
  - Testing strategy (JUnit 5, MockK, Turbine, Espresso)
  - Security practices (Argon2id, AES-256-GCM, SQLCipher)
  - API documentation for DAOs and Repositories

- **Import/Export Spec**: `docs/specs/import_export_spec.md`
  - ZIP and CSV format specifications
  - Column mapping algorithm (Levenshtein distance)
  - Encryption details

**Code Style**:
- **Kotlin**: Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Compose**: Use Material 3 guidelines
- **Detekt**: Enforced in CI, max issues = 0
- **Lint**: Android Lint checks enforced
- **Max line length**: 120 characters
- **Max function complexity**: 15 (Detekt rule)

---

### Testing

**Test Coverage Requirements**:
- **Global**: ≥ 60%
- **ViewModels**: ≥ 70%
- **Repositories**: ≥ 65%
- **DAOs**: 100% (simple CRUD, easy to test)
- **Crypto modules**: ≥ 95%

**Test Types**:
1. **Unit Tests** (`src/test/`):
   - JUnit 5 + MockK for mocking
   - Turbine for Flow testing
   - Robolectric for Android framework

2. **Instrumentation Tests** (`src/androidTest/`):
   - Espresso for UI testing
   - Compose UI Testing for Jetpack Compose
   - On-device database testing

**Running Tests**:
```bash
# All unit tests
./gradlew testDebugUnitTest

# All instrumentation tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Coverage report (HTML + XML)
./gradlew jacocoTestReport
# Open: app/build/reports/jacoco/html/index.html

# Coverage verification (enforces thresholds)
./gradlew jacocoTestCoverageVerification
```

---

### Release Process

**Versioning**:
- Follow [Semantic Versioning](https://semver.org/)
- Format: `MAJOR.MINOR.PATCH` (e.g., 3.0.0)
- Version code increments by 1 for each release

**Release Workflow**:
1. **Create release branch**: `release/v3.0.0`
2. **Update version**: `app/build.gradle.kts`
   - `versionName = "3.0.0"`
   - `versionCode = 30`
3. **Update changelog**: `CHANGELOG.md`
4. **Update release notes**: `RELEASE_NOTES_v3.0.0.md`
5. **Run full test suite**: Manual + automated tests
6. **Build release APK**: `./gradlew assembleRelease`
7. **Sign APK**: With production keystore
8. **Tag release**: `git tag v3.0.0 && git push --tags`
9. **GitHub Release**: Upload APK + checksums + release notes
10. **Google Play**: Upload to production track (staged rollout)
11. **F-Droid**: Submit merge request to fdroiddata repo

**See**: `docs/ROLLOUT_PLAN.md` for detailed staged rollout process

---

## Support Channels Summary

| Channel | Purpose | Response Time | Audience |
|---------|---------|---------------|----------|
| **GitHub Issues** | Bug reports | < 48h | Users + Developers |
| **GitHub Discussions** | Feature requests, Q&A | < 72h | Users + Developers |
| **Reddit** | Community support | Varies | Users |
| **Email** | Private issues (future) | 2-3 days | Users |
| **Security Email** | Vulnerability reports | < 24h (P0) | Security researchers |
| **Pull Requests** | Code contributions | 3-5 days | Developers |

---

## FAQs

### General

**Q: Is MineraLog free?**
A: Yes, 100% free and open source (Apache License 2.0). No ads, no tracking, no in-app purchases.

**Q: Is my data safe?**
A: Yes. All data stored locally on your device. Database encrypted with AES-256 (SQLCipher). Backups protected with Argon2id + AES-256-GCM.

**Q: Does MineraLog collect any data?**
A: No. Zero telemetry, zero tracking. App doesn't require internet permission. Completely offline.

**Q: Can I sync across devices?**
A: Not yet (v3.0). Planned for v3.1+ with optional encrypted cloud sync.

---

### Technical

**Q: What Android versions are supported?**
A: Android 8.1+ (API 27+). Tested up to Android 15.

**Q: How do I backup my data?**
A: Settings → Export → ZIP. Choose a strong password for encryption.

**Q: How do I import from another app?**
A: Export your data to CSV, then use Settings → Import → CSV. Map columns to MineraLog fields.

**Q: Can I use MineraLog on tablets?**
A: Yes! Optimized for phones and tablets (7-10" screens).

**Q: Does MineraLog work offline?**
A: Yes, 100% offline. Maps feature (optional) requires internet for tile downloads.

---

### Troubleshooting

**Q: App crashes on launch**
A:
1. Clear app cache: Settings → Apps → MineraLog → Clear Cache
2. Ensure Android 8.1+ (minSdk 27)
3. Free up device storage (> 100 MB required)
4. Report bug with crash log

**Q: Import CSV fails**
A:
1. Ensure UTF-8 encoding
2. Use provided CSV template (Export → empty collection)
3. Check error log for line-specific issues
4. See `docs/CSV_FORMAT.md` for format details

**Q: Photos missing after restore**
A:
1. Backups don't include photos (file size)
2. Export photos separately: Settings → Export Photos
3. Restore photos to `MineraLog/Photos/` directory
4. Then import backup ZIP

**Q: QR codes not scanning**
A:
1. Ensure camera permission granted
2. Use good lighting
3. Hold phone 15-30cm from QR code
4. QR code must be generated by MineraLog (format: `mineralapp://mineral/{uuid}`)

**Q: Database migration failed**
A:
1. Backup your data first (Export ZIP)
2. Clear app data
3. Reinstall app
4. Import backup
5. Report issue with version numbers (old → new)

---

## Contact Information

**Website**: https://mineralog.app (coming soon)

**GitHub**: https://github.com/VBlackJack/MineraLog

**Email**:
- Support: support@mineralog.app (coming soon)
- Security: security@mineralog.app (coming soon)
- General: contact@mineralog.app (coming soon)

**Social Media**:
- Twitter: [@MineraLogApp](https://twitter.com/MineraLogApp) (coming soon)
- Mastodon: @mineralog@fosstodon.org (coming soon)

---

## Acknowledgments

MineraLog is built with love by:

**Core Team**:
- [Developer names]

**Contributors**:
- See [GitHub Contributors](https://github.com/VBlackJack/MineraLog/graphs/contributors)

**Special Thanks**:
- r/mineralogy community for feature feedback
- Beta testers who helped polish v3.0.0
- Open source libraries: Room, Compose, SQLCipher, Argon2, ZXing, ML Kit, Coil

**Sponsors**:
- [List sponsors if applicable]
- Consider sponsoring: [GitHub Sponsors link] (coming soon)

---

**Last Updated**: 2025-11-16
**Document Version**: 1.0
**For Version**: MineraLog 3.0.0
