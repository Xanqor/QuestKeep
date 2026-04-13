package com.questkeep.domain.entities

data class PlayerNpc(
    val id: String = "",
    val campaignId: String = "",
    val name: String = "",
    val description: String = "",
    val hp: Int = 0,
    val armor: Int = 0,
    val damage: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
