package com.questkeep.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.questkeep.data.local.database.CampaignDao
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.Campaign
import com.questkeep.domain.repository.CampaignRepository
import com.questkeep.utils.Constants
import com.questkeep.utils.generateInviteCode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignRepositoryImpl @Inject constructor(
    private val dao: CampaignDao,
    private val firestore: FirebaseFirestore
) : CampaignRepository {

    // Query by allMemberIds — includes gmId + coGmIds + playerIds
    override fun getCampaignsForUser(userId: String): Flow<List<Campaign>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_CAMPAIGNS)
            .whereArrayContains("allMemberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { return@addSnapshotListener }
                val campaigns = snapshot?.documents?.mapNotNull { doc ->
                    docToCampaign(doc.id, doc) ?: return@mapNotNull null
                } ?: emptyList()
                trySend(campaigns)
            }
        awaitClose { listener.remove() }
    }

    /** Real-time listener — watches Firestore for the campaign document.
     *  Replaces the old Room-only read which returned stale data (empty playerIds). */
    override fun getCampaign(campaignId: String): Flow<Campaign?> = callbackFlow {
        val listener = firestore
            .collection(Constants.COLLECTION_CAMPAIGNS)
            .document(campaignId)
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener
                val campaign = if (doc != null && doc.exists()) docToCampaign(doc.id, doc) else null
                if (campaign != null) launch { dao.insertCampaign(campaign.toEntity()) }
                trySend(campaign)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createCampaign(campaign: Campaign): Result<Campaign> {
        return try {
            val inviteCode = generateInviteCode()
            val ref = firestore.collection(Constants.COLLECTION_CAMPAIGNS).document()
            val newCampaign = campaign.copy(
                id = ref.id,
                inviteCode = inviteCode,
                playerIds = emptyList(),                // GM is NOT a player
                coGmIds = emptyList(),
                allMemberIds = listOf(campaign.gmId)    // only GM at start
            )
            ref.set(newCampaign.toFirestoreMap()).await()
            dao.insertCampaign(newCampaign.toEntity())
            Result.success(newCampaign)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun joinCampaign(inviteCode: String, userId: String): Result<Campaign> {
        return try {
            val query = firestore.collection(Constants.COLLECTION_CAMPAIGNS)
                .whereEqualTo("inviteCode", inviteCode.uppercase())
                .limit(1).get().await()
            val doc = query.documents.firstOrNull()
                ?: return Result.failure(Exception("Кампания не найдена"))

            val gmId = doc.getString("gmId") ?: ""
            @Suppress("UNCHECKED_CAST")
            val existingPlayerIds = (doc.get("playerIds") as? List<String>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val existingCoGmIds = (doc.get("coGmIds") as? List<String>) ?: emptyList()

            if (userId == gmId || userId in existingCoGmIds)
                return Result.failure(Exception("Вы уже являетесь ГМом этой кампании"))
            if (userId in existingPlayerIds)
                return Result.failure(Exception("Вы уже состоите в этой кампании"))

            val playerIds = existingPlayerIds.toMutableList().also { it.add(userId) }
            @Suppress("UNCHECKED_CAST")
            val allMemberIds = ((doc.get("allMemberIds") as? List<String>) ?: emptyList()).toMutableList()
            if (userId !in allMemberIds) allMemberIds.add(userId)

            doc.reference.update(mapOf("playerIds" to playerIds, "allMemberIds" to allMemberIds)).await()
            val campaign = docToCampaign(doc.id, doc)?.copy(
                playerIds = playerIds, allMemberIds = allMemberIds)
                ?: return Result.failure(Exception("Parse error"))
            dao.insertCampaign(campaign.toEntity())
            Result.success(campaign)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun leaveCampaign(campaignId: String, userId: String): Result<Unit> {
        return try {
            val doc = firestore.collection(Constants.COLLECTION_CAMPAIGNS)
                .document(campaignId).get().await()
            @Suppress("UNCHECKED_CAST")
            val playerIds = ((doc.get("playerIds") as? List<String>) ?: emptyList()).filter { it != userId }
            @Suppress("UNCHECKED_CAST")
            val allMemberIds = ((doc.get("allMemberIds") as? List<String>) ?: emptyList()).filter { it != userId }
            doc.reference.update(mapOf("playerIds" to playerIds, "allMemberIds" to allMemberIds)).await()
            dao.deleteCampaign(campaignId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteCampaign(campaignId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_CAMPAIGNS).document(campaignId).delete().await()
            dao.deleteCampaign(campaignId)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateCampaign(campaign: Campaign): Result<Campaign> {
        return try {
            firestore.collection(Constants.COLLECTION_CAMPAIGNS)
                .document(campaign.id)
                .update(mapOf(
                    "name" to campaign.name, "description" to campaign.description,
                    "gameSystem" to campaign.gameSystem, "maxPlayers" to campaign.maxPlayers
                )).await()
            dao.insertCampaign(campaign.toEntity())
            Result.success(campaign)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun addCoGm(campaignId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_CAMPAIGNS).document(campaignId)
                .update(mapOf(
                    "coGmIds" to FieldValue.arrayUnion(userId),
                    "allMemberIds" to FieldValue.arrayUnion(userId)
                )).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun removeCoGm(campaignId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_CAMPAIGNS).document(campaignId)
                .update(mapOf(
                    "coGmIds" to FieldValue.arrayRemove(userId),
                    "allMemberIds" to FieldValue.arrayRemove(userId)
                )).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun docToCampaign(id: String, doc: com.google.firebase.firestore.DocumentSnapshot): Campaign? {
        return try {
            Campaign(
                id = id,
                name = doc.getString("name") ?: "",
                description = doc.getString("description") ?: "",
                gameSystem = doc.getString("gameSystem") ?: "Custom",
                maxPlayers = (doc.getLong("maxPlayers") ?: 5L).toInt(),
                gmId = doc.getString("gmId") ?: "",
                coGmIds = (doc.get("coGmIds") as? List<String>) ?: emptyList(),
                inviteCode = doc.getString("inviteCode") ?: "",
                playerIds = (doc.get("playerIds") as? List<String>) ?: emptyList(),
                allMemberIds = (doc.get("allMemberIds") as? List<String>) ?: emptyList(),
                createdAt = doc.getLong("createdAt") ?: 0L
            )
        } catch (e: Exception) { null }
    }

    private fun Campaign.toFirestoreMap() = mapOf(
        "name" to name, "description" to description, "gameSystem" to gameSystem,
        "maxPlayers" to maxPlayers, "gmId" to gmId, "inviteCode" to inviteCode,
        "coGmIds" to coGmIds, "playerIds" to playerIds, "allMemberIds" to allMemberIds,
        "createdAt" to createdAt
    )
}
