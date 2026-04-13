package com.questkeep.data.local.database

import androidx.room.*
import com.questkeep.data.local.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE characterId = :characterId ORDER BY createdAt ASC")
    fun getItemsForCharacter(characterId: String): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("DELETE FROM inventory_items WHERE characterId = :characterId")
    suspend fun deleteAllForCharacter(characterId: String)
}
