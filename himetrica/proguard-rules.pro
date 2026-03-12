# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Himetrica models
-keep,includedescriptorclasses class com.himetrica.android.**$$serializer { *; }
-keepclassmembers class com.himetrica.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.himetrica.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
