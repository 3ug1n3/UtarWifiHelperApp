package com.student.wifihelper.ui.games

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import com.student.wifihelper.ui.theme.StatusGreen
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class SparkParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float = 1.0f
)

@Composable
fun RouterThrowGame(
    onGameEnd: (score: Int) -> Unit,
    onClose: () -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var knivesRemaining by remember { mutableIntStateOf(7) }
    var isGameOver by remember { mutableStateOf(false) }
    var isWin by remember { mutableStateOf(false) }

    val stuckAngles = remember { mutableStateListOf<Float>() }
    val particles = remember { mutableStateListOf<SparkParticle>() }

    var flyingKnifeProgress by remember { mutableFloatStateOf(0f) }
    var isKnifeFlying by remember { mutableStateOf(false) }

    var wheelAngle by remember { mutableFloatStateOf(0f) }
    var wheelSpeed by remember { mutableFloatStateOf(2.5f) }

    // Smooth Wheel Rotation Loop
    LaunchedEffect(isGameOver, level) {
        var stepCount = 0
        while (!isGameOver) {
            stepCount++
            // Sine-wave speed fluctuation for smooth difficulty
            val speedFactor = 1.0f + 0.3f * sin(stepCount * 0.05f).toFloat()
            wheelAngle = (wheelAngle + wheelSpeed * speedFactor) % 360f

            if (stepCount % 120 == 0 && level >= 2) {
                wheelSpeed = -wheelSpeed // Smooth direction reversal
            }

            // Update particle effects
            val iter = particles.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                p.x += p.vx
                p.y += p.vy
                p.alpha -= 0.05f
                if (p.alpha <= 0f) iter.remove()
            }

            delay(16)
        }
    }

    // Flying Knife Animation
    LaunchedEffect(isKnifeFlying) {
        if (isKnifeFlying) {
            var progress = 0f
            while (progress < 1.0f) {
                progress += 0.2f
                flyingKnifeProgress = progress
                delay(16)
            }

            isKnifeFlying = false
            flyingKnifeProgress = 0f

            val hitAngleOnWheel = (270f - wheelAngle + 360f) % 360f

            // Collision Check (within 18 degrees)
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

                // Spawn Spark Particles
                for (i in 0..12) {
                    val angleRad = kotlin.random.Random.nextDouble(0.0, 2 * Math.PI)
                    val speed = kotlin.random.Random.nextFloat() * 8f + 2f
                    particles.add(
                        SparkParticle(
                            x = 0f, // Center relative
                            y = 0f,
                            vx = (cos(angleRad) * speed).toFloat(),
                            vy = (sin(angleRad) * speed).toFloat()
                        )
                    )
                }

                if (knivesRemaining <= 0) {
                    if (level < 3) {
                        level++
                        knivesRemaining = 7 + level
                        stuckAngles.clear()
                        wheelSpeed = if (level % 2 == 0) -3.5f else 3.5f
                    } else {
                        isGameOver = true
                        isWin = true
                        onGameEnd(score + 100)
                    }
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎯 Router Throw (Level $level)",
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

            // Dashboard Info Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = ProPurpleContainer)) {
                    Text(
                        text = "⭐ Points: $score",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ProPurpleOnContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))) {
                    Text(
                        text = "📡 Antennas: $knivesRemaining",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
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

                    // Draw Spinning Target Wheel
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

                        // Draw Stuck Antennas
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

                    // Draw Sparks Particle Effect
                    particles.forEach { p ->
                        drawCircle(
                            color = Color.Yellow.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                            radius = 4.dp.toPx(),
                            center = Offset(centerX + p.x, centerY + wheelRadius + p.y)
                        )
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
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isWin) "🎉 Level Cleared! +$score Pts" else "💥 Collision! +$score Pts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isWin) ProPurpleOnContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                score = 0
                                level = 1
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
            } else {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF2B2930),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(14.dp),
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
