package com.student.wifihelper.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.student.wifihelper.data.model.ConfessionCategory
import com.student.wifihelper.ui.games.MiniGameType
import com.student.wifihelper.ui.games.PkRoomSession
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePkRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    fun observePkRoom(roomCode: String): Flow<PkRoomSession?> = callbackFlow {
        val cleanCode = roomCode.replace(Regex("[^0-9]"), "").trim()
        val listener = try {
            firestore.collection("pk_rooms").document(cleanCode)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        trySend(null)
                        return@addSnapshotListener
                    }

                    try {
                        val code = snapshot.getString("roomCode") ?: cleanCode
                        val gameTypeStr = snapshot.getString("gameType") ?: MiniGameType.ROUTER_THROW.name
                        val gameType = try {
                            MiniGameType.valueOf(gameTypeStr)
                        } catch (e: Exception) {
                            MiniGameType.ROUTER_THROW
                        }
                        val hostScore = snapshot.getLong("hostScore")?.toInt() ?: 0
                        val guestScore = snapshot.getLong("guestScore")?.toInt() ?: 0
                        val status = snapshot.getString("status") ?: "WAITING"

                        trySend(
                            PkRoomSession(
                                roomCode = code,
                                gameType = gameType,
                                hostScore = hostScore,
                                guestScore = guestScore,
                                isGuestJoined = status == "PLAYING" || status == "FINISHED",
                                isFinished = status == "FINISHED"
                            )
                        )
                    } catch (e: Exception) {
                        trySend(null)
                    }
                }
        } catch (e: Exception) {
            trySend(null)
            null
        }

        awaitClose { listener?.remove() }
    }

    suspend fun createPkRoom(roomCode: String, gameType: MiniGameType): Result<Unit> {
        val cleanCode = roomCode.replace(Regex("[^0-9]"), "").trim()
        return try {
            val data = mapOf(
                "roomCode" to cleanCode,
                "gameType" to gameType.name,
                "hostScore" to 0,
                "guestScore" to 0,
                "status" to "WAITING",
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("pk_rooms").document(cleanCode).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinPkRoom(roomCode: String): Result<Unit> {
        val cleanCode = roomCode.replace(Regex("[^0-9]"), "").trim()
        if (cleanCode.isEmpty()) return Result.failure(Exception("Invalid Room Code"))

        return try {
            val docRef = firestore.collection("pk_rooms").document(cleanCode)
            val doc = docRef.get().await()
            if (doc.exists()) {
                docRef.update("status", "PLAYING").await()
                Result.success(Unit)
            } else {
                val query = firestore.collection("pk_rooms")
                    .whereEqualTo("roomCode", cleanCode)
                    .get()
                    .await()

                val foundDoc = query.documents.firstOrNull()
                if (foundDoc != null) {
                    foundDoc.reference.update("status", "PLAYING").await()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Room #$cleanCode not found or expired"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePkScore(roomCode: String, isHost: Boolean, score: Int): Result<Unit> {
        return try {
            val field = if (isHost) "hostScore" else "guestScore"
            firestore.collection("pk_rooms").document(roomCode)
                .update(field, score)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun finishPkRoom(roomCode: String): Result<Unit> {
        return try {
            firestore.collection("pk_rooms").document(roomCode)
                .update("status", "FINISHED")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
