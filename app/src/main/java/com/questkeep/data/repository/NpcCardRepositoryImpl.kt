package com.questkeep.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.questkeep.data.local.database.NpcCardDao
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.NpcCard
import com.questkeep.domain.repository.NpcCardRepository
import com.questkeep.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NpcCardRepositoryImpl @Inject constructor(
    private val dao: NpcCardDao,
    private val firestore: FirebaseFirestore
) : NpcCardRepository {

    private fun npcsCollection(campaignId: String) =
        firestore.collection(Constants.COLLECTION_CAMPAIGNS)
            .document(campaignId)
            .collection("npc_cards")

    override fun getNpcs(campaignId: String): Flow<List<NpcCard>> =
        kotlinx.coroutines.flow.channelFlow {
            // 1. Immediately emit from local Room database so UI never hangs in loading
            val localJob = launch {
                dao.getNpcsForCampaign(campaignId).collect { entities ->
                    trySend(entities.map { it.toDomain() })
                }
            }

            // 2. Real-time sync with Firestore (sort in memory to avoid missing index / field errors)
            val listener = npcsCollection(campaignId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    val npcs = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            NpcCard(
                                id = doc.id, campaignId = campaignId,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                hp = (doc.getLong("hp") ?: 0L).toInt(),
                                armor = (doc.getLong("armor") ?: 0L).toInt(),
                                damage = doc.getString("damage") ?: "",
                                tags = (doc.get("tags") as? List<String>) ?: emptyList(),
                                statsJson = doc.getString("statsJson") ?: "{}",
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        } catch (e: Exception) { null }
                    }?.sortedBy { it.createdAt } ?: emptyList()

                    launch {
                        npcs.forEach { dao.insertNpc(it.toEntity()) }
                    }
                }

            awaitClose {
                localJob.cancel()
                listener.remove()
            }
        }

    override suspend fun saveNpc(npc: NpcCard): Result<NpcCard> {
        return try {
            val id = UUID.randomUUID().toString()
            val newNpc = npc.copy(id = id)
            npcsCollection(npc.campaignId).document(id).set(newNpc.toFirestoreMap()).await()
            dao.insertNpc(newNpc.toEntity())
            Result.success(newNpc)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteNpc(npcId: String, campaignId: String): Result<Unit> {
        return try {
            npcsCollection(campaignId).document(npcId).delete().await()
            dao.deleteNpc(npcId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateNpc(npc: NpcCard): Result<Unit> {
        return try {
            npcsCollection(npc.campaignId).document(npc.id).set(npc.toFirestoreMap()).await()
            dao.insertNpc(npc.toEntity())
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun NpcCard.toFirestoreMap() = mapOf(
        "name" to name, "description" to description,
        "hp" to hp, "armor" to armor, "damage" to damage,
        "tags" to tags, "statsJson" to statsJson, "createdAt" to createdAt
    )
}
