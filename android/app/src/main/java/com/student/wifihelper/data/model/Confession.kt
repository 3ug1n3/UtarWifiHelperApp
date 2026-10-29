package com.student.wifihelper.data.model

enum class ConfessionCategory(
    val displayName: String,
    val hashtag: String,
    val iconEmoji: String
) {
    ALL("All", "#All", "💬"),
    SAVED("Saved", "#Saved", "🔖"),
    LOVE("Love & Crush", "#Love", "❤️"),
    CAMPUS("Campus Life", "#Campus", "🏫"),
    STUDY("Study & Exam", "#Study", "📚"),
    RANT("Rant & Secret", "#Rant", "📢"),
    LOST_FOUND("Lost & Found", "#LostFound", "🔍"),
    GENERAL("General", "#General", "💡")
}

data class ConfessionPoll(
    val question: String,
    val options: List<String>,
    val voteCounts: List<Int>,
    val userVotedOptionIndex: Int? = null
)

data class Confession(
    val id: String,
    val confessionNumber: Int,
    val content: String,
    val category: ConfessionCategory,
    val timestamp: Long,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val reportCount: Int = 0,
    val commentsCount: Int = 0,
    val imageUrl: String? = null,
    val isBookmarkedByMe: Boolean = false,
    val poll: ConfessionPoll? = null
)
