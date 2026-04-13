package com.questkeep.domain.repository

import com.questkeep.domain.entities.Character
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun getCharacter(campaignId: String, playerId: String): Flow<Character?>
    fun getCharactersForCampaign(campaignId: String): Flow<List<Character>>
    suspend fun saveCharacter(character: Character): Result<Unit>
    suspend fun updateCharacter(character: Character): Result<Unit>
    suspend fun deleteCharacter(id: String): Result<Unit>
}
