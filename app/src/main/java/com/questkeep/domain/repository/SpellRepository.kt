package com.questkeep.domain.repository

import com.questkeep.domain.entities.Spell
import kotlinx.coroutines.flow.Flow

interface SpellRepository {
    fun getSpellsForCharacter(characterId: String): Flow<List<Spell>>
    suspend fun saveSpell(spell: Spell)
    suspend fun deleteSpell(spell: Spell)
}
