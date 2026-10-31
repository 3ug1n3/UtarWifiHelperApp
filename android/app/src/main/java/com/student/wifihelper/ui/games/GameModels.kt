package com.student.wifihelper.ui.games

import android.content.Context
import android.content.SharedPreferences

enum class MiniGameType(
    val title: String,
    val description: String,
    val iconEmoji: String,
    val rewardMultiplier: String
) {
    ROUTER_THROW(
        title = "Router Throw",
        description = "Tap to throw antennas into the spinning Wi-Fi hub. Avoid hitting stuck antennas!",
        iconEmoji = "🎯",
        rewardMultiplier = "+10 Pts / Hit"
    ),
    TURBO_RACER(
        title = "Turbo Wi-Fi Racer",
        description = "Swipe lanes to dodge campus buggies and collect green data packets!",
        iconEmoji = "🏎️",
        rewardMultiplier = "+1 Pt / Meter"
    ),
    BRICK_BREAKER(
        title = "Wi-Fi Brick Breaker",
        description = "Bounce the glowing signal ball to smash 404 & Offline obstacle bricks!",
        iconEmoji = "🧱",
        rewardMultiplier = "+5 Pts / Brick"
    ),
    FLAPPY_DUCK(
        title = "Flappy Lake Duck",
        description = "Tap to flap Kampar Lake Duck between tall Wi-Fi antenna towers!",
        iconEmoji = "🐤",
        rewardMultiplier = "+10 Pts / Tower"
    )
}

data class AutoConnectRedemptionOffer(
    val pointsCost: Int,
    val durationHours: Int,
    val title: String,
    val badgeEmoji: String
)

val REDEMPTION_OFFERS = listOf(
    AutoConnectRedemptionOffer(100, 1, "1 Hour Auto-Connect", "⚡"),
    AutoConnectRedemptionOffer(300, 6, "6 Hours Auto-Connect", "🔥"),
    AutoConnectRedemptionOffer(500, 24, "24 Hours Unlimited Auto-Connect", "🚀")
)

class UserPointsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("utar_game_points_prefs", Context.MODE_PRIVATE)

    fun getPoints(): Int {
        return prefs.getInt("key_user_points", 150)
    }

    fun addPoints(amount: Int): Int {
        val newPoints = (getPoints() + amount).coerceAtLeast(0)
        prefs.edit().putInt("key_user_points", newPoints).apply()
        return newPoints
    }

    fun deductPoints(amount: Int): Boolean {
        val current = getPoints()
        return if (current >= amount) {
            prefs.edit().putInt("key_user_points", current - amount).apply()
            true
        } else {
            false
        }
    }
}
