# ML Kit Barcode Scanning
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-dontwarn com.google.mlkit.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin metadata for reflection
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }
