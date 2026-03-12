# Keep public API classes
-keep class com.himetrica.android.Himetrica { *; }
-keep class com.himetrica.android.HimetricaConfig { *; }
-keep class com.himetrica.android.HimetricaConfig$Builder { *; }
-keep class com.himetrica.android.ErrorSeverity { *; }

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
-keep,includedescriptorclasses class com.himetrica.android.**$$serializer { *; }
-keepclassmembers class com.himetrica.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.himetrica.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Compose extensions if Compose is present
-keep class com.himetrica.android.compose.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
