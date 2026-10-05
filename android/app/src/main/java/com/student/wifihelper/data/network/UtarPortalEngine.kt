package com.student.wifihelper.data.network

import com.student.wifihelper.data.crypto.Rc4Cipher
import com.student.wifihelper.data.model.LoginResult
import com.student.wifihelper.data.model.PortalDetectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UtarPortalEngine {

    private val probeUrls = listOf(
        "http://connectivitycheck.gstatic.com/generate_204",
        "http://www.msftconnecttest.com/redirect",
        "http://captive.apple.com/hotspot-detect.html"
    )

    private fun getProbeClient(network: android.net.Network? = null): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .apply {
                if (network != null) {
                    socketFactory(network.socketFactory)
                    dns(object : Dns {
                        override fun lookup(hostname: String) = network.getAllByName(hostname).toList()
                    })
                }
            }
            .build()
    }

    private fun getRedirectClient(network: android.net.Network? = null): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .apply {
                if (network != null) {
                    socketFactory(network.socketFactory)
                    dns(object : Dns {
                        override fun lookup(hostname: String) = network.getAllByName(hostname).toList()
                    })
                }
            }
            .build()
    }

    private fun getLoginClient(network: android.net.Network? = null): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .apply {
                if (network != null) {
                    socketFactory(network.socketFactory)
                    dns(object : Dns {
                        override fun lookup(hostname: String) = network.getAllByName(hostname).toList()
                    })
                }
            }
            .build()
    }

    suspend fun checkInternetAccess(network: android.net.Network? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://connectivitycheck.gstatic.com/generate_204")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()
                .build()

            getProbeClient(network).newCall(request).execute().use { response: Response ->
                response.code == 204
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun forceSystemValidation(network: android.net.Network?) {
        if (network == null) return
        withContext(Dispatchers.IO) {
            repeat(3) {
                try {
                    checkInternetAccess(network)
                } catch (e: Exception) {}
                kotlinx.coroutines.delay(500)
            }
        }
    }

    suspend fun detectPortalUrl(network: android.net.Network? = null): PortalDetectionResult = withContext(Dispatchers.IO) {
        if (checkInternetAccess(network)) {
            return@withContext PortalDetectionResult(
                detected = false,
                isInternetActive = true,
                message = "Internet access is already active (HTTP 204)."
            )
        }

        for (probeUrl in probeUrls) {
            try {
                val request = Request.Builder()
                    .url(probeUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()
                    .build()

                getProbeClient(network).newCall(request).execute().use { response: Response ->
                    val locationHeader = response.header("Location") ?: response.header("location")
                    if (locationHeader != null) {
                        val fullUrl = try {
                            java.net.URL(response.request.url.toUrl(), locationHeader).toString()
                        } catch (e: Exception) {
                            if (locationHeader.startsWith("http")) locationHeader else "http://$locationHeader"
                        }
                        
                        val body = try {
                            val bodyReq = Request.Builder().url(fullUrl).get().build()
                            getRedirectClient(network).newCall(bodyReq).execute().use { it.body?.string() }
                        } catch (e: Exception) { null }

                        return@withContext PortalDetectionResult(
                            detected = true,
                            portalUrl = fullUrl,
                            message = "Captive portal redirect detected: $fullUrl",
                            htmlBody = body
                        )
                    }
                }
            } catch (e: Exception) {
            }
        }

        for (probeUrl in probeUrls) {
            try {
                val request = Request.Builder()
                    .url(probeUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get()
                    .build()

                getRedirectClient(network).newCall(request).execute().use { response: Response ->
                    val finalUrl = response.request.url.toString()
                    val body = response.body?.string() ?: ""
                    
                    if (finalUrl != probeUrl || body.contains("login") || body.contains("user")) {
                        return@withContext PortalDetectionResult(
                            detected = true,
                            portalUrl = finalUrl,
                            message = "Captive portal landing page detected: $finalUrl",
                            htmlBody = body
                        )
                    }
                    
                    parsePortalUrlFromBody(body, response.request.url.toUrl())?.let {
                        return@withContext PortalDetectionResult(
                            detected = true,
                            portalUrl = it,
                            message = "Captive portal detected in body: $it",
                            htmlBody = body
                        )
                    }
                }
            } catch (e: Exception) {
            }
        }

        PortalDetectionResult(
            detected = false,
            isInternetActive = false,
            message = "Unable to detect captive portal login page. Ensure you are connected to Campus WiFi."
        )
    }

    private fun parsePortalUrlFromBody(body: String, baseUrl: java.net.URL): String? {
        if (body.isEmpty()) return null
        val patterns = listOf(
            Regex("""window\.location(?:\.href)?\s*=\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE),
            Regex("""<meta[^>]*http-equiv=['"]?refresh['"]?[^>]*content=['"]?[^'"]*url=([^'">]+)['"]?""", RegexOption.IGNORE_CASE),
            Regex("""<a[^>]*href=['"]([^'"]*?/ac_portal/[^'"]*)['"]""", RegexOption.IGNORE_CASE),
            Regex("""<form[^>]*action=['"]([^'"]*?/ac_portal/[^'"]*)['"]""", RegexOption.IGNORE_CASE),
            Regex(""""location"\s*:\s*"([^"]*?/ac_portal/[^"]*)"""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                val location = match.groupValues[1]
                if (location.isNotEmpty()) {
                    return if (location.startsWith("http")) location else java.net.URL(baseUrl, location).toString()
                }
            }
        }
        return null
    }

    suspend fun login(portalUrl: String, username: String, password: String, network: android.net.Network? = null): LoginResult = withContext(Dispatchers.IO) {
        if (username.isBlank() || password.isBlank()) {
            return@withContext LoginResult.Failure("Username and password cannot be empty.")
        }

        val parsedUrl = portalUrl.toHttpUrlOrNull() ?: return@withContext LoginResult.Failure("Invalid portal URL")
        
        val portSuffix = if ((parsedUrl.scheme == "http" && parsedUrl.port == 80) || (parsedUrl.scheme == "https" && parsedUrl.port == 443)) {
            ""
        } else {
            ":${parsedUrl.port}"
        }
        val baseUrl = "${parsedUrl.scheme}://${parsedUrl.host}$portSuffix"

        val querySuffix = if (!parsedUrl.query.isNullOrBlank()) "?${parsedUrl.query}" else ""
        val endpoints = listOf(
            portalUrl,
            "$baseUrl/ac_portal/login.php$querySuffix",
            "$baseUrl/ac_portal/login.php",
            "$baseUrl/login.php",
            "$baseUrl/ac_portal/login.cgi"
        ).distinct()

        val rckey = System.currentTimeMillis().toString()
        val encryptedPwd = Rc4Cipher.encrypt(rckey, password)

        val formBody = FormBody.Builder()
            .add("opr", "pwdLogin")
            .add("userName", username)
            .add("pwd", encryptedPwd)
            .add("auth_tag", rckey)
            .add("rememberPwd", "0")
            .build()

        var lastError = ""
        for (loginEndpoint in endpoints) {
            val request = Request.Builder()
                .url(loginEndpoint)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", portalUrl)
                .post(formBody)
                .build()

            try {
                getLoginClient(network).newCall(request).execute().use { response: Response ->
                    val responseBodyStr = response.body?.string() ?: ""
                    
                    val isSuccess = try {
                        val json = JSONObject(responseBodyStr)
                        json.optBoolean("success", false) || responseBodyStr.contains("\"success\":true") || responseBodyStr.contains("\"success\": true")
                    } catch (e: Exception) {
                        responseBodyStr.contains("\"success\":true") || responseBodyStr.contains("\"success\": true")
                    }

                    if (isSuccess || checkInternetAccess(network)) {
                        return@withContext LoginResult.Success("Login successful!")
                    } else {
                        lastError = try {
                            JSONObject(responseBodyStr).optString("msg", "Rejected by portal")
                        } catch (e: Exception) { "Login failed at $loginEndpoint" }
                    }
                }
            } catch (e: Exception) {
                // If the request caused a network state change that opened internet access, verify it!
                if (checkInternetAccess(network)) {
                    return@withContext LoginResult.Success("Login successful!")
                }
                lastError = e.localizedMessage ?: "Connection error"
            }
        }

        return@withContext LoginResult.Failure(lastError)
    }
}
