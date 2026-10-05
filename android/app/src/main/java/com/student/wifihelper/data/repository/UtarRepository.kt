package com.student.wifihelper.data.repository

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import com.student.wifihelper.data.db.ConfigDao
import com.student.wifihelper.data.db.LogDao
import com.student.wifihelper.data.db.PortalDao
import com.student.wifihelper.data.model.LogStatus
import com.student.wifihelper.data.model.LoginLog
import com.student.wifihelper.data.model.LoginResult
import com.student.wifihelper.data.model.NetworkState
import com.student.wifihelper.data.model.PortalEndpoint
import com.student.wifihelper.data.model.UserConfig
import com.student.wifihelper.data.network.UtarPortalEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class UtarRepository(
    private val context: Context,
    private val configDao: ConfigDao,
    private val logDao: LogDao,
    private val portalDao: PortalDao,
    private val portalEngine: UtarPortalEngine = UtarPortalEngine()
) {
    val configFlow: Flow<UserConfig?> = configDao.getConfigFlow()
    val logsFlow: Flow<List<LoginLog>> = logDao.getAllLogs()

    suspend fun getConfig(): UserConfig {
        return configDao.getConfig() ?: UserConfig().also {
            configDao.saveConfig(it)
        }
    }

    suspend fun saveConfig(config: UserConfig) {
        configDao.saveConfig(config)
        addLog(LogStatus.INFO, "Configuration updated. Target SSID: ${config.targetSsid}")
        if (config.autoLoginEnabled) {
            triggerWifiNudge(config.targetSsid)
        } else {
            removeWifiNudge(config.targetSsid)
        }
    }

    fun removeWifiNudge(ssid: String) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val suggestion = WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .build()
                wifiManager.removeNetworkSuggestions(listOf(suggestion))
                addLogSync(LogStatus.INFO, "Network suggestion removed for '$ssid'")
            } catch (e: Exception) {}
        }
    }

    fun triggerWifiNudge(ssid: String) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val builder = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setIsAppInteractionRequired(false)
                .setPriority(100)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setIsInitialAutojoinEnabled(true)
            }

            val suggestion = builder.build()
            try {
                wifiManager.removeNetworkSuggestions(listOf(suggestion))
                val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
                addLogSync(LogStatus.INFO, "Network suggestion added for '$ssid' (status: $status)")
            } catch (e: Exception) {
                addLogSync(LogStatus.WARNING, "Failed to add network suggestion: ${e.message}")
            }
            try {
                @Suppress("DEPRECATION")
                wifiManager.startScan()
            } catch (e: Exception) {}
        } else {
            // Android 9 and below: Legacy auto-connect
            try {
                @Suppress("DEPRECATION")
                if (!wifiManager.isWifiEnabled) {
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = true
                }
                @Suppress("DEPRECATION")
                val cleanSsid = ssid.replace("\"", "")
                @Suppress("DEPRECATION")
                val configured = try {
                    wifiManager.configuredNetworks ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                @Suppress("DEPRECATION")
                var netId = configured.find { it.SSID?.replace("\"", "") == cleanSsid }?.networkId ?: -1

                if (netId == -1) {
                    @Suppress("DEPRECATION")
                    val conf = android.net.wifi.WifiConfiguration().apply {
                        SSID = "\"$cleanSsid\""
                        @Suppress("DEPRECATION")
                        allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE)
                    }
                    @Suppress("DEPRECATION")
                    netId = wifiManager.addNetwork(conf)
                }

                if (netId != -1) {
                    @Suppress("DEPRECATION")
                    wifiManager.disconnect()
                    @Suppress("DEPRECATION")
                    wifiManager.enableNetwork(netId, true)
                    @Suppress("DEPRECATION")
                    wifiManager.reconnect()
                }
            } catch (e: Exception) {
                addLogSync(LogStatus.WARNING, "Legacy WiFi connect error: ${e.message}")
            }
        }
    }

    fun getCurrentWifiSsid(): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val wifiInfo: WifiInfo? = wifiManager.connectionInfo
            val ssid = wifiInfo?.ssid?.replace("\"", "")
            
            if (ssid != null && ssid != "<unknown ssid>" && ssid != "0x" && ssid.isNotBlank()) {
                ssid
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ NetworkCapabilities transportInfo fallback
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
                val info = caps?.transportInfo as? WifiInfo
                val s = info?.ssid?.replace("\"", "")
                if (s != null && s != "<unknown ssid>" && s != "0x" && s.isNotBlank()) {
                    s
                } else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun addLogSync(status: LogStatus, message: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            addLog(status, message)
        }
    }

    suspend fun addLog(status: LogStatus, message: String, ssid: String? = null, portalUrl: String? = null) {
        val log = LoginLog(
            timestamp = System.currentTimeMillis(),
            status = status,
            message = message,
            ssid = ssid,
            portalUrl = portalUrl
        )
        logDao.insertLog(log)
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }

    suspend fun ensureWifiEnabled(): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return false

        @Suppress("DEPRECATION")
        if (!wifiManager.isWifiEnabled) {
            addLog(LogStatus.INFO, "WiFi is turned OFF. Enabling WiFi adapter...")

            try {
                @Suppress("DEPRECATION")
                val success = wifiManager.setWifiEnabled(true)
                if (success) {
                    addLog(LogStatus.INFO, "WiFi enable command issued to system.")
                } else {
                    addLog(LogStatus.WARNING, "System restricted automatic WiFi toggle. Opening WiFi Settings...")
                    openWifiSettings()
                }
            } catch (e: Exception) {
                addLog(LogStatus.WARNING, "Failed to toggle WiFi: ${e.message}")
                openWifiSettings()
            }

            var attempts = 0
            @Suppress("DEPRECATION")
            while (!wifiManager.isWifiEnabled && attempts < 10) {
                delay(500)
                attempts++
            }

            @Suppress("DEPRECATION")
            if (!wifiManager.isWifiEnabled) {
                addLog(LogStatus.ERROR, "WiFi could not be turned on automatically. Opened WiFi Settings.")
                openWifiSettings()
                return false
            }

            addLog(LogStatus.SUCCESS, "WiFi turned ON! Waiting for device to scan & connect to Campus network...")

            var connAttempts = 0
            while (connAttempts < 10) {
                delay(1000)
                connAttempts++

                val ssid = getCurrentWifiSsid()
                if (ssid != null) {
                    addLog(LogStatus.SUCCESS, "Connected to WiFi network: $ssid")
                    break
                }

                val portalCheck = portalEngine.detectPortalUrl()
                if (portalCheck.detected) {
                    addLog(LogStatus.SUCCESS, "Captive portal detected: ${portalCheck.portalUrl}")
                    break
                }
            }

            delay(1000)
            return true
        }

        return true
    }

    private fun openWifiSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
        }
    }

    fun getActiveWifiNetwork(): Network? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val active = connectivityManager.activeNetwork
            if (active != null) {
                val caps = connectivityManager.getNetworkCapabilities(active)
                if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    return active
                }
            }
        }

        for (network in connectivityManager.allNetworks) {
            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                return network
            }
        }
        
        return null
    }

    suspend fun evaluateNetworkStatus(currentSsid: String?): NetworkState {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val isWifiEnabled = try {
            @Suppress("DEPRECATION")
            wifiManager?.isWifiEnabled == true
        } catch (e: Exception) { false }

        if (!isWifiEnabled) {
            return NetworkState.WifiDisabled
        }

        val wifiNetwork = getActiveWifiNetwork()
        if (wifiNetwork == null) {
            return NetworkState.NotConnected
        }

        val actualSsid = currentSsid ?: getCurrentWifiSsid()

        if (portalEngine.checkInternetAccess(wifiNetwork)) {
            return NetworkState.InternetActive
        }

        val config = getConfig()
        val targetSsid = config.targetSsid

        val portalResult = portalEngine.detectPortalUrl(wifiNetwork)
        return if (portalResult.detected && portalResult.portalUrl != null) {
            NetworkState.CaptivePortalDetected(
                portalUrl = portalResult.portalUrl,
                ssid = actualSsid ?: targetSsid
            )
        } else if (actualSsid != null && !isSsidMatch(actualSsid, targetSsid)) {
            NetworkState.ConnectedNotTarget(
                currentSsid = actualSsid,
                targetSsid = targetSsid
            )
        } else if (portalResult.isInternetActive) {
            NetworkState.InternetActive
        } else {
            NetworkState.WifiConnected(actualSsid)
        }
    }

    suspend fun performLogin(providedPortalUrl: String? = null, currentSsid: String? = null): LoginResult = coroutineScope {
        ensureWifiEnabled()

        val config = getConfig()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val targetNetwork = getActiveWifiNetwork()

        if (targetNetwork == null) {
            return@coroutineScope LoginResult.Failure("Campus WiFi connection not found. Please connect to 'utarwifi' first.")
        }

        if (config.username.isBlank() || config.password.isBlank()) {
            return@coroutineScope LoginResult.Failure("Credentials not set.")
        }

        addLog(LogStatus.INFO, "Login initiated across campus endpoints...", currentSsid)

        try {
            // Bind process to WiFi network cleanly without triggering system UI loops
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.bindProcessToNetwork(targetNetwork)
            } else {
                @Suppress("DEPRECATION")
                ConnectivityManager.setProcessDefaultNetwork(targetNetwork)
            }

            // High-frequency gateway IP resolution
            val gateway = getGatewayIp()
            val targetUrls = mutableSetOf<String>()
            
            if (providedPortalUrl != null) targetUrls.add(providedPortalUrl)

            // Active probe to discover active captive portal URL directly on targetNetwork
            val detected = portalEngine.detectPortalUrl(targetNetwork)
            if (detected.detected && detected.portalUrl != null) {
                targetUrls.add(detected.portalUrl)
            }

            if (gateway != null) {
                targetUrls.add("http://$gateway")
                targetUrls.add("http://$gateway/ac_portal/default/pc/login.html")
                targetUrls.add("http://$gateway/ac_portal/login.php")
            }

            // Known faculty gateway endpoints
            targetUrls.addAll(listOf("http://10.10.0.1", "http://10.0.0.1", "http://192.168.1.1", "http://203.0.113.1", "http://203.0.144.1"))
            
            // Self-learned endpoints
            val learnedEndpoints = portalDao.getAllEndpoints()
            learnedEndpoints.forEach { targetUrls.add(it.url) }

            var workingUrl: String? = null

            val finalResult: LoginResult = coroutineScope {
                val resultChannel = Channel<Pair<String, LoginResult>>(targetUrls.size)
                
                val jobs = targetUrls.map { url ->
                    launch {
                        val loginRes = withTimeoutOrNull(3000) {
                            portalEngine.login(url, config.username, config.password, targetNetwork)
                        }
                        if (loginRes != null) {
                            resultChannel.send(url to loginRes)
                        } else {
                            resultChannel.send(url to LoginResult.Failure("Connection timed out"))
                        }
                    }
                }

                var firstSuccess: LoginResult? = null
                var lastFailureMsg = "Portal authentication failed. Please verify credentials."
                var completedCount = 0

                while (completedCount < targetUrls.size) {
                    val (url, res) = resultChannel.receive()
                    completedCount++
                    if (res is LoginResult.Success) {
                        firstSuccess = res
                        workingUrl = url
                        jobs.forEach { it.cancel() }
                        break
                    } else if (res is LoginResult.Failure && res.errorMessage.isNotBlank() && !res.errorMessage.contains("timed out")) {
                        lastFailureMsg = res.errorMessage
                    }
                }

                firstSuccess ?: LoginResult.Failure(lastFailureMsg)
            }

            if (finalResult is LoginResult.Success) {
                addLog(LogStatus.SUCCESS, "Campus WiFi login successful!", currentSsid)
                
                workingUrl?.let { url ->
                    portalDao.insertEndpoint(PortalEndpoint(url = url, lastUsedTime = System.currentTimeMillis()))
                }
                
                // Single clean notification to OS after login success
                delay(300)
                try {
                    connectivityManager.reportNetworkConnectivity(targetNetwork, true)
                } catch (e: Exception) {}

                configDao.saveConfig(config.copy(
                    lastLoginTime = System.currentTimeMillis(),
                    lastLoginSuccess = true,
                    lastStatusMessage = "Login Success"
                ))
            } else {
                addLog(LogStatus.ERROR, "Login attempt failed. Please check credentials or server.", currentSsid)
            }

            finalResult
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.bindProcessToNetwork(null)
            } else {
                @Suppress("DEPRECATION")
                ConnectivityManager.setProcessDefaultNetwork(null)
            }
        }
    }

    private fun isSsidMatch(current: String, target: String): Boolean {
        if (current.equals(target, ignoreCase = true)) return true
        val cleanCurrent = current.replace("\"", "").lowercase()
        val cleanTarget = target.replace("\"", "").lowercase()
        if (cleanCurrent == cleanTarget) return true
        if (cleanCurrent.startsWith(cleanTarget)) return true
        if (cleanCurrent.contains("utar") && cleanTarget.contains("utar")) return true
        return false
    }

    private fun getGatewayIp(): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val dhcp = wifiManager.dhcpInfo
            val gateway = dhcp?.gateway ?: 0
            if (gateway == 0) return null
            
            String.format(
                "%d.%d.%d.%d",
                gateway and 0xff,
                gateway shr 8 and 0xff,
                gateway shr 16 and 0xff,
                gateway shr 24 and 0xff
            )
        } catch (e: Exception) {
            null
        }
    }
}
