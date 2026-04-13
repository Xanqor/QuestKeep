package com.questkeep.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.questkeep.data.local.database.InventoryDao
import com.questkeep.data.local.database.ItemCardDao
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.InventoryItem
import com.questkeep.domain.entities.ItemCard
import com.questkeep.domain.entities.ItemRarity
import com.questkeep.domain.repository.ItemCardRepository
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
class ItemCardRepositoryImpl @Inject constructor(
    private val dao: ItemCardDao,
    private val inventoryDao: InventoryDao,
    private val firestore: FirebaseFirestore
) : ItemCardRepository {

    private fun itemCardsCollection(campaignId: String) =
        firestore.collection(Constants.COLLECTION_CAMPAIGNS)
            .document(campaignId)
            .collection(Constants.COLLECTION_ITEM_CARDS)

    private fun inventoryCollection(campaignId: String) =
        firestore.collection(Constants.COLLECTION_CAMPAIGNS)
            .document(campaignId)
            .collection(Constants.COLLECTION_INVENTORY)

    override fun getItemCards(campaignId: String): Flow<List<ItemCard>> = callbackFlow {
        val listener = itemCardsCollection(campaignId)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val cards = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        ItemCard(
                            id = doc.id,
                            campaignId = campaignId,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            rarity = ItemRarity.entries.find {
                                it.name == doc.getString("rarity")
                            } ?: ItemRarity.COMMON,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                launch { cards.forEach { dao.insertItemCard(it.toEntity()) } }
                trySend(cards)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun saveItemCard(card: ItemCard): Result<ItemCard> {
        return try {
            val id = if (card.id.isBlank()) UUID.randomUUID().toString() else card.id
            // Preserve original createdAt when updating an existing card
            val createdAt = if (card.id.isBlank()) System.currentTimeMillis() else card.createdAt
            val newCard = card.copy(id = id, createdAt = createdAt)
            itemCardsCollection(card.campaignId).document(id).set(newCard.toFirestoreMap()).await()
            dao.insertItemCard(newCard.toEntity())
            Result.success(newCard)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteItemCard(cardId: String, campaignId: String): Result<Unit> {
        return try {
            itemCardsCollection(campaignId).document(cardId).delete().await()
            dao.deleteItemCard(cardId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun giveItemToCharacter(
        card: ItemCard,
        characterId: String,
        quantity: Int
    ): Result<InventoryItem> {
        return try {
            val id = UUID.randomUUID().toString()
            val item = InventoryItem(
                id = id,
                characterId = characterId,
                campaignId = card.campaignId,
                name = card.name,
                description = card.description,
                quantity = quantity,
                itemCardId = card.id,
                createdAt = System.currentTimeMillis()
            )
            val itemMap = mapOf(
                "characterId" to item.characterId,
                "campaignId" to item.campaignId,
                "name" to item.name,
                "description" to item.description,
                "quantity" to item.quantity,
                "itemCardId" to item.itemCardId,
                "createdAt" to item.createdAt
            )
            inventoryCollection(card.campaignId).document(id).set(itemMap).await()
            inventoryDao.insertItem(item.toEntity())
            Result.success(item)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun ItemCard.toFirestoreMap() = mapOf(
        "name" to name, "description" to description,
        "rarity" to rarity.name, "createdAt" to createdAt
    )
}
