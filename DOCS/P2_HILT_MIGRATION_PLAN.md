# P2.2: Hilt DI Migration Plan

## Status: Deferred (Requires Test Validation)

The Hilt DI migration is a significant architectural change that requires comprehensive testing to validate. This document outlines the migration plan for future implementation.

## Why Defer?

1. **Breaking changes**: Requires modifying 8 ViewModels, all Compose screens, and MineraLogApplication
2. **Test validation required**: All unit and integration tests must pass after migration
3. **Risk**: Cannot validate without running Gradle/tests (network constraints)
4. **Time**: Estimated 2-3 days of work with full test coverage

## Migration Steps (When Ready)

### Step 1: Add Hilt Dependencies

```kotlin
// gradle/libs.versions.toml
[versions]
hilt = "2.51"

[libraries]
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version = "1.2.0" }

[plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

```kotlin
// build.gradle.kts (root)
plugins {
    alias(libs.plugins.hilt) apply false
}

// app/build.gradle.kts
plugins {
    alias(libs.plugins.hilt)
}

dependencies {
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
```

### Step 2: Update Application Class

```kotlin
// MineraLogApplication.kt
@HiltAndroidApp
class MineraLogApplication : Application(), Configuration.Provider {
    // Remove all lazy repository initializations
    // Hilt will handle injection
}
```

### Step 3: Create Hilt Modules

```kotlin
// di/DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MineraLogDatabase {
        return MineraLogDatabase.getDatabase(context)
    }

    @Provides
    fun provideMineralDao(database: MineraLogDatabase) = database.mineralDao()

    @Provides
    fun provideProvenanceDao(database: MineraLogDatabase) = database.provenanceDao()

    @Provides
    fun provideStorageDao(database: MineraLogDatabase) = database.storageDao()

    @Provides
    fun providePhotoDao(database: MineraLogDatabase) = database.photoDao()

    @Provides
    fun provideFilterPresetDao(database: MineraLogDatabase) = database.filterPresetDao()
}

// di/RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMineralRepository(
        database: MineraLogDatabase,
        mineralDao: MineralDao,
        provenanceDao: ProvenanceDao,
        storageDao: StorageDao,
        photoDao: PhotoDao
    ): MineralRepository {
        return MineralRepositoryImpl(database, mineralDao, provenanceDao, storageDao, photoDao)
    }

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
        database: MineraLogDatabase
    ): BackupRepository {
        return BackupRepositoryImpl(context, database)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideStatisticsRepository(
        mineralDao: MineralDao
    ): StatisticsRepository {
        return StatisticsRepositoryImpl(mineralDao)
    }

    @Provides
    @Singleton
    fun provideFilterPresetRepository(
        filterPresetDao: FilterPresetDao
    ): FilterPresetRepository {
        return FilterPresetRepositoryImpl(filterPresetDao)
    }
}
```

### Step 4: Update ViewModels (8 files)

Convert each ViewModel from:

```kotlin
class HomeViewModel(
    private val context: Context,
    private val mineralRepository: MineralRepository,
    // ...
) : ViewModel() { ... }

class HomeViewModelFactory(...) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // factory code
    }
}
```

To:

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mineralRepository: MineralRepository,
    // ...
) : ViewModel() { ... }

// DELETE HomeViewModelFactory class
```

**Files to modify:**
1. `/app/src/main/java/net/meshcore/mineralog/ui/screens/home/HomeViewModel.kt`
2. `/app/src/main/java/net/meshcore/mineralog/ui/screens/settings/SettingsViewModel.kt`
3. `/app/src/main/java/net/meshcore/mineralog/ui/screens/add/AddMineralViewModel.kt`
4. `/app/src/main/java/net/meshcore/mineralog/ui/screens/edit/EditMineralViewModel.kt`
5. `/app/src/main/java/net/meshcore/mineralog/ui/screens/detail/MineralDetailViewModel.kt`
6. `/app/src/main/java/net/meshcore/mineralog/ui/screens/gallery/PhotoGalleryViewModel.kt`
7. `/app/src/main/java/net/meshcore/mineralog/ui/screens/comparator/ComparatorViewModel.kt`
8. `/app/src/main/java/net/meshcore/mineralog/ui/screens/statistics/StatisticsViewModel.kt`

### Step 5: Update Compose Screens

Convert from:

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            context = LocalContext.current.applicationContext,
            mineralRepository = (LocalContext.current.applicationContext as MineraLogApplication).mineralRepository,
            // ...
        )
    )
) { ... }
```

To:

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) { ... }
```

### Step 6: Update MainActivity

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Hilt will inject dependencies as needed
}
```

### Step 7: Update ProGuard Rules

```proguard
# Hilt
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclassmembers class * {
    @javax.inject.* <fields>;
    @javax.inject.* <init>(...);
}
```

### Step 8: Update Tests

All tests must be updated to use Hilt testing framework:

```kotlin
@HiltAndroidTest
class HomeViewModelTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var mineralRepository: MineralRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }
}
```

## Benefits of Migration

1. **Remove boilerplate**: ~120 LOC of ViewModelFactory code eliminated
2. **Improved testability**: Easier to mock dependencies
3. **Type safety**: Compile-time dependency resolution
4. **Standard architecture**: Follow Android best practices
5. **Scalability**: Easier to add new dependencies

## Risks

1. **Breaking changes**: All screens and ViewModels must be updated simultaneously
2. **Test updates**: All unit and integration tests must be updated
3. **Build configuration**: ProGuard rules must be updated for Hilt
4. **Learning curve**: Team must understand Hilt concepts

## Recommendation

**Defer Hilt migration to a dedicated sprint with:**
- Full test coverage validation
- Code review by Android expert
- Gradual rollout strategy (if possible)
- Comprehensive documentation

## Alternative: Keep Current Service Locator

The current service locator pattern (MineraLogApplication) is:
- ✅ Simple and understandable
- ✅ Working well for current app size
- ✅ No breaking changes required
- ✅ Easier to debug for small teams

Consider keeping it unless team has strong Hilt experience or app grows significantly.
