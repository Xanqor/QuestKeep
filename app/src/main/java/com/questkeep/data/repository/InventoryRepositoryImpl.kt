package com.questkeep.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.questkeep.data.local.database.InventoryDao
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.InventoryItem
import com.questkeep.domain.repository.InventoryRepository
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
class InventoryRepositoryImpl @Inject constructor(
    private val dao: InventoryDao,
    private val firestore: FirebaseFirestore
) : InventoryRepository {

    private fun inventoryCollection(campaignId: String) =
        firestore.collection(Constants.COLLECTION_CAMPAIGNS)
            .document(campaignId)
            .collection(Constants.COLLECTION_INVENTORY)

    override fun getInventory(characterId: String, campaignId: String): Flow<List<InventoryItem>> =
        callbackFlow {
            val listener = inventoryCollection(campaignId)
                .whereEqualTo("characterId", characterId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    val items = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            InventoryItem(
                                id = doc.id,
                                characterId = doc.getString("characterId") ?: "",
                                campaignId = campaignId,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                quantity = (doc.getLong("quantity") ?: 1L).toInt(),
                                itemCardId = doc.getString("itemCardId"),
                                createdAt = doc.getLong("createdAt") ?: 0L
                            )
                        } catch (e: Exception) { null }
                    }?.sortedBy { it.createdAt } ?: emptyList()
                    launch { items.forEach { dao.insertItem(it.toEntity()) } }
                    trySend(items)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun addItem(item: InventoryItem): Result<InventoryItem> {
        return try {
            val id = UUID.randomUUID().toString()
            val newItem = item.copy(id = id, createdAt = System.currentTimeMillis())
            inventoryCollection(item.campaignId).document(id).set(newItem.toFirestoreMap()).await()
            dao.insertItem(newItem.toEntity())
            Result.success(newItem)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateItem(item: InventoryItem): Result<Unit> {
        return try {
            inventoryCollection(item.campaignId).document(item.id).set(item.toFirestoreMap()).await()
            dao.insertItem(item.toEntity())
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteItem(itemId: String, characterId: String, campaignId: String): Result<Unit> {
        return try {
            inventoryCollection(campaignId).document(itemId).delete().await()
            dao.deleteItem(itemId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun InventoryItem.toFirestoreMap() = mapOf(
        "characterId" to characterId,
        "campaignId" to campaignId,
        "name" to name,
        "description" to description,
        "quantity" to quantity,
        "itemCardId" to itemCardId,
        "createdAt" to createdAt
    )
}
