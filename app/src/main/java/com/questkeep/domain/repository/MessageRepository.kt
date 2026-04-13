package com.questkeep.domain.repository

import android.net.Uri
import com.questkeep.domain.entities.ChatMessage
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessages(campaignId: String, userId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(message: ChatMessage): Result<Unit>
    suspend fun sendImageMessage(
        campaignId: String,
        imageUri: Uri,
        senderId: String,
        senderName: String
    ): Result<Unit>
}
