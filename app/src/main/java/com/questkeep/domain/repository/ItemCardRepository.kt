package com.questkeep.domain.repository

import com.questkeep.domain.entities.ItemCard
import com.questkeep.domain.entities.InventoryItem
import kotlinx.coroutines.flow.Flow

interface ItemCardRepository {
    fun getItemCards(campaignId: String): Flow<List<ItemCard>>
    suspend fun saveItemCard(card: ItemCard): Result<ItemCard>
    suspend fun deleteItemCard(cardId: String, campaignId: String): Result<Unit>
    /** GM gives an item card to a specific player character as an inventory item. */
    suspend fun giveItemToCharacter(
        card: ItemCard,
        characterId: String,
        quantity: Int = 1
    ): Result<InventoryItem>
}
