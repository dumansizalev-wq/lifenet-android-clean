# ========================================
# LIFENET ProGuard Rules
# ========================================

# Keep all data classes
-keep class net.lifenet.core.data.** { *; }
-keep class net.lifenet.core.messaging.MessageEnvelope { *; }
-keep class net.lifenet.core.emergency.SOSAlert { *; }

# Keep Room database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }

# Keep Jetpack components
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }

# Keep NanoHTTPD (Captive Portal)
-keep class fi.iki.elonen.** { *; }

# Keep ZXing (QR codes)
-keep class com.google.zxing.** { *; }

# Keep OSMDroid (Maps)
-keep class org.osmdroid.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Keep BuildConfig
-keep class net.lifenet.core.BuildConfig { *; }

# Keep custom exceptions
-keep public class * extends java.lang.Exception

# Optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Attributes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Serialization
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
