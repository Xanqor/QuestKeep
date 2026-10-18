package com.questkeep.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.domain.entities.NpcCard
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.BestiaryUiState
import com.questkeep.viewmodel.BestiaryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BestiaryScreen(
    campaignId: String?,
    onBack: () -> Unit,
    viewModel: BestiaryViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    val uiState by viewModel.uiState.collectAsState()
    val editingNpc by viewModel.editingNpc.collectAsState()
    val deletingNpc by viewModel.deletingNpc.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Local editor state
    var editorName by remember { mutableStateOf("") }
    var editorDescription by remember { mutableStateOf("") }
    var editorHp by remember { mutableStateOf("") }
    var editorArmor by remember { mutableStateOf("") }
    var editorDamage by remember { mutableStateOf("") }
    var editorTags by remember { mutableStateOf("") }

    // Sync editor fields when the editing NPC changes
    LaunchedEffect(editingNpc) {
        editorName        = editingNpc?.name        ?: ""
        editorDescription = editingNpc?.description ?: ""
        editorHp          = editingNpc?.hp?.toString()    ?: ""
        editorArmor       = editingNpc?.armor?.toString() ?: ""
        editorDamage      = editingNpc?.damage      ?: ""
        editorTags        = editingNpc?.tags?.joinToString(", ") ?: ""
    }

    LaunchedEffect(campaignId) {
        if (campaignId != null) viewModel.loadNpcs(campaignId)
    }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = viewModel.exportNpcsJson()
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return@rememberLauncherForActivityResult
        viewModel.importNpcsFromJson(json)
    }

    Scaffold(
        containerColor = t.colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Бестиарий",
                        color = t.colors.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = t.colors.text
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("npcs.json") }) {
                        Icon(Icons.Filled.Upload, contentDescription = "Экспорт", tint = t.colors.accent)
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Filled.Download, contentDescription = "Импорт", tint = t.colors.accent)
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
                Icon(Icons.Filled.Add, contentDescription = "Добавить NPC")
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is BestiaryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = t.colors.accent)
                }
            }
            is BestiaryUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = t.colors.hot, fontSize = 16.sp, fontFamily = t.body)
                }
            }
            is BestiaryUiState.Success -> {
                if (state.npcs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📜", fontSize = 48.sp)
                            Text(
                                "Бестиарий пуст",
                                color = t.colors.text,
                                fontFamily = t.display,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "Нажмите «+» внизу, чтобы добавить первого NPC",
                                color = t.colors.muted,
                                fontFamily = t.body,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.npcs, key = { it.id }) { npc ->
                            NpcCardItem(
                                npc = npc,
                                onEdit = { viewModel.startEditing(npc) },
                                onDelete = { viewModel.requestDelete(npc) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (deletingNpc != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            containerColor = t.colors.panel,
            shape = RoundedCornerShape(t.corner),
            title = {
                Text(
                    text = "Удалить NPC?",
                    color = t.colors.text,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Вы уверены, что хотите удалить «${deletingNpc!!.name}»?",
                    color = t.colors.muted,
                    fontFamily = t.body
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("Удалить", color = t.colors.hot, fontFamily = t.display)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Отмена", color = t.colors.muted, fontFamily = t.body)
                }
            }
        )
    }

    // ModalBottomSheet: visible when editingNpc is not null
    if (editingNpc != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelEdit() },
            sheetState = sheetState,
            containerColor = t.colors.panel
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (editingNpc!!.id.isEmpty()) "Создать NPC" else "Редактировать NPC",
                    color = t.colors.text,
                    fontFamily = t.display,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // NPC name
                OutlinedTextField(
                    value = editorName,
                    onValueChange = { editorName = it },
                    label = { Text("Имя NPC", color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = bestiaryTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // NPC description (multiline)
                OutlinedTextField(
                    value = editorDescription,
                    onValueChange = { editorDescription = it },
                    label = { Text("Описание", color = t.colors.muted, fontFamily = t.body) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    colors = bestiaryTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stats row: HP + Armor + Damage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // HP integer field
                    OutlinedTextField(
                        value = editorHp,
                        onValueChange = { editorHp = it.filter { c -> c.isDigit() } },
                        label = { Text("❤️ HP", color = t.colors.muted, fontFamily = t.body) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = bestiaryTextFieldColors()
                    )
                    // Armor integer field
                    OutlinedTextField(
                        value = editorArmor,
                        onValueChange = { editorArmor = it.filter { c -> c.isDigit() } },
                        label = { Text("🛡️ Броня", color = t.colors.muted, fontFamily = t.body) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = bestiaryTextFieldColors()
                    )
                    // Damage string field (e.g. 2d6+3)
                    OutlinedTextField(
                        value = editorDamage,
                        onValueChange = { editorDamage = it },
                        label = { Text("⚔️ Урон", color = t.colors.muted, fontFamily = t.body) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = bestiaryTextFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tags field (comma-separated)
                OutlinedTextField(
                    value = editorTags,
                    onValueChange = { editorTags = it },
                    label = { Text("Теги (через запятую)", color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = bestiaryTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                Button(
                    onClick = {
                        scope.launch {
                            val parsedTags = editorTags
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            viewModel.saveNpc(
                                editingNpc!!.copy(
                                    name        = editorName,
                                    description = editorDescription,
                                    hp          = editorHp.toIntOrNull() ?: 0,
                                    armor       = editorArmor.toIntOrNull() ?: 0,
                                    damage      = editorDamage,
                                    tags        = parsedTags
                                )
                            )
                            sheetState.hide()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(t.corner),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = t.colors.accent,
                        contentColor = t.colors.bg
                    )
                ) {
                    Text("Сохранить", fontFamily = t.display, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// NPC card showing name, description, mini-stats, tags, and action icons
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NpcCardItem(
    npc: NpcCard,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val t = LocalXanteya.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(t.corner),
        colors = CardDefaults.cardColors(containerColor = t.colors.panel),
        border = BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // NPC name
            Text(
                text = npc.name,
                color = t.colors.text,
                fontFamily = t.display,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            // Description preview (2 lines max)
            if (npc.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = npc.description,
                    color = t.colors.muted,
                    fontFamily = t.body,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mini-stats row: HP / Armor / Damage
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("❤️ HP: ${npc.hp}", color = t.colors.hot, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = t.body)
                Text("🛡️ Броня: ${npc.armor}", color = t.colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = t.body)
                if (npc.damage.isNotBlank()) {
                    Text("⚔️ Урон: ${npc.damage}", color = t.colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = t.body)
                }
            }

            // Tags chips
            if (npc.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    npc.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(t.corner / 2))
                                .background(t.colors.bg)
                                .border(1.dp, t.colors.line, RoundedCornerShape(t.corner / 2))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = tag, color = t.colors.muted, fontSize = 11.sp, fontFamily = t.body)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Edit / Delete action row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Редактировать",
                        tint = t.colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Удалить",
                        tint = t.colors.hot,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Shared OutlinedTextField color scheme for Bestiary editor
@Composable
private fun bestiaryTextFieldColors(): TextFieldColors {
    val t = LocalXanteya.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = t.colors.text,
        unfocusedTextColor = t.colors.text,
        focusedBorderColor = t.colors.accent,
        unfocusedBorderColor = t.colors.line,
        cursorColor = t.colors.accent,
        focusedLabelColor = t.colors.accent,
        unfocusedLabelColor = t.colors.muted,
        focusedContainerColor = t.colors.bg,
        unfocusedContainerColor = t.colors.bg
    )
}
