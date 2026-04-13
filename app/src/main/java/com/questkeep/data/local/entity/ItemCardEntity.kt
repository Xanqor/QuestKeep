package com.questkeep.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.ItemCard
import com.questkeep.domain.entities.ItemRarity

@Entity(tableName = "item_cards")
data class ItemCardEntity(
    @PrimaryKey val id: String,
    val campaignId: String,
    val name: String,
    val description: String,
    val rarity: String,
    val createdAt: Long
) {
    fun toDomain() = ItemCard(
        id = id, campaignId = campaignId,
        name = name, description = description,
        rarity = ItemRarity.entries.find { it.name == rarity } ?: ItemRarity.COMMON,
        createdAt = createdAt
    )
}

fun ItemCard.toEntity() = ItemCardEntity(
    id = id, campaignId = campaignId,
    name = name, description = description,
    rarity = rarity.name, createdAt = createdAt
)
