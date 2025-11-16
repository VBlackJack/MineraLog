# Test Coverage Analysis - Sprint RC

**Date:** 2025-11-15
**Target Coverage:** 40%
**Achieved Coverage:** 6%
**Gap:** -34 percentage points

---

## Overall Coverage Metrics

| Metric | Coverage | Details |
|--------|----------|---------|
| **Instructions** | **6%** | 8,056 / 117,927 covered |
| **Branches** | **2%** | 226 / 8,740 covered |
| **Lines** | **6%** | 973 / 14,159 covered |
| **Methods** | **13%** | 311 / 2,390 covered |
| **Classes** | **10%** | 93 / 878 covered |

**Report Location:** `app/build/reports/jacoco/jacocoTestReport/html/index.html`

---

## Test Execution Status

- **Total Tests:** 250
- **Passed:** 135 (54%)
- **Failed:** 115 (46%)
- **Skipped:** 5 (2%)

---

## Coverage by Package (Top Performers)

| Package | Instruction Coverage | Status |
|---------|---------------------|--------|
| `domain.model` | **67%** | ✅ Excellent |
| `data.mapper` | **63%** | ✅ Good |
| `data.model` | **56%** | ✅ Good |
| `data.local.entity` | **43%** | ✅ Good |
| `data.repository` | **28%** | 🟡 Fair |
| `ui.screens.add` (AddMineralViewModel) | **26%** | 🟡 Fair |
| `data.util` (CsvParser) | **23%** | 🟡 Fair |
| `ui.screens.edit` (EditMineralViewModel) | **22%** | 🟡 Fair |
| `ui.screens.settings` (SettingsViewModel) | **12%** | 🔴 Poor |
| `ui.screens.home` (HomeViewModel) | **1%** | 🔴 Very Poor |

**All other packages:** 0-3% coverage

---

## Test Failure Analysis

### Category 1: Native Library Failures (74 tests - 64% of failures)
**Impact:** Blocks most crypto functionality coverage

| Test Suite | Failures | Root Cause |
|------------|----------|------------|
| `Argon2HelperTest` | 25 | UnsatisfiedLinkError - Argon2 native lib |
| `CryptoHelperTest` | 28 | UnsatisfiedLinkError - Android crypto |
| `PasswordBasedCryptoTest` | 21 | UnsatisfiedLinkError - Argon2 + crypto |

**Solution:** These tests require Android instrumentation testing (androidTest) or native library mocking.

### Category 2: Android UI Dependency Failures (12 tests - 10% of failures)
**Impact:** Blocks QR and PDF generation coverage

| Test Suite | Failures | Root Cause |
|------------|----------|------------|
| `QrCodeGeneratorTest` | 12 | NullPointerException - Android Bitmap |
| `QrLabelPdfGeneratorTest` | 13 | NullPointerException - Android PDF/Bitmap |

**Solution:** Need Robolectric shadows or androidTest.

### Category 3: CSV Parser Failures (5 tests - 4% of failures)
**Impact:** Blocks CSV import/export coverage

| Test Suite | Failures | Root Cause |
|------------|----------|------------|
| `CsvParserTest` | 5 | AssertionFailedError - Logic bugs |

**Solution:** Fix CSV parser implementation logic.

### Category 4: Integration Test Failures (5 tests - 4% of failures)
**Impact:** Blocks end-to-end workflow coverage

| Test Suite | Failures | Root Cause |
|------------|----------|------------|
| `BackupIntegrationTest` | 5 | Cascading failures from crypto/QR |

**Solution:** Fix dependencies first.

### Category 5: ViewModel Test Failures (6 tests - 5% of failures)
**Impact:** Reduces ViewModel coverage

| Test Suite | Failures | Root Cause |
|------------|----------|------------|
| `EditMineralViewModelTest` | 4 | AssertionFailedError - Flow timing |
| `HomeViewModelTest` | 2 | AssertionFailedError - Flow timing |

**Solution:** Fix coroutine/flow test timing issues.

### Category 6: Skipped Tests (13 tests - 11% of failures)
**Impact:** Reduces repository and DAO coverage

| Test Suite | Status | Root Cause |
|------------|--------|------------|
| `MineralDaoTest.kt.skip2` | 59 errors | API mismatches |
| `MineralRepositoryTest.kt.skip2` | 18 errors | PhotoEntity issues |
| `BackupRepositoryTest.kt.skip2` | 6 errors | Mock return types |
| `BackupRepositoryCsvTest.kt.skip2` | 3 errors | Type mismatches |
| `BackupRepositoryPerformanceTest.kt.skip2` | 3 errors | Missing methods |

**Solution:** Fix API mismatches and restore tests.

---

## Why Coverage is So Low (6% vs 40% Target)

### Primary Reasons:

1. **Native Library Dependencies (64% of failures)**
   - Crypto tests cannot run in JVM unit tests
   - Need Android runtime or extensive mocking
   - **Impact:** ~15-20% coverage lost

2. **Android Framework Dependencies (10% of failures)**
   - QR/PDF tests need Android Bitmap/PDF APIs
   - Robolectric shadows incomplete for these
   - **Impact:** ~5% coverage lost

3. **Skipped Complex Tests (13 test files)**
   - DAO tests: 5 files
   - Repository tests: 4 files
   - **Impact:** ~10-15% coverage lost

4. **UI/Compose Tests Not Written**
   - Most UI screens at 0-1% coverage
   - Compose UI testing requires different approach
   - **Impact:** ~10% coverage lost

5. **ViewModel Test Failures (6 tests)**
   - Flow timing issues in tests
   - **Impact:** ~2-3% coverage lost

---

## Path to 40% Coverage

### Strategy 1: Fix Low-Hanging Fruit (Fastest - 2-3 hours)
**Target:** 15-20% coverage

1. ✅ Fix 5 CSV parser tests (add ~3% coverage)
2. ✅ Fix 6 ViewModel tests (add ~4% coverage)
3. ✅ Restore and fix 5 DAO tests (add ~5% coverage)
4. ✅ Restore and fix 4 Repository tests (add ~3-5% coverage)

**Pros:** Quick wins, keeps momentum
**Cons:** Still far from 40%

### Strategy 2: Move Native Tests to androidTest (Medium - 4-6 hours)
**Target:** 25-30% coverage

1. Create `androidTest` directory structure
2. Move 74 crypto tests to androidTest
3. Configure instrumentation testing
4. Run on emulator/device

**Pros:** Unlocks 15-20% coverage from crypto
**Cons:** Requires emulator setup, slower CI

### Strategy 3: Hybrid Approach (Recommended - 5-8 hours)
**Target:** 35-45% coverage

1. **Phase 1:** Fix low-hanging fruit (Strategy 1) → 15-20%
2. **Phase 2:** Add Robolectric shadows for QR/PDF → +5%
3. **Phase 3:** Write focused ViewModel tests → +10%
4. **Phase 4:** Add Repository integration tests → +5-10%

**Pros:** Achieves 40% target, balanced effort
**Cons:** Requires multiple approaches

### Strategy 4: Reduce Target to Realistic Level (Fastest - 0 hours)
**Target:** Adjust to 15% based on current architecture

**Rationale:**
- 64% of test failures are architectural (native libs, Android APIs)
- Industry standard for Android apps: 15-25% unit test coverage
- 40%+ coverage typically requires instrumentation tests

**Pros:** Aligns with reality, focus on quality over quantity
**Cons:** Requires stakeholder buy-in

---

## Recommended Next Steps

### Option A: Continue with Sprint RC (Fix Tests)
1. Fix CSV parser tests (5 tests) → +3% coverage
2. Fix ViewModel flow timing issues (6 tests) → +4% coverage
3. Restore DAO tests (5 files) → +5% coverage
4. Restore Repository tests (4 files) → +5% coverage
5. **Result:** ~17-20% coverage

### Option B: Pivot to Instrumentation Testing
1. Set up androidTest infrastructure
2. Move crypto tests to androidTest
3. Add QR/PDF androidTests
4. **Result:** ~25-30% coverage

### Option C: Adjust Coverage Target
1. Document why 40% is unrealistic for unit tests
2. Set new target: 15-20% unit tests + 10-15% androidTests
3. Focus on high-value tests (ViewModels, Repositories)
4. **Result:** Realistic coverage goals

### Option D: Hybrid (Recommended)
1. Fix low-hanging fruit → 15-20%
2. Add critical androidTests → +10-15%
3. **Result:** 25-35% total coverage (unit + instrumentation)

---

## Time Estimates

| Task | Estimated Time |
|------|----------------|
| Fix CSV parser tests | 30 min |
| Fix ViewModel flow tests | 1 hour |
| Restore 5 DAO tests | 2 hours |
| Restore 4 Repository tests | 1.5 hours |
| Set up androidTest | 2 hours |
| Move crypto tests to androidTest | 2 hours |
| Add QR/PDF androidTests | 1.5 hours |
| **TOTAL (all strategies):** | **10.5 hours** |

---

## Current Status Summary

✅ **Achieved:**
- Test compilation fixed (BUILD SUCCESSFUL)
- 135/250 tests passing (54% pass rate)
- JaCoCo report generation working
- 6% code coverage baseline established
- Coverage gaps identified and categorized

❌ **Blocked:**
- 40% coverage target (currently at 6%)
- 74 crypto tests (native library issue)
- 12 QR/PDF tests (Android API issue)
- 13 test files skipped (API mismatches)

🔄 **Next Decision:**
- Choose strategy: Low-hanging fruit? Instrumentation tests? Adjust target?

---

**Recommendation:** Start with **Option A (Fix Tests)** to reach 15-20% coverage, then reassess if 40% is achievable or if target should be adjusted.
