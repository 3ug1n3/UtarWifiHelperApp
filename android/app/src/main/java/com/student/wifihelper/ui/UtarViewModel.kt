package com.student.wifihelper.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student.wifihelper.UtarApplication
import com.student.wifihelper.data.model.LogStatus
import com.student.wifihelper.data.model.LoginLog
import com.student.wifihelper.data.model.NetworkState
import com.student.wifihelper.data.model.UserConfig
import com.student.wifihelper.data.repository.UtarRepository
import com.student.wifihelper.data.service.UtarWifiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UtarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UtarRepository = (application as UtarApplication).repository

    val configState: StateFlow<UserConfig?> = repository.configFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val logsState: StateFlow<List<LoginLog>> = repository.logsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isServiceRunning: StateFlow<Boolean> = UtarWifiService.isServiceRunning

    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Checking)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _isLoginLoading = MutableStateFlow(false)
    val isLoginLoading: StateFlow<Boolean> = _isLoginLoading.asStateFlow()

    private val _onboardingStep = MutableStateFlow<Int?>(null)
    val onboardingStep: StateFlow<Int?> = _onboardingStep.asStateFlow()

    // Draft states for Credentials screen
    val draftUsername = MutableStateFlow("")
    val draftPassword = MutableStateFlow("")

    private val _wifiUsageGb = MutableStateFlow(0.0)
    val wifiUsageGb: StateFlow<Double> = _wifiUsageGb.asStateFlow()

    private val _shouldAnimateSave = MutableStateFlow(false)
    val shouldAnimateSave: StateFlow<Boolean> = _shouldAnimateSave.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    init {
        checkNetworkStatus()
        checkOnboardingStatus()
        startDataUsageTracking()
    }

    private fun startDataUsageTracking() {
        viewModelScope.launch {
            val statsManager = getApplication<Application>().getSystemService(android.content.Context.NETWORK_STATS_SERVICE) as? android.app.usage.NetworkStatsManager
            while (true) {
                try {
                    // Calculate start of current month
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    val startTime = calendar.timeInMillis
                    val endTime = System.currentTimeMillis()

                    if (statsManager != null) {
                        // Query WiFi usage for the current month
                        val bucket = statsManager.querySummaryForDevice(
                            android.net.NetworkCapabilities.TRANSPORT_WIFI,
                            "",
                            startTime,
                            endTime
                        )
                        val totalBytes = bucket.rxBytes + bucket.txBytes
                        _wifiUsageGb.value = totalBytes / (1024.0 * 1024.0 * 1024.0)
                    } else {
                        // Fallback to TrafficStats (since boot, WiFi only)
                        val wifiRx = (android.net.TrafficStats.getTotalRxBytes() - android.net.TrafficStats.getMobileRxBytes()).coerceAtLeast(0L)
                        val wifiTx = (android.net.TrafficStats.getTotalTxBytes() - android.net.TrafficStats.getMobileTxBytes()).coerceAtLeast(0L)
                        val totalBytes = wifiRx + wifiTx
                        _wifiUsageGb.value = totalBytes / (1024.0 * 1024.0 * 1024.0)
                    }
                } catch (e: Exception) {
                    // Fallback to TrafficStats (since boot, WiFi only) if NetworkStatsManager requires missing usage permission
                    val wifiRx = (android.net.TrafficStats.getTotalRxBytes() - android.net.TrafficStats.getMobileRxBytes()).coerceAtLeast(0L)
                    val wifiTx = (android.net.TrafficStats.getTotalTxBytes() - android.net.TrafficStats.getMobileTxBytes()).coerceAtLeast(0L)
                    val totalBytes = wifiRx + wifiTx
                    _wifiUsageGb.value = totalBytes / (1024.0 * 1024.0 * 1024.0)
                }
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    fun triggerSaveAnimation() {
        viewModelScope.launch {
            _shouldAnimateSave.value = true
            kotlinx.coroutines.delay(1000)
            _shouldAnimateSave.value = false
        }
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            val config = repository.getConfig()
            if (!config.isOnboardingCompleted) {
                _onboardingStep.value = 1
            }
            // Pre-fill drafts
            draftUsername.value = config.username
            draftPassword.value = config.password
        }
    }

    fun nextOnboardingStep() {
        val current = _onboardingStep.value ?: return
        if (current < 7) {
            _onboardingStep.value = current + 1
        } else {
            completeOnboarding()
        }
    }

    fun setOnboardingStep(step: Int?) {
        _onboardingStep.value = step
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            val current = configState.value ?: UserConfig()
            repository.saveConfig(current.copy(isOnboardingCompleted = true))
            _onboardingStep.value = null
        }
    }

    fun checkNetworkStatus(currentSsid: String? = null) {
        viewModelScope.launch {
            _networkState.value = NetworkState.Checking
            val state = repository.evaluateNetworkStatus(currentSsid)
            _networkState.value = state
        }
    }

    fun performManualLogin(currentSsid: String? = null) {
        viewModelScope.launch {
            _isLoginLoading.value = true
            repository.addLog(LogStatus.INFO, "Manual login triggered by user.", currentSsid)
            val result = repository.performLogin(currentSsid = currentSsid)
            _isLoginLoading.value = false
            checkNetworkStatus(currentSsid)

            val msg = when (result) {
                is com.student.wifihelper.data.model.LoginResult.Success -> result.message
                is com.student.wifihelper.data.model.LoginResult.Failure -> result.errorMessage
            }
            _toastMessage.value = msg
        }
    }

    fun saveConfig(
        username: String,
        password: String,
        targetSsid: String,
        autoLoginEnabled: Boolean,
        pollingIntervalSeconds: Int,
        biometricLockEnabled: Boolean = false
    ) {
        viewModelScope.launch {
            val current = configState.value ?: UserConfig()
            val updated = current.copy(
                username = username.trim(),
                password = password.trim(),
                targetSsid = targetSsid.trim().ifBlank { "utarwifi" },
                autoLoginEnabled = autoLoginEnabled,
                pollingIntervalSeconds = pollingIntervalSeconds.coerceAtLeast(10),
                biometricLockEnabled = biometricLockEnabled
            )
            repository.saveConfig(updated)

            if (isServiceRunning.value) {
                UtarWifiService.startService(getApplication())
            }
        }
    }

    fun toggleService(enable: Boolean) {
        viewModelScope.launch {
            val current = configState.value ?: UserConfig()
            val updated = current.copy(autoLoginEnabled = enable)
            repository.saveConfig(updated)

            if (enable) {
                repository.triggerWifiNudge(updated.targetSsid)
                UtarWifiService.startService(getApplication())
            } else {
                repository.removeWifiNudge(updated.targetSsid)
                UtarWifiService.stopService(getApplication())
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }
}
