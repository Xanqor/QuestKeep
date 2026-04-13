package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.domain.entities.PlayerNpc
import com.questkeep.domain.repository.PlayerNpcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlayerNpcUiState {
    object Loading : PlayerNpcUiState()
    data class Success(val npcs: List<PlayerNpc>) : PlayerNpcUiState()
    data class Error(val message: String) : PlayerNpcUiState()
}

@HiltViewModel
class PlayerNpcViewModel @Inject constructor(
    private val npcRepository: PlayerNpcRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerNpcUiState>(PlayerNpcUiState.Loading)
    val uiState: StateFlow<PlayerNpcUiState> = _uiState.asStateFlow()

    private val _editingNpc = MutableStateFlow<PlayerNpc?>(null)
    val editingNpc: StateFlow<PlayerNpc?> = _editingNpc.asStateFlow()

    private val _deletingNpc = MutableStateFlow<PlayerNpc?>(null)
    val deletingNpc: StateFlow<PlayerNpc?> = _deletingNpc.asStateFlow()

    private var currentCampaignId: String? = null

    fun loadNpcs(campaignId: String) {
        if (campaignId == currentCampaignId) return
        currentCampaignId = campaignId
        viewModelScope.launch {
            npcRepository.getNpcs(campaignId)
                .catch { _uiState.value = PlayerNpcUiState.Error(it.message ?: "Error") }
                .collect { _uiState.value = PlayerNpcUiState.Success(it) }
        }
    }

    fun startCreating() {
        _editingNpc.value = PlayerNpc(campaignId = currentCampaignId ?: "")
    }

    fun startEditing(npc: PlayerNpc) { _editingNpc.value = npc }
    fun cancelEdit() { _editingNpc.value = null }

    fun saveNpc(npc: PlayerNpc) {
        viewModelScope.launch {
            val state = _uiState.value
            val isNew = state !is PlayerNpcUiState.Success ||
                state.npcs.none { it.id == npc.id }
            if (isNew) npcRepository.saveNpc(npc)
            else npcRepository.updateNpc(npc)
            _editingNpc.value = null
        }
    }

    fun requestDelete(npc: PlayerNpc) { _deletingNpc.value = npc }
    fun cancelDelete() { _deletingNpc.value = null }

    fun confirmDelete() {
        val npc = _deletingNpc.value ?: return
        val cid = currentCampaignId ?: return
        viewModelScope.launch {
            npcRepository.deleteNpc(npc.id, cid)
            _deletingNpc.value = null
        }
    }
}
