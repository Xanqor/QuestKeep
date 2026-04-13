package com.questkeep.data.repository

import com.questkeep.data.local.database.SpellDao
import com.questkeep.data.local.entity.toDomain
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.Spell
import com.questkeep.domain.repository.SpellRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpellRepositoryImpl @Inject constructor(
    private val spellDao: SpellDao
) : SpellRepository {

    override fun getSpellsForCharacter(characterId: String): Flow<List<Spell>> =
        spellDao.getSpellsForCharacter(characterId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveSpell(spell: Spell) = spellDao.insertSpell(spell.toEntity())

    override suspend fun deleteSpell(spell: Spell) = spellDao.deleteSpell(spell.toEntity())
}
