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

data class TrafficObstacle(
    val lane: Int, // 0, 1, 2
    var yPos: Float,
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

    val obstacles = remember { mutableStateListOf<TrafficObstacle>() }
    var roadOffsetY by remember { mutableFloatStateOf(0f) }

    // Game loop
    LaunchedEffect(isGameOver) {
        var frameCounter = 0
        while (!isGameOver) {
            frameCounter++
            distanceMeters += 1
            roadOffsetY = (roadOffsetY + 15f) % 60f

            // Spawn obstacles
            if (frameCounter % 35 == 0) {
                val spawnLane = kotlin.random.Random.nextInt(3)
                val isData = kotlin.random.Random.nextBoolean()
                obstacles.add(TrafficObstacle(lane = spawnLane, yPos = -100f, isDataPacket = isData))
            }

            // Move obstacles down
            val iterator = obstacles.iterator()
            while (iterator.hasNext()) {
                val obs = iterator.next()
                obs.yPos += 18f

                // Collision check (Y position range around 80% screen height)
                if (obs.yPos in 650f..820f && obs.lane == playerLane) {
                    if (obs.isDataPacket) {
                        score += 25
                        iterator.remove()
                    } else {
                        isGameOver = true
                        onGameEnd(score + (distanceMeters / 10))
                    }
                } else if (obs.yPos > 1200f) {
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
            // Header Bar
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

            // Score Dashboard Bar
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

            // Highway Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val roadWidth = size.width
                    val laneWidth = roadWidth / 3f
                    val playerY = size.height * 0.78f

                    // Draw Asphalt Road
                    drawRect(color = Color(0xFF2B2930), size = size)

                    // Draw Lane Dividers
                    val dashHeight = 30f
                    val dashGap = 30f
                    var y = -60f + roadOffsetY
                    while (y < size.height) {
                        drawLine(
                            color = Color.Yellow.copy(alpha = 0.8f),
                            start = Offset(laneWidth, y),
                            end = Offset(laneWidth, y + dashHeight),
                            strokeWidth = 6f
                        )
                        drawLine(
                            color = Color.Yellow.copy(alpha = 0.8f),
                            start = Offset(laneWidth * 2f, y),
                            end = Offset(laneWidth * 2f, y + dashHeight),
                            strokeWidth = 6f
                        )
                        y += dashHeight + dashGap
                    }

                    // Draw Obstacles & Data Packets
                    obstacles.forEach { obs ->
                        val obsX = (obs.lane * laneWidth) + (laneWidth / 2f)
                        if (obs.isDataPacket) {
                            drawCircle(
                                color = StatusGreen,
                                radius = 22f,
                                center = Offset(obsX, obs.yPos)
                            )
                        } else {
                            drawRoundRect(
                                color = Color(0xFFE91E63),
                                topLeft = Offset(obsX - 30f, obs.yPos - 40f),
                                size = Size(60f, 80f),
                                cornerRadius = CornerRadius(12f)
                            )
                        }
                    }

                    // Draw Player Car
                    if (!isGameOver) {
                        val playerX = (playerLane * laneWidth) + (laneWidth / 2f)
                        drawRoundRect(
                            color = ProPurplePrimary,
                            topLeft = Offset(playerX - 35f, playerY - 45f),
                            size = Size(70f, 90f),
                            cornerRadius = CornerRadius(16f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 12f,
                            center = Offset(playerX, playerY - 20f)
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
