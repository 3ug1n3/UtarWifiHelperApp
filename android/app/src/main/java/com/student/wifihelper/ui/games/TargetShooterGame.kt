package com.student.wifihelper.ui.games

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import com.student.wifihelper.ui.theme.StatusGreen
import kotlinx.coroutines.delay
import kotlin.math.hypot

data class TargetOrb(
    val id: Long,
    val xProgress: Float, // 0.15f .. 0.85f
    val yProgress: Float, // 0.20f .. 0.75f
    var radius: Float = 50f,
    var shrinkSpeed: Float = 0.8f,
    var isHit: Boolean = false
)

@Composable
fun TargetShooterGame(
    onGameEnd: (score: Int) -> Unit,
    onClose: () -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var lives by remember { mutableIntStateOf(3) }
    var isGameOver by remember { mutableStateOf(false) }

    val targets = remember { mutableStateListOf<TargetOrb>() }

    // Wave Difficulty Curve: Lvl 1 & 2 = Easy ➔ Lvl 3 = Hard ➔ Lvl 4 = Easy ➔ Lvl 5 = Ultra Hard!
    val levelDifficultyTag = when (level) {
        1 -> "Level 1: Easy Start 🌱"
        2 -> "Level 2: Smooth Flow 🍃"
        3 -> "Level 3: HARD SPEED RUSH! 🔥"
        4 -> "Level 4: Easy Recovery ☕"
        5 -> "Level 5: ULTRA HARD FRENZY! ⚡"
        else -> "Level $level: Hard Rush 🔥"
    }

    val targetShrinkRate = when (level) {
        1 -> 0.4f
        2 -> 0.5f
        3 -> 1.2f // Hard speed spike
        4 -> 0.4f // Easy recovery
        5 -> 1.5f // Ultra hard frenzy
        else -> 1.0f
    }

    // Game loop
    LaunchedEffect(isGameOver, level) {
        var frame = 0
        while (!isGameOver && lives > 0) {
            frame++

            // Spawn target orb based on difficulty wave
            val spawnInterval = when (level) {
                3 -> 25 // Fast spawn
                5 -> 18 // Ultra fast spawn
                else -> 40
            }

            if (frame % spawnInterval == 0) {
                targets.add(
                    TargetOrb(
                        id = System.currentTimeMillis() + kotlin.random.Random.nextInt(1000),
                        xProgress = kotlin.random.Random.nextFloat() * 0.7f + 0.15f,
                        yProgress = kotlin.random.Random.nextFloat() * 0.55f + 0.20f,
                        shrinkSpeed = targetShrinkRate
                    )
                )
            }

            // Shrink and update targets
            val iter = targets.iterator()
            while (iter.hasNext()) {
                val t = iter.next()
                if (!t.isHit) {
                    t.radius -= t.shrinkSpeed
                    if (t.radius <= 5f) {
                        iter.remove()
                        lives--
                        if (lives <= 0) {
                            isGameOver = true
                            onGameEnd(score)
                        }
                    }
                } else {
                    iter.remove()
                }
            }

            // Level Progression Check
            if (score >= level * 100) {
                level++
                targets.clear()
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
                        text = "🎯 Signal Target Shooter",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = levelDifficultyTag,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (level == 3 || level >= 5) Color(0xFFFF5252) else StatusGreen
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

            // Canvas Game Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .clickable(!isGameOver) {
                        // Check if tap hit any target
                    }
                ) {
                    targets.filter { !it.isHit }.forEach { target ->
                        val tx = target.xProgress * size.width
                        val ty = target.yProgress * size.height

                        // Outer ring
                        drawCircle(
                            color = StatusGreen.copy(alpha = 0.4f),
                            radius = target.radius + 10f,
                            center = Offset(tx, ty)
                        )
                        // Target orb
                        drawCircle(
                            color = StatusGreen,
                            radius = target.radius,
                            center = Offset(tx, ty)
                        )
                        // Inner bullseye
                        drawCircle(
                            color = Color.White,
                            radius = (target.radius * 0.4f).coerceAtLeast(3f),
                            center = Offset(tx, ty)
                        )
                    }
                }

                // Overlay Touch Receiver
                targets.forEach { target ->
                    if (!target.isHit) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    target.isHit = true
                                    score += 15
                                }
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
                            text = "💥 Game Over! Total Points Earned: $score",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                score = 0
                                level = 1
                                lives = 3
                                targets.clear()
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
        }
    }
}
