# Retrofit (interface generics must survive R8)
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Room entities / converters
-keep class com.saveflow.downloader.data.database.** { *; }
