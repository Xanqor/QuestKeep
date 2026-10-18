package com.questkeep.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.domain.entities.PlayerNote
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.PlayerNoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerNotesScreen(
    onBack: () -> Unit,
    viewModel: PlayerNoteViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    val notes by viewModel.notes.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotes()
    }

    Scaffold(
        containerColor = t.colors.bg,
        topBar = {
            TopAppBar(
                title = { Text("📝 Мои заметки", color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = t.colors.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = t.colors.panel)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.startCreating() },
                containerColor = t.colors.accent,
                contentColor = t.colors.bg,
                shape = RoundedCornerShape(t.corner)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Новая заметка")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Нет заметок. Нажмите + чтобы создать.",
                    color = t.colors.muted,
                    fontFamily = t.body,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onEdit = { viewModel.startEditing(note) },
                        onDelete = { viewModel.deleteNote(note) }
                    )
                }
            }
        }
    }

    error?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearError()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = t.colors.hot,
            contentColor = t.colors.bg
        ) { Text(msg, fontFamily = t.body) }
    }

    if (editingNote != null) {
        NoteEditSheet(
            note = editingNote!!,
            isSaving = isSaving,
            onDismiss = { viewModel.cancelEdit() },
            onSave = { title, content -> viewModel.saveNote(title, content) }
        )
    }
}

@Composable
private fun NoteCard(
    note: PlayerNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val t = LocalXanteya.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateStr = remember(note.updatedAt) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(t.corner),
        color = t.colors.panel,
        border = BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (note.title.isNotBlank()) {
                        Text(
                            note.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = t.colors.text,
                            fontFamily = t.display,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (note.content.isNotBlank()) {
                        Text(
                            note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = t.colors.muted,
                            fontFamily = t.body,
                            maxLines = 4
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать", tint = t.colors.accent)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = t.colors.hot)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = t.colors.muted, fontFamily = t.body)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = t.colors.panel,
            shape = RoundedCornerShape(t.corner),
            title = { Text("Удалить заметку?", color = t.colors.text, fontFamily = t.display) },
            text = { Text("Это действие нельзя отменить.", color = t.colors.muted, fontFamily = t.body) },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = t.colors.hot, contentColor = t.colors.bg),
                    shape = RoundedCornerShape(t.corner)
                ) { Text("Удалить", fontFamily = t.display) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена", color = t.colors.muted, fontFamily = t.body)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditSheet(
    note: PlayerNote,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit
) {
    val t = LocalXanteya.current
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = t.colors.accent,
        unfocusedBorderColor = t.colors.line,
        focusedTextColor = t.colors.text,
        unfocusedTextColor = t.colors.text,
        focusedLabelColor = t.colors.accent,
        unfocusedLabelColor = t.colors.muted,
        cursorColor = t.colors.accent
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        dragHandle = { BottomSheetDefaults.DragHandle(color = t.colors.muted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val isNew = note.title.isBlank() && note.content.isBlank()
            Text(
                if (isNew) "Новая заметка" else "Редактировать заметку",
                style = MaterialTheme.typography.titleLarge,
                color = t.colors.text,
                fontFamily = t.display,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Заголовок", color = t.colors.muted, fontFamily = t.body) },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
                singleLine = true
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Содержание", color = t.colors.muted, fontFamily = t.body) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                colors = fieldColors,
                maxLines = 10
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(t.corner),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.muted),
                    border = BorderStroke(1.dp, t.colors.line)
                ) { Text("Отмена", fontFamily = t.display) }

                Button(
                    onClick = { onSave(title, content) },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving && (title.isNotBlank() || content.isNotBlank()),
                    shape = RoundedCornerShape(t.corner),
                    colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = t.colors.bg)
                    else Text("Сохранить", fontFamily = t.display)
                }
            }
        }
    }
}
