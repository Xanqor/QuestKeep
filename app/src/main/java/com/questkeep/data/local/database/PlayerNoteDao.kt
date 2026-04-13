package com.questkeep.data.local.database

import androidx.room.*
import com.questkeep.data.local.entity.PlayerNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerNoteDao {
    @Query("SELECT * FROM player_notes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getNotes(userId: String): Flow<List<PlayerNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: PlayerNoteEntity)

    @Query("DELETE FROM player_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)
}
