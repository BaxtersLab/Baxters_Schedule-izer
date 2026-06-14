# ProGuard / R8 rules for release builds.
# Release now runs with minifyEnabled + shrinkResources, so anything reached by
# JNI name lookup or reflection must be kept explicitly.

# --- RemoteDexter JNI bridge ---------------------------------------------------
# The native library resolves these methods by their fully-qualified name at
# runtime (System.loadLibrary + RegisterNatives). Renaming or removing the class
# or its native methods breaks the tunnel.
-keep class com.baxter.schedulaizer.transfer.RemoteDexterNative { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- BuildConfig accessed via reflection ---------------------------------------
# SchedulaizerApp.enableNativeLibraryIfAllowed() loads BuildConfig and reads the
# ENABLE_JNI field by name via reflection, so the field must survive shrinking.
-keep class com.baxter.schedulaizer.BuildConfig { *; }

# --- Room entities & type converters -------------------------------------------
# Room generates code against these; keep the entity/converter shapes to be safe.
-keep class com.baxter.schedulaizer.data.db.entity.** { *; }
-keep class com.baxter.schedulaizer.data.db.AppTypeConverters { *; }

# --- Kotlin / coroutines -------------------------------------------------------
# Most rules ship as consumer rules, but keep coroutine internals that are
# occasionally accessed reflectively.
-keepclassmembernames class kotlinx.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
