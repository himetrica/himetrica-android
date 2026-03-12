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
-keep,includedescriptorclasses class com.himetrica.tracker.**$$serializer { *; }
-keepclassmembers class com.himetrica.tracker.** {
    *** Companion;
}
-keepclasseswithmembers class com.himetrica.tracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}
