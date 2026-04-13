package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.domain.entities.Character
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.domain.repository.CharacterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class CharacterSheetUiState {
    object Loading : CharacterSheetUiState()
    data class Success(val character: Character) : CharacterSheetUiState()
    object NoCampaign : CharacterSheetUiState()
    object NoCharacter : CharacterSheetUiState()
    data class GmPicker(val characters: List<Character>) : CharacterSheetUiState()
    data class Error(val message: String) : CharacterSheetUiState()
}

@HiltViewModel
class CharacterSheetViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CharacterSheetUiState>(CharacterSheetUiState.Loading)
    val uiState: StateFlow<CharacterSheetUiState> = _uiState.asStateFlow()

    private val _showDamageDialog = MutableStateFlow(false)
    val showDamageDialog: StateFlow<Boolean> = _showDamageDialog.asStateFlow()

    private val _showArmorDialog = MutableStateFlow(false)
    val showArmorDialog: StateFlow<Boolean> = _showArmorDialog.asStateFlow()

    private val _showGmEditSheet = MutableStateFlow(false)
    val showGmEditSheet: StateFlow<Boolean> = _showGmEditSheet.asStateFlow()

    private val _showPlayerEditSheet = MutableStateFlow(false)
    val showPlayerEditSheet: StateFlow<Boolean> = _showPlayerEditSheet.asStateFlow()

    private var currentCharacter: Character? = null
    private var isGmMode = false
    private var loadedCampaignId: String? = null
    private var loadingJob: Job? = null

    fun loadCharacter(campaignId: String?, isGm: Boolean = false) {
        if (campaignId == null) { _uiState.value = CharacterSheetUiState.NoCampaign; return }
        if (campaignId == loadedCampaignId && isGm == isGmMode &&
            _uiState.value !is CharacterSheetUiState.Loading) return

        loadedCampaignId = campaignId
        isGmMode = isGm
        loadingJob?.cancel()
        _uiState.value = CharacterSheetUiState.Loading

        loadingJob = viewModelScope.launch {
            if (isGm) {
                characterRepository.getCharactersForCampaign(campaignId)
                    .catch { _uiState.value = CharacterSheetUiState.GmPicker(emptyList()) }
                    .collect { list ->
                        val currentId = currentCharacter?.id
                        if (currentId != null) {
                            val updated = list.find { it.id == currentId }
                            if (updated != null) {
                                currentCharacter = updated
                                _uiState.value = CharacterSheetUiState.Success(updated)
                                return@collect
                            }
                        }
                        _uiState.value = CharacterSheetUiState.GmPicker(list)
                    }
            } else {
                val userId = authRepository.currentUserId ?: return@launch
                characterRepository.getCharacter(campaignId, userId)
                    .catch { _uiState.value = CharacterSheetUiState.Error(it.message ?: "Error") }
                    .collect { character ->
                        if (character == null) _uiState.value = CharacterSheetUiState.NoCharacter
                        else { currentCharacter = character; _uiState.value = CharacterSheetUiState.Success(character) }
                    }
            }
        }
    }

    fun gmSelectCharacter(character: Character) {
        currentCharacter = character
        _uiState.value = CharacterSheetUiState.Success(character)
    }

    fun gmBackToPicker(campaignId: String) {
        currentCharacter = null
        loadedCampaignId = null
        loadCharacter(campaignId, isGm = true)
    }

    fun createCharacter(campaignId: String, name: String, className: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            characterRepository.saveCharacter(Character(
                id = UUID.randomUUID().toString(),
                name = name, className = className,
                campaignId = campaignId, playerId = userId,
                maxHp = 10, currentHp = 10
            ))
        }
    }

    fun applyDamage(amount: Int) {
        val c = currentCharacter ?: return
        updateCharacter(c.copy(currentHp = (c.currentHp - amount).coerceAtLeast(0)))
    }

    fun applyHealing(amount: Int) {
        val c = currentCharacter ?: return
        updateCharacter(c.copy(currentHp = (c.currentHp + amount).coerceAtMost(c.maxHp)))
    }

    fun applyArmorDamage(amount: Int) {
        val c = currentCharacter ?: return
        updateCharacter(c.copy(armor = (c.armor - amount).coerceAtLeast(0)))
    }

    fun applyArmorRepair(amount: Int) {
        val c = currentCharacter ?: return
        updateCharacter(c.copy(armor = (c.armor + amount).coerceAtMost(c.maxArmor)))
    }

    fun updateInspirationPoints(delta: Int) {
        val c = currentCharacter ?: return
        updateCharacter(c.copy(inspirationPoints = (c.inspirationPoints + delta).coerceIn(0, 6)))
    }

    fun awardXp(amount: Int) {
        val c = currentCharacter ?: return
        updateCharacter(c.copy(experiencePoints = (c.experiencePoints + amount).coerceAtLeast(0)))
    }

    fun updateResource(resourceName: String, delta: Float) {
        val c = currentCharacter ?: return
        fun coerce(v: Float, max: Float) = (v + delta).coerceIn(0f, max)
        val updated = when (resourceName) {
            "quar"   -> c.copy(quarResource   = coerce(c.quarResource,   c.maxQuarResource))
            "ombra"  -> c.copy(ombraResource  = coerce(c.ombraResource,  c.maxOmbraResource))
            "veruna" -> c.copy(verunaResource = coerce(c.verunaResource, c.maxVerunaResource))
            "sumeni" -> c.copy(sumeniResource = coerce(c.sumeniResource, c.maxSumeniResource))
            else     -> c
        }
        updateCharacter(updated)
    }

    fun gmFullUpdateCharacter(updated: Character) = updateCharacter(updated)

    fun openDamageDialog()  { _showDamageDialog.value = true  }
    fun closeDamageDialog() { _showDamageDialog.value = false }
    fun openArmorDialog()   { _showArmorDialog.value  = true  }
    fun closeArmorDialog()  { _showArmorDialog.value  = false }
    fun openGmEditSheet()     { _showGmEditSheet.value     = true  }
    fun closeGmEditSheet()    { _showGmEditSheet.value     = false }
    fun openPlayerEditSheet() { _showPlayerEditSheet.value = true  }
    fun closePlayerEditSheet(){ _showPlayerEditSheet.value = false }

    fun playerUpdateCharacter(updated: Character) = updateCharacter(updated)

    val currentCharacterSnapshot: Character? get() = currentCharacter

    private fun updateCharacter(character: Character) {
        currentCharacter = character
        _uiState.value = CharacterSheetUiState.Success(character)
        viewModelScope.launch { characterRepository.updateCharacter(character) }
    }
}
