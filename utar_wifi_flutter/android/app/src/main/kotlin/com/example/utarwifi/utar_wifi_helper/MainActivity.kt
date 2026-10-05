package com.example.utarwifi.utar_wifi_helper

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.example.utarwifi/engine"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "startPulse" -> {
                    result.success(null)
                }
                "stopPulse" -> {
                    result.success(null)
                }
                "reportConnected" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        val network = cm.activeNetwork
                        if (network != null) {
                            // Try multiple ways to suppress the notification
                            cm.reportNetworkConnectivity(network, true)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                // Hint to the system that the captive portal is handled
                                cm.getNetworkCapabilities(network)
                            }
                        }
                    }
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}
