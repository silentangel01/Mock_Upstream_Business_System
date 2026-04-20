# OkHttp platform adapters (Android/Conscrypt/BouncyCastle/OpenJSSE not present on desktop JVM)
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn android.**
-dontwarn dalvik.**

# Ktor
-dontwarn io.ktor.network.sockets.**
-dontwarn io.ktor.utils.io.**

# SLF4J
-dontwarn org.slf4j.**

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mubs.mobile.data.model.**$$serializer { *; }
-keepclassmembers class com.mubs.mobile.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.mubs.mobile.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Ktor client classes
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
