package com.student.wifihelper

import android.app.Application
import com.student.wifihelper.data.db.AppDatabase
import com.student.wifihelper.data.repository.UtarRepository
import kotlinx.coroutines.launch

class UtarApplication : Application() {

    lateinit var repository: UtarRepository
        private set

    lateinit var confessionRepository: com.student.wifihelper.data.repository.ConfessionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val database = AppDatabase.getDatabase(this)
        repository = UtarRepository(
            context = this,
            configDao = database.configDao(),
            logDao = database.logDao(),
            portalDao = database.portalDao()
        )

        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()) {
                confessionRepository = com.student.wifihelper.data.repository.FirebaseConfessionRepository()
            } else {
                confessionRepository = com.student.wifihelper.data.repository.LocalConfessionRepository(this)
            }
        } catch (e: Exception) {
            confessionRepository = com.student.wifihelper.data.repository.LocalConfessionRepository(this)
        }

        // Initialize Google Mobile Ads (AdMob)
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
        } catch (e: Exception) {}
    }

    companion object {
        const val CHANNEL_ID = "wifi_auto_login_channel"
        lateinit var instance: UtarApplication
            private set
    }
}
