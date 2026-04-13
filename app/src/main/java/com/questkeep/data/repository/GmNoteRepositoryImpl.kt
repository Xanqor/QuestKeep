package com.questkeep.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.questkeep.data.local.database.GmNoteDao
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.GmNote
import com.questkeep.domain.entities.NoteType
import com.questkeep.domain.repository.GmNoteRepository
import com.questkeep.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GmNoteRepositoryImpl @Inject constructor(
    private val dao: GmNoteDao,
    private val firestore: FirebaseFirestore
) : GmNoteRepository {

    private fun notesCollection(campaignId: String) =
        firestore.collection(Constants.COLLECTION_CAMPAIGNS)
            .document(campaignId)
            .collection("gm_notes")

    override fun getNotes(campaignId: String): Flow<List<GmNote>> = callbackFlow {
        val listener = notesCollection(campaignId)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { return@addSnapshotListener }
                val notes = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        GmNote(
                            id = doc.id,
                            campaignId = campaignId,
                            parentId = doc.getString("parentId"),
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: "",
                            type = NoteType.entries.find { it.name == doc.getString("type") } ?: NoteType.OTHER,
                            isRevealedToPlayers = doc.getBoolean("isRevealedToPlayers") ?: false,
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            updatedAt = doc.getLong("updatedAt") ?: 0L
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(notes)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveNote(note: GmNote): Result<GmNote> {
        return try {
            val id = UUID.randomUUID().toString()
            val newNote = note.copy(id = id)
            notesCollection(note.campaignId).document(id).set(newNote.toFirestoreMap()).await()
            dao.insertNote(newNote.toEntity())
            Result.success(newNote)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteNote(noteId: String, campaignId: String): Result<Unit> {
        return try {
            notesCollection(campaignId).document(noteId).delete().await()
            dao.deleteNote(noteId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateNote(note: GmNote): Result<Unit> {
        return try {
            notesCollection(note.campaignId).document(note.id).set(note.toFirestoreMap()).await()
            dao.insertNote(note.toEntity())
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun GmNote.toFirestoreMap() = mapOf(
        "parentId" to parentId, "title" to title, "content" to content,
        "type" to type.name, "isRevealedToPlayers" to isRevealedToPlayers,
        "createdAt" to createdAt, "updatedAt" to updatedAt
    )
}
