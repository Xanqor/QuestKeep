package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.questkeep.auth.AuthState
import com.questkeep.domain.entities.PlayerNote
import com.questkeep.domain.repository.AuthRepository
import com.questkeep.domain.repository.PlayerNoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerNoteViewModel @Inject constructor(
    private val noteRepository: PlayerNoteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<PlayerNote>>(emptyList())
    val notes: StateFlow<List<PlayerNote>> = _notes.asStateFlow()

    private val _editingNote = MutableStateFlow<PlayerNote?>(null)
    val editingNote: StateFlow<PlayerNote?> = _editingNote.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentLoadedUserId: String? = null

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                if (user != null) {
                    loadNotesForUser(user.id)
                }
            }
        }
    }

    fun loadNotes() {
        val userId = authRepository.currentUserId ?: return
        loadNotesForUser(userId)
    }

    private fun loadNotesForUser(userId: String) {
        if (currentLoadedUserId == userId && _notes.value.isNotEmpty()) return
        currentLoadedUserId = userId
        viewModelScope.launch {
            noteRepository.getNotes(userId)
                .catch { _error.value = it.message }
                .collect { _notes.value = it }
        }
    }

    fun startCreating() {
        val userId = authRepository.currentUserId ?: return
        _editingNote.value = PlayerNote(userId = userId)
    }

    fun startEditing(note: PlayerNote) {
        _editingNote.value = note
    }

    fun cancelEdit() {
        _editingNote.value = null
    }

    fun saveNote(title: String, content: String) {
        val base = _editingNote.value ?: return
        val userId = authRepository.currentUserId ?: return
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            _isSaving.value = true
            val now = System.currentTimeMillis()
            val noteId = if (base.id.isNotBlank()) base.id else java.util.UUID.randomUUID().toString()
            val note = base.copy(
                id = noteId,
                userId = userId,
                title = title.trim(),
                content = content.trim(),
                updatedAt = now,
                createdAt = if (base.createdAt > 0L) base.createdAt else now
            )

            // Instant optimistic in-memory update
            val currentList = _notes.value.filter { it.id != note.id }.toMutableList()
            currentList.add(0, note)
            _notes.value = currentList
            _editingNote.value = null

            noteRepository.saveNote(note)
                .onFailure { _error.value = it.message }
            _isSaving.value = false
        }
    }

    fun deleteNote(note: PlayerNote) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            _notes.value = _notes.value.filter { it.id != note.id }
            noteRepository.deleteNote(note.id, userId)
                .onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }
}
