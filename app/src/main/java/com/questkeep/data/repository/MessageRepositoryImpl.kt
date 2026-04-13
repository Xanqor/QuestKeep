package com.questkeep.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.questkeep.data.local.database.MessageDao
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.ChatMessage
import com.questkeep.domain.entities.MessageType
import com.questkeep.domain.repository.ImageRepository
import com.questkeep.domain.repository.MessageRepository
import com.questkeep.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val dao: MessageDao,
    private val firestore: FirebaseFirestore,
    private val imageRepository: ImageRepository
) : MessageRepository {

    // Uses subcollection: campaigns/{campaignId}/messages
    // Single-field orderBy on a subcollection does NOT require a composite index
    override fun getMessages(campaignId: String, userId: String): Flow<List<ChatMessage>> =
        callbackFlow {
            val listener = firestore
                .collection(Constants.COLLECTION_CAMPAIGNS)
                .document(campaignId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Don't close the flow on Firestore error — just skip this update
                        // so the app doesn't crash while the index or connection recovers
                        return@addSnapshotListener
                    }
                    val messages = snapshot?.documents?.mapNotNull { doc ->
                        val isPublic = doc.getBoolean("isPublic") ?: true
                        val targetId = doc.getString("targetPlayerId")
                        val senderId = doc.getString("senderId") ?: return@mapNotNull null
                        // Filter: public + whispers involving this user
                        if (!isPublic && targetId != userId && senderId != userId) return@mapNotNull null
                        val typeStr = doc.getString("type") ?: "NORMAL"
                        val type = MessageType.entries.find { it.name == typeStr } ?: MessageType.NORMAL
                        ChatMessage(
                            id = doc.id,
                            senderId = senderId,
                            senderName = doc.getString("senderName") ?: "",
                            campaignId = campaignId,
                            content = doc.getString("content") ?: "",
                            type = type,
                            isPublic = isPublic,
                            targetPlayerId = targetId,
                            targetPlayerName = doc.getString("targetPlayerName"),
                            imageUrl = doc.getString("imageUrl"),
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } ?: emptyList()
                    trySend(messages)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            val msg = message.copy(id = id)
            // Write to subcollection: campaigns/{campaignId}/messages/{id}
            firestore
                .collection(Constants.COLLECTION_CAMPAIGNS)
                .document(msg.campaignId)
                .collection("messages")
                .document(id)
                .set(mapOf(
                    "senderId"       to msg.senderId,
                    "senderName"     to msg.senderName,
                    "campaignId"     to msg.campaignId,
                    "content"        to msg.content,
                    "type"           to msg.type.name,
                    "isPublic"       to msg.isPublic,
                    "targetPlayerId"   to msg.targetPlayerId,
                    "targetPlayerName" to msg.targetPlayerName,
                    "imageUrl"         to msg.imageUrl,
                    "createdAt"      to msg.createdAt
                ))
                .await()
            dao.insertMessage(msg.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendImageMessage(
        campaignId: String,
        imageUri: Uri,
        senderId: String,
        senderName: String
    ): Result<Unit> {
        return try {
            // Upload image to Imgbb
            val downloadUrl = imageRepository.uploadImage(imageUri).getOrThrow()

            // Send message with IMAGE type
            val id = UUID.randomUUID().toString()
            val msg = ChatMessage(
                id = id,
                senderId = senderId,
                senderName = senderName,
                campaignId = campaignId,
                content = "",
                type = MessageType.IMAGE,
                isPublic = true,
                imageUrl = downloadUrl
            )
            firestore
                .collection(Constants.COLLECTION_CAMPAIGNS)
                .document(campaignId)
                .collection("messages")
                .document(id)
                .set(mapOf(
                    "senderId"       to msg.senderId,
                    "senderName"     to msg.senderName,
                    "campaignId"     to msg.campaignId,
                    "content"        to msg.content,
                    "type"           to msg.type.name,
                    "isPublic"       to msg.isPublic,
                    "targetPlayerId" to null,
                    "imageUrl"       to downloadUrl,
                    "createdAt"      to msg.createdAt
                ))
                .await()
            dao.insertMessage(msg.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
