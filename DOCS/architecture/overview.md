# MineraLog Architecture Documentation

## Overview

MineraLog follows **Clean Architecture** principles with clear separation between layers:

```
┌─────────────────────────────────────────────────────────┐
│                     Presentation Layer                   │
│        (Compose UI, ViewModels, State Management)        │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                      Domain Layer                        │
│          (Business Models, Use Cases, Rules)             │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                       Data Layer                         │
│  (Room Database, Repositories, External Data Sources)    │
└─────────────────────────────────────────────────────────┘
```

---

## Data Integrity Strategy

### 1. Foreign Key Design Philosophy

**TLDR**: We use **unidirectional manual relationships** instead of Room's built-in foreign keys for maximum control and flexibility.

#### Why Manual Foreign Keys?

##### ✅ Advantages

1. **Custom Cascade Logic**
   - Full control over deletion behavior
   - Ability to log deletions for audit trails
   - Support for undo functionality (future)
   - Conditional cascades based on business rules

2. **Simplified Serialization**
   - Clean JSON export/import without circular references
   - Domain models can be serialized with kotlinx.serialization
   - Easier to version and migrate data

3. **Performance Optimization**
   - Batch loading avoids N+1 queries
   - Partial entity loading (don't always need related data)
   - Explicit control over JOIN queries
   - Example: `BackupRepository.exportZip()` loads all Provenances in one query

4. **Flexibility**
   - Nullable relationships support progressive data entry
   - Optional metadata collection
   - Orphan cleanup strategies can be customized

##### ❌ Tradeoffs

1. **Manual Cascade Implementation**
   - Developer must remember to cascade deletes
   - Risk of orphaned records if cascade is forgotten
   - **Mitigation**: Repository pattern centralizes all data access

2. **No Database-Level Referential Integrity**
   - Foreign key constraints not enforced by SQLite
   - **Mitigation**: Transaction-based operations ensure atomicity
   - **Mitigation**: Validation in Repository layer

### 2. Relationship Patterns

#### One-to-One (Mineral → Provenance/Storage)

```kotlin
// MineralEntity.kt
data class MineralEntity(
    val id: String,
    val name: String,
    val provenanceId: String?, // FK to ProvenanceEntity
    val storageId: String?,    // FK to StorageEntity
    // ...
)

// ProvenanceEntity.kt
data class ProvenanceEntity(
    val id: String,
    val mineralId: String, // Back-reference (unidirectional)
    val country: String?,
    // ...
)
```

**Loading Pattern:**

```kotlin
// ❌ BAD: N+1 queries
minerals.map { mineral ->
    val provenance = provenanceDao.getByMineralId(mineral.id)
    mineral.toDomain(provenance)
}

// ✅ GOOD: Batch query
val mineralIds = minerals.map { it.id }
val provenances = provenanceDao.getByMineralIds(mineralIds)
    .associateBy { it.mineralId }

minerals.map { mineral ->
    mineral.toDomain(provenances[mineral.id])
}
```

#### One-to-Many (Mineral → Photos)

```kotlin
// PhotoEntity.kt
data class PhotoEntity(
    val id: String,
    val mineralId: String, // FK to MineralEntity
    val fileName: String,
    // ...
)
```

**Loading Pattern:**

```kotlin
val photos = photoDao.getByMineralIds(mineralIds)
    .groupBy { it.mineralId }

minerals.map { mineral ->
    mineral.toDomain(
        provenance = provenances[mineral.id],
        storage = storages[mineral.id],
        photos = photos[mineral.id] ?: emptyList()
    )
}
```

### 3. Cascade Delete Implementation

**Location**: `MineralRepositoryImpl.delete()`

```kotlin
suspend fun delete(id: String) {
    database.runInTransaction {
        // Delete related entities BEFORE parent
        provenanceDao.deleteByMineralId(id)
        storageDao.deleteByMineralId(id)
        photoDao.deleteByMineralId(id)

        // Delete parent last
        mineralDao.deleteById(id)
    }
}
```

**Key Principles:**

1. **Transaction Boundary**: All deletes in single transaction (atomic)
2. **Order Matters**: Delete children before parent
3. **Explicit**: No hidden magic, clear what's being deleted
4. **Logged**: Easy to add logging/auditing at each step

### 4. Import/Export Data Integrity

#### Export (ZIP Backup)

**File**: `BackupRepository.exportZip()`

```kotlin
// Batch load all related data
val mineralIds = mineralEntities.map { it.id }
val provenances = provenanceDao.getByMineralIds(mineralIds)
    .associateBy { it.mineralId }
val storages = storageDao.getByMineralIds(mineralIds)
    .associateBy { it.mineralId }
val photos = photoDao.getByMineralIds(mineralIds)
    .groupBy { it.mineralId }

// Map to domain with relationships
minerals.map { entity ->
    entity.toDomain(
        provenances[entity.id],
        storages[entity.id],
        photos[entity.id] ?: emptyList()
    )
}
```

**Guarantees:**

- All related data included in export
- No orphans (complete snapshot)
- Transactional consistency

#### Import (ZIP Restore)

**Modes:**

1. **MERGE**: Upsert by ID, keep existing if conflict
2. **REPLACE**: Delete all, insert fresh
3. **MAP_IDS**: Remap UUIDs on conflict

```kotlin
database.runInTransaction {
    minerals.forEach { mineral ->
        mineralDao.insert(mineral.toEntity())

        // Insert related entities with same mineralId
        mineral.provenance?.let {
            provenanceDao.insert(it.toEntity())
        }
        mineral.storage?.let {
            storageDao.insert(it.toEntity())
        }
        mineral.photos.forEach {
            photoDao.insert(it.toEntity())
        }
    }
}
```

**Guarantees:**

- Atomicity: All-or-nothing import
- FK consistency: mineralId matches across tables
- Orphan prevention: Related entities inserted together

#### CSV Import

**Limitations**: CSV doesn't support photos (file references only)

**Validation:**

1. Name field required (throws error)
2. Numeric fields validated (`toFloatOrNull()`)
3. Boolean parsing ("true", "yes", "1" → true)
4. FK integrity maintained (auto-generated UUIDs)

**Modes:**

- `MERGE`: Update if name exists, insert if new
- `REPLACE`: Clear all, insert from CSV
- `SKIP_DUPLICATES`: Skip rows with duplicate names

### 5. Transaction Best Practices

#### ✅ DO

```kotlin
database.runInTransaction {
    // All related inserts/updates/deletes
    mineralDao.insert(mineral)
    provenanceDao.insert(provenance)
    storageDao.insert(storage)
}
```

#### ❌ DON'T

```kotlin
// Separate transactions = data inconsistency risk
mineralDao.insert(mineral)
provenanceDao.insert(provenance) // Could fail, leaving orphan mineral
```

### 6. Warning Logs for FK Issues

**TODO (Sprint Item #3)**: Add logging to detect orphaned lookups

```kotlin
val provenance = provenanceDao.getByMineralId(mineralId)
if (provenance == null && mineral.provenanceId != null) {
    Log.w("MineralRepository", "Orphaned provenance FK detected: ${mineral.provenanceId}")
}
```

**When to Check:**

- During domain mapping (`toDomain()`)
- In Repository read operations
- On app startup (orphan cleanup job)

---

## Room Database Schema

**Version**: 4 (as of v1.4.1)

**Tables:**

| Table | Purpose | Key Indexes | Foreign Keys |
|-------|---------|-------------|--------------|
| `minerals` | Main specimen data | `name`, `group`, `statusType`, `provenanceId`, `storageId` | Manual: `provenanceId`, `storageId` |
| `provenances` | Geographic origin | `mineralId` | Manual: `mineralId` |
| `storage` | Location info | `mineralId` | Manual: `mineralId` |
| `photos` | Photo metadata | `mineralId`, `type` | Manual: `mineralId` |
| `filter_presets` | Saved filters | `createdAt` | None |

**Migration Strategy:**

- Room auto-migrations for schema changes
- Manual migrations for data transformations
- See: `MineraLogDatabase.kt` for migration definitions

---

## Repository Pattern

**Purpose**: Centralize all data access, enforce business rules

### MineralRepository

**Responsibilities:**

- CRUD operations with cascade deletes
- Search and filtering
- Batch operations (avoid N+1)
- Statistics queries
- Atomic transactions for data integrity

**Key Methods:**

- `delete(id)` → Manual cascade to Provenance/Storage/Photos (atomic transaction)
- `deleteByIds(ids)` → Batch delete with cascade
- `getAll()` → Returns MineralEntity only (no relations)
- `getAllWithDetails()` → Loads relations in batch
- All multi-table operations wrapped in `database.withTransaction { }`

**Performance Optimizations (v1.6.0):**

- N+1 query elimination via `MineralPagingSource`
- Batch loading: `getByIds()`, `getByMineralIds()` methods
- 93.4% query reduction in paging (61 → 4 queries per page)
- Proper indexing on foreign keys (`provenanceId`, `storageId`)

### BackupRepository (Facade Pattern)

**Refactored in v1.6.0**: God class (744 LOC) → Clean facade (117 LOC) + 4 specialized services

**Responsibilities:**

- Export: ZIP (with media), CSV (data only)
- Import: ZIP (3 modes), CSV (3 modes)
- Encryption: Argon2id+AES-256-GCM for ZIP
- Validation: Type checking, FK integrity

**Architecture (Facade + Services):**

```kotlin
BackupRepositoryImpl (Facade: 117 LOC)
    ├─> ZipBackupService (331 LOC) - ZIP export/import
    ├─> CsvBackupService (259 LOC) - CSV export/import
    ├─> BackupEncryptionService (138 LOC) - Argon2+AES crypto
    └─> MineralCsvMapper (151 LOC) - CSV row parsing
```

**Import Modes:**

- `ImportMode.MERGE` → Upsert by UUID
- `ImportMode.REPLACE` → Delete all, insert
- `ImportMode.MAP_IDS` → Remap UUIDs on conflict

**CSV Modes:**

- `CsvImportMode.MERGE` → Match by name, upsert
- `CsvImportMode.REPLACE` → Clear all, insert
- `CsvImportMode.SKIP_DUPLICATES` → Skip if name exists

**Service Layer Details:**

1. **ZipBackupService**
   - Handles ZIP creation and extraction
   - Manages manifest.json with metadata
   - Coordinates encryption via BackupEncryptionService
   - Handles photo file bundling

2. **CsvBackupService**
   - UTF-8 encoding with BOM
   - RFC 4180 compliant (commas, quotes, newlines)
   - 35 columns covering all mineral properties
   - Column mapping for import flexibility

3. **BackupEncryptionService**
   - Wraps `PasswordBasedCrypto` for consistency
   - Handles salt and IV encoding/decoding
   - Provides clean encrypt/decrypt API
   - Fixed in v1.6.0: Argon2 key derivation functional

4. **MineralCsvMapper**
   - Bidirectional mapping (CSV ↔ Mineral domain model)
   - Type conversions (String → Float, Boolean, Enum)
   - Null handling and validation
   - Extensible for new fields

---

## Testing Strategy

### Unit Tests

**Target**: 80% coverage on data layer

**Focus Areas:**

1. **DAO Tests** (`MineralDaoTest.kt`)
   - CRUD operations
   - Complex queries (filtering, search)
   - Edge cases (nulls, empty results)

2. **Repository Tests**
   - Cascade delete logic
   - Batch operations
   - Transaction rollback

3. **CSV Parser Tests**
   - Encoding detection
   - Delimiter detection
   - Quote handling
   - Error cases

### Integration Tests

**Instrumented Tests:**

1. Import/Export roundtrips
2. Encryption/Decryption cycles
3. Large dataset performance (1000+ minerals)
4. Orphan detection

### Test Data Helpers

```kotlin
fun createTestMineral(
    id: String = UUID.randomUUID().toString(),
    name: String = "Test Mineral",
    withProvenance: Boolean = false,
    withStorage: Boolean = false
): MineralEntity {
    // ...
}
```

---

## Performance Considerations

### Database Indexing

**Indexed Columns:**

- `name` → Frequent sorting/filtering
- `group` → Category filtering
- `statusType` → Status filtering
- `provenanceId`, `storageId` → JOIN optimization
- `createdAt`, `updatedAt` → Sorting

### Batch Query Patterns

**Always prefer batch queries:**

```kotlin
// ❌ BAD: N queries
minerals.map { photoDao.getByMineralId(it.id) }

// ✅ GOOD: 1 query
photoDao.getByMineralIds(minerals.map { it.id })
```

### Pagination

**TODO (Sprint Item #4)**: Implement Paging 3 for large collections

**Current**: Load all minerals (works for <1000 items)
**Future**: `Flow<PagingData<Mineral>>` for smooth scrolling with 5000+ items

---

## Dependency Injection

### Current Implementation (v1.6.0): Manual DI

**Pattern**: Application-scoped lazy initialization

```kotlin
// MineraLogApplication.kt
class MineraLogApplication : Application() {
    val database by lazy { MineraLogDatabase.getDatabase(this) }
    val mineralRepository by lazy { MineralRepositoryImpl(database) }
    val backupRepository by lazy { BackupRepositoryImpl(this, database) }
    val statisticsRepository by lazy { StatisticsRepositoryImpl(database.mineralDao()) }
    // ... other repositories
}
```

**Pros:**
- ✅ Simple and functional
- ✅ No additional dependencies
- ✅ Easy to understand and test
- ✅ Suitable for current codebase size (5 ViewModels, 8 repositories)

**Cons:**
- ⚠️ Manual wiring (no compile-time safety)
- ⚠️ ViewModelFactory boilerplate (~15 LOC per ViewModel)
- ⚠️ Not scalable beyond ~10 ViewModels

**Future Migration (Planned for v2.0):**

Hilt DI migration documented in `DOCS/P2_HILT_MIGRATION_PLAN.md`:
- 8 ViewModels → `@HiltViewModel`
- 8 ViewModelFactory classes → DELETE (~120 LOC reduction)
- Compile-time dependency graph validation
- Improved testability with `@HiltAndroidTest`

---

## Security & Privacy

### Encryption

**Algorithm**: Argon2id (KDF) + AES-256-GCM (AEAD)

**Use Cases:**

1. ZIP backups (user password optional)
2. Database at-rest (SQLCipher with Android Keystore)
3. Export to untrusted storage (cloud)

**Implementation**:
- `Argon2Helper.kt` - Key derivation (fixed in v1.6.0)
- `CryptoHelper.kt` - AES-256-GCM encryption
- `PasswordBasedCrypto.kt` - High-level API
- `DatabaseKeyManager.kt` - Keystore integration

**Key Parameters:**

- **Argon2id**: 128MB memory, 4 iterations, 2 parallelism, 32-byte output
- **AES-GCM**: 256-bit key, 96-bit IV (random per operation), 128-bit auth tag
- **Android Keystore**: Hardware-backed key storage (TEE/StrongBox when available)

**Security Enhancements (v1.6.0):**

1. **P0.1**: Argon2 key derivation restored (was returning zeros)
2. **P0.2**: Database encryption with SQLCipher + EncryptedSharedPreferences
3. **P0.5**: Comprehensive crypto test coverage (61 tests, >95% coverage)
4. **P1.1**: Deep link UUID validation (dual-layer defense)
5. **P1.2**: Production APK signing with environment variables
6. **P1.3**: `allowBackup=false` prevents cloud/adb extraction
7. **P1.4**: Network security config (HTTPS-only, cleartext blocked)

### Data Privacy

**No Analytics**: Zero telemetry, all data stays local

**Photo Storage**: Internal app storage (`Context.filesDir`)

**Backups**: User-controlled exports only (no automatic cloud sync)

**Database Encryption**: SQLCipher with Android Keystore passphrase management

---

## Future Architecture Improvements

### Planned Enhancements

1. **Paging 3 Integration** (Sprint Item #4)
   - Handle 10,000+ specimens smoothly
   - Reduce memory footprint

2. **Repository Tests** (Sprint Item #7)
   - 80% coverage target
   - Cascade delete verification

3. **Orphan Cleanup Job**
   - Background WorkManager task
   - Detect and fix orphaned FKs
   - User notification of issues

4. **Undo Stack** (Post-v1.5)
   - Leverage manual cascade pattern
   - Store deleted entities temporarily
   - "Undo Delete" button in Snackbar

---

## References

### Key Files

- **Database**: `MineraLogDatabase.kt`
- **Entities**: `data/local/entity/*.kt`
- **DAOs**: `data/local/dao/*.kt`
- **Repositories**: `data/repository/*.kt`
- **Mappers**: `data/mapper/EntityMappers.kt`

### External Documentation

- [Room Database](https://developer.android.com/training/data-storage/room)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)

---

## Change Log

### v1.6.0 (Post-Audit Refactoring)

**Security Hardening:**
- ✅ Argon2 key derivation functional (fixed P0.1)
- ✅ Database encrypted at-rest with SQLCipher (P0.2)
- ✅ Deep link UUID validation (P1.1)
- ✅ Production APK signing (P1.2)
- ✅ Backup protection (`allowBackup=false`, P1.3)
- ✅ HTTPS-only enforcement (P1.4)
- ✅ Crypto test coverage >95% (61 tests, P0.5 + P1.5)

**Performance Optimization:**
- ✅ N+1 query elimination (93.4% reduction, P0.4)
- ✅ Custom `MineralPagingSource` with batch loading
- ✅ Atomic transactions for data integrity (P0.3)
- ✅ 6 transaction-wrapped operations in `MineralRepository`

**Code Quality:**
- ✅ God class eliminated: `BackupRepository` 744 → 117 LOC
- ✅ 4 new services extracted (Zip, Csv, Encryption, Mapper)
- ✅ Magic numbers centralized (UiConstants, DatabaseConstants)
- ✅ ProGuard rules refined (no wildcards)
- ✅ Detekt strict config (`maxIssues: 0`)

**Testing:**
- ✅ Test coverage: 40.5% (exceeds 35-40% target)
- ✅ 29 unit tests + 2 instrumented tests = 31 total
- ✅ ~344 total test cases across all files
- ✅ Critical paths: 100% coverage (crypto, deep links, transactions)

**Documentation:**
- ✅ 5 P2 implementation plans (Hilt, Composables, Performance, Cleanup, Summary)
- ✅ Comprehensive acceptance validation report
- ✅ Updated architecture documentation

### v1.5.0 (Photo Workflows & QR Scanning)
- See CHANGELOG.md for details

---

**Last Updated**: v1.6.0 (Post-Audit - 2025-11-14)
**Maintained By**: MineraLog Development Team
