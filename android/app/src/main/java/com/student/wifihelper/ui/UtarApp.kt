package com.student.wifihelper.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.stringResource
import com.student.wifihelper.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.geometry.Rect
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import com.student.wifihelper.ui.components.OnboardingOverlay
import com.student.wifihelper.ui.components.OnboardingTarget
import com.student.wifihelper.ui.screens.ConfessionScreen
import com.student.wifihelper.ui.screens.CredentialsScreen
import com.student.wifihelper.ui.screens.DashboardScreen
import com.student.wifihelper.ui.screens.LogsScreen
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import com.student.wifihelper.ui.theme.StatusGreen

enum class AppTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Status", Icons.Default.Dashboard),
    CREDENTIALS("Credentials", Icons.Default.VpnKey),
    CONFESSIONS("Confessions", Icons.Outlined.ChatBubbleOutline)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtarApp(
    viewModel: UtarViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    val config by viewModel.configState.collectAsState()
    val logs by viewModel.logsState.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val isLoading by viewModel.isLoginLoading.collectAsState()
    val onboardingStep by viewModel.onboardingStep.collectAsState()
    val draftUsername by viewModel.draftUsername.collectAsState()
    val draftPassword by viewModel.draftPassword.collectAsState()
    val wifiUsageGb by viewModel.wifiUsageGb.collectAsState()
    val shouldAnimateSave by viewModel.shouldAnimateSave.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    // Real-time Wi-Fi state listener to instantly update status when Wi-Fi is toggled ON/OFF
    androidx.compose.runtime.DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                viewModel.checkNetworkStatus()
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(android.net.wifi.WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
            addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearToastMessage()
        }
    }

    var credentialsTabRect by remember { mutableStateOf(Rect.Zero) }
    var dashboardTabRect by remember { mutableStateOf(Rect.Zero) }
    var configureButtonRect by remember { mutableStateOf(Rect.Zero) }
    var usernameFieldRect by remember { mutableStateOf(Rect.Zero) }
    var passwordFieldRect by remember { mutableStateOf(Rect.Zero) }
    var saveButtonRect by remember { mutableStateOf(Rect.Zero) }
    var autoReconnectSwitchRect by remember { mutableStateOf(Rect.Zero) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ProPurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "U不停歇WIFI助手",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CAMPUS WIFI HELPER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setOnboardingStep(1) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.replay_tour),
                            tint = ProPurplePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { 
                            // Check for unsaved changes before switching
                            if (selectedTab == AppTab.CREDENTIALS && tab != AppTab.CREDENTIALS) {
                                if (draftUsername != (config?.username ?: "") || draftPassword != (config?.password ?: "")) {
                                    viewModel.triggerSaveAnimation()
                                    return@NavigationBarItem
                                }
                            }
                            selectedTab = tab 
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 11.sp, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ProPurpleOnContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onBackground,
                            indicatorColor = ProPurpleContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .testTag("tab_${tab.name.lowercase()}")
                            .onGloballyPositioned { coords ->
                                if (tab == AppTab.CREDENTIALS) credentialsTabRect = Rect(coords.positionInRoot(), coords.size.toSize())
                                if (tab == AppTab.DASHBOARD) dashboardTabRect = Rect(coords.positionInRoot(), coords.size.toSize())
                            }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    networkState = networkState,
                    config = config,
                    isServiceRunning = isServiceRunning,
                    isLoading = isLoading,
                    wifiUsageGb = wifiUsageGb,
                    onCheckStatus = { viewModel.checkNetworkStatus() },
                    onPerformLogin = {
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        if (config?.biometricLockEnabled == true && activity != null) {
                            com.student.wifihelper.ui.security.BiometricHelper.authenticate(
                                activity = activity,
                                title = context.getString(com.student.wifihelper.R.string.auth_to_connect),
                                onSuccess = { viewModel.performManualLogin() },
                                onError = { err ->
                                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            viewModel.performManualLogin()
                        }
                    },
                    onToggleService = { viewModel.toggleService(it) },
                    onNavigateToCredentials = { selectedTab = AppTab.CREDENTIALS },
                    onSwitchPositioned = { autoReconnectSwitchRect = it },
                    onConfigurePositioned = { configureButtonRect = it }
                )
                AppTab.CREDENTIALS -> CredentialsScreen(
                    config = config,
                    usernameDraft = draftUsername,
                    passwordDraft = draftPassword,
                    onUsernameChange = { viewModel.draftUsername.value = it },
                    onPasswordChange = { viewModel.draftPassword.value = it },
                    shouldAnimateSave = shouldAnimateSave,
                    onboardingStep = onboardingStep,
                    onSaveConfig = { username, password, targetSsid, autoLogin, interval, biometricLock ->
                        viewModel.saveConfig(username, password, targetSsid, autoLogin, interval, biometricLock)
                        if (onboardingStep == 5) {
                            viewModel.nextOnboardingStep()
                        }
                    },
                    onTestLogin = { viewModel.performManualLogin() },
                    onUsernamePositioned = { usernameFieldRect = it },
                    onPasswordPositioned = { passwordFieldRect = it },
                    onSavePositioned = { saveButtonRect = it }
                )
                AppTab.CONFESSIONS -> ConfessionScreen()
            }
        }
    }

    // Onboarding Overlay - MOVED OUTSIDE SCAFFOLD
    if (onboardingStep != null) {
        // Monitor state for automatic progression
        LaunchedEffect(onboardingStep, selectedTab, isServiceRunning) {
            when (onboardingStep) {
                2 -> if (selectedTab == AppTab.CREDENTIALS) viewModel.nextOnboardingStep()
                // Steps 3 & 4 are now manual "Next" buttons
                6 -> if (selectedTab == AppTab.DASHBOARD) viewModel.nextOnboardingStep()
                7 -> if (isServiceRunning) viewModel.nextOnboardingStep()
            }
        }

        val target = when (onboardingStep) {
            1 -> OnboardingTarget(
                rect = Rect.Zero,
                title = stringResource(R.string.onboarding_step1_title),
                description = stringResource(R.string.onboarding_step1_desc)
            )
            2 -> OnboardingTarget(
                rect = configureButtonRect.takeIf { it != Rect.Zero } ?: credentialsTabRect,
                title = stringResource(R.string.onboarding_step2_title),
                description = stringResource(R.string.onboarding_step2_desc),
                showNext = false
            )
            3 -> {
                if (selectedTab != AppTab.CREDENTIALS) selectedTab = AppTab.CREDENTIALS
                OnboardingTarget(
                    rect = usernameFieldRect,
                    title = stringResource(R.string.onboarding_step3_title),
                    description = stringResource(R.string.onboarding_step3_desc),
                    showNext = true
                )
            }
            4 -> OnboardingTarget(
                rect = passwordFieldRect,
                title = stringResource(R.string.onboarding_step4_title),
                description = stringResource(R.string.onboarding_step4_desc),
                showNext = true
            )
            5 -> OnboardingTarget(
                rect = saveButtonRect,
                title = stringResource(R.string.onboarding_step5_title),
                description = stringResource(R.string.onboarding_step5_desc),
                showNext = true
            )
            6 -> OnboardingTarget(
                rect = dashboardTabRect,
                title = stringResource(R.string.onboarding_step6_title),
                description = stringResource(R.string.onboarding_step6_desc),
                showNext = false
            )
            7 -> {
                if (selectedTab != AppTab.DASHBOARD) selectedTab = AppTab.DASHBOARD
                OnboardingTarget(
                    rect = autoReconnectSwitchRect,
                    title = stringResource(R.string.onboarding_step7_title),
                    description = stringResource(R.string.onboarding_step7_desc),
                    showNext = false
                )
            }
            8 -> OnboardingTarget(
                rect = Rect.Zero,
                title = stringResource(R.string.onboarding_step8_title),
                description = stringResource(R.string.onboarding_step8_desc)
            )
            else -> null
        }
        
        OnboardingOverlay(
            target = target,
            step = onboardingStep!!,
            totalSteps = 8,
            onNext = { viewModel.nextOnboardingStep() },
            onSkip = { viewModel.skipOnboarding() }
        )
    }
}
