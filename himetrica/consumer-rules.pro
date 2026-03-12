# Keep public API classes
-keep class com.himetrica.tracker.Himetrica { *; }
-keep class com.himetrica.tracker.HimetricaConfig { *; }
-keep class com.himetrica.tracker.HimetricaConfig$Builder { *; }
-keep class com.himetrica.tracker.ErrorSeverity { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializable models
-keep,includedescriptorclasses class com.himetrica.tracker.**$$serializer { *; }
-keepclassmembers class com.himetrica.tracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.himetrica.tracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Compose extensions if Compose is present
-keep class com.himetrica.tracker.compose.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
