package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.domain.entities.Campaign
import com.questkeep.domain.entities.Character
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.domain.repository.CampaignRepository
import com.questkeep.domain.repository.CharacterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerListItem(
    val userId: String,
    val nickname: String,
    val avatarEmoji: String,
    val isGm: Boolean,
    val isCoGm: Boolean,
    val character: Character? = null
)

@HiltViewModel
class PlayerListViewModel @Inject constructor(
    private val campaignRepository: CampaignRepository,
    private val authRepository: AuthRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    private val _players = MutableStateFlow<List<PlayerListItem>>(emptyList())
    val players: StateFlow<List<PlayerListItem>> = _players.asStateFlow()

    private val _campaign = MutableStateFlow<Campaign?>(null)
    val campaign: StateFlow<Campaign?> = _campaign.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val currentUserId: String? get() = authRepository.currentUserId

    fun isCurrentUserGm(): Boolean {
        val c = _campaign.value ?: return false
        val myId = currentUserId ?: return false
        return c.gmId == myId || myId in c.coGmIds
    }

    fun loadPlayers(campaignId: String) {
        viewModelScope.launch {
            combine(
                campaignRepository.getCampaign(campaignId),
                characterRepository.getCharactersForCampaign(campaignId)
            ) { campaign, characters ->
                campaign to characters
            }
            .catch { _isLoading.value = false }
            .collect { (campaign, characters) ->
                _campaign.value = campaign
                if (campaign == null) {
                    _isLoading.value = false
                    return@collect
                }
                val allIds = (listOf(campaign.gmId) + campaign.coGmIds + campaign.playerIds).distinct()
                val items = allIds.map { userId ->
                    val user = authRepository.getUser(userId)
                    val char = characters.find { it.playerId == userId }
                    PlayerListItem(
                        userId      = userId,
                        nickname    = user?.nickname ?: userId.take(8),
                        avatarEmoji = user?.avatarEmoji ?: "🧙",
                        isGm        = userId == campaign.gmId,
                        isCoGm      = userId in campaign.coGmIds,
                        character   = char
                    )
                }
                _players.value = items
                _isLoading.value = false
            }
        }
    }

    fun grantCoGm(campaignId: String, userId: String) {
        viewModelScope.launch { campaignRepository.addCoGm(campaignId, userId) }
    }

    fun revokeCoGm(campaignId: String, userId: String) {
        viewModelScope.launch { campaignRepository.removeCoGm(campaignId, userId) }
    }
}
