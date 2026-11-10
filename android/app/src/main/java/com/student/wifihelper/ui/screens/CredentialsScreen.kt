package com.student.wifihelper.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.data.model.UserConfig
import com.student.wifihelper.ui.theme.ProOutline
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import kotlinx.coroutines.launch

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toSize

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale

@Composable
fun CredentialsScreen(
    config: UserConfig?,
    usernameDraft: String,
    passwordDraft: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSaveConfig: (username: String, password: String, targetSsid: String, autoLogin: Boolean, pollingInterval: Int, biometricLock: Boolean) -> Unit,
    onTestLogin: () -> Unit,
    shouldAnimateSave: Boolean = false,
    onboardingStep: Int? = null,
    onUsernamePositioned: (Rect) -> Unit = {},
    onPasswordPositioned: (Rect) -> Unit = {},
    onSavePositioned: (Rect) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var targetSsid by remember { mutableStateOf(config?.targetSsid ?: "utarwifi") }
    var autoLoginEnabled by remember { mutableStateOf(config?.autoLoginEnabled ?: true) }
    var pollingInterval by remember { mutableStateOf(config?.pollingIntervalSeconds ?: 30) }
    var biometricLockEnabled by remember { mutableStateOf(config?.biometricLockEnabled ?: false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val saveButtonScale by animateFloatAsState(
        targetValue = if (shouldAnimateSave) 1.25f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "saveScale"
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Auto-scroll to Save button when nudge or step 5 is triggered
    LaunchedEffect(shouldAnimateSave, onboardingStep) {
        if (shouldAnimateSave || onboardingStep == 5) {
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = tween(durationMillis = 600)
            )
        } else if (onboardingStep == 3 || onboardingStep == 4) {
            scrollState.animateScrollTo(
                value = 0,
                animationSpec = tween(durationMillis = 400)
            )
        }
    }

    var isConfigInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(config) {
        if (config != null && !isConfigInitialized) {
            targetSsid = config.targetSsid
            autoLoginEnabled = config.autoLoginEnabled
            pollingInterval = config.pollingIntervalSeconds
            biometricLockEnabled = config.biometricLockEnabled
            isConfigInitialized = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SnackbarHost(hostState = snackbarHostState)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ProOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Campus Portal Credentials",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Your credentials are saved locally in Room SQLite database and sent directly to the campus captive portal login endpoint.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = usernameDraft,
                    onValueChange = onUsernameChange,
                    label = { Text("Login ID / Student ID") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ProPurplePrimary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            onUsernamePositioned(Rect(coords.positionInRoot(), coords.size.toSize()))
                        }
                )

                OutlinedTextField(
                    value = passwordDraft,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = ProPurplePrimary)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (biometricLockEnabled && !passwordVisible) {
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity != null) {
                                    com.student.wifihelper.ui.security.BiometricHelper.authenticate(
                                        activity = activity,
                                        title = context.getString(com.student.wifihelper.R.string.auth_to_view_credentials),
                                        onSuccess = { passwordVisible = true },
                                        onError = {}
                                    )
                                } else {
                                    passwordVisible = true
                                }
                            } else {
                                passwordVisible = !passwordVisible
                            }
                        }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            onPasswordPositioned(Rect(coords.positionInRoot(), coords.size.toSize()))
                        }
                )

                OutlinedTextField(
                    value = targetSsid,
                    onValueChange = { targetSsid = it },
                    label = { Text("Target WiFi SSID") },
                    leadingIcon = {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = ProPurplePrimary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Auto-Login Behavior & Gamified Time Remaining Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ProOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Auto-Login Behavior",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Remaining Unlocked Time Status Banner (Ticking only when Auto-Connect is ON)
                val remainingSeconds = config?.autoConnectRemainingSeconds ?: 0L
                val isUnlocked = remainingSeconds > 0L
                val remainingHours = remainingSeconds / 3600L
                val remainingMinutes = (remainingSeconds % 3600L) / 60L
                val remainingSecs = remainingSeconds % 60L

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isUnlocked) ProPurpleContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isUnlocked) ProPurplePrimary.copy(alpha = 0.3f) else ProOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUnlocked) "⚡" else "🔒",
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isUnlocked) (if (autoLoginEnabled) "Auto-Connect Active (Counting Down)" else "Auto-Connect Paused (Saved)") else "Auto-Connect Time Locked",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isUnlocked) ProPurpleOnContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isUnlocked) "Time remaining: ${remainingHours}h ${remainingMinutes}m ${remainingSecs}s ${if (!autoLoginEnabled) "(PAUSED)" else ""}" else "Earn points in Games tab to unlock auto-connect hours!",
                                fontSize = 12.sp,
                                fontWeight = if (isUnlocked) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isUnlocked) ProPurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Auto-Login", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "Automatically detect captive portal and submit login request.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoLoginEnabled,
                        onCheckedChange = { enabled ->
                            autoLoginEnabled = enabled
                            onSaveConfig(usernameDraft, passwordDraft, targetSsid, enabled, pollingInterval, biometricLockEnabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ProPurplePrimary
                        )
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = ProPurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Background Polling Interval", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Text("$pollingInterval seconds", fontWeight = FontWeight.Bold, color = ProPurplePrimary)
                    }

                    Slider(
                        value = pollingInterval.toFloat(),
                        onValueChange = { pollingInterval = it.toInt() },
                        valueRange = 10f..120f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = ProPurplePrimary,
                            activeTrackColor = ProPurplePrimary
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Dedicated Security & Biometrics Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ProOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = ProPurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Security & Biometrics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.student.wifihelper.R.string.biometric_lock),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.student.wifihelper.R.string.biometric_lock_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = biometricLockEnabled,
                        onCheckedChange = { biometricLockEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ProPurplePrimary
                        )
                    )
                }
            }
        }

        // Feedback & Bug Report Card
        var feedbackText by remember { mutableStateOf("") }
        var isSubmittingFeedback by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ProOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = null,
                        tint = ProPurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Feedback & Bug Report",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Facing an issue or have a feature suggestion? Feedback is sent directly to developer email (01110708861.my@gmail.com) & Firebase cloud!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Write your feedback or issue report here...", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProPurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val text = feedbackText.trim()
                            if (text.isNotBlank()) {
                                isSubmittingFeedback = true
                                scope.launch {
                                    try {
                                        val docId = java.util.UUID.randomUUID().toString()
                                        val data = mapOf(
                                            "id" to docId,
                                            "content" to text,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("feedback")
                                            .document(docId)
                                            .set(data)
                                    } catch (e: Exception) {}
                                    isSubmittingFeedback = false
                                    feedbackText = ""
                                    snackbarHostState.showSnackbar("Thank you! Your feedback has been received.")
                                }
                            }
                        },
                        enabled = feedbackText.trim().isNotBlank() && !isSubmittingFeedback,
                        colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isSubmittingFeedback) "Submitting..." else "Submit Cloud", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val text = feedbackText.trim()
                            try {
                                val encodedSubject = android.net.Uri.encode("UTAR WiFi Helper App Feedback")
                                val encodedBody = android.net.Uri.encode(if (text.isNotBlank()) text else "App Feedback")
                                val mailUri = android.net.Uri.parse("mailto:01110708861.my@gmail.com?subject=$encodedSubject&body=$encodedBody")
                                val intent = Intent(Intent.ACTION_SENDTO, mailUri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found on device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ProPurpleContainer, contentColor = ProPurpleOnContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Send Email", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Button(
            onClick = {
                onSaveConfig(usernameDraft, passwordDraft, targetSsid, autoLoginEnabled, pollingInterval, biometricLockEnabled)
                scope.launch {
                    snackbarHostState.showSnackbar("Configuration saved successfully!")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .scale(saveButtonScale)
                .onGloballyPositioned { coords ->
                    onSavePositioned(Rect(coords.positionInRoot(), coords.size.toSize()))
                }
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Credentials", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
