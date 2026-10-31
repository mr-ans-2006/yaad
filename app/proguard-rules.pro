# ONNX Runtime calls into Java from native code, so keep its classes as they are.
-keep class ai.onnxruntime.** { *; }
# ML Kit ships its own rules; nothing extra needed.
-dontwarn org.slf4j.**
