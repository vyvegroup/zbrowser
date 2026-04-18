# ZBrowser ProGuard Rules

# Keep WebView JavaScript interface
-keepclassmembers class com.zbrowser.app.web.** {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Room entities
-keep class com.zbrowser.app.data.** { *; }

# Keep WebView related
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# General Android
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
