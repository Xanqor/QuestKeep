package com.questkeep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.ChatMessage
import com.questkeep.domain.entities.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderName: String,
    val campaignId: String,
    val content: String,
    val type: String,
    val isPublic: Boolean,
    val targetPlayerId: String?,
    @ColumnInfo(defaultValue = "") val targetPlayerName: String?,
    @ColumnInfo(defaultValue = "") val imageUrl: String?,
    val createdAt: Long
) {
    fun toDomain() = ChatMessage(
        id = id, senderId = senderId, senderName = senderName,
        campaignId = campaignId, content = content,
        type = MessageType.valueOf(type),
        isPublic = isPublic, targetPlayerId = targetPlayerId,
        targetPlayerName = targetPlayerName,
        imageUrl = imageUrl, createdAt = createdAt
    )
}

fun ChatMessage.toEntity() = MessageEntity(
    id = id, senderId = senderId, senderName = senderName,
    campaignId = campaignId, content = content,
    type = type.name, isPublic = isPublic,
    targetPlayerId = targetPlayerId, targetPlayerName = targetPlayerName,
    imageUrl = imageUrl, createdAt = createdAt
)
