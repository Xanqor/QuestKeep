package com.questkeep.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.domain.entities.GmNote
import com.questkeep.domain.entities.NoteType
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.GmNotesUiState
import com.questkeep.viewmodel.GmNotesViewModel

private fun NoteType.emoji() = when (this) {
    NoteType.WORLD -> "\uD83C\uDF0D"
    NoteType.CITY  -> "\uD83C\uDFD9\uFE0F"
    NoteType.NPC   -> "\uD83D\uDC64"
    NoteType.PLOT  -> "\uD83D\uDCD6"
    NoteType.OTHER -> "\uD83D\uDCDD"
}

private fun NoteType.label() = when (this) {
    NoteType.WORLD -> "Мир"
    NoteType.CITY  -> "Город"
    NoteType.NPC   -> "НПС"
    NoteType.PLOT  -> "Сюжет"
    NoteType.OTHER -> "Прочее"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GmNotesScreen(
    campaignId: String?,
    onBack: () -> Unit,
    viewModel: GmNotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()

    LaunchedEffect(campaignId) {
        campaignId?.let { viewModel.loadNotes(it) }
    }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = viewModel.exportNotesJson()
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return@rememberLauncherForActivityResult
        viewModel.importNotesFromJson(json)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Заметки ГМ", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("notes.json") }) {
                        Icon(Icons.Filled.Upload, contentDescription = "Экспорт", tint = TextSecondary)
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Filled.Download, contentDescription = "Импорт", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.startCreating() }, containerColor = PrimaryPurple) {
                Icon(Icons.Filled.Add, contentDescription = "Новая заметка", tint = OnPrimary)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is GmNotesUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryPurple)
                is GmNotesUiState.Error ->
                    Text(state.message, color = AccentRed,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp))
                is GmNotesUiState.Success -> {
                    if (campaignId == null) {
                        Text("Кампания не выбрана", color = TextSecondary, modifier = Modifier.align(Alignment.Center))
                    } else if (state.notes.isEmpty()) {
                        Column(modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Заметок пока нет", color = TextSecondary)
                            Text("Нажмите + чтобы создать первую", color = TextDisabled,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        val rootNotes = state.notes.filter { it.parentId == null }
                        val childMap = state.notes.filter { it.parentId != null }.groupBy { it.parentId!! }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            rootNotes.forEach { root ->
                                item(key = root.id) {
                                    GmNoteCard(note = root, indent = 0,
                                        onEdit = { viewModel.startEditing(root) },
                                        onDelete = { viewModel.deleteNote(root) },
                                        onAddChild = { viewModel.startCreating(parentId = root.id) })
                                }
                                childMap[root.id]?.forEach { child ->
                                    item(key = child.id) {
                                        GmNoteCard(note = child, indent = 1,
                                            onEdit = { viewModel.startEditing(child) },
                                            onDelete = { viewModel.deleteNote(child) },
                                            onAddChild = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingNote != null) {
        NoteEditorSheet(
            note = editingNote!!,
            onSave = { updated -> viewModel.saveNote(updated) },
            onDismiss = { viewModel.cancelEdit() }
        )
    }
}

@Composable
private fun GmNoteCard(
    note: GmNote,
    indent: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddChild: (() -> Unit)?
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        if (indent > 0) Spacer(Modifier.width((indent * 20).dp))
        Column(modifier = Modifier.weight(1f)
            .background(DarkCard, RoundedCornerShape(10.dp))
            .padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(note.type.emoji(), fontSize = 16.sp)
                Text(note.title.ifBlank { "Без названия" }, color = TextPrimary,
                    fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f))
                if (note.isRevealedToPlayers)
                    Text("\uD83D\uDC41\uFE0F", fontSize = 14.sp)
            }
            if (note.content.isNotBlank()) {
                Text(note.content, color = TextSecondary, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.End) {
                if (onAddChild != null) {
                    TextButton(onClick = onAddChild,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("+ Дочерняя", color = PrimaryPurpleLight, fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Редактировать",
                        tint = SecondaryGold, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить",
                        tint = AccentRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorSheet(
    note: GmNote,
    onSave: (GmNote) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var selectedType by remember(note.id) { mutableStateOf(note.type) }
    var revealed by remember(note.id) { mutableStateOf(note.isRevealedToPlayers) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PrimaryPurple, unfocusedBorderColor = DarkSurfaceVariant,
        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
        focusedLabelColor = PrimaryPurpleLight, unfocusedLabelColor = TextSecondary
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = DarkCard,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(if (note.title.isBlank()) "Новая заметка" else "Редактировать заметку",
                    color = SecondaryGold, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
            }
        }
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it },
                label = { Text("Заголовок") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = fieldColors)
            OutlinedTextField(value = content, onValueChange = { content = it },
                label = { Text("Содержание") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                minLines = 4, colors = fieldColors)
            Text("Тип", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NoteType.entries.forEach { type ->
                    FilterChip(selected = selectedType == type, onClick = { selectedType = type },
                        label = { Text("${type.emoji()} ${type.label()}", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryPurpleDark, selectedLabelColor = PrimaryPurpleLight,
                            containerColor = DarkSurface, labelColor = TextSecondary))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (revealed) "Показывается игрокам" else "Скрыто от игроков",
                    color = if (revealed) AccentGreen else TextSecondary)
                Switch(checked = revealed, onCheckedChange = { revealed = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen))
            }
            Button(
                onClick = {
                    onSave(note.copy(title = title.trim(), content = content.trim(),
                        type = selectedType, isRevealedToPlayers = revealed,
                        updatedAt = System.currentTimeMillis()))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) { Text("Сохранить", fontWeight = FontWeight.Bold) }
        }
    }
}
