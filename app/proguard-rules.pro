# ── Retrofit + OkHttp ──
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# ── Gson: keep all data model classes (fields used via reflection) ──
-keep class com.app.viam.data.model.** { *; }

# ── Gson internals ──
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ── Keep @SerializedName annotations so Gson mapping works ──
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── CameraX / ML Kit (QR scanning) ──
-dontwarn com.google.mlkit.**
-keep class com.google.mlkit.** { *; }

# ── Passport / OAuth ──
-dontwarn org.conscrypt.**

# ── Kotlin serialization (if used) ──
-keepclassmembers class kotlinx.serialization.** { *; }

# ── Preserve line numbers for crash reports ──
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
