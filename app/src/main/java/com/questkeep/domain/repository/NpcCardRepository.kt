package com.questkeep.domain.repository

import com.questkeep.domain.entities.NpcCard
import kotlinx.coroutines.flow.Flow

interface NpcCardRepository {
    fun getNpcs(campaignId: String): Flow<List<NpcCard>>
    suspend fun saveNpc(npc: NpcCard): Result<NpcCard>
    suspend fun deleteNpc(npcId: String, campaignId: String): Result<Unit>
    suspend fun updateNpc(npc: NpcCard): Result<Unit>
}
