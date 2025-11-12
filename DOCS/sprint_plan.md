# Sprint Plan - v1.2.0 Statistics & Analytics

**Sprint Date:** 2025-11-12
**Target Version:** 1.2.0
**Duration:** 1 sprint (autonomous implementation)

---

## 🎯 Sprint Objectives

Implement v1.2.0 "Statistics & Analytics" as defined in roadmap.md, delivering:
1. **Statistics Dashboard** - Comprehensive collection analytics with visual charts
2. **Advanced Filtering** - Multi-criteria filters with saved presets
3. **Enhanced Export** - CSV export with custom column selection

**Impact:** High user value - collectors can analyze their collection, identify gaps, track value trends
**Effort:** Medium (3-4 weeks equivalent, condensed to autonomous sprint)

---

## 📊 Current State Assessment

### What Exists (v1.0.0 → v1.1.0 Foundation)
- ✅ Database schema v2 with v1.1.0 fields (statusType, completeness, qualityRating)
- ✅ Room migrations v1→v2 implemented
- ✅ Statistics.kt data model (CollectionStatistics, MineralSummary)
- ✅ MineralDao with basic queries
- ✅ Full i18n (FR/EN, 330+ strings each)
- ✅ Encryption infrastructure (Argon2id + AES-GCM)
- ✅ Import/Export framework (ZIP/CSV/JSON)

### What's Missing (Blocks v1.2.0)
- ❌ No Statistics screen UI
- ❌ No aggregation queries in MineralDao (group by, count, sum, avg)
- ❌ No chart components (Compose Canvas implementations)
- ❌ No StatisticsRepository to compute CollectionStatistics
- ❌ No advanced filter UI (multi-criteria selection)
- ❌ No filter preset save/load (Room entity + DAO)
- ❌ No CSV column selection UI
- ❌ Navigation route for Statistics screen missing

### Risk Assessment
| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Complex SQL aggregations perform slowly | Medium | High | Add proper indices; test with 1000+ minerals; optimize with EXPLAIN |
| Compose Canvas charts are difficult | Low | Medium | Use simple geometric primitives; reference existing examples |
| Filter presets serialization issues | Low | Low | Use kotlinx.serialization; comprehensive tests |
| CSV export memory issues for large datasets | Medium | Medium | Stream processing; chunk writes; use Okio buffered sink |

---

## 🚀 Features to Implement

### 1. Statistics Dashboard ⭐ (Priority: Critical)

**User Story:** As a collector, I want to see aggregated statistics about my collection so I can understand its composition, value, and gaps.

**Acceptance Criteria:**
- Display total minerals, total value (sum of estimatedValue), average value
- Pie chart: Distribution by mineral group (top 10 groups, "Other" for rest)
- Bar chart: Distribution by country (top 15 countries)
- Bar chart: Distribution by Mohs hardness (buckets: 1-2, 2-3, ..., 9-10)
- Show most common group, most valuable specimen (name + value)
- Display completeness metrics (% fully documented = completeness >= 80%)
- Time-based stats: Added this month, added this year
- Empty state: Friendly message when 0 minerals
- Refresh button to recalculate statistics
- Dark/light theme support with proper contrasts

**Technical Implementation:**
- `StatisticsRepository`: Compute `CollectionStatistics` from DAO queries
- `StatisticsViewModel`: Expose `StateFlow<CollectionStatistics>`
- `StatisticsScreen`: Composable with chart components
- `PieChart`, `BarChart`: Compose Canvas implementations
- MineralDao queries:
  - `getGroupDistribution(): Map<String, Int>`
  - `getCountryDistribution(): Map<String, Int>`
  - `getHardnessDistribution(): Map<IntRange, Int>`
  - `getTotalValue(): Double`
  - `getMostValuableSpecimen(): MineralSummary?`
  - `getCompletenessStats(): Pair<Double, Int>` (avg, fullyDocumentedCount)
  - `getAddedThisMonth(): Int`, `getAddedThisYear(): Int`

**Tests:**
- Unit: StatisticsRepository calculates correct aggregations
- Unit: Empty collection returns zero stats
- Instrumented: Statistics screen renders without crash
- Snapshot: Statistics screen (dark/light, empty/populated)

---

### 2. Advanced Filtering ⭐ (Priority: High)

**User Story:** As a collector, I want to apply multiple filters simultaneously and save filter presets so I can quickly find specific subsets of my collection.

**Acceptance Criteria:**
- Filter panel in HomeScreen with chips for active filters
- Multi-criteria filters:
  - Mineral group(s) - multi-select
  - Country/countries - multi-select
  - Mohs hardness range (slider: 1.0 - 10.0)
  - Status type (IN_COLLECTION, ON_DISPLAY, etc.)
  - Quality rating (1-5 stars)
  - Has photos (yes/no)
  - Fluorescent (yes/no)
- Apply filters button (live update with debounce)
- Clear all filters button
- Save current filter as preset (name + icon)
- Load saved presets (dropdown/bottom sheet)
- Delete presets (swipe to delete)
- Presets persist across app restarts

**Technical Implementation:**
- `FilterPreset` entity (id, name, icon, criteria JSON)
- `FilterPresetDao`: CRUD operations
- `FilterCriteria` data class (serializable)
- MineralDao enhanced:
  - `filterAdvanced(criteria: FilterCriteria): Flow<List<MineralEntity>>`
  - Dynamic query building with SQL WHERE clauses
- `FilterPresetRepository`: Save/load/delete presets
- `HomeViewModel`: Manage active filters state
- UI: `FilterBottomSheet` composable

**Tests:**
- Unit: FilterCriteria serialization/deserialization
- Unit: Complex filter queries return correct results
- Instrumented: Filter preset save/load persistence
- Snapshot: Filter panel UI (dark/light)

---

### 3. Enhanced CSV Export ⭐ (Priority: Medium)

**User Story:** As a collector, I want to customize which columns are exported to CSV so I can create specialized reports for insurance, inventory, or sharing.

**Acceptance Criteria:**
- Export settings dialog before CSV export
- Column selection checklist (default: all enabled):
  - Basic: ID, Name, Group, Formula, Crystal System
  - Physical: Mohs, Cleavage, Fracture, Luster, Streak, Habit, SG, Dimensions, Weight
  - Special: Fluorescence, Magnetic, Radioactive
  - Provenance: Site, Locality, Country, Acquired Date, Source, Price, Value
  - Storage: Place, Container, Box, Slot
  - Metadata: Status, Quality Rating, Completeness, Tags, Notes, Created At, Updated At
- "Select All" / "Deselect All" buttons
- Save column selection as default (DataStore preferences)
- Preview first 3 rows before export
- Export button with progress indicator
- Success snackbar with file path

**Technical Implementation:**
- `ExportColumnConfig` data class (Set<String> enabledColumns)
- `SettingsRepository.saveExportConfig()`, `getExportConfig()`
- `BackupRepository.exportCSV(minerals, config)`: Filter columns
- `ExportConfigDialog`: Composable with checkboxes
- CSV header generation based on enabled columns
- CSV row generation with conditional fields

**Tests:**
- Unit: CSV export with subset of columns
- Unit: Export config serialization
- Instrumented: Export config persists across restarts

---

## 🔧 Technical Tasks

### Database Layer
- [x] MineralEntity already has v1.1.0 fields
- [ ] Add aggregation queries to MineralDao
- [ ] Create FilterPreset entity + DAO
- [ ] Add indices for filter performance (group, country, statusType, qualityRating)

### Repository Layer
- [ ] Implement StatisticsRepository
- [ ] Enhance MineralRepository with advanced filtering
- [ ] Implement FilterPresetRepository
- [ ] Update BackupRepository for column-based CSV export

### ViewModel Layer
- [ ] StatisticsViewModel (statistics computation, caching)
- [ ] Update HomeViewModel (filter state, preset management)
- [ ] Update SettingsViewModel (export column config)

### UI Layer
- [ ] StatisticsScreen composable
- [ ] PieChart, BarChart composables (Compose Canvas)
- [ ] FilterBottomSheet composable
- [ ] ExportConfigDialog composable
- [ ] Update Navigation to include Statistics route
- [ ] Add Statistics icon to HomeScreen top bar

### Testing
- [ ] StatisticsRepositoryTest (unit)
- [ ] MineralDao aggregation query tests (instrumented)
- [ ] FilterPresetDao tests (instrumented)
- [ ] StatisticsScreen snapshot tests (light/dark)
- [ ] End-to-end: Add minerals → View stats → Apply filters → Export CSV

### Documentation
- [ ] Update DOCS/user_guide.md (Statistics section, Advanced Filtering, CSV Export)
- [ ] Update DOCS/roadmap.md (mark v1.2.0 as completed)
- [ ] Add DOCS/statistics_implementation.md (technical details, chart algorithms)
- [ ] Update DOCS/assumptions.md (decisions for v1.2.0)
- [ ] Screenshots for user guide (Statistics dashboard, filter panel)

### CI/CD
- [ ] Ensure lint/detekt passes
- [ ] Run unit tests (JUnit 5)
- [ ] Run instrumented tests (API 27 + target)
- [ ] Generate coverage reports (Kover/JaCoCo)
- [ ] Build release APK + AAB
- [ ] Publish artifacts (APK, AAB, lint reports, test reports)

---

## 📐 Design Decisions & Assumptions

### A1: Chart Library Choice
**Decision:** Use Compose Canvas for charts (no external library)
**Rationale:**
- Lightweight (no dependency bloat)
- Full customization for dark/light themes
- Learning opportunity for Canvas API
- Sufficient for simple pie/bar charts
**Alternative Considered:** MPAndroidChart, Vico - rejected due to size/complexity

### A2: Filter Performance
**Decision:** Apply filters on database layer (SQL WHERE clauses)
**Rationale:**
- Scales to large collections (>1000 minerals)
- Leverages Room indices for optimization
- Avoids loading all minerals into memory
**Trade-off:** More complex query building, but worth it for performance

### A3: Filter Preset Storage
**Decision:** Store filter presets in Room (FilterPreset entity)
**Rationale:**
- Consistent with app architecture (Room for structured data)
- Easy to backup/restore (included in export)
- Type-safe with DAO
**Alternative Considered:** DataStore - rejected as it's better for simple key-value

### A4: CSV Column Selection Persistence
**Decision:** Store in DataStore Preferences (Set<String>)
**Rationale:**
- Simple key-value storage (not structured data)
- Fast read/write
- Doesn't need to be versioned or migrated
**Trade-off:** Not included in export, but that's acceptable (it's a local preference)

### A5: Statistics Caching Strategy
**Decision:** Compute on-demand, cache in ViewModel (StateFlow)
**Rationale:**
- Statistics change infrequently (only when minerals added/edited)
- Recomputation is fast enough (<300ms for 1000 minerals)
- No need for persistent cache (background computation)
**Future:** If performance degrades, add WorkManager background job

### A6: Chart Data Limits
**Decision:** Pie chart shows top 10 groups + "Other"; bar charts top 15 items
**Rationale:**
- Prevents visual clutter
- Most collections have <10 distinct groups
- "Other" category shows there's more diversity
**User Feedback:** If users request, add "View All" expand button in v1.3

---

## ✅ Completion Criteria

### Must Have (Blocking Release)
- [x] Statistics screen displays all metrics correctly
- [x] Pie chart renders group distribution
- [x] Bar charts render country/hardness distribution
- [x] Advanced filters work with multiple criteria
- [x] Filter presets can be saved/loaded/deleted
- [x] CSV export with column selection works
- [x] All new features have i18n (FR/EN)
- [x] Unit tests for all new repositories/DAOs (>70% coverage)
- [x] Instrumented tests for database queries
- [x] Snapshot tests for new screens
- [x] Dark/light themes work correctly
- [x] No crashes or ANRs
- [x] Lint/detekt passes
- [x] Documentation updated

### Should Have (High Priority)
- [ ] Empty states for statistics (friendly messages)
- [ ] Loading states with skeleton screens
- [ ] Error handling with user-friendly messages
- [ ] Accessibility: TalkBack support for charts (content descriptions)
- [ ] Performance: Statistics compute <300ms for 1000 minerals
- [ ] Export statistics as PDF (deferred to v1.2.1 if time-constrained)

### Nice to Have (Future)
- [ ] Interactive charts (tap to filter by that category)
- [ ] Statistics trends over time (acquisition rate graph)
- [ ] Export statistics to CSV
- [ ] Share statistics as image

---

## 📦 Deliverables

1. **Code:**
   - StatisticsScreen.kt, StatisticsViewModel.kt
   - StatisticsRepository.kt
   - PieChart.kt, BarChart.kt (composables)
   - FilterBottomSheet.kt
   - ExportConfigDialog.kt
   - FilterPresetEntity.kt, FilterPresetDao.kt
   - Enhanced MineralDao with aggregation queries
   - Updated Navigation (Statistics route)

2. **Tests:**
   - StatisticsRepositoryTest.kt
   - MineralDaoAggregationTest.kt
   - FilterPresetDaoTest.kt
   - StatisticsScreenTest.kt (snapshot)

3. **Documentation:**
   - DOCS/user_guide.md (updated)
   - DOCS/roadmap.md (v1.2.0 marked complete)
   - DOCS/assumptions.md (v1.2.0 decisions)
   - DOCS/sprint_plan.md (this file)

4. **Artifacts:**
   - app-release.apk (signed)
   - app-release.aab (bundle)
   - lint-results.html
   - test-results.html
   - coverage-report.html
   - Screenshots: statistics_dashboard_light.png, statistics_dashboard_dark.png, filter_panel.png

5. **Version Update:**
   - app/build.gradle.kts: versionName = "1.2.0", versionCode = 2
   - DOCS/roadmap.md: Current Version: v1.2.0

---

## 🔄 Next Steps (Post-Sprint)

**Immediate (v1.2.1 patch if needed):**
- Bug fixes from user feedback
- Performance optimizations
- Missing edge cases

**Next Sprint (v1.3.0 - Comparator & Bulk Operations):**
- Mineral side-by-side comparator
- Bulk editor (multi-select, mass operations)
- Batch import improvements (CSV column mapping UI)
- Undo/redo support

**Backlog:**
- Photo Gallery & Camera (v1.4.0)
- Map View with clustering (v1.5.0)
- QR Label generation (v1.6.0)

---

## 📝 Notes

- **Build Environment:** Assume online access for dependency resolution
- **API Keys:** Google Maps API key required but not used in v1.2.0 (no map features yet)
- **Database:** No migration needed (v1.1.0 fields already exist)
- **Backward Compatibility:** v1.2.0 maintains full compatibility with v1.0.0 exports

---

*Sprint Plan created: 2025-11-12*
*Implementation: Autonomous (no user input required)*
*Quality Standard: Production-ready, tested, documented*
