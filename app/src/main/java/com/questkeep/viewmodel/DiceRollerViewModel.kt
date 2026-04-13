package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.domain.entities.ChatMessage
import com.questkeep.domain.entities.MessageType
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.domain.repository.MessageRepository
import com.questkeep.utils.DiceParser
import com.questkeep.utils.DiceRollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DicePreset(val label: String, val expression: String)

@HiltViewModel
class DiceRollerViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _lastResult = MutableStateFlow<DiceRollResult?>(null)
    val lastResult: StateFlow<DiceRollResult?> = _lastResult.asStateFlow()

    private val _rollHistory = MutableStateFlow<List<DiceRollResult>>(emptyList())
    val rollHistory: StateFlow<List<DiceRollResult>> = _rollHistory.asStateFlow()

    private val _presets = MutableStateFlow<List<DicePreset>>(emptyList())
    val presets: StateFlow<List<DicePreset>> = _presets.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setExpression(expr: String) {
        _expression.value = expr
        _error.value = null
    }

    fun roll(): DiceRollResult? {
        val result = DiceParser.parse(_expression.value)
        if (result == null) {
            _error.value = "Invalid expression (e.g. 2d6+3)"
            return null
        }
        _lastResult.value = result
        _rollHistory.value = listOf(result) + _rollHistory.value.take(9)
        return result
    }

    /**
     * Rolls and posts result to chat.
     * @param privateTargetId the GM userId — set when isPublic=false so the GM can see
     *        player's secret roll (stored as targetPlayerId in the message).
     */
    fun rollAndPost(
        campaignId: String?,
        senderName: String,
        isPublic: Boolean,
        privateTargetId: String? = null
    ) {
        val result = roll() ?: return
        if (campaignId == null) return
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val content = "🎲 ${result.expression} → **${result.total}**" +
                if (result.rolls.size > 1) " [${result.rolls.joinToString(", ")}]" else ""
            messageRepository.sendMessage(
                ChatMessage(
                    senderId = userId,
                    senderName = senderName,
                    campaignId = campaignId,
                    content = content,
                    type = MessageType.DICE_ROLL,
                    isPublic = isPublic,
                    targetPlayerId = if (isPublic) null else privateTargetId
                )
            )
        }
    }

    fun quickRoll(sides: Int) {
        _expression.value = "d$sides"
        roll()
    }

    fun addPreset(label: String) {
        val expr = _expression.value.trim()
        if (expr.isEmpty() || DiceParser.parse(expr) == null) return
        _presets.value = _presets.value + DicePreset(label = label.ifBlank { expr }, expression = expr)
    }

    fun removePreset(preset: DicePreset) {
        _presets.value = _presets.value - preset
    }
}
