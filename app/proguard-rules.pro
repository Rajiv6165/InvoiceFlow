# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firestore Models (CRITICAL - prevents data parsing crashes)
-keep class com.invoiceflow.billing.model.** { *; }
-keepclassmembers class com.invoiceflow.billing.model.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Kotlin Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Jetpack Compose
-keep class androidx.compose.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }

# CameraX
-keep class androidx.camera.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
