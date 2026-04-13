package com.questkeep.domain.repository

import com.questkeep.domain.entities.GmNote
import kotlinx.coroutines.flow.Flow

interface GmNoteRepository {
    fun getNotes(campaignId: String): Flow<List<GmNote>>
    suspend fun saveNote(note: GmNote): Result<GmNote>
    suspend fun deleteNote(noteId: String, campaignId: String): Result<Unit>
    suspend fun updateNote(note: GmNote): Result<Unit>
}
