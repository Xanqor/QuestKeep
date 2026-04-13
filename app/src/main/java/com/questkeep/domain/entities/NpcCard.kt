package com.questkeep.domain.entities

data class NpcCard(
    val id: String = "",
    val campaignId: String = "",
    val name: String = "",
    val description: String = "",
    val hp: Int = 0,
    val armor: Int = 0,
    val damage: String = "",         // e.g. "d6+2" or "8"
    val tags: List<String> = emptyList(), // e.g. ["boss", "undead"]
    val statsJson: String = "{}",    // JSON with extra stats
    val createdAt: Long = System.currentTimeMillis()
)
