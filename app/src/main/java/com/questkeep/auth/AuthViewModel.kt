package com.questkeep.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.domain.entities.User
import com.questkeep.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
}

sealed class AuthEvent {
    data class Error(val message: String) : AuthEvent()
    object RegisterSuccess : AuthEvent()
    object LoginSuccess : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _authState.value = if (user != null) AuthState.Authenticated(user)
                                   else AuthState.Unauthenticated
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch { _events.emit(AuthEvent.Error("Заполните все поля")) }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.login(email.trim(), password)
            _isLoading.value = false
            result.onSuccess { _events.emit(AuthEvent.LoginSuccess) }
            result.onFailure { _events.emit(AuthEvent.Error(it.message ?: "Ошибка входа")) }
        }
    }

    fun register(email: String, password: String, nickname: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank() || nickname.isBlank() || confirmPassword.isBlank()) {
            viewModelScope.launch { _events.emit(AuthEvent.Error("Заполните все поля")) }
            return
        }
        if (password != confirmPassword) {
            viewModelScope.launch { _events.emit(AuthEvent.Error("Пароли не совпадают")) }
            return
        }
        if (password.length < 8) {
            viewModelScope.launch { _events.emit(AuthEvent.Error("Пароль минимум 8 символов")) }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.register(email.trim(), password, nickname.trim())
            _isLoading.value = false
            result.onSuccess { _events.emit(AuthEvent.RegisterSuccess) }
            result.onFailure { _events.emit(AuthEvent.Error(it.message ?: "Ошибка регистрации")) }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.signInWithGoogle(idToken)
            _isLoading.value = false
            result.onSuccess { _events.emit(AuthEvent.LoginSuccess) }
            result.onFailure { _events.emit(AuthEvent.Error(it.message ?: "Ошибка Google входа")) }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
