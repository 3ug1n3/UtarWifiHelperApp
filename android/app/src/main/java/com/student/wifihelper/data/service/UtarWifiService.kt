package com.student.wifihelper.data.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.student.wifihelper.MainActivity
import com.student.wifihelper.UtarApplication
import com.student.wifihelper.data.model.LogStatus
import com.student.wifihelper.data.model.LoginResult
import com.student.wifihelper.data.model.NetworkState
import com.student.wifihelper.data.repository.UtarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UtarWifiService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var repository: UtarRepository
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var pollingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = UtarApplication.instance.repository
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                startForegroundServiceWithNotification("Monitoring Campus WiFi network...")
                registerNetworkCallback()
                startPolling()
                _isServiceRunning.value = true
            }
            ACTION_STOP -> {
                stopPolling()
                unregisterNetworkCallback()
                _isServiceRunning.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TRIGGER_CHECK -> {
                serviceScope.launch {
                    performCheckAndAutoLogin("Manual trigger from notification")
                }
            }
        }

        return START_STICKY
    }

    private fun startForegroundServiceWithNotification(statusText: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val checkIntent = Intent(this, UtarWifiService::class.java).apply { action = ACTION_TRIGGER_CHECK }
        val checkPendingIntent = PendingIntent.getService(
            this,
            1,
            checkIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, UtarApplication.CHANNEL_ID)
            .setContentTitle("U不停歇WIFI助手")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(0, "Check Now", checkPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, UtarApplication.CHANNEL_ID)
            .setContentTitle("U不停歇WIFI助手")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                serviceScope.launch {
                    repository.addLog(LogStatus.INFO, "WiFi network detected available.")
                    performCheckAndAutoLogin("WiFi connected")
                }
            }

            override fun onLost(network: Network) {
                serviceScope.launch {
                    repository.addLog(LogStatus.WARNING, "WiFi network lost.")
                    updateNotification("WiFi Disconnected")
                }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
        }
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
            }
        }
        networkCallback = null
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                val config = repository.getConfig()
                val interval = (config.pollingIntervalSeconds.coerceAtLeast(10)) * 1000L
                delay(interval)

                if (config.autoLoginEnabled) {
                    performCheckAndAutoLogin("Periodic background check")
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun performCheckAndAutoLogin(triggerReason: String) {
        val config = repository.getConfig()
        val currentSsid = repository.getCurrentWifiSsid()
        
        if (currentSsid == null && config.autoLoginEnabled) {
            repository.triggerWifiNudge(config.targetSsid)
        }

        val networkState = repository.evaluateNetworkStatus(currentSsid)

        when (networkState) {
            is NetworkState.InternetActive -> {
                updateNotification("Active - Connected to Internet")
            }
            is NetworkState.CaptivePortalDetected -> {
                if (config.autoLoginEnabled) {
                    updateNotification("Logging in to Campus WiFi...")
                    val result = repository.performLogin(
                        providedPortalUrl = networkState.portalUrl,
                        currentSsid = currentSsid
                    )
                    when (result) {
                        is LoginResult.Success -> updateNotification("Logged in to Campus WiFi")
                        is LoginResult.Failure -> updateNotification("Login Failed: ${result.errorMessage}")
                    }
                } else {
                    updateNotification("⚠️ Campus Wi-Fi Connected — Tap 'Login' in app to connect.")
                }
            }
            is NetworkState.ConnectedNotTarget -> {
                val current = networkState.currentSsid.lowercase()
                if ((current.contains("utar") || current.contains("campus")) && config.autoLoginEnabled) {
                    updateNotification("Campus WiFi detected - Logging in...")
                    val result = repository.performLogin(currentSsid = networkState.currentSsid)
                    if (result is LoginResult.Success) updateNotification("Logged in to Campus WiFi")
                } else {
                    updateNotification("Connected to ${networkState.currentSsid} (Target: ${networkState.targetSsid})")
                }
            }
            is NetworkState.WifiConnected -> {
                val ssid = networkState.ssid ?: ""
                if ((ssid.isEmpty() || ssid.contains("utarwifi", ignoreCase = true) || ssid.contains("campus", ignoreCase = true)) && config.autoLoginEnabled) {
                    updateNotification("Campus WiFi detected - Logging in...")
                    val result = repository.performLogin(currentSsid = ssid)
                    if (result is LoginResult.Success) updateNotification("Logged in to Campus WiFi")
                } else {
                    updateNotification("WiFi Connected — Tap 'Login' in app to connect.")
                }
            }
            is NetworkState.Disconnected, is NetworkState.WifiDisabled, is NetworkState.NotConnected -> {
                updateNotification("Waiting for WiFi connection...")
            }
            else -> {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPolling()
        unregisterNetworkCallback()
        _isServiceRunning.value = false
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_UTAR_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_UTAR_SERVICE"
        const val ACTION_TRIGGER_CHECK = "ACTION_TRIGGER_UTAR_CHECK"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun startService(context: Context) {
            try {
                val intent = Intent(context, UtarWifiService::class.java).apply { action = ACTION_START }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Safely handle ForegroundServiceStartNotAllowedException on Android 15+
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, UtarWifiService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }

        fun triggerCheck(context: Context) {
            val intent = Intent(context, UtarWifiService::class.java).apply { action = ACTION_TRIGGER_CHECK }
            context.startService(intent)
        }
    }
}
