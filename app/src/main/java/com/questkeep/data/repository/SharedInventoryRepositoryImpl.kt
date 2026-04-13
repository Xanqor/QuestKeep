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

    override fun getItems(campaignId: String): Flow<List<SharedInventoryItem>> = callbackFlow {
        val listener = bankCollection(campaignId)
            .addSnapshotListener { snapshot, error ->
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
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    override fun getBankCrowns(campaignId: String): Flow<Int> = callbackFlow {
        val listener = campaignRef(campaignId)
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener
                trySend((doc?.getLong("bankCrowns") ?: 0L).toInt())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun depositItem(item: SharedInventoryItem): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            val newItem = item.copy(id = id, createdAt = System.currentTimeMillis())
            bankCollection(item.campaignId).document(id).set(mapOf(
                "name" to newItem.name,
                "description" to newItem.description,
                "quantity" to newItem.quantity,
                "depositedBy" to newItem.depositedBy,
                "createdAt" to newItem.createdAt
            )).await()
            dao.insertItem(newItem.toEntity())
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun withdrawItem(itemId: String, campaignId: String): Result<Unit> {
        return try {
            bankCollection(campaignId).document(itemId).delete().await()
            dao.deleteItem(itemId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun depositCrowns(campaignId: String, userId: String, amount: Int): Result<Unit> {
        return try {
            val charRef = resolveCharacterRef(campaignId, userId)
                ?: return Result.failure(Exception("Персонаж не найден. Создайте персонажа в кампании."))
            val campRef = campaignRef(campaignId)
            firestore.runTransaction { tx ->
                val charDoc = tx.get(charRef)
                val campDoc = tx.get(campRef)
                val playerCrowns = (charDoc.getLong("crowns") ?: 0L).toInt()
                if (playerCrowns < amount) throw Exception("Недостаточно монет (у вас $playerCrowns)")
                val bankCrowns = (campDoc.getLong("bankCrowns") ?: 0L).toInt()
                tx.update(charRef, "crowns", playerCrowns - amount)
                tx.update(campRef, "bankCrowns", bankCrowns + amount)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun withdrawCrowns(campaignId: String, userId: String, amount: Int): Result<Unit> {
        return try {
            val charRef = resolveCharacterRef(campaignId, userId)
                ?: return Result.failure(Exception("Персонаж не найден. Создайте персонажа в кампании."))
            val campRef = campaignRef(campaignId)
            firestore.runTransaction { tx ->
                val charDoc = tx.get(charRef)
                val campDoc = tx.get(campRef)
                val bankCrowns = (campDoc.getLong("bankCrowns") ?: 0L).toInt()
                if (bankCrowns < amount) throw Exception("В банке только $bankCrowns монет")
                val playerCrowns = (charDoc.getLong("crowns") ?: 0L).toInt()
                tx.update(campRef, "bankCrowns", bankCrowns - amount)
                tx.update(charRef, "crowns", playerCrowns + amount)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
