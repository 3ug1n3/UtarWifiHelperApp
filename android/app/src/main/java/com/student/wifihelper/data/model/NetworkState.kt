package com.student.wifihelper.data.model

sealed class LoginResult {
    data class Success(val message: String, val redirectUrl: String? = null) : LoginResult()
    data class Failure(val errorMessage: String) : LoginResult()
}

sealed class NetworkState {
    object Checking : NetworkState()
    object InternetActive : NetworkState()
    data class CaptivePortalDetected(val portalUrl: String, val ssid: String) : NetworkState()
    data class ConnectedNotTarget(val currentSsid: String, val targetSsid: String) : NetworkState()
    data class WifiConnected(val ssid: String?) : NetworkState()
    object WifiDisabled : NetworkState()
    object NotConnected : NetworkState()
    object Disconnected : NetworkState()
    data class Error(val message: String) : NetworkState()
}

data class PortalDetectionResult(
    val detected: Boolean,
    val portalUrl: String? = null,
    val isInternetActive: Boolean = false,
    val message: String = "",
    val htmlBody: String? = null
)
