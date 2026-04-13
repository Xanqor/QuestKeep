package com.questkeep.domain.repository

import com.questkeep.domain.entities.PlayerNote
import kotlinx.coroutines.flow.Flow

interface PlayerNoteRepository {
    fun getNotes(userId: String): Flow<List<PlayerNote>>
    suspend fun saveNote(note: PlayerNote): Result<Unit>
    suspend fun deleteNote(noteId: String, userId: String): Result<Unit>
}
