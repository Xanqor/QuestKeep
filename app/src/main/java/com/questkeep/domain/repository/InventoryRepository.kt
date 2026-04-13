package com.questkeep.domain.repository

import com.questkeep.domain.entities.InventoryItem
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun getInventory(characterId: String, campaignId: String): Flow<List<InventoryItem>>
    suspend fun addItem(item: InventoryItem): Result<InventoryItem>
    suspend fun updateItem(item: InventoryItem): Result<Unit>
    suspend fun deleteItem(itemId: String, characterId: String, campaignId: String): Result<Unit>
}
