# MineraLog - CI Debugging Guide

Guide complet pour déboguer et corriger les échecs CI dans le pipeline Android.

## Table des matières

1. [Architecture CI](#architecture-ci)
2. [Jobs et dépendances](#jobs-et-dépendances)
3. [Déboguer les échecs](#déboguer-les-échecs)
4. [Exécution locale](#exécution-locale)
5. [Politique anti-flaky](#politique-anti-flaky)

---

## Architecture CI

### Workflow: `.github/workflows/ci.yml`

**Déclencheurs:**
- Push sur `main`, `develop`
- Pull requests vers `main`, `develop`

**Jobs (5):**
1. **Lint & Detekt** - Analyse statique du code
2. **Unit Tests** - Tests JVM (JUnit 5)
3. **Instrumentation Tests** - Tests Android (API 27 & 35)
4. **Build Release APK** - Construction APK production

**Timeouts:**
- Lint & Detekt: 20 min
- Unit Tests: 20 min
- Instrumentation Tests: 45 min par API level
- Build: 20 min

---

## Jobs et dépendances

### 1. Lint & Detekt

**Commandes:**
```bash
./gradlew lint --no-daemon --stacktrace --max-workers=2
./gradlew detekt --no-daemon --stacktrace --max-workers=2
```

**Configuration Detekt:** `config/detekt/detekt.yml`
- MaxLineLength: 140 caractères
- maxIssues: 0 (zero tolerance)

**Artifacts générés:**
- `lint-reports/*.html`
- `detekt-reports/detekt.{xml,html}`

### 2. Unit Tests

**Commande:**
```bash
./gradlew testDebugUnitTest --no-daemon --stacktrace --max-workers=2
```

**Framework:** JUnit 5 (Jupiter) avec Kotlin Coroutines Test

**Artifacts:**
- `**/build/reports/tests/` - Rapports HTML
- `**/build/reports/jacoco/` - Couverture de code

### 3. Instrumentation Tests

**Matrice:** API 27, API 35

**Emulator config:**
- Architecture: x86_64
- Profile: Pixel 6
- RAM: 4GB
- No animations, no audio, no boot anim

**Commande:**
```bash
adb wait-for-device
adb shell settings put global window_animation_scale 0.0
./gradlew connectedDebugAndroidTest --no-daemon --stacktrace --max-workers=2
```

**Artifacts:**
- `**/build/reports/androidTests/`
- `**/build/outputs/androidTest-results/`

### 4. Build Release APK

**Dépendances:** Nécessite `lint` ET `test` SUCCESS

**Commande:**
```bash
./gradlew assembleRelease --no-daemon --stacktrace --max-workers=2
```

**Artifact:** `app/build/outputs/apk/release/*.apk`

---

## Déboguer les échecs

### Accéder aux logs CI

#### Via GitHub UI
1. Aller sur https://github.com/VBlackJack/MineraLog/actions
2. Cliquer sur le run échoué
3. Cliquer sur le job échoué (ex: "Lint & Detekt")
4. Déplier les steps pour voir stdout/stderr

#### Via GitHub CLI
```bash
# Lister les runs récents
gh run list --repo VBlackJack/MineraLog --limit 10

# Voir logs d'un run
gh run view <RUN_ID> --repo VBlackJack/MineraLog --log

# Télécharger artifacts
gh run download <RUN_ID> --repo VBlackJack/MineraLog
```

### Détekt failures

**1. Identifier violations:**
```bash
# Télécharger detekt-reports artifact
gh run download <RUN_ID> --name detekt-reports

# Inspecter XML
cat detekt-reports/detekt.xml | grep "<error"
```

**2. Types de violations courants:**

**MaxLineLength (140 chars):**
```kotlin
// ❌ WRONG (150 chars)
val result = repository.importCsv(uri, columnMapping = mapOf("name" to "Name"), mode = CsvImportMode.MERGE): Result<ImportResult>

// ✅ CORRECT
val result = repository.importCsv(
    uri,
    columnMapping = mapOf("name" to "Name"),
    mode = CsvImportMode.MERGE
): Result<ImportResult>
```

**LongMethod:**
```kotlin
// Si une fonction dépasse 60 lignes, la découper
private fun complexOperation() {
    // 80 lignes de code
}

// ✅ Refactor en sous-fonctions
private fun complexOperation() {
    val step1 = performStep1()
    val step2 = performStep2(step1)
    finalizeOperation(step2)
}
```

### Test unitaire failures

**1. Suspend functions dans tests:**
```kotlin
// ❌ WRONG - Compilation error
@Test
fun `test suspend function`() {
    val result = suspendingRepo.getData() // ERROR: suspend function called outside coroutine
}

// ✅ CORRECT - Utiliser runTest
@Test
fun `test suspend function`() = runTest {
    val result = suspendingRepo.getData() // OK dans coroutine context
}
```

**2. Dépendances manquantes:**
```kotlin
// build.gradle.kts
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
testImplementation("io.mockk:mockk:1.13.9")
testImplementation("app.cash.turbine:turbine:1.0.0")
```

### Instrumentation test failures

**1. Timeouts:**
```kotlin
// ❌ Utiliser sleep() dans tests
Thread.sleep(2000)

// ✅ Utiliser IdlingResource ou waitUntil
composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onAllNodesWithText("Loaded").fetchSemanticsNodes().isNotEmpty()
}
```

**2. Animations:**
```kotlin
// CI désactive déjà les animations, mais en local:
adb shell settings put global window_animation_scale 0.0
adb shell settings put global transition_animation_scale 0.0
adb shell settings put global animator_duration_scale 0.0
```

---

## Exécution locale

### Prérequis

```bash
# JDK 17
java -version  # openjdk version "17.0.x"

# Android SDK
export ANDROID_HOME=$HOME/Android/Sdk

# local.properties
echo "MAPS_API_KEY=dummy_key_for_ci" > local.properties
```

### Lint & Detekt

```bash
# Lint
./gradlew lint --no-daemon

# Rapports: app/build/reports/lint-results-debug.html

# Detekt
./gradlew detekt --no-daemon

# Rapports: app/build/reports/detekt/detekt.html
```

### Tests unitaires

```bash
# Tous les tests
./gradlew testDebugUnitTest --no-daemon

# Un test spécifique
./gradlew test --tests "net.meshcore.mineralog.data.util.CsvParserTest"

# Rapports: app/build/reports/tests/testDebugUnitTest/index.html
```

### Instrumentation tests (emulator required)

```bash
# Démarrer emulator
emulator -avd Pixel_6_API_35 -no-snapshot-load

# Lancer tests
./gradlew connectedDebugAndroidTest --no-daemon

# Rapports: app/build/reports/androidTests/connected/index.html
```

### Build release

```bash
./gradlew assembleRelease --no-daemon

# APK: app/build/outputs/apk/release/app-release.apk
```

---

## Politique anti-flaky

### Principes

1. **Isolation**: Chaque test doit être indépendant
2. **Déterminisme**: Pas de `sleep()`, pas de timestamps actuels
3. **Idempotence**: Résultat identique à chaque exécution
4. **Fast failure**: Échouer rapidement si conditions invalides

### Tests flaky courants

**❌ Flaky: Sleep arbitraire**
```kotlin
@Test
fun testAsyncOperation() {
    viewModel.loadData()
    Thread.sleep(1000) // Peut être trop court ou trop long
    assertEquals(expected, viewModel.state.value)
}
```

**✅ Stable: Synchronisation explicite**
```kotlin
@Test
fun testAsyncOperation() = runTest {
    viewModel.loadData()
    advanceUntilIdle() // Avance temps virtuel jusqu'à idle
    assertEquals(expected, viewModel.state.value)
}
```

**❌ Flaky: Dépendance temporelle**
```kotlin
@Test
fun testTimestamp() {
    val result = repo.createMineral(name = "Quartz")
    assertEquals(System.currentTimeMillis(), result.createdAt) // Peut différer de quelques ms
}
```

**✅ Stable: Injection de temps**
```kotlin
@Test
fun testTimestamp() {
    val clock = TestClock(fixedTime = 1000000L)
    val repo = MineralRepository(clock)
    val result = repo.createMineral(name = "Quartz")
    assertEquals(1000000L, result.createdAt)
}
```

### Retry strategy

**❌ Masquer les flaky:**
```yaml
# build.gradle.kts
android {
    testOptions {
        unitTests.all {
            maxRetries = 3 // Cache les vrais problèmes
        }
    }
}
```

**✅ Identifier et corriger:**
```bash
# Exécuter 10 fois pour détecter flakiness
for i in {1..10}; do
    echo "Run $i"
    ./gradlew test --tests MyFlakyTest || echo "FAILED on run $i"
done
```

---

## Troubleshooting

### "Gradle distribution download failed"

**Cause:** Offline ou proxy

**Solution:**
```bash
# Vérifier gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip

# Si offline, utiliser distribution locale
distributionUrl=file:///path/to/gradle-8.7-bin.zip
```

### "KVM permission denied" (Instrumentation tests)

**Cause:** User non membre du groupe `kvm`

**Solution:**
```bash
sudo usermod -aG kvm $USER
# Logout/login required
```

### "Test fixtures not found"

**Cause:** Fichiers test resources manquants

**Solution:**
```bash
# Vérifier présence
ls app/src/test/resources/

# Reconstruire
./gradlew clean testDebugUnitTest
```

---

## Résumé commandes rapides

```bash
# Check complet local (avant push)
./gradlew clean lint detekt testDebugUnitTest --no-daemon

# Si tout vert localement mais CI rouge:
# 1. Télécharger artifacts CI
gh run download <RUN_ID> --repo VBlackJack/MineraLog

# 2. Comparer environnements
./gradlew --version  # Version Gradle
java -version        # Version JDK

# 3. Reproduire exact CI command
./gradlew detekt --no-daemon --stacktrace --max-workers=2
```

---

**Dernière mise à jour:** 2025-11-14
**Auteur:** MineraLog Team
