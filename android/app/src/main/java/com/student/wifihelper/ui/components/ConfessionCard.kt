package com.student.wifihelper.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.student.wifihelper.data.model.Confession
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary

@Composable
fun ConfessionCard(
    confession: Confession,
    onLikeToggle: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onVotePoll: (optionIndex: Int) -> Unit = {},
    onOpenComments: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }

    val likeIconColor by animateColorAsState(
        targetValue = if (confession.isLikedByMe) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "likeColor"
    )

    val likeScale by animateFloatAsState(
        targetValue = if (confession.isLikedByMe) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Confession ID & Category Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#UTARConfession #${confession.confessionNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ProPurplePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${formatRelativeTime(confession.timestamp)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category Badge
                Surface(
                    shape = CircleShape,
                    color = ProPurpleContainer,
                    contentColor = ProPurpleOnContainer
                ) {
                    Text(
                        text = "${confession.category.iconEmoji} ${confession.category.displayName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Content
            Text(
                text = confession.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Optional Image Attachment Preview
            if (!confession.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    AsyncImage(
                        model = confession.imageUrl,
                        contentDescription = "Confession Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Interactive Student Poll Component
            confession.poll?.let { poll ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ProPurpleContainer.copy(alpha = 0.25f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📊 ${poll.question}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ProPurplePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val totalVotes = poll.voteCounts.sum().coerceAtLeast(1)

                        poll.options.forEachIndexed { index, optionText ->
                            val votes = poll.voteCounts.getOrElse(index) { 0 }
                            val percentage = (votes * 100) / totalVotes
                            val isVoted = poll.userVotedOptionIndex == index

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onVotePoll(index) },
                                color = if (isVoted) ProPurplePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                border = if (isVoted) BorderStroke(1.dp, ProPurplePrimary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isVoted) "✓ " else ""}$optionText",
                                        fontSize = 12.sp,
                                        fontWeight = if (isVoted) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isVoted) ProPurplePrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$percentage% ($votes)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Row: Like, Comment, Bookmark, Copy, Report
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Like Button
                    IconButton(
                        onClick = onLikeToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (confession.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = likeIconColor,
                            modifier = Modifier.scale(likeScale)
                        )
                    }
                    Text(
                        text = "${confession.likesCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (confession.isLikedByMe) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Comment Counter
                    IconButton(
                        onClick = onOpenComments,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "${confession.commentsCount}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Bookmark / Save button
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (confession.isBookmarkedByMe) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (confession.isBookmarkedByMe) ProPurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Copy text button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("UTAR Confession", confession.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Confession text copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Report button
                    IconButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "Report",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Confession") },
            text = { Text("Are you sure you want to report post #${confession.confessionNumber} for inappropriate or abusive content?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReportDialog = false
                        onReport()
                    }
                ) {
                    Text("Report", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
