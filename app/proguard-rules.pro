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

-dontwarn androidx.media3.**
-dontnote androidx.media3.**

-keep class androidx.media3.exoplayer.dash.DashMediaSource$Factory { <init>(...); }
-keep class androidx.media3.exoplayer.hls.HlsMediaSource$Factory { <init>(...); }
-keep class androidx.media3.exoplayer.smoothstreaming.SsMediaSource$Factory { <init>(...); }
-keep class androidx.media3.exoplayer.rtsp.RtspMediaSource$Factory { <init>(...); }

-keep class androidx.media3.decoder.**Renderer { <init>(...); }

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-dontwarn org.jetbrains.annotations.**
