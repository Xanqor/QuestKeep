package com.questkeep.domain.entities

enum class NoteType { WORLD, CITY, NPC, PLOT, OTHER }

data class GmNote(
    val id: String = "",
    val campaignId: String = "",
    val parentId: String? = null,   // null = root node
    val title: String = "",
    val content: String = "",       // Markdown text
    val type: NoteType = NoteType.OTHER,
    val isRevealedToPlayers: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
