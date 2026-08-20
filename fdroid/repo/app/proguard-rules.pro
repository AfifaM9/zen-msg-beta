# Keep kotlin metadata used by Room and serialization.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
