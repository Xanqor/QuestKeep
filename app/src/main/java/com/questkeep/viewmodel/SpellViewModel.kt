package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.questkeep.domain.entities.Character
import com.questkeep.domain.entities.Spell
import com.questkeep.domain.repository.CharacterRepository
import com.questkeep.domain.repository.SpellRepository
import com.questkeep.utils.SpellsExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class SpellCastResult {
    object Success : SpellCastResult()
    data class NotEnoughResources(val missing: String) : SpellCastResult()
}

@HiltViewModel
class SpellViewModel @Inject constructor(
    private val spellRepository: SpellRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    private val _savedSpells = MutableStateFlow<List<Spell>>(emptyList())
    val savedSpells: StateFlow<List<Spell>> = _savedSpells.asStateFlow()

    private val _castResult = MutableSharedFlow<SpellCastResult>()
    val castResult: SharedFlow<SpellCastResult> = _castResult.asSharedFlow()

    private var loadedCharacterId: String? = null

    fun loadSpells(characterId: String) {
        if (characterId == loadedCharacterId) return
        loadedCharacterId = characterId
        viewModelScope.launch {
            spellRepository.getSpellsForCharacter(characterId)
                .catch { /* ignore */ }
                .collect { _savedSpells.value = it }
        }
    }

    /** Casts a spell: deducts resources from character. Returns success or error. */
    fun castSpell(spell: Spell, character: Character) {
        // Check resources
        val missingParts = mutableListOf<String>()
        if (character.quarResource < spell.quarCost)     missingParts += "Куар"
        if (character.ombraResource < spell.ombraCost)   missingParts += "Омбра"
        if (character.verunaResource < spell.verunaCost) missingParts += "Веруна"
        if (character.sumeniResource < spell.sumeniCost) missingParts += "Сумени"

        viewModelScope.launch {
            if (missingParts.isNotEmpty()) {
                _castResult.emit(SpellCastResult.NotEnoughResources(missingParts.joinToString(", ")))
                return@launch
            }
            val updated = character.copy(
                quarResource   = (character.quarResource   - spell.quarCost).coerceAtLeast(0f),
                ombraResource  = (character.ombraResource  - spell.ombraCost).coerceAtLeast(0f),
                verunaResource = (character.verunaResource - spell.verunaCost).coerceAtLeast(0f),
                sumeniResource = (character.sumeniResource - spell.sumeniCost).coerceAtLeast(0f),
                updatedAt = System.currentTimeMillis()
            )
            characterRepository.updateCharacter(updated)
            _castResult.emit(SpellCastResult.Success)
        }
    }

    fun saveSpell(spell: Spell) {
        viewModelScope.launch { spellRepository.saveSpell(spell) }
    }

    fun deleteSpell(spell: Spell) {
        viewModelScope.launch { spellRepository.deleteSpell(spell) }
    }

    fun exportSpellsJson(): String =
        Gson().toJson(SpellsExport(spells = _savedSpells.value))

    fun importSpellsFromJson(json: String) {
        val cid = loadedCharacterId ?: return
        try {
            val export = Gson().fromJson(json, SpellsExport::class.java) ?: return
            viewModelScope.launch {
                export.spells.forEach { spell ->
                    spellRepository.saveSpell(spell.copy(id = UUID.randomUUID().toString(), characterId = cid))
                }
            }
        } catch (_: Exception) {}
    }
}
