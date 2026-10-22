package com.student.wifihelper.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.student.wifihelper.UtarApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.net.wifi.STATE_CHANGE" || action == "android.net.conn.CONNECTIVITY_CHANGE") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repository = UtarApplication.instance.repository
                    val config = repository.getConfig()
                    // On Android 15+ (API 35+), direct foreground service start from BOOT_COMPLETED receiver is restricted
                    if (config.autoLoginEnabled && android.os.Build.VERSION.SDK_INT < 35) {
                        UtarWifiService.startService(context)
                    }
                } catch (e: Exception) {}
            }
        }
    }
}
