# MineraLog Development Guide

This document covers testing, API documentation, and security for MineraLog v3.0.0.

## Testing Strategy

### Test Coverage
- **Minimum Coverage**: 60% (enforced via JaCoCo in CI)
- **Current Coverage**: Check latest CI run
- **Command**: `./gradlew jacocoTestReport jacocoTestCoverageVerification`

### Unit Tests (`app/src/test/`)

#### Framework Setup
- **JUnit 5 Jupiter** - Modern testing framework
- **MockK** - Kotlin-first mocking library
- **Turbine** - Flow testing library
- **Robolectric** - Android framework for JVM tests
- **kotlinx-coroutines-test** - Coroutine testing utilities

#### Test Categories

**1. DAO Tests** (e.g., `MineralDaoTest.kt`)
```kotlin
@RunWith(RobolectricTestRunner::class)
class MineralDaoTest {
    private lateinit var database: MineraLogDatabase
    private lateinit var mineralDao: MineralDao

    @BeforeEach
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MineraLogDatabase::class.java
        ).allowMainThreadQueries().build()
        mineralDao = database.mineralDao()
    }
}
```

**2. Repository Tests** (e.g., `MineralRepositoryTest.kt`)
```kotlin
class MineralRepositoryTest {
    private val mineralDao = mockk<MineralDao>()
    private val repository = MineralRepositoryImpl(mineralDao, ...)

    @Test
    fun `insert mineral should call dao`() = runTest {
        val mineral = createTestMineral()
        coEvery { mineralDao.insert(any()) } returns 1L

        repository.insertMineral(mineral)

        coVerify { mineralDao.insert(any()) }
    }
}
```

**3. ViewModel Tests** (e.g., `HomeViewModelTest.kt`)
```kotlin
class HomeViewModelTest {
    @Test
    fun `loading state emits correctly`() = runTest {
        val viewModel = HomeViewModel(mockRepository)

        viewModel.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            // ... more assertions
        }
    }
}
```

### Integration Tests (`app/src/androidTest/`)

**Key Scenarios**:
- `BackupIntegrationTest` - Full backup/restore cycle
- `DatabaseMigrationTest` - All migration paths (v1→v7)
- `CsvImportExportTest` - CSV round-trip

### Running Tests

```bash
# All unit tests
./gradlew testDebugUnitTest

# Specific test class
./gradlew testDebugUnitTest --tests "*MineralDaoTest"

# With coverage
./gradlew jacocoTestReport

# Instrumented tests (requires emulator/device)
./gradlew connectedDebugAndroidTest

# Coverage verification (60% threshold)
./gradlew jacocoTestCoverageVerification
```

### Test Data Builders

Use factory functions for test data:
```kotlin
fun createTestMineral(
    id: String = UUID.randomUUID().toString(),
    name: String = "Test Quartz",
    type: MineralType = MineralType.SPECIMEN
) = Mineral(id = id, name = name, type = type, ...)
```

---

## API Documentation

### Room DAOs

All DAOs use coroutines (`suspend`) and reactive streams (`Flow`).

#### MineralDao

**CRUD Operations**:
```kotlin
suspend fun insert(mineral: MineralEntity): Long
suspend fun update(mineral: MineralEntity)
suspend fun delete(mineral: MineralEntity)
suspend fun getById(id: String): MineralEntity?
fun getAllFlow(): Flow<List<MineralEntity>>
```

**Search & Filter**:
```kotlin
fun searchMinerals(query: String): PagingSource<Int, MineralEntity>
fun filterByType(type: MineralType): Flow<List<MineralEntity>>
fun filterByTags(tags: List<String>): Flow<List<MineralEntity>>
```

**Statistics**:
```kotlin
suspend fun getCount(): Int
suspend fun getCountByType(): Map<MineralType, Int>
```

#### ReferenceMineralDao

```kotlin
suspend fun insert(mineral: ReferenceMineralEntity): Long
suspend fun search(query: String): List<ReferenceMineralEntity>
fun getAllFlow(): Flow<List<ReferenceMineralEntity>>
suspend fun getByName(name: String): ReferenceMineralEntity?
```

### Repository Interfaces

#### MineralRepository

**Core Operations**:
```kotlin
suspend fun insertMineral(mineral: Mineral): Long
suspend fun updateMineral(mineral: Mineral)
suspend fun deleteMineral(mineralId: String)
suspend fun getMineralById(id: String): Mineral?
fun getAllMineralsPaged(
    query: String = "",
    filters: MineralFilters = MineralFilters()
): Flow<PagingData<Mineral>>
```

**Bulk Operations**:
```kotlin
suspend fun bulkDelete(mineralIds: List<String>)
suspend fun exportToCsv(minerals: List<Mineral>): File
suspend fun generateQrLabels(mineralIds: List<String>): File
```

#### BackupRepository

**Backup Operations**:
```kotlin
suspend fun createBackup(
    outputUri: Uri,
    password: String,
    includePhotos: Boolean = true
): Result<Unit>

suspend fun restoreBackup(
    inputUri: Uri,
    password: String
): Result<RestoreResult>

suspend fun exportCsv(
    minerals: List<Mineral>,
    outputUri: Uri
): Result<Unit>

suspend fun importCsv(
    inputUri: Uri,
    columnMapping: Map<String, String>
): Result<ImportResult>
```

---

## Security

### Database Encryption (SQLCipher)

**Configuration**:
- **Algorithm**: AES-256-CBC
- **Page Size**: 4096 bytes
- **PBKDF2 Iterations**: Handled by Argon2id externally

**Key Derivation (Argon2id)**:
```kotlin
Argon2Kt().hash(
    mode = Argon2Mode.ARGON2_ID,
    password = userPassword.toByteArray(),
    salt = salt, // 16 bytes random
    iterations = 3,
    memory = 65536, // 64 MB
    parallelism = 4,
    hashLength = 32 // 256 bits
)
```

**Key Storage**:
- **Master Key**: Android Keystore (hardware-backed if available)
- **Library**: Google Tink for Keystore management
- **Key Wrapping**: Master key encrypts database key
- **Persistence**: Encrypted key stored in SharedPreferences

**Code Reference**: `app/src/main/java/net/meshcore/mineralog/data/local/DatabaseKeyManager.kt`

### Backup Security

**Encrypted ZIP**:
1. User provides password
2. Derive encryption key via Argon2id
3. Encrypt each file (CSV + photos) with AES-256-GCM
4. Package into ZIP with manifest
5. ZIP itself is password-protected (standard ZIP encryption as secondary layer)

**Integrity**:
- SHA-256 checksums for all files in manifest
- Verification on restore
- Rollback on checksum mismatch

### Photo Security

**Storage**:
- **Location**: `context.filesDir/photos/` (app-internal)
- **Permissions**: No external storage access required (Android 10+)
- **Encryption**: Android filesystem encryption (FBE/FDE)
- **Cleanup**: Orphaned photos deleted on app cleanup

**Privacy**:
- No cloud upload by default
- No third-party analytics
- No telemetry collection
- All data stays on-device

### Network Security

**Current**: No network operations (fully offline app)

**Future (Cloud Sync)**:
- Certificate pinning for API
- TLS 1.3 only
- End-to-end encryption for backups
- Zero-knowledge architecture (server cannot decrypt data)

### Permissions

**Required**:
- `CAMERA` - Photo capture (runtime permission)

**Not Required**:
- ~~`READ_EXTERNAL_STORAGE`~~ - Uses scoped storage
- ~~`WRITE_EXTERNAL_STORAGE`~~ - Uses SAF for backups
- ~~`INTERNET`~~ - Fully offline app

### Security Best Practices

**DO**:
- ✅ Use Argon2id for password hashing (not bcrypt/PBKDF2)
- ✅ Use Android Keystore for key storage
- ✅ Use SQLCipher for database encryption
- ✅ Use scoped storage (no external storage permissions)
- ✅ Validate all user input
- ✅ Clear clipboard after 30s (SecureClipboard utility)

**DON'T**:
- ❌ Store passwords in plain text
- ❌ Use weak KDFs (MD5, SHA-1, simple PBKDF2)
- ❌ Store sensitive data in SharedPreferences unencrypted
- ❌ Request unnecessary permissions
- ❌ Trust user input without validation

### Vulnerability Scanning

**CI Pipeline**:
- **CodeQL**: Semantic code analysis
- **Dependency Review**: Checks for known CVEs
- **Detekt**: Static analysis for Kotlin

**Manual Audits**:
- Review crypto implementations quarterly
- Update dependencies monthly
- Monitor SQLCipher/Tink security advisories

---

## Contributing

### Code Style
- **Kotlin Style Guide**: Follow [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- **Detekt**: Enforced via CI (`./gradlew detekt`)
- **Formatting**: Use Android Studio default formatter

### Commit Messages
```
type(scope): subject

body (optional)

BREAKING CHANGE: (if applicable)
```

**Types**: feat, fix, docs, refactor, test, chore

**Example**:
```
feat(backup): add encrypted ZIP backup

Implements AES-256-GCM encryption for backup files.
Includes SHA-256 integrity verification.

Closes #123
```

### Pull Requests
1. Create feature branch: `feature/descriptive-name`
2. Ensure tests pass: `./gradlew testDebugUnitTest`
3. Verify coverage: `./gradlew jacocoTestCoverageVerification`
4. Run lint: `./gradlew lint detekt`
5. Create PR with description
6. Wait for CI to pass
7. Request review

### CI Requirements
- ✅ All tests pass
- ✅ Coverage ≥ 60%
- ✅ Lint clean
- ✅ Detekt clean
- ✅ CodeQL clean
- ✅ Build success (debug + release)

---

## Resources

- [Architecture](./ARCHITECTURE.md) - System architecture
- [CHANGELOG](../CHANGELOG.md) - Version history
- [README](../README.md) - Project overview
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [Room Documentation](https://developer.android.com/training/data-storage/room)
- [SQLCipher for Android](https://www.zetetic.net/sqlcipher/sqlcipher-for-android/)
