package com.student.wifihelper.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import com.student.wifihelper.ui.theme.StatusGreen
import kotlinx.coroutines.delay

data class MemoryCardItem(
    val id: Int,
    val iconEmoji: String,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)

@Composable
fun MemoryMatchGame(
    onGameEnd: (score: Int) -> Unit,
    onClose: () -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var isGameOver by remember { mutableStateOf(false) }

    val cardList = remember { mutableStateListOf<MemoryCardItem>() }
    var firstSelectedIndex by remember { mutableStateOf<Int?>(null) }
    var isCheckingMatch by remember { mutableStateOf(false) }

    val levelDifficultyTag = when (level) {
        1 -> "Level 1: Easy Starter (4 Cards) 🌱"
        2 -> "Level 2: Smooth Flow (6 Cards) 🍃"
        3 -> "Level 3: HARD MEMORY GRID! (10 Cards) 🔥"
        4 -> "Level 4: Easy Recovery (6 Cards) ☕"
        5 -> "Level 5: MASTER BRAIN GRID! (12 Cards) ⚡"
        else -> "Level $level: Hard Grid 🔥"
    }

    // Initialize level cards
    fun initLevelCards(lvl: Int) {
        cardList.clear()
        firstSelectedIndex = null
        isCheckingMatch = false

        val pool = listOf("🐤", "💻", "📚", "🚀", "🎓", "🏫", "📶", "☕")
        val pairCount = when (lvl) {
            1 -> 2 // 4 cards
            2 -> 3 // 6 cards
            3 -> 5 // 10 cards (Hard wave)
            4 -> 3 // 6 cards (Recovery wave)
            5 -> 6 // 12 cards (Master wave)
            else -> 4
        }

        val selectedEmojis = pool.shuffled().take(pairCount)
        val combined = (selectedEmojis + selectedEmojis).shuffled()

        combined.forEachIndexed { index, emoji ->
            cardList.add(MemoryCardItem(id = index, iconEmoji = emoji))
        }
    }

    LaunchedEffect(level) {
        initLevelCards(level)
    }

    // Handle card clicks
    fun onCardClicked(index: Int) {
        if (isCheckingMatch || isGameOver) return
        val card = cardList[index]
        if (card.isFaceUp || card.isMatched) return

        card.isFaceUp = true

        if (firstSelectedIndex == null) {
            firstSelectedIndex = index
        } else {
            val firstIdx = firstSelectedIndex!!
            val firstCard = cardList[firstIdx]

            if (firstCard.iconEmoji == card.iconEmoji) {
                // Match!
                firstCard.isMatched = true
                card.isMatched = true
                score += 30
                firstSelectedIndex = null

                // Level clear check
                if (cardList.all { it.isMatched }) {
                    if (level < 5) {
                        level++
                    } else {
                        isGameOver = true
                        onGameEnd(score + 100)
                    }
                }
            } else {
                // Not match - Flip back
                isCheckingMatch = true
            }
        }
    }

    // Flip back non-matching cards
    LaunchedEffect(isCheckingMatch) {
        if (isCheckingMatch) {
            delay(800)
            cardList.forEach { if (!it.isMatched) it.isFaceUp = false }
            firstSelectedIndex = null
            isCheckingMatch = false
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
                        text = "🧠 Campus Memory Match",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = levelDifficultyTag,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (level == 3 || level == 5) Color(0xFFFF5252) else StatusGreen
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

            Spacer(modifier = Modifier.height(12.dp))

            // Score Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = ProPurpleContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⭐ Points Earned: $score",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ProPurpleOnContainer
                    )
                    Text(
                        text = "Level $level / 5",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ProPurplePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Memory Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (cardList.size > 8) 4 else 3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(cardList) { index, card ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                card.isMatched -> StatusGreen.copy(alpha = 0.2f)
                                card.isFaceUp -> ProPurpleContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onCardClicked(index) }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (card.isFaceUp || card.isMatched) {
                                Text(
                                    text = card.iconEmoji,
                                    fontSize = 28.sp
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = ProPurplePrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "❓", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isGameOver) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = ProPurpleContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 Memory Master Cleared! Earned $score Pts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = ProPurpleOnContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                score = 0
                                level = 1
                                isGameOver = false
                                initLevelCards(1)
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
