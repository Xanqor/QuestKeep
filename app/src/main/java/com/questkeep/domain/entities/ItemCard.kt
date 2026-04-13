package com.questkeep.domain.entities

data class ItemCard(
    val id: String = "",
    val campaignId: String = "",
    val name: String = "",
    val description: String = "",
    val rarity: ItemRarity = ItemRarity.COMMON,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ItemRarity(val labelRu: String) {
    COMMON("Мусорное"),
    UNCOMMON("Обычный"),
    RARE("Хорошее"),
    EPIC("Отличное"),
    LEGENDARY("Элитное")
}
