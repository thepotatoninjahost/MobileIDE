# Add project specific ProGuard rules here.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

-keep class com.fuck13.mobileide.data.model.** { *; }
-keep class com.fuck13.mobileide.** { *; }

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
