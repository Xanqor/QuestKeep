package com.questkeep.data.local.database

import androidx.room.*
import com.questkeep.data.local.entity.NpcCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NpcCardDao {
    @Query("SELECT * FROM npc_cards WHERE campaignId = :campaignId ORDER BY createdAt ASC")
    fun getNpcsForCampaign(campaignId: String): Flow<List<NpcCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNpc(npc: NpcCardEntity)

    @Query("DELETE FROM npc_cards WHERE id = :npcId")
    suspend fun deleteNpc(npcId: String)
}
