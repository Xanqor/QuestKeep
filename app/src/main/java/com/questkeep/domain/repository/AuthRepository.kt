package com.questkeep.domain.repository

import com.questkeep.domain.entities.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: String?
    val isLoggedIn: Boolean
    fun observeAuthState(): Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, nickname: String): Result<User>
    suspend fun logout()
    suspend fun getCurrentUser(): User?
    suspend fun getUser(userId: String): User?
    suspend fun updateProfile(nickname: String, avatarEmoji: String): Result<Unit>
    suspend fun updateAvatarUrl(url: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<User>
}
