# **M1 Sprint Plan: Data & Security Foundation**
**Sprint Duration:** 10 jours (2 semaines)
**Milestone:** M1 - Data & Security
**Created:** 2025-11-14
**Sprint Goal:** Débloquer import/export CSV complet + activation encryption UI

---

## **1. Sprint Overview**

### **Contexte**
- **Backend prêt:** PasswordBasedCrypto.kt (Argon2id + AES-256-GCM), BackupRepository.importCsv() et .exportCsv() implémentés
- **UI existante:** EncryptPasswordDialog, DecryptPasswordDialog, export/import ZIP encrypted dans SettingsScreen
- **Gap principal:** Aucune UI pour CSV import, column mapping, ou affichage validation errors

### **Items M1 du Roadmap**
1. **#1** CSV Import UI + column mapping
2. **#2** Encryption UI (partiellement fait, manque toggle Settings)
3. **#3** Import validation + error reporting détaillé
4. **#7** Error handling systématique (snackbars/toasts)

### **KPIs de succès M1**
- ✅ Import success rate ≥ 95% (test avec 5 CSV variés)
- ✅ Encryption round-trip success rate = 100%
- ✅ Zero silent failures (toutes erreurs loggées + UI feedback)
- ✅ Test coverage ≥ 20% (baseline: ~15%)

---

## **2. Technical Tasks Breakdown (7 tâches)**

### **Task 1: Create CSV Test Fixtures**
**Estimation:** 1-2h
**Assigné:** Claude
**Dépendances:** Aucune

**Description:**
Créer 4 fichiers CSV de test pour valider l'implémentation:
1. **`test_basic.csv`** - 10 minéraux simples, headers standards
2. **`test_complex.csv`** - 100 minéraux, tous champs remplis, accents, UTF-8 BOM, virgules dans notes
3. **`test_invalid.csv`** - 20 minéraux avec erreurs variées (mohs > 10, lat > 90, champs requis manquants)
4. **`test_encrypted_backup.zip`** - 1 ZIP avec 50 minéraux encrypted (password: "Test1234!")

**Risques:**
- ❌ Encodings non-UTF-8 (ISO-8859-1, Windows-1252) → Risque: Moyen
  - **Mitigation:** CsvParser.detectEncoding() gère déjà UTF-8/UTF-16/BOM
- ❌ Champs avec newlines et quotes échappés → Risque: Faible
  - **Mitigation:** CsvParser.parseLine() implémente RFC 4180

**Done criteria:**
- [ ] 4 fichiers dans `/app/src/test/resources/fixtures/`
- [ ] README.md décrivant chaque fixture et cas d'usage
- [ ] Fixtures référencés dans tests unitaires (Task 6)

---

### **Task 2: Implement CSV Import UI in SettingsScreen**
**Estimation:** 2-3h
**Assigné:** Claude
**Dépendances:** Task 1 (fixtures pour tester)

**Description:**
Ajouter UI pour CSV import dans `SettingsScreen.kt`:
- File picker avec `ActivityResultContracts.GetContent()` pour sélectionner CSV
- Bouton "Import CSV" dans section "Backup & Restore"
- Afficher preview des headers détectés (5 premières lignes)
- Lancer column mapping dialog (Task 3)

**Changements fichiers:**
- `app/src/main/java/net/meshcore/mineralog/ui/screens/settings/SettingsScreen.kt`
- `app/src/main/java/net/meshcore/mineralog/ui/screens/settings/SettingsViewModel.kt`

**Risques:**
- ❌ Permissions SAF (Storage Access Framework) sur Android 10+ → Risque: Moyen
  - **Mitigation:** Utiliser SAF avec `GetContent()` au lieu de permissions runtime
- ❌ Fichiers très larges (> 50 MB) causent OOM → Risque: Moyen
  - **Mitigation:** Limiter preview à 100 lignes, afficher avertissement si > 1000 lignes

**Done criteria:**
- [ ] Bouton "Import CSV" visible dans SettingsScreen
- [ ] File picker lance et accepte `.csv`
- [ ] Preview affiche headers + 5 lignes (ou "Large file: 1234 rows")
- [ ] Appel à ColumnMappingDialog avec headers détectés
- [ ] Snackbar "Import started..." affiché

---

### **Task 3: Create ColumnMappingDialog**
**Estimation:** 3h
**Assigné:** Claude
**Dépendances:** Task 2

**Description:**
Créer dialog Compose pour mapping manuel des colonnes CSV:
- Liste des headers CSV détectés
- Dropdown par header pour sélectionner champ domain (`name`, `group`, `mohs`, etc.)
- Auto-mapping avec `CsvColumnMapper.mapHeaders()` pré-rempli
- Bouton "Skip" pour colonnes non mappées
- Preview 3 lignes avec mapping appliqué

**Nouveau fichier:**
- `app/src/main/java/net/meshcore/mineralog/ui/components/ColumnMappingDialog.kt`

**Risques:**
- ❌ Headers ambigus ("Hardness" → `mohs` ou `mohsMin`?) → Risque: Moyen
  - **Mitigation:** UI affiche suggestion CsvColumnMapper + dropdown permet override manuel
- ❌ Users mappent wrong columns (ex: "Country" → `name`) → Risque: Faible
  - **Mitigation:** Validation affichera erreurs ligne par ligne (Task 4)

**Done criteria:**
- [ ] Dialog affiche tous headers CSV
- [ ] Dropdowns pré-remplis avec auto-mapping
- [ ] Preview 3 lignes avec valeurs mappées
- [ ] Bouton "Import" déclenche `viewModel.importCsv(uri, columnMapping)`
- [ ] Bouton "Cancel" ferme dialog

---

### **Task 4: Implement Import Validation Error Display**
**Estimation:** 2h
**Assigné:** Claude
**Dépendances:** Task 2, 3

**Description:**
Afficher `ImportResult` avec erreurs détaillées ligne par ligne:
- AlertDialog avec liste scrollable d'erreurs
- Format: `"Row 42: Mohs hardness 15.0 exceeds max (10.0)"`
- Statistiques en header: "✅ Imported: 145 | ⚠️ Skipped: 5"
- Bouton "Copy to clipboard" pour partager erreurs

**Changements fichiers:**
- `app/src/main/java/net/meshcore/mineralog/ui/components/ImportResultDialog.kt` (nouveau)
- `SettingsViewModel.kt` - exposer `importResultState`

**Risques:**
- ❌ Liste très longue d'erreurs (> 500 lignes) → Risque: Faible
  - **Mitigation:** LazyColumn avec virtualisation, limiter à 100 premières erreurs affichées

**Done criteria:**
- [ ] Dialog affiché après import avec statistiques
- [ ] Liste scrollable d'erreurs (si > 0 errors)
- [ ] Bouton "Copy to clipboard" fonctionnel
- [ ] Snackbar success si 0 errors: "✅ 145 minerals imported successfully"

---

### **Task 5: Add CSV Export UI**
**Estimation:** 1h
**Assigné:** Claude
**Dépendances:** Aucune (backend existe)

**Description:**
Ajouter bouton CSV export dans SettingsScreen:
- Launcher `CreateDocument("text/csv")`
- Appel `viewModel.exportCsv(uri)`
- Snackbar success/error

**Changements fichiers:**
- `SettingsScreen.kt`
- `SettingsViewModel.kt` - ajouter `exportCsv()` function

**Done criteria:**
- [ ] Bouton "Export CSV" dans section Backup
- [ ] File picker avec nom par défaut `mineralog_export_YYYYMMDD.csv`
- [ ] Export réussit avec 100 minéraux test
- [ ] Snackbar: "✅ CSV exported: 100 minerals"

---

### **Task 6: Add 'Encrypt Backups by Default' Toggle**
**Estimation:** 1-2h
**Assigné:** Claude
**Dépendances:** Aucune

**Description:**
Ajouter toggle Settings pour activer encryption par défaut:
- Switch "Encrypt backups by default" dans SettingsScreen
- Sauvegarder dans DataStore Preferences
- Utiliser valeur dans export flows (ZIP + CSV si on ajoute encryption CSV plus tard)

**Changements fichiers:**
- `SettingsScreen.kt` - ajouter Switch
- `SettingsRepository.kt` - ajouter `encryptByDefault: Flow<Boolean>`
- `SettingsViewModel.kt` - exposer setting

**Risques:**
- ❌ Users oublient password et perdent données → Risque: Critique
  - **Mitigation:** Warning dialog: "⚠️ Password recovery impossible. Store password safely."

**Done criteria:**
- [ ] Toggle visible dans Settings sous "Copy photos to internal storage"
- [ ] Valeur sauvegardée dans DataStore
- [ ] Export ZIP utilise encryption si toggle activé (sauf si user clique "Skip password")
- [ ] Warning dialog affiché au premier toggle ON

---

### **Task 7: Write Unit Tests for CSV Import Validation**
**Estimation:** 2-3h
**Assigné:** Claude
**Dépendances:** Task 1 (fixtures)

**Description:**
Tests unitaires pour validation CSV:
- `CsvParserTest.kt` - parsing edge cases (quotes, newlines, UTF-8 BOM)
- `CsvColumnMapperTest.kt` - auto-mapping avec headers variés
- `BackupRepositoryTest.kt` - `importCsv()` avec fixtures (basic, complex, invalid)

**Nouveaux fichiers:**
- `app/src/test/java/net/meshcore/mineralog/data/util/CsvParserTest.kt`
- `app/src/test/java/net/meshcore/mineralog/data/util/CsvColumnMapperTest.kt`
- `app/src/test/java/net/meshcore/mineralog/data/repository/BackupRepositoryTest.kt` (augmenter tests existants)

**Done criteria:**
- [ ] 15+ tests unitaires pour CsvParser
- [ ] 10+ tests pour CsvColumnMapper
- [ ] 8+ tests pour BackupRepository.importCsv()
- [ ] All tests pass avec `./gradlew test`
- [ ] Test coverage ≥ 20% (mesuré avec JaCoCo)

---

## **3. Technical Risks & Mitigations**

| Risque | Probabilité | Impact | Mitigation |
|--------|-------------|--------|------------|
| **CSV encoding issues** (non-UTF-8) | Moyen | Moyen | CsvParser.detectEncoding() gère UTF-8/16/BOM. Ajouter test avec ISO-8859-1. |
| **SAF permissions denied** | Faible | Haut | Utiliser `GetContent()` (no permissions needed). Snackbar actionable si erreur. |
| **Large CSV files OOM** (> 100 MB) | Faible | Moyen | Limiter preview à 100 lignes. Warning si > 10,000 lignes. |
| **Users forget encryption password** | Moyen | Critique | Warning dialog obligatoire au toggle ON. Docs sur password recovery = impossible. |
| **Malformed CSV breaks parser** | Moyen | Faible | CsvParser collecte errors sans crash. ImportResult affiche erreurs. |
| **Column mapping ambiguïté** | Moyen | Faible | UI permet override manuel. Preview 3 lignes pour vérifier. |
| **Crypto edge cases** (empty password, wrong IV) | Faible | Haut | Unit tests round-trip avec fixtures. PasswordBasedCrypto déjà testé. |

---

## **4. Implementation Order & Commits**

Chaque tâche = **1 commit** avec tests (Rule R2).

```
Commit 1: feat: add CSV test fixtures (basic, complex, invalid, encrypted ZIP)
  - Files: app/src/test/resources/fixtures/*.csv
  - Tests: None (fixtures only)

Commit 2: feat: add CSV import UI in SettingsScreen with file picker
  - Files: SettingsScreen.kt, SettingsViewModel.kt
  - Tests: Manual testing with test_basic.csv
  - Snackbar: "Import started..."

Commit 3: feat: add ColumnMappingDialog with auto-detection and manual override
  - Files: ui/components/ColumnMappingDialog.kt
  - Tests: Manual testing with preview
  - Snackbar: None (dialog only)

Commit 4: feat: add ImportResultDialog with error list and statistics
  - Files: ui/components/ImportResultDialog.kt, SettingsViewModel.kt
  - Tests: Manual with test_invalid.csv
  - Snackbar: "✅ 145 imported | ⚠️ 5 skipped"

Commit 5: feat: add CSV export UI in SettingsScreen
  - Files: SettingsScreen.kt, SettingsViewModel.kt
  - Tests: Manual export → import round-trip
  - Snackbar: "✅ CSV exported: 100 minerals"

Commit 6: feat: add 'Encrypt backups by default' toggle in Settings
  - Files: SettingsScreen.kt, SettingsRepository.kt, SettingsViewModel.kt
  - Tests: Manual toggle + export
  - Snackbar: None (warning dialog)

Commit 7: test: add unit tests for CSV parser, mapper, and import validation
  - Files: CsvParserTest.kt, CsvColumnMapperTest.kt, BackupRepositoryTest.kt
  - Tests: 30+ unit tests
  - CI: ./gradlew test (must pass)
```

---

## **5. Test Plan**

### **Unit Tests (Task 7)**
- **CsvParserTest:** 15 tests (encoding detection, delimiter detection, quoted fields, newlines, malformed CSV)
- **CsvColumnMapperTest:** 10 tests (auto-mapping, fuzzy matching, suggestions, normalization)
- **BackupRepositoryTest:** 8 tests (importCsv MERGE/REPLACE/SKIP modes, validation errors, round-trip)

### **Manual Testing Checklist**
| Scénario | Fixture | Expected Result | Status |
|----------|---------|-----------------|--------|
| Import basic CSV (10 minerals) | `test_basic.csv` | ✅ 10 imported, 0 errors | ⬜ |
| Import complex CSV (100 minerals, UTF-8 BOM) | `test_complex.csv` | ✅ 100 imported, 0 errors | ⬜ |
| Import invalid CSV (20 minerals, errors) | `test_invalid.csv` | ⚠️ ~15 imported, ~5 errors shown | ⬜ |
| Import encrypted ZIP (50 minerals) | `test_encrypted_backup.zip` | ✅ 50 imported after password | ⬜ |
| Export CSV → Import round-trip | Export 100 → Import | ✅ 100 minerals identical | ⬜ |
| Column mapping auto-detection | `test_basic.csv` (headers: "Name", "Hardness") | ✅ Auto-mapped to `name`, `mohs` | ⬜ |
| Column mapping manual override | Select wrong column → fix in dialog | ✅ Preview updates, import succeeds | ⬜ |
| Large CSV file (> 1000 lines) | Generate 5000 lines | ⚠️ Warning: "Large file, preview limited" | ⬜ |
| Toggle "Encrypt by default" ON | Export ZIP | ✅ Password dialog appears | ⬜ |
| Wrong password on import | Import encrypted ZIP | ❌ Error: "Wrong password. 2 attempts remaining" | ⬜ |

### **Instrumentation Tests (Optional, M2)**
- CSV import UI flow (file picker → column mapping → result dialog)
- Encryption toggle → export → import round-trip

---

## **6. Definition of Done (Sprint M1)**

### **Functional Criteria**
- ✅ CSV import UI avec sélection fichier (SAF) fonctionnel
- ✅ Column mapping auto-détecté + manuel override
- ✅ Import réussit avec 100 minéraux test (fixtures)
- ✅ Validation affiche erreurs ligne par ligne (ex: "Ligne 42: hardness invalide '12' (max 10)")
- ✅ CSV export UI fonctionnel
- ✅ Settings toggle "Encrypt backups by default" fonctionnel
- ✅ Export ZIP encrypted → import ZIP décrypté round-trip succès
- ✅ Snackbars/toasts sur toutes opérations async (import, export, delete)

### **Quality Criteria (Rule R1)**
- ✅ CI green (lint, detekt, tests)
- ✅ Detekt violations = 0 (MaxLineLength, etc.)
- ✅ Test coverage ≥ 20% (baseline: ~15%)
- ✅ Zero P0 bugs (blocking workflows)

### **Documentation**
- ✅ Fixtures README.md avec descriptions
- ✅ CHANGELOG.md updated avec features M1
- ✅ README.md features list updated (remove "planned" tags)

---

## **7. Session Handoff for Next Task**

### **After M1 Sprint Plan Creation**
```xml
<task_description>
  <persona>Android Developer</persona>
  <task>Implement Task 1: Create CSV test fixtures.</task>
  <details>Create 4 CSV files in app/src/test/resources/fixtures/</details>
</task_description>
```

### **Progress Tracking**
- **Task 1:** ⬜ CSV test fixtures
- **Task 2:** ⬜ CSV import UI
- **Task 3:** ⬜ ColumnMappingDialog
- **Task 4:** ⬜ ImportResultDialog
- **Task 5:** ⬜ CSV export UI
- **Task 6:** ⬜ Encrypt toggle
- **Task 7:** ⬜ Unit tests

---

**Sprint created:** 2025-11-14
**Owner:** Claude Code (Tech Lead)
**Status:** Ready to start Task 1
**Next action:** Create CSV test fixtures
