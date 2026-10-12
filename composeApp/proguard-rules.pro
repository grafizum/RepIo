# ---------------------------------------------------------------- getString / getQuantityString
#
# util/Resources.kt resolves strings by NAME through Resources.getIdentifier, which reflects over
# the merged R class at runtime. R8 cannot see that reachability, so the string and plurals tables
# must be kept verbatim - without these rules the release build resolves every getString("key")
# to 0 and falls back to printing the raw key, which looks like a translation bug and is not one.
#
# `**.R$string` and `**.R$plurals` are named rather than `**.R$*`: keeping every R inner class
# would also pin R$drawable, R$style and the rest, defeating the resource shrinker that this
# build turns on (isShrinkResources = true) and costing download size for no benefit.
-keepclassmembers class **.R$string { public static <fields>; }
-keepclassmembers class **.R$plurals { public static <fields>; }

# ---------------------------------------------------------------- Compose / Firebase
#
# Compose and Firebase ship their own consumer rules; nothing extra is required here. If a release
# build ever crashes with a missing-method NoSuchMethodError from a library class, add the keep
# rule for THAT library next to its version comment rather than widening to -keep class **.

# ---------------------------------------------------------------- crash reports
#
# Uncommented deliberately: a Play release is the build whose stack traces matter most, and a
# release with obfuscated, line-number-less traces cannot be read against source. R8 keeps the
# file name and line table for every method.
-keepattributes SourceFile,LineNumberTable

# Rename the source file attribute so the readable file name still ships while the on-disk path
# stays abstracted.
-renamesourcefileattribute SourceFile
