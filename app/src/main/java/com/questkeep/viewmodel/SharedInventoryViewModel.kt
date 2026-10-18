package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.domain.entities.InventoryItem
import com.questkeep.domain.entities.SharedInventoryItem
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.domain.repository.InventoryRepository
import com.questkeep.domain.repository.SharedInventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedInventoryViewModel @Inject constructor(
    private val repo: SharedInventoryRepository,
    private val authRepository: AuthRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<SharedInventoryItem>>(emptyList())
    val items: StateFlow<List<SharedInventoryItem>> = _items.asStateFlow()

    private val _bankCrowns = MutableStateFlow(0)
    val bankCrowns: StateFlow<Int> = _bankCrowns.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val currentUserId: String? get() = authRepository.currentUserId

    private var itemsJob: kotlinx.coroutines.Job? = null
    private var crownsJob: kotlinx.coroutines.Job? = null

    fun load(campaignId: String) {
        itemsJob?.cancel()
        crownsJob?.cancel()
        itemsJob = viewModelScope.launch {
            repo.getItems(campaignId).catch { }.collect { _items.value = it }
        }
        crownsJob = viewModelScope.launch {
            repo.getBankCrowns(campaignId).catch { }.collect { _bankCrowns.value = it }
        }
    }

    private fun formatError(t: Throwable): String {
        val msg = t.message ?: ""
        return if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("insufficient permissions", ignoreCase = true) ||
            msg.contains("Missing or insufficient permissions", ignoreCase = true)
        ) {
            "Действие отклонено: только Гейм-мастер может распределять казну"
        } else {
            msg.ifBlank { "Произошла ошибка при выполнении операции" }
        }
    }

    fun depositItem(campaignId: String, name: String, description: String, quantity: Int) {
        val uid = authRepository.currentUserId ?: return
        if (name.isBlank() || quantity <= 0) return
        viewModelScope.launch {
            repo.depositItem(SharedInventoryItem(
                campaignId = campaignId, name = name.trim(),
                description = description.trim(), quantity = quantity, depositedBy = uid
            )).onFailure { _error.value = formatError(it) }
        }
    }

    fun withdrawItem(item: SharedInventoryItem) {
        viewModelScope.launch {
            repo.withdrawItem(item.id, item.campaignId)
                .onFailure { _error.value = formatError(it) }
        }
    }

    /** Remove item from the bank and add it to the current player's personal inventory. */
    fun withdrawItemToInventory(item: SharedInventoryItem) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            inventoryRepository.addItem(
                InventoryItem(
                    characterId = uid,
                    campaignId = item.campaignId,
                    name = item.name,
                    description = item.description,
                    quantity = item.quantity
                )
            )
            repo.withdrawItem(item.id, item.campaignId)
                .onFailure { _error.value = formatError(it) }
        }
    }

    fun depositCrowns(campaignId: String, amount: Int) {
        val uid = authRepository.currentUserId ?: return
        if (amount <= 0) return
        viewModelScope.launch {
            repo.depositCrowns(campaignId, uid, amount)
                .onFailure { _error.value = formatError(it) }
        }
    }

    fun withdrawCrowns(campaignId: String, amount: Int) {
        val uid = authRepository.currentUserId ?: return
        if (amount <= 0) return
        viewModelScope.launch {
            repo.withdrawCrowns(campaignId, uid, amount)
                .onFailure { _error.value = formatError(it) }
        }
    }

    fun clearError() { _error.value = null }
}
