package com.questkeep.domain.entities

data class InventoryItem(
    val id: String = "",
    val characterId: String = "",
    val campaignId: String = "",
    val name: String = "",
    val description: String = "",
    val quantity: Int = 1,
    // itemCardId is non-null when the item was added from a GM item card template
    val itemCardId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
