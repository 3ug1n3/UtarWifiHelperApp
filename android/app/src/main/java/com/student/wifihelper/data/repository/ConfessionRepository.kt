package com.student.wifihelper.data.repository

import com.student.wifihelper.data.model.Confession
import com.student.wifihelper.data.model.ConfessionCategory
import com.student.wifihelper.data.model.ConfessionComment
import com.student.wifihelper.data.model.ConfessionPoll
import kotlinx.coroutines.flow.Flow

interface ConfessionRepository {
    fun getConfessionsFlow(): Flow<List<Confession>>
    suspend fun submitConfession(content: String, category: ConfessionCategory, imageUrl: String? = null, poll: ConfessionPoll? = null): Result<Unit>
    suspend fun toggleLike(confessionId: String): Result<Unit>
    suspend fun toggleBookmark(confessionId: String): Result<Unit>
    suspend fun votePoll(confessionId: String, optionIndex: Int): Result<Unit>
    suspend fun reportConfession(confessionId: String): Result<Unit>

    fun getCommentsFlow(confessionId: String): Flow<List<ConfessionComment>>
    suspend fun addComment(confessionId: String, content: String): Result<Unit>
}
