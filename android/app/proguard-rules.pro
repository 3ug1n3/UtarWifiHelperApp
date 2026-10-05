# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\HUAWEI\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room Database Keep Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.student.wifihelper.data.model.** { *; }
-keep class com.student.wifihelper.data.db.** { *; }

# OkHttp & Coroutines Keep Rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class kotlinx.coroutines.** { *; }
