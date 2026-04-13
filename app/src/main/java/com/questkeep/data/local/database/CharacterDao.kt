package com.questkeep.data.local.database

import androidx.room.*
import com.questkeep.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("SELECT * FROM characters WHERE id = :id")
    fun getCharacterById(id: String): Flow<CharacterEntity?>

    @Query("SELECT * FROM characters WHERE campaignId = :campaignId AND playerId = :playerId LIMIT 1")
    fun getCharacterForPlayer(campaignId: String, playerId: String): Flow<CharacterEntity?>

    @Query("SELECT * FROM characters WHERE campaignId = :campaignId")
    fun getCharactersForCampaign(campaignId: String): Flow<List<CharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity)

    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteCharacter(id: String)
}
