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

    // Query by allMemberIds and gmId, backed by instant Room cache
    override fun getCampaignsForUser(userId: String): Flow<List<Campaign>> =
        kotlinx.coroutines.flow.channelFlow {
            // 1. Immediately emit from local Room DB
            val localJob = launch {
                dao.getAllCampaigns().collect { entities ->
                    val userCampaigns = entities.map { it.toDomain() }.filter {
                        userId == it.gmId || userId in it.playerIds || userId in it.coGmIds || userId in it.allMemberIds
                    }
                    trySend(userCampaigns)
                }
            }

            // 2. Real-time Firestore sync
            val campaignsMap = java.util.concurrent.ConcurrentHashMap<String, Campaign>()

            fun updateAndSave() {
                val list = campaignsMap.values.toList().sortedByDescending { it.createdAt }
                launch { dao.insertCampaigns(list.map { it.toEntity() }) }
            }

            val l1 = firestore.collection(Constants.COLLECTION_CAMPAIGNS)
                .whereArrayContains("allMemberIds", userId)
                .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.documents?.forEach { doc ->
                        docToCampaign(doc.id, doc)?.let { campaignsMap[it.id] = it }
                    }
                    updateAndSave()
                }

            val l2 = firestore.collection(Constants.COLLECTION_CAMPAIGNS)
                .whereEqualTo("gmId", userId)
                .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.documents?.forEach { doc ->
                        docToCampaign(doc.id, doc)?.let { campaignsMap[it.id] = it }
                    }
                    updateAndSave()
                }

            awaitClose {
                localJob.cancel()
                l1.remove()
                l2.remove()
            }
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

            // Index invite code for permission-safe, instant player discovery
            try {
                firestore.collection(Constants.COLLECTION_INVITE_CODES)
                    .document(inviteCode.uppercase())
                    .set(mapOf(
                        "campaignId" to ref.id,
                        "gmId" to campaign.gmId,
                        "name" to campaign.name,
                        "createdAt" to System.currentTimeMillis()
                    )).await()
            } catch (_: Exception) {}

            dao.insertCampaign(newCampaign.toEntity())
            Result.success(newCampaign)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun joinCampaign(inviteCode: String, userId: String): Result<Campaign> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            kotlinx.coroutines.withTimeout(12000L) {
                val cleanCode = inviteCode.trim().uppercase()
                if (cleanCode.length != 6) {
                    return@withTimeout Result.failure(Exception("Код приглашения должен содержать ровно 6 символов"))
                }

                var foundCampaignId: String? = null
                var campaignDoc: com.google.firebase.firestore.DocumentSnapshot? = null

                // 1. Try public index first (works even if player cannot read root campaigns query)
                try {
                    val codeDoc = firestore.collection(Constants.COLLECTION_INVITE_CODES)
                        .document(cleanCode)
                        .get()
                        .await()
                    if (codeDoc.exists()) {
                        foundCampaignId = codeDoc.getString("campaignId")
                    }
                } catch (_: Exception) {}

                // 2. Fallback to direct query on campaigns collection
                if (foundCampaignId == null) {
                    try {
                        val query = firestore.collection(Constants.COLLECTION_CAMPAIGNS)
                            .whereEqualTo("inviteCode", cleanCode)
                            .limit(1)
                            .get()
                            .await()
                        val doc = query.documents.firstOrNull()
                        if (doc != null) {
                            foundCampaignId = doc.id
                            campaignDoc = doc
                        }
                    } catch (_: Exception) {}
                }

                if (foundCampaignId == null) {
                    return@withTimeout Result.failure(Exception("Кампания с кодом «$cleanCode» не найдена. Проверьте правильность кода."))
                }

                // 3. Fetch current live campaign document
                val liveDoc = campaignDoc ?: firestore.collection(Constants.COLLECTION_CAMPAIGNS)
                    .document(foundCampaignId)
                    .get()
                    .await()

                if (!liveDoc.exists()) {
                    return@withTimeout Result.failure(Exception("Кампания не найдена или была удалена."))
                }

                val gmId = liveDoc.getString("gmId") ?: ""
                @Suppress("UNCHECKED_CAST")
                val existingPlayerIds = (liveDoc.get("playerIds") as? List<String>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val existingCoGmIds = (liveDoc.get("coGmIds") as? List<String>) ?: emptyList()
                val maxPlayers = (liveDoc.getLong("maxPlayers") ?: 5L).toInt()

                if (userId == gmId || userId in existingCoGmIds) {
                    return@withTimeout Result.failure(Exception("Вы уже являетесь Гейм-мастером этой кампании"))
                }
                if (userId in existingPlayerIds) {
                    val existing = docToCampaign(liveDoc.id, liveDoc)
                    if (existing != null) dao.insertCampaign(existing.toEntity())
                    return@withTimeout Result.success(existing ?: return@withTimeout Result.failure(Exception("Вы уже состоите в этой кампании")))
                }
                if (existingPlayerIds.size >= maxPlayers) {
                    return@withTimeout Result.failure(Exception("В кампании уже максимальное число игроков ($maxPlayers)"))
                }

                // 4. Atomic arrayUnion: avoids concurrency race conditions
                liveDoc.reference.update(mapOf(
                    "playerIds" to FieldValue.arrayUnion(userId),
                    "allMemberIds" to FieldValue.arrayUnion(userId)
                )).await()

                val updatedPlayers = existingPlayerIds.toMutableList().also { if (userId !in it) it.add(userId) }
                @Suppress("UNCHECKED_CAST")
                val existingMembers = (liveDoc.get("allMemberIds") as? List<String>) ?: emptyList()
                val updatedMembers = existingMembers.toMutableList().also { if (userId !in it) it.add(userId) }

                val updatedCampaign = docToCampaign(liveDoc.id, liveDoc)?.copy(
                    playerIds = updatedPlayers,
                    allMemberIds = updatedMembers
                ) ?: return@withTimeout Result.failure(Exception("Ошибка чтения данных кампании"))

                dao.insertCampaign(updatedCampaign.toEntity())
                Result.success(updatedCampaign)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Result.failure(Exception("Превышено время ожидания сервера. Проверьте интернет-соединение."))
        } catch (e: Exception) {
            val msg = if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                "Доступ ограничен правилами безопасности Firebase. Обратитесь к ГМу кампании."
            } else {
                e.localizedMessage ?: "Не удалось присоединиться к кампании"
            }
            Result.failure(Exception(msg))
        }
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
            try {
                val doc = firestore.collection(Constants.COLLECTION_CAMPAIGNS).document(campaignId).get().await()
                val code = doc.getString("inviteCode")
                if (!code.isNullOrBlank()) {
                    firestore.collection(Constants.COLLECTION_INVITE_CODES).document(code.uppercase()).delete().await()
                }
            } catch (_: Exception) {}
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
