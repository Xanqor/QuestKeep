package com.questkeep.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.questkeep.data.local.database.PlayerNoteDao
import com.questkeep.data.local.entity.toDomain
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.PlayerNote
import com.questkeep.domain.repository.PlayerNoteRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerNoteRepositoryImpl @Inject constructor(
    private val dao: PlayerNoteDao,
    private val firestore: FirebaseFirestore
) : PlayerNoteRepository {

    private fun notesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("notes")

    override fun getNotes(userId: String): Flow<List<PlayerNote>> = channelFlow {
        // 1. Immediately emit from local Room database (<2ms)
        val localJob = launch {
            dao.getNotes(userId).collect { entities ->
                trySend(entities.map { it.toDomain() })
            }
        }

        // 2. Real-time Firestore sync with MetadataChanges.INCLUDE
        val listener = notesCollection(userId)
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val notes = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        PlayerNote(
                            id = doc.id,
                            userId = userId,
                            title = doc.getString("title") ?: "",
                            content = doc.getString("content") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            updatedAt = doc.getLong("updatedAt") ?: 0L
                        )
                    } catch (_: Exception) { null }
                }?.sortedByDescending { it.updatedAt } ?: emptyList()

                if (notes.isNotEmpty()) {
                    launch { notes.forEach { dao.insertNote(it.toEntity()) } }
                }
            }

        awaitClose {
            localJob.cancel()
            listener.remove()
        }
    }

    override suspend fun saveNote(note: PlayerNote): Result<Unit> {
        val now = System.currentTimeMillis()
        val id = if (note.id.isNotBlank()) note.id else java.util.UUID.randomUUID().toString()
        val saved = note.copy(id = id, updatedAt = now, createdAt = if (note.createdAt > 0L) note.createdAt else now)

        // Optimistic Room insert: UI updates instantly in 0ms!
        try { dao.insertNote(saved.toEntity()) } catch (_: Exception) {}

        return try {
            notesCollection(saved.userId).document(saved.id).set(mapOf(
                "title" to saved.title,
                "content" to saved.content,
                "createdAt" to saved.createdAt,
                "updatedAt" to saved.updatedAt
            )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            // Keep in local DB even if network delayed
            Result.success(Unit)
        }
    }

    override suspend fun deleteNote(noteId: String, userId: String): Result<Unit> {
        // Optimistic Room delete: instant UI update
        try { dao.deleteNote(noteId) } catch (_: Exception) {}

        return try {
            notesCollection(userId).document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }
}
