package com.questkeep.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.questkeep.data.local.database.SpellDao
import com.questkeep.data.local.entity.toDomain
import com.questkeep.data.local.entity.toEntity
import com.questkeep.domain.entities.Spell
import com.questkeep.domain.repository.SpellRepository
import com.questkeep.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpellRepositoryImpl @Inject constructor(
    private val spellDao: SpellDao,
    private val firestore: FirebaseFirestore
) : SpellRepository {

    private fun spellsCollection(characterId: String) =
        firestore.collection(Constants.COLLECTION_CHARACTERS)
            .document(characterId)
            .collection("spells")

    override fun getSpellsForCharacter(characterId: String): Flow<List<Spell>> = channelFlow {
        // 1. Immediately emit cached spells from Room SQLite database (0ms UI latency)
        val localJob = launch {
            spellDao.getSpellsForCharacter(characterId).collect { list ->
                trySend(list.map { it.toDomain() })
            }
        }

        // 2. Real-time Firestore sync with offline persistence support
        if (characterId.isNotBlank()) {
            val listener = spellsCollection(characterId)
                .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    val spells = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            docToSpell(doc, characterId)
                        } catch (e: Exception) {
                            null
                        }
                    }?.sortedBy { it.createdAt } ?: emptyList()

                    launch {
                        spells.forEach { spellDao.insertSpell(it.toEntity()) }
                    }
                }

            awaitClose {
                localJob.cancel()
                listener.remove()
            }
        } else {
            awaitClose { localJob.cancel() }
        }
    }

    override suspend fun saveSpell(spell: Spell) {
        // Optimistic local insert
        spellDao.insertSpell(spell.toEntity())

        // Cloud sync to Firestore
        if (spell.characterId.isNotBlank()) {
            try {
                spellsCollection(spell.characterId)
                    .document(spell.id)
                    .set(spellToMap(spell))
                    .await()
            } catch (_: Exception) {
                // Offline persistence will sync automatically when network is available
            }
        }
    }

    override suspend fun deleteSpell(spell: Spell) {
        // Local removal
        spellDao.deleteSpell(spell.toEntity())

        // Cloud removal
        if (spell.characterId.isNotBlank()) {
            try {
                spellsCollection(spell.characterId)
                    .document(spell.id)
                    .delete()
                    .await()
            } catch (_: Exception) {
                // Offline persistence will sync deletion
            }
        }
    }

    private fun docToSpell(doc: DocumentSnapshot, characterId: String): Spell {
        return Spell(
            id = doc.id,
            characterId = doc.getString("characterId") ?: characterId,
            name = doc.getString("name") ?: "",
            coreId = doc.getString("coreId") ?: "",
            coreName = doc.getString("coreName") ?: "",
            modId = doc.getString("modId") ?: "none",
            modName = doc.getString("modName") ?: "",
            expressionId = doc.getString("expressionId") ?: "",
            expressionName = doc.getString("expressionName") ?: "",
            accentsJson = doc.getString("accentsJson") ?: "",
            powerLevel = (doc.getLong("powerLevel") ?: 1L).toInt(),
            effectText = doc.getString("effectText") ?: "",
            quarCost = (doc.getDouble("quarCost") ?: 0.0).toFloat(),
            ombraCost = (doc.getDouble("ombraCost") ?: 0.0).toFloat(),
            verunaCost = (doc.getDouble("verunaCost") ?: 0.0).toFloat(),
            sumeniCost = (doc.getDouble("sumeniCost") ?: 0.0).toFloat(),
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun spellToMap(spell: Spell): Map<String, Any> {
        return mapOf(
            "id" to spell.id,
            "characterId" to spell.characterId,
            "name" to spell.name,
            "coreId" to spell.coreId,
            "coreName" to spell.coreName,
            "modId" to spell.modId,
            "modName" to spell.modName,
            "expressionId" to spell.expressionId,
            "expressionName" to spell.expressionName,
            "accentsJson" to spell.accentsJson,
            "powerLevel" to spell.powerLevel,
            "effectText" to spell.effectText,
            "quarCost" to spell.quarCost.toDouble(),
            "ombraCost" to spell.ombraCost.toDouble(),
            "verunaCost" to spell.verunaCost.toDouble(),
            "sumeniCost" to spell.sumeniCost.toDouble(),
            "createdAt" to spell.createdAt
        )
    }
}
