package com.student.wifihelper

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.ui.UtarApp
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import com.student.wifihelper.ui.theme.UtarWifiTheme
import kotlinx.coroutines.launch

class MainActivity : androidx.fragment.app.FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UtarWifiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppWithSplashScreen()
                }
            }
        }
    }
}

@Composable
fun AppWithSplashScreen() {
    var isSplashFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        isSplashFinished = true
    }

    Crossfade(
        targetState = isSplashFinished,
        animationSpec = tween(durationMillis = 600),
        label = "splashCrossfade"
    ) { finished ->
        if (finished) {
            UtarApp()
        } else {
            AnimatedSplashScreen()
        }
    }
}

@Composable
fun AnimatedSplashScreen() {
    val scale = remember { androidx.compose.animation.core.Animatable(0.3f) }
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(1200, easing = LinearEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2A1B54), Color(0xFF0F0B1E)),
                    center = Offset.Unspecified
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // 3D Outer Rotating Glowing Ring
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(3.dp, Brush.sweepGradient(listOf(ProPurplePrimary, Color.Cyan, ProPurplePrimary))),
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer(
                            scaleX = scale.value * 1.15f,
                            scaleY = scale.value * 1.15f,
                            rotationZ = rotation.value
                        )
                ) {}

                // Center Icon Container
                Surface(
                    shape = CircleShape,
                    color = ProPurpleContainer,
                    border = BorderStroke(2.dp, ProPurplePrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "App Icon",
                            tint = ProPurplePrimary,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "U不停歇WIFI助手",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "UTAR Campus Wi-Fi Auto-Login & Helper",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ProPurplePrimary
            )
        }
    }
}
