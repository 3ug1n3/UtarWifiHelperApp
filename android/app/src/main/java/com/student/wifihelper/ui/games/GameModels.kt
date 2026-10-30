package com.student.wifihelper.ui.games

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
