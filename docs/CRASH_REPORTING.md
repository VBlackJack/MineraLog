# MineraLog - Crash Reporting Strategy (Privacy-First)

**Version**: 3.0.0
**Status**: Planned for v3.1.0
**Last Updated**: 2025-11-16

---

## Principles

MineraLog follows a **privacy-first** approach to crash reporting. We will NEVER:

❌ Track user behavior
❌ Collect personally identifiable information (PII)
❌ Send data without explicit user consent
❌ Share data with third parties (except crash report processors)
❌ Use crash reports for marketing or analytics

We will ALWAYS:

✅ Make crash reporting **opt-in only** (disabled by default)
✅ Collect **minimum necessary data** (stack traces only)
✅ Store crash reports **locally first** (user can review/delete)
✅ Be **transparent** about what data is collected
✅ Allow users to **disable at any time** with no penalty
✅ **Open source** our crash reporting implementation

---

## Current State (v3.0.0)

**Status**: No custom crash reporting implemented

**What happens now**:
1. App crashes → Android system crash handler takes over
2. User sees "App has stopped" dialog
3. Android may collect crash report (depends on device settings)
4. **We receive no crash data** (no Firebase, no Crashlytics, no ACRA)

**How users report crashes**:
- Manually via GitHub Issues
- Include device info, steps to reproduce, logs if available
- See `docs/SUPPORT.md` for bug report template

**Why we don't have crash reporting yet**:
- **Privacy commitment**: We refuse to compromise on user privacy
- **No opt-in implementation yet**: We won't collect data without explicit consent
- **Open source audit**: We want crash reporting code to be auditable

---

## Planned Implementation (v3.1.0)

**Target Release**: v3.1.0 (Q1 2025)
**Library**: ACRA (Application Crash Reports for Android)
**License**: Apache License 2.0 (open source)

### Why ACRA?

**Advantages**:
- ✅ **Open source** and auditable
- ✅ **Privacy-friendly** by default
- ✅ **Fully configurable** (we control what data is collected)
- ✅ **Local-first** (stores crashes locally before upload)
- ✅ **No third-party dependencies** (Firebase, Google Analytics, etc.)
- ✅ **Lightweight** (~200 KB)

**Alternatives considered**:
- **Firebase Crashlytics**: ❌ Proprietary, Google dependency, not privacy-friendly
- **Sentry**: ❌ Third-party service, cloud-based, not fully open source
- **Bugsnag**: ❌ Third-party service, commercial, not privacy-friendly
- **Custom solution**: ⚠️ Reinventing the wheel, maintenance burden

**Decision**: ACRA is the best fit for our privacy-first principles.

---

## Data Collection Policy

### What We Will Collect (with user consent)

**Crash Metadata**:
- **App version**: e.g., "3.1.0" (versionName + versionCode)
- **Android version**: e.g., "Android 13 (API 33)"
- **Device model**: e.g., "Pixel 6" (manufacturer + model, NO unique IDs)
- **Crash timestamp**: e.g., "2025-01-15 14:32:18 UTC"

**Crash Details**:
- **Exception type**: e.g., "NullPointerException", "SQLiteException"
- **Stack trace**: Full Java/Kotlin stack trace (code line numbers, file names)
- **Thread name**: e.g., "main", "DefaultDispatcher-worker-1"

**App State** (minimal):
- **Activity stack**: Which screens were open (e.g., "HomeScreen → MineralDetailScreen")
- **Available memory**: e.g., "1.2 GB free / 4 GB total"
- **Available storage**: e.g., "8 GB free"

**User Actions** (last 10 actions only, no PII):
- Example: "Tapped Add Mineral button → Entered text in name field → Tapped Save"
- **NO** actual data (no mineral names, no photos, no notes)
- Only action types and UI elements

### What We Will NEVER Collect

❌ **Personal Information**:
- Names, emails, phone numbers
- GPS coordinates or location data
- IP addresses or MAC addresses
- Device unique IDs (IMEI, Android ID, Advertising ID)

❌ **User Data**:
- Mineral names, formulas, notes
- Photo files or photo metadata
- Provenance, storage, or collection data
- Backup passwords or encryption keys
- Database contents

❌ **Behavioral Tracking**:
- Time spent in app
- Feature usage frequency
- Search queries
- User preferences

❌ **Network Activity**:
- API calls (we don't make any)
- URLs visited (we're offline-first)

---

## User Experience

### Opt-In Flow (First Launch)

**When**: First launch after installing v3.1.0+

**Dialog**:
```
┌─────────────────────────────────────────────┐
│  Help Improve MineraLog                     │
├─────────────────────────────────────────────┤
│                                              │
│  Would you like to help us fix crashes by   │
│  sending anonymous crash reports?           │
│                                              │
│  • Crash reports contain only technical     │
│    error details (stack traces)             │
│  • No personal data or mineral collection   │
│    data is included                         │
│  • You can disable this at any time in      │
│    Settings                                 │
│                                              │
│  [Learn More]  [No Thanks]  [Enable]        │
└─────────────────────────────────────────────┘
```

**Options**:
1. **Enable**: Crash reporting enabled, user sees confirmation
2. **No Thanks**: Crash reporting disabled (can enable later in Settings)
3. **Learn More**: Opens `docs/CRASH_REPORTING.md` (this file) in browser

**Default**: Disabled (no pre-checked boxes, must explicitly opt-in)

---

### Settings Screen

**Location**: Settings → Advanced → Crash Reporting

**Toggle**:
```
Crash Reporting                          [  ] OFF
Help improve the app by sending crash
reports when the app unexpectedly closes.
No personal data is collected.

[Tap for details]
```

**Details Screen** (when tapped):
```
┌─────────────────────────────────────────────┐
│  Crash Reporting Details                    │
├─────────────────────────────────────────────┤
│                                              │
│  Status: Disabled                            │
│                                              │
│  When enabled, MineraLog will:              │
│  • Save crash logs locally when the app     │
│    unexpectedly closes                      │
│  • Ask if you want to send the crash report │
│  • Send only technical error details        │
│                                              │
│  Crash reports include:                     │
│  ✓ App version and Android version          │
│  ✓ Device model (e.g., Pixel 6)             │
│  ✓ Error stack trace (code line numbers)    │
│  ✓ Timestamp of crash                       │
│                                              │
│  Crash reports DO NOT include:              │
│  ✗ Your mineral data                        │
│  ✗ Photos or notes                          │
│  ✗ Personal information                     │
│  ✗ Location or IP address                   │
│                                              │
│  All crash report code is open source and   │
│  auditable on GitHub.                       │
│                                              │
│  [View Crash Logs]  [Enable]  [Disable]     │
└─────────────────────────────────────────────┘
```

---

### Post-Crash Flow

**When app crashes** (if crash reporting enabled):

1. **App crashes** → Android system shows "App has stopped" dialog
2. **User reopens app** → MineraLog detects pending crash report
3. **Dialog appears**:

```
┌─────────────────────────────────────────────┐
│  MineraLog Crashed                          │
├─────────────────────────────────────────────┤
│                                              │
│  We're sorry! MineraLog unexpectedly closed │
│  last time.                                 │
│                                              │
│  Would you like to send a crash report to   │
│  help us fix this issue?                    │
│                                              │
│  [View Report]  [Don't Send]  [Send]        │
└─────────────────────────────────────────────┘
```

**Options**:
1. **Send**: Upload crash report, show "Thank you" message
2. **Don't Send**: Delete crash report, user can continue normally
3. **View Report**: Show full stack trace in text viewer (technical users)

**View Report Screen**:
```
┌─────────────────────────────────────────────┐
│  Crash Report                                │
├─────────────────────────────────────────────┤
│  Date: 2025-01-15 14:32:18 UTC              │
│  Version: 3.1.0 (versionCode 31)            │
│  Device: Google Pixel 6 (Android 14)        │
│                                              │
│  Exception: java.lang.NullPointerException  │
│  Message: Attempt to invoke virtual method  │
│  'java.lang.String...' on a null object     │
│                                              │
│  Stack Trace:                               │
│  at net.meshcore.mineralog.ui.HomeScreen... │
│    (HomeScreen.kt:245)                      │
│  at androidx.compose.runtime.Composer...    │
│    ...                                       │
│                                              │
│  [Copy to Clipboard]  [Share]  [Send]       │
└─────────────────────────────────────────────┘
```

---

## Local Crash Log Storage

**Location**: `<app-data>/files/crashes/`
- Example: `/data/data/net.meshcore.mineralog/files/crashes/`

**File Format**: JSON
- Example: `crash_2025-01-15_14-32-18.json`

**Storage Limit**:
- Keep last **10 crash reports** only
- Delete older reports automatically (FIFO)
- Max size per report: ~50 KB
- Total max: ~500 KB

**User Access**:
- Settings → Advanced → Crash Reporting → View Crash Logs
- List of recent crashes with date/time
- Tap to view, share (email, GitHub issue), or delete

**Retention**:
- Crash logs stored locally until:
  - User sends the report
  - User deletes the report
  - 30 days pass (auto-delete)
  - User uninstalls app (all data deleted)

---

## Crash Report Upload

### Upload Mechanism

**Option 1: GitHub Issues (Recommended for v3.1.0)**

**Pros**:
- ✅ No third-party service
- ✅ Public and transparent
- ✅ Existing infrastructure (GitHub)
- ✅ Users can follow progress on their crash

**Cons**:
- ⚠️ Requires GitHub account (for user to submit)
- ⚠️ Manual process (no automated aggregation)
- ⚠️ GitHub API rate limits

**Implementation**:
1. User taps "Send" → Opens GitHub issue template pre-filled with crash report
2. User reviews, edits, and submits
3. We see issue, triage, and fix

**Option 2: Email**

**Pros**:
- ✅ Simple
- ✅ No account required

**Cons**:
- ⚠️ Manual sorting
- ⚠️ Spam risk

**Implementation**:
1. User taps "Send" → Opens email client
2. To: crashes@mineralog.app
3. Subject: "MineraLog Crash Report - v3.1.0 - Pixel 6 - 2025-01-15"
4. Body: JSON crash report
5. User sends email
6. We receive, triage, and fix

**Option 3: Self-Hosted Crash Collector (Future v3.2+)**

**Pros**:
- ✅ Automated aggregation (group similar crashes)
- ✅ Deduplication
- ✅ Statistics (crash rate, affected versions)

**Cons**:
- ⚠️ Requires server infrastructure
- ⚠️ Maintenance burden
- ⚠️ Privacy audit needed

**Implementation** (future):
1. Deploy Sentry (self-hosted) or custom ACRA backend
2. HTTPS-only endpoint
3. No logging of IP addresses
4. Open source backend code
5. Optional: User can see aggregated stats (opt-in)

**Decision for v3.1.0**: Use Option 1 (GitHub Issues) initially, evaluate Option 3 if crash volume > 50/month.

---

## ACRA Configuration (v3.1.0)

**Dependency** (`build.gradle.kts`):
```kotlin
dependencies {
    implementation("ch.acra:acra-core:5.11.3")
    implementation("ch.acra:acra-dialog:5.11.3")
    implementation("ch.acra:acra-limiter:5.11.3")
}
```

**Configuration** (`MineraLogApplication.kt`):
```kotlin
@AcraCore(
    buildConfigClass = BuildConfig::class,
    reportFormat = StringFormat.JSON,
    reportContent = [
        ReportField.APP_VERSION_NAME,
        ReportField.APP_VERSION_CODE,
        ReportField.ANDROID_VERSION,
        ReportField.PHONE_MODEL,
        ReportField.BRAND,
        ReportField.STACK_TRACE,
        ReportField.LOGCAT,
        ReportField.CRASH_CONFIGURATION,
        ReportField.USER_CRASH_DATE,
        ReportField.AVAILABLE_MEM_SIZE,
        ReportField.TOTAL_MEM_SIZE,
        // Explicitly exclude PII fields:
        // NO: ReportField.DEVICE_ID
        // NO: ReportField.ANDROID_ID
        // NO: ReportField.SETTINGS_SYSTEM
        // NO: ReportField.SHARED_PREFERENCES (contains sensitive data)
    ],
    alsoReportToAndroidFramework = true
)
@AcraDialog(
    resText = R.string.crash_dialog_text,
    resCommentPrompt = R.string.crash_dialog_comment_prompt,
    resPositiveButtonText = R.string.send,
    resNegativeButtonText = R.string.dont_send,
    resIcon = R.drawable.ic_crash
)
@AcraLimiter(
    exceptionClassLimit = 3,  // Max 3 reports for same exception
    overallLimit = 10,         // Max 10 reports total per install
    resetPeriod = 86400        // Reset limits every 24h
)
class MineraLogApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        // Only initialize ACRA if user has opted in
        val prefs = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val crashReportingEnabled = prefs.getBoolean("crash_reporting_enabled", false)

        if (crashReportingEnabled) {
            ACRA.init(this)
        }
    }
}
```

**Strings** (`strings.xml`):
```xml
<string name="crash_dialog_text">
    MineraLog crashed unexpectedly. Would you like to send a crash report to help us fix this issue?\n\n
    The report contains only technical error details. No personal data or mineral collection data is included.
</string>
<string name="crash_dialog_comment_prompt">
    Optional: Describe what you were doing when the crash occurred
</string>
```

---

## Privacy Compliance

### GDPR Compliance (Europe)

**User Rights**:
- ✅ **Right to know**: Full transparency in `docs/CRASH_REPORTING.md`
- ✅ **Right to consent**: Opt-in only, no pre-checked boxes
- ✅ **Right to withdraw**: Disable in Settings at any time
- ✅ **Right to access**: View all crash logs locally
- ✅ **Right to deletion**: Delete crash logs individually or all at once
- ✅ **Data minimization**: Collect only essential crash data
- ✅ **Purpose limitation**: Use crash reports ONLY for debugging

**Legal Basis**: Legitimate interest (improving app quality) + explicit consent

---

### CCPA Compliance (California)

**User Rights**:
- ✅ **Right to know**: Privacy policy explains what data is collected
- ✅ **Right to delete**: User can delete all crash logs
- ✅ **Right to opt-out**: Disable crash reporting at any time
- ✅ **No sale of data**: We never sell crash data (or any data)

---

### Children's Privacy (COPPA)

**Status**: MineraLog is **not directed at children under 13**

**If used by children**:
- No additional data is collected
- Crash reporting requires parent/guardian consent (honor system, no age verification)

---

## Security

### Transmission Security

**HTTPS Only**:
- All crash report uploads use HTTPS (TLS 1.2+)
- Certificate pinning (optional, for self-hosted backend in v3.2+)

**No Sensitive Data**:
- Crash reports never contain passwords, encryption keys, or user data
- Database contents never included
- Audit ACRA config to ensure no sensitive fields collected

**Encryption**:
- Crash reports stored locally are **not encrypted** (they contain no PII)
- If we implement self-hosted backend (v3.2+), consider encrypting reports at rest

---

## Metrics & Reporting (Internal Use)

**Crash Analytics** (for developers only):
- Group crashes by exception type
- Identify most common crash causes
- Track crash rate by:
  - App version (e.g., v3.1.0 has 2% crash rate, v3.1.1 has 0.5%)
  - Android version (e.g., Android 8.1 crashes more than Android 13)
  - Device model (e.g., Samsung devices crash more than Pixels)

**Dashboards** (if self-hosted backend in v3.2+):
- Crash rate over time (line chart)
- Top 10 crash causes (bar chart)
- Affected users by version (pie chart)

**Public Transparency** (optional):
- Publish anonymized crash statistics quarterly
- Example: "Q1 2025: 0.8% crash rate, top issue: NullPointerException in HomeScreen (fixed in v3.1.2)"

---

## Testing Crash Reporting

**Test Crash** (developer mode):

1. Enable crash reporting in Settings
2. Go to Settings → Advanced → Developer Options → Trigger Test Crash
3. App crashes intentionally with test exception
4. Reopen app → Crash report dialog appears
5. Verify crash report contains correct data
6. Send report → Verify it arrives (GitHub issue or email)

**Test Code**:
```kotlin
// Add to SettingsScreen.kt (behind developer mode toggle)
Button(onClick = {
    throw RuntimeException("Test crash from developer mode")
}) {
    Text("Trigger Test Crash")
}
```

---

## Rollout Plan for Crash Reporting

**Phase 1: v3.1.0-beta** (2 weeks)
- Deploy ACRA crash reporting to beta testers (50-100 users)
- Use GitHub Issues for crash uploads
- Collect feedback on opt-in flow and privacy transparency
- Fix any bugs in crash reporting implementation

**Phase 2: v3.1.0 Production** (staged rollout)
- Deploy to 5% → 10% → 25% → 50% → 100% of users
- Monitor opt-in rate (target: ≥ 20% opt-in)
- Monitor crash report volume
- Respond to user privacy concerns

**Phase 3: v3.2.0** (self-hosted backend, if needed)
- If crash report volume > 50/month, deploy self-hosted Sentry or custom ACRA backend
- Migrate from GitHub Issues to automated aggregation
- Publish anonymized crash statistics

---

## Alternatives Considered

### Option 1: No Crash Reporting
**Pros**: Maximum privacy
**Cons**: We have no visibility into crashes, users must manually report
**Decision**: Not sustainable for improving quality

### Option 2: Firebase Crashlytics
**Pros**: Easy integration, good tooling
**Cons**: Proprietary, Google dependency, not privacy-friendly, requires Firebase SDK
**Decision**: Conflicts with privacy-first principles

### Option 3: Sentry (cloud)
**Pros**: Great UI, good aggregation
**Cons**: Third-party service, commercial, not fully open source
**Decision**: Cloud-based conflicts with offline-first philosophy

### Option 4: ACRA (local-first)
**Pros**: Open source, privacy-friendly, local storage, fully configurable
**Cons**: Manual upload flow (GitHub/email), no automated aggregation in v3.1
**Decision**: ✅ Best fit for v3.1.0, can upgrade to self-hosted backend later

---

## User Communication

**Privacy Policy** (to be created):
- Section on crash reporting
- Link to this document for technical details
- Explain opt-in, data collected, data usage, data retention

**In-App Transparency**:
- First-launch dialog clearly explains crash reporting
- Settings screen links to this document
- "Learn More" buttons everywhere

**Blog Post** (when v3.1.0 launches):
- Title: "How MineraLog Balances Crash Reporting with Privacy"
- Explain our privacy-first approach
- Showcase ACRA choice and local-first storage
- Invite feedback from privacy advocates

---

## Open Questions

1. **Should we include logcat?**
   - Pros: More context for debugging
   - Cons: May contain sensitive data from other apps
   - Decision: Include last 100 lines of logcat, filtered to MineraLog package only

2. **Should we allow user comments in crash reports?**
   - Pros: More context ("I was trying to import a CSV with 1000 rows")
   - Cons: Users may accidentally include PII
   - Decision: Yes, but with warning: "Do not include personal information"

3. **Should we deduplicate crashes client-side?**
   - Pros: Don't spam us with 100 identical crashes
   - Cons: May miss patterns
   - Decision: Use ACRA's built-in limiter (max 3 reports for same exception)

4. **Should we offer crash reporting opt-in incentive?**
   - Example: "Enable crash reporting to get early access to beta features"
   - Decision: No. Crash reporting is purely altruistic (help improve the app)

---

## Implementation Checklist (v3.1.0)

- [ ] Add ACRA dependencies to `build.gradle.kts`
- [ ] Configure ACRA in `MineraLogApplication.kt`
- [ ] Add opt-in dialog on first launch
- [ ] Add Settings toggle for crash reporting
- [ ] Add "View Crash Logs" screen
- [ ] Add "Trigger Test Crash" button (developer mode)
- [ ] Write strings (EN/FR) for crash dialogs
- [ ] Test opt-in flow
- [ ] Test crash report generation and upload
- [ ] Update `PRIVACY_POLICY.md` (to be created)
- [ ] Update `SUPPORT.md` with crash reporting section
- [ ] Write blog post explaining privacy-first crash reporting
- [ ] Deploy to beta testers
- [ ] Collect feedback and iterate

---

**Status**: ⬜ Not Started (planned for v3.1.0)
**Responsible**: Tech Lead
**ETA**: Q1 2025
