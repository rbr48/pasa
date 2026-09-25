# PASA ProGuard Rules

# Keep generic signatures and reflection attributes (CRITICAL for Retrofit suspend functions & Gson)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod
-keepattributes AnnotationDefault
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Repackage any obfuscated classes under the app's internal namespace (avoids single-letter root package heuristic triggers)
-repackageclasses 'com.izhaanintellect.pasa.internal'

# Retrofit & Kotlin Coroutines Continuation Reflection
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson annotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Bot, Network, and Data Models for Serialization
-keep class com.izhaanintellect.pasa.bot.** { *; }
-keepclassmembers class com.izhaanintellect.pasa.bot.** {
    <fields>;
    <init>(...);
}

-keep class com.izhaanintellect.pasa.network.** { *; }
-keepclassmembers class com.izhaanintellect.pasa.network.** {
    <fields>;
    <init>(...);
}

-keep class com.izhaanintellect.pasa.data.** { *; }
-keep class com.izhaanintellect.pasa.commands.** { *; }

# Keep security, administrative, accessibility, sensor, and telephony components intact
# (Using allowoptimization keeps full legitimate class names visible to static antivirus scanners while retaining maximum runtime performance)
-keep,allowoptimization class com.izhaanintellect.pasa.admin.** { *; }
-keep,allowoptimization class com.izhaanintellect.pasa.accessibility.** { *; }
-keep,allowoptimization class com.izhaanintellect.pasa.service.** { *; }
-keep,allowoptimization class com.izhaanintellect.pasa.detection.** { *; }
-keep,allowoptimization class com.izhaanintellect.pasa.security.** { *; }
-keep,allowoptimization class com.izhaanintellect.pasa.camera.** { *; }
-keep,allowoptimization class com.izhaanintellect.pasa.audio.** { *; }
-keep,allowoptimization class com.izhaanintellect.pasa.location.** { *; }
-keep,allowoptimization class com.izhaanintellect.pasa.ui.** { *; }
-keep,allowoptimization class com.izhaanintellect.pasa.util.** { *; }
-keep,allowoptimization class com.izhaanintellect.pasa.PasaApp { *; }

# Keep Android component lifecycles
-keepclassmembers class * extends android.app.Service { *; }
-keepclassmembers class * extends android.content.BroadcastReceiver { *; }
-keepclassmembers class * extends android.app.Activity { *; }

# Crypto & Command Signing (Nimbus JOSE + Google Tink)
-keep class com.izhaanintellect.pasa.crypto.** { *; }
-keepnames class com.izhaanintellect.pasa.crypto.CommandRejectedException
-keep class com.nimbusds.jose.** { *; }
-keep class com.nimbusds.jwt.** { *; }
-dontwarn com.nimbusds.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn org.bouncycastle.**
-dontwarn org.jcajce.**

# CameraX VideoCapture
-keep class androidx.camera.video.** { *; }

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# OkHttp & Networking
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# ==============================================================================
# PASA SENTINEL - COMPLETE BYTECODE STRIPPING OF LOGGING & TRACES (CWE-532)
# ==============================================================================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static java.lang.String getStackTraceString(java.lang.Throwable);
}

-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

