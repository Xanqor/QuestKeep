package com.questkeep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.questkeep.domain.entities.GmNote
import com.questkeep.domain.entities.NoteType
import com.questkeep.domain.repository.GmNoteRepository
import com.questkeep.utils.NotesExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class GmNotesUiState {
    object Loading : GmNotesUiState()
    data class Success(val notes: List<GmNote>) : GmNotesUiState()
    data class Error(val message: String) : GmNotesUiState()
}

@HiltViewModel
class GmNotesViewModel @Inject constructor(
    private val noteRepository: GmNoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GmNotesUiState>(GmNotesUiState.Loading)
    val uiState: StateFlow<GmNotesUiState> = _uiState.asStateFlow()

    private val _editingNote = MutableStateFlow<GmNote?>(null)
    val editingNote: StateFlow<GmNote?> = _editingNote.asStateFlow()

    private val _expandedNoteId = MutableStateFlow<String?>(null)
    val expandedNoteId: StateFlow<String?> = _expandedNoteId.asStateFlow()

    private var currentCampaignId: String? = null

    fun loadNotes(campaignId: String) {
        if (campaignId == currentCampaignId) return
        currentCampaignId = campaignId
        viewModelScope.launch {
            noteRepository.getNotes(campaignId)
                .catch { _uiState.value = GmNotesUiState.Error(it.message ?: "Error") }
                .collect { _uiState.value = GmNotesUiState.Success(it) }
        }
    }

    fun startCreating(parentId: String? = null, type: NoteType = NoteType.OTHER) {
        _editingNote.value = GmNote(
            id = UUID.randomUUID().toString(),
            campaignId = currentCampaignId ?: "",
            parentId = parentId,
            type = type
        )
    }

    fun startEditing(note: GmNote) { _editingNote.value = note }
    fun cancelEdit() { _editingNote.value = null }

    fun saveNote(note: GmNote) {
        viewModelScope.launch {
            val state = _uiState.value
            val isNew = state !is GmNotesUiState.Success ||
                state.notes.none { it.id == note.id }
            if (isNew) noteRepository.saveNote(note)
            else noteRepository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            _editingNote.value = null
        }
    }

    fun deleteNote(note: GmNote) {
        viewModelScope.launch {
            val cid = currentCampaignId ?: return@launch
            noteRepository.deleteNote(note.id, cid)
        }
    }

    fun toggleReveal(note: GmNote) {
        viewModelScope.launch {
            noteRepository.updateNote(note.copy(isRevealedToPlayers = !note.isRevealedToPlayers))
        }
    }

    fun toggleExpand(noteId: String) {
        _expandedNoteId.value = if (_expandedNoteId.value == noteId) null else noteId
    }

    fun exportNotesJson(): String {
        val notes = (_uiState.value as? GmNotesUiState.Success)?.notes ?: emptyList()
        return Gson().toJson(NotesExport(notes = notes))
    }

    fun importNotesFromJson(json: String) {
        val cid = currentCampaignId ?: return
        try {
            val export = Gson().fromJson(json, NotesExport::class.java) ?: return
            viewModelScope.launch {
                export.notes.forEach { note ->
                    noteRepository.saveNote(note.copy(id = UUID.randomUUID().toString(), campaignId = cid))
                }
            }
        } catch (_: Exception) {}
    }
}
