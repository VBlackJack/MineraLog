# MineraLog v3.0.0 - Security Audit Checklist (RC)

**Version**: 3.0.0-rc
**Date**: 2025-11-16
**Auditor**: [Name]
**Status**: ⬜ In Progress | ⬜ Completed

---

## Overview

This security audit validates that all P0 (critical) and P1 (high-priority) security fixes from v1.6.0 are still active and no regressions have occurred.

**Reference**: See CHANGELOG.md § [1.6.0] for original security fixes.

---

## 1. Database Security (SQLCipher)

**Objective**: Verify database is encrypted at rest with AES-256.

### 1.1 Database Encryption

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| DB-1 | Database file exists | `/data/data/net.meshcore.mineralog/databases/mineralog_database` | ⬜ | | |
| DB-2 | Database is encrypted | File header is NOT `SQLite format 3` | ⬜ | | Use `file` command or `xxd` |
| DB-3 | No plaintext database | No `.db` files with plaintext header | ⬜ | | Check all DB files |
| DB-4 | SQLCipher version | v4.5.4+ | ⬜ | | Check dependencies |
| DB-5 | AES-256 cipher | PRAGMA cipher = 'aes-256-cbc' | ⬜ | | Check DB config |

**Validation Command**:
```bash
# Pull database file (requires rooted device or debuggable build)
adb shell "run-as net.meshcore.mineralog cat /data/data/net.meshcore.mineralog/databases/mineralog_database" > test_db.db

# Check file header (should NOT say "SQLite format 3")
file test_db.db
xxd test_db.db | head -n 5

# If plaintext, you'll see:
# 00000000: 5351 4c69 7465 2066 6f72 6d61 7420 3300  SQLite format 3.

# If encrypted, you'll see random bytes:
# 00000000: a7f3 9c2d 8b1e 4f6a c8d9 2e5b 7a3c 1f90  ...-..Oj...[z<..
```

**Expected Result**: ✅ Database file is encrypted (random bytes, not SQLite header)

---

### 1.2 Database Key Management

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| DK-1 | Key stored in Android Keystore | `DatabaseKeyManager` uses `EncryptedSharedPreferences` | ⬜ | | Check source code |
| DK-2 | Master key in Keystore | `MasterKey` created with `KeyGenParameterSpec` | ⬜ | | Check `DatabaseKeyManager.kt` |
| DK-3 | Hardware-backed storage | Keystore uses TEE/StrongBox when available | ⬜ | | Device-dependent |
| DK-4 | Passphrase rotation | `DatabaseKeyManager.rotateKey()` exists | ⬜ | | Check API |

**Files to Review**:
- `app/src/main/java/net/meshcore/mineralog/data/crypto/DatabaseKeyManager.kt`
- `app/src/main/java/net/meshcore/mineralog/data/local/MineraLogDatabase.kt`

---

## 2. Backup Encryption (Argon2id + AES-256-GCM)

**Objective**: Verify backups are encrypted with strong KDF.

### 2.1 Argon2id Key Derivation

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| AR-1 | Argon2 library | `argon2-jvm` v13.1+ | ⬜ | | Check `libs.versions.toml` |
| AR-2 | Argon2id mode | `Argon2Factory.createAdvanced(Argon2Types.ARGON2id)` | ⬜ | | Not Argon2i or Argon2d |
| AR-3 | Memory cost (m) | m=65536 (64 MiB) | ⬜ | | Check `Argon2Helper.kt` |
| AR-4 | Time cost (t) | t=3 iterations | ⬜ | | |
| AR-5 | Parallelism (p) | p=2 threads | ⬜ | | |
| AR-6 | Salt generation | 16-byte random salt via `SecureRandom` | ⬜ | | Unique per backup |
| AR-7 | Hash output | 32-byte (256-bit) key | ⬜ | | Matches AES-256 key size |
| AR-8 | No hardcoded passwords | No default passwords in code | ⬜ | | grep for "password =" |

**Validation Command**:
```bash
# Search for Argon2 configuration
grep -r "Argon2Parameters" app/src/main/java/
grep -r "mCostInKibibyte\|tCost\|parallelism" app/src/main/java/
```

**Files to Review**:
- `app/src/main/java/net/meshcore/mineralog/data/crypto/Argon2Helper.kt`
- `app/src/main/java/net/meshcore/mineralog/data/crypto/PasswordBasedCrypto.kt`

---

### 2.2 AES-256-GCM Encryption

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| AES-1 | Cipher algorithm | AES/GCM/NoPadding | ⬜ | | Check `CryptoHelper.kt` |
| AES-2 | Key size | 256-bit | ⬜ | | AES-256 |
| AES-3 | GCM mode | Authenticated encryption | ⬜ | | Not CBC or ECB |
| AES-4 | IV generation | 12-byte random IV via `SecureRandom` | ⬜ | | Unique per encryption |
| AES-5 | IV storage | IV prepended to ciphertext | ⬜ | | First 12 bytes |
| AES-6 | Authentication tag | 128-bit tag (default GCM) | ⬜ | | Integrity protection |
| AES-7 | No key reuse | Unique key per backup | ⬜ | | Via Argon2 with unique salt |

**Validation Command**:
```bash
# Check AES configuration
grep -r "Cipher.getInstance" app/src/main/java/ | grep -i "aes"
grep -r "GCMParameterSpec" app/src/main/java/
```

**Files to Review**:
- `app/src/main/java/net/meshcore/mineralog/data/crypto/CryptoHelper.kt`

---

### 2.3 Backup Encryption Integration

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| BK-1 | Password entry dialog | `EncryptPasswordDialog` exists | ⬜ | | Check UI |
| BK-2 | Password strength indicator | Weak/Medium/Strong shown | ⬜ | | User feedback |
| BK-3 | Password confirmation | Confirm password matches | ⬜ | | Prevents typos |
| BK-4 | Show/hide password toggle | Eye icon toggles visibility | ⬜ | | UX feature |
| BK-5 | Decrypt password dialog | `DecryptPasswordDialog` exists | ⬜ | | For restore |
| BK-6 | Attempt counter | Max 3 password attempts | ⬜ | | Brute-force mitigation |
| BK-7 | Memory clearing | `CharArray.fill('\u0000')` after use | ⬜ | | Prevents memory dumps |
| BK-8 | No password logging | No passwords in LogCat | ⬜ | | Check logs |

**Files to Review**:
- `app/src/main/java/net/meshcore/mineralog/ui/components/EncryptPasswordDialog.kt`
- `app/src/main/java/net/meshcore/mineralog/ui/components/DecryptPasswordDialog.kt`

---

## 3. Input Validation & Injection Prevention

**Objective**: Verify all user inputs are sanitized.

### 3.1 CSV Injection Protection

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| CSV-1 | Formula escaping | Leading `=` escaped to `'=` | ⬜ | | Check `MineralCsvMapper.escapeCSV()` |
| CSV-2 | Plus escaping | Leading `+` escaped to `'+` | ⬜ | | |
| CSV-3 | Minus escaping | Leading `-` escaped to `'-` | ⬜ | | |
| CSV-4 | At escaping | Leading `@` escaped to `'@` | ⬜ | | |
| CSV-5 | Tab escaping | Leading `\t` escaped | ⬜ | | |
| CSV-6 | CR escaping | Leading `\r` escaped | ⬜ | | |
| CSV-7 | Quote escaping | `"` escaped to `""` (RFC 4180) | ⬜ | | Standard CSV |
| CSV-8 | Comma escaping | Fields with commas quoted | ⬜ | | Standard CSV |
| CSV-9 | Test DDE attack | Formula `=1+1` exported as `'=1+1` | ⬜ | | Manual test |
| CSV-10 | Test HYPERLINK | `=HYPERLINK(...)` escaped | ⬜ | | Manual test |

**Validation Command**:
```bash
# Search for CSV escaping logic
grep -A 10 "fun escapeCSV" app/src/main/java/net/meshcore/mineralog/data/
```

**Manual Test**:
1. Create mineral with formula `=SUM(A1:A10)`
2. Export to CSV
3. Open CSV in text editor (NOT Excel)
4. Verify formula is `'=SUM(A1:A10)` (leading quote)

**Files to Review**:
- `app/src/main/java/net/meshcore/mineralog/data/mapper/MineralCsvMapper.kt`
- `app/src/test/java/net/meshcore/mineralog/data/service/CsvInjectionProtectionTest.kt`

---

### 3.2 SQL Injection Protection

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| SQL-1 | Parameterized queries | All DAO methods use `@Query` with `:param` | ⬜ | | Room framework |
| SQL-2 | No string concatenation | No `"SELECT * FROM " + table` | ⬜ | | Search codebase |
| SQL-3 | No raw SQL | No `database.execSQL()` with user input | ⬜ | | |
| SQL-4 | Deep link validation | UUID format validated (regex) | ⬜ | | Check `DeepLinkValidationTest.kt` |

**Validation Command**:
```bash
# Search for potential SQL injection
grep -r "execSQL\|rawQuery" app/src/main/java/net/meshcore/mineralog/data/ | grep -v ".kt:"
```

**Files to Review**:
- All DAO files in `app/src/main/java/net/meshcore/mineralog/data/local/dao/`

---

### 3.3 Deep Link Validation

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| DL-1 | UUID regex validation | `^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$` | ⬜ | | |
| DL-2 | MainActivity validation | First layer validates before nav | ⬜ | | `MainActivity.kt:32-42` |
| DL-3 | NavHost validation | Second layer validates in nav graph | ⬜ | | `MineraLogNavHost.kt:68-76` |
| DL-4 | SQL injection attempt blocked | `'; DROP TABLE--` rejected | ⬜ | | Test case |
| DL-5 | Path traversal blocked | `../../etc/passwd` rejected | ⬜ | | Test case |
| DL-6 | XSS attempt blocked | `<script>alert('xss')</script>` rejected | ⬜ | | Test case |
| DL-7 | Command injection blocked | `$(whoami)` rejected | ⬜ | | Test case |

**Files to Review**:
- `app/src/main/java/net/meshcore/mineralog/ui/MainActivity.kt`
- `app/src/main/java/net/meshcore/mineralog/ui/navigation/MineraLogNavHost.kt`
- `app/src/test/java/net/meshcore/mineralog/ui/DeepLinkValidationTest.kt`

---

## 4. Android Platform Security

**Objective**: Verify Android security best practices.

### 4.1 Manifest Security

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| MF-1 | Backup disabled | `android:allowBackup="false"` | ⬜ | | Check `AndroidManifest.xml` |
| MF-2 | Debug disabled in release | `android:debuggable="false"` (default) | ⬜ | | Not explicitly set |
| MF-3 | Export flags | Activities/Services use `android:exported` explicitly | ⬜ | | Android 12+ requirement |
| MF-4 | Deep link validation | `android:autoVerify="true"` for App Links | ⬜ | | Optional but recommended |
| MF-5 | Min SDK | `minSdk = 27` (Android 8.1) | ⬜ | | Check `build.gradle.kts` |
| MF-6 | Target SDK | `targetSdk = 35` (Android 15) | ⬜ | | Latest |

**Validation Command**:
```bash
# Check manifest
grep "allowBackup\|debuggable\|exported" app/src/main/AndroidManifest.xml
```

**Files to Review**:
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`

---

### 4.2 Network Security Config

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| NS-1 | Network config file exists | `res/xml/network_security_config.xml` | ⬜ | | |
| NS-2 | Cleartext traffic blocked | `cleartextTrafficPermitted="false"` | ⬜ | | No HTTP, only HTTPS |
| NS-3 | Manifest reference | `android:networkSecurityConfig="@xml/network_security_config"` | ⬜ | | |
| NS-4 | No debug domains | No `<debug-overrides>` in production | ⬜ | | |

**Validation Command**:
```bash
# Check network config
cat app/src/main/res/xml/network_security_config.xml
```

**Files to Review**:
- `app/src/main/res/xml/network_security_config.xml`

---

### 4.3 Permissions

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| PM-1 | Minimum permissions | Only CAMERA (optional) + STORAGE (scoped) | ⬜ | | No unnecessary perms |
| PM-2 | No internet permission | `INTERNET` NOT in manifest | ⬜ | | Offline-first |
| PM-3 | No location permission | `ACCESS_FINE_LOCATION` NOT in manifest (maps optional) | ⬜ | | |
| PM-4 | Camera permission optional | App works without camera (no photos) | ⬜ | | Graceful degradation |
| PM-5 | Storage scoped (API 29+) | Uses `MediaStore` or app-specific dirs | ⬜ | | No `READ_EXTERNAL_STORAGE` |

**Validation Command**:
```bash
# List all permissions
grep "<uses-permission" app/src/main/AndroidManifest.xml
```

---

## 5. Code Obfuscation & Hardening

**Objective**: Verify release builds are obfuscated.

### 5.1 ProGuard/R8 Configuration

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| PG-1 | R8 enabled | `isMinifyEnabled = true` (release) | ⬜ | | Check `build.gradle.kts` |
| PG-2 | Obfuscation enabled | R8 default obfuscation active | ⬜ | | |
| PG-3 | Shrinking enabled | `isShrinkResources = true` | ⬜ | | Removes unused code |
| PG-4 | ProGuard rules file | `proguard-rules.pro` exists | ⬜ | | |
| PG-5 | Keep rules specific | No wildcard `**` rules | ⬜ | | Check refactored rules |
| PG-6 | Debug logs removed | `-assumenosideeffects` for `Log.d/v/i` | ⬜ | | No sensitive logs |
| PG-7 | Room protected | DAO/Entity classes kept | ⬜ | | |
| PG-8 | Compose protected | `@Composable` methods kept | ⬜ | | |
| PG-9 | ViewModel factories protected | `ViewModelProvider.Factory` kept | ⬜ | | Reflection-based |

**Validation Command**:
```bash
# Check ProGuard config
grep "isMinifyEnabled\|isShrinkResources" app/build.gradle.kts
wc -l app/proguard-rules.pro
grep -c "\*\*" app/proguard-rules.pro  # Should be 0 or very few
```

**Files to Review**:
- `app/build.gradle.kts`
- `app/proguard-rules.pro`

---

### 5.2 APK Analysis

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| APK-1 | APK signed | Release APK signed with production key | ⬜ | | Check signature |
| APK-2 | APK alignment | `zipalign` applied | ⬜ | | Build process |
| APK-3 | APK size | < 20 MB | ⬜ | | Check file size |
| APK-4 | No debug info | `BuildConfig.DEBUG = false` | ⬜ | | Check decompiled APK |
| APK-5 | Classes obfuscated | Class names like `a.b.c` not `MineralRepository` | ⬜ | | Decompile APK |

**Validation Command**:
```bash
# Check APK signature
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk

# Check APK size
du -h app/build/outputs/apk/release/app-release.apk

# Decompile APK (requires jadx or similar)
jadx -d output/ app/build/outputs/apk/release/app-release.apk
grep -r "class MineralRepository" output/  # Should NOT find readable names
```

---

## 6. Secure Data Handling

**Objective**: Verify sensitive data is handled securely.

### 6.1 Clipboard Management

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| CB-1 | SecureClipboard utility exists | `util/SecureClipboard.kt` | ⬜ | | |
| CB-2 | Auto-clear timeout | 30 seconds | ⬜ | | Check `CLIPBOARD_CLEAR_DELAY_MS` |
| CB-3 | Coroutine cleanup | Cancellation support on dispose | ⬜ | | |
| CB-4 | Used for sensitive data | Import errors, mineral IDs use secure copy | ⬜ | | Check `ImportResultDialog` |

**Files to Review**:
- `app/src/main/java/net/meshcore/mineralog/util/SecureClipboard.kt`
- `app/src/main/java/net/meshcore/mineralog/ui/components/ImportResultDialog.kt`

---

### 6.2 Logging

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| LOG-1 | No passwords logged | grep "password" logs → 0 results | ⬜ | | |
| LOG-2 | No sensitive PII | No names, emails, locations in logs | ⬜ | | |
| LOG-3 | Log level release | Release builds strip `Log.d/v/i` | ⬜ | | ProGuard rules |
| LOG-4 | Error logs sanitized | Stack traces don't leak sensitive data | ⬜ | | |

**Validation Command**:
```bash
# Search for potential logging issues
grep -r "Log\\.d\|Log\\.v\|Log\\.i" app/src/main/java/ | grep -i "password\|email\|location"
```

---

### 6.3 Data at Rest

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| DR-1 | Database encrypted | ✅ (verified in Section 1) | ⬜ | | |
| DR-2 | Backups encrypted | ✅ (verified in Section 2) | ⬜ | | |
| DR-3 | Photos not encrypted | Photos in app-specific dir (optional) | ⬜ | | Not critical (visible anyway) |
| DR-4 | Preferences encrypted | `EncryptedSharedPreferences` for settings | ⬜ | | Check `DatabaseKeyManager` |
| DR-5 | No temp files | No sensitive data in `/tmp` or `/cache` | ⬜ | | |

---

## 7. Third-Party Dependencies

**Objective**: Verify no known vulnerabilities in dependencies.

### 7.1 Dependency Audit

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| DEP-1 | SQLCipher up-to-date | v4.5.4+ | ⬜ | | Check `libs.versions.toml` |
| DEP-2 | Argon2 up-to-date | v13.1+ | ⬜ | | |
| DEP-3 | Room up-to-date | Latest stable (2.6.x) | ⬜ | | |
| DEP-4 | Compose up-to-date | Latest stable (1.7.x) | ⬜ | | |
| DEP-5 | CameraX up-to-date | Latest stable (1.4.x) | ⬜ | | |
| DEP-6 | No deprecated libraries | No libraries with known CVEs | ⬜ | | Check OWASP Dependency Check |

**Validation Command**:
```bash
# Check dependency versions
cat gradle/libs.versions.toml | grep "version ="

# Run dependency check (requires plugin)
# ./gradlew dependencyCheckAnalyze
```

**Files to Review**:
- `gradle/libs.versions.toml`

---

## 8. Test Coverage for Security Features

**Objective**: Verify security code has adequate test coverage.

### 8.1 Crypto Tests

| ID | Check | Expected | Status | Evidence | Notes |
|----|-------|----------|--------|----------|-------|
| TEST-1 | Argon2HelperTest exists | 28 tests | ⬜ | | Check test file |
| TEST-2 | CryptoHelperTest exists | 33 tests | ⬜ | | |
| TEST-3 | PasswordBasedCryptoTest exists | 23 tests | ⬜ | | |
| TEST-4 | CsvInjectionProtectionTest exists | 13 tests | ⬜ | | |
| TEST-5 | DeepLinkValidationTest exists | 10 tests | ⬜ | | |
| TEST-6 | Total crypto test coverage | > 95% | ⬜ | | Run JaCoCo |

**Files to Review**:
- `app/src/test/java/net/meshcore/mineralog/data/crypto/`
- `app/src/test/java/net/meshcore/mineralog/data/service/CsvInjectionProtectionTest.kt`
- `app/src/test/java/net/meshcore/mineralog/ui/DeepLinkValidationTest.kt`

---

## 9. Security Regression Tests

**Objective**: Verify previous security fixes haven't regressed.

### 9.1 Known Vulnerability Checks

| ID | Original Issue | Fix Version | Status | Notes |
|----|----------------|-------------|--------|-------|
| REG-1 | P0.1: Argon2 all-zero keys | v1.6.0 | ⬜ | Verify `argon2.hash()` called |
| REG-2 | P0.2: Plaintext database | v1.6.0 | ⬜ | Verify SQLCipher active |
| REG-3 | P0.3: Missing transactions | v1.6.0 | ⬜ | Verify `withTransaction` used |
| REG-4 | P1.1: Deep link UUID validation | v1.6.0 | ⬜ | Verify dual-layer validation |
| REG-5 | P1.2: Debug keystore in release | v1.6.0 | ⬜ | Verify production signing |
| REG-6 | P1.3: Android backup enabled | v1.6.0 | ⬜ | Verify `allowBackup="false"` |
| REG-7 | P1.4: Cleartext HTTP allowed | v1.6.0 | ⬜ | Verify network config |
| REG-8 | P1-7: CSV injection | v3.0.0-beta | ⬜ | Verify escapeCSV() used |

---

## Audit Summary

**Total Checks**: 150+
**Passed**: ___
**Failed**: ___
**Warnings**: ___

**Pass Rate**: ___% (Target: 100%)

---

## Critical Issues Found

| ID | Severity | Component | Description | Remediation |
|----|----------|-----------|-------------|-------------|
| | P0/P1/P2 | | | |

---

## Recommendations

1. **High Priority**:
   -

2. **Medium Priority**:
   -

3. **Future Enhancements**:
   - Consider implementing certificate pinning for future network features
   - Add runtime integrity checks (SafetyNet/Play Integrity API)
   - Implement root detection for sensitive operations

---

## Sign-Off

**Security Auditor**: ___________________  Date: __________

**Tech Lead**: ___________________  Date: __________

**Release Manager**: ___________________  Date: __________

---

**Status**: ⬜ Approved for Production | ⬜ Requires Fixes
