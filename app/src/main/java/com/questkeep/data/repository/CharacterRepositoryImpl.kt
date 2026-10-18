package com.questkeep.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.questkeep.data.local.database.CharacterDao
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.Character
import com.questkeep.domain.repository.CharacterRepository
import com.questkeep.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepositoryImpl @Inject constructor(
    private val dao: CharacterDao,
    private val firestore: FirebaseFirestore
) : CharacterRepository {

    /** Real-time listener — watches Firestore for the player's character in this campaign.
     *  Uses a single whereEqualTo to avoid requiring a Firestore composite index,
     *  then filters by playerId in-memory. */
    override fun getCharacter(campaignId: String, playerId: String): Flow<Character?> = callbackFlow {
        val localJob = launch {
            dao.getCharacterForPlayer(campaignId, playerId).collect { entity ->
                trySend(entity?.toDomain())
            }
        }

        val listener = firestore
            .collection(Constants.COLLECTION_CHARACTERS)
            .whereEqualTo("campaignId", campaignId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val character = snapshot?.documents
                    ?.mapNotNull { docToCharacter(it) }
                    ?.firstOrNull { it.playerId == playerId }
                if (character != null) {
                    launch { dao.insertCharacter(character.toEntity()) }
                }
            }

        awaitClose {
            localJob.cancel()
            listener.remove()
        }
    }

    override fun getCharactersForCampaign(campaignId: String): Flow<List<Character>> = callbackFlow {
        val localJob = launch {
            dao.getCharactersForCampaign(campaignId).collect { entities ->
                trySend(entities.map { it.toDomain() })
            }
        }

        val listener = firestore
            .collection(Constants.COLLECTION_CHARACTERS)
            .whereEqualTo("campaignId", campaignId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val characters = snapshot?.documents?.mapNotNull { docToCharacter(it) } ?: emptyList()
                launch { characters.forEach { dao.insertCharacter(it.toEntity()) } }
            }

        awaitClose {
            localJob.cancel()
            listener.remove()
        }
    }

    override suspend fun saveCharacter(character: Character): Result<Unit> {
        return try {
            dao.insertCharacter(character.toEntity())
            firestore.collection(Constants.COLLECTION_CHARACTERS)
                .document("${character.campaignId}_${character.id}")
                .set(character.toFirestoreMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCharacter(character: Character): Result<Unit> {
        return try {
            dao.updateCharacter(character.toEntity())
            firestore.collection(Constants.COLLECTION_CHARACTERS)
                .document("${character.campaignId}_${character.id}")
                .set(character.toFirestoreMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCharacter(id: String): Result<Unit> {
        return try {
            val localChar = dao.findCharacterById(id)
            if (localChar != null) {
                firestore.collection(Constants.COLLECTION_CHARACTERS)
                    .document("${localChar.campaignId}_${localChar.id}")
                    .delete()
                    .await()
            } else {
                val snapshot = firestore.collection(Constants.COLLECTION_CHARACTERS)
                    .whereEqualTo("id", id)
                    .get()
                    .await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            }
            dao.deleteCharacter(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun docToCharacter(doc: DocumentSnapshot): Character? = try {
        Character(
            id            = doc.getString("id") ?: doc.id,
            name          = doc.getString("name") ?: "",
            className     = doc.getString("className") ?: "",
            level         = (doc.getLong("level") ?: 1L).toInt(),
            campaignId    = doc.getString("campaignId") ?: "",
            playerId      = doc.getString("playerId") ?: "",
            currentHp     = (doc.getLong("currentHp") ?: 10L).toInt(),
            maxHp         = (doc.getLong("maxHp") ?: 10L).toInt(),
            armor         = (doc.getLong("armor") ?: 0L).toInt(),
            maxArmor      = (doc.getLong("maxArmor") ?: 0L).toInt(),
            armorBlock    = (doc.getLong("armorBlock") ?: 0L).toInt(),
            constitution  = (doc.getLong("constitution") ?: doc.getLong("wellbeing") ?: 0L).toInt(),
            movement      = (doc.getLong("movement") ?: 0L).toInt(),
            perception    = (doc.getLong("perception") ?: doc.getLong("attention") ?: 0L).toInt(),
            intelligence  = (doc.getLong("intelligence") ?: doc.getLong("thinking") ?: 0L).toInt(),
            influence     = (doc.getLong("influence") ?: doc.getLong("communication") ?: 0L).toInt(),
            willpower     = (doc.getLong("willpower") ?: doc.getLong("combat") ?: 0L).toInt(),
            damage        = (doc.getLong("damage") ?: 0L).toInt(),
            dodge         = (doc.getLong("dodge") ?: 0L).toInt(),
            initiative    = (doc.getLong("initiative") ?: 0L).toInt(),
            quarResource  = (doc.getDouble("quarResource") ?: 0.0).toFloat(),
            ombraResource = (doc.getDouble("ombraResource") ?: 0.0).toFloat(),
            verunaResource= (doc.getDouble("verunaResource") ?: 0.0).toFloat(),
            sumeniResource= (doc.getDouble("sumeniResource") ?: 0.0).toFloat(),
            maxQuarResource  = (doc.getDouble("maxQuarResource") ?: 4.0).toFloat(),
            maxOmbraResource = (doc.getDouble("maxOmbraResource") ?: 4.0).toFloat(),
            maxVerunaResource= (doc.getDouble("maxVerunaResource") ?: 4.0).toFloat(),
            maxSumeniResource= (doc.getDouble("maxSumeniResource") ?: 4.0).toFloat(),
            inspirationPoints= (doc.getLong("inspirationPoints") ?: 0L).toInt(),
            experiencePoints = (doc.getLong("experiencePoints") ?: 0L).toInt(),
            crowns        = (doc.getLong("crowns") ?: 0L).toInt(),
            updatedAt     = doc.getLong("updatedAt") ?: System.currentTimeMillis()
        )
    } catch (e: Exception) { null }

    private fun Character.toFirestoreMap(): Map<String, Any?> = mapOf(
        "id"              to id,
        "name"            to name,
        "className"       to className,
        "level"           to level,
        "campaignId"      to campaignId,
        "playerId"        to playerId,
        "currentHp"       to currentHp,
        "maxHp"           to maxHp,
        "armor"           to armor,
        "maxArmor"        to maxArmor,
        "armorBlock"      to armorBlock,
        "constitution"    to constitution,
        "movement"        to movement,
        "perception"      to perception,
        "intelligence"    to intelligence,
        "influence"       to influence,
        "willpower"       to willpower,
        // Legacy keys for backwards compatibility with existing clients
        "wellbeing"       to constitution,
        "combat"          to willpower,
        "thinking"        to intelligence,
        "attention"       to perception,
        "communication"   to influence,
        "damage"          to damage,
        "dodge"           to dodge,
        "initiative"      to initiative,
        "quarResource"    to quarResource,
        "ombraResource"   to ombraResource,
        "verunaResource"  to verunaResource,
        "sumeniResource"  to sumeniResource,
        "maxQuarResource"  to maxQuarResource,
        "maxOmbraResource" to maxOmbraResource,
        "maxVerunaResource" to maxVerunaResource,
        "maxSumeniResource" to maxSumeniResource,
        "inspirationPoints" to inspirationPoints,
        "experiencePoints"  to experiencePoints,
        "crowns"            to crowns,
        "updatedAt"       to updatedAt
    )
}
