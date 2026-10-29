package com.student.wifihelper.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student.wifihelper.data.model.ConfessionCategory
import com.student.wifihelper.ui.ConfessionSortOrder
import com.student.wifihelper.ui.ConfessionViewMode
import com.student.wifihelper.ui.ConfessionViewModel
import com.student.wifihelper.ui.components.ConfessionCard
import com.student.wifihelper.ui.components.ConfessionCommentsSheet
import com.student.wifihelper.ui.components.ConfessionGridItem
import com.student.wifihelper.ui.components.CreateConfessionDialog
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary

@Composable
fun ConfessionScreen(
    viewModel: ConfessionViewModel = viewModel()
) {
    val context = LocalContext.current
    val confessions by viewModel.confessions.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isCreateDialogOpen by viewModel.isCreateDialogOpen.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val selectedConfessionForComments by viewModel.selectedConfessionForComments.collectAsState()
    val commentsForSelectedConfession by viewModel.commentsForSelectedConfession.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Post Confession",
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "Post Confession",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                },
                containerColor = ProPurplePrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar with View Mode Toggle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Search confessions or #1024...", fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProPurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Instagram-style View Mode Toggle Button (List ☰ vs 3-Column Grid ⊞)
                Surface(
                    shape = CircleShape,
                    color = ProPurpleContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (viewMode == ConfessionViewMode.LIST) Icons.Outlined.GridView else Icons.Outlined.ViewStream,
                            contentDescription = "Switch View",
                            tint = ProPurpleOnContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Category & Sort Order Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chips (including #Saved 🔖)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(ConfessionCategory.entries.toTypedArray()) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(category) },
                            label = {
                                Text(
                                    text = "${category.iconEmoji} ${category.displayName}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ProPurpleContainer,
                                selectedLabelColor = ProPurpleOnContainer
                            ),
                            shape = CircleShape
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Feed Sort Order Chips (Latest vs Top Liked)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ConfessionSortOrder.entries.forEach { sort ->
                        val isSelected = sortOrder == sort
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSortOrder(sort) },
                            label = {
                                Text(
                                    text = "${sort.iconEmoji} ${sort.displayName}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ProPurplePrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = CircleShape
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Feed Content
            if (confessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ProPurpleContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Empty",
                                    tint = ProPurplePrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedCategory == ConfessionCategory.SAVED) "No Saved Confessions" else "No Confessions Found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when {
                                selectedCategory == ConfessionCategory.SAVED -> "Tap the bookmark icon on any confession to save it here!"
                                searchQuery.isNotBlank() -> "No results matching '$searchQuery'"
                                else -> "Be the first UTAR student to post a confession in this category!"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                when (viewMode) {
                    ConfessionViewMode.LIST -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = confessions,
                                key = { it.id }
                            ) { confession ->
                                ConfessionCard(
                                    confession = confession,
                                    onLikeToggle = { viewModel.toggleLike(confession.id) },
                                    onBookmarkToggle = { viewModel.toggleBookmark(confession.id) },
                                    onVotePoll = { optionIndex -> viewModel.votePoll(confession.id, optionIndex) },
                                    onOpenComments = { viewModel.openCommentsSheet(confession) },
                                    onReport = { viewModel.reportConfession(confession.id) }
                                )
                            }
                        }
                    }
                    ConfessionViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = confessions,
                                key = { it.id }
                            ) { confession ->
                                ConfessionGridItem(
                                    confession = confession,
                                    onClick = { viewModel.openCommentsSheet(confession) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isCreateDialogOpen) {
        CreateConfessionDialog(
            isSubmitting = isSubmitting,
            onDismiss = { viewModel.closeCreateDialog() },
            onSubmit = { content, category, imageUrl, poll ->
                viewModel.submitConfession(content, category, imageUrl, poll)
            }
        )
    }

    selectedConfessionForComments?.let { confession ->
        ConfessionCommentsSheet(
            confession = confession,
            comments = commentsForSelectedConfession,
            onDismiss = { viewModel.closeCommentsSheet() },
            onSubmitComment = { commentContent ->
                viewModel.submitComment(commentContent)
            }
        )
    }
}
