package com.student.wifihelper

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
            confessionRepository = com.student.wifihelper.data.repository.FirebaseConfessionRepository()
        } catch (e: Exception) {
            confessionRepository = com.student.wifihelper.data.repository.LocalConfessionRepository(this)
        }

        createNotificationChannel()

        // Auto-resume background service if auto-connect is active
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val config = repository.getConfig()
                if (config.autoLoginEnabled) {
                    com.student.wifihelper.data.service.UtarWifiService.startService(this@UtarApplication)
                }
            } catch (e: Exception) {}
        }

        // Initialize Google Mobile Ads (AdMob)
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
        } catch (e: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "U不停歇WIFI助手",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors WiFi connection and automatically logs into campus captive portal."
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "wifi_auto_login_channel"
        lateinit var instance: UtarApplication
            private set
    }
}
