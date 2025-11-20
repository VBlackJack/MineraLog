# ===============================================================================
# MineraLog ProGuard Rules - Production Release Configuration
# Version: 3.2.0
# Last Updated: 2025-01-20
# ===============================================================================
#
# This file contains R8/ProGuard rules for MineraLog release builds.
# Rules are organized by library/framework and documented with reasons.
#
# IMPORTANT: Modify with extreme caution. Missing rules can cause runtime crashes.
# Test thoroughly after any changes using: ./gradlew assembleRelease
# ===============================================================================

# ===============================================================================
# OPTIMIZATION SETTINGS
# ===============================================================================

# Enable aggressive optimization (5 passes for maximum size reduction)
-optimizationpasses 5

# Preserve case sensitivity for better obfuscation
-dontusemixedcaseclassnames

# Process third-party libraries as well
-dontskipnonpubliclibraryclasses

# Enable verbose logging for debugging ProGuard issues
-verbose

# ===============================================================================
# ANDROID FRAMEWORK CORE
# ===============================================================================

# Keep native methods (required for JNI)
# Reason: Native methods are called from native code, reflection cannot be used
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep custom views (required for XML inflation)
# Reason: Android framework uses reflection to instantiate views from XML
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep Parcelable implementations (required for IPC)
# Reason: Android Parcelable framework uses reflection to access CREATOR field
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep serialization attributes
# Reason: Required for proper serialization/deserialization
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# ===============================================================================
# HILT DEPENDENCY INJECTION (CRITICAL)
# ===============================================================================

# Keep Hilt entry point
# Reason: Hilt uses reflection to find @HiltAndroidApp annotated classes
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# Keep Hilt modules and components
# Reason: Hilt uses code generation and reflection for dependency injection
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.components.SingletonComponent class * { *; }

# Keep Hilt-injected constructors
# Reason: Hilt needs to see @Inject constructors for injection
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep Hilt ViewModels
# Reason: Hilt ViewModel injection uses reflection
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep all Hilt-generated classes
# Reason: Generated classes are used at runtime for injection
-keep class dagger.hilt.** { *; }
-keep class **_HiltModules** { *; }
-keep class **_HiltComponents** { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# Suppress Hilt warnings
-dontwarn dagger.hilt.**
-dontwarn javax.inject.**

# ===============================================================================
# ROOM DATABASE (CRITICAL)
# ===============================================================================

# Keep Room entities (v1.x - Core)
# Reason: Room uses reflection to access entity fields and constructors
-keep @androidx.room.Entity class * { *; }
-keep class net.meshcore.mineralog.data.local.entity.MineralEntity { *; }
-keep class net.meshcore.mineralog.data.local.entity.ProvenanceEntity { *; }
-keep class net.meshcore.mineralog.data.local.entity.StorageEntity { *; }
-keep class net.meshcore.mineralog.data.local.entity.PhotoEntity { *; }
-keep class net.meshcore.mineralog.data.local.entity.FilterPresetEntity { *; }

# Keep Room entities (v2.0 - Aggregates)
# Reason: Added in v2.0 for mineral aggregate support
-keep class net.meshcore.mineralog.data.local.entity.SimplePropertiesEntity { *; }
-keep class net.meshcore.mineralog.data.local.entity.MineralComponentEntity { *; }

# Keep Room entities (v3.0 - Reference Library)
# Reason: Added in v3.0 for reference mineral database
-keep class net.meshcore.mineralog.data.local.entity.ReferenceMineralEntity { *; }

# Keep Room DAOs (v1.x - Core)
# Reason: Room uses reflection to implement DAO interfaces
-keep interface net.meshcore.mineralog.data.local.dao.MineralDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.ProvenanceDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.StorageDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.PhotoDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.FilterPresetDao { *; }

# Keep Room DAOs (v2.0 - Specialized)
# Reason: Composite DAO pattern for better maintainability
-keep interface net.meshcore.mineralog.data.local.dao.MineralBasicDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.MineralQueryDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.MineralStatisticsDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.MineralPagingDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.SimplePropertiesDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.MineralComponentDao { *; }

# Keep Room DAOs (v3.0 - Reference Library)
# Reason: Reference mineral library support
-keep interface net.meshcore.mineralog.data.local.dao.ReferenceMineralDao { *; }

# Keep Room composite wrapper
# Reason: Non-DAO class that delegates to specialized DAOs
-keep class net.meshcore.mineralog.data.local.dao.MineralDaoComposite { *; }

# Keep Room database class
# Reason: Room database base class
-keep class net.meshcore.mineralog.data.local.MineraLogDatabase { *; }

# ===============================================================================
# DOMAIN MODELS (Used by Room, Serialization, and UI)
# ===============================================================================

# Keep domain models (v1.x - Core)
# Reason: Used for Room-to-domain mapping and JSON serialization
-keep class net.meshcore.mineralog.data.model.** { *; }
-keep class net.meshcore.mineralog.domain.model.Mineral { *; }
-keep class net.meshcore.mineralog.domain.model.Provenance { *; }
-keep class net.meshcore.mineralog.domain.model.Storage { *; }
-keep class net.meshcore.mineralog.domain.model.Photo { *; }
-keep class net.meshcore.mineralog.domain.model.FilterPreset { *; }

# Keep domain models (v2.0 - Aggregates)
# Reason: Mineral aggregate support
-keep class net.meshcore.mineralog.domain.model.MineralComponent { *; }
-keep class net.meshcore.mineralog.domain.model.SimpleProperties { *; }

# Keep domain models (v3.0 - Reference Library)
# Reason: Reference mineral library
-keep class net.meshcore.mineralog.domain.model.ReferenceMineralInfo { *; }

# Keep all fields and constructors of domain models
# Reason: Kotlinx Serialization and Room need access to all properties
-keepclassmembers class net.meshcore.mineralog.domain.model.** {
    <fields>;
    <init>(...);
}

# ===============================================================================
# ENUMS (CRITICAL - R8 often breaks enums)
# ===============================================================================

# Keep all enum classes
# Reason: R8 optimization can break enum valueOf() and values() methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# Keep specific enums used throughout the app
# Reason: These are used in database, UI state, and business logic
-keep enum net.meshcore.mineralog.domain.model.MineralType { *; }
-keep enum net.meshcore.mineralog.data.repository.ImportMode { *; }
-keep enum net.meshcore.mineralog.data.repository.CsvImportMode { *; }

# ===============================================================================
# SEALED CLASSES (State Management)
# ===============================================================================

# Keep sealed classes used for UI state
# Reason: Sealed classes use reflection for exhaustive when expressions
-keep class net.meshcore.mineralog.ui.screens.settings.BackupExportState { *; }
-keep class net.meshcore.mineralog.ui.screens.settings.BackupImportState { *; }
-keep class net.meshcore.mineralog.ui.screens.settings.CsvImportState { *; }
-keep class net.meshcore.mineralog.ui.screens.settings.SampleDataState { *; }
-keep class net.meshcore.mineralog.ui.screens.settings.ReanalysisState { *; }
-keep class net.meshcore.mineralog.ui.screens.add.AddMineralState { *; }
-keep class net.meshcore.mineralog.ui.screens.edit.UpdateMineralState { *; }

# Keep all sealed class subclasses
# Reason: Kotlin reflection needs access to all sealed subclasses
-keep class * extends net.meshcore.mineralog.ui.screens.settings.BackupExportState { *; }
-keep class * extends net.meshcore.mineralog.ui.screens.settings.BackupImportState { *; }
-keep class * extends net.meshcore.mineralog.ui.screens.settings.CsvImportState { *; }
-keep class * extends net.meshcore.mineralog.ui.screens.settings.SampleDataState { *; }
-keep class * extends net.meshcore.mineralog.ui.screens.settings.ReanalysisState { *; }
-keep class * extends net.meshcore.mineralog.ui.screens.add.AddMineralState { *; }
-keep class * extends net.meshcore.mineralog.ui.screens.edit.UpdateMineralState { *; }

# ===============================================================================
# KOTLINX SERIALIZATION (JSON/CSV Export)
# ===============================================================================

# Keep serialization annotations
# Reason: Kotlinx Serialization uses annotations for code generation
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Companion objects (contain serializers)
# Reason: Kotlinx Serialization generates serializers in Companion objects
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep our serializable domain models
# Reason: Used for CSV/JSON export functionality
-keep,includedescriptorclasses class net.meshcore.mineralog.domain.model.**$$serializer { *; }
-keepclassmembers class net.meshcore.mineralog.domain.model.** {
    *** Companion;
}
-keepclasseswithmembers class net.meshcore.mineralog.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ===============================================================================
# CRYPTOGRAPHY (CRITICAL - SECURITY)
# ===============================================================================

# SQLCipher - Database encryption
# Reason: Native library for encrypted database, must keep all JNI methods
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep interface net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# EncryptedSharedPreferences & MasterKey
# Reason: Used to persist database encryption key, critical for app functionality
# WITHOUT THESE RULES: Database cannot be decrypted after app restart
-keep class androidx.security.crypto.EncryptedSharedPreferences { *; }
-keep class androidx.security.crypto.EncryptedSharedPreferences$* { *; }
-keep class androidx.security.crypto.MasterKey { *; }
-keep class androidx.security.crypto.MasterKey$* { *; }
-keep class com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient { *; }
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn androidx.security.crypto.**

# Tink crypto library
# Reason: Used for backup encryption (AES-GCM)
-keep class com.google.crypto.tink.Aead { *; }
-keep class com.google.crypto.tink.KeysetHandle { *; }
-keep class com.google.crypto.tink.aead.AeadConfig { *; }
-keep class com.google.crypto.tink.aead.AeadKeyTemplates { *; }
-keep class com.google.crypto.tink.integration.android.AndroidKeysetManager { *; }
-keep class com.google.crypto.tink.integration.android.AndroidKeysetManager$Builder { *; }

# Argon2 - Password hashing
# Reason: Used for backup password derivation
-keep class com.lambdapioneer.argon2kt.Argon2Kt { *; }
-keep class com.lambdapioneer.argon2kt.Argon2KtResult { *; }
-keep class com.lambdapioneer.argon2kt.Argon2Mode { *; }
-keep class com.lambdapioneer.argon2kt.Argon2Version { *; }

# Our crypto implementation classes
# Reason: Critical for database security and backup encryption
-keep class net.meshcore.mineralog.data.local.DatabaseKeyManager { *; }
-keep class net.meshcore.mineralog.data.local.DatabaseMigrationHelper { *; }
-keep class net.meshcore.mineralog.data.local.DatabaseMigrationHelper$MigrationResult { *; }
-keep class net.meshcore.mineralog.data.local.DatabaseMigrationHelper$MigrationResult$* { *; }
-keep class net.meshcore.mineralog.data.crypto.** { *; }

# ===============================================================================
# JETPACK COMPOSE
# ===============================================================================

# Keep Composable functions
# Reason: Compose compiler uses reflection for recomposition tracking
-keep class androidx.compose.runtime.Composable { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# Keep Compose Navigation
# Reason: Navigation component uses reflection for route handling
-keep class androidx.navigation.compose.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.navigation.Navigator { *; }

# ===============================================================================
# VIEWMODELS (Hilt Injection + Factory Pattern)
# ===============================================================================

# Keep all ViewModels
# Reason: ViewModelFactory uses reflection to instantiate ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep ViewModelFactory implementations
# Reason: Factory pattern uses reflection
-keep class * extends androidx.lifecycle.ViewModelProvider$Factory {
    <init>(...);
}

# ===============================================================================
# KOTLIN COROUTINES
# ===============================================================================

# Keep coroutine dispatchers
# Reason: ServiceLoader uses reflection to load MainDispatcherFactory
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep volatile fields in coroutines
# Reason: Coroutines use volatile for thread-safe state
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ===============================================================================
# THIRD-PARTY LIBRARIES
# ===============================================================================

# ZXing - QR Code Generation
# Reason: Used for QR code export feature
-keep class com.google.zxing.BarcodeFormat { *; }
-keep class com.google.zxing.EncodeHintType { *; }
-keep class com.google.zxing.Writer { *; }
-keep class com.google.zxing.WriterException { *; }
-keep class com.google.zxing.common.BitMatrix { *; }
-keep class com.google.zxing.qrcode.QRCodeWriter { *; }
-keep class com.google.zxing.qrcode.decoder.ErrorCorrectionLevel { *; }

# ML Kit - Barcode Scanning
# Reason: Used for barcode scanning in add mineral flow
-keep class com.google.mlkit.vision.barcode.Barcode { *; }
-keep class com.google.mlkit.vision.barcode.BarcodeScanner { *; }
-keep class com.google.mlkit.vision.barcode.BarcodeScanning { *; }
-keep class com.google.mlkit.vision.barcode.BarcodeScannerOptions { *; }
-keep class com.google.mlkit.vision.common.InputImage { *; }

# Google Maps
# Reason: Used for provenance location visualization
-keep class com.google.android.gms.maps.CameraUpdateFactory { *; }
-keep class com.google.android.gms.maps.GoogleMap { *; }
-keep class com.google.android.gms.maps.model.LatLng { *; }
-keep class com.google.android.gms.maps.model.MarkerOptions { *; }
-keep class com.google.android.gms.maps.model.CameraPosition { *; }
-keep class com.google.maps.android.compose.** { *; }

# Okio - Efficient I/O
# Reason: Used by backup/CSV export for efficient file operations
-dontwarn okio.**
-keep class okio.Buffer { *; }
-keep class okio.BufferedSink { *; }
-keep class okio.BufferedSource { *; }
-keep class okio.Okio { *; }
-keep class okio.Sink { *; }
-keep class okio.Source { *; }

# CameraX
# Reason: Used for photo capture feature
-keep class androidx.camera.core.Camera { *; }
-keep class androidx.camera.core.CameraSelector { *; }
-keep class androidx.camera.core.ImageCapture { *; }
-keep class androidx.camera.core.Preview { *; }
-keep class androidx.camera.lifecycle.ProcessCameraProvider { *; }
-keep interface androidx.camera.core.ImageCapture$OnImageSavedCallback { *; }
-dontwarn androidx.camera.**

# Coil - Image Loading
# Reason: Used for efficient image loading in gallery and detail views
-keep class coil.ImageLoader { *; }
-keep class coil.request.ImageRequest { *; }
-keep class coil.size.Size { *; }
-keep class coil.transform.Transformation { *; }
-dontwarn coil.**

# DataStore - Preferences
# Reason: Used for app settings persistence
-keep class androidx.datastore.*.** { *; }

# WorkManager
# Reason: Future use for background tasks (backup reminders, sync)
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker {
    <init>(...);
}

# ===============================================================================
# SECURITY: LOG REMOVAL (CRITICAL)
# ===============================================================================

# Remove ALL Android Log statements in release
# Reason: Prevent information disclosure via logcat
# Security Impact: High - Logs can leak sensitive data (encryption keys, user data)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Remove custom AppLogger statements
# Reason: Same as above - prevent information leakage
-assumenosideeffects class net.meshcore.mineralog.util.AppLogger {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ===============================================================================
# END OF PROGUARD RULES
# ===============================================================================
