package com.questkeep.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.domain.entities.ChatMessage
import com.questkeep.domain.entities.MessageType
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.domain.repository.CampaignRepository
import com.questkeep.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Simple player info shown in whisper picker
data class PlayerInfo(val userId: String, val name: String)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val campaignRepository: CampaignRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _imageUploadError = MutableStateFlow<String?>(null)
    val imageUploadError: StateFlow<String?> = _imageUploadError.asStateFlow()

    // Players in current campaign (for whisper picker)
    private val _campaignPlayers = MutableStateFlow<List<PlayerInfo>>(emptyList())
    val campaignPlayers: StateFlow<List<PlayerInfo>> = _campaignPlayers.asStateFlow()

    // Currently selected whisper target (null = public)
    private val _whisperTarget = MutableStateFlow<PlayerInfo?>(null)
    val whisperTarget: StateFlow<PlayerInfo?> = _whisperTarget.asStateFlow()

    // GM ID of current campaign (used to show private dice to GM)
    private val _campaignGmId = MutableStateFlow<String?>(null)
    val campaignGmId: StateFlow<String?> = _campaignGmId.asStateFlow()

    val currentUserId: String? get() = authRepository.currentUserId

    private var loadedCampaignId: String? = null
    private var messageJob: kotlinx.coroutines.Job? = null

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun loadMessages(campaignId: String) {
        if (campaignId == loadedCampaignId && _messages.value.isNotEmpty()) return
        loadedCampaignId = campaignId
        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            authRepository.observeAuthState()
                .filterNotNull()
                .flatMapLatest { user ->
                    messageRepository.getMessages(campaignId, user.id)
                }
                .catch { /* silently ignore */ }
                .collect { _messages.value = it }
        }
    }

    fun sendMessage(campaignId: String, content: String, senderName: String) {
        if (content.isBlank()) return
        val userId = authRepository.currentUserId ?: return
        val target = _whisperTarget.value
        viewModelScope.launch {
            _isSending.value = true
            val message = ChatMessage(
                senderId = userId,
                senderName = senderName,
                campaignId = campaignId,
                content = content.trim(),
                type = if (target != null) MessageType.WHISPER else MessageType.NORMAL,
                isPublic = target == null,
                targetPlayerId = target?.userId,
                targetPlayerName = target?.name
            )
            messageRepository.sendMessage(message)
            _isSending.value = false
            // Reset whisper after sending
            if (target != null) _whisperTarget.value = null
        }
    }

    fun sendImageMessage(campaignId: String, imageUri: Uri, senderName: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _isUploadingImage.value = true
            _imageUploadError.value = null
            val result = messageRepository.sendImageMessage(campaignId, imageUri, userId, senderName)
            _isUploadingImage.value = false
            if (result.isFailure) {
                _imageUploadError.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Не удалось отправить изображение. Проверьте интернет-соединение."
            }
        }
    }

    fun clearImageUploadError() {
        _imageUploadError.value = null
    }

    fun sendDiceResult(
        campaignId: String, expression: String, result: Int,
        senderName: String, isPublic: Boolean
    ) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val content = "\uD83C\uDFB2 $expression \u2192 **$result**"
            messageRepository.sendMessage(ChatMessage(
                senderId = userId, senderName = senderName, campaignId = campaignId,
                content = content, type = MessageType.DICE_ROLL, isPublic = isPublic
            ))
        }
    }

    fun setWhisperTarget(player: PlayerInfo?) {
        _whisperTarget.value = player
    }

    /**
     * Load the player list for whisper picker.
     * Fetches real nicknames from Firestore users collection
     * instead of deriving them from message history.
     */
    fun loadCampaignPlayers(campaignId: String, currentUserName: String) {
        viewModelScope.launch {
            campaignRepository.getCampaign(campaignId)
                .catch { }
                .collect { campaign ->
                    if (campaign == null) return@collect
                    val myId = authRepository.currentUserId ?: return@collect
                    _campaignGmId.value = campaign.gmId

                    // Collect all member IDs except current user
                    val memberIds = (listOf(campaign.gmId) + campaign.coGmIds + campaign.playerIds)
                        .distinct()
                        .filter { it != myId }

                    // Fetch real nicknames from Firestore
                    val players = memberIds.mapNotNull { userId ->
                        val user = authRepository.getUser(userId)
                        val nickname = user?.nickname ?: userId.take(8)
                        val isGmOrCoGm = userId == campaign.gmId || userId in campaign.coGmIds
                        PlayerInfo(
                            userId = userId,
                            name   = if (isGmOrCoGm) "\u2694 $nickname" else nickname
                        )
                    }
                    _campaignPlayers.value = players
                }
        }
    }
}
