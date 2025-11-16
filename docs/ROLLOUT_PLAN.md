# MineraLog v3.0.0 - Staged Rollout Plan

**Version**: 3.0.0
**Type**: Major Release
**Planned Launch**: January 2025
**Target Platforms**: Google Play Store, F-Droid

---

## Overview

This document outlines the **staged rollout strategy** for MineraLog v3.0.0, designed to minimize risk and gather feedback before full public release.

**Key Principles**:
1. **Progressive deployment**: Start small, scale gradually
2. **Data-driven decisions**: Monitor metrics at each stage
3. **Fast rollback**: Ability to revert quickly if issues detected
4. **User communication**: Transparent communication at all stages

---

## Phase 1: Internal Testing (Week 1)

**Duration**: 7 days
**Target Date**: January 6-12, 2025

### Audience
- **Size**: 1 developer + 5 trusted beta testers
- **Selection criteria**: Technical users familiar with app, diverse devices
- **Distribution**: GitHub Releases (pre-release tag)

### Objectives
- Validate core functionality on real devices
- Identify critical (P0) bugs before wider distribution
- Test migration from v1.x/v2.x databases
- Verify performance on low-end devices (Android 8.1)

### Test Matrix
| Device Type | Android | Tester | Status |
|-------------|---------|--------|--------|
| Pixel 6 | 14 | Developer | ⬜ |
| Samsung S21 | 13 | Beta Tester 1 | ⬜ |
| OnePlus 9 | 12 | Beta Tester 2 | ⬜ |
| Xiaomi Redmi | 11 | Beta Tester 3 | ⬜ |
| Motorola G7 | 9 | Beta Tester 4 | ⬜ |
| Emulator | 8.1 (minSdk) | Beta Tester 5 | ⬜ |

### Success Criteria
- [ ] **0 P0 (critical) bugs**
  - No app crashes on launch
  - No data loss during migration
  - No security vulnerabilities
  - Database migration successful from v1-v6

- [ ] **< 3 P1 (high) bugs**
  - UI rendering issues acceptable
  - Minor performance regressions acceptable
  - Non-critical feature bugs acceptable

- [ ] **All testers complete test plan**
  - Manual test plan: 30/30 core flows completed
  - Feedback form submitted by all testers

### Feedback Collection
- **Method**: Google Forms + GitHub Issues (private repo access)
- **Questions**:
  1. Did the app crash? (Y/N, if yes, describe)
  2. Did database migration succeed? (Y/N)
  3. Performance compared to v1.x? (Better/Same/Worse)
  4. Any data loss? (Y/N, if yes, describe)
  5. Overall impression (1-5 scale)
  6. Would you recommend? (Y/N)

### Go/No-Go Decision
**Criteria to proceed to Phase 2**:
- ✅ 0 P0 bugs
- ✅ < 3 P1 bugs
- ✅ All testers rate ≥ 3/5
- ✅ No data loss reports
- ✅ No crash rate > 1%

**Responsible**: Tech Lead
**Decision Date**: January 12, 2025 (end of Week 1)

---

## Phase 2: Open Beta (Week 2-3)

**Duration**: 14 days
**Target Date**: January 13-26, 2025

### Audience
- **Size**: 50-100 early adopters
- **Selection**: Open beta opt-in via Google Play Beta track
- **Announcement**: GitHub Discussions, Reddit (r/mineralogy), Twitter

### Distribution Channels
1. **Google Play Beta Track**
   - Set up "Closed Beta" test track
   - Invite opt-in users via email list
   - Max 100 testers for beta track

2. **GitHub Releases**
   - Tag: `v3.0.0-beta`
   - Label: "Pre-release"
   - Include release notes and known issues

### Monitoring Metrics
| Metric | Target | Alert Threshold | Data Source |
|--------|--------|-----------------|-------------|
| **Crash Rate** | < 0.5% | > 1% | Firebase Crashlytics (opt-in) |
| **ANR Rate** | < 0.2% | > 0.5% | Google Play Console |
| **1-Day Retention** | > 80% | < 70% | Google Play Console |
| **7-Day Retention** | > 50% | < 40% | Google Play Console |
| **Average Rating** | ≥ 4.0/5.0 | < 3.5 | Google Play Reviews |
| **Database Migrations** | 100% success | < 98% | In-app telemetry (opt-in) |

### Feedback Channels
- **Google Play Reviews**: Monitor daily, respond within 24h
- **GitHub Issues**: Public bug reports
- **GitHub Discussions**: Feature requests, general feedback
- **Beta Tester Survey**: Email survey at end of Week 3

### Known Issues to Announce
- List any P2 bugs that won't be fixed before beta
- Example: "Photo gallery scroll performance on devices with 1000+ photos may be slower than expected on low-end devices"

### Go/No-Go Decision
**Criteria to proceed to Phase 3**:
- ✅ Crash rate < 1%
- ✅ ANR rate < 0.5%
- ✅ Average rating ≥ 4.0
- ✅ No new P0 bugs discovered
- ✅ 7-day retention ≥ 40%
- ✅ ≥ 25 active testers (25% of 100)

**Responsible**: Product Owner
**Decision Date**: January 26, 2025 (end of Week 3)

---

## Phase 3: Staged Production Rollout (Week 4)

**Duration**: 10 days
**Target Date**: January 27 - February 5, 2025

### Rollout Schedule

| Day | Rollout % | Est. Users | Monitoring | Decision Point |
|-----|-----------|------------|------------|----------------|
| **Day 1** | 5% | ~50 | Every 4 hours | Proceed if < 0.5% crashes |
| **Day 3** | 10% | ~100 | Twice daily | Proceed if < 0.5% crashes |
| **Day 5** | 25% | ~250 | Daily | Proceed if < 0.75% crashes |
| **Day 7** | 50% | ~500 | Daily | Proceed if < 1% crashes |
| **Day 10** | 100% | ~1,000+ | Weekly | Full release |

**Note**: User estimates assume 1,000 total installs. Adjust based on actual install base from v1.x/v2.x.

### Rollout Configuration (Google Play Console)

1. **Go to Release → Production → Create new release**
2. **Upload APK**: `mineralog-v3.0.0.apk` (signed with production key)
3. **Release name**: `3.0.0 - Reference Library`
4. **Release notes**: Copy from `RELEASE_NOTES_v3.0.0.md` (EN/FR)
5. **Enable staged rollout**: Check "Release to a percentage of users"
6. **Set initial percentage**: 5%

### Monitoring During Rollout

**Automated Alerts** (Google Play Console + Firebase):
- Email alert if crash rate > 1%
- Email alert if ANR rate > 0.5%
- Email alert if rating drops below 3.5
- Email alert if uninstall rate > 10%

**Daily Manual Checks**:
- [ ] Crash rate trend (should be stable or decreasing)
- [ ] Review new 1-star ratings (respond within 24h)
- [ ] Check for "data loss" keywords in reviews
- [ ] Monitor GitHub Issues for new bugs

### Rollback Triggers

**Immediate Rollback** (within 1 hour):
- 🚨 Crash rate > 2%
- 🚨 Data loss reports (> 2 confirmed cases)
- 🚨 Security vulnerability discovered (P0)
- 🚨 Database corruption (> 1% of users)

**Pause Rollout** (investigate before proceeding):
- ⚠️ Crash rate > 1% but < 2%
- ⚠️ ANR rate > 1%
- ⚠️ Average rating < 3.5
- ⚠️ Uninstall rate > 15%
- ⚠️ 3+ reports of same P1 bug

### Rollback Procedure

If rollback required:

1. **Stop rollout**:
   - Google Play Console → Production → Halt rollout
   - Reverts affected users to previous version (v2.x) on next update check

2. **Communicate**:
   - Post to GitHub Discussions: "Investigating issue with v3.0.0 rollout"
   - Twitter/social: "We've paused the v3.0.0 rollout to investigate [issue]. Existing users not affected."
   - Email beta testers: Detailed explanation + timeline

3. **Investigate**:
   - Download crash reports from Firebase
   - Reproduce bug locally
   - Identify root cause
   - Develop fix

4. **Release hotfix**:
   - Version: v3.0.1
   - Hotfix branch: `hotfix/3.0.1-[issue-description]`
   - Fast-track testing (internal only, 24h max)
   - Re-start rollout with v3.0.1

---

## Phase 4: F-Droid Submission (Week 5)

**Duration**: ~14 days (F-Droid review process)
**Target Date**: February 6-19, 2025

### Prerequisites
- ✅ Google Play 100% rollout successful
- ✅ No P0/P1 bugs in production
- ✅ Crash rate < 1% for 7 consecutive days

### Submission Checklist

1. **Prepare metadata**:
   - [ ] `fastlane/metadata/android/en-US/` populated
   - [ ] `fastlane/metadata/android/fr-FR/` populated (optional)
   - [ ] Screenshots (8 images, 16:9) generated
   - [ ] `metadata.yml` validated

2. **Verify F-Droid requirements**:
   - [ ] No proprietary dependencies (Google Play Services, Firebase)
   - [ ] No tracking or analytics code
   - [ ] All dependencies from F-Droid, Maven Central, or jCenter
   - [ ] Reproducible builds (optional but recommended)

3. **Submit Merge Request**:
   - Fork https://gitlab.com/fdroid/fdroiddata
   - Create `metadata/net.meshcore.mineralog.yml`
   - Add screenshots to `metadata/net.meshcore.mineralog/en-US/phoneScreenshots/`
   - Submit MR with title: "New app: MineraLog v3.0.0"

4. **F-Droid Review**:
   - Expect 1-2 weeks for initial review
   - Respond to reviewer feedback within 48h
   - Common requests: Remove non-free dependencies, fix reproducibility issues

### F-Droid Metadata Example

```yaml
Categories:
  - Science & Education
License: Apache-2.0
SourceCode: https://github.com/VBlackJack/MineraLog
IssueTracker: https://github.com/VBlackJack/MineraLog/issues
Changelog: https://github.com/VBlackJack/MineraLog/blob/main/CHANGELOG.md

AutoName: MineraLog
Summary: Offline mineral collection manager with encrypted database

Description: |-
    MineraLog is a privacy-focused, offline-first mineral collection manager
    for collectors, students, and geology enthusiasts.

    Features:
    * 300+ mineral reference library with 17 collector-specific fields
    * Photo cataloging with QR codes
    * Support for mineral aggregates (Granite, Gneiss, etc.)
    * AES-256 encrypted database (SQLCipher)
    * CSV import/export
    * Advanced search and filtering
    * Bilingual (EN/FR)
    * 100% offline (no internet required)

RepoType: git
Repo: https://github.com/VBlackJack/MineraLog.git

Builds:
  - versionName: '3.0.0'
    versionCode: 30
    commit: v3.0.0
    subdir: app
    gradle:
      - release
    prebuild: echo "MAPS_API_KEY=none" > ../local.properties
    scandelete:
      - tools/

MaintainerNotes: |-
    Requires local.properties with dummy MAPS_API_KEY for build.
    Google Maps is optional and not included in F-Droid build.
```

---

## Phase 5: Post-Launch Monitoring (Week 6+)

**Duration**: Ongoing (first 30 days critical)
**Target Date**: February 20 - March 21, 2025

### Success Metrics (30 Days Post-Launch)

| Metric | Target | Stretch Goal | Actual | Status |
|--------|--------|--------------|--------|--------|
| **Total Installs** | 500+ | 1,000+ | ___ | ⬜ |
| **Active Users (D30)** | 250+ | 500+ | ___ | ⬜ |
| **Average Rating** | ≥ 4.2/5.0 | ≥ 4.5 | ___ | ⬜ |
| **Crash Rate** | < 1% | < 0.5% | ___ | ⬜ |
| **ANR Rate** | < 0.5% | < 0.2% | ___ | ⬜ |
| **7-Day Retention** | ≥ 40% | ≥ 50% | ___ | ⬜ |
| **30-Day Retention** | ≥ 25% | ≥ 35% | ___ | ⬜ |
| **F-Droid Rating** | ≥ 4.0/5.0 | ≥ 4.3 | ___ | ⬜ |
| **GitHub Stars** | 10+ | 25+ | ___ | ⬜ |

### Monitoring Cadence

**Week 1-2 (Critical Period)**:
- Daily crash report review
- Daily review of 1-2 star ratings
- Respond to all GitHub Issues within 24h
- Monitor social media mentions

**Week 3-4**:
- Every 2 days crash report review
- Every 2 days rating review
- Respond to GitHub Issues within 48h

**Week 5+**:
- Weekly crash report review
- Weekly rating review
- Respond to GitHub Issues within 72h

### Communication Plan

**Launch Announcements** (Day 1 of 100% rollout):
1. **GitHub Release**:
   - Tag `v3.0.0`
   - Release notes from `RELEASE_NOTES_v3.0.0.md`
   - Attach signed APK + checksums

2. **Social Media**:
   - Reddit: Post to r/mineralogy, r/rockhounds, r/android
   - Twitter: Thread highlighting key features
   - LinkedIn: Professional post for geology students/teachers

3. **Direct Outreach**:
   - Email beta testers: "v3.0.0 is live!"
   - Email existing users (if email opt-in available)

**Weekly Updates** (First 4 weeks):
- GitHub Discussions: "Week X Update" with stats and highlights
- Twitter: User testimonials, feature spotlights

**Monthly Newsletter** (if mailing list exists):
- Usage statistics (aggregated, privacy-respecting)
- Top feature requests
- Roadmap for v3.1.0

---

## Risk Mitigation

### Identified Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Critical bug in production** | Low | High | Staged rollout, automated alerts, fast rollback |
| **Database migration failure** | Medium | High | Extensive testing, backup prompts, migration tests |
| **Performance regression on low-end devices** | Medium | Medium | Test on Android 8.1 emulator, performance benchmarks |
| **F-Droid rejection** | Medium | Low | Pre-validate metadata, remove non-free deps |
| **Low adoption rate** | Medium | Medium | Marketing, community engagement, user testimonials |
| **Negative reviews** | Low | Medium | Proactive support, fast bug fixes, transparent communication |

### Contingency Plans

**If adoption < 250 users after 30 days**:
1. Conduct user research: Why aren't people using it?
2. A/B test store listing (screenshots, description)
3. Increase community engagement (Reddit AMAs, tutorials)
4. Consider promotional campaigns (Product Hunt, Hacker News)

**If crash rate remains > 1% after Week 1**:
1. Identify top 3 crash causes (Firebase Crashlytics)
2. Release hotfix v3.0.1 within 48h
3. Communicate transparently: "We're aware of crashes on [device/Android version], fix incoming"

---

## Rollout Checklist

### Pre-Launch
- [ ] All P0/P1 bugs resolved
- [ ] Manual test plan completed (200+ tests)
- [ ] Automated tests passing (100%)
- [ ] Security audit completed
- [ ] Monkey test (10K events, 0 crashes)
- [ ] APK signed with production key
- [ ] Release notes finalized (EN/FR)
- [ ] Screenshots generated (8 images)
- [ ] Google Play Store listing complete
- [ ] F-Droid metadata prepared
- [ ] Rollout plan approved by Product Owner

### Launch
- [ ] Phase 1 (Internal): 6/6 testers completed
- [ ] Phase 2 (Beta): ≥ 25 active testers, ≥ 4.0 rating
- [ ] Phase 3 (Production): Staged 5% → 10% → 25% → 50% → 100%
- [ ] Phase 4 (F-Droid): Merge request submitted
- [ ] Phase 5 (Monitoring): 30-day metrics tracked

### Post-Launch
- [ ] GitHub Release published
- [ ] Social media announcements posted
- [ ] Reddit posts submitted
- [ ] Beta testers thanked
- [ ] Week 1-4 updates posted
- [ ] 30-day retrospective completed
- [ ] v3.1.0 planning started

---

## Sign-Off

**Product Owner**: ___________________  Date: __________

**Tech Lead**: ___________________  Date: __________

**QA Lead**: ___________________  Date: __________

**Release Manager**: ___________________  Date: __________

---

**Document Version**: 1.0
**Last Updated**: 2025-11-16
**Next Review**: After Phase 2 completion (January 26, 2025)
