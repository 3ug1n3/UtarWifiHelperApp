package com.student.wifihelper.ui.games

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary
import com.student.wifihelper.ui.theme.StatusGreen

@Composable
fun GameCenterScreen(
    userPoints: Int,
    onAddPoints: (points: Int) -> Unit,
    onRedeemOffer: (offer: AutoConnectRedemptionOffer) -> Unit
) {
    val context = LocalContext.current
    var activeGame by remember { mutableStateOf<MiniGameType?>(null) }
    var showCreatePkDialog by remember { mutableStateOf(false) }
    var showJoinPkDialog by remember { mutableStateOf(false) }
    var pkRoomCodeInput by remember { mutableStateOf("") }
    var generatedRoomCode by remember { mutableStateOf("1024") }

    val rewardedAdUnitId = "ca-app-pub-9942249708634262/1721313103"
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }

    LaunchedEffect(Unit) {
        try {
            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(
                context,
                rewardedAdUnitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                    }
                }
            )
        } catch (e: Exception) {}
    }

    val pointsRepo = remember { UserPointsRepository(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeGame != null) {
            when (activeGame) {
                MiniGameType.ROUTER_THROW -> RouterThrowGame(
                    onGameEnd = { earned ->
                        onAddPoints(earned)
                        val oldBest = pointsRepo.getHighScore(MiniGameType.ROUTER_THROW)
                        val newBest = pointsRepo.updateHighScore(MiniGameType.ROUTER_THROW, earned)
                        if (earned > oldBest && oldBest > 0) {
                            Toast.makeText(context, "🏆 NEW RECORD! $newBest Pts!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "🎉 +$earned Points Earned!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClose = { activeGame = null }
                )
                MiniGameType.TURBO_RACER -> TurboRacerGame(
                    onGameEnd = { earned ->
                        onAddPoints(earned)
                        val oldBest = pointsRepo.getHighScore(MiniGameType.TURBO_RACER)
                        val newBest = pointsRepo.updateHighScore(MiniGameType.TURBO_RACER, earned)
                        if (earned > oldBest && oldBest > 0) {
                            Toast.makeText(context, "🏆 NEW RECORD! $newBest Pts!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "🎉 +$earned Points Earned!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClose = { activeGame = null }
                )
                MiniGameType.BRICK_BREAKER -> BrickBreakerGame(
                    onGameEnd = { earned ->
                        onAddPoints(earned)
                        val oldBest = pointsRepo.getHighScore(MiniGameType.BRICK_BREAKER)
                        val newBest = pointsRepo.updateHighScore(MiniGameType.BRICK_BREAKER, earned)
                        if (earned > oldBest && oldBest > 0) {
                            Toast.makeText(context, "🏆 NEW RECORD! $newBest Pts!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "🎉 +$earned Points Earned!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClose = { activeGame = null }
                )
                MiniGameType.FLAPPY_DUCK -> FlappyDuckGame(
                    onGameEnd = { earned ->
                        onAddPoints(earned)
                        val oldBest = pointsRepo.getHighScore(MiniGameType.FLAPPY_DUCK)
                        val newBest = pointsRepo.updateHighScore(MiniGameType.FLAPPY_DUCK, earned)
                        if (earned > oldBest && oldBest > 0) {
                            Toast.makeText(context, "🏆 NEW RECORD! $newBest Pts!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "🎉 +$earned Points Earned!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClose = { activeGame = null }
                )
                MiniGameType.TARGET_SHOOTER -> TargetShooterGame(
                    onGameEnd = { earned ->
                        onAddPoints(earned)
                        pointsRepo.updateHighScore(MiniGameType.TARGET_SHOOTER, earned)
                        Toast.makeText(context, "🎉 +$earned Points Earned!", Toast.LENGTH_SHORT).show()
                    },
                    onClose = { activeGame = null }
                )
                MiniGameType.MEMORY_MATCH -> MemoryMatchGame(
                    onGameEnd = { earned ->
                        onAddPoints(earned)
                        pointsRepo.updateHighScore(MiniGameType.MEMORY_MATCH, earned)
                        Toast.makeText(context, "🎉 +$earned Points Earned!", Toast.LENGTH_SHORT).show()
                    },
                    onClose = { activeGame = null }
                )
                else -> {}
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header & Points Summary Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = ProPurpleContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = ProPurplePrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "⭐ $userPoints Points",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = ProPurpleOnContainer
                                )
                                Text(
                                    text = "Your Auto-Connect Balance",
                                    fontSize = 11.sp,
                                    color = ProPurpleOnContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val activity = context as? android.app.Activity
                                if (rewardedAd != null && activity != null) {
                                    rewardedAd?.show(activity) { _ ->
                                        onAddPoints(50)
                                        Toast.makeText(context, "🎉 +50 Bonus Points Added!", Toast.LENGTH_SHORT).show()
                                    }
                                    rewardedAd = null
                                    try {
                                        RewardedAd.load(
                                            context,
                                            rewardedAdUnitId,
                                            AdRequest.Builder().build(),
                                            object : RewardedAdLoadCallback() {
                                                override fun onAdLoaded(ad: RewardedAd) {
                                                    rewardedAd = ad
                                                }
                                                override fun onAdFailedToLoad(error: LoadAdError) {
                                                    rewardedAd = null
                                                }
                                            }
                                        )
                                    } catch (e: Exception) {}
                                } else {
                                    onAddPoints(50)
                                    Toast.makeText(context, "📺 +50 Bonus Points Added!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+50 Pts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1v1 Friend PK Duel Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, ProPurplePrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚔️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "1v1 Friend PK Duel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Challenge a UTAR friend to a live duel & win +100 Pts!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    generatedRoomCode = kotlin.random.Random.nextInt(1000, 9999).toString()
                                    showCreatePkDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Create Room", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showJoinPkDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ProPurpleContainer, contentColor = ProPurpleOnContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Join Room", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "⚔️ UTAR FRIEND PK CHALLENGE!\n\nI just scored $userPoints Points in UTAR Mini-Games! Can you beat my high score?\n\nPlay & Challenge me on U不停歇WIFI助手 App 🎓"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Challenge Friend via"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Unable to open share menu", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(ProPurplePrimary.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(imageVector = Icons.Outlined.Share, contentDescription = "Share Challenge", tint = ProPurplePrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "🎮 Select Mini-Game",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Clean 2x2 Grid of Mini-Game Cards
                val games = MiniGameType.entries
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (i in games.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left Game Card
                            val game1 = games[i]
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, ProPurplePrimary.copy(alpha = 0.25f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeGame = game1 }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = ProPurpleContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = game1.iconEmoji, fontSize = 20.sp)
                                            }
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = ProPurplePrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = game1.rewardMultiplier,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = game1.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    val best1 = pointsRepo.getHighScore(game1)
                                    if (best1 > 0) {
                                        Text(
                                            text = "🏆 Best: $best1 Pts",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ProPurplePrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { activeGame = game1 },
                                        colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Right Game Card (if exists)
                            if (i + 1 < games.size) {
                                val game2 = games[i + 1]
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, ProPurplePrimary.copy(alpha = 0.25f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeGame = game2 }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = ProPurpleContainer,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(text = game2.iconEmoji, fontSize = 20.sp)
                                                }
                                            }

                                            Surface(
                                                shape = CircleShape,
                                                color = ProPurplePrimary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = game2.rewardMultiplier,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusGreen,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = game2.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        val best2 = pointsRepo.getHighScore(game2)
                                        if (best2 > 0) {
                                            Text(
                                                text = "🏆 Best: $best2 Pts",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ProPurplePrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = { activeGame = game2 },
                                            colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Points Redemption Store Section
                Text(
                    text = "🎁 Auto-Connect Points Store",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    REDEMPTION_OFFERS.forEach { offer ->
                        val canAfford = userPoints >= offer.pointsCost
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (canAfford) ProPurpleContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = canAfford) {
                                    onRedeemOffer(offer)
                                    Toast.makeText(context, "🎉 Redeemed ${offer.title}!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = offer.badgeEmoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${offer.durationHours} Hours",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (canAfford) ProPurpleOnContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "⭐ ${offer.pointsCost} Pts",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = if (canAfford) ProPurplePrimary else Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Create PK Room Dialog (Live Cloud Room)
    if (showCreatePkDialog) {
        val pkRepo = remember { com.student.wifihelper.data.repository.FirebasePkRepository() }

        LaunchedEffect(generatedRoomCode) {
            pkRepo.createPkRoom(generatedRoomCode, MiniGameType.ROUTER_THROW)
        }

        val pkSession by pkRepo.observePkRoom(generatedRoomCode).collectAsState(initial = null)

        LaunchedEffect(pkSession?.isGuestJoined) {
            if (pkSession?.isGuestJoined == true) {
                showCreatePkDialog = false
                activeGame = MiniGameType.ROUTER_THROW
                Toast.makeText(context, "⚔️ Friend Joined Room #$generatedRoomCode! Live Duel Started!", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog(
            onDismissRequest = { showCreatePkDialog = false },
            title = { Text("⚔️ 1v1 Friend PK Room Created") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your PK Room Code is:", fontSize = 13.sp)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ProPurpleContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                            Text(text = "Room #$generatedRoomCode", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = ProPurplePrimary)
                        }
                    }
                    Text("⏳ Waiting for friend to enter code & join live duel...", fontSize = 12.sp, color = ProPurplePrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "⚔️ UTAR FRIEND 1v1 PK DUEL!\n\nJoin my live PK Room #$generatedRoomCode on U不停歇WIFI助手 App!\n\nWinner gets +100 Bonus Auto-Connect Points! 🎓"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Room Code via"))
                        } catch (e: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary)
                ) {
                    Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share on WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Join PK Room Dialog
    if (showJoinPkDialog) {
        val pkRepo = remember { com.student.wifihelper.data.repository.FirebasePkRepository() }
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showJoinPkDialog = false },
            title = { Text("⚔️ Join Friend PK Room") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your friend's 4-digit Room Code:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = pkRoomCodeInput,
                        onValueChange = { pkRoomCodeInput = it },
                        placeholder = { Text("e.g. 1024") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = pkRoomCodeInput.trim()
                        if (code.isNotBlank()) {
                            coroutineScope.launch {
                                val result = pkRepo.joinPkRoom(code)
                                if (result.isSuccess) {
                                    showJoinPkDialog = false
                                    activeGame = MiniGameType.ROUTER_THROW
                                    Toast.makeText(context, "⚔️ Joined PK Room #$code! Live Duel Started!", Toast.LENGTH_SHORT).show()
                                    pkRoomCodeInput = ""
                                } else {
                                    Toast.makeText(context, "Room #$code not found or expired. Please check code!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = pkRoomCodeInput.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary)
                ) {
                    Text("Join & Play Live Duel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinPkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
