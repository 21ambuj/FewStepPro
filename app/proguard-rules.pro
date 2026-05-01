# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Firebase/Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-keep class androidx.credentials.** { *; }

# Keep data model classes (CRITICAL for Firestore serialization)
-keep class com.example.fewstep.data.model.** { *; }
-keepclassmembers class com.example.fewstep.data.model.** { *; }

# AdMob Keep Rules
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Keep Lifecycle and ViewModel
-keep class androidx.lifecycle.** { *; }

# Kotlin Serialization / Property Names
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class ** {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.IgnoreExtraProperties *;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Suppress warnings
-dontwarn com.google.errorprone.annotations.**
-dontwarn sun.misc.Unsafe
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
