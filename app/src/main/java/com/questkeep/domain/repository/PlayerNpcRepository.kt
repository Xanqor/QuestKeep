package com.questkeep.domain.repository

import com.questkeep.domain.entities.PlayerNpc
import kotlinx.coroutines.flow.Flow

interface PlayerNpcRepository {
    fun getNpcs(campaignId: String): Flow<List<PlayerNpc>>
    suspend fun saveNpc(npc: PlayerNpc): Result<PlayerNpc>
    suspend fun updateNpc(npc: PlayerNpc): Result<Unit>
    suspend fun deleteNpc(npcId: String, campaignId: String): Result<Unit>
}
