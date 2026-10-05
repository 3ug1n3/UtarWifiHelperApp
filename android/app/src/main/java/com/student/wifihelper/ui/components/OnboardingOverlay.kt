package com.student.wifihelper.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.ui.theme.ProPurplePrimary

data class OnboardingTarget(
    val rect: Rect,
    val title: String,
    val description: String,
    val showNext: Boolean = true
)

@Composable
fun OnboardingOverlay(
    target: OnboardingTarget?,
    step: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    if (target == null) return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val dimColor = Color.Black.copy(alpha = 0.65f)

    Box(modifier = Modifier.fillMaxSize()) {
        if (target.rect != Rect.Zero) {
            val hole = target.rect.inflate(with(density) { 8.dp.toPx() })
            
            // 4-Box Architecture: This creates a literal PHYSICAL HOLE in the UI.
            // Since there is no box at the hole coordinates, touches go straight to the app.
            
            // Top box
            Box(modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawRect(color = dimColor, size = androidx.compose.ui.geometry.Size(screenWidthPx, hole.top))
                })

            // Bottom box
            Box(modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawRect(
                        color = dimColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, hole.bottom),
                        size = androidx.compose.ui.geometry.Size(screenWidthPx, screenHeightPx - hole.bottom)
                    )
                })

            // Left box
            Box(modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawRect(
                        color = dimColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, hole.top),
                        size = androidx.compose.ui.geometry.Size(hole.left, hole.height)
                    )
                })

            // Right box
            Box(modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawRect(
                        color = dimColor,
                        topLeft = androidx.compose.ui.geometry.Offset(hole.right, hole.top),
                        size = androidx.compose.ui.geometry.Size(screenWidthPx - hole.right, hole.height)
                    )
                })

            // Pulsing Border (Non-blocking)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = ProPurplePrimary.copy(alpha = pulseAlpha),
                    topLeft = hole.topLeft,
                    size = hole.size,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        } else {
            // Full screen dim for step 1 or completion
            Box(modifier = Modifier.fillMaxSize().background(dimColor))
        }

        // Tooltip - Intercepts touches for its own buttons
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = if (target.rect == Rect.Zero || target.rect.top > screenHeightPx / 2) Alignment.TopCenter else Alignment.BottomCenter
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.width(320.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { step / totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = ProPurplePrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = target.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ProPurplePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onSkip, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Skip", tint = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = target.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$step / $totalSteps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (target.showNext) {
                            TextButton(onClick = onSkip) {
                                Text("Skip", fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = onNext,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (step == totalSteps) "Got it!" else "Next", fontSize = 13.sp)
                                if (step < totalSteps) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Action Required",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProPurplePrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
