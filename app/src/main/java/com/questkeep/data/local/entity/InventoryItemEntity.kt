package com.questkeep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.InventoryItem

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val campaignId: String,
    val name: String,
    val description: String,
    val quantity: Int,
    @ColumnInfo(defaultValue = "") val itemCardId: String?,
    val createdAt: Long
) {
    fun toDomain() = InventoryItem(
        id = id, characterId = characterId, campaignId = campaignId,
        name = name, description = description, quantity = quantity,
        itemCardId = itemCardId?.ifEmpty { null }, createdAt = createdAt
    )
}

fun InventoryItem.toEntity() = InventoryItemEntity(
    id = id, characterId = characterId, campaignId = campaignId,
    name = name, description = description, quantity = quantity,
    itemCardId = itemCardId, createdAt = createdAt
)
