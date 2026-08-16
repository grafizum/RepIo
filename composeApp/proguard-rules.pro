# Keep all resource identifiers to allow getIdentifier lookups
-keep class **.R$* {
    <fields>;
}

# Keep the getString bridge implementation
-keep class com.grafizum.repio.util.Resources_androidKt {
    public static <methods>;
}

# Keep the strings themselves from being obfuscated or removed
-keepclassmembers class **.R$string {
    public static <fields>;
}

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
