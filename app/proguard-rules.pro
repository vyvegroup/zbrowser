# Add project specific ProGuard rules here.
# General Android WebView rules
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebView related classes
-keep class com.zbrowser.app.web.** { *; }
-keep class com.zbrowser.app.data.** { *; }
-keep class com.zbrowser.app.storage.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# OkHttp (used by Coil)
-dontwarn okhttp3.**
-dontwarn okio.**
