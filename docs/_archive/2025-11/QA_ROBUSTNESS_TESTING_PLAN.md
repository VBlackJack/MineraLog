# 📋 Plan de Tests de Robustesse - MineraLog

**Auteur**: QA Lead
**Date**: 2025-11-13
**Version**: 1.0
**Objectif**: Augmenter la robustesse via tests ciblés sur les zones à risque critique

---

## Executive Summary

Cette analyse identifie **52 scénarios critiques manquants** dans 3 zones à haut risque :
- **BackupRepository** (Import/Export ZIP/CSV) - 0% couverture actuelle
- **Room Migrations** (v1→v4) - 0% couverture actuelle
- **PDF Label Generation** - 0% couverture actuelle

**Impact** : Risque élevé de corruption de données utilisateur, perte lors de migrations, et vulnérabilités sécurité (ZIP bombs, path injection).

**Plan d'action** : 62 tests sur 4 semaines pour atteindre 85% couverture globale.

---

## 1. GAPS - Scénarios Critiques Manquants

### 🔴 CRITIQUE - Import/Export ZIP (BackupRepository)

**Fichier**: `app/src/main/java/net/meshcore/mineralog/data/repository/BackupRepository.kt` (586 lignes)
**Couverture actuelle**: 0% ❌

#### A. Sécurité

| # | Scénario | Ligne(s) | Risque | Priorité |
|---|----------|----------|--------|----------|
| 1 | ZIP Bomb (ratio 1:10000) | 154-163 | DoS, crash app | P0 |
| 2 | Archive ZIP corrompue/tronquée | 165-187 | Exception non gérée | P0 |
| 3 | Mauvais mot de passe décryptage | 219 | UX message erreur | P0 |
| 4 | Path injection (`../../../etc/passwd`) | 179-183 | Sécurité filesystem | P0 |
| 5 | Fichiers media invalides (fake JPG) | 130-136 | Corruption stockage | P1 |

#### B. Compatibilité de schéma

| # | Scénario | Ligne(s) | Risque | Priorité |
|---|----------|----------|--------|----------|
| 6 | Version schéma incompatible (v2.0 → v1.0) | 96 | Import échec | P0 |
| 7 | Champs manquants (backup v3 sans `currency`) | 190-201 | Crash parsing | P0 |
| 8 | Champs obsolètes (backup futur) | 58 (`ignoreUnknownKeys`) | Déjà géré ✓ | P2 |

#### C. Intégrité des données

| # | Scénario | Ligne(s) | Risque | Priorité |
|---|----------|----------|--------|----------|
| 9 | Conflits UUID (mode MERGE) | 34, 238-249 | Overwrite silencieux | P0 |
| 10 | Photos orphelines (mineralId inexistant) | 179-183 | Stockage pollué | P1 |
| 11 | Transaction rollback partiel | 230-250 | Données incohérentes | P0 |
| 12 | Collection vide (0 minéraux) | 65-66 | Déjà géré ✓ | P1 |

#### D. Performance

| # | Scénario | Ligne(s) | Objectif | Priorité |
|---|----------|----------|----------|----------|
| 13 | Import 1000 minéraux | 147-259 | < 30s, < 100MB RAM | P1 |
| 14 | Export 500 photos | 128-137 | Compression efficace | P1 |
| 15 | Annulation import (coroutine) | N/A | Pas de mécanisme ❌ | P2 |

---

### 🔴 CRITIQUE - Migrations Room

**Fichier**: `app/src/main/java/net/meshcore/mineralog/data/local/migration/Migrations.kt` (150 lignes)
**Couverture actuelle**: 0% ❌

#### A. Préservation des données

| Migration | Changements | Tests Manquants | Priorité |
|-----------|-------------|-----------------|----------|
| **1→2** | + 6 colonnes (statusType, qualityRating, provenanceId, storageId, completeness, statusDetails) | Vérifier toutes données existantes préservées | P0 |
| **2→3** | + table `filter_presets` | Table créée sans impact données existantes | P1 |
| **3→4** | + colonne `currency` | Défaut 'USD' appliqué | P1 |
| **1→4** | Chaîne complète | Migration multi-étapes 1→2→3→4 | P0 |

#### B. Valeurs par défaut

| Colonne | Valeur Défaut | Ligne | Test Manquant | Priorité |
|---------|---------------|-------|---------------|----------|
| `statusType` | 'in_collection' | 33 | Tous anciens minéraux ont cette valeur | P0 |
| `completeness` | 0 | 48 | Valeur appliquée correctement | P0 |
| `currency` | 'USD' | 142 | Provenances existantes ont USD | P1 |

#### C. Indices et contraintes

| Test | Objectif | Ligne(s) | Priorité |
|------|----------|----------|----------|
| 5 indices créés (MIGRATION_1_2) | Vérifier création sans erreur | 62-85 | P1 |
| Performance queries | Amélioration mesurable après indices | N/A | P2 |
| Pas de FK constraints | Intentionnel pour flexibilité | 87-89 | P1 |

#### D. Schémas exportés

| Test | Objectif | Ligne | Priorité |
|------|----------|-------|----------|
| Validation JSON schémas | Comparer `/app/schemas/1.json` à `4.json` | N/A | P2 |
| Compatibilité descendante | Migration inverse impossible (intentionnel) | N/A | P2 |

---

### 🟡 MOYEN - Import CSV

**Fichier**: `app/src/main/java/net/meshcore/mineralog/data/repository/BackupRepository.kt` (lignes 356-547)
**Couverture**: Parser testé ✅, Repository 0% ❌

#### A. Validation des données

| # | Validation | Ligne | Comportement Actuel | Comportement Attendu | Priorité |
|---|------------|-------|---------------------|----------------------|----------|
| 28 | Mohs < 0 ou > 10 | 485-486 | `toFloatOrNull()` accepte | Rejet avec erreur | P0 |
| 29 | Latitude > 90, Longitude > 180 | N/A | Pas de validation ❌ | Validation range | P0 |
| 30 | UUID malformé | 479 | `UUID.randomUUID()` génère nouveau | Pas de validation import ❌ | P1 |
| 31 | Float invalide (`"abc"`) | 466 | Retourne `null` silencieusement | Log warning + null | P1 |
| 32 | Date invalide | 325 | Non implémenté ❌ | Parse ISO-8601 ou skip | P2 |

#### B. Modes d'import

| Mode | Ligne | Comportement | Test Manquant | Priorité |
|------|-------|--------------|---------------|----------|
| **MERGE** | 413-418 | Update si nom existe, sinon insert | Vérifier update correct | P1 |
| **REPLACE** | 385-390 | `deleteAll()` puis import | Vérifier DB vide puis remplie | P1 |
| **SKIP_DUPLICATES** | 407-411 | Skip lignes avec nom existant | Vérifier skip + count | P1 |

#### C. Gestion erreurs

| Erreur | Ligne | Comportement | Test Manquant | Priorité |
|--------|-------|--------------|---------------|----------|
| Ligne invalide | 439 | Erreur collectée, continue parsing | Vérifier list `errors` | P1 |
| Nom vide | 398-401 | Skip ligne + erreur | Testé mais non automatisé | P0 |
| CSV vide | 372 | Exception | Vérifier message erreur | P2 |
| Headers only | N/A | 0 rows importées | Success avec count=0 | P2 |

---

### 🟡 MOYEN - Génération PDF Étiquettes

**Fichier**: `app/src/main/java/net/meshcore/mineralog/data/util/QrLabelPdfGenerator.kt` (232 lignes)
**Couverture actuelle**: 0% ❌ (QrCodeGenerator testé ✅)

#### A. Layout

| Scénario | Ligne | Test Manquant | Priorité |
|----------|-------|---------------|----------|
| Nom très long (3+ lignes) | 146 | Wrapping correct, pas de débordement | P1 |
| Formule unicode (`α-SiO₂`) | 153 | Rendu correct des caractères spéciaux | P1 |
| Groupe long (multi-lignes) | 172 | Wrapping ligne 197 `wrapText()` | P1 |
| Tous champs vides | 56-98 | Seulement ID + QR code affichés | P2 |

#### B. Multi-pages

| Labels | Pages Attendues | Ligne | Test Manquant | Priorité |
|--------|-----------------|-------|---------------|----------|
| 1 | 1 | 61 | Vérifier `totalPages = 1` | P1 |
| 8 | 1 (LABELS_PER_PAGE) | 36 | Exactement 1 page | P1 |
| 9 | 2 | 61 | Pagination correcte | P1 |
| 100 | 13 | 61 | 13 pages générées | P1 |

#### C. QR Codes

| Test | Ligne | Objectif | Priorité |
|------|-------|----------|----------|
| Lisibilité QR scannable | 125-129 | QR code lisible après impression 120x120pt | P0 |
| Format deep link | 126 | `mineralog://mineral/{uuid}` | P0 |
| Positionnement | 131-132 | Margin 20pt respecté | P2 |

#### D. Erreurs

| Erreur | Ligne | Comportement Actuel | Test Manquant | Priorité |
|--------|-------|---------------------|---------------|----------|
| Liste vide | 56 | Pas de check ❌ | Retourner `Result.failure` | P1 |
| Disque plein | 89 | Exception catchée ligne 95 ✓ | Vérifier message erreur | P2 |
| Bitmap OOM (1000 labels) | N/A | Pas de gestion ❌ | Limiter ou paginer | P2 |

---

## 2. FIXTURES - Datasets et Fixtures Minimaux

### 📦 A. Fixtures ZIP Import/Export

**Location**: `app/src/test/resources/fixtures/backups/`

#### 1. `valid_backup_unencrypted.zip`
```
Contenu:
  - manifest.json
    {
      "app": "MineraLog",
      "schemaVersion": "1.0.0",
      "exportedAt": "2025-11-13T10:00:00Z",
      "counts": { "minerals": 3, "photos": 1 },
      "encrypted": false
    }
  - minerals.json
    [
      {
        "id": "test-001",
        "name": "Quartz",
        "formula": "SiO₂",
        "group": "Silicates",
        "mohsMin": 7.0,
        "mohsMax": 7.0,
        ...
      },
      { "id": "test-002", "name": "Calcite", ... },
      { "id": "test-003", "name": "Hematite", ... }
    ]
  - media/photo_001.jpg (16x16 PNG stub)
```

#### 2. `valid_backup_encrypted.zip`
```
Contenu:
  - manifest.json
    {
      "encrypted": true,
      "encryption": {
        "algorithm": "Argon2id+AES-256-GCM",
        "salt": "base64_encoded_salt",
        "iv": "base64_encoded_iv"
      }
    }
  - minerals.json (ciphertext AES-256-GCM)

Password: "Test123!"
```

#### 3. `corrupted_zip.zip`
Archive tronquée (50% des bytes pour simuler téléchargement interrompu)

#### 4. `zip_bomb.zip`
Ratio compression 1:10000 (1KB compressé → 10MB décompressé)

#### 5. `invalid_manifest.zip`
```json
manifest.json: { "app": "MineraLog", INVALID JSON >>>
```

#### 6. `missing_minerals_json.zip`
Contenu: seulement `manifest.json`, pas de `minerals.json`

#### 7. `schema_v2_backup.zip`
```json
manifest.json: { "schemaVersion": "2.0.0" }  // Future version incompatible
```

#### 8. `path_injection.zip`
Fichier nommé: `../../../etc/passwd` (test sanitization)

#### 9. `orphan_photos.zip`
```json
minerals.json: [{ "id": "min-001", "photos": [] }]
+ media/photo_orphan.jpg (mineralId: "non-existent")
```

#### 10. `empty_collection.zip`
```json
minerals.json: []
```

---

### 📄 B. Fixtures CSV Import

**Location**: `app/src/test/resources/fixtures/csv/`

#### 1. `valid_minimal.csv`
```csv
Name,Formula
Quartz,SiO₂
Calcite,CaCO₃
```

#### 2. `valid_full.csv`
```csv
Name,Group,Formula,Mohs Min,Mohs Max,Storage Place,Notes,Tags
Quartz,Silicates,SiO₂,7.0,7.0,Cabinet A,Beautiful crystal,clear;transparent
Hematite,Oxides,Fe₂O₃,5.5,6.5,Drawer 3,Magnetic,red;metallic
```

#### 3. `invalid_mohs.csv`
```csv
Name,Mohs Min,Mohs Max
BadMineral,-5.0,15.0
```

#### 4. `invalid_coordinates.csv`
```csv
Name,Prov Latitude,Prov Longitude
BadGPS,200.0,400.0
```

#### 5. `missing_name.csv`
```csv
Name,Formula
,SiO₂
```

#### 6. `duplicate_names.csv`
```csv
Name,Formula
Quartz,SiO₂
Quartz,SiO₂
```

#### 7. `malformed_boolean.csv`
```csv
Name,Radioactive,Magnetic
Test,maybe,sometimes
```

#### 8. `unicode_content.csv`
```csv
Name,Formula,Notes
Azurite,Cu₃(CO₃)₂(OH)₂,Couleur bleue α-cristal 🔷
```

#### 9. `empty.csv`
(fichier vide)

#### 10. `headers_only.csv`
```csv
Name,Group,Formula
```

---

### 🗄️ C. Fixtures Room Migrations

**Location**: `app/src/androidTest/assets/databases/`

#### 1. `mineralog_v1.db`
- **Version**: 1
- **Schema**: `id, name, group, formula, createdAt`
- **Données**: 10 minéraux basiques

#### 2. `mineralog_v2.db`
- **Version**: 2
- **Nouvelles colonnes**: `statusType, statusDetails, qualityRating, completeness, provenanceId, storageId`
- **Données**: 10 minéraux avec nouveaux champs

#### 3. `mineralog_v3.db`
- **Version**: 3
- **Nouvelle table**: `filter_presets`
- **Données**: 5 presets de filtres

#### 4. `mineralog_v4.db` (actuel)
- **Version**: 4
- **Nouvelle colonne**: `currency` dans `provenances`
- **Données**: Dataset complet

#### 5. `mineralog_v1_populated.db`
- **Version**: 1
- **Dataset volumieux**: 100 minéraux + 50 provenances + 200 photos
- **Usage**: Tester performance migration

---

### 📑 D. Fixtures PDF (Code Kotlin)

```kotlin
// app/src/test/java/fixtures/MineralFixtures.kt

object MineralFixtures {
    val shortName = Mineral(
        id = "test-001",
        name = "Quartz",
        formula = "SiO₂",
        group = "Silicates"
    )

    val longName = Mineral(
        id = "test-002",
        name = "Potassium Aluminum Silicate Hydroxide Fluoride Complex",
        formula = "KAl₂(AlSi₃O₁₀)(F,OH)₂",
        group = "Phyllosilicates - Mica Group Minerals with Extended Classification"
    )

    val minimal = Mineral(
        id = "test-003",
        name = "Unknown",
        formula = null,
        group = null
    )

    val unicode = Mineral(
        id = "test-004",
        name = "Azurite α-crystal",
        formula = "Cu₃(CO₃)₂(OH)₂",
        group = "Carbonates"
    )

    val batch100 = (1..100).map { i ->
        Mineral(
            id = "batch-$i",
            name = "Mineral #$i",
            formula = "XYZ$i",
            group = "Test Group"
        )
    }

    val batch1000 = (1..1000).map { i ->
        Mineral(
            id = "large-$i",
            name = "Specimen $i",
            formula = "ABC$i"
        )
    }
}
```

---

## 3. TEST_PLAN - Plan de Tests Détaillé

### 📊 Vue d'ensemble

- **Total tests**: 62
- **Tests P0 (Critique)**: 25 (40%)
- **Tests P1 (Important)**: 30 (48%)
- **Tests P2 (Nice-to-have)**: 7 (11%)

### A. BackupRepository - Import/Export ZIP (20 tests)

| # | Test | Type | Fichier | Objectif | Priorité | Done |
|---|------|------|---------|----------|----------|------|
| 1 | `exportZip_unencrypted_createsValidArchive` | Unit | BackupRepositoryTest.kt | Vérifier structure ZIP (manifest + minerals.json + media) | P0 | ❌ |
| 2 | `exportZip_encrypted_requiresPassword` | Unit | BackupRepositoryTest.kt | Chiffrement AES-256-GCM avec Argon2id (ligne 89-91) | P0 | ❌ |
| 3 | `exportZip_emptyCollection_returnsFailure` | Unit | BackupRepositoryTest.kt | Tester ligne 65-66 | P1 | ❌ |
| 4 | `exportZip_withPhotos_includesMediaFiles` | Unit | BackupRepositoryTest.kt | Photos copiées dans ZIP (lignes 128-137) | P1 | ❌ |
| 5 | `exportZip_missingPhotoFile_skipsGracefully` | Unit | BackupRepositoryTest.kt | Photo référencée mais fichier absent (ligne 131) | P2 | ❌ |
| 6 | `importZip_unencrypted_importsSuccessfully` | Unit | BackupRepositoryTest.kt | Flux complet import avec fixture `valid_backup_unencrypted.zip` | P0 | ❌ |
| 7 | `importZip_encrypted_correctPassword_succeeds` | Unit | BackupRepositoryTest.kt | Décryptage avec bon mot de passe (lignes 209-220) | P0 | ❌ |
| 8 | `importZip_encrypted_wrongPassword_fails` | Unit | BackupRepositoryTest.kt | DecryptionException ligne 219, message utilisateur clair | P0 | ❌ |
| 9 | `importZip_tooLarge_rejects` | Unit | BackupRepositoryTest.kt | File size > 100MB (ligne 154-162) | P0 | ❌ |
| 10 | `importZip_corrupted_handlesGracefully` | Unit | BackupRepositoryTest.kt | ZIP tronqué/malformé avec fixture `corrupted_zip.zip` | P0 | ❌ |
| 11 | `importZip_zipBomb_protects` | Unit | BackupRepositoryTest.kt | **NOUVEAU** - Détecter ratio décompression > 100:1, rejeter | P0 | ❌ |
| 12 | `importZip_pathInjection_sanitizes` | Unit | BackupRepositoryTest.kt | **NOUVEAU** - Fichier `../../passwd` bloqué/sanitizé | P0 | ❌ |
| 13 | `importZip_modeMerge_upsertsByID` | Unit | BackupRepositoryTest.kt | Mode MERGE (ligne 34) - upsert par UUID | P1 | ❌ |
| 14 | `importZip_modeReplace_clearsDatabase` | Unit | BackupRepositoryTest.kt | Mode REPLACE (ligne 231-236) - deleteAll() appelé | P1 | ❌ |
| 15 | `importZip_modeMapIds_remapsConflicts` | Unit | BackupRepositoryTest.kt | Mode MAP_IDS (ligne 36) - nouveaux UUID générés | P1 | ❌ |
| 16 | `importZip_transactionRollback_onError` | Unit | BackupRepositoryTest.kt | Erreur ligne 245 → rollback transaction ligne 230 | P0 | ❌ |
| 17 | `importZip_schemaVersionMismatch_fails` | Unit | BackupRepositoryTest.kt | **NOUVEAU** - Manifest schemaVersion != 1.0.0 rejeté | P0 | ❌ |
| 18 | `importZip_invalidMediaFile_skips` | Unit | BackupRepositoryTest.kt | **NOUVEAU** - Fake JPG (magic bytes incorrects) skip | P1 | ❌ |
| 19 | `importZip_orphanPhotos_cleaned` | Unit | BackupRepositoryTest.kt | **NOUVEAU** - Photos sans mineralId détectées/nettoyées | P1 | ❌ |
| 20 | `importZip_1000minerals_performance` | Performance | BackupRepositoryTest.kt | < 30s, < 100MB RAM avec fixture 1000 minéraux | P1 | ❌ |

---

### B. BackupRepository - Import/Export CSV (13 tests)

| # | Test | Type | Fichier | Objectif | Priorité | Done |
|---|------|------|---------|----------|----------|------|
| 21 | `exportCsv_validData_createsRFC4180` | Unit | BackupRepositoryTest.kt | CSV conforme RFC 4180 (lignes 261-354) | P1 | ❌ |
| 22 | `exportCsv_withSpecialChars_escapesCorrectly` | Unit | BackupRepositoryTest.kt | Fonction escapeCSV ligne 549 (quotes, commas, newlines) | P1 | ❌ |
| 23 | `importCsv_minimal_imports` | Unit | BackupRepositoryTest.kt | Seulement Name + Formula avec fixture `valid_minimal.csv` | P1 | ❌ |
| 24 | `importCsv_fullColumns_imports` | Unit | BackupRepositoryTest.kt | Tous champs mappés (ligne 376) avec fixture `valid_full.csv` | P1 | ❌ |
| 25 | `importCsv_missingName_collectsError` | Unit | BackupRepositoryTest.kt | Validation ligne 398-401 avec fixture `missing_name.csv` | P0 | ❌ |
| 26 | `importCsv_invalidMohs_handled` | Unit | BackupRepositoryTest.kt | **NOUVEAU** - Mohs < 0 ou > 10 rejeté avec erreur | P0 | ❌ |
| 27 | `importCsv_invalidCoordinates_handled` | Unit | BackupRepositoryTest.kt | **NOUVEAU** - Lat > ±90 ou Lon > ±180 rejeté | P0 | ❌ |
| 28 | `importCsv_invalidFloat_setsNull` | Unit | BackupRepositoryTest.kt | toFloatOrNull() ligne 466 avec "abc" → null | P1 | ❌ |
| 29 | `importCsv_modeMerge_updatesByName` | Unit | BackupRepositoryTest.kt | Mode MERGE (ligne 413-418) - existingByName lookup | P1 | ❌ |
| 30 | `importCsv_modeReplace_clearsAll` | Unit | BackupRepositoryTest.kt | Mode REPLACE (ligne 385-390) - deleteAll() appelé | P1 | ❌ |
| 31 | `importCsv_modeSkipDuplicates_skips` | Unit | BackupRepositoryTest.kt | Mode SKIP_DUPLICATES (ligne 407-411) avec fixture `duplicate_names.csv` | P1 | ❌ |
| 32 | `importCsv_emptyFile_fails` | Unit | BackupRepositoryTest.kt | CSV vide avec fixture `empty.csv` | P2 | ❌ |
| 33 | `importCsv_headersOnly_succeeds` | Unit | BackupRepositoryTest.kt | 0 rows importées avec fixture `headers_only.csv` | P2 | ❌ |

---

### C. Room Migrations (11 tests)

| # | Test | Type | Fichier | Objectif | Priorité | Done |
|---|------|------|---------|----------|----------|------|
| 34 | `migration_1_to_2_preservesData` | Instrumented | MigrationTest.kt | Toutes colonnes conservées avec DB fixture v1 | P0 | ❌ |
| 35 | `migration_1_to_2_addsDefaultStatusType` | Instrumented | MigrationTest.kt | statusType = 'in_collection' (ligne 33) | P0 | ❌ |
| 36 | `migration_1_to_2_addsCompleteness` | Instrumented | MigrationTest.kt | completeness = 0 (ligne 48) | P0 | ❌ |
| 37 | `migration_1_to_2_createsIndices` | Instrumented | MigrationTest.kt | 5 indices créés (lignes 62-85) vérifiés via PRAGMA | P1 | ❌ |
| 38 | `migration_2_to_3_createsFilterPresets` | Instrumented | MigrationTest.kt | Table filter_presets existe (ligne 105) | P1 | ❌ |
| 39 | `migration_3_to_4_addsCurrency` | Instrumented | MigrationTest.kt | currency = 'USD' pour provenances (ligne 142) | P1 | ❌ |
| 40 | `migration_1_to_4_multiStep_succeeds` | Instrumented | MigrationTest.kt | Chaîne 1→2→3→4 complète sans perte de données | P0 | ❌ |
| 41 | `migration_1_to_4_with100minerals_preserves` | Instrumented | MigrationTest.kt | Dataset volumieux (fixture `mineralog_v1_populated.db`) | P1 | ❌ |
| 42 | `migration_noForeignKeys_verified` | Instrumented | MigrationTest.kt | PRAGMA foreign_keys = OFF (ligne 87 commentaire) | P1 | ❌ |
| 43 | `migration_schemaExport_matchesExpected` | Unit | MigrationTest.kt | Comparer JSON `/app/schemas/1.json` à `4.json` | P2 | ❌ |
| 44 | `migration_performance_1000rows` | Performance | MigrationTest.kt | Migration v1→v4 avec 1000 minerals < 5s | P2 | ❌ |

---

### D. PDF Label Generation (13 tests)

| # | Test | Type | Fichier | Objectif | Priorité | Done |
|---|------|------|---------|----------|----------|------|
| 45 | `generatePdf_singleLabel_creates1Page` | Unit | QrLabelPdfGeneratorTest.kt | 1 label = 1 page (ligne 61) | P1 | ❌ |
| 46 | `generatePdf_8labels_creates1Page` | Unit | QrLabelPdfGeneratorTest.kt | LABELS_PER_PAGE = 8 (ligne 36) | P1 | ❌ |
| 47 | `generatePdf_9labels_creates2Pages` | Unit | QrLabelPdfGeneratorTest.kt | Pagination correcte (ligne 61 `totalPages`) | P1 | ❌ |
| 48 | `generatePdf_longName_wrapsText` | Unit | QrLabelPdfGeneratorTest.kt | wrapText() ligne 197 avec fixture `longName` | P1 | ❌ |
| 49 | `generatePdf_unicodeFormula_renders` | Unit | QrLabelPdfGeneratorTest.kt | Cu₃(CO₃)₂(OH)₂ affichage correct (ligne 153) | P1 | ❌ |
| 50 | `generatePdf_minimalMineral_noFormula` | Unit | QrLabelPdfGeneratorTest.kt | Optional fields null (ligne 153-177) | P2 | ❌ |
| 51 | `generatePdf_qrCodeReadable_scans` | Integration | QrLabelPdfGeneratorTest.kt | QR scannable après génération (ZXing decode) | P0 | ❌ |
| 52 | `generatePdf_qrEncoding_correctUri` | Unit | QrLabelPdfGeneratorTest.kt | `mineralog://mineral/{uuid}` (ligne 126) | P0 | ❌ |
| 53 | `generatePdf_emptyList_fails` | Unit | QrLabelPdfGeneratorTest.kt | **NOUVEAU** - Check avant ligne 56, retourner Result.failure | P1 | ❌ |
| 54 | `generatePdf_100labels_performance` | Performance | QrLabelPdfGeneratorTest.kt | < 10s (ligne 228 estimation 100ms/label) | P1 | ❌ |
| 55 | `generatePdf_1000labels_memoryTest` | Performance | QrLabelPdfGeneratorTest.kt | **NOUVEAU** - Pas d'OOM, < 50MB heap | P2 | ❌ |
| 56 | `generatePdf_layout_margins` | Snapshot | QrLabelPdfGeneratorTest.kt | Vérifier MARGIN=20pt (ligne 43) via PDF parsing | P2 | ❌ |
| 57 | `generatePdf_layout_gridAlignment` | Snapshot | QrLabelPdfGeneratorTest.kt | 2×4 grid correct (lignes 72-82) | P2 | ❌ |

---

### E. Integration End-to-End (5 tests)

| # | Test | Type | Fichier | Objectif | Priorité | Done |
|---|------|------|---------|----------|----------|------|
| 58 | `e2e_exportImport_roundTrip` | Integration | BackupIntegrationTest.kt | Export → Import → Vérification données identiques | P0 | ❌ |
| 59 | `e2e_exportEncrypted_importDecrypted` | Integration | BackupIntegrationTest.kt | Workflow chiffrement complet avec password | P0 | ❌ |
| 60 | `e2e_csvExport_importBack` | Integration | BackupIntegrationTest.kt | CSV export → import → données identiques | P1 | ❌ |
| 61 | `e2e_migration_export_upgrade_import` | Integration | MigrationIntegrationTest.kt | v1 export → migrate v4 → import → vérification | P1 | ❌ |
| 62 | `e2e_generatePdf_scanQr_loadMineral` | Integration | PdfIntegrationTest.kt | PDF → scan QR → deep link → ouverture fiche mineral | P1 | ❌ |

---

## 4. COVERAGE_TARGET - Objectifs de Couverture

### 🎯 Objectifs Globaux

| Module | Lignes | Couverture Actuelle | Cible | Gap | Tests Requis |
|--------|--------|---------------------|-------|-----|--------------|
| **BackupRepository** | 586 | 0% ❌ | **85%** | +85% | 33 tests |
| **Migrations** | 150 | 0% ❌ | **100%** | +100% | 11 tests |
| **QrLabelPdfGenerator** | 232 | 0% ❌ | **75%** | +75% | 13 tests |
| **CsvParser** | 377 | ~90% ✅ | 95% | +5% | 3 tests |
| **QrCodeGenerator** | 112 | ~85% ✅ | 90% | +5% | 2 tests |
| **MineralDao** | 200 | ~80% ✅ | 85% | +5% | 3 tests |
| **Crypto** | 150 | ~70% ✅ | 80% | +10% | 5 tests |
| **UI Accessibility** | 500 | ~75% ✅ | 80% | +5% | 5 tests |

**Total cible globale**: **82%** (contre ~35% actuel)

---

### 📊 Détail par Zone à Risque

#### A. Import/Export ZIP (BackupRepository:61-259)

**Lignes**: 199
**Cible couverture**: 85% = ~169 lignes couvertes
**Tests requis**: 20 tests

**Métriques attendues**:
- ✅ **Branch coverage**: > 80% (tous if/when/catch couverts)
- ✅ **Exception paths**: 100% (tous les catch testés)
- ✅ **Modes import**: 100% (MERGE, REPLACE, MAP_IDS)
- ✅ **Security**: ZIP bomb, path injection, schema version

**Breakdown**:
```
exportZip (61-145):    85 lignes → 72 lignes couvertes (85%)
importZip (147-259):  113 lignes → 96 lignes couvertes (85%)
```

---

#### B. Import CSV (BackupRepository:356-547)

**Lignes**: 192
**Cible couverture**: 85% = ~163 lignes couvertes
**Tests requis**: 13 tests

**Métriques attendues**:
- ✅ **Data validation**: 100% (Mohs, coords, UUID, floats)
- ✅ **Modes import**: 100% (MERGE, REPLACE, SKIP_DUPLICATES)
- ✅ **Error collection**: 100% (erreurs par ligne)
- ✅ **Column mapping**: CsvColumnMapper déjà testé ✅

**Breakdown**:
```
importCsv (356-450):            95 lignes → 81 lignes couvertes (85%)
parseMineralFromCsvRow (455-547): 93 lignes → 79 lignes couvertes (85%)
```

---

#### C. Migrations Room (Migrations.kt:1-150)

**Lignes**: 150
**Cible couverture**: **100%** (critique, pas de tolérance)
**Tests requis**: 11 tests

**Métriques attendues**:
- ✅ **Data preservation**: 100% (aucune perte de données)
- ✅ **Default values**: 100% (toutes colonnes avec défauts vérifiés)
- ✅ **Schema validation**: JSON exports comparés
- ✅ **Performance**: < 5s pour 1000 rows

**Breakdown**:
```
MIGRATION_1_2 (28-91):  64 lignes → 64 lignes couvertes (100%)
MIGRATION_2_3 (102-127): 26 lignes → 26 lignes couvertes (100%)
MIGRATION_3_4 (137-145):  9 lignes →  9 lignes couvertes (100%)
Multi-step chains:       51 lignes → 51 lignes couvertes (100%)
```

---

#### D. PDF Labels (QrLabelPdfGenerator.kt:1-232)

**Lignes**: 232
**Cible couverture**: 75% = ~174 lignes couvertes
**Tests requis**: 13 tests

**Métriques attendues**:
- ✅ **Layout correctness**: Snapshot tests (marges, grille)
- ✅ **QR readability**: Integration avec ZXing decoder
- ✅ **Performance**: < 10s pour 100 labels, < 50MB pour 1000
- ✅ **Text wrapping**: Noms/groupes longs

**Breakdown**:
```
generate (55-98):       44 lignes → 37 lignes couvertes (85%)
drawLabel (108-187):    80 lignes → 60 lignes couvertes (75%)
wrapText (197-219):     23 lignes → 20 lignes couvertes (87%)
Helper methods:         85 lignes → 57 lignes couvertes (67%)
```

---

### 🔬 Métriques de Qualité

#### Tests Unit (JUnit 5)

**Framework**:
- JUnit 5 (Jupiter)
- kotlin.test assertions
- MockK pour mocks (Context, ContentResolver, Database)
- @TempDir pour fichiers temporaires

**Exemple test BackupRepository**:
```kotlin
@Test
fun `importZip encrypted correctPassword succeeds`() = runTest {
    // Given
    val uri = Uri.parse("content://test/valid_backup_encrypted.zip")
    val password = "Test123!".toCharArray()
    mockContentResolver.openInputStream(uri) returns
        FileInputStream(fixture("valid_backup_encrypted.zip"))

    // When
    val result = repository.importZip(uri, password, ImportMode.MERGE)

    // Then
    assertTrue(result.isSuccess)
    val importResult = result.getOrThrow()
    assertEquals(3, importResult.imported)
    assertEquals(0, importResult.skipped)
    assertTrue(importResult.errors.isEmpty())
}
```

---

#### Tests Instrumented (AndroidX Test)

**Framework**:
- AndroidX Test (AndroidJUnit4)
- Room Testing Library (`MigrationTestHelper`)
- In-memory database: `Room.inMemoryDatabaseBuilder()`
- Coroutines: `runTest` avec `TestDispatcher`

**Exemple test Migration**:
```kotlin
@Test
fun migration_1_to_2_preservesData() = runTest {
    // Given
    val db = helper.createDatabase(TEST_DB, 1).apply {
        execSQL("INSERT INTO minerals (id, name, formula) VALUES ('id1', 'Quartz', 'SiO2')")
        close()
    }

    // When
    helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

    // Then
    val migratedDb = helper.getMigrationDatabase()
    val cursor = migratedDb.query("SELECT * FROM minerals WHERE id = 'id1'")
    assertTrue(cursor.moveToFirst())
    assertEquals("Quartz", cursor.getString(cursor.getColumnIndex("name")))
    assertEquals("in_collection", cursor.getString(cursor.getColumnIndex("statusType")))
    assertEquals(0, cursor.getInt(cursor.getColumnIndex("completeness")))
}
```

---

#### Tests Performance

**Métriques**:
- **Temps exécution**: mesure via `measureTimeMillis`
- **Mémoire heap**: `Runtime.getRuntime().totalMemory() - freeMemory()`
- **Allocations**: Android Profiler pour tests complexes

**Benchmarks**:
| Opération | Seuil | Mesure Actuelle | Status |
|-----------|-------|-----------------|--------|
| Import 1000 minéraux | < 30s | ❓ Non testé | ❌ |
| PDF 100 labels | < 10s | ❓ Non testé | ❌ |
| Migration 1000 rows | < 5s | ❓ Non testé | ❌ |
| Export 500 photos | < 20s | ❓ Non testé | ❌ |

**Exemple test performance**:
```kotlin
@Test
fun `importZip 1000minerals performance`() = runTest {
    // Given
    val fixture = createLargeBackup(mineralCount = 1000)

    // When
    val duration = measureTimeMillis {
        repository.importZip(fixture.uri, null, ImportMode.REPLACE)
    }

    // Then
    assertTrue(duration < 30_000, "Import took ${duration}ms, expected < 30s")

    val memoryUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    assertTrue(memoryUsed < 100_000_000, "Used ${memoryUsed / 1_000_000}MB, expected < 100MB")
}
```

---

#### Tests Integration

**End-to-End workflows**:
1. **Export → Import round-trip**: Vérifier données identiques
2. **PDF → Scan QR → Load mineral**: Deep link fonctionnel
3. **Migration → Export → Import**: Compatibilité versions

**Exemple test E2E**:
```kotlin
@Test
fun `e2e exportImport roundTrip`() = runTest {
    // Given - Populate database
    val minerals = listOf(
        createMineral("Quartz", "SiO₂"),
        createMineral("Calcite", "CaCO₃")
    )
    database.mineralDao().insertAll(minerals)

    // When - Export
    val exportUri = Uri.parse("content://test/backup.zip")
    repository.exportZip(exportUri).getOrThrow()

    // And - Clear database
    database.clearAllTables()

    // And - Import
    repository.importZip(exportUri, mode = ImportMode.REPLACE).getOrThrow()

    // Then - Verify data identical
    val imported = database.mineralDao().getAll().first()
    assertEquals(2, imported.size)
    assertTrue(imported.any { it.name == "Quartz" && it.formula == "SiO₂" })
    assertTrue(imported.any { it.name == "Calcite" && it.formula == "CaCO₃" })
}
```

---

### 📈 Roadmap d'Implémentation

#### **Phase 1 - Critique (Semaines 1-2)** - P0

**Objectif**: Éliminer risques bloquants production

| Semaine | Tests | Couverture Cible | Livrables |
|---------|-------|------------------|-----------|
| Semaine 1 | BackupRepository Import/Export ZIP (tests #1-20) | BackupRepository: 70% | - 20 tests unit<br>- Fixtures ZIP créées<br>- Protection ZIP bomb<br>- Validation schéma version |
| Semaine 2 | Room Migrations (tests #34-44) | Migrations: 100% | - 11 tests instrumented<br>- Fixtures DB v1-v4<br>- Schema JSON exports<br>- MigrationTestHelper setup |

**Résultat Phase 1**:
- ✅ BackupRepository: 0% → **75%**
- ✅ Migrations: 0% → **100%**
- ✅ Couverture globale: ~35% → **55%**

---

#### **Phase 2 - Important (Semaine 3)** - P1

**Objectif**: Valider fonctionnalités principales

| Tâches | Tests | Couverture Cible | Livrables |
|--------|-------|------------------|-----------|
| CSV Import validation | Tests #21-33 | BackupRepository CSV: 80% | - 13 tests unit<br>- Fixtures CSV créées<br>- Validation Mohs/coords<br>- 3 modes import testés |
| PDF génération basique | Tests #45-52 | QrLabelPdfGenerator: 60% | - 10 tests unit<br>- Fixtures Mineral<br>- Layout wrapping<br>- QR readability |

**Résultat Phase 2**:
- ✅ BackupRepository: 75% → **85%**
- ✅ QrLabelPdfGenerator: 0% → **65%**
- ✅ Couverture globale: 55% → **70%**

---

#### **Phase 3 - Amélioration (Semaine 4)** - P2

**Objectif**: Performance et polish

| Tâches | Tests | Couverture Cible | Livrables |
|--------|-------|------------------|-----------|
| Performance tests | Tests #20, #44, #54, #55 | N/A | - 5 tests performance<br>- Benchmarks établis<br>- Profiling mémoire |
| Integration E2E | Tests #58-62 | N/A | - 5 tests integration<br>- Workflows complets<br>- Deep links testés |
| Snapshot tests PDF | Tests #56-57 | QrLabelPdfGenerator: 75% | - 2 tests snapshot<br>- Layout validation |
| Amélioration existants | CsvParser, QrCodeGenerator | +5% chacun | - 5 tests additionnels |

**Résultat Phase 3**:
- ✅ QrLabelPdfGenerator: 65% → **75%**
- ✅ CsvParser: 90% → **95%**
- ✅ QrCodeGenerator: 85% → **90%**
- ✅ Couverture globale: 70% → **82%**

---

### 🚀 CI/CD Integration

**GitHub Actions workflow**:

```yaml
# .github/workflows/tests.yml
name: Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run unit tests
        run: ./gradlew test
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./app/build/reports/jacoco/testDebugUnitTest/jacocoTestReport.xml
          fail_ci_if_error: true

  instrumented-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run instrumented tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          script: ./gradlew connectedDebugAndroidTest

  performance-tests:
    runs-on: ubuntu-latest
    if: github.event_name == 'schedule' # Nightly only
    steps:
      - name: Run benchmarks
        run: ./gradlew benchmark
```

**Seuils qualité**:
- ✅ **Coverage JaCoCo**: > 80% requis pour merge PR
- ✅ **Tests P0**: 100% pass obligatoire
- ✅ **Tests P1**: > 95% pass requis
- ✅ **Performance**: Aucune régression > 20%

---

## 5. Recommandations Stratégiques

### 🔒 Sécurité

1. **Ajouter validation ZIP bomb** (P0)
   - Ratio décompression > 100:1 → rejet
   - Limite taille décompressée totale: 500MB

2. **Sanitizer paths ZIP entries** (P0)
   ```kotlin
   fun sanitizePath(path: String): String {
       return path.replace("..", "_").replace("/", "_")
   }
   ```

3. **Valider magic bytes media files** (P1)
   - PNG: `89 50 4E 47`
   - JPG: `FF D8 FF`

### 📊 Performance

4. **Implémenter cancellation import/export** (P2)
   ```kotlin
   suspend fun importZip(uri: Uri, onProgress: (Int) -> Unit) {
       ensureActive() // Check cancellation
   }
   ```

5. **Ajouter pagination PDF** (P2)
   - Limite: 100 labels par appel
   - Streaming: écriture par page

### 🧪 Tests

6. **Setup MigrationTestHelper** (P0)
   - Créer fixtures DB v1-v4
   - Générer schemas JSON
   - Automatiser dans CI

7. **Benchmarking baseline** (P1)
   - Établir métriques actuelles
   - Android Studio Profiler
   - Baseline profiles

### 📝 Documentation

8. **Documenter breaking changes** (P1)
   - CHANGELOG.md avec versions schema
   - Migration guide utilisateurs
   - Backup compatibility matrix

---

## 6. Conclusion

**Impact attendu**:
- ✅ **Couverture**: +47% (35% → 82%)
- ✅ **Bugs critiques**: -52 scénarios à risque
- ✅ **Sécurité**: +4 protections (ZIP bomb, path injection, schema version, media validation)
- ✅ **Confiance**: 100% migrations testées

**Effort estimé**:
- **Phase 1** (P0): 2 semaines - 31 tests
- **Phase 2** (P1): 1 semaine - 23 tests
- **Phase 3** (P2): 1 semaine - 8 tests
- **Total**: **4 semaines** pour 62 tests

**ROI**:
- Prévention corruption données utilisateur (impact critique)
- Réduction bugs production de ~80% (zones testées)
- Accélération debugging (fixtures reproductibles)
- Confiance déploiement migrations (100% coverage)

---

**Next steps**:
1. ✅ Valider ce plan avec l'équipe
2. ⏳ Créer fixtures (semaine 1, jours 1-2)
3. ⏳ Implémenter tests P0 (semaine 1-2)
4. ⏳ Setup CI/CD avec seuils coverage (semaine 2)
5. ⏳ Tests P1 + P2 (semaines 3-4)
