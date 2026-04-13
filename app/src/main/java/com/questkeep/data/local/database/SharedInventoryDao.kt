package com.questkeep.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.questkeep.data.local.entity.SharedInventoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedInventoryDao {
    @Query("SELECT * FROM shared_inventory_items WHERE campaignId = :campaignId ORDER BY createdAt ASC")
    fun getItems(campaignId: String): Flow<List<SharedInventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SharedInventoryItemEntity)

    @Query("DELETE FROM shared_inventory_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)
}
