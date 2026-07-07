-keep class android.** { *; }
-keep class de.robv.android.xposed.** { *; }

# Workaround to bypass verification of in-memory built class xposed.dummy.XResourcesSuperClass
-keepclassmembers class org.ahmoze.vector.legacy.LegacyDelegateImpl$ResourceProxy { *; }
