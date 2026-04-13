package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.questkeep.domain.entities.Character
import com.questkeep.domain.entities.ItemCard
import com.questkeep.domain.entities.ItemRarity
import com.questkeep.domain.repository.CharacterRepository
import com.questkeep.domain.repository.ItemCardRepository
import com.questkeep.utils.ItemCardsExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ItemCardViewModel @Inject constructor(
    private val itemCardRepository: ItemCardRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    private val _itemCards = MutableStateFlow<List<ItemCard>>(emptyList())
    val itemCards: StateFlow<List<ItemCard>> = _itemCards.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    /** All characters in the current campaign — for the "give item to player" picker. */
    private val _allCharacters = MutableStateFlow<List<Character>>(emptyList())
    val allCharacters: StateFlow<List<Character>> = _allCharacters.asStateFlow()

    private val _editingCard = MutableStateFlow<ItemCard?>(null)
    val editingCard: StateFlow<ItemCard?> = _editingCard.asStateFlow()

    fun loadItemCards(campaignId: String) {
        viewModelScope.launch {
            itemCardRepository.getItemCards(campaignId)
                .catch { }
                .collect { _itemCards.value = it }
        }
    }

    fun loadCharacters(campaignId: String) {
        viewModelScope.launch {
            characterRepository.getCharactersForCampaign(campaignId)
                .catch { }
                .collect { _allCharacters.value = it }
        }
    }

    fun saveItemCard(campaignId: String, name: String, description: String, rarity: ItemRarity) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _isSaving.value = true
            val card = ItemCard(
                campaignId = campaignId,
                name = name.trim(),
                description = description.trim(),
                rarity = rarity
            )
            val result = itemCardRepository.saveItemCard(card)
            _isSaving.value = false
            _saveSuccess.value = result.isSuccess
        }
    }

    fun deleteItemCard(card: ItemCard) {
        viewModelScope.launch {
            itemCardRepository.deleteItemCard(card.id, card.campaignId)
        }
    }

    fun giveItemToCharacter(card: ItemCard, characterId: String, quantity: Int) {
        viewModelScope.launch {
            itemCardRepository.giveItemToCharacter(card, characterId, quantity)
        }
    }

    fun resetSaveSuccess() { _saveSuccess.value = false }

    fun startEditing(card: ItemCard) { _editingCard.value = card }
    fun cancelEdit() { _editingCard.value = null }

    fun updateItemCard(card: ItemCard) {
        if (card.name.isBlank()) return
        viewModelScope.launch {
            _isSaving.value = true
            itemCardRepository.saveItemCard(card)
            _isSaving.value = false
            _editingCard.value = null
        }
    }

    fun exportItemCardsJson(): String =
        Gson().toJson(ItemCardsExport(itemCards = _itemCards.value))

    fun importItemCardsFromJson(json: String, campaignId: String) {
        if (campaignId.isBlank()) return
        try {
            val export = Gson().fromJson(json, ItemCardsExport::class.java) ?: return
            viewModelScope.launch {
                export.itemCards.forEach { card ->
                    itemCardRepository.saveItemCard(card.copy(id = UUID.randomUUID().toString(), campaignId = campaignId))
                }
            }
        } catch (_: Exception) {}
    }
}
