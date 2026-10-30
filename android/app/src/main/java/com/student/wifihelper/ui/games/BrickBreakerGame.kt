package com.student.wifihelper.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import com.student.wifihelper.ui.theme.StatusGreen
import kotlinx.coroutines.delay
import kotlin.math.abs

data class GameBrick(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    var isDestroyed: Boolean = false
)

@Composable
fun BrickBreakerGame(
    onGameEnd: (score: Int) -> Unit,
    onClose: () -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var isGameOver by remember { mutableStateOf(false) }

    var paddleX by remember { mutableFloatStateOf(400f) }
    val paddleWidth = 160f

    var ballX by remember { mutableFloatStateOf(400f) }
    var ballY by remember { mutableFloatStateOf(800f) }
    var ballVx by remember { mutableFloatStateOf(10f) }
    var ballVy by remember { mutableFloatStateOf(-12f) }

    val bricks = remember { mutableStateListOf<GameBrick>() }

    // Init Bricks
    LaunchedEffect(Unit) {
        val rows = 4
        val cols = 5
        val brickWidth = 140f
        val brickHeight = 45f
        val padding = 12f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val bx = c * (brickWidth + padding) + 30f
                val by = r * (brickHeight + padding) + 120f
                val color = if (r % 2 == 0) ProPurplePrimary else StatusGreen
                bricks.add(GameBrick(bx, by, brickWidth, brickHeight, color))
            }
        }
    }

    // Physics Engine
    LaunchedEffect(isGameOver) {
        while (!isGameOver && lives > 0) {
            ballX += ballVx
            ballY += ballVy

            // Wall bounce
            if (ballX <= 30f || ballX >= 750f) ballVx = -ballVx
            if (ballY <= 100f) ballVy = -ballVy

            // Bottom miss
            if (ballY >= 1100f) {
                lives--
                if (lives <= 0) {
                    isGameOver = true
                    onGameEnd(score)
                } else {
                    ballX = paddleX
                    ballY = 800f
                    ballVy = -12f
                }
            }

            // Paddle bounce
            if (ballY >= 950f && ballY <= 970f && ballX in (paddleX - 80f)..(paddleX + 80f)) {
                ballVy = -abs(ballVy)
                ballVx = (ballX - paddleX) * 0.2f
            }

            // Brick collisions
            bricks.filter { !it.isDestroyed }.forEach { brick ->
                if (ballX >= brick.x && ballX <= (brick.x + brick.width) &&
                    ballY >= brick.y && ballY <= (brick.y + brick.height)
                ) {
                    brick.isDestroyed = true
                    ballVy = -ballVy
                    score += 15
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
                        text = "🧱 Wi-Fi Brick Breaker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Drag paddle to bounce signal ball & smash obstacle bricks!",
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
                        text = "⭐ Points: $score",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ProPurpleOnContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))) {
                    Text(
                        text = "❤️ Lives: $lives",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Game Canvas with Drag Touch Gesture
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            paddleX = (paddleX + dragAmount.x).coerceIn(100f, 700f)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw Bricks
                    bricks.filter { !it.isDestroyed }.forEach { brick ->
                        drawRoundRect(
                            color = brick.color,
                            topLeft = Offset(brick.x, brick.y),
                            size = Size(brick.width, brick.height),
                            cornerRadius = CornerRadius(8f)
                        )
                    }

                    // Draw Paddle
                    drawRoundRect(
                        color = ProPurplePrimary,
                        topLeft = Offset(paddleX - (paddleWidth / 2f), 960f),
                        size = Size(paddleWidth, 24f),
                        cornerRadius = CornerRadius(12f)
                    )

                    // Draw Bouncing Signal Ball
                    if (!isGameOver) {
                        drawCircle(
                            color = StatusGreen,
                            radius = 16f,
                            center = Offset(ballX, ballY)
                        )
                    }
                }
            }

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
                            text = "💥 Game Over! Earned $score Points",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                score = 0
                                lives = 3
                                isGameOver = false
                                ballX = 400f
                                ballY = 800f
                                ballVy = -12f
                                bricks.forEach { it.isDestroyed = false }
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
        }
    }
}
