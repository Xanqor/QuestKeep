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

    private val _selectedTab = MutableStateFlow(0) // 0: D&D, 1: Nexur
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // D&D state
    private val _selectedDndDie = MutableStateFlow(20)
    val selectedDndDie: StateFlow<Int> = _selectedDndDie.asStateFlow()

    private val _dndCount = MutableStateFlow(1)
    val dndCount: StateFlow<Int> = _dndCount.asStateFlow()

    private val _dndModifier = MutableStateFlow(0)
    val dndModifier: StateFlow<Int> = _dndModifier.asStateFlow()

    private val _lastDndResult = MutableStateFlow<com.questkeep.utils.DndRollResult?>(null)
    val lastDndResult: StateFlow<com.questkeep.utils.DndRollResult?> = _lastDndResult.asStateFlow()

    // Nexur state
    private val _nexurPoolSize = MutableStateFlow(4)
    val nexurPoolSize: StateFlow<Int> = _nexurPoolSize.asStateFlow()

    private val _lastNexurResult = MutableStateFlow<com.questkeep.utils.NexurRollResult?>(null)
    val lastNexurResult: StateFlow<com.questkeep.utils.NexurRollResult?> = _lastNexurResult.asStateFlow()

    // Legacy custom expression
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

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    // ── D&D functions ──────────────────────────────────────────────────────────
    fun setDndDie(sides: Int) { _selectedDndDie.value = sides }
    fun setDndCount(count: Int) { _dndCount.value = count.coerceIn(1, 30) }
    fun changeDndCount(delta: Int) { setDndCount(_dndCount.value + delta) }
    fun setDndModifier(mod: Int) { _dndModifier.value = mod.coerceIn(-99, 99) }
    fun changeDndModifier(delta: Int) { setDndModifier(_dndModifier.value + delta) }

    fun rollDnd(): com.questkeep.utils.DndRollResult {
        val result = DiceParser.rollDnd(
            sides = _selectedDndDie.value,
            count = _dndCount.value,
            modifier = _dndModifier.value
        )
        _lastDndResult.value = result
        return result
    }

    fun rollDndAndPost(
        campaignId: String?,
        senderName: String,
        isPublic: Boolean,
        privateTargetId: String? = null
    ) {
        val result = rollDnd()
        if (campaignId == null) return
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val modStr = when {
                result.modifier > 0 -> " + ${result.modifier}"
                result.modifier < 0 -> " - ${-result.modifier}"
                else -> ""
            }
            val rollsStr = if (result.rolls.size > 1) " [${result.rolls.joinToString(", ")}]" else ""
            val critStr = when {
                result.isNat20 -> "  🔥 КРИТИЧЕСКИЙ УСПЕХ!"
                result.isNat1 -> "  💀 КРИТИЧЕСКИЙ ПРОВАЛ!"
                else -> ""
            }
            val content = "🎲 **D&D (${result.count}d${result.sides}$modStr)**$rollsStr → **${result.total}**$critStr"
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

    // ── Nexur functions ────────────────────────────────────────────────────────
    fun setNexurPoolSize(size: Int) { _nexurPoolSize.value = size.coerceIn(1, 30) }
    fun changeNexurPoolSize(delta: Int) { setNexurPoolSize(_nexurPoolSize.value + delta) }

    fun rollNexur(): com.questkeep.utils.NexurRollResult {
        val result = DiceParser.rollNexur(_nexurPoolSize.value)
        _lastNexurResult.value = result
        return result
    }

    fun rollNexurAndPost(
        campaignId: String?,
        senderName: String,
        isPublic: Boolean,
        privateTargetId: String? = null
    ) {
        val result = rollNexur()
        if (campaignId == null) return
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val icons = mapOf(1 to "⚀", 2 to "⚁", 3 to "⚂", 4 to "⚃", 5 to "⚄", 6 to "⚅")
            val diceStr = result.dice.joinToString(" ") { die ->
                val icon = icons[die.face] ?: "${die.face}"
                val pts = when (die.successes) {
                    2 -> "+2★"
                    1 -> "+1"
                    else -> "0"
                }
                "[$icon $pts]"
            }
            val triumphsNote = if (result.triumphsCount > 0) " (Триумфов: ${result.triumphsCount}🌟)" else ""
            val content = "🎲 **Nexur Pool: ${result.poolSize}d6** → **${result.totalSuccesses} успехов**$triumphsNote\n$diceStr"

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

    // ── Legacy custom roll support ─────────────────────────────────────────────
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
