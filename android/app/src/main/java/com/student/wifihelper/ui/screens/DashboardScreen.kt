package com.student.wifihelper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.student.wifihelper.ui.games.GameCenterScreen
import androidx.compose.material3.Text
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.data.model.NetworkState
import com.student.wifihelper.data.model.UserConfig
import com.student.wifihelper.ui.theme.ProOutline
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import com.student.wifihelper.ui.theme.StatusGreen
import com.student.wifihelper.ui.theme.StatusRed
import com.student.wifihelper.ui.theme.StatusYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize

@Composable
fun DashboardScreen(
    networkState: NetworkState,
    config: UserConfig?,
    isServiceRunning: Boolean,
    isLoading: Boolean,
    onCheckStatus: () -> Unit,
    onPerformLogin: () -> Unit,
    onToggleService: (Boolean) -> Unit,
    onNavigateToCredentials: () -> Unit,
    wifiUsageGb: Double = 0.0,
    onSwitchPositioned: (Rect) -> Unit = {},
    onConfigurePositioned: (Rect) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isGameCenterOpen by remember { mutableStateOf(false) }
    var userPoints by remember { mutableIntStateOf(150) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatusHeroCard(
            networkState = networkState,
            isLoading = isLoading,
            onCheckStatus = onCheckStatus,
            onPerformLogin = onPerformLogin
        )

        // Middle Ad Banner placed between Wi-Fi Status card and Login Button
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ProOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AdBannerView(adUnitId = "ca-app-pub-9942249708634262/3295983760")
            }
        }

        // Wi-Fi Disabled In-Page Banner Box Card
        if (networkState is NetworkState.WifiDisabled) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.student.wifihelper.R.string.wifi_off_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.student.wifihelper.R.string.wifi_off_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.student.wifihelper.R.string.open_wifi_settings),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }

        if (config != null && (config.username.isBlank() || config.password.isBlank())) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusYellow,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Credentials Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Please configure your Campus login ID and password.",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                    Button(
                        onClick = onNavigateToCredentials,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.onGloballyPositioned { coords ->
                            onConfigurePositioned(Rect(coords.positionInRoot(), coords.size.toSize()))
                        }
                    ) {
                        Text("Configure", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ProOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Game Center & Points Entrance Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = ProPurpleContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { isGameCenterOpen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎮", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "UTAR Mini-Game Center",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ProPurpleOnContainer
                                )
                                Text(
                                    text = "⭐ $userPoints Points • Play 4 games to earn points!",
                                    fontSize = 11.sp,
                                    color = ProPurpleOnContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = { isGameCenterOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Play Games", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = onPerformLogin,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Authenticating...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Login,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manual Login Push", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        // Bottom AdMob Banner Container
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ProOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AdBannerView(adUnitId = "ca-app-pub-9942249708634262/1850707016")
            }
        }
    }

    if (isGameCenterOpen) {
        GameCenterScreen(
            userPoints = userPoints,
            onAddPoints = { userPoints += it },
            onRedeemOffer = { offer ->
                if (userPoints >= offer.pointsCost) {
                    userPoints -= offer.pointsCost
                }
            },
            onDismiss = { isGameCenterOpen = false }
        )
    }
}

@Composable
private fun StatusHeroCard(
    networkState: NetworkState,
    isLoading: Boolean,
    onCheckStatus: () -> Unit,
    onPerformLogin: () -> Unit
) {
    val (statusTitle, statusSubtitle, statusTag) = when (networkState) {
        is NetworkState.InternetActive -> Triple(
            "Guest_WiFi_Public",
            "Internet connected (HTTP 204 verified)",
            "CONNECTED"
        )
        is NetworkState.CaptivePortalDetected -> Triple(
            "utarwifi",
            "Portal detected: 10.10.0.1 / 192.168.1.1",
            "PORTAL DETECTED"
        )
        is NetworkState.ConnectedNotTarget -> Triple(
            networkState.currentSsid,
            "Connected (Target: ${networkState.targetSsid})",
            "NON-TARGET"
        )
        is NetworkState.WifiConnected -> Triple(
            networkState.ssid ?: "WiFi Connected",
            if (networkState.ssid == null) "Network name hidden by system." else "Waiting for portal detection...",
            "WIFI ACTIVE"
        )
        is NetworkState.Checking -> Triple(
            "Checking Network...",
            "Sending HTTP probes to portal endpoints...",
            "CHECKING"
        )
        is NetworkState.WifiDisabled -> Triple(
            "Wi-Fi Turned OFF",
            "Wi-Fi adapter is disabled. Tap below to turn ON Wi-Fi.",
            "WIFI OFF"
        )
        is NetworkState.NotConnected -> Triple(
            "Wi-Fi ON (Not Connected)",
            "Wi-Fi is ON. Please select and connect to 'utarwifi' network.",
            "NOT CONNECTED"
        )
        is NetworkState.Disconnected -> Triple(
            "Offline",
            "Please connect to 'utarwifi' campus network.",
            "OFFLINE"
        )
        is NetworkState.Error -> Triple(
            "Connection Error",
            networkState.message,
            "ERROR"
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ProPurpleContainer),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current Connection",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ProPurpleOnContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProPurpleOnContainer
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (networkState is NetworkState.InternetActive) StatusGreen else StatusYellow)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProPurpleOnContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Router,
                    contentDescription = null,
                    tint = ProPurpleOnContainer.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusSubtitle,
                    fontSize = 12.sp,
                    color = ProPurpleOnContainer.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCheckStatus,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ProPurpleOnContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ProPurpleOnContainer.copy(alpha = 0.4f))
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Re-Check", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun AdBannerView(
    modifier: Modifier = Modifier,
    // Live AdMob Banner Ad Unit ID
    adUnitId: String = "ca-app-pub-9942249708634262/1850707016"
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
