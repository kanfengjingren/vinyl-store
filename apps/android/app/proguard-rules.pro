# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.vinylstore.app.data.model.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Socket.IO
-dontwarn io.socket.**
