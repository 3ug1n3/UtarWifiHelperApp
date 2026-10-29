package com.student.wifihelper.data.repository

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.student.wifihelper.UtarApplication
import com.student.wifihelper.data.model.Confession
import com.student.wifihelper.data.model.ConfessionCategory
import com.student.wifihelper.data.model.ConfessionComment
import com.student.wifihelper.data.model.ConfessionPoll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseConfessionRepository : ConfessionRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val prefs by lazy {
        UtarApplication.instance.getSharedPreferences("bookmarked_confessions_prefs", Context.MODE_PRIVATE)
    }

    private fun getBookmarkedIds(): MutableSet<String> {
        return prefs.getStringSet("key_bookmarks", emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveBookmarkedIds(set: Set<String>) {
        prefs.edit().putStringSet("key_bookmarks", set).apply()
    }

    override fun getConfessionsFlow(): Flow<List<Confession>> = callbackFlow {
        val listener = firestore.collection("confessions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val bookmarks = getBookmarkedIds()
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val confessionNumber = doc.getLong("confessionNumber")?.toInt() ?: 1000
                            val content = doc.getString("content") ?: ""
                            val categoryStr = doc.getString("category") ?: ConfessionCategory.GENERAL.name
                            val category = try {
                                ConfessionCategory.valueOf(categoryStr)
                            } catch (e: Exception) {
                                ConfessionCategory.GENERAL
                            }
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
                            val reportCount = doc.getLong("reportCount")?.toInt() ?: 0
                            val commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0
                            val imageUrl = doc.getString("imageUrl")

                            val pollMap = doc.get("poll") as? Map<*, *>
                            val poll = if (pollMap != null) {
                                val question = pollMap["question"] as? String ?: ""
                                val options = (pollMap["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                val voteCounts = (pollMap["voteCounts"] as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
                                val votedIndex = prefs.getInt("key_voted_$id", -1).takeIf { it >= 0 }
                                ConfessionPoll(question, options, voteCounts, votedIndex)
                            } else null

                            Confession(
                                id = id,
                                confessionNumber = confessionNumber,
                                content = content,
                                category = category,
                                timestamp = timestamp,
                                likesCount = likesCount,
                                isLikedByMe = false,
                                reportCount = reportCount,
                                commentsCount = commentsCount,
                                imageUrl = imageUrl,
                                isBookmarkedByMe = bookmarks.contains(id),
                                poll = poll
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.filter { it.reportCount < 5 }

                    trySend(list)
                }
            }

        awaitClose { listener.remove() }
    }

    private suspend fun uploadImageToStorage(imageUriString: String): String? {
        return try {
            if (imageUriString.startsWith("http://") || imageUriString.startsWith("https://")) {
                return imageUriString
            }
            val uri = android.net.Uri.parse(imageUriString)
            val storageRef = Firebase.storage.reference.child("confessions/${UUID.randomUUID()}.jpg")
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()
            downloadUrl
        } catch (e: Exception) {
            imageUriString
        }
    }

    override suspend fun submitConfession(
        content: String,
        category: ConfessionCategory,
        imageUrl: String?,
        poll: ConfessionPoll?
    ): Result<Unit> {
        return try {
            val snapshot = firestore.collection("confessions")
                .orderBy("confessionNumber", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val lastNumber = snapshot.documents.firstOrNull()?.getLong("confessionNumber")?.toInt() ?: 1000
            val nextNumber = lastNumber + 1
            val docId = UUID.randomUUID().toString()

            val data = mutableMapOf<String, Any>(
                "id" to docId,
                "confessionNumber" to nextNumber,
                "content" to content.trim(),
                "category" to category.name,
                "timestamp" to System.currentTimeMillis(),
                "likesCount" to 0,
                "reportCount" to 0
            )

            if (!imageUrl.isNullOrBlank()) {
                val uploadedUrl = uploadImageToStorage(imageUrl.trim())
                if (!uploadedUrl.isNullOrBlank()) {
                    data["imageUrl"] = uploadedUrl
                }
            }

            if (poll != null) {
                data["poll"] = mapOf(
                    "question" to poll.question,
                    "options" to poll.options,
                    "voteCounts" to poll.voteCounts
                )
            }

            firestore.collection("confessions").document(docId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleLike(confessionId: String): Result<Unit> {
        return try {
            firestore.collection("confessions").document(confessionId)
                .update("likesCount", FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleBookmark(confessionId: String): Result<Unit> {
        val current = getBookmarkedIds()
        if (current.contains(confessionId)) {
            current.remove(confessionId)
        } else {
            current.add(confessionId)
        }
        saveBookmarkedIds(current)
        return Result.success(Unit)
    }

    override suspend fun votePoll(confessionId: String, optionIndex: Int): Result<Unit> {
        return try {
            val docRef = firestore.collection("confessions").document(confessionId)
            firestore.runTransaction { transaction ->
                val doc = transaction.get(docRef)
                val pollMap = doc.get("poll") as? Map<*, *>
                if (pollMap != null) {
                    val question = pollMap["question"] as? String ?: ""
                    val options = (pollMap["options"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val voteCounts = (pollMap["voteCounts"] as? List<*>)?.mapNotNull { (it as? Long)?.toInt() }?.toMutableList() ?: mutableListOf()

                    if (optionIndex in voteCounts.indices) {
                        voteCounts[optionIndex] = voteCounts[optionIndex] + 1
                        val updatedPoll = mapOf(
                            "question" to question,
                            "options" to options,
                            "voteCounts" to voteCounts
                        )
                        transaction.update(docRef, "poll", updatedPoll)
                    }
                }
            }.await()

            prefs.edit().putInt("key_voted_$confessionId", optionIndex).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reportConfession(confessionId: String): Result<Unit> {
        return try {
            firestore.collection("confessions").document(confessionId)
                .update("reportCount", FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCommentsFlow(confessionId: String): Flow<List<ConfessionComment>> = callbackFlow {
        val listener = firestore.collection("confessions").document(confessionId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: doc.id
                        val content = doc.getString("content") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        ConfessionComment(
                            id = id,
                            confessionId = confessionId,
                            content = content,
                            timestamp = timestamp
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addComment(confessionId: String, content: String): Result<Unit> {
        return try {
            val commentId = UUID.randomUUID().toString()
            val commentData = mapOf(
                "id" to commentId,
                "confessionId" to confessionId,
                "content" to content.trim(),
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("confessions").document(confessionId)
                .collection("comments").document(commentId).set(commentData).await()

            firestore.collection("confessions").document(confessionId)
                .update("commentsCount", FieldValue.increment(1)).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
