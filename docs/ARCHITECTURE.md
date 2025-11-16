# MineraLog Architecture

## Overview

MineraLog follows **Clean Architecture** principles with clear separation between data, domain, and presentation layers. The architecture is designed to be testable, maintainable, and scalable.

## Architecture Layers

### 1. Data Layer
The data layer is responsible for all data operations, including persistence, network, and file operations.

#### Database (Room)
- **Location**: `app/src/main/java/net/meshcore/mineralog/data/local/`
- **Database**: `MineraLogDatabase.kt` - SQLCipher-encrypted Room database
- **Current Schema Version**: 7
- **Encryption**: AES-256 via SQLCipher with Argon2id-derived keys

**Key Entities**:
- `MineralEntity` - Main mineral specimen data
- `ReferenceMineralEntity` - Reference mineral library
- `PhotoEntity` - Photo metadata and paths
- `ProvenanceEntity` - Mineral provenance information
- `StorageEntity` - Storage location data
- `SimplePropertiesEntity` - Physical properties (hardness, density, etc.)
- `MineralComponentEntity` - Chemical composition
- `FilterPresetEntity` - Saved filter configurations

**DAOs**:
- `MineralDao` - Mineral CRUD + search queries with Paging 3
- `ReferenceMineralDao` - Reference library operations
- `PhotoDao` - Photo management
- `ProvenanceDao` - Provenance tracking
- `StorageDao` - Storage location management
- `SimplePropertiesDao` - Properties operations
- `MineralComponentDao` - Composition management
- `FilterPresetDao` - Filter preset persistence

#### Repositories
- **Location**: `app/src/main/java/net/meshcore/mineralog/data/repository/`
- **Pattern**: Repository pattern providing single source of truth

**Key Repositories**:
- `MineralRepositoryImpl` - Mineral data operations, combines multiple DAOs
- `ReferenceMineralRepositoryImpl` - Reference mineral library
- `BackupRepositoryImpl` - Backup/restore operations (CSV, encrypted ZIP)
- `SettingsRepositoryImpl` - User settings via DataStore Preferences
- `StatisticsRepositoryImpl` - Collection statistics aggregation
- `FilterPresetRepositoryImpl` - Filter preset management

#### Migrations
- **Location**: `app/src/main/java/net/meshcore/mineralog/data/local/migrations/`
- **Versions**: v1 → v7 (current)
- **Migration Strategy**: Additive migrations with data preservation
- **Key Migrations**:
  - v6: Added Reference Mineral library
  - v7: Enhanced photo metadata with UV/Macro support

### 2. Domain Layer
The domain layer contains business logic and domain models.

#### Domain Models
- **Location**: `app/src/main/java/net/meshcore/mineralog/domain/model/`
- **Purpose**: Pure Kotlin POJOs independent of framework

**Key Models**:
- `Mineral` - Domain representation of mineral specimen
- `ReferenceMineral` - Domain representation of reference mineral
- `Photo` - Photo metadata
- `Provenance` - Provenance information
- `Storage` - Storage location
- `SimpleProperties` - Physical properties
- `MineralComponent` - Chemical composition

#### Mappers
- **Location**: `app/src/main/java/net/meshcore/mineralog/data/mapper/EntityMappers.kt`
- **Purpose**: Convert between Entity (data layer) and Model (domain layer)
- **Pattern**: Extension functions for bidirectional mapping

### 3. Presentation Layer
The presentation layer handles UI and user interactions using Jetpack Compose.

#### ViewModels
- **Location**: `app/src/main/java/net/meshcore/mineralog/ui/screens/*/`
- **Pattern**: MVVM with StateFlow for state management
- **Lifecycle**: `ViewModelScoped` (manual DI via Application)

**Key ViewModels**:
- `HomeViewModel` - Home screen, mineral list, search, filtering
- `AddMineralViewModel` - Add mineral flow with draft persistence
- `EditMineralViewModel` - Edit mineral flow
- `MineralDetailViewModel` - Mineral detail display, QR label generation
- `SettingsViewModel` - Settings management
- `StatisticsViewModel` - Collection statistics
- `ReferenceMineralListViewModel` - Reference library browsing
- `ComparatorViewModel` - Side-by-side mineral comparison

#### UI Components (Jetpack Compose)
- **Location**: `app/src/main/java/net/meshcore/mineralog/ui/`
- **Screens**: Full-screen composables (`*Screen.kt`)
- **Components**: Reusable composables (`ui/components/`)
- **Theme**: Material Design 3 with dynamic color support

**Key Composable Categories**:
- **Screens**: Top-level destinations (HomeScreen, AddMineralScreen, etc.)
- **Components**: Reusable UI elements (PhotoManager, TagChipsInput, etc.)
- **Dialogs**: Modal dialogs (ImportResultDialog, ColumnMappingDialog, etc.)
- **Empty States**: `EmptyStateComposables.kt` - Empty/loading/error states
- **Error Handling**: `ErrorStateComposables.kt` - Consistent error UI

#### Navigation
- **Location**: `app/src/main/java/net/meshcore/mineralog/ui/navigation/MineraLogNavHost.kt`
- **Library**: Jetpack Navigation Compose
- **Pattern**: Type-safe navigation with sealed routes
- **Deep Linking**: Support for mineral detail via QR codes

## Data Flow

```
User Action → Screen → ViewModel → Repository → DAO → Database
                ↓                      ↓
           UI State ← StateFlow ← Flow (Room)
```

### Example: Loading Minerals
1. User opens HomeScreen
2. `HomeViewModel` subscribes to `mineralRepository.getAllMineralsPaged()`
3. Repository returns Paging3 `Flow<PagingData<Mineral>>`
4. ViewModel exposes as `StateFlow`
5. Screen collects via `collectAsLazyPagingItems()` and renders

### Example: Adding Mineral
1. User fills form in AddMineralScreen
2. User clicks Save
3. `AddMineralViewModel.saveMineralWithPhotos()` called
4. ViewModel calls `mineralRepository.insertMineralWithPhotos(mineral)`
5. Repository coordinates:
   - Insert `MineralEntity` via `MineralDao`
   - Insert `PhotoEntity` records via `PhotoDao`
   - Copy photos to internal storage
   - All in a single transaction
6. ViewModel updates UI state on success/failure

## Dependency Injection

### Current: Manual DI via Application
- **Implementation**: `MineraLogApplication.kt`
- **Scope**: Singleton for Database, Repositories, Crypto
- **ViewModels**: Manual factory pattern (`*ViewModelFactory.kt`)

**Pros**: Simple, explicit, no library overhead
**Cons**: Boilerplate factory code for each ViewModel

### Future: Hilt (Deferred)
- **Status**: Dependencies added in v3.0.0-beta
- **Migration Path**:
  1. Annotate Application with `@HiltAndroidApp`
  2. Create `@Module` for Database, Repositories
  3. Convert ViewModels to `@HiltViewModel`
  4. Remove manual factories
- **Benefits**: Reduced boilerplate, compile-time validation

## Security Architecture

### Database Encryption
- **Library**: SQLCipher 4.5.4
- **Algorithm**: AES-256-CBC
- **Key Derivation**: Argon2id with:
  - Memory: 64 MB
  - Iterations: 3
  - Parallelism: 4
  - Output: 256-bit key
- **Key Storage**: Android Keystore via Tink library
- **Migration**: Master key rotation support

### Backup Encryption
- **Format**: Password-protected ZIP
- **Content**: Encrypted CSV files + photos
- **Key Derivation**: Same Argon2id parameters as database
- **Integrity**: SHA-256 checksums for all files

### Photo Storage
- **Location**: App-internal storage (`context.filesDir/photos/`)
- **Permissions**: No external storage access (scoped storage)
- **Encryption**: Filesystem-level encryption (Android 7.0+)
- **Cleanup**: Automatic orphan photo deletion

## Performance Optimizations

### Paging 3
- **Load Size**: 20 items per page
- **Prefetch Distance**: 10 items
- **Database**: `LIMIT/OFFSET` queries via Room Paging integration
- **Benefits**: Memory efficient for large collections (1000+ minerals)

### Image Loading (Coil)
- **Memory Cache**: LRU cache, 25% available memory
- **Disk Cache**: 100 MB
- **Downsampling**: Automatic based on target ImageView size
- **Placeholders**: Skeleton loading during fetch

### Search Optimization
- **Full-Text Search**: Room FTS5 virtual table
- **Index**: Name, description, tags
- **Query Time**: Sub-100ms for 10,000 minerals (tested)

### Database Indexing
- **Indexed Columns**:
  - `name` - For sorting and search
  - `type` - For filtering by type
  - `date_acquired` - For sorting by date
  - `tags` - For tag filtering (JSON)

## Testing Architecture

### Unit Tests
- **Location**: `app/src/test/`
- **Framework**: JUnit 5 Jupiter
- **Mocking**: MockK
- **Coroutines**: kotlinx-coroutines-test with TestDispatcher
- **Coverage Target**: 60% (enforced via JaCoCo)

**Test Categories**:
- DAO tests (Robolectric + in-memory Room)
- Repository tests (MockK)
- ViewModel tests (Turbine for Flow testing)
- Utility/mapper tests

### Integration Tests
- **Location**: `app/src/androidTest/`
- **Framework**: AndroidX Test + Espresso
- **Runner**: AndroidJUnitRunner
- **Database**: In-memory Room for isolation

**Test Scenarios**:
- Full backup/restore cycle
- CSV import/export
- Database migrations (v1 → v7)

### UI Tests
- **Framework**: Compose UI Test
- **Semantics**: Content descriptions for accessibility testing
- **Isolation**: Mocked repositories via dependency injection

## Build Configuration

### Modules
- **app**: Main application module
- **No feature modules**: Monolithic for simplicity (may split in future)

### Build Variants
- **Debug**: Fast builds, logging enabled, SQLCipher passthrough
- **Release**: ProGuard/R8, resource shrinking, proper signing

### Build Tools
- **Gradle**: 8.7.3
- **Kotlin**: 2.0.21
- **AGP**: 8.7.3
- **Min SDK**: 27 (Android 8.1)
- **Target SDK**: 35 (Android 15)

### Code Quality
- **Detekt**: Static analysis for Kotlin
- **Lint**: Android-specific checks
- **JaCoCo**: Code coverage (60% minimum)

## Concurrency Model

### Coroutines
- **Scope**: `viewModelScope` for ViewModel operations
- **Dispatchers**:
  - `Dispatchers.IO` - Database, file I/O
  - `Dispatchers.Default` - CPU-intensive (Argon2, encryption)
  - `Dispatchers.Main` - UI updates
- **Error Handling**: Structured concurrency with `try-catch` in ViewModels

### Room
- **DAO Suspend Functions**: Automatic thread dispatch via Room
- **Flow**: Reactive queries with automatic updates
- **Transactions**: Atomic multi-table operations

## File Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/net/meshcore/mineralog/
│   │   │   ├── data/
│   │   │   │   ├── local/          # Room database
│   │   │   │   │   ├── dao/        # Data Access Objects
│   │   │   │   │   ├── entity/     # Room entities
│   │   │   │   │   └── migrations/ # Database migrations
│   │   │   │   ├── repository/     # Repository implementations
│   │   │   │   ├── service/        # Backup/export services
│   │   │   │   ├── mapper/         # Entity ↔ Model mappers
│   │   │   │   └── util/           # Data utilities
│   │   │   ├── domain/
│   │   │   │   └── model/          # Domain models
│   │   │   ├── ui/
│   │   │   │   ├── screens/        # Screen composables
│   │   │   │   ├── components/     # Reusable components
│   │   │   │   ├── navigation/     # Navigation setup
│   │   │   │   ├── theme/          # Material Design 3 theme
│   │   │   │   └── common/         # UI utilities (UiState)
│   │   │   ├── util/               # App-wide utilities
│   │   │   ├── MineraLogApplication.kt
│   │   │   └── MainActivity.kt
│   │   └── res/                    # Resources (layouts, strings, etc.)
│   ├── test/                       # Unit tests
│   └── androidTest/                # Instrumented tests
└── build.gradle.kts                # Build configuration
```

## Future Architecture Improvements

### Planned (v3.1.0+)
1. **Hilt Migration**: Remove ViewModelFactory boilerplate
2. **Feature Modules**: Split into `app`, `core-data`, `feature-collection`, `feature-reference`
3. **Offline-First Sync**: Support for cloud backup via WorkManager
4. **MVI Architecture**: Consider migrating to strict MVI for complex screens

### Under Consideration
1. **Compose Multiplatform**: iOS support
2. **KMP Data Layer**: Shared business logic across platforms
3. **GraphQL**: For future cloud sync (vs REST)

## References

- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)
- [SQLCipher](https://www.zetetic.net/sqlcipher/)
