package com.questkeep.domain.entities

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val campaignId: String = "",
    val content: String = "",
    val type: MessageType = MessageType.NORMAL,
    val isPublic: Boolean = true,
    val targetPlayerId: String? = null,   // for whispers (UID)
    val targetPlayerName: String? = null, // display name of whisper target
    val imageUrl: String? = null,         // for IMAGE type
    val createdAt: Long = System.currentTimeMillis()
)

enum class MessageType {
    NORMAL,
    SYSTEM,
    WHISPER,
    DICE_ROLL,
    IMAGE
}
