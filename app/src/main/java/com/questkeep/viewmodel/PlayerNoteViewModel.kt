package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        authRepository.currentUserId?.let { userId ->
            viewModelScope.launch {
                noteRepository.getNotes(userId)
                    .catch { _error.value = it.message }
                    .collect { _notes.value = it }
            }
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
            val note = base.copy(
                userId = userId,
                title = title.trim(),
                content = content.trim()
            )
            noteRepository.saveNote(note)
                .onFailure { _error.value = it.message }
            _isSaving.value = false
            _editingNote.value = null
        }
    }

    fun deleteNote(note: PlayerNote) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            noteRepository.deleteNote(note.id, userId)
                .onFailure { _error.value = it.message }
        }
    }

    fun clearError() { _error.value = null }
}
