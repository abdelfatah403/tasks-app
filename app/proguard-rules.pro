# Add project specific ProGuard rules here.
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.taskflow.app.data.model.**$$serializer { *; }
-keepclassmembers class com.taskflow.app.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.taskflow.app.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
