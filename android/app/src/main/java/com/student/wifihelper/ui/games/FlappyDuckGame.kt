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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import kotlinx.coroutines.delay

data class TowerPipe(
    var x: Float,
    val topGapY: Float,
    val gapHeight: Float = 280f,
    var passed: Boolean = false
)

@Composable
fun FlappyDuckGame(
    onGameEnd: (score: Int) -> Unit,
    onClose: () -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }

    var duckY by remember { mutableFloatStateOf(500f) }
    var velocity by remember { mutableFloatStateOf(0f) }
    val gravity = 0.85f
    val jumpPower = -14f

    val pipes = remember { mutableStateListOf<TowerPipe>() }
    var cloudOffsetX by remember { mutableFloatStateOf(0f) }

    // Game Loop
    LaunchedEffect(isGameOver) {
        var frame = 0
        while (!isGameOver) {
            frame++
            cloudOffsetX = (cloudOffsetX - 2f) % 800f

            velocity += gravity
            duckY += velocity

            // Ground & Ceiling bounds check
            if (duckY >= 1000f || duckY <= 50f) {
                isGameOver = true
                onGameEnd(score * 10)
            }

            // Spawn pipes
            if (frame % 65 == 0) {
                val gapY = kotlin.random.Random.nextInt(200, 600).toFloat()
                pipes.add(TowerPipe(x = 800f, topGapY = gapY))
            }

            // Move pipes & collision check
            val duckX = 200f

            val iterator = pipes.iterator()
            while (iterator.hasNext()) {
                val pipe = iterator.next()
                pipe.x -= 8.5f

                // Score check
                if (!pipe.passed && pipe.x < duckX) {
                    pipe.passed = true
                    score++
                }

                // Collision check
                if (pipe.x in (duckX - 40f)..(duckX + 40f)) {
                    if (duckY < pipe.topGapY || duckY > (pipe.topGapY + pipe.gapHeight)) {
                        isGameOver = true
                        onGameEnd(score * 10)
                    }
                }

                if (pipe.x < -100f) {
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
                        text = "🐤 Flappy Lake Duck",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Tap screen to flap duck between Wi-Fi towers!",
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
                        text = "⭐ Points: ${score * 10}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ProPurpleOnContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))) {
                    Text(
                        text = "🚩 Towers Passed: $score",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(enabled = !isGameOver) {
                        velocity = jumpPower
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Parallax Moving Cloud
                    val cX = (400f + cloudOffsetX + 800f) % 800f
                    drawCircle(color = Color.White.copy(alpha = 0.15f), radius = 60f, center = Offset(cX, 120f))

                    // Draw Antenna Pipes
                    pipes.forEach { pipe ->
                        // Top Pipe
                        drawRoundRect(
                            color = ProPurplePrimary,
                            topLeft = Offset(pipe.x - 35f, 0f),
                            size = Size(70f, pipe.topGapY),
                            cornerRadius = CornerRadius(12f)
                        )
                        // Bottom Pipe
                        drawRoundRect(
                            color = ProPurplePrimary,
                            topLeft = Offset(pipe.x - 35f, pipe.topGapY + pipe.gapHeight),
                            size = Size(70f, size.height - (pipe.topGapY + pipe.gapHeight)),
                            cornerRadius = CornerRadius(12f)
                        )
                    }

                    // Draw Flappy Duck with Tilt Angle
                    if (!isGameOver) {
                        val duckAngle = (velocity * 3f).coerceIn(-30f, 45f)
                        rotate(degrees = duckAngle, pivot = Offset(200f, duckY)) {
                            drawCircle(
                                color = Color(0xFFFFD54F), // Yellow Body
                                radius = 24f,
                                center = Offset(200f, duckY)
                            )
                            drawCircle(
                                color = Color.Black, // Eye
                                radius = 4f,
                                center = Offset(210f, duckY - 6f)
                            )
                            drawCircle(
                                color = Color(0xFFFF9800), // Beak
                                radius = 8f,
                                center = Offset(222f, duckY + 2f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            text = "💥 Touched Tower! Earned ${score * 10} Points",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                score = 0
                                duckY = 500f
                                velocity = 0f
                                pipes.clear()
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
                            text = "👆 TAP ANYWHERE TO FLAP DUCK 🐤",
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
