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
import com.student.wifihelper.ui.theme.StatusGreen
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    onReport: (reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }
    var isImageZoomed by remember { mutableStateOf(false) }

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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Confession ID, Timestamp & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "#UTARConfession #${confession.confessionNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ProPurplePrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatRelativeTime(confession.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Category Badge (Guaranteed single line on all screen sizes)
                Surface(
                    shape = CircleShape,
                    color = ProPurpleContainer,
                    contentColor = ProPurpleOnContainer
                ) {
                    Text(
                        text = "${confession.category.iconEmoji} ${confession.category.displayName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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

            // Optional Image Attachment Preview (Tap to Enlarge Full-Screen)
            if (!confession.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { isImageZoomed = true },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ProPurplePrimary.copy(alpha = 0.25f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = confession.imageUrl,
                            contentDescription = "Confession Attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "🔍 Tap to enlarge",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Student Poll Component with Fill Progress Bar
            confession.poll?.let { poll ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = ProPurpleContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, ProPurplePrimary.copy(alpha = 0.3f))
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
                            val percentageFloat = votes.toFloat() / totalVotes.toFloat()
                            val percentageInt = (percentageFloat * 100).toInt()
                            val isVoted = poll.userVotedOptionIndex == index

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onVotePoll(index) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
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
                                        text = "$percentageInt% ($votes)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { percentageFloat },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isVoted) ProPurplePrimary else StatusGreen,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
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

                    // Share Confession Button
                    IconButton(
                        onClick = {
                            try {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "💬 #UTARConfession #${confession.confessionNumber}\n\n\"${confession.content}\"\n\n— Shared from U不停歇WIFI助手 App 🎓"
                                    )
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Confession via"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to open share menu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

        // Full-Screen High-Resolution Photo Lightbox Dialog
        if (isImageZoomed && !confession.imageUrl.isNullOrBlank()) {
            Dialog(
                onDismissRequest = { isImageZoomed = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .clickable { isImageZoomed = false },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = confession.imageUrl,
                        contentDescription = "Enlarged Confession Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    IconButton(
                        onClick = { isImageZoomed = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Photo",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        if (showReportDialog) {
            var reportReason by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                title = { Text("Report Post #${confession.confessionNumber}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Please describe why you are reporting this post:", fontSize = 13.sp)
                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            placeholder = { Text("e.g. Inappropriate content, harassment, fake news...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val text = reportReason.trim()
                            if (text.isNotBlank()) {
                                showReportDialog = false
                                onReport(text)
                            }
                        },
                        enabled = reportReason.trim().isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Submit Report")
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
