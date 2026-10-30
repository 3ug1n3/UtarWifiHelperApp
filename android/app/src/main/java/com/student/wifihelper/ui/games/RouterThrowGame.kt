package com.student.wifihelper.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import com.student.wifihelper.ui.theme.StatusGreen
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RouterThrowGame(
    onGameEnd: (score: Int) -> Unit,
    onClose: () -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var knivesRemaining by remember { mutableIntStateOf(7) }
    var isGameOver by remember { mutableStateOf(false) }
    var isWin by remember { mutableStateOf(false) }

    val stuckAngles = remember { mutableStateListOf<Float>() }
    var flyingKnifeProgress by remember { mutableFloatStateOf(0f) }
    var isKnifeFlying by remember { mutableStateOf(false) }

    var wheelAngle by remember { mutableFloatStateOf(0f) }
    var wheelSpeed by remember { mutableFloatStateOf(2f) }

    // Rotate wheel continuously
    LaunchedEffect(isGameOver) {
        while (!isGameOver) {
            wheelAngle = (wheelAngle + wheelSpeed) % 360f
            // Periodically reverse wheel direction for skill variation
            if (kotlin.random.Random.nextInt(100) < 3) {
                wheelSpeed = -wheelSpeed
            }
            delay(16)
        }
    }

    // Flying knife animation
    LaunchedEffect(isKnifeFlying) {
        if (isKnifeFlying) {
            var progress = 0f
            while (progress < 1.0f) {
                progress += 0.15f
                flyingKnifeProgress = progress
                delay(16)
            }
            // Knife hit target
            isKnifeFlying = false
            flyingKnifeProgress = 0f

            // Calculate hit angle relative to wheel's current rotation
            val hitAngleOnWheel = (270f - wheelAngle + 360f) % 360f

            // Check collision with existing stuck knives (within 18 degrees)
            val isCollision = stuckAngles.any { angle ->
                val diff = abs(angle - hitAngleOnWheel)
                diff < 18f || (360f - diff) < 18f
            }

            if (isCollision) {
                isGameOver = true
                isWin = false
            } else {
                stuckAngles.add(hitAngleOnWheel)
                score += 10
                knivesRemaining--

                if (knivesRemaining <= 0) {
                    isGameOver = true
                    isWin = true
                    onGameEnd(score + 50) // Bonus for clearing level
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141218))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎯 Router Throw",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Tap screen to launch antenna into spinning hub!",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.DarkGray, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Score & Knives Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = ProPurpleContainer)) {
                    Text(
                        text = "⭐ Points: $score",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ProPurpleOnContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))) {
                    Text(
                        text = "📡 Antennas Left: $knivesRemaining",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Game Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(enabled = !isKnifeFlying && !isGameOver) {
                        isKnifeFlying = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    val centerY = size.height * 0.35f
                    val wheelRadius = 80.dp.toPx()

                    // Draw Spinning Wi-Fi Target Wheel
                    rotate(degrees = wheelAngle, pivot = Offset(centerX, centerY)) {
                        drawCircle(
                            color = ProPurplePrimary,
                            radius = wheelRadius,
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = wheelRadius - 10.dp.toPx(),
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 16.dp.toPx(),
                            center = Offset(centerX, centerY)
                        )

                        // Draw stuck antennas
                        stuckAngles.forEach { angle ->
                            val rad = Math.toRadians(angle.toDouble())
                            val startX = centerX + (wheelRadius * cos(rad)).toFloat()
                            val startY = centerY + (wheelRadius * sin(rad)).toFloat()
                            val endX = centerX + ((wheelRadius + 45.dp.toPx()) * cos(rad)).toFloat()
                            val endY = centerY + ((wheelRadius + 45.dp.toPx()) * sin(rad)).toFloat()

                            drawLine(
                                color = StatusGreen,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 8.dp.toPx()
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = Offset(endX, endY)
                            )
                        }
                    }

                    // Draw Flying Antenna
                    val knifeStartY = size.height * 0.85f
                    val knifeTargetY = centerY + wheelRadius + 20.dp.toPx()
                    val currentKnifeY = if (isKnifeFlying) {
                        knifeStartY - (flyingKnifeProgress * (knifeStartY - knifeTargetY))
                    } else {
                        knifeStartY
                    }

                    if (!isGameOver) {
                        drawLine(
                            color = StatusGreen,
                            start = Offset(centerX, currentKnifeY),
                            end = Offset(centerX, currentKnifeY + 45.dp.toPx()),
                            strokeWidth = 8.dp.toPx()
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(centerX, currentKnifeY)
                        )
                    }
                }
            }

            // Bottom Tap Prompt / Game Over Popup
            if (isGameOver) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isWin) ProPurpleContainer else MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isWin) "🎉 Level Cleared! +$score Pts" else "💥 Antenna Collision!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isWin) ProPurpleOnContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    score = 0
                                    knivesRemaining = 7
                                    stuckAngles.clear()
                                    isGameOver = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Again")
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF2B2930),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👆 TAP ANYWHERE TO LAUNCH ANTENNA 🚀",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
