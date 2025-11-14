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

## **Session Handoff**

### **Next Session Prompt (M1 Sprint Plan)**

```xml
<task_description>
  <persona>Tech Lead + Sprint Planner</persona>
  <task>Créer le plan de sprint M1 détaillé (5-7 tâches) et lancer l'implémentation.</task>
  <milestone>M1: Data & Security (10j)</milestone>
  <items>#1 CSV Import UI, #2 Encryption UI, #3 Import validation, #7 Error handling</items>
</task_description>

<context_data>
  Base: DOCS/ROADMAP_3-6_WEEKS.md, backend crypto prêt (PasswordBasedCrypto.kt), BackupRepository.importZip() partiellement implémenté.
</context_data>

<detailed_instructions>
  <steps>
    <step n="1">Décomposer items M1 en 5-7 tâches techniques (2-3h chacune).</step>
    <step n="2">Identifier risques techniques (permissions SAF, crypto edge cases, CSV encoding).</step>
    <step n="3">Créer fixtures de test (3 CSV samples: basique, complexe, invalide).</step>
    <step n="4">Implémenter dans l'ordre: ImportCsvDialog UI → column mapping logic → validation → encryption dialogs.</step>
    <step n="5">Tests: unit tests pour CsvParser validation, instrumentation pour dialogs.</step>
  </steps>

  <rules>
    <rule id="R1">Maintenir CI green (lint, detekt, tests avant chaque commit).</rule>
    <rule id="R2">Chaque tâche = 1 commit avec tests.</rule>
    <rule id="R3">User feedback obligatoire (snackbar/toast sur chaque opération).</rule>
  </rules>
</detailed_instructions>

<output_format>
  <format>
    <response>
      <sprint_plan>[7 tâches avec estimation, dépendances, risques]</sprint_plan>
      <test_fixtures>[3 CSV samples + 1 encrypted ZIP]</test_fixtures>
      <implementation_order>[séquence commits avec tests]</implementation_order>
    </response>
  </format>
</output_format>
```

### **Proposed Follow-Up Actions**

| Action | Effort | Priorité | Rationale |
|--------|--------|----------|-----------|
| **Plan de sprint M1** (tâches, tests, risques) | **S** (3h) | **P0** | Débloquer implémentation immédiate |
| **Design technique** import validation (edge cases CSV) | **M** (5h) | P0 | Item le plus risqué (encodings, malformed CSV) |
| **Checklist QA/CI** pour M1 (20 scénarios) | **S** (2h) | P1 | Éviter régression CI |
| **Fixtures de test** (3 CSV + 1 ZIP encrypted) | **S** (2h) | P0 | Nécessaire pour dev + tests |
| **Spike** CameraX permissions Android 14 | **M** (4h) | P2 | Préparer M2, anticiper blocage |

---

**Document généré le:** 2025-11-14
**Auteur:** Claude Code (Product Strategist + Tech Lead)
**Durée analyse:** 1 session
**Prochaine étape:** Lancer sprint M1
