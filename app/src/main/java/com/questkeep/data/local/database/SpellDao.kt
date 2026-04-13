package com.questkeep.data.local.database

import androidx.room.*
import com.questkeep.data.local.entity.SpellEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpellDao {
    @Query("SELECT * FROM spells WHERE characterId = :characterId ORDER BY createdAt ASC")
    fun getSpellsForCharacter(characterId: String): Flow<List<SpellEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpell(spell: SpellEntity)

    @Delete
    suspend fun deleteSpell(spell: SpellEntity)

    @Query("DELETE FROM spells WHERE characterId = :characterId")
    suspend fun deleteAllForCharacter(characterId: String)
}
