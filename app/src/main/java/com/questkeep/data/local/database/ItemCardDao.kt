package com.questkeep.data.local.database

import androidx.room.*
import com.questkeep.data.local.entity.ItemCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemCardDao {
    @Query("SELECT * FROM item_cards WHERE campaignId = :campaignId ORDER BY createdAt ASC")
    fun getItemCards(campaignId: String): Flow<List<ItemCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemCard(card: ItemCardEntity)

    @Query("DELETE FROM item_cards WHERE id = :cardId")
    suspend fun deleteItemCard(cardId: String)
}
