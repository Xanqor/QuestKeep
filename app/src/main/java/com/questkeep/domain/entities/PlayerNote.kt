package com.questkeep.domain.entities

import java.util.UUID

data class PlayerNote(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
