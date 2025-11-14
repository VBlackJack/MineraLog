# Prompts d'Implémentation — Corrections Audit MineraLog

Ce document contient les prompts prêts à l'emploi pour implémenter les corrections identifiées dans l'audit profond.

---

## 📋 SESSION 1 — Corrections P0 (Critiques, 3-4 jours)

### Prompt Session P0

```xml
<task_description>
  <persona>Staff Software Engineer (Sécurité + Performances)</persona>
  <task>Implémenter les 5 corrections P0 critiques identifiées dans l'audit MineraLog. Ordre: transactions → Argon2 → tests crypto → DB encryption → N+1 paging. Tests obligatoires pour chaque correction. Itérer jusqu'à CI vert complet.</task>
  <tone>Opérationnel, autonome, zéro question.</tone>
</task_description>

<context_data>
Audit complet disponible dans le repo. Résumé des 5 corrections P0:

**P0.1 - Fixer Chiffrement Argon2** (BLOQUEUR SÉCURITÉ)
- Fichier: app/src/main/java/net/meshcore/mineralog/data/crypto/Argon2Helper.kt
- Problème: Lignes 78-80 retournent ByteArray(32) all-zeros au lieu de dériver clé via Argon2kt
- Cause: API Argon2kt incompatible, code commenté lignes 63-75
- Solution: Fixer appel argon2.hash() avec bons params OU downgrade lib si breaking change
- Impact: Tous backups chiffrés actuellement vulnérables (clé nulle)
- Tests requis: PasswordBasedCryptoTest doit passer, round-trip encrypt/decrypt

**P0.2 - Chiffrer Base de Données Room** (BLOQUEUR SÉCURITÉ)
- Fichier: app/src/main/java/net/meshcore/mineralog/data/local/MineraLogDatabase.kt
- Problème: Ligne 56-64, Room.databaseBuilder sans encryption
- Solution:
  * Ajouter SQLCipher: implementation("net.zetetic:android-database-sqlcipher:4.5.4")
  * Wrapper avec SupportFactory(passphrase.toByteArray())
  * Générer passphrase depuis Android Keystore
- Impact: PII (prix, géoloc, noms) protégées at-rest
- Migration: Script pour convertir DB existante (si users ont data)
- Tests requis: Migration tests, round-trip, vérifier DB pas lisible raw

**P0.3 - Ajouter Transactions Repository** (INTÉGRITÉ DONNÉES)
- Fichier: app/src/main/java/net/meshcore/mineralog/data/repository/MineralRepository.kt
- Problème: Lignes 66-96, insert/update/delete/deleteByIds SANS @Transaction
- Solution: Wrapper chaque méthode avec database.withTransaction { ... }
- Impact: Atomicité garantie, pas d'orphelins provenance/storage/photos
- Tests requis: Test rollback sur erreur, test atomicité multi-insert

**P0.4 - Fixer N+1 Paging** (PERFORMANCES CRITIQUE)
- Fichier: app/src/main/java/net/meshcore/mineralog/data/repository/MineralRepository.kt
- Problème: Lignes 287-349, getAllPaged/searchPaged/filterAdvancedPaged font N queries
- Solution: Créer MineralPagingSource custom qui batch-load related entities AVANT mapping
- Impact: 10x faster (3000ms → 300ms page load)
- Tests requis: Performance test, vérifier data correcte, pas de N+1 (Database Inspector)

**P0.5 - Tests Crypto Manquants** (VALIDATION SÉCURITÉ)
- Fichiers: app/src/test/java/net/meshcore/mineralog/data/crypto/
- Problème: Argon2Helper.kt et CryptoHelper.kt NON TESTÉS
- Solution:
  * Argon2HelperTest: salt length, iterations, memory cost, parallelism, key length
  * CryptoHelperTest: IV unique, tag verification, wrong key rejection, key derivation
  * Edge cases: empty password, long password, salt reuse, IV reuse
- Tests requis: 20+ tests, coverage >90% sur crypto module
</context_data>

<detailed_instructions>
  <steps>
    <step n="1">TRANSACTION WRAPPER (2h, fondation)
      - Modifier MineralRepositoryImpl méthodes insert/update/delete/deleteByIds
      - Wrapper avec database.withTransaction { }
      - Écrire tests atomicité: insérer mineral+provenance, fail provenance → rollback mineral
      - Vérifier aucun orphelin créé
    </step>

    <step n="2">FIX ARGON2 (4h, bloqueur backup)
      - Analyser Argon2kt 1.3.0 API changes (check changelog/docs)
      - Option A: Fixer appel argon2.hash() avec nouvelle signature
      - Option B: Si API breaking irréparable, downgrade à version stable
      - Décommenter lignes 63-75, adapter params
      - Vérifier: 128MB memory, 4 iterations, parallelism 2, output 32 bytes
      - Run PasswordBasedCryptoTest → doit passer
      - Test manuel: créer backup chiffré, déchiffrer avec bon/mauvais password
    </step>

    <step n="3">TESTS CRYPTO (6h, validation P0.2)
      - Créer Argon2HelperTest.kt:
        * testDeriveKeyWithValidPassword()
        * testDeriveKeyGeneratesUniqueSalts()
        * testDeriveKeyWithSameSaltProducesSameKey()
        * testVerifyPasswordCorrect()
        * testVerifyPasswordIncorrect()
        * testEmptyPasswordThrowsException()
        * testSaltLength()
        * testKeyLength()
      - Créer CryptoHelperTest.kt:
        * testEncryptDecryptRoundTrip()
        * testIvIsUnique()
        * testWrongKeyFailsDecryption()
        * testTamperedCiphertextFailsDecryption()
        * testTamperedIvFailsDecryption()
        * testEmptyPlaintextHandled()
      - Vérifier coverage crypto module >90%
    </step>

    <step n="4">CHIFFRER DB ROOM (8h, migration critique)
      - Ajouter dépendance SQLCipher dans build.gradle.kts
      - Modifier MineraLogDatabase companion:
        * Générer passphrase depuis Android Keystore (MasterKey.Builder)
        * Wrapper builder avec SupportFactory
      - Créer script migration DB existante → DB chiffrée
      - Tests:
        * Créer DB, insérer data, fermer, rouvrir → data accessible
        * Vérifier fichier DB raw non lisible (pas de texte clair)
        * Test migration: DB v4 non-chiffrée → v4 chiffrée
      - Doc: DOCS/assumptions.md → stratégie migration users existants
    </step>

    <step n="5">FIX N+1 PAGING (6h, perf)
      - Créer MineralPagingSource.kt custom PagingSource:
        * Dans load(), récupérer entities page
        * Batch load: provenanceDao.getByMineralIds(ids)
        * Batch load: storageDao.getByMineralIds(ids)
        * Batch load: photoDao.getByMineralIds(ids)
        * Map avec associateBy { it.mineralId }
        * Retourner Page avec minerals mappés
      - Modifier MineralRepository.getAllPaged/searchPaged/filterAdvancedPaged
      - Tests:
        * Performance test: page 20 items <300ms
        * Vérifier Database Inspector: pas de N+1
        * Test data correcte: provenance/storage/photos présents
      - Mesurer avec Profiler avant/après
    </step>

    <step n="6">CI VALIDATION
      - ./gradlew clean lint detekt testDebugUnitTest
      - Vérifier aucune régression couverture
      - ./gradlew connectedDebugAndroidTest (si émulateur dispo)
      - ./gradlew assembleRelease
      - Tous jobs doivent passer vert
    </step>

    <step n="7">COMMIT & PUSH
      - Commit atomique par correction (5 commits)
      - Messages: "fix(security): restore Argon2 key derivation", etc.
      - Push vers branche courante
      - Documenter assumptions dans DOCS/assumptions.md
    </step>
  </steps>

  <rules>
    <rule id="R1">AUCUNE question utilisateur. Décisions autonomes documentées.</rule>
    <rule id="R2">Tests OBLIGATOIRES avant commit. CI doit être vert.</rule>
    <rule id="R3">Si Argon2kt API bloquée >2h, downgrade lib et documente.</rule>
    <rule id="R4">Migration DB: tester sur fixtures 100 minerals, vérifier intégrité.</rule>
    <rule id="R5">Performance: mesurer AVANT/APRÈS avec Profiler ou logs timestamps.</rule>
  </rules>

  <output_format>
    <constraint>Rapport technique détaillé avec preuves (diffs, logs CI, metrics).</constraint>
    <format>
      <response>
        <summary>Résumé 5 corrections P0 implémentées</summary>
        <changeset>
          <!-- Pour chaque correction P0.1 à P0.5 -->
          <correction id="P0.x">
            <files_modified>[liste fichiers avec lignes]</files_modified>
            <diff_key_changes>[extraits diffs critiques]</diff_key_changes>
            <tests_added>[liste tests avec assertions clés]</tests_added>
            <evidence>[logs tests passés, metrics perf, screenshots]</evidence>
          </correction>
        </changeset>
        <ci_status>
          <lint>✅ PASS</lint>
          <detekt>✅ PASS</detekt>
          <unit_tests>✅ PASS (X tests, Y coverage)</unit_tests>
          <instrumentation>✅ PASS (si lancé)</instrumentation>
          <build_release>✅ SUCCESS</build_release>
        </ci_status>
        <performance_metrics>
          <paging>AVANT: 3000ms | APRÈS: 280ms | ✅ 10.7x faster</paging>
          <crypto>Argon2 derivation: ~450ms (acceptable)</crypto>
        </performance_metrics>
        <residual_risks>
          [Liste items P1/P2 à traiter ensuite]
        </residual_risks>
        <assumptions_documented>
          [Résumé DOCS/assumptions.md: stratégie migration DB, choix Argon2 params, etc.]
        </assumptions_documented>
      </response>
    </format>
  </output_format>
</detailed_instructions>

<acceptance_criteria>
  <criterion id="AC1">✅ Backups chiffrés déchiffrables avec bon password, rejetés si mauvais</criterion>
  <criterion id="AC2">✅ Base de données fichier non lisible raw (vérifier avec hexdump)</criterion>
  <criterion id="AC3">✅ Insert mineral échoue atomiquement si provenance fails (test rollback)</criterion>
  <criterion id="AC4">✅ Page load 20 minerals <300ms (log timestamps ou Profiler)</criterion>
  <criterion id="AC5">✅ Database Inspector: aucune query N+1 visible pendant scroll</criterion>
  <criterion id="AC6">✅ Coverage crypto module >90% (JaCoCo report)</criterion>
  <criterion id="AC7">✅ CI complet vert (lint + detekt + tests + build)</criterion>
  <criterion id="AC8">✅ 5 commits atomiques pushés avec messages clairs</criterion>
</acceptance_criteria>
```

---

## 📋 SESSION 2 — Corrections P1 Sécurité (1 semaine)

### Prompt Session P1-Security

```xml
<task_description>
  <persona>Security Engineer + Android Developer</persona>
  <task>Implémenter les 6 corrections P1 sécurité: deep links validation, release signing, backup disabled, network security config, tests ViewModels critiques, CI coverage gates. Tests obligatoires. CI vert.</task>
  <tone>Opérationnel, autonome, sécurité-first.</tone>
</task_description>

<context_data>
Corrections P1 sécurité (6 items):

**P1.1 - Valider Deep Links** (1h)
- Fichiers: MainActivity.kt:30, ui/navigation/MineraLogNavHost.kt:64-67
- Problème: intent.data.lastPathSegment accepté sans validation UUID
- Solution: try { UUID.fromString(id) } catch → log error, ignore navigation
- Tests: test deep link valide, malformé, injection tentative

**P1.2 - Configurer Signing Release** (2h + CI setup)
- Fichier: app/build.gradle.kts:54-66
- Problème: Release uses debug keystore (public key)
- Solution:
  * Générer release.keystore avec keytool
  * Stocker dans GitHub Secrets (RELEASE_KEYSTORE_BASE64, PASSWORD, ALIAS, KEY_PASSWORD)
  * CI: decode base64 → keystore file
  * build.gradle: read from env vars
- Tests: vérifier signature APK release ≠ debug

**P1.3 - Désactiver Android Backup** (30min)
- Fichier: AndroidManifest.xml:24
- Problème: allowBackup=true + DB non-chiffré (risque si P0.2 pas fait)
- Solution: android:allowBackup="false"
- Alternative: si P0.2 fait, garder allowBackup mais exclude DB règles strictes
- Tests: vérifier adb backup rejeté

**P1.4 - Network Security Config** (1h)
- Créer: app/src/main/res/xml/network_security_config.xml
- Configurer: cleartextTrafficPermitted="false"
- Manifest: android:networkSecurityConfig="@xml/network_security_config"
- Tests: vérifier HTTP bloqué (si test network calls)

**P1.5 - Tests ViewModels Critiques (6h, subset)
- Prioriser: SettingsViewModel (backup/restore), EditMineralViewModel (validation)
- Tests minimum:
  * SettingsViewModel: exportBackup success/error, importBackup modes
  * EditMineralViewModel: save validation, photo add/delete, draft state
- Coverage target: >70% sur ces 2 ViewModels

**P1.6 - CI Coverage Gates** (3h)
- Fichiers: .github/workflows/ci.yml, app/build.gradle.kts
- Ajouter job JaCoCo:
  * Generate coverage report: jacocoTestReport
  * Verify threshold: jacocoTestCoverageVerification (60%)
  * Upload to Codecov (optionnel)
  * Fail CI si <60%
- Tests: vérifier PR avec coverage <60% rejetée
</context_data>

<detailed_instructions>
  <steps>
    <step n="1">DEEP LINKS VALIDATION
      - MainActivity.kt: wrapper intent.data parsing dans try-catch
      - MineraLogNavHost.kt: valider UUID avant navigation
      - Tests: DeepLinkTest avec valid/invalid/malicious UUIDs
    </step>

    <step n="2">RELEASE SIGNING
      - keytool -genkey -v -keystore release.keystore ...
      - Encode base64: cat release.keystore | base64
      - GitHub Secrets: RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PASSWORD, etc.
      - build.gradle.kts: decode env vars, config signingConfigs.release
      - CI: add decode step avant build
      - Test local: assembleRelease, verify signature
    </step>

    <step n="3">DISABLE BACKUP (ou SECURE)
      - Si P0.2 DB encryption fait: garder allowBackup, renforcer backup_rules.xml
      - Sinon: allowBackup="false"
      - Test: adb backup -f test.ab net.meshcore.mineralog (doit échouer)
    </step>

    <step n="4">NETWORK SECURITY CONFIG
      - Créer network_security_config.xml
      - Manifest: android:networkSecurityConfig
      - Si app offline-first: aucune régression attendue
    </step>

    <step n="5">TESTS VIEWMODELS
      - SettingsViewModelTest.kt: 10+ tests (export/import/theme/language)
      - EditMineralViewModelTest.kt: 8+ tests (validation/photo/save)
      - MockK pour repos, Turbine pour StateFlows
    </step>

    <step n="6">CI JACOCO
      - build.gradle.kts: configure JaCoCo plugin
      - tasks: jacocoTestReport, jacocoTestCoverageVerification
      - ci.yml: add step after unit tests
      - Test: commit avec tests supprimés → CI fail
    </step>

    <step n="7">CI VALIDATION & COMMIT
      - CI complet vert
      - 6 commits atomiques
      - Doc: DOCS/assumptions.md → keystore management, backup strategy
    </step>
  </steps>

  <output_format>
    <format>
      <response>
        <summary>6 corrections P1 sécurité implémentées</summary>
        <changeset>[diffs par correction]</changeset>
        <security_validation>
          <deep_links>✅ UUID validation, injection blocked</deep_links>
          <signing>✅ Release APK signed with private key (SHA256: ...)</signing>
          <backup>✅ Disabled / Secured</backup>
          <network>✅ Cleartext blocked</network>
        </security_validation>
        <ci_status>[lint/test/coverage/build status]</ci_status>
        <coverage_report>Total: X%, ViewModels: Y% (target >70% sur critiques)</coverage_report>
      </response>
    </format>
  </output_format>
</detailed_instructions>
```

---

## 📋 SESSION 3 — Corrections P1 Données + i18n (1 semaine)

### Prompt Session P1-Data-i18n

```xml
<task_description>
  <persona>Data Engineer + i18n Specialist</persona>
  <task>Implémenter corrections P1 données (schemas JSON alignment, CSV MERGE fix, tests DAOs) + corrections i18n (45 hardcoded strings) + accessibilité (32 contentDescription). Tests obligatoires. CI vert.</task>
  <tone>Opérationnel, méticuleux sur data integrity.</tone>
</task_description>

<context_data>
**P1.7 - Aligner Schemas JSON/Code** (4h)
- Fichiers: DOCS/json_schema/mineralog_v1.1.0.json, StorageEntity.kt, mappers
- Problèmes:
  * Storage: place ≠ location, qrContent ≠ qrCode, nfcTagId ≠ nfcTag
  * PhotoType: UPPERCASE vs lowercase
  * Structure: nested vs flat
- Solution: décider référence (code ou schema), aligner l'autre
- Tests: export 100 minerals, valider JSON contre schema avec validator

**P1.8 - Fixer CSV Import MERGE** (1h)
- Fichier: BackupRepository.kt:588
- Problème: MERGE mode génère toujours nouveau UUID au lieu d'update existant
- Solution: vérifier si mineral avec même name existe, utiliser son ID
- Tests: import CSV 2×, vérifier pas de duplicatas

**P1.9 - Tests DAOs Manquants** (8h)
- PhotoDao, ProvenanceDao, StorageDao, FilterPresetDao
- Tests minimum par DAO:
  * CRUD operations
  * Cascade deletes (si ForeignKey)
  * Batch queries (getByMineralIds)
  * Edge cases (null fields, duplicates)
- Coverage target: >80% sur DAOs

**P1.10 - i18n Hardcoded Strings** (4h)
- 45 occurrences "Back", "Save", dialog messages, section labels
- Fichiers: HomeScreen, EditScreen, AddScreen, SettingsScreen, dialogs
- Solution: remplacer par stringResource(R.string.XXX)
- Ajouter strings manquants dans strings.xml/strings-fr.xml
- Tests: vérifier locale FR affiche français

**P1.11 - Accessibilité contentDescription** (3h)
- 32 Icons avec contentDescription = null
- Fichiers: HomeScreen, SettingsScreen, FilterBottomSheet, etc.
- Solution: contentDescription = stringResource(R.string.cd_XXX)
- Tests: TalkBack manual test, automated a11y scanner
</context_data>

<detailed_instructions>
  <steps>
    <step n="1">ALIGN JSON SCHEMAS
      - Décision: code = référence (car plus récent)
      - Mettre à jour mineralog_v1.1.0.json pour matcher entities
      - Ou: créer mineralog_v1.2.0.json si breaking
      - Tests: BackupIntegrationTest round-trip, JSON validator
    </step>

    <step n="2">FIX CSV MERGE
      - BackupRepository parseMineralFromCsvRow: check existing by name
      - Si existe ET mode=MERGE: réutiliser ID existant
      - Tests: CsvImportTest avec MERGE mode, assert no duplicates
    </step>

    <step n="3">TESTS DAOs
      - PhotoDaoTest: insert/delete/getByMineralId/cascade
      - ProvenanceDaoTest: CRUD + batch
      - StorageDaoTest: CRUD + batch
      - FilterPresetDaoTest: CRUD + JSON serialization
      - Utiliser in-memory database (@RunWith RobolectricTestRunner)
    </step>

    <step n="4">i18n HARDCODED STRINGS
      - Grep "Text\(\"" sans stringResource
      - Pour chaque: identifier string existante ou créer nouvelle
      - Ajouter traductions FR si nouvelles
      - Remplacer par stringResource()
      - Vérifier build avec locale FR
    </step>

    <step n="5">ACCESSIBILITY contentDescription
      - Grep "contentDescription = null"
      - Identifier si decorative (null OK) ou functional (needs description)
      - Ajouter cd_XXX strings si manquantes
      - Remplacer null par stringResource(R.string.cd_XXX)
      - Tests: automated a11y checks
    </step>

    <step n="6">CI VALIDATION & COMMIT
      - 5 commits atomiques
      - Tests passent
      - Coverage DAOs >80%
    </step>
  </steps>

  <output_format>
    <format>
      <response>
        <summary>Corrections P1 data + i18n + a11y</summary>
        <data_integrity>
          <json_schema>✅ Aligned, validation passes</json_schema>
          <csv_merge>✅ No duplicates on re-import</csv_merge>
          <dao_coverage>PhotoDao: X%, ProvenanceDao: Y%, ... (all >80%)</dao_coverage>
        </data_integrity>
        <i18n_status>
          <hardcoded_removed>45/45 (100%)</hardcoded_removed>
          <locale_fr_test>✅ 100% French UI</locale_fr_test>
        </i18n_status>
        <a11y_status>
          <contentdescription_fixed>32/32 (100%)</contentdescription_fixed>
          <talkback_test>✅ All icons identified</talkback_test>
        </a11y_status>
      </response>
    </format>
  </output_format>
</detailed_instructions>
```

---

## 📋 SESSION 4 — Corrections P2 Dette Technique (1-2 semaines)

### Prompt Session P2-TechDebt

```xml
<task_description>
  <persona>Staff Refactoring Engineer + DX Specialist</persona>
  <task>Refactorer dette technique P2: BackupRepository god class, migration Hilt DI, refactor large composables, extract magic numbers, optimisations perf (CSV, photos), ProGuard refinement, Detekt strict. Pas de régression fonctionnelle. Tests refactoring-proof.</task>
  <tone>Méthodique, focus qualité code long-terme.</tone>
</task_description>

<context_data>
**P2.1 - Refactor BackupRepository** (2-3j)
- 715 LOC → 4 services <200 LOC
- Extraire: ZipBackupService, CsvBackupService, BackupEncryptionService, MineralCsvMapper
- BackupRepository devient facade/coordinator
- Tests: tous existants doivent passer sans changement

**P2.2 - Migration Hilt DI** (2-3j)
- Supprimer service locator (as MineraLogApplication)
- Ajouter Hilt plugin, @HiltAndroidApp, @AndroidEntryPoint
- @HiltViewModel pour 8 ViewModels
- Supprimer 8 ViewModelFactory classes
- Tests: injection fonctionne, ViewModels instanciés

**P2.3 - Refactor Large Composables** (1-2j)
- HomeScreen 866 LOC → extraire: HomeTopBar, HomeSearchBar, MineralListSection, FilterChipSection
- ImportCsvDialog 641 LOC → ColumnMappingSection, ModeSelectionSection, PreviewSection
- SettingsScreen 610 LOC → ThemeSection, LanguageSection, BackupSection, AboutSection
- Tests: UI tests existants passent

**P2.4 - Extract Magic Numbers** (2h)
- Créer UiConstants.kt, DatabaseConstants.kt
- Débounce times, delays, batch sizes, timeouts
- Tests: vérifier aucun changement comportement

**P2.5 - Optimiser CSV Export** (2h)
- BackupRepository exportCsv: StringBuilder batch writes
- Tests: export 1000 minerals <2s (log timestamps)

**P2.6 - Optimiser Photo Loading** (1h)
- PhotoManager: Coil .size(400, 400)
- Tests: Memory Profiler, vérifier <4 MB pour 20 photos

**P2.7 - ProGuard Refinement** (1-2h)
- Remplacer wildcards ** par rules spécifiques
- Keep minimum nécessaire
- Tests: release APK fonctionne, décompiler vérifier obfuscation

**P2.8 - Detekt Strict Config** (1h)
- Ajouter rules: ComplexMethod (15), LargeClass (400), MagicNumber
- Fixer violations détectées
- Tests: detekt passe maxIssues: 0

**P2.9 - Clean Unused Resources** (1h)
- Android Lint unused-resources
- Supprimer ~225 unused strings
- Tests: build réussit, aucune string manquante runtime
</context_data>

<detailed_instructions>
  <steps>
    <step n="1">REFACTOR BACKUP REPOSITORY (TDD approach)
      - Lancer tous tests BackupRepository → baseline vert
      - Extraire ZipBackupService (export/import ZIP)
      - Tests passent
      - Extraire CsvBackupService (export/import CSV)
      - Tests passent
      - Extraire BackupEncryptionService (encrypt/decrypt)
      - Tests passent
      - Extraire MineralCsvMapper (parsing)
      - Tests passent
      - BackupRepository délègue aux services
      - Tous tests finaux passent
    </step>

    <step n="2">MIGRATE HILT
      - build.gradle: plugins hilt, dependencies
      - @HiltAndroidApp sur MineraLogApplication
      - @Module @InstallIn pour fournir repos/database
      - @HiltViewModel sur 8 ViewModels
      - @AndroidEntryPoint sur screens
      - Supprimer factories
      - Tests: injection works
    </step>

    <step n="3">REFACTOR COMPOSABLES
      - Extraire sous-composables
      - @Composable preview pour chaque
      - Tests UI existants passent
    </step>

    <step n="4-9">AUTRES P2
      - Extract constants
      - Optimisations perf
      - ProGuard refinement
      - Detekt strict
      - Clean resources
    </step>

    <step n="10">REGRESSION TESTING
      - Full CI suite
      - Manual smoke test: create/edit/delete mineral, backup/restore
      - Performance benchmarks: avant/après
    </step>
  </steps>

  <output_format>
    <format>
      <response>
        <summary>9 corrections P2 dette technique</summary>
        <refactoring_metrics>
          <backup_repository>
            <before>715 LOC, 1 file</before>
            <after>4 services (150 LOC avg) + 1 facade (80 LOC)</after>
            <tests>✅ All pass (no changes needed)</tests>
          </backup_repository>
          <hilt_migration>
            <factories_removed>8</factories_removed>
            <boilerplate_removed>~120 LOC</boilerplate_removed>
            <injection>✅ All ViewModels injected</injection>
          </hilt_migration>
          <composables>
            <homescreen>866 → 180 LOC (5 sub-composables)</homescreen>
            <importcsvdialog>641 → 150 LOC (4 sub-composables)</importcsvdialog>
            <settingsscreen>610 → 120 LOC (5 sub-composables)</settingsscreen>
          </composables>
        </refactoring_metrics>
        <performance_improvements>
          <csv_export>5s → 1.8s (2.8x faster)</csv_export>
          <photo_grid_memory>8 MB → 3.5 MB (56% reduction)</photo_grid_memory>
        </performance_improvements>
        <code_quality>
          <detekt>✅ PASS (maxIssues: 0, strict rules)</detekt>
          <apk_size>15 MB → 14.2 MB (5% reduction)</apk_size>
          <unused_resources>225 strings removed</unused_resources>
        </code_quality>
      </response>
    </format>
  </output_format>
</detailed_instructions>
```

---

## 📋 SESSION BONUS — Full Regression & Documentation

### Prompt Session Final-QA

```xml
<task_description>
  <persona>QA Engineer + Technical Writer</persona>
  <task>Suite complète tests régression post-corrections. Smoke tests manuels. Mise à jour documentation (ARCHITECTURE.md, CHANGELOG.md, README.md). Vérification acceptance checklist complète. Rapport final.</task>
  <tone>Rigoureux, exhaustif, orientation qualité.</tone>
</task_description>

<detailed_instructions>
  <steps>
    <step n="1">REGRESSION TESTING
      - CI full suite: lint + detekt + unit + instrumentation + build
      - Manual smoke tests:
        * Create mineral with all fields
        * Add photos (NORMAL, UV, MACRO)
        * Edit mineral
        * Delete mineral
        * Bulk select + delete
        * Export backup (encrypted + plain)
        * Import backup
        * CSV import/export
        * Search + filter
        * Statistics screen
        * Theme switch
        * Language switch FR/EN
      - Performance checks:
        * Page scroll 100 minerals (smooth)
        * Photo grid load
        * Export 1000 minerals
      - Security checks:
        * Deep link injection attempts
        * Backup with wrong password
        * Database file inspection (hexdump)
    </step>

    <step n="2">ACCEPTANCE CHECKLIST
      - Vérifier tous 30+ critères audit
      - Documenter résultats dans DOCS/ACCEPTANCE_VALIDATION.md
    </step>

    <step n="3">UPDATE DOCUMENTATION
      - ARCHITECTURE.md: refléter Hilt DI, services refactorés
      - CHANGELOG.md: ajouter version post-audit avec tous fixes
      - README.md: update features si nécessaire
      - DOCS/assumptions.md: finaliser
    </step>

    <step n="4">FINAL REPORT
      - Résumé corrections implémentées
      - Métriques avant/après (coverage, perf, APK size, etc.)
      - Screenshots/vidéos démos
      - Roadmap items P2 restants (si pas tous faits)
    </step>
  </steps>

  <output_format>
    <format>
      <response>
        <summary>QA complète post-audit + documentation finalisée</summary>
        <regression_results>
          <automated_tests>✅ X/X passed</automated_tests>
          <manual_tests>✅ All smoke tests pass</manual_tests>
          <performance>✅ All targets met</performance>
          <security>✅ All validations pass</security>
        </regression_results>
        <acceptance_checklist>30/30 ✅ (100%)</acceptance_checklist>
        <documentation_updated>
          <architecture>✅ Updated for Hilt + refactoring</architecture>
          <changelog>✅ v1.6.0 post-audit added</changelog>
          <readme>✅ Current</readme>
        </documentation_updated>
        <metrics_summary>
          <coverage>Before: 35% → After: 65%</coverage>
          <security>Critical issues: 5 → 0</security>
          <performance>Paging: 3000ms → 280ms</performance>
          <code_quality>God classes: 1 → 0, Large files: 5 → 0</code_quality>
          <i18n>Hardcoded: 45 → 0</i18n>
          <a11y>Missing descriptions: 32 → 0</a11y>
        </metrics_summary>
      </response>
    </format>
  </output_format>
</detailed_instructions>
```

---

## 🎯 Ordre d'Exécution Recommandé

1. **SESSION 1 - P0 Critiques** (3-4 jours) → BLOQUEUR PRODUCTION
2. **SESSION 2 - P1 Sécurité** (1 semaine) → AVANT RELEASE
3. **SESSION 3 - P1 Données + i18n** (1 semaine) → QUALITÉ UTILISATEUR
4. **SESSION 4 - P2 Dette Technique** (1-2 semaines) → MAINTENABILITÉ
5. **SESSION BONUS - QA Final** (2-3 jours) → VALIDATION

**Total:** 6-8 semaines pour complétion 100%

---

## 📝 Notes d'Utilisation

- Chaque prompt est autonome et peut être copié/collé dans une nouvelle session Claude Code
- Les prompts incluent context, instructions détaillées, et critères d'acceptation
- Format XML pour parsing facile si automatisation
- Adapter les timings selon la vélocité de l'équipe
- Prioriser SESSION 1 (P0) en urgence si production imminente
