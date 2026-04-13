package com.questkeep.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.questkeep.domain.entities.SharedInventoryItem

@Entity(tableName = "shared_inventory_items")
data class SharedInventoryItemEntity(
    @PrimaryKey val id: String,
    val campaignId: String,
    val name: String,
    val description: String,
    val quantity: Int,
    val depositedBy: String,
    val createdAt: Long
) {
    fun toDomain() = SharedInventoryItem(
        id = id, campaignId = campaignId, name = name,
        description = description, quantity = quantity,
        depositedBy = depositedBy, createdAt = createdAt
    )
}

fun SharedInventoryItem.toEntity() = SharedInventoryItemEntity(
    id = id, campaignId = campaignId, name = name,
    description = description, quantity = quantity,
    depositedBy = depositedBy, createdAt = createdAt
)
