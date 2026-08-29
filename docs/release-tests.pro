# The separate test APK calls shared libraries outside the app's call graph.
# Its runner cannot resolve those entry points after the app removes/renames them.
# These rules are loaded ONLY by the QA init script, never by production builds.
# Do not keep Gson or app converters here: tests must exercise production R8 rules.
-keep class androidx.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
