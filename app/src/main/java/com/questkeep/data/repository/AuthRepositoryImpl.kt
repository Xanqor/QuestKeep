package com.questkeep.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.questkeep.domain.entities.User
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val currentUserId: String? get() = auth.currentUser?.uid
    override val isLoggedIn: Boolean get() = auth.currentUser != null

    /**
     * Observes auth state AND Firestore user document in real-time.
     * Emits a new User whenever nickname or avatarEmoji changes in Firestore.
     */
    override fun observeAuthState(): Flow<User?> = callbackFlow {
        var firestoreListener: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            // Remove any existing Firestore listener first
            firestoreListener?.remove()
            firestoreListener = null

            if (firebaseUser != null) {
                // Listen to user document for real-time profile changes
                firestoreListener = firestore
                    .collection(Constants.COLLECTION_USERS)
                    .document(firebaseUser.uid)
                    .addSnapshotListener { doc, error ->
                        if (error != null || doc == null) { trySend(null); return@addSnapshotListener }
                        trySend(User(
                            id          = firebaseUser.uid,
                            email       = doc.getString("email") ?: firebaseUser.email ?: "",
                            nickname    = doc.getString("nickname") ?: firebaseUser.displayName ?: "Player",
                            avatarEmoji = doc.getString("avatarEmoji") ?: "🧙",
                            avatarUrl   = doc.getString("avatarUrl")
                        ))
                    }
            } else {
                trySend(null)
            }
        }

        auth.addAuthStateListener(authListener)
        awaitClose {
            auth.removeAuthStateListener(authListener)
            firestoreListener?.remove()
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Auth failed"))
            Result.success(fetchUser(uid))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun register(email: String, password: String, nickname: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("Registration failed"))
            val user = User(id = uid, email = email, nickname = nickname, avatarEmoji = "🧙")
            firestore.collection(Constants.COLLECTION_USERS).document(uid).set(mapOf(
                "email" to email, "nickname" to nickname,
                "avatarEmoji" to "🧙", "createdAt" to user.createdAt
            )).await()
            Result.success(user)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Google sign-in failed"))
            val uid = firebaseUser.uid
            val docRef = firestore.collection(Constants.COLLECTION_USERS).document(uid)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                docRef.set(mapOf(
                    "email" to (firebaseUser.email ?: ""),
                    "nickname" to (firebaseUser.displayName ?: "Player"),
                    "avatarEmoji" to "🧙",
                    "createdAt" to System.currentTimeMillis()
                )).await()
            }
            Result.success(fetchUser(uid))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateProfile(nickname: String, avatarEmoji: String): Result<Unit> {
        return try {
            val uid = currentUserId ?: return Result.failure(Exception("Not logged in"))
            firestore.collection(Constants.COLLECTION_USERS).document(uid)
                .update(mapOf("nickname" to nickname.trim(), "avatarEmoji" to avatarEmoji))
                .await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateAvatarUrl(url: String): Result<Unit> {
        return try {
            val uid = currentUserId ?: return Result.failure(Exception("Not logged in"))
            firestore.collection(Constants.COLLECTION_USERS).document(uid)
                .update(mapOf("avatarUrl" to url))
                .await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun logout() { auth.signOut() }

    override suspend fun getCurrentUser(): User? {
        val uid = currentUserId ?: return null
        return try { fetchUser(uid) } catch (e: Exception) { null }
    }

    override suspend fun getUser(userId: String): User? =
        try { fetchUser(userId) } catch (e: Exception) { null }

    private suspend fun fetchUser(uid: String): User {
        val doc = firestore.collection(Constants.COLLECTION_USERS).document(uid).get().await()
        return User(
            id          = uid,
            email       = doc.getString("email") ?: "",
            nickname    = doc.getString("nickname") ?: "Player",
            avatarEmoji = doc.getString("avatarEmoji") ?: "🧙",
            avatarUrl   = doc.getString("avatarUrl")
        )
    }
}
