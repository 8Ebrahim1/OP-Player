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
-dontnote androidx.media3.**

# Media source factories are resolved by name from DefaultMediaSourceFactory,
# so only their constructors have to survive shrinking.
-keep class androidx.media3.exoplayer.dash.DashMediaSource$Factory { <init>(...); }
-keep class androidx.media3.exoplayer.hls.HlsMediaSource$Factory { <init>(...); }
-keep class androidx.media3.exoplayer.smoothstreaming.SsMediaSource$Factory { <init>(...); }
-keep class androidx.media3.exoplayer.rtsp.RtspMediaSource$Factory { <init>(...); }

# Optional software decoders are instantiated reflectively by DefaultRenderersFactory.
-keep class androidx.media3.decoder.**Renderer { <init>(...); }

# ---- Parcelable (@Parcelize on PlaybackRequest / EpisodePattern) ----
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ---- Compose ----
-dontwarn org.jetbrains.annotations.**
