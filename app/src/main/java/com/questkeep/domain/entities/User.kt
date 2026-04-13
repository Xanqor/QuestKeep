package com.questkeep.domain.entities

data class User(
    val id: String = "",
    val email: String = "",
    val nickname: String = "",
    val avatarEmoji: String = "🧙",
    val avatarUrl: String? = null,     // photo avatar URL (Imgbb); null = use emoji
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
