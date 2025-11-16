# **MineraLog — Roadmap Priorisée 3-6 Semaines**
**Version actuelle:** 1.4.1 | **DB:** v4 | **Date:** 2025-11-14
**Contexte:** Post-stabilisation CI, performance 10x améliorée, 87 bugs corrigés

---

## **1. État des Lieux**

### **Forces ✅**
- **Architecture solide**: Clean Architecture + MVVM, séparation data/domain/ui claire
- **Stack moderne**: Kotlin 2.0, Compose, Coroutines, Room 2.6.1, Material 3
- **CI/CD fonctionnel**: Pipeline Android (lint, tests, instrumentation, build) stabilisé après effort récent
- **Performance récente**: Optimisation batch queries (10x plus rapide), queries parallèles (70% plus rapide statistiques)
- **Fonctionnalités v1.1-1.4**: CRUD complet, statistiques dashboard, filtres avancés, comparateur, export CSV UI, backend crypto prêt (Argon2id + AES-256-GCM)
- **DB bien conçue**: Migrations propres (v1→v4), indices optimisés, schéma complet (25+ champs minéral)
- **i18n partielle**: EN/FR pour features implémentées

### **Faiblesses ⚠️**
- **Gap fonctionnel**: README promet 90% features non implémentées (caméra, maps, QR scan/gen complet, encryption UI)
- **Test coverage faible**: ~15% (tests unitaires OK, instrumentation minimale, pas de tests UI Compose)
- **CI fragile historiquement**: Vient d'être stabilisé (3 PRs consécutives de fixes Nov 13-14)
- **Import CSV manquant**: Export existe, import non (backend partiellement prêt)
- **Encryption UI absente**: Backend crypto fonctionnel mais aucune UI pour activer
- **Photo management incomplet**: Entités existent, UI capture/gallery manquante
- **DI manuel**: Factories manuelles au lieu de Hilt (maintenabilité réduite)

### **Risques 🔴**
- **Promesses non tenues**: Users s'attendent à encryption fonctionnelle (docs), mais UI manquante → frustration
- **CI instabilité**: Historique de flakiness, risque de régression
- **Manque de tests**: Refactoring dangereux, risque de régression silencieuse
- **Import absent**: Users peuvent exporter mais pas réimporter facilement → lock-in partiel
- **Roadmap irréaliste**: Docs prévoient v1.5 en Q2 2026 (photo gallery) alors que basics manquent

### **Quick Wins 🚀**
1. **CSV Import UI** (backend 60% prêt, manque UI + column mapping) — 3-4j
2. **Encryption UI** (backend 100% prêt, manque 2 dialogs) — 2-3j
3. **QR Scan** (ML Kit déjà intégré, manque screen + deep link handler) — 2-3j
4. **Photo Capture** (CameraX intégré, manque Composable) — 3-4j
5. **Error toasts/snackbars** (beaucoup de silent failures) — 1-2j

---

## **2. Thèmes Stratégiques (4 thèmes)**

| # | Thème | Rationale |
|---|-------|-----------|
| **T1** | **Data Completeness** | Finaliser import/export (CSV import, validation, error reporting) → users peuvent migrer données |
| **T2** | **Security Activation** | Activer encryption UI existante → délivrer promesse docs, différenciation marché |
| **T3** | **Core UX Gaps** | Photo capture/gallery basics, QR scan, error feedback → app utilisable quotidiennement |
| **T4** | **Quality Foundations** | Tests (40%+ coverage), CI stability monitoring, edge cases → confiance refactoring |

---

## **3. Items Priorisés (9 items)**

| # | Item | Thème | Impact | Effort | Dépendances |
|---|------|-------|--------|--------|-------------|
| **1** | CSV Import UI + column mapping | T1 | **H** | **M** (5-6j) | None |
| **2** | Encryption UI (password dialogs + settings toggle) | T2 | **H** | **S** (2-3j) | None |
| **3** | Import validation + error reporting détaillé | T1 | **H** | **S** (2-3j) | #1 |
| **4** | Photo capture UI (CameraX Composable) | T3 | **M** | **M** (4-5j) | None |
| **5** | Photo gallery viewer (swipe, fullscreen) | T3 | **M** | **S** (3j) | #4 |
| **6** | QR code scanning + deep links | T3 | **M** | **S** (2-3j) | None |
| **7** | Error handling systématique (snackbars, retry) | T3 | **M** | **S** (2j) | None |
| **8** | Tests coverage → 40% (Repository, ViewModel, UI) | T4 | **M** | **M** (4-5j) | None |
| **9** | CI monitoring dashboard (build time, flaky tests) | T4 | **L** | **S** (1j) | None |

**Effort:** S=1-3j, M=4-6j, L=7-10j
**Impact:** H=déblocage users critique, M=amélioration UX notable, L=nice-to-have

---

## **4. Jalons (3 milestones)**

### **M1: Data & Security (Semaines 1-2, 10j)**
**Objectif:** Débloquer import/export complet + encryption
**Items:** #1, #2, #3, #7 (partiellement)

**Critères de done:**
- [ ] CSV import UI avec sélection fichier (SAF)
- [ ] Column mapping auto-détecté + manuel override
- [ ] Import réussit avec 100 minéraux test (fixtures)
- [ ] Validation affiche erreurs ligne par ligne (ex: "Ligne 42: hardness invalide '12' (max 10)")
- [ ] Encryption dialog (password + confirmation + strength meter)
- [ ] Settings toggle "Encrypt backups" fonctionnel
- [ ] Export ZIP encrypted → import ZIP décrypté round-trip succès
- [ ] Snackbars/toasts sur toutes opérations async (import, export, delete)

**KPIs M1:**
- Import success rate ≥ 95% (test avec 5 CSV variés)
- Encryption round-trip success rate = 100%
- Zero silent failures (toutes erreurs loggées + UI feedback)

---

### **M2: Photo Workflows (Semaines 3-4, 10j)**
**Objectif:** Activer capture/gestion photos basiques
**Items:** #4, #5, #6, #8 (partiellement)

**Critères de done:**
- [ ] CameraX Composable intégré dans MineralDetailScreen
- [ ] Capture photo → sauvegarde dans PhotoEntity avec type (Normal/UV/Macro/Context)
- [ ] Gallery viewer: grille 3×N avec thumbnails
- [ ] Tap photo → fullscreen avec swipe horizontal
- [ ] QR scanner screen (ML Kit) accessible depuis HomeScreen FAB
- [ ] Scan QR `mineralapp://mineral/{uuid}` → navigate to detail
- [ ] Deep link handler enregistré dans AndroidManifest
- [ ] 30+ tests unitaires ajoutés (BackupRepository import, PhotoDao, QrScanner utils)

**KPIs M2:**
- Photo capture réussie sur API 27 & 35 (instrumentation tests)
- QR scan latency < 500ms (benchmark)
- Test coverage ≥ 30% (étape intermédiaire)

---

### **RC: Polish & Release Candidate (Semaines 5-6, 8j)**
**Objectif:** Quality gate avant v1.5.0 release
**Items:** #8 (finir), #9, polish pass

**Critères de done:**
- [ ] Test coverage ≥ 40% (unit + instrumentation)
- [ ] Zero P0 bugs (blocking user workflows)
- [ ] CI build time < 15 min (actuellement ~12 min, surveiller)
- [ ] Detekt violations = 0 (déjà le cas, maintenir)
- [ ] Accessibility audit: TalkBack navigation fonctionne sur 5 screens principaux
- [ ] README mis à jour: features claims = implemented features uniquement
- [ ] CHANGELOG.md v1.5.0 draft
- [ ] APK release signé (debug keystore OK pour RC, prod keystore pour GA)

**KPIs RC:**
- CI green streak ≥ 10 runs consécutifs
- Manual QA checklist 100% (20 scenarios critiques)
- Lighthouse accessibility score ≥ 85 (Android a11y scanner)

---

## **5. KPIs Cibles (6 semaines)**

| KPI | Baseline (v1.4.1) | Cible M1 | Cible M2 | Cible RC |
|-----|-------------------|----------|----------|----------|
| **CSV import success rate** | 0% (n/a) | ≥95% | ≥98% | ≥99% |
| **Test coverage** | ~15% | 20% | 30% | **40%** |
| **CI build time** | 12 min | <15 min | <15 min | **<15 min** |
| **P0 bugs production** | 0 (peu users) | 0 | 0 | **0** |
| **Encryption adoption** | 0% (UI manquante) | Mesurable | 10% exports | **20% exports** |
| **Photo uploads/mineral** | 0 (UI manquante) | n/a | Mesurable | **Avg 1.5** |
| **CI green rate** | 60% (historique flaky) | 80% | 90% | **≥95%** |

---

## **6. Dépendances & Risques**

### **Dépendances Techniques**
- **Aucune circulaire** ✅ (items séquencés proprement)
- **#3 dépend de #1** (validation nécessite import flow)
- **#5 dépend de #4** (gallery viewer nécessite photos capturées)

### **Risques Majeurs**

| Risque | Probabilité | Impact | Mitigation |
|--------|-------------|--------|------------|
| CI régression (flaky tests) | **Moyen** | Haut | CI monitoring (#9), run 3× avant merge |
| Encryption bugs (perte données) | Faible | **Critique** | Tests exhaustifs round-trip, backup non-encrypted par défaut |
| CameraX permissions Android 14+ | Moyen | Moyen | Test sur API 27, 30, 33, 35 en instrumentation |
| Scope creep (ajouter maps, NFC, etc.) | **Moyen** | Moyen | **Strict scope freeze, roadmap locked** |

---

## **7. Hors Scope (Report à v1.6+)**

- ❌ Google Maps integration (roadmap Q3 2026)
- ❌ QR label PDF generation (roadmap Q4 2026, scan seulement en v1.5)
- ❌ Photo editing (crop, rotate, filters) — v1.6+
- ❌ Hilt migration (tech debt OK pour l'instant)
- ❌ Cloud sync (v2.0+)
- ❌ Batch photo upload (single upload uniquement)

---

## **Session Handoff — Prompts Structurés**

### **📋 Instructions Générales**

Pour chaque session de sprint, copier-coller le prompt XML correspondant ci-dessous. À la fin de chaque sprint, le document de session doit générer le prompt pour le sprint suivant.

**Progression:** M1 → M2 → RC → (optionnel) Rétrospective

---

### **🎯 Sprint M1: Data & Security**

#### **Prompt Session M1**

```xml
<task_description>
  <persona>Tech Lead + Sprint Engineer</persona>
  <task>Implémenter le sprint M1 (Data & Security) avec 4 items prioritaires.</task>
  <milestone>M1: Data & Security</milestone>
  <duration>10 jours (semaines 1-2)</duration>
  <items>
    <item id="1">CSV Import UI + column mapping (M, 5-6j)</item>
    <item id="2">Encryption UI (password dialogs + settings toggle) (S, 2-3j)</item>
    <item id="3">Import validation + error reporting détaillé (S, 2-3j, dépend de #1)</item>
    <item id="7">Error handling systématique (snackbars, retry) (S, 2j)</item>
  </items>
</task_description>

<context_data>
  <roadmap>DOCS/ROADMAP_3-6_WEEKS.md</roadmap>
  <changelog>CHANGELOG.md (version actuelle: 1.4.1)</changelog>
  <architecture>ARCHITECTURE.md</architecture>
  <backend_ready>
    <file>app/src/main/java/net/meshcore/mineralog/data/crypto/PasswordBasedCrypto.kt</file>
    <file>app/src/main/java/net/meshcore/mineralog/data/repository/BackupRepository.kt (importZip partiellement implémenté)</file>
    <file>app/src/main/java/net/meshcore/mineralog/data/util/CsvParser.kt</file>
  </backend_ready>
  <existing_ui>
    <file>app/src/main/java/net/meshcore/mineralog/ui/screens/home/ImportCsvDialog.kt (basic structure)</file>
    <file>app/src/main/java/net/meshcore/mineralog/ui/screens/home/EncryptPasswordDialog.kt (stub)</file>
    <file>app/src/main/java/net/meshcore/mineralog/ui/screens/home/DecryptPasswordDialog.kt (stub)</file>
  </existing_ui>
</context_data>

<detailed_instructions>
  <phase name="Planning (1h)">
    <step n="1">Lire DOCS/ROADMAP_3-6_WEEKS.md section M1</step>
    <step n="2">Décomposer 4 items en 7-9 tâches techniques (2-4h chacune)</step>
    <step n="3">Identifier dépendances: #3 après #1, #7 en parallèle</step>
    <step n="4">Lister risques: CSV encodings (UTF-8/Latin1), SAF permissions, crypto edge cases</step>
    <step n="5">Créer checklist M1 (8 critères de done de la roadmap)</step>
  </phase>

  <phase name="Fixtures (2h)">
    <step n="6">Créer 3 CSV samples en app/src/test/resources/:
      - valid_basic.csv (10 rows, colonnes standard)
      - valid_complex.csv (100 rows, tous champs, UTF-8 avec accents)
      - invalid_malformed.csv (erreurs: hardness>10, dates invalides, missing required fields)
    </step>
    <step n="7">Créer 1 ZIP encrypted test en app/src/test/resources/:
      - backup_encrypted.zip (password: "Test1234!", 5 minerals)
    </step>
  </phase>

  <phase name="Implementation (6-7j)">
    <step n="8">Item #2 (Quick Win): Encryption UI
      - Implémenter EncryptPasswordDialog (password + confirm + strength meter)
      - Implémenter DecryptPasswordDialog (password + error state)
      - Ajouter Settings toggle "Encrypt backups" (DataStore)
      - Intégrer dans HomeViewModel.exportZip() et importZip()
      - Tests: PasswordStrengthTest, encryption round-trip test
    </step>
    <step n="9">Item #7 (Foundation): Error handling
      - Créer sealed class OperationResult&lt;T&gt; (Success, Error avec message)
      - Wrapper toutes opérations async (import, export, delete)
      - Snackbar component réutilisable avec retry action
      - Tests: error state propagation
    </step>
    <step n="10">Item #1: CSV Import UI
      - Implémenter SAF file picker dans ImportCsvDialog
      - Ajouter column mapping UI (auto-detect + manual override)
      - Preview 5 premières lignes avant import
      - Progress indicator pendant import
      - Tests: UI state machine, column mapping logic
    </step>
    <step n="11">Item #3: Import validation
      - Implémenter CsvValidator avec règles (hardness 1-10, required fields, date formats)
      - Error reporting détaillé (ligne + colonne + erreur)
      - UI: afficher erreurs dans scrollable list
      - Option: continuer avec rows valides ou abort
      - Tests: validation rules (15+ test cases)
    </step>
  </phase>

  <phase name="Testing (1-2j)">
    <step n="12">Unit tests:
      - CsvParserTest (encodings, malformed)
      - CsvValidatorTest (15 validation rules)
      - PasswordBasedCryptoTest (round-trip)
      - BackupRepositoryTest (import with validation)
    </step>
    <step n="13">Instrumentation tests:
      - ImportCsvDialogTest (file picker, column mapping)
      - EncryptPasswordDialogTest (strength meter, validation)
      - End-to-end: export encrypted → import decrypted
    </step>
  </phase>

  <phase name="Validation M1 (1j)">
    <step n="14">Vérifier 8 critères de done M1 (voir roadmap)</step>
    <step n="15">Mesurer KPIs:
      - Import success rate ≥95% (tester 5 CSV)
      - Encryption round-trip success rate = 100%
      - Zero silent failures (vérifier logs)
    </step>
    <step n="16">CI green check (lint, detekt, tests)</step>
    <step n="17">Créer DOCS/M1_SPRINT_SUMMARY.md avec résultats</step>
  </phase>
</detailed_instructions>

<rules>
  <rule id="R1">Maintenir CI green — lint, detekt, tests avant chaque commit</rule>
  <rule id="R2">Chaque item = 1+ commits avec tests unitaires</rule>
  <rule id="R3">User feedback obligatoire — snackbar/toast sur toute opération async</rule>
  <rule id="R4">Pas de scope creep — si hors M1 items, créer ticket pour M2/RC</rule>
  <rule id="R5">Documentation — commenter edge cases, documenter validation rules</rule>
</rules>

<output_format>
  <deliverables>
    <deliverable>DOCS/M1_SPRINT_SUMMARY.md — résultats sprint, KPIs, blockers résolus</deliverable>
    <deliverable>7-9 commits sur branch claude/sprint-m1-*</deliverable>
    <deliverable>Test coverage +5-10% (20% → 25-30%)</deliverable>
    <deliverable>3 CSV fixtures + 1 ZIP encrypted en test/resources</deliverable>
    <deliverable>Prompt session M2 (copier template ci-dessous)</deliverable>
  </deliverables>
</output_format>

<next_session_prompt>
  À la fin du sprint M1, générer le prompt M2 en remplaçant:
  - milestone: "M2: Photo Workflows"
  - items: #4 (Photo capture), #5 (Gallery viewer), #6 (QR scan), #8 (Tests)
  - context_data: ajouter résultats M1 (KPIs atteints, blockers)
  - deliverables: DOCS/M2_SPRINT_SUMMARY.md
</next_session_prompt>
```

---

### **📸 Sprint M2: Photo Workflows**

#### **Prompt Session M2** (à utiliser après M1)

```xml
<task_description>
  <persona>Tech Lead + Sprint Engineer</persona>
  <task>Implémenter le sprint M2 (Photo Workflows) avec 4 items.</task>
  <milestone>M2: Photo Workflows</milestone>
  <duration>10 jours (semaines 3-4)</duration>
  <items>
    <item id="4">Photo capture UI (CameraX Composable) (M, 4-5j)</item>
    <item id="5">Photo gallery viewer (swipe, fullscreen) (S, 3j, dépend de #4)</item>
    <item id="6">QR code scanning + deep links (S, 2-3j)</item>
    <item id="8">Tests coverage → 40% (Repository, ViewModel, UI) (M, 4-5j, parallèle)</item>
  </items>
</task_description>

<context_data>
  <roadmap>DOCS/ROADMAP_3-6_WEEKS.md</roadmap>
  <m1_results>DOCS/M1_SPRINT_SUMMARY.md — KPIs M1, lessons learned</m1_results>
  <changelog>CHANGELOG.md (version actuelle: 1.4.1, passer à 1.5.0-rc1 après M2)</changelog>
  <backend_ready>
    <file>app/src/main/java/net/meshcore/mineralog/ui/components/PhotoManager.kt (partial)</file>
    <file>app/src/main/java/net/meshcore/mineralog/data/local/entity/PhotoEntity.kt</file>
    <file>app/src/main/java/net/meshcore/mineralog/data/local/dao/PhotoDao.kt</file>
  </backend_ready>
  <dependencies>
    <dependency>CameraX 1.4.1 (déjà dans build.gradle)</dependency>
    <dependency>ML Kit Barcode Scanning (déjà dans build.gradle)</dependency>
    <dependency>Coil 2.7.0 pour image loading (déjà dans build.gradle)</dependency>
  </dependencies>
</context_data>

<detailed_instructions>
  <phase name="Planning (1h)">
    <step n="1">Lire DOCS/ROADMAP_3-6_WEEKS.md section M2</step>
    <step n="2">Review M1 results — identifier risques/patterns à éviter</step>
    <step n="3">Décomposer 4 items en 8-10 tâches techniques</step>
    <step n="4">Identifier risques: CameraX permissions Android 14, storage permissions, QR scan latency</step>
  </phase>

  <phase name="Implementation (7-8j)">
    <step n="5">Item #6 (Quick Win): QR Scanning
      - Créer QrScannerScreen composable (ML Kit integration)
      - Implémenter deep link handler (mineralapp://mineral/{uuid})
      - Ajouter route navigation + AndroidManifest intent-filter
      - Tests: QR decode, deep link routing
    </step>
    <step n="6">Item #4: Photo Capture
      - Créer CameraXComposable avec preview + capture button
      - Gérer permissions (CAMERA, WRITE_EXTERNAL_STORAGE si API<29)
      - Photo type selector (Normal/UV SW/UV LW/Macro)
      - Intégrer dans MineralDetailScreen
      - Tests: permission flow, capture success
    </step>
    <step n="7">Item #5: Gallery Viewer
      - Créer PhotoGridView (LazyVerticalGrid 3 colonnes)
      - FullscreenPhotoViewer avec HorizontalPager (swipe)
      - Zoom/pinch gestures (optional si temps)
      - Delete photo action
      - Tests: grid layout, swipe navigation
    </step>
    <step n="8">Item #8: Test Coverage
      - Ajouter 40+ unit tests (Repositories, ViewModels)
      - Ajouter 10+ instrumentation tests (UI flows critiques)
      - Viser 40% coverage global (JaCoCo report)
    </step>
  </phase>

  <phase name="Validation M2 (1j)">
    <step n="9">Vérifier 7 critères de done M2 (voir roadmap)</step>
    <step n="10">Mesurer KPIs:
      - Photo capture réussie sur API 27 & 35
      - QR scan latency < 500ms
      - Test coverage ≥ 30% (cible intermédiaire)
    </step>
    <step n="11">Créer DOCS/M2_SPRINT_SUMMARY.md</step>
  </phase>
</detailed_instructions>

<rules>
  <rule id="R1">Maintenir CI green (hérité de M1)</rule>
  <rule id="R2">Permissions: graceful degradation si refusées</rule>
  <rule id="R3">Performance: photo capture < 2s, QR scan < 500ms</rule>
  <rule id="R4">Accessibility: CameraX avec contentDescription, QR scan avec haptic feedback</rule>
</rules>

<output_format>
  <deliverables>
    <deliverable>DOCS/M2_SPRINT_SUMMARY.md</deliverable>
    <deliverable>8-10 commits sur branch claude/sprint-m2-*</deliverable>
    <deliverable>Test coverage 30-35%</deliverable>
    <deliverable>Prompt session RC (copier template ci-dessous)</deliverable>
  </deliverables>
</output_format>

<next_session_prompt>
  À la fin du sprint M2, générer le prompt RC en remplaçant:
  - milestone: "RC: Polish & Release Candidate"
  - items: #8 (finir coverage 40%), #9 (CI monitoring), polish pass
  - context_data: ajouter résultats M1+M2
</next_session_prompt>
```

---

### **✨ Sprint RC: Polish & Release Candidate**

#### **Prompt Session RC** (à utiliser après M2)

```xml
<task_description>
  <persona>Tech Lead + QA Engineer</persona>
  <task>Finaliser RC v1.5.0 avec quality gates et polish.</task>
  <milestone>RC: Polish & Release Candidate</milestone>
  <duration>8 jours (semaines 5-6)</duration>
  <focus>Quality, stabilité, documentation, release prep</focus>
</task_description>

<context_data>
  <roadmap>DOCS/ROADMAP_3-6_WEEKS.md</roadmap>
  <m1_results>DOCS/M1_SPRINT_SUMMARY.md</m1_results>
  <m2_results>DOCS/M2_SPRINT_SUMMARY.md</m2_results>
  <changelog>CHANGELOG.md (préparer v1.5.0)</changelog>
</context_data>

<detailed_instructions>
  <phase name="Test Coverage Finalization (3j)">
    <step n="1">Compléter coverage 40%:
      - Ajouter tests manquants (ViewModels, edge cases)
      - Instrumentation tests pour flows critiques
      - JaCoCo report validation
    </step>
  </phase>

  <phase name="CI Monitoring (1j)">
    <step n="2">Implémenter CI dashboard (item #9):
      - Script analyse build times (parse GitHub Actions logs)
      - Identifier flaky tests (run history analysis)
      - Rapport dans DOCS/CI_HEALTH_REPORT.md
    </step>
  </phase>

  <phase name="Accessibility Audit (1j)">
    <step n="3">TalkBack testing sur 5 screens:
      - HomeScreen, AddMineralScreen, MineralDetailScreen, SettingsScreen, StatisticsScreen
      - Corriger semantic properties manquantes
      - Vérifier touch targets 48×48dp
    </step>
  </phase>

  <phase name="Polish Pass (2j)">
    <step n="4">Bug fixes P1/P2 identifiés en M1/M2</step>
    <step n="5">UI polish: animations, empty states, loading states</step>
    <step n="6">Performance: profiling, optimizations si nécessaire</step>
  </phase>

  <phase name="Release Prep (1j)">
    <step n="7">Mettre à jour README (features = implemented only)</step>
    <step n="8">CHANGELOG.md v1.5.0 complet</step>
    <step n="9">Version bump 1.4.1 → 1.5.0 (versionCode 7 → 8)</step>
    <step n="10">Release APK signé (si prod keystore dispo, sinon debug)</step>
  </phase>

  <phase name="Validation RC (1j)">
    <step n="11">Vérifier 8 critères de done RC (voir roadmap)</step>
    <step n="12">Mesurer KPIs RC:
      - CI green streak ≥ 10 runs
      - Manual QA checklist 100%
      - Accessibility score ≥ 85
    </step>
    <step n="13">Créer DOCS/RC_VALIDATION_REPORT.md</step>
  </phase>
</detailed_instructions>

<rules>
  <rule id="R1">Zero P0 bugs avant release</rule>
  <rule id="R2">Documentation à jour (README = reality)</rule>
  <rule id="R3">CI doit être stable (≥95% green rate)</rule>
</rules>

<output_format>
  <deliverables>
    <deliverable>DOCS/RC_VALIDATION_REPORT.md</deliverable>
    <deliverable>CHANGELOG.md v1.5.0</deliverable>
    <deliverable>README.md mis à jour</deliverable>
    <deliverable>Release APK v1.5.0</deliverable>
    <deliverable>Tag git v1.5.0</deliverable>
  </deliverables>
</output_format>
```

---

### **📊 Suivi Inter-Sprints**

À la fin de chaque sprint, créer un document `DOCS/MX_SPRINT_SUMMARY.md` avec:

```markdown
# Sprint MX Summary

**Milestone:** MX: [Nom]
**Dates:** YYYY-MM-DD → YYYY-MM-DD
**Items complétés:** #1, #2, #3...

## KPIs Atteints
- [KPI 1]: [Valeur] (cible: [X])
- [KPI 2]: [Valeur] (cible: [Y])

## Commits
- [hash] [message]
- ...

## Blockers Résolus
- [Blocker 1]: [Solution]

## Lessons Learned
- [Lesson 1]
- [Lesson 2]

## Risques Identifiés pour MX+1
- [Risque 1]: [Mitigation]

## Prompt Session Suivante
[Copier le prompt du sprint suivant ici]
```

---

**Document généré le:** 2025-11-14
**Auteur:** Claude Code (Product Strategist + Tech Lead)
**Durée analyse:** 1 session
**Prochaine étape:** Lancer sprint M1 avec prompt ci-dessus
