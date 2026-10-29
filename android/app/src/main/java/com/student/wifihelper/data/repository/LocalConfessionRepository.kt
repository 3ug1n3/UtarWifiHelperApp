package com.student.wifihelper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.student.wifihelper.data.model.Confession
import com.student.wifihelper.data.model.ConfessionCategory
import com.student.wifihelper.data.model.ConfessionComment
import com.student.wifihelper.data.model.ConfessionPoll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalConfessionRepository(context: Context) : ConfessionRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("utar_confessions_prefs", Context.MODE_PRIVATE)
    private val _confessionsFlow = MutableStateFlow<List<Confession>>(emptyList())

    init {
        loadConfessions()
    }

    override fun getConfessionsFlow(): Flow<List<Confession>> = _confessionsFlow.asStateFlow()

    private fun loadConfessions() {
        val jsonString = prefs.getString(KEY_CONFESSIONS, null)
        if (jsonString.isNullOrEmpty()) {
            val defaultList = createDefaultConfessions()
            saveConfessionsToPrefs(defaultList)
            _confessionsFlow.value = defaultList
        } else {
            try {
                val list = parseJsonToConfessions(jsonString).filterNot { it.id.startsWith("c") && it.id.length <= 3 }
                _confessionsFlow.value = list
                saveConfessionsToPrefs(list)
            } catch (e: Exception) {
                val defaultList = createDefaultConfessions()
                saveConfessionsToPrefs(defaultList)
                _confessionsFlow.value = defaultList
            }
        }
    }

    override suspend fun submitConfession(
        content: String,
        category: ConfessionCategory,
        imageUrl: String?,
        poll: ConfessionPoll?
    ): Result<Unit> {
        val currentList = _confessionsFlow.value.toMutableList()
        val nextNumber = (currentList.maxOfOrNull { it.confessionNumber } ?: 1000) + 1

        val newConfession = Confession(
            id = UUID.randomUUID().toString(),
            confessionNumber = nextNumber,
            content = content.trim(),
            category = category,
            timestamp = System.currentTimeMillis(),
            likesCount = 0,
            isLikedByMe = false,
            reportCount = 0,
            commentsCount = 0,
            imageUrl = imageUrl?.trim()?.ifBlank { null },
            poll = poll
        )

        currentList.add(0, newConfession)
        _confessionsFlow.value = currentList
        saveConfessionsToPrefs(currentList)
        return Result.success(Unit)
    }

    override suspend fun toggleLike(confessionId: String): Result<Unit> {
        val currentList = _confessionsFlow.value.map { item ->
            if (item.id == confessionId) {
                val newLikedState = !item.isLikedByMe
                val newCount = if (newLikedState) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                item.copy(isLikedByMe = newLikedState, likesCount = newCount)
            } else {
                item
            }
        }
        _confessionsFlow.value = currentList
        saveConfessionsToPrefs(currentList)
        return Result.success(Unit)
    }

    override suspend fun toggleBookmark(confessionId: String): Result<Unit> {
        val currentList = _confessionsFlow.value.map { item ->
            if (item.id == confessionId) {
                item.copy(isBookmarkedByMe = !item.isBookmarkedByMe)
            } else {
                item
            }
        }
        _confessionsFlow.value = currentList
        saveConfessionsToPrefs(currentList)
        return Result.success(Unit)
    }

    override suspend fun votePoll(confessionId: String, optionIndex: Int): Result<Unit> {
        val currentList = _confessionsFlow.value.map { item ->
            if (item.id == confessionId && item.poll != null) {
                val poll = item.poll
                val newCounts = poll.voteCounts.toMutableList()
                if (optionIndex in newCounts.indices) {
                    newCounts[optionIndex] = newCounts[optionIndex] + 1
                }
                item.copy(poll = poll.copy(voteCounts = newCounts, userVotedOptionIndex = optionIndex))
            } else {
                item
            }
        }
        _confessionsFlow.value = currentList
        saveConfessionsToPrefs(currentList)
        return Result.success(Unit)
    }

    override suspend fun reportConfession(confessionId: String): Result<Unit> {
        val currentList = _confessionsFlow.value.map { item ->
            if (item.id == confessionId) {
                item.copy(reportCount = item.reportCount + 1)
            } else {
                item
            }
        }.filter { it.reportCount < 5 }

        _confessionsFlow.value = currentList
        saveConfessionsToPrefs(currentList)
        return Result.success(Unit)
    }

    private val commentsStateFlow = MutableStateFlow<Map<String, List<ConfessionComment>>>(emptyMap())

    override fun getCommentsFlow(confessionId: String): Flow<List<ConfessionComment>> {
        return commentsStateFlow.map { mapData -> mapData[confessionId] ?: emptyList() }
    }

    override suspend fun addComment(confessionId: String, content: String): Result<Unit> {
        val currentMap = commentsStateFlow.value.toMutableMap()
        val commentList = (currentMap[confessionId] ?: emptyList()).toMutableList()
        commentList.add(
            ConfessionComment(
                id = UUID.randomUUID().toString(),
                confessionId = confessionId,
                content = content.trim(),
                timestamp = System.currentTimeMillis()
            )
        )
        currentMap[confessionId] = commentList
        commentsStateFlow.value = currentMap

        val updatedConfessions = _confessionsFlow.value.map { item ->
            if (item.id == confessionId) {
                item.copy(commentsCount = item.commentsCount + 1)
            } else {
                item
            }
        }
        _confessionsFlow.value = updatedConfessions
        saveConfessionsToPrefs(updatedConfessions)
        return Result.success(Unit)
    }

    private fun saveConfessionsToPrefs(list: List<Confession>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("confessionNumber", item.confessionNumber)
                put("content", item.content)
                put("category", item.category.name)
                put("timestamp", item.timestamp)
                put("likesCount", item.likesCount)
                put("isLikedByMe", item.isLikedByMe)
                put("reportCount", item.reportCount)
                put("commentsCount", item.commentsCount)
                put("imageUrl", item.imageUrl ?: "")
                put("isBookmarkedByMe", item.isBookmarkedByMe)

                if (item.poll != null) {
                    val pollObj = JSONObject().apply {
                        put("question", item.poll.question)
                        put("options", JSONArray(item.poll.options))
                        put("voteCounts", JSONArray(item.poll.voteCounts))
                        put("userVotedOptionIndex", item.poll.userVotedOptionIndex ?: -1)
                    }
                    put("poll", pollObj)
                }
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_CONFESSIONS, jsonArray.toString()).apply()
    }

    private fun parseJsonToConfessions(jsonString: String): List<Confession> {
        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<Confession>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val categoryStr = obj.optString("category", ConfessionCategory.GENERAL.name)
            val category = try {
                ConfessionCategory.valueOf(categoryStr)
            } catch (e: Exception) {
                ConfessionCategory.GENERAL
            }

            val pollObj = obj.optJSONObject("poll")
            val poll = if (pollObj != null) {
                val question = pollObj.getString("question")
                val optionsArr = pollObj.getJSONArray("options")
                val voteCountsArr = pollObj.getJSONArray("voteCounts")
                val options = mutableListOf<String>()
                val voteCounts = mutableListOf<Int>()
                for (j in 0 until optionsArr.length()) {
                    options.add(optionsArr.getString(j))
                    voteCounts.add(voteCountsArr.getInt(j))
                }
                val userVotedIndex = pollObj.optInt("userVotedOptionIndex", -1).takeIf { it >= 0 }
                ConfessionPoll(question, options, voteCounts, userVotedIndex)
            } else null

            list.add(
                Confession(
                    id = obj.getString("id"),
                    confessionNumber = obj.getInt("confessionNumber"),
                    content = obj.getString("content"),
                    category = category,
                    timestamp = obj.getLong("timestamp"),
                    likesCount = obj.optInt("likesCount", 0),
                    isLikedByMe = obj.optBoolean("isLikedByMe", false),
                    reportCount = obj.optInt("reportCount", 0),
                    commentsCount = obj.optInt("commentsCount", 0),
                    imageUrl = obj.optString("imageUrl").ifBlank { null },
                    isBookmarkedByMe = obj.optBoolean("isBookmarkedByMe", false),
                    poll = poll
                )
            )
        }
        return list
    }

    private fun createDefaultConfessions(): List<Confession> {
        return emptyList()
    }

    companion object {
        private const val KEY_CONFESSIONS = "key_confessions_data"
    }
}
