package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.questkeep.domain.entities.NpcCard
import com.questkeep.domain.repository.NpcCardRepository
import com.questkeep.utils.NpcsExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class BestiaryUiState {
    object Loading : BestiaryUiState()
    data class Success(val npcs: List<NpcCard>) : BestiaryUiState()
    data class Error(val message: String) : BestiaryUiState()
}

@HiltViewModel
class BestiaryViewModel @Inject constructor(
    private val npcRepository: NpcCardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BestiaryUiState>(BestiaryUiState.Loading)
    val uiState: StateFlow<BestiaryUiState> = _uiState.asStateFlow()

    private val _editingNpc = MutableStateFlow<NpcCard?>(null)
    val editingNpc: StateFlow<NpcCard?> = _editingNpc.asStateFlow()

    private val _deletingNpc = MutableStateFlow<NpcCard?>(null)
    val deletingNpc: StateFlow<NpcCard?> = _deletingNpc.asStateFlow()

    private var currentCampaignId: String? = null

    fun loadNpcs(campaignId: String) {
        if (campaignId == currentCampaignId) return
        currentCampaignId = campaignId
        viewModelScope.launch {
            npcRepository.getNpcs(campaignId)
                .catch { _uiState.value = BestiaryUiState.Error(it.message ?: "Error") }
                .collect { _uiState.value = BestiaryUiState.Success(it) }
        }
    }

    fun startCreating() {
        _editingNpc.value = NpcCard(campaignId = currentCampaignId ?: "")
    }

    fun startEditing(npc: NpcCard) { _editingNpc.value = npc }
    fun cancelEdit() { _editingNpc.value = null }

    fun saveNpc(npc: NpcCard) {
        viewModelScope.launch {
            val state = _uiState.value
            val isNew = state !is BestiaryUiState.Success ||
                state.npcs.none { it.id == npc.id }
            if (isNew) npcRepository.saveNpc(npc)
            else npcRepository.updateNpc(npc)
            _editingNpc.value = null
        }
    }

    fun requestDelete(npc: NpcCard) { _deletingNpc.value = npc }
    fun cancelDelete() { _deletingNpc.value = null }

    fun confirmDelete() {
        val npc = _deletingNpc.value ?: return
        val cid = currentCampaignId ?: return
        viewModelScope.launch {
            npcRepository.deleteNpc(npc.id, cid)
            _deletingNpc.value = null
        }
    }

    fun exportNpcsJson(): String {
        val npcs = (_uiState.value as? BestiaryUiState.Success)?.npcs ?: emptyList()
        return Gson().toJson(NpcsExport(npcs = npcs))
    }

    fun importNpcsFromJson(json: String) {
        val cid = currentCampaignId ?: return
        try {
            val export = Gson().fromJson(json, NpcsExport::class.java) ?: return
            viewModelScope.launch {
                export.npcs.forEach { npc ->
                    npcRepository.saveNpc(npc.copy(id = UUID.randomUUID().toString(), campaignId = cid))
                }
            }
        } catch (_: Exception) {}
    }
}
