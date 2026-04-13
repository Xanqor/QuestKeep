package com.questkeep.domain.entities

data class Campaign(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val gameSystem: String = "Custom",
    val maxPlayers: Int = 5,
    val gmId: String = "",
    val coGmIds: List<String> = emptyList(),   // assistant GMs (same rights as GM, don't take player slots)
    val inviteCode: String = "",
    val playerIds: List<String> = emptyList(), // ONLY actual players (NOT GM, NOT co-GMs)
    val allMemberIds: List<String> = emptyList(), // playerIds + gmId + coGmIds — for Firestore query
    val createdAt: Long = System.currentTimeMillis()
)
