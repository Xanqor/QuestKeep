package com.questkeep.data.local.database

import androidx.room.*
import com.questkeep.data.local.entity.GmNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GmNoteDao {
    @Query("SELECT * FROM gm_notes WHERE campaignId = :campaignId ORDER BY createdAt ASC")
    fun getNotesForCampaign(campaignId: String): Flow<List<GmNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: GmNoteEntity)

    @Query("DELETE FROM gm_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)
}
