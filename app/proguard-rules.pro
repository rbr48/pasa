# PASA ProGuard Rules

# Keep generic signatures and reflection attributes (CRITICAL for Retrofit suspend functions & Gson)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses, EnclosingMethod
-keepattributes AnnotationDefault

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
-keep class com.izhaanintellect.pasa.admin.PasaDeviceAdmin { *; }

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
