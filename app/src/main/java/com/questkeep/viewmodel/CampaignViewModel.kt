package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.domain.entities.Campaign
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.domain.repository.CampaignRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CampaignUiEvent {
    data class Error(val message: String) : CampaignUiEvent()
    data class JoinSuccess(val campaignId: String) : CampaignUiEvent()
    data class CreateSuccess(val campaignId: String) : CampaignUiEvent()
    object DeleteSuccess : CampaignUiEvent()
    object UpdateSuccess : CampaignUiEvent()
}

@HiltViewModel
class CampaignViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _campaigns = MutableStateFlow<List<Campaign>>(emptyList())
    val campaigns: StateFlow<List<Campaign>> = _campaigns.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<CampaignUiEvent>()
    val events: SharedFlow<CampaignUiEvent> = _events.asSharedFlow()

    val currentUserId: String? get() = authRepository.currentUserId

    init { loadCampaigns() }

    private fun loadCampaigns() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            campaignRepository.getCampaignsForUser(userId).collect { _campaigns.value = it }
        }
    }

    fun createCampaign(name: String, description: String, gameSystem: String, maxPlayers: Int) {
        if (name.isBlank()) {
            viewModelScope.launch { _events.emit(CampaignUiEvent.Error("Название кампании обязательно")) }
            return
        }
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = campaignRepository.createCampaign(Campaign(
                name = name.trim(), description = description.trim(),
                gameSystem = gameSystem, maxPlayers = maxPlayers, gmId = userId
            ))
            _isLoading.value = false
            result.fold(
                onSuccess = { _events.emit(CampaignUiEvent.CreateSuccess(it.id)) },
                onFailure = { _events.emit(CampaignUiEvent.Error(it.message ?: "Ошибка создания")) }
            )
        }
    }

    fun joinCampaign(inviteCode: String) {
        if (inviteCode.length != 6) {
            viewModelScope.launch { _events.emit(CampaignUiEvent.Error("Код должен быть 6 символов")) }
            return
        }
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = campaignRepository.joinCampaign(inviteCode.uppercase(), userId)
            _isLoading.value = false
            result.fold(
                onSuccess = { _events.emit(CampaignUiEvent.JoinSuccess(it.id)) },
                onFailure = { _events.emit(CampaignUiEvent.Error(it.message ?: "Кампания не найдена")) }
            )
        }
    }

    fun leaveCampaign(campaignId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch { campaignRepository.leaveCampaign(campaignId, userId) }
    }

    fun deleteCampaign(campaignId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = campaignRepository.deleteCampaign(campaignId)
            _isLoading.value = false
            result.fold(
                onSuccess = { _events.emit(CampaignUiEvent.DeleteSuccess) },
                onFailure = { _events.emit(CampaignUiEvent.Error(it.message ?: "Ошибка удаления")) }
            )
        }
    }

    fun updateCampaign(campaign: Campaign, name: String, description: String, gameSystem: String, maxPlayers: Int) {
        if (name.isBlank()) {
            viewModelScope.launch { _events.emit(CampaignUiEvent.Error("Название не может быть пустым")) }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = campaignRepository.updateCampaign(campaign.copy(
                name = name.trim(), description = description.trim(),
                gameSystem = gameSystem, maxPlayers = maxPlayers
            ))
            _isLoading.value = false
            result.fold(
                onSuccess = { _events.emit(CampaignUiEvent.UpdateSuccess) },
                onFailure = { _events.emit(CampaignUiEvent.Error(it.message ?: "Ошибка обновления")) }
            )
        }
    }

    fun addCoGm(campaignId: String, userId: String) {
        viewModelScope.launch {
            val result = campaignRepository.addCoGm(campaignId, userId)
            result.onFailure { _events.emit(CampaignUiEvent.Error(it.message ?: "Ошибка")) }
        }
    }

    fun removeCoGm(campaignId: String, userId: String) {
        viewModelScope.launch {
            val result = campaignRepository.removeCoGm(campaignId, userId)
            result.onFailure { _events.emit(CampaignUiEvent.Error(it.message ?: "Ошибка")) }
        }
    }

    /** Returns true if userId is GM or co-GM of this campaign */
    fun isGm(campaign: Campaign): Boolean {
        val uid = authRepository.currentUserId ?: return false
        return campaign.gmId == uid || campaign.coGmIds.contains(uid)
    }
}
