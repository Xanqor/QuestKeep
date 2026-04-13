package com.questkeep.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.questkeep.data.local.database.PlayerNpcDao
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.PlayerNpc
import com.questkeep.domain.repository.PlayerNpcRepository
import com.questkeep.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerNpcRepositoryImpl @Inject constructor(
    private val dao: PlayerNpcDao,
    private val firestore: FirebaseFirestore
) : PlayerNpcRepository {

    private fun npcsCollection(campaignId: String) =
        firestore.collection(Constants.COLLECTION_CAMPAIGNS)
            .document(campaignId)
            .collection("player_npcs")

    override fun getNpcs(campaignId: String): Flow<List<PlayerNpc>> = callbackFlow {
        val listener = npcsCollection(campaignId)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { return@addSnapshotListener }
                val npcs = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        PlayerNpc(
                            id = doc.id, campaignId = campaignId,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            hp = (doc.getLong("hp") ?: 0L).toInt(),
                            armor = (doc.getLong("armor") ?: 0L).toInt(),
                            damage = doc.getString("damage") ?: "",
                            tags = (doc.get("tags") as? List<String>) ?: emptyList(),
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(npcs)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveNpc(npc: PlayerNpc): Result<PlayerNpc> {
        return try {
            val id = UUID.randomUUID().toString()
            val newNpc = npc.copy(id = id)
            npcsCollection(npc.campaignId).document(id).set(newNpc.toFirestoreMap()).await()
            dao.insertNpc(newNpc.toEntity())
            Result.success(newNpc)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateNpc(npc: PlayerNpc): Result<Unit> {
        return try {
            npcsCollection(npc.campaignId).document(npc.id).set(npc.toFirestoreMap()).await()
            dao.insertNpc(npc.toEntity())
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteNpc(npcId: String, campaignId: String): Result<Unit> {
        return try {
            npcsCollection(campaignId).document(npcId).delete().await()
            dao.deleteNpc(npcId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun PlayerNpc.toFirestoreMap() = mapOf(
        "name" to name, "description" to description,
        "hp" to hp, "armor" to armor, "damage" to damage,
        "tags" to tags, "createdAt" to createdAt
    )
}
