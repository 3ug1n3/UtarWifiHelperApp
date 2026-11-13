package com.student.wifihelper

import android.app.Application
import com.student.wifihelper.data.db.AppDatabase
import com.student.wifihelper.data.repository.UtarRepository
import kotlinx.coroutines.launch

class UtarApplication : Application() {

    lateinit var repository: UtarRepository
        private set

    var confessionRepository: com.student.wifihelper.data.repository.ConfessionRepository = com.student.wifihelper.data.repository.LocalConfessionRepository(this)
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Global Exception Safeguard to keep app process alive and stable
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("UtarApplication", "Safely caught background exception on thread ${thread.name}: ${throwable.message}", throwable)
        }

        val database = AppDatabase.getDatabase(this)
        repository = UtarRepository(
            context = this,
            configDao = database.configDao(),
            logDao = database.logDao(),
            portalDao = database.portalDao()
        )

        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
            confessionRepository = com.student.wifihelper.data.repository.FirebaseConfessionRepository()
        } catch (e: Throwable) {
            confessionRepository = com.student.wifihelper.data.repository.LocalConfessionRepository(this)
        }

        // Initialize Google Mobile Ads (AdMob)
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
        } catch (e: Throwable) {}
    }

    companion object {
        const val CHANNEL_ID = "wifi_auto_login_channel"
        lateinit var instance: UtarApplication
            private set
    }
}
