package com.questkeep.data.local.database

import androidx.room.*
import com.questkeep.data.local.entity.PlayerNpcEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerNpcDao {
    @Query("SELECT * FROM player_npcs WHERE campaignId = :campaignId ORDER BY createdAt ASC")
    fun getNpcsForCampaign(campaignId: String): Flow<List<PlayerNpcEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNpc(npc: PlayerNpcEntity)

    @Query("DELETE FROM player_npcs WHERE id = :npcId")
    suspend fun deleteNpc(npcId: String)
}
