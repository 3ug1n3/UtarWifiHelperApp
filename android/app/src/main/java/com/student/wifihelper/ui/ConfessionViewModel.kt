package com.student.wifihelper.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student.wifihelper.UtarApplication
import com.student.wifihelper.data.model.Confession
import com.student.wifihelper.data.model.ConfessionCategory
import com.student.wifihelper.data.model.ConfessionComment
import com.student.wifihelper.data.model.ConfessionPoll
import com.student.wifihelper.data.repository.ConfessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ConfessionSortOrder(val displayName: String, val iconEmoji: String) {
    LATEST("Latest", "🕒"),
    TOP_LIKED("Top Liked", "🔥")
}

enum class ConfessionViewMode {
    LIST,
    GRID
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConfessionViewModel(application: Application) : AndroidViewModel(application) {

    private val confessionRepository: ConfessionRepository = (application as UtarApplication).confessionRepository

    private val _selectedCategory = MutableStateFlow(ConfessionCategory.ALL)
    val selectedCategory: StateFlow<ConfessionCategory> = _selectedCategory.asStateFlow()

    private val _sortOrder = MutableStateFlow(ConfessionSortOrder.LATEST)
    val sortOrder: StateFlow<ConfessionSortOrder> = _sortOrder.asStateFlow()

    private val _viewMode = MutableStateFlow(ConfessionViewMode.LIST)
    val viewMode: StateFlow<ConfessionViewMode> = _viewMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isCreateDialogOpen = MutableStateFlow(false)
    val isCreateDialogOpen: StateFlow<Boolean> = _isCreateDialogOpen.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Comments Sheet State
    private val _selectedConfessionForComments = MutableStateFlow<Confession?>(null)
    val selectedConfessionForComments: StateFlow<Confession?> = _selectedConfessionForComments.asStateFlow()

    val commentsForSelectedConfession: StateFlow<List<ConfessionComment>> = _selectedConfessionForComments
        .flatMapLatest { confession ->
            if (confession != null) {
                confessionRepository.getCommentsFlow(confession.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var lastPostTimestamp: Long = 0L

    val confessions: StateFlow<List<Confession>> = combine(
        confessionRepository.getConfessionsFlow(),
        _selectedCategory,
        _searchQuery,
        _sortOrder
    ) { list, category, query, sort ->
        val filtered = list.filter { item ->
            val matchesCategory = when (category) {
                ConfessionCategory.ALL -> true
                ConfessionCategory.SAVED -> item.isBookmarkedByMe
                else -> item.category == category
            }
            val matchesQuery = query.isBlank() ||
                    item.content.contains(query, ignoreCase = true) ||
                    "#${item.confessionNumber}".contains(query) ||
                    item.category.displayName.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        when (sort) {
            ConfessionSortOrder.LATEST -> filtered.sortedByDescending { it.timestamp }
            ConfessionSortOrder.TOP_LIKED -> filtered.sortedByDescending { it.likesCount }
        }
    }.catch { emit(emptyList()) }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectCategory(category: ConfessionCategory) {
        _selectedCategory.value = category
    }

    fun setSortOrder(sort: ConfessionSortOrder) {
        _sortOrder.value = sort
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ConfessionViewMode.LIST) ConfessionViewMode.GRID else ConfessionViewMode.LIST
    }

    fun setViewMode(mode: ConfessionViewMode) {
        _viewMode.value = mode
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openCreateDialog() {
        _isCreateDialogOpen.value = true
    }

    fun closeCreateDialog() {
        _isCreateDialogOpen.value = false
    }

    fun openCommentsSheet(confession: Confession) {
        _selectedConfessionForComments.value = confession
    }

    fun closeCommentsSheet() {
        _selectedConfessionForComments.value = null
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun submitConfession(
        content: String,
        category: ConfessionCategory,
        imageUrl: String? = null,
        poll: ConfessionPoll? = null
    ) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            _toastMessage.value = "Confession content cannot be empty."
            return
        }

        val now = System.currentTimeMillis()
        val timeSinceLastPost = now - lastPostTimestamp
        if (timeSinceLastPost < 60_000) {
            val secondsRemaining = ((60_000 - timeSinceLastPost) / 1000).coerceAtLeast(1)
            _toastMessage.value = "⏱️ Anti-spam timer: Please wait $secondsRemaining seconds before posting again."
            return
        }

        if (containsProfanity(trimmed)) {
            _toastMessage.value = "⚠️ Post contains inappropriate or abusive words. Please keep confessions respectful!"
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            val result = confessionRepository.submitConfession(trimmed, category, imageUrl, poll)
            _isSubmitting.value = false
            if (result.isSuccess) {
                lastPostTimestamp = System.currentTimeMillis()
                _toastMessage.value = "Confession submitted successfully!"
                closeCreateDialog()
            } else {
                _toastMessage.value = "Failed to submit confession. Please try again."
            }
        }
    }

    fun submitComment(content: String) {
        val confession = _selectedConfessionForComments.value ?: return
        val trimmed = content.trim()
        if (trimmed.isBlank()) return

        if (containsProfanity(trimmed)) {
            _toastMessage.value = "⚠️ Comment contains inappropriate words. Please keep replies respectful!"
            return
        }

        viewModelScope.launch {
            confessionRepository.addComment(confession.id, trimmed)
        }
    }

    fun toggleLike(confessionId: String) {
        viewModelScope.launch {
            confessionRepository.toggleLike(confessionId)
        }
    }

    fun toggleBookmark(confessionId: String) {
        viewModelScope.launch {
            confessionRepository.toggleBookmark(confessionId)
            _toastMessage.value = "Bookmark updated!"
        }
    }

    fun votePoll(confessionId: String, optionIndex: Int) {
        viewModelScope.launch {
            confessionRepository.votePoll(confessionId, optionIndex)
            _toastMessage.value = "Vote recorded!"
        }
    }

    fun reportConfession(confessionId: String) {
        viewModelScope.launch {
            confessionRepository.reportConfession(confessionId)
            _toastMessage.value = "Thank you. Post reported and sent for review."
        }
    }

    private fun containsProfanity(text: String): Boolean {
        val lower = text.lowercase()
        val blockedList = listOf("fuck", "bitch", "bastard", "idiot", "stupid", "lanjiao", "ccb", "pukimak", "sohai", "pokkai")
        return blockedList.any { lower.contains(it) }
    }
}
