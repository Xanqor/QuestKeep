package com.questkeep.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.questkeep.data.local.database.SharedInventoryDao
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.SharedInventoryItem
import com.questkeep.domain.repository.SharedInventoryRepository
import com.questkeep.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedInventoryRepositoryImpl @Inject constructor(
    private val dao: SharedInventoryDao,
    private val firestore: FirebaseFirestore
) : SharedInventoryRepository {

    private fun bankCollection(campaignId: String) =
        firestore.collection(Constants.COLLECTION_CAMPAIGNS)
            .document(campaignId)
            .collection("shared_inventory")

    private fun campaignRef(campaignId: String) =
        firestore.collection(Constants.COLLECTION_CAMPAIGNS).document(campaignId)

    /** Returns the Firestore DocumentReference for the player's character (queried by playerId). */
    private suspend fun resolveCharacterRef(campaignId: String, userId: String) =
        firestore.collection(Constants.COLLECTION_CHARACTERS)
            .whereEqualTo("campaignId", campaignId)
            .whereEqualTo("playerId", userId)
            .get().await()
            .documents.firstOrNull()?.reference

    override fun getItems(campaignId: String): Flow<List<SharedInventoryItem>> =
        kotlinx.coroutines.flow.channelFlow {
            // 1. Instantly emit cached items from Room
            val localJob = launch {
                dao.getItems(campaignId).collect { entities ->
                    trySend(entities.map { it.toDomain() })
                }
            }

            // 2. Real-time Firestore sync
            val listener = bankCollection(campaignId)
                .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    val items = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            SharedInventoryItem(
                                id = doc.id,
                                campaignId = campaignId,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                quantity = (doc.getLong("quantity") ?: 1L).toInt(),
                                depositedBy = doc.getString("depositedBy") ?: "",
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        } catch (_: Exception) { null }
                    }?.sortedBy { it.createdAt } ?: emptyList()
                    launch { items.forEach { dao.insertItem(it.toEntity()) } }
                }

            awaitClose {
                localJob.cancel()
                listener.remove()
            }
        }

    private fun treasuryVaultRef(campaignId: String) =
        campaignRef(campaignId).collection("treasury").document("vault")

    override fun getBankCrowns(campaignId: String): Flow<Int> = callbackFlow {
        // treasury/vault is the single source of truth. Previously a second
        // listener to campaigns/{id}.bankCrowns could arrive later and replace
        // a valid vault balance in the UI with a stale zero.
        val vaultListener = treasuryVaultRef(campaignId)
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { doc, error ->
                if (error != null) return@addSnapshotListener
                trySend((doc?.getLong("crowns") ?: 0L).toInt())
            }

        awaitClose { vaultListener.remove() }
    }

    override suspend fun depositItem(item: SharedInventoryItem): Result<Unit> {
        val id = if (item.id.isNotBlank()) item.id else UUID.randomUUID().toString()
        val newItem = item.copy(id = id, createdAt = if (item.createdAt > 0L) item.createdAt else System.currentTimeMillis())
        try { dao.insertItem(newItem.toEntity()) } catch (_: Exception) {}
        return try {
            bankCollection(item.campaignId).document(id).set(mapOf(
                "name" to newItem.name,
                "description" to newItem.description,
                "quantity" to newItem.quantity,
                "depositedBy" to newItem.depositedBy,
                "createdAt" to newItem.createdAt
            )).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun withdrawItem(itemId: String, campaignId: String): Result<Unit> {
        try { dao.deleteItem(itemId) } catch (_: Exception) {}
        return try {
            bankCollection(campaignId).document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun depositCrowns(campaignId: String, userId: String, amount: Int): Result<Unit> {
        if (amount <= 0) return Result.failure(Exception("Сумма должна быть больше 0"))
        return try {
            val campRef = campaignRef(campaignId)
            val vaultRef = treasuryVaultRef(campaignId)
            val campDoc = campRef.get().await()
            val gmId = campDoc.getString("gmId")
            @Suppress("UNCHECKED_CAST")
            val coGmIds = (campDoc.get("coGmIds") as? List<String>) ?: emptyList()

            // If user is GM/co-GM, deposit directly without requiring a character
            if (userId == gmId || userId in coGmIds) {
                val vaultDoc = vaultRef.get().await()
                if (vaultDoc.exists()) {
                    vaultRef.update("crowns", com.google.firebase.firestore.FieldValue.increment(amount.toLong())).await()
                } else {
                    val initial = (campDoc.getLong("bankCrowns") ?: 0L).toInt() + amount
                    vaultRef.set(mapOf(
                        "crowns" to initial,
                        "updatedAt" to System.currentTimeMillis(),
                        "lastModifiedBy" to userId
                    )).await()
                }
                try {
                    campRef.update("bankCrowns", com.google.firebase.firestore.FieldValue.increment(amount.toLong())).await()
                } catch (_: Exception) {}
                return Result.success(Unit)
            }

            val charRef = resolveCharacterRef(campaignId, userId)
                ?: return Result.failure(Exception("Персонаж не найден. Создайте персонажа в кампании."))

            firestore.runTransaction { tx ->
                val charDoc = tx.get(charRef)
                val vaultDoc = tx.get(vaultRef)
                val playerCrowns = (charDoc.getLong("crowns") ?: 0L).toInt()
                if (playerCrowns < amount) throw Exception("Недостаточно монет (у вас $playerCrowns)")

                val currentVault = if (vaultDoc.exists()) {
                    (vaultDoc.getLong("crowns") ?: 0L).toInt()
                } else {
                    val liveCampDoc = tx.get(campRef)
                    (liveCampDoc.getLong("bankCrowns") ?: 0L).toInt()
                }

                tx.update(charRef, "crowns", playerCrowns - amount)
                if (vaultDoc.exists()) {
                    tx.update(vaultRef, mapOf(
                        "crowns" to (currentVault + amount),
                        "updatedAt" to System.currentTimeMillis(),
                        "lastModifiedBy" to userId
                    ))
                } else {
                    tx.set(vaultRef, mapOf(
                        "crowns" to (currentVault + amount),
                        "updatedAt" to System.currentTimeMillis(),
                        "lastModifiedBy" to userId
                    ))
                }
            }.await()

            // Best effort sync root camp for GM
            try {
                campRef.update("bankCrowns", com.google.firebase.firestore.FieldValue.increment(amount.toLong()))
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun withdrawCrowns(campaignId: String, userId: String, amount: Int): Result<Unit> {
        if (amount <= 0) return Result.failure(Exception("Сумма должна быть больше 0"))
        return try {
            val campRef = campaignRef(campaignId)
            val vaultRef = treasuryVaultRef(campaignId)
            val campDoc = campRef.get().await()
            val gmId = campDoc.getString("gmId")
            @Suppress("UNCHECKED_CAST")
            val coGmIds = (campDoc.get("coGmIds") as? List<String>) ?: emptyList()

            // If user is GM/co-GM, withdraw directly
            if (userId == gmId || userId in coGmIds) {
                val vaultDoc = vaultRef.get().await()
                val currentVault = if (vaultDoc.exists()) {
                    (vaultDoc.getLong("crowns") ?: 0L).toInt()
                } else {
                    (campDoc.getLong("bankCrowns") ?: 0L).toInt()
                }
                if (currentVault < amount) return Result.failure(Exception("В банке только $currentVault монет"))

                if (vaultDoc.exists()) {
                    vaultRef.update("crowns", com.google.firebase.firestore.FieldValue.increment(-amount.toLong())).await()
                } else {
                    vaultRef.set(mapOf(
                        "crowns" to (currentVault - amount),
                        "updatedAt" to System.currentTimeMillis(),
                        "lastModifiedBy" to userId
                    )).await()
                }
                try {
                    campRef.update("bankCrowns", com.google.firebase.firestore.FieldValue.increment(-amount.toLong())).await()
                } catch (_: Exception) {}
                return Result.success(Unit)
            }

            val charRef = resolveCharacterRef(campaignId, userId)
                ?: return Result.failure(Exception("Персонаж не найден. Создайте персонажа в кампании."))

            firestore.runTransaction { tx ->
                val charDoc = tx.get(charRef)
                val vaultDoc = tx.get(vaultRef)
                val currentVault = if (vaultDoc.exists()) {
                    (vaultDoc.getLong("crowns") ?: 0L).toInt()
                } else {
                    val liveCampDoc = tx.get(campRef)
                    (liveCampDoc.getLong("bankCrowns") ?: 0L).toInt()
                }
                if (currentVault < amount) throw Exception("В банке только $currentVault монет")
                val playerCrowns = (charDoc.getLong("crowns") ?: 0L).toInt()

                if (vaultDoc.exists()) {
                    tx.update(vaultRef, mapOf(
                        "crowns" to (currentVault - amount),
                        "updatedAt" to System.currentTimeMillis(),
                        "lastModifiedBy" to userId
                    ))
                } else {
                    tx.set(vaultRef, mapOf(
                        "crowns" to (currentVault - amount),
                        "updatedAt" to System.currentTimeMillis(),
                        "lastModifiedBy" to userId
                    ))
                }
                tx.update(charRef, "crowns", playerCrowns + amount)
            }.await()

            try {
                campRef.update("bankCrowns", com.google.firebase.firestore.FieldValue.increment(-amount.toLong()))
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
