package com.student.wifihelper.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.isNullOrEmpty()) return
        // Auto-connect on boot is disabled per user request until gamified points features are added
    }
}
