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
