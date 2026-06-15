# Add project specific ProGuard rules here.

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.porrawc2026.app.data.remote.** { *; }

# Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.xmlbeans.**
-dontwarn javax.xml.stream.**
-dontwarn aQute.bnd.**
-dontwarn org.apache.logging.**
-dontwarn javax.servlet.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn java.security.**
-dontwarn javax.crypto.**
-dontwarn javax.naming.**
-dontwarn org.w3c.dom.**
-dontwarn org.xml.sax.**

# PDFBox
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.apache.fontbox.**

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Hilt
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep data classes for Gson serialization
-keepclassmembers class com.porrawc2026.app.data.remote.** { *; }
-keepclassmembers class com.porrawc2026.app.data.local.entity.** { *; }
