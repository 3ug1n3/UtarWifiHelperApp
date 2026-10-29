package com.student.wifihelper.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.student.wifihelper.data.model.ConfessionCategory
import com.student.wifihelper.data.model.ConfessionPoll
import com.student.wifihelper.ui.theme.ProPurpleContainer
import com.student.wifihelper.ui.theme.ProPurpleOnContainer
import com.student.wifihelper.ui.theme.ProPurplePrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateConfessionDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (content: String, category: ConfessionCategory, imageUrl: String?, poll: ConfessionPoll?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var textContent by remember { mutableStateOf("") }
    var imageUrlText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCategory by remember { mutableStateOf(ConfessionCategory.CAMPUS) }

    // Native Android System Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            imageUrlText = uri.toString()
        }
    }

    // Dynamic Auto-Expanding Poll states
    var isPollEnabled by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }

    val maxChars = 1000

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✍️",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New UTAR Confession",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips
            Text(
                text = "Select Category",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ConfessionCategory.entries.filter { it != ConfessionCategory.ALL && it != ConfessionCategory.SAVED }.forEach { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = "${category.iconEmoji} ${category.displayName}",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ProPurpleContainer,
                            selectedLabelColor = ProPurpleOnContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confession Input Box
            OutlinedTextField(
                value = textContent,
                onValueChange = { if (it.length <= maxChars) textContent = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = "Write your confession anonymously...\n\n(e.g., To the girl in Kampar Library Block G on Monday...)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProPurplePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Character Counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${textContent.length} / $maxChars",
                    fontSize = 11.sp,
                    color = if (textContent.length >= maxChars) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Photo Album Picker Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProPurpleContainer, contentColor = ProPurpleOnContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Choose Photo",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedImageUri != null) "Photo Selected ✓" else "Choose Photo from Phone Album",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (selectedImageUri != null) {
                    TextButton(onClick = {
                        selectedImageUri = null
                        imageUrlText = ""
                    }) {
                        Text("Remove", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (selectedImageUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Photo Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = imageUrlText,
                    onValueChange = { imageUrlText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Or paste optional Image / Meme Web URL...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProPurplePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Poll Section Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Poll,
                        contentDescription = "Poll",
                        tint = ProPurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attach Student Poll",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Switch(
                    checked = isPollEnabled,
                    onCheckedChange = { isPollEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ProPurplePrimary)
                )
            }

            if (isPollEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ProPurpleContainer.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = pollQuestion,
                            onValueChange = { pollQuestion = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Poll Question (e.g. Is KB aircond too cold?)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Dynamic Auto-Expanding Option Fields
                        pollOptions.forEachIndexed { index, optionValue ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = optionValue,
                                    onValueChange = { newValue ->
                                        val newOptions = pollOptions.toMutableList()
                                        newOptions[index] = newValue
                                        // Auto-expand: if typing in the last option and it's not blank, append a new blank option field
                                        if (index == newOptions.lastIndex && newValue.isNotBlank()) {
                                            newOptions.add("")
                                        }
                                        pollOptions = newOptions
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = {
                                        Text(
                                            text = if (index < 2) "Option ${index + 1}" else "Option ${index + 1} (optional)",
                                            fontSize = 12.sp
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                if (pollOptions.size > 2 && index >= 2) {
                                    IconButton(
                                        onClick = {
                                            val newOptions = pollOptions.toMutableList()
                                            newOptions.removeAt(index)
                                            pollOptions = newOptions
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Option",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Anonymity Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ProPurpleContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Anonymous",
                        tint = ProPurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "100% Anonymous. No student ID, name, email, or IP address is tracked or saved.",
                        fontSize = 12.sp,
                        color = ProPurpleOnContainer,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (textContent.trim().isNotBlank()) {
                            // Filter out any blank/empty options on submit!
                            val validPollOptions = pollOptions.map { it.trim() }.filter { it.isNotBlank() }

                            val pollObj = if (isPollEnabled && pollQuestion.isNotBlank() && validPollOptions.size >= 2) {
                                ConfessionPoll(
                                    question = pollQuestion.trim(),
                                    options = validPollOptions,
                                    voteCounts = List(validPollOptions.size) { 0 }
                                )
                            } else null

                            onSubmit(textContent, selectedCategory, imageUrlText.ifBlank { null }, pollObj)
                        }
                    },
                    enabled = textContent.trim().isNotBlank() && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = ProPurplePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Submitting...")
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Submit",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Post Confession")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
