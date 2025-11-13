# Add project specific ProGuard rules here.

# Keep Room entities and DAOs
-keep class net.meshcore.mineralog.data.local.entity.** { *; }
-keep interface net.meshcore.mineralog.data.local.dao.** { *; }

# Keep Tink crypto classes
-keep class com.google.crypto.tink.** { *; }

# Keep Argon2
-keep class com.lambdapioneer.argon2kt.** { *; }

# Keep serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class net.meshcore.mineralog.**$$serializer { *; }
-keepclassmembers class net.meshcore.mineralog.** {
    *** Companion;
}
-keepclasseswithmembers class net.meshcore.mineralog.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep ZXing
-keep class com.google.zxing.** { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }

# Keep Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }

# Okio
-dontwarn okio.**
-keep class okio.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Jetpack Compose
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose Navigation
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment

# Domain models (used in serialization and Room)
-keep class net.meshcore.mineralog.domain.model.** { *; }
-keepclassmembers class net.meshcore.mineralog.domain.model.** { *; }

# ViewModels (reflection via ViewModelFactory)
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModelProvider$Factory {
    <init>(...);
}

# CameraX
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Coil image loading
-keep class coil.** { *; }
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
