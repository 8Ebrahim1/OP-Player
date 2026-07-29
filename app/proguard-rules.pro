# ---- kotlinx.serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.opplayer.app.**$$serializer { *; }
-keepclassmembers class com.opplayer.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.opplayer.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Media3 / ExoPlayer ----
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.** { *; }

# ---- Compose ----
-dontwarn org.jetbrains.annotations.**
