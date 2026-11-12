# ====================================================================
# Proguard / R8 Keep Rules for UtarWifiHelper
# ====================================================================

# 1. Keep All App Data Models, Repositories, ViewModels, and Entities
-keep class com.student.wifihelper.data.model.** { *; }
-keepclassmembers class com.student.wifihelper.data.model.** { *; }
-keep class com.student.wifihelper.data.db.** { *; }
-keepclassmembers class com.student.wifihelper.data.db.** { *; }
-keep class com.student.wifihelper.data.repository.** { *; }
-keep class com.student.wifihelper.ui.** { *; }

# 2. Firebase Cloud Firestore & Storage Keep Rules
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** { *; }
-keep class com.google.firestore.** { *; }
-keepclassmembers class com.google.firestore.** { *; }
-dontwarn com.google.firebase.**

# 3. Google Mobile Ads (AdMob) & Play Services Keep Rules
-keep class com.google.android.gms.ads.** { *; }
-keepclassmembers class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.**

# 4. Room Database Keep Rules
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Dao { *; }
-dontwarn androidx.room.paging.**

# 5. OkHttp & Okio Keep Rules
-keep class okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# 6. Kotlin Coroutines Keep Rules
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 7. Coil Image Loading Keep Rules
-keep class coil.** { *; }
-dontwarn coil.**
