package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.domain.entities.Character
import com.questkeep.domain.entities.InventoryItem
import com.questkeep.domain.entities.SharedInventoryItem
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.domain.repository.CharacterRepository
import com.questkeep.domain.repository.InventoryRepository
import com.questkeep.domain.repository.SharedInventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val authRepository: AuthRepository,
    private val characterRepository: CharacterRepository,
    private val sharedInventoryRepository: SharedInventoryRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<InventoryItem>>(emptyList())
    val items: StateFlow<List<InventoryItem>> = _items.asStateFlow()

    /** Character for the current player — needed for crowns display/edit. */
    private val _character = MutableStateFlow<Character?>(null)
    val character: StateFlow<Character?> = _character.asStateFlow()

    private var inventoryJob: Job? = null
    private var characterJob: Job? = null

    val currentUserId: String? get() = authRepository.currentUserId

    /**
     * Load inventory and character for the given playerId.
     * Used both by players (playerId = their own auth UID)
     * and by GMs (playerId = selected character's playerId).
     */
    fun loadInventory(playerId: String, campaignId: String) {
        // Cancel previous subscriptions to avoid stale data from a different character
        inventoryJob?.cancel()
        characterJob?.cancel()
        _items.value = emptyList()

        inventoryJob = viewModelScope.launch {
            inventoryRepository.getInventory(playerId, campaignId)
                .catch { }
                .collect { _items.value = it }
        }
        characterJob = viewModelScope.launch {
            characterRepository.getCharacter(campaignId, playerId)
                .catch { }
                .collect { _character.value = it }
        }
    }

    /** Convenience wrapper for when the current auth user is loading their own inventory. */
    fun autoLoad(campaignId: String) {
        val userId = authRepository.currentUserId ?: return
        loadInventory(userId, campaignId)
    }

    fun addItem(campaignId: String, name: String, description: String, quantity: Int) {
        // Use character's playerId if available, otherwise fall back to auth user
        val playerId = _character.value?.playerId ?: authRepository.currentUserId ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val item = InventoryItem(
                characterId = playerId,
                campaignId = campaignId,
                name = name.trim(),
                description = description.trim(),
                quantity = quantity.coerceAtLeast(1)
            )
            inventoryRepository.addItem(item)
        }
    }

    fun updateQuantity(item: InventoryItem, delta: Int) {
        val newQty = (item.quantity + delta).coerceAtLeast(0)
        viewModelScope.launch {
            if (newQty == 0) {
                inventoryRepository.deleteItem(item.id, item.characterId, item.campaignId)
            } else {
                inventoryRepository.updateItem(item.copy(quantity = newQty))
            }
        }
    }

    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            inventoryRepository.deleteItem(item.id, item.characterId, item.campaignId)
        }
    }

    /** Save updated crowns value — immediately reflects in UI and persists to Firestore. */
    fun updateCrowns(newCrowns: Int) {
        val c = _character.value ?: return
        // Immediately update local state so the UI responds without waiting for Firestore round-trip
        val updated = c.copy(crowns = newCrowns, updatedAt = System.currentTimeMillis())
        _character.value = updated
        viewModelScope.launch {
            characterRepository.updateCharacter(updated)
        }
    }

    /** Move an item from personal inventory to the campaign bank. */
    fun sendToBank(item: InventoryItem) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            sharedInventoryRepository.depositItem(
                SharedInventoryItem(
                    campaignId = item.campaignId,
                    name = item.name,
                    description = item.description,
                    quantity = item.quantity,
                    depositedBy = uid
                )
            )
            inventoryRepository.deleteItem(item.id, item.characterId, item.campaignId)
        }
    }

    /**
     * Move [quantity] units of [item] to the campaign bank.
     * If [quantity] >= item's current stock, the item is deleted;
     * otherwise its quantity is reduced by [quantity].
     */
    fun sendToBankPartial(item: InventoryItem, quantity: Int) {
        if (quantity <= 0) return
        val uid = authRepository.currentUserId ?: return
        val clamped = quantity.coerceAtMost(item.quantity)
        viewModelScope.launch {
            sharedInventoryRepository.depositItem(
                SharedInventoryItem(
                    campaignId = item.campaignId,
                    name = item.name,
                    description = item.description,
                    quantity = clamped,
                    depositedBy = uid
                )
            )
            if (clamped >= item.quantity) {
                inventoryRepository.deleteItem(item.id, item.characterId, item.campaignId)
            } else {
                inventoryRepository.updateItem(item.copy(quantity = item.quantity - clamped))
            }
        }
    }
}
