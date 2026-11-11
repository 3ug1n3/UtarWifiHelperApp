package com.student.wifihelper.ui.games

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import com.student.wifihelper.ui.theme.StatusGreen
import kotlinx.coroutines.delay
import kotlin.math.abs

data class TrafficObstacle(
    val lane: Int, // 0, 1, 2
    var yPosProgress: Float, // 0.0f (top) to 1.0f (bottom)
    val isDataPacket: Boolean = false
)

@Composable
fun TurboRacerGame(
    onGameEnd: (score: Int) -> Unit,
    onClose: () -> Unit
) {
    var playerLane by remember { mutableIntStateOf(1) } // 0=Left, 1=Center, 2=Right
    var score by remember { mutableIntStateOf(0) }
    var distanceMeters by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }

    val animatedLaneOffset by animateFloatAsState(
        targetValue = playerLane.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "laneSlide"
    )

    val obstacles = remember { mutableStateListOf<TrafficObstacle>() }
    var roadOffsetY by remember { mutableFloatStateOf(0f) }

    // Game loop
    LaunchedEffect(isGameOver) {
        var frameCounter = 0
        while (!isGameOver) {
            frameCounter++
            distanceMeters += 1
            roadOffsetY = (roadOffsetY + 18f) % 60f

            // Spawn obstacles
            if (frameCounter % 32 == 0) {
                val spawnLane = kotlin.random.Random.nextInt(3)
                val isData = kotlin.random.Random.nextBoolean()
                obstacles.add(TrafficObstacle(lane = spawnLane, yPosProgress = -0.1f, isDataPacket = isData))
            }

            // Move obstacles down smoothly
            val iterator = obstacles.iterator()
            while (iterator.hasNext()) {
                val obs = iterator.next()
                obs.yPosProgress += 0.018f

                // Exact relative visual bounding box collision check at player Y (0.78f)
                val isVisualYOverlap = abs(obs.yPosProgress - 0.78f) < 0.045f
                val isVisualXOverlap = abs(animatedLaneOffset - obs.lane.toFloat()) < 0.40f

                if (isVisualYOverlap && isVisualXOverlap) {
                    if (obs.isDataPacket) {
                        score += 25
                        iterator.remove()
                    } else {
                        isGameOver = true
                        onGameEnd(score + (distanceMeters / 10))
                    }
                } else if (obs.yPosProgress > 1.1f) {
                    iterator.remove()
                }
            }

            delay(16)
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
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🏎️ Turbo Wi-Fi Racer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Tap Left/Right to dodge buggies & collect green data!",
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

            Spacer(modifier = Modifier.height(8.dp))

            // Dashboard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = ProPurpleContainer)) {
                    Text(
                        text = "⭐ Points: ${score + (distanceMeters / 10)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ProPurpleOnContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))) {
                    Text(
                        text = "🚩 Distance: ${distanceMeters}m",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3D Perspective Canvas Highway
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val horizonY = h * 0.15f
                    val horizonW = w * 0.25f

                    // Draw Cyber Sky Gradient
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F0B1E), Color(0xFF1D1635))
                        ),
                        size = Size(w, horizonY)
                    )

                    // Draw 3D Perspective Road
                    val roadPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo((w - horizonW) / 2f, horizonY)
                        lineTo((w + horizonW) / 2f, horizonY)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = roadPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E1A2B), Color(0xFF2D2640))
                        )
                    )

                    // Draw 3D Perspective Lane Dividers
                    val dashCount = 8
                    for (i in 0 until dashCount) {
                        val progress = ((i.toFloat() / dashCount) + (roadOffsetY / 60f)) % 1.0f
                        val y = horizonY + progress * (h - horizonY)
                        val scale = 0.25f + progress * 0.75f
                        val currentW = horizonW + (w - horizonW) * progress

                        val l1X = (w - currentW) / 2f + (currentW / 3f)
                        val l2X = (w - currentW) / 2f + (currentW * 2f / 3f)

                        drawLine(color = Color.Yellow.copy(alpha = progress.coerceIn(0.2f, 0.9f)), start = Offset(l1X, y), end = Offset(l1X, y + 15f * scale), strokeWidth = 5f * scale)
                        drawLine(color = Color.Yellow.copy(alpha = progress.coerceIn(0.2f, 0.9f)), start = Offset(l2X, y), end = Offset(l2X, y + 15f * scale), strokeWidth = 5f * scale)
                    }

                    // Draw 3D Obstacles & Data Orbs with Perspective Depth Scale
                    obstacles.forEach { obs ->
                        val p = obs.yPosProgress.coerceIn(0f, 1f)
                        val currentW = horizonW + (w - horizonW) * p
                        val scale = 0.3f + p * 0.7f
                        val obsY = horizonY + p * (h - horizonY)
                        val laneCenterX = (w - currentW) / 2f + (currentW / 6f) + (obs.lane * (currentW / 3f))

                        if (obs.isDataPacket) {
                            drawCircle(
                                color = StatusGreen,
                                radius = 22f * scale,
                                center = Offset(laneCenterX, obsY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 10f * scale,
                                center = Offset(laneCenterX, obsY)
                            )
                        } else {
                            // 3D Shadow
                            drawOval(
                                color = Color.Black.copy(alpha = 0.4f),
                                topLeft = Offset(laneCenterX - 30f * scale, obsY + 20f * scale),
                                size = Size(60f * scale, 20f * scale)
                            )
                            // 3D Block Chassis
                            drawRoundRect(
                                color = Color(0xFFE91E63),
                                topLeft = Offset(laneCenterX - 32f * scale, obsY - 40f * scale),
                                size = Size(64f * scale, 80f * scale),
                                cornerRadius = CornerRadius(12f * scale)
                            )
                        }
                    }

                    // Draw Player 3D Cyber Car
                    if (!isGameOver) {
                        val playerY = h * 0.82f
                        val currentW = horizonW + (w - horizonW) * 0.82f
                        val carX = (w - currentW) / 2f + (currentW / 6f) + (animatedLaneOffset * (currentW / 3f))

                        // Headlight Beam Effect
                        val beamPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(carX - 25f, playerY - 30f)
                            lineTo(carX - 50f, playerY - 180f)
                            lineTo(carX + 50f, playerY - 180f)
                            lineTo(carX + 25f, playerY - 30f)
                            close()
                        }
                        drawPath(
                            path = beamPath,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Cyan.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )

                        // 3D Car Shadow
                        drawOval(
                            color = Color.Black.copy(alpha = 0.5f),
                            topLeft = Offset(carX - 40f, playerY + 30f),
                            size = Size(80f, 25f)
                        )

                        // 3D Metallic Car Body
                        drawRoundRect(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(ProPurplePrimary, Color(0xFF4A148C))
                            ),
                            topLeft = Offset(carX - 38f, playerY - 45f),
                            size = Size(76f, 90f),
                            cornerRadius = CornerRadius(16f)
                        )

                        // Windshield
                        drawRoundRect(
                            color = Color.Cyan,
                            topLeft = Offset(carX - 24f, playerY - 25f),
                            size = Size(48f, 28f),
                            cornerRadius = CornerRadius(8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Control Buttons
            if (isGameOver) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💥 Crashed! Earned ${score + (distanceMeters / 10)} Pts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                score = 0
                                distanceMeters = 0
                                playerLane = 1
                                obstacles.clear()
                                isGameOver = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Try Again")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { if (playerLane > 0) playerLane-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("◀ LEFT", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Button(
                        onClick = { if (playerLane < 2) playerLane++ },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("RIGHT ▶", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
