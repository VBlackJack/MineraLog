# Add project specific ProGuard rules here.

# Keep Room entities and DAOs - be specific to avoid over-keeping
-keep class net.meshcore.mineralog.data.local.entity.MineralEntity { *; }
-keep class net.meshcore.mineralog.data.local.entity.ProvenanceEntity { *; }
-keep class net.meshcore.mineralog.data.local.entity.StorageEntity { *; }
-keep class net.meshcore.mineralog.data.local.entity.PhotoEntity { *; }
-keep class net.meshcore.mineralog.data.local.entity.FilterPresetEntity { *; }
-keep interface net.meshcore.mineralog.data.local.dao.MineralDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.ProvenanceDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.StorageDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.PhotoDao { *; }
-keep interface net.meshcore.mineralog.data.local.dao.FilterPresetDao { *; }

# Keep Tink crypto classes - minimum required
-keep class com.google.crypto.tink.Aead { *; }
-keep class com.google.crypto.tink.KeysetHandle { *; }
-keep class com.google.crypto.tink.aead.AeadConfig { *; }
-keep class com.google.crypto.tink.aead.AeadKeyTemplates { *; }
-keep class com.google.crypto.tink.integration.android.AndroidKeysetManager { *; }
-keep class com.google.crypto.tink.integration.android.AndroidKeysetManager$Builder { *; }

# Keep Argon2 - minimum required
-keep class com.lambdapioneer.argon2kt.Argon2Kt { *; }
-keep class com.lambdapioneer.argon2kt.Argon2KtResult { *; }
-keep class com.lambdapioneer.argon2kt.Argon2Mode { *; }
-keep class com.lambdapioneer.argon2kt.Argon2Version { *; }

# Keep serialization classes - specific to our domain models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class net.meshcore.mineralog.domain.model.**$$serializer { *; }
-keepclassmembers class net.meshcore.mineralog.domain.model.** {
    *** Companion;
}
-keepclasseswithmembers class net.meshcore.mineralog.domain.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep ZXing - minimum required for QR code generation
-keep class com.google.zxing.BarcodeFormat { *; }
-keep class com.google.zxing.EncodeHintType { *; }
-keep class com.google.zxing.Writer { *; }
-keep class com.google.zxing.WriterException { *; }
-keep class com.google.zxing.common.BitMatrix { *; }
-keep class com.google.zxing.qrcode.QRCodeWriter { *; }
-keep class com.google.zxing.qrcode.decoder.ErrorCorrectionLevel { *; }

# Keep ML Kit - minimum required for barcode scanning
-keep class com.google.mlkit.vision.barcode.Barcode { *; }
-keep class com.google.mlkit.vision.barcode.BarcodeScanner { *; }
-keep class com.google.mlkit.vision.barcode.BarcodeScanning { *; }
-keep class com.google.mlkit.vision.barcode.BarcodeScannerOptions { *; }
-keep class com.google.mlkit.vision.common.InputImage { *; }

# Keep Google Maps - minimum required
-keep class com.google.android.gms.maps.CameraUpdateFactory { *; }
-keep class com.google.android.gms.maps.GoogleMap { *; }
-keep class com.google.android.gms.maps.model.LatLng { *; }
-keep class com.google.android.gms.maps.model.MarkerOptions { *; }
-keep class com.google.android.gms.maps.model.CameraPosition { *; }
-keep class com.google.maps.android.compose.** { *; }

# Okio - minimum required
-dontwarn okio.**
-keep class okio.Buffer { *; }
-keep class okio.BufferedSink { *; }
-keep class okio.BufferedSource { *; }
-keep class okio.Okio { *; }
-keep class okio.Sink { *; }
-keep class okio.Source { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Jetpack Compose - minimum required (Compose handles most obfuscation internally)
-keep class androidx.compose.runtime.Composable { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**

# Compose Navigation - minimum required
-keep class androidx.navigation.compose.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.navigation.Navigator { *; }

# Domain models (used in serialization and Room) - specific classes
-keep class net.meshcore.mineralog.domain.model.Mineral { *; }
-keep class net.meshcore.mineralog.domain.model.Provenance { *; }
-keep class net.meshcore.mineralog.domain.model.Storage { *; }
-keep class net.meshcore.mineralog.domain.model.Photo { *; }
-keep class net.meshcore.mineralog.domain.model.FilterPreset { *; }
-keepclassmembers class net.meshcore.mineralog.domain.model.* {
    <fields>;
    <init>(...);
}

# ViewModels (reflection via ViewModelFactory)
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModelProvider$Factory {
    <init>(...);
}

# CameraX - minimum required
-keep class androidx.camera.core.Camera { *; }
-keep class androidx.camera.core.CameraSelector { *; }
-keep class androidx.camera.core.ImageCapture { *; }
-keep class androidx.camera.core.Preview { *; }
-keep class androidx.camera.lifecycle.ProcessCameraProvider { *; }
-keep interface androidx.camera.core.ImageCapture$OnImageSavedCallback { *; }
-dontwarn androidx.camera.**

# Coil image loading - minimum required
-keep class coil.ImageLoader { *; }
-keep class coil.request.ImageRequest { *; }
-keep class coil.size.Size { *; }
-keep class coil.transform.Transformation { *; }
-dontwarn coil.**

# DataStore
-keep class androidx.datastore.*.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker {
    <init>(...);
}

# Remove all logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Optimize and obfuscate
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Prevent obfuscation of Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
