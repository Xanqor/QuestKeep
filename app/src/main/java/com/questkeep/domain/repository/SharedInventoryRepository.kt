package com.questkeep.domain.repository

import com.questkeep.domain.entities.SharedInventoryItem
import kotlinx.coroutines.flow.Flow

interface SharedInventoryRepository {
    fun getItems(campaignId: String): Flow<List<SharedInventoryItem>>
    fun getBankCrowns(campaignId: String): Flow<Int>
    suspend fun depositItem(item: SharedInventoryItem): Result<Unit>
    suspend fun withdrawItem(itemId: String, campaignId: String): Result<Unit>
    /** Atomically moves [amount] crowns from player to bank. Fails if player has fewer than [amount]. */
    suspend fun depositCrowns(campaignId: String, userId: String, amount: Int): Result<Unit>
    /** Atomically moves [amount] crowns from bank to player. Fails if bank has fewer than [amount]. */
    suspend fun withdrawCrowns(campaignId: String, userId: String, amount: Int): Result<Unit>
}
