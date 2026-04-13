package com.questkeep.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.domain.entities.User
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.domain.repository.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiEvent {
    object SaveSuccess : ProfileUiEvent()
    data class Error(val message: String) : ProfileUiEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()

    private val _events = MutableSharedFlow<ProfileUiEvent>()
    val events: SharedFlow<ProfileUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            _user.value = authRepository.getCurrentUser()
        }
    }

    fun saveProfile(nickname: String, avatarEmoji: String) {
        if (nickname.isBlank()) {
            viewModelScope.launch { _events.emit(ProfileUiEvent.Error("Никнейм не может быть пустым")) }
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val result = authRepository.updateProfile(nickname.trim(), avatarEmoji)
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _user.value = _user.value?.copy(nickname = nickname.trim(), avatarEmoji = avatarEmoji)
                    _events.emit(ProfileUiEvent.SaveSuccess)
                },
                onFailure = { _events.emit(ProfileUiEvent.Error(it.message ?: "Ошибка сохранения")) }
            )
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            imageRepository.uploadImage(uri).fold(
                onSuccess = { url ->
                    authRepository.updateAvatarUrl(url).fold(
                        onSuccess = { _user.value = _user.value?.copy(avatarUrl = url) },
                        onFailure = { _events.emit(ProfileUiEvent.Error(it.message ?: "Ошибка сохранения")) }
                    )
                },
                onFailure = { _events.emit(ProfileUiEvent.Error(it.message ?: "Ошибка загрузки фото")) }
            )
            _isUploadingAvatar.value = false
        }
    }
}
